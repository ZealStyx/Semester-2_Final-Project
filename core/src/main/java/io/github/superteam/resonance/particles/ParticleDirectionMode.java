package io.github.superteam.resonance.particles;

public enum ParticleDirectionMode {
    UP,
    OUTWARD,
    VECTOR,
    RANDOM,
    CHAOS,
    MULTI,
    INWARD,
    TANGENT,
    SURFACE_NORMAL;

    public static ParticleDirectionMode fromName(String value) {
        if (value == null || value.isBlank()) {
            return UP;
        }

        for (ParticleDirectionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return UP;
    }
}
