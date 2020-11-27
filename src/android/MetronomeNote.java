package org.cordova.plugins.metronome;

public class MetronomeNote {
    public Character symbol;
    public short[] buffer;
    public float volume;

    public MetronomeNote(Character symbol, short[] buffer, float volume) {
        this.symbol = symbol;
        this.buffer = buffer;
        this.volume = volume;
    }
}
