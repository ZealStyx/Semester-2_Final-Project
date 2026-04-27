package io.github.superteam.resonance.audio;

import com.badlogic.gdx.math.MathUtils;

/**
 * Mutable channel volume configuration.
 */
public final class AudioChannelConfig {
    private float masterVolume = 1f;
    private float musicVolume = 0.8f;
    private float ambienceVolume = 0.7f;
    private float sfxVolume = 1f;

    public float volumeFor(AudioChannel channel) {
        return switch (channel == null ? AudioChannel.MASTER : channel) {
            case MASTER -> masterVolume;
            case MUSIC -> musicVolume;
            case AMBIENCE -> ambienceVolume;
            case SFX -> sfxVolume;
        };
    }

    public void setVolume(AudioChannel channel, float volume) {
        float clamped = MathUtils.clamp(volume, 0f, 1f);
        switch (channel == null ? AudioChannel.MASTER : channel) {
            case MASTER -> masterVolume = clamped;
            case MUSIC -> musicVolume = clamped;
            case AMBIENCE -> ambienceVolume = clamped;
            case SFX -> sfxVolume = clamped;
        }
    }

    public float masterVolume() {
        return masterVolume;
    }

    public float musicVolume() {
        return musicVolume;
    }

    public float ambienceVolume() {
        return ambienceVolume;
    }

    public float sfxVolume() {
        return sfxVolume;
    }
}
