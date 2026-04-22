package io.github.superteam.resonance.rendering.blind.modifiers;

import io.github.superteam.resonance.rendering.blind.BlindEffectModifier;

/**
 * Always-on baseline visibility contribution.
 */
public final class BlindBaselineModifier implements BlindEffectModifier {
    private float baselineMeters;

    public BlindBaselineModifier(float baselineMeters) {
        this.baselineMeters = Math.max(0.1f, baselineMeters);
    }

    public void setBaselineMeters(float baselineMeters) {
        this.baselineMeters = Math.max(0.1f, baselineMeters);
    }

    @Override
    public void update(float deltaSeconds) {
        // Static contribution; no time decay.
    }

    @Override
    public float getVisibilityDeltaMeters() {
        return baselineMeters;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public String getModifierName() {
        return "Baseline";
    }
}
