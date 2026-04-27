package io.github.superteam.resonance.particles;

public enum ParticleBillboardMode {
    SPHERICAL,
    Y_AXIS_LOCKED,
    VELOCITY_ALIGNED,
    VELOCITY_STRETCHED,
    HORIZONTAL,
    WORLD_FIXED;

    public static ParticleBillboardMode fromName(String value) {
        if (value == null || value.isBlank()) {
            return SPHERICAL;
        }

        for (ParticleBillboardMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return SPHERICAL;
    }
}