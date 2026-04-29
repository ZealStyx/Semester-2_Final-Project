package io.github.superteam.resonance.footstep;

/** Material types used by the footstep system. */
public enum SurfaceMaterial {
    CONCRETE(1.0f),
    WOOD(0.92f),
    METAL(1.05f),
    GRAVEL(1.10f),
    WATER(1.08f),
    CARPET(0.72f),
    TILE(0.96f),
    DIRT(0.88f);

    private final float intensityMultiplier;

    SurfaceMaterial(float intensityMultiplier) {
        this.intensityMultiplier = intensityMultiplier;
    }

    public float intensityMultiplier() {
        return intensityMultiplier;
    }
}
