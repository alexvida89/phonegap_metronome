package org.cordova.plugins.metronome;

public interface Metronome {
    void start(int speed, String measure);
    void stop();
    boolean isPlaying();
    void playSingleNote(Character symbol);
}
