package org.cordova.plugins.metronome;

import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class SoundMetronome implements Metronome {

    private static final String TAG = Metronome.class.getCanonicalName();

    private final Resources resources;
    private final String packageName;

    private String measure;
    private int bpm;
    private final Map<Character, MetronomeNote> notes;

    private boolean isPlaying;

    private int beatCount;

    private final LooperThread looperThread;

    private final Pattern measurePattern = Pattern.compile("([^XefghEijklImnopM])");

    public SoundMetronome(Resources resources, String packageName) {
        this.notes = new HashMap<>();
        this.resources = resources;
        this.packageName = packageName;
        this.isPlaying = false;
        this.measure = "";
        this.looperThread = new LooperThread();
        this.looperThread.start();

        defineSounds();
    }

    @Override
    public void start(int bpm, String measure) throws IllegalArgumentException {

        if (measure.length() == 0) {
            throw new IllegalArgumentException("measure cannot be empty");
        }

        if (bpm == 0) {
            throw new IllegalArgumentException("bpm cannot be zero");
        }

        Matcher m = measurePattern.matcher(measure);
        if (m.matches()) {
            throw new IllegalArgumentException(String.format("measure entry '%s' is invalid", m.group(0)));
        }


        if (measure.equals("X")) {
            stop();
            return;
        }

        Log.d(TAG, String.format("starting metronome with '%s', at: %d BPM", measure, bpm));
        if (notes.isEmpty()) {
            return; // no sounds do nothing
        }
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

    @Override
    public void playSingleNote(Character symbol) {
        playSingleNote(notes.get(symbol));
    }

    public void playSingleNote(MetronomeNote note) {
        try {
            int bufferSize = getBufferSize();
            AudioTrack audioTrack = buildAudioTrack(bufferSize * 2);
            audioTrack.setVolume(note.volume);
            audioTrack.write(note.buffer, 0, note.buffer.length);
            audioTrack.setPlaybackRate(Constants.SAMPLE_RATE);
            audioTrack.play();
            Log.d(TAG, String.format(
                "Note %s, was played at %.1f vol. buffer length: %d",
                note.symbol, note.volume, note.buffer.length));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private int getBufferSize() {
        int bufferSize = 2 * AudioTrack.getMinBufferSize(Constants.SAMPLE_RATE,
            Constants.CHANNEL_MASK, Constants.ENCODING);
        Log.d(TAG, String.format("getBufferSize: %d", bufferSize));

        return bufferSize;
    }

    private void defineSounds() {

        if (!notes.isEmpty()) return; // need to be execute only one time

        try {
            short[] high = resourceToBuffer(resolveResourceId("raw", "hq_woodblock_high"));
            defineSound('e', high, 1.0f);
            defineSound('f', high, 0.5f);
            defineSound('g', high, 0.3f);
            defineSound('h', high, 0.1f);
            defineSound('E', high, 0f);

            short[] mid = resourceToBuffer(resolveResourceId("raw", "hq_woodblock_mid"));
            defineSound('i', mid, 1.0f);
            defineSound('j', mid, 0.5f);
            defineSound('k', mid, 0.3f);
            defineSound('l', mid, 0.1f);
            defineSound('I', mid, 0f);

            short[] low = resourceToBuffer(resolveResourceId("raw", "hq_woodblock_low"));
            defineSound('m', low, 1.0f);
            defineSound('n', low, 0.5f);
            defineSound('o', low, 0.3f);
            defineSound('p', low, 0.1f);
            defineSound('M', low, 0f);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private AudioTrack buildAudioTrack(int bufferSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return buildAudioTrackNew(bufferSize);
        }

        return buildAudioTrackOld(bufferSize);
    }

    private AudioTrack buildAudioTrackNew(int bufferSize) {
        return new AudioTrack.Builder()
            .setBufferSizeInBytes(bufferSize)
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(Constants.ENCODING)
                .setSampleRate(Constants.SAMPLE_RATE)
                .setChannelMask(Constants.CHANNEL_MASK)
                .build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build();
    }

    private AudioTrack buildAudioTrackOld(int bufferSize) {
        return new AudioTrack(
            AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE, Constants.CHANNEL_MASK,
            Constants.ENCODING, bufferSize, AudioTrack.MODE_STATIC);
    }

    private void defineSound(Character symbol, short[] buffer, float volume) {
        Log.d(TAG, String.format("defining sound buffer '%s' with volume: %.1f", symbol, volume));
        notes.put(symbol, new MetronomeNote(symbol, buffer, volume));
    }

    private int resolveResourceId(String resourceType, String resourceName) {
        return resources.getIdentifier(resourceName, resourceType, packageName);
    }

    private short[] resourceToBuffer(int resId) throws IOException {
        int bufferSize = 512;
        byte[] buffer = new byte[bufferSize];

        Log.d(TAG, String.format("resource (%d) to buffer", resId));
        InputStream inputStream = resources.openRawResource(resId);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int chunkSize;
        while ((chunkSize = bufferedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, chunkSize);
        }

        short[] shorts = new short[outputStream.size() / 2];
        ByteBuffer
            .wrap(outputStream.toByteArray())
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(shorts);

        return shorts;
    }

    private MetronomeNote currentBuffer() {
        while (true) {
            try {
                Character soundSymbol = measure.charAt(beatCount % measure.length());
                Log.d(TAG, String.format("current buffer to play %s", soundSymbol));
                return notes.get(soundSymbol);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
                beatCount += 1;
            }
        }
    }

    private void scheduleBeat() {
        if (!isPlaying) return;


        playSingleNote(currentBuffer());

        this.looperThread.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                beatCount += 1;
                scheduleBeat();
            }
        }, delayInterval());
    }

    private long delayInterval() {
        return 60000 / bpm / measure.length();
    }

    static class LooperThread extends Thread {
        public Handler handler = new Handler(Looper.myLooper());

        @Override
        public void run() {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }

            Looper.loop();
        }
    }
}
