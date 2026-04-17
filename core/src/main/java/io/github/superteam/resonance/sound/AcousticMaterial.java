package io.github.superteam.resonance.sound;

/**
 * Acoustic surface categories used to tune sound travel through the graph.
 */
public enum AcousticMaterial {
    AIR(1.00f, 1.00f, 0.00f, 0.00f),
    CONCRETE(1.00f, 0.05f, 0.82f, 0.13f),
    METAL(0.82f, 0.12f, 0.75f, 0.13f),
    WOOD(1.15f, 0.22f, 0.55f, 0.23f),
    WATER(1.35f, 0.08f, 0.70f, 0.22f),
    GLASS(0.90f, 0.35f, 0.55f, 0.10f),
    FABRIC(1.20f, 0.70f, 0.05f, 0.25f),
    DIRT(1.10f, 0.75f, 0.10f, 0.15f),
    VENT_DUCT(0.70f, 0.15f, 0.65f, 0.20f);

    private final float traversalMultiplier;
    private final float transmissionCoefficient;
    private final float reflectionCoefficient;
    private final float absorptionCoefficient;

    AcousticMaterial(
        float traversalMultiplier,
        float transmissionCoefficient,
        float reflectionCoefficient,
        float absorptionCoefficient
    ) {
        if (traversalMultiplier < 0f) {
            throw new IllegalArgumentException("Traversal multiplier must be non-negative.");
        }
        if (transmissionCoefficient < 0f || transmissionCoefficient > 1f) {
            throw new IllegalArgumentException("Transmission coefficient must be within [0, 1].");
        }
        if (reflectionCoefficient < 0f || reflectionCoefficient > 1f) {
            throw new IllegalArgumentException("Reflection coefficient must be within [0, 1].");
        }
        if (absorptionCoefficient < 0f || absorptionCoefficient > 1f) {
            throw new IllegalArgumentException("Absorption coefficient must be within [0, 1].");
        }

        this.traversalMultiplier = traversalMultiplier;
        this.transmissionCoefficient = transmissionCoefficient;
        this.reflectionCoefficient = reflectionCoefficient;
        this.absorptionCoefficient = absorptionCoefficient;
    }

    public float traversalMultiplier() {
        return traversalMultiplier;
    }

    public float transmissionCoefficient() {
        return transmissionCoefficient;
    }

    public float reflectionCoefficient() {
        return reflectionCoefficient;
    }

    public float absorptionCoefficient() {
        return absorptionCoefficient;
    }

    /**
     * Backward-compatible accessor kept temporarily while the codebase migrates.
     */
    public float getMultiplier() {
        return traversalMultiplier;
    }
}
