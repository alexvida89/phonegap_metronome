package org.cordova.plugins.metronome;

public class MetronomeNote {
    public Character symbol;
    public int soundId = -1;
    public float volume;

    public MetronomeNote(Character symbol, int soundId, float volume) {
        this.symbol = symbol;
        this.soundId = soundId;
        this.volume = volume;
    }
}

