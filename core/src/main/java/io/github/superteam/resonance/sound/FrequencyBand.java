package io.github.superteam.resonance.sound;

/**
 * Frequency-band buckets used by the dual-band propagation model.
 */
public enum FrequencyBand {
    LOW(0.18f),
    HIGH(0.55f);

    private final float defaultAttenuationAlpha;

    FrequencyBand(float defaultAttenuationAlpha) {
        if (defaultAttenuationAlpha < 0f) {
            throw new IllegalArgumentException("Default attenuation alpha must be non-negative.");
        }
        this.defaultAttenuationAlpha = defaultAttenuationAlpha;
    }

    public float defaultAttenuationAlpha() {
        return defaultAttenuationAlpha;
    }
}
