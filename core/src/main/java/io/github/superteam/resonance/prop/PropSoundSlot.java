package io.github.superteam.resonance.prop;

public final class PropSoundSlot {
    public String name = "slot";
    public String soundId = "";
    public float volume = 1f;
    public boolean looped;
    public float radius = 1f;

    public PropSoundSlot() {
    }

    public PropSoundSlot(String name, String soundId, float volume, boolean looped, float radius) {
        this.name = name;
        this.soundId = soundId;
        this.volume = volume;
        this.looped = looped;
        this.radius = radius;
    }
}
