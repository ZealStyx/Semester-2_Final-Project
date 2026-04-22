package io.github.superteam.resonance.rendering.blind.modifiers;

import io.github.superteam.resonance.rendering.blind.BlindEffectModifier;

/**
 * Temporary flare reveal modifier.
 */
public final class BlindFlareModifier implements BlindEffectModifier {
    private final float visibilityDeltaMeters;
    private float remainingSeconds;

    public BlindFlareModifier(float visibilityDeltaMeters, float durationSeconds) {
        this.visibilityDeltaMeters = Math.max(0f, visibilityDeltaMeters);
        this.remainingSeconds = Math.max(0f, durationSeconds);
    }

    @Override
    public void update(float deltaSeconds) {
        remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, deltaSeconds));
    }

    @Override
    public float getVisibilityDeltaMeters() {
        return remainingSeconds > 0f ? visibilityDeltaMeters : 0f;
    }

    @Override
    public boolean isExpired() {
        return remainingSeconds <= 0f;
    }

    @Override
    public String getModifierName() {
        return "Flare";
    }

    @Override
    public String debugSummary() {
        return "Flare=+" + String.format("%.2f", getVisibilityDeltaMeters()) + "m (" + String.format("%.1f", remainingSeconds) + "s)";
    }
}
