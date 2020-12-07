package org.cordova.plugins.metronome;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class ToneGenerator {

    public static Tone generateTone(double frequency, int duration, boolean loop) {
        int count = (int)(Constants.SAMPLE_RATE * 2.0 * (duration / 1000.0)) & ~1;
        short[] samples = new short[count];
        for(int i = 0; i < count; i += 2){
            short sample = (short)(Math.sin(2 * Math.PI * i / (Constants.SAMPLE_RATE / frequency)) * 0x7FFF);
            samples[i] = sample;
            samples[i + 1] = sample;
        }

        AudioTrack track = buildAudioTrack(count * (Short.SIZE / 8));
        track.write(samples, 0, count);

        if (loop) {
            track.setLoopPoints(0, samples.length / 4, -1);
        }

        return new Tone(track);
    }

    private static AudioTrack buildAudioTrack(int bufferSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return buildAudioTrackNew(bufferSize);
        }

        return buildAudioTrackOld(bufferSize);
    }

    private static AudioTrack buildAudioTrackNew(int bufferSize) {
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

    private static AudioTrack buildAudioTrackOld(int bufferSize) {
        return new AudioTrack(
            AudioManager.STREAM_MUSIC, Constants.SAMPLE_RATE, Constants.CHANNEL_MASK,
            Constants.ENCODING, bufferSize, AudioTrack.MODE_STATIC);
    }

    public static class Tone {
        private AudioTrack track;
        private boolean isPlaying = false;

        Tone(AudioTrack track) {
            this.track = track;
        }

        public void play() {
            track.play();
            isPlaying = true;
        }

        public void release() {
            track.stop();
            track.release();
            isPlaying = false;
        }

        public boolean isPlaying() {
            return isPlaying;
        }
    }
}
