package io.github.superteam.resonance.model;

/**
 * Immutable metadata for a single named animation clip.
 */
public final class ModelAnimationInfo {

    private final String name;
    private final float durationSeconds;

    public ModelAnimationInfo(String name, float durationSeconds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Animation name must not be blank.");
        }
        if (durationSeconds < 0f) {
            throw new IllegalArgumentException("Animation duration must be non-negative.");
        }
        this.name = name;
        this.durationSeconds = durationSeconds;
    }

    public String name() {
        return name;
    }

    public float durationSeconds() {
        return durationSeconds;
    }
}
