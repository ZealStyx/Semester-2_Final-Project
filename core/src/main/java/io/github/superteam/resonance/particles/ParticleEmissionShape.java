package io.github.superteam.resonance.particles;

public enum ParticleEmissionShape {
    POINT,
    SPHERE,
    BOX,
    RING,
    DISC,
    CONE,
    LINE,
    SURFACE_SPHERE,
    TORUS;

    public static ParticleEmissionShape fromName(String value) {
        if (value == null || value.isBlank()) {
            return POINT;
        }

        for (ParticleEmissionShape shape : values()) {
            if (shape.name().equalsIgnoreCase(value)) {
                return shape;
            }
        }
        return POINT;
    }
}
