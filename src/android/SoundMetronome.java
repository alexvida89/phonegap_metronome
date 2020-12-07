package org.cordova.plugins.metronome;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
import java.util.HashMap;
import java.util.Map;

public class SoundMetronome implements Metronome {

    private static final String TAG = Metronome.class.getCanonicalName();

    private final Context context;
    private final Resources resources;
    private final String packageName;

    private String measure;
    private int bpm;
    private final Map<Character, MetronomeNote> notes;

    private boolean isPlaying;

    private int beatCount;

    private final Handler handler;

    private final SoundPool soundPool;


    public SoundMetronome(Context context) {
        this.context = context;
        this.resources = context.getResources();
        this.packageName = context.getPackageName();

        this.notes = new HashMap<>();
        this.isPlaying = false;
        this.measure = "";
        this.soundPool = buildSoundPool();

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        this.handler = new Handler(Looper.myLooper());

        defineSounds();
    }

    @Override
    public void start(int bpm, String measure) throws IllegalArgumentException {

        if (measure.length() == 0) {
            Exception e = new Exception("the measure cannot be empty");
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        if (measure.equals("X")) {
            stop();
            return;
        }

        if (notes.isEmpty()) {
            return; // no sounds do nothing
        }

        Log.d(TAG, String.format("starting metronome with '%s', at: %d BPM", measure, bpm));

        this.bpm = bpm;

        if (isPlaying) {
            beatCount = measure.equals(this.measure) ? beatCount % this.measure.length() : 0;
            return;
        }

        this.measure = measure;
        beatCount = 0;

        isPlaying = true;

        scheduleBeat();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stopping metronome");
        isPlaying = false;
    }

    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    private void playSingleNote(MetronomeNote note) {
        soundPool.play(note.soundId, note.volume, note.volume, 0, 0, 1.0f);
        Log.d(TAG, String.format(
            "Note %s, was played at %.1f vol. sound: %d",
            note.symbol, note.volume, note.soundId));
    }

    private void defineSounds() {

        if (!notes.isEmpty()) return; // need to be execute only one time

        int highId = resolveResourceId("hq_woodblock_high_ogg");
        defineSound('e', highId, 1.0f);
        defineSound('f', highId, 0.5f);
        defineSound('g', highId, 0.3f);
        defineSound('h', highId, 0.1f);
        defineSound('E', highId, 0f);

        int midId = resolveResourceId("hq_woodblock_mid_ogg");
        defineSound('i', midId, 1.0f);
        defineSound('j', midId, 0.5f);
        defineSound('k', midId, 0.3f);
        defineSound('l', midId, 0.1f);
        defineSound('I', midId, 0f);

        int lowId = resolveResourceId("hq_woodblock_low_ogg");
        defineSound('m', lowId, 1.0f);
        defineSound('n', lowId, 0.5f);
        defineSound('o', lowId, 0.3f);
        defineSound('p', lowId, 0.1f);
        defineSound('M', lowId, 0f);
    }

    private SoundPool buildSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return buildSoundPoolNew();
        }

        return buildSoundPoolOld();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private SoundPool buildSoundPoolNew() {
        return new SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .build();
    }

    @SuppressWarnings("deprecation")
    private SoundPool buildSoundPoolOld() {
        return new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    private void defineSound(Character symbol, int resId, float volume) {
        int soundId = soundPool.load(context, resId, 0);
        notes.put(symbol, new MetronomeNote(symbol, soundId, volume));
    }

    private int resolveResourceId(String resourceName) {
        return resources.getIdentifier(resourceName, "raw", packageName);
    }

    private MetronomeNote currentBuffer() {
        while (true) {
            int compass = beatCount % measure.length();
            try {
                Character soundSymbol = measure.charAt(compass);
                Log.d(TAG, String.format("current buffer to play %s", soundSymbol));
                return notes.get(soundSymbol);
            } catch (NullPointerException e) {
                Log.d(TAG, String.format("buffer for compass %s doesn't exists", compass));

                beatCount += 1;
            }
        }
    }

    private void scheduleBeat() {
        if (!isPlaying) return;


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                beatCount += 1;
                scheduleBeat();
            }
        }, delayInterval());

        playSingleNote(currentBuffer());
    }

    private long delayInterval() {
        return 60000 / bpm / measure.length();
    }
}
