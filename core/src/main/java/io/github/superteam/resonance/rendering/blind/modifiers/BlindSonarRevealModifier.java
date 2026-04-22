package io.github.superteam.resonance.rendering.blind.modifiers;

import io.github.superteam.resonance.rendering.blind.BlindEffectModifier;

/**
 * Temporary sonar reveal modifier from clap/shout events.
 */
public final class BlindSonarRevealModifier implements BlindEffectModifier {
    private final float revealDeltaMeters;
    private float remainingSeconds;

    public BlindSonarRevealModifier(float revealDeltaMeters, float durationSeconds) {
        this.revealDeltaMeters = Math.max(0f, revealDeltaMeters);
        this.remainingSeconds = Math.max(0f, durationSeconds);
    }

    @Override
    public void update(float deltaSeconds) {
        remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, deltaSeconds));
    }

    @Override
    public float getVisibilityDeltaMeters() {
        return remainingSeconds > 0f ? revealDeltaMeters : 0f;
    }

    @Override
    public boolean isExpired() {
        return remainingSeconds <= 0f;
    }

    @Override
    public String getModifierName() {
        return "Sonar";
    }

    @Override
    public String debugSummary() {
        return "Sonar=+" + String.format("%.2f", getVisibilityDeltaMeters()) + "m (" + String.format("%.1f", remainingSeconds) + "s)";
    }
}
