package io.github.superteam.resonance.rendering.blind.modifiers;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.rendering.blind.BlindEffectModifier;
import io.github.superteam.resonance.rendering.blind.PanicLevelProvider;

/**
 * Dynamic panic contribution that shrinks visibility as panic rises.
 */
public final class BlindPanicModifier implements BlindEffectModifier {
    private final PanicLevelProvider panicLevelProvider;
    private float reductionFactor;
    private float triggerThreshold;
    private float smoothedPanic;

    public BlindPanicModifier(PanicLevelProvider panicLevelProvider, float reductionFactor, float triggerThreshold) {
        this.panicLevelProvider = panicLevelProvider;
        this.reductionFactor = MathUtils.clamp(reductionFactor, 0f, 1f);
        this.triggerThreshold = MathUtils.clamp(triggerThreshold, 0f, 1f);
    }

    public void setReductionFactor(float reductionFactor) {
        this.reductionFactor = MathUtils.clamp(reductionFactor, 0f, 1f);
    }

    public void setTriggerThreshold(float triggerThreshold) {
        this.triggerThreshold = MathUtils.clamp(triggerThreshold, 0f, 1f);
    }

    @Override
    public void update(float deltaSeconds) {
        float rawPanic = panicLevelProvider == null ? 0f : MathUtils.clamp(panicLevelProvider.currentPanicLevel(), 0f, 1f);
        float alpha = MathUtils.clamp(Math.max(0f, deltaSeconds) * 7f, 0f, 1f);
        smoothedPanic = MathUtils.lerp(smoothedPanic, rawPanic, alpha);
    }

    @Override
    public float getVisibilityDeltaMeters() {
        if (smoothedPanic <= triggerThreshold) {
            return 0f;
        }

        float normalized = (smoothedPanic - triggerThreshold) / Math.max(0.0001f, (1f - triggerThreshold));
        return -MathUtils.clamp(normalized, 0f, 1f) * reductionFactor;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public String getModifierName() {
        return "Panic";
    }

    @Override
    public String debugSummary() {
        return "Panic=" + String.format("%.2f", smoothedPanic) + " delta=" + String.format("%.2f", getVisibilityDeltaMeters()) + "m";
    }
}
