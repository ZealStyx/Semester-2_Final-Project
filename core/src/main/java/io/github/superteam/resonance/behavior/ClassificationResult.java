package io.github.superteam.resonance.behavior;

import java.util.Arrays;

/** Result of a behavior classification pass. */
public final class ClassificationResult {
    private final PlayerArchetype archetype;
    private final float[] scores;
    private final int sampleCount;
    private final float inertia;

    public ClassificationResult(PlayerArchetype archetype, float[] scores, int sampleCount, float inertia) {
        this.archetype = archetype == null ? PlayerArchetype.METHODICAL : archetype;
        this.scores = scores == null ? new float[PlayerArchetype.values().length] : Arrays.copyOf(scores, scores.length);
        this.sampleCount = Math.max(0, sampleCount);
        this.inertia = Math.max(0f, inertia);
    }

    public static ClassificationResult neutral() {
        return new ClassificationResult(PlayerArchetype.METHODICAL, new float[] { 0.25f, 0.25f, 0.25f, 0.25f }, 0, 0f);
    }

    public PlayerArchetype archetype() {
        return archetype;
    }

    public float[] scores() {
        return Arrays.copyOf(scores, scores.length);
    }

    public int sampleCount() {
        return sampleCount;
    }

    public float inertia() {
        return inertia;
    }

    public float scoreFor(PlayerArchetype value) {
        int index = value.ordinal();
        if (index < 0 || index >= scores.length) {
            return 0f;
        }
        return scores[index];
    }
}
