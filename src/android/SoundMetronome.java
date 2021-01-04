package org.cordova.plugins.metronome;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoundMetronome implements Metronome, HapticFeedback, Runnable {

    private static final String TAG = Metronome.class.getCanonicalName();

    private final Vibrator vibrator;
    private final Context context;
    private final Resources resources;
    private final String packageName;

    private String measure;
    private int bpm;
    private final Map<Character, MetronomeNote> notes;

    private boolean isPlaying;

    private int beatCount;

    private long delayDiff = 1000;

    private final Handler handler;

    private final SoundPool soundPool;


    public SoundMetronome(Context context) {
        this.context = context;
        this.resources = context.getResources();
        this.packageName = context.getPackageName();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        this.notes = new HashMap<>();
        this.isPlaying = false;
        this.measure = "";
        this.soundPool = buildSoundPool();

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        this.handler = new Handler(Looper.myLooper());

        defineSounds();
        calculateDelayDiff();
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

        if (measure.equals("Z")) {
            haptic();
            return;
        }

        if (notes.isEmpty()) {
            return; // no sounds do nothing
        }

        Log.d(TAG, String.format("starting metronome with '%s', at: %d BPM", measure, bpm));

        if (isPlaying) {
            beatCount = measure.equals(this.measure) ? beatCount % this.measure.length() : 0;
            this.measure = measure;
            this.bpm = bpm;
        } else {
            beatCount = 0;
            this.measure = measure;
            this.bpm = bpm;

            isPlaying = true;

            run();
        }
        haptic();
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

    @Override
    public void haptic() {
        vibrator.vibrate(HapticFeedbackConstants.CONFIRM);
    }

    private void playSingleNote(MetronomeNote note) {
        soundPool.play(note.soundId, note.volume, note.volume, 1, 0, 1.0f);
        Log.d(TAG, String.format(
            "Note %s, was played at %.1f vol. sound: %d, beatCount: %s",
            note.symbol, note.volume, note.soundId, beatCount));
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

    private void calculateDelayDiff() {
        long start = System.currentTimeMillis();
        playSingleNote(notes.get('E'));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long end = System.currentTimeMillis();
                delayDiff = end - start;
                Log.d(TAG, String.format("System delay diff: %s", delayDiff));
            }
        }, 1000L);
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
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build())
            .build();
    }

    @SuppressWarnings("deprecation")
    private SoundPool buildSoundPoolOld() {
        return new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    private void defineSound(Character symbol, int resId, float volume) {
        try {
            AssetFileDescriptor fd = resources.openRawResourceFd(resId);
            MediaExtractor me = new MediaExtractor();
            me.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());

            MediaFormat mf = me.getTrackFormat(0);
            int bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
            int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            fd.close();

            Log.d(TAG, String.format("loading: %s, bitRate: %s, sampleRate: %s", symbol, bitRate, sampleRate));

            int soundId = soundPool.load(context, resId, 0);
            notes.put(symbol, new MetronomeNote(symbol, soundId, volume));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
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

    @Override
    public void run() {
        if (!isPlaying) return;

        playSingleNote(currentBuffer());
        beatCount += 1;
        handler.postDelayed(this, delayInterval());
    }

    private long delayInterval() {
        long diff = 1000 * 1000L / delayDiff;
        long interval = 60L * diff / bpm / measure.length();

        Log.d(TAG, String.format("interval %s (-+ %s)", interval, Math.abs(diff - 1000L)));

        return interval;

    }
}
