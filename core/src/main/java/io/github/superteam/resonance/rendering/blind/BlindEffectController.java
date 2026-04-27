package io.github.superteam.resonance.rendering.blind;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.rendering.blind.modifiers.BlindBaselineModifier;
import io.github.superteam.resonance.rendering.blind.modifiers.BlindFlareModifier;
import io.github.superteam.resonance.rendering.blind.modifiers.BlindPanicModifier;
import io.github.superteam.resonance.rendering.blind.modifiers.BlindSonarRevealModifier;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Maintains blind-visibility modifier stack and exposes shader-ready fog values.
 */
public final class BlindEffectController {
    private final Array<BlindEffectModifier> runtimeModifiers = new Array<>();
    private final Array<String> debugLines = new Array<>();
    private final BlindBaselineModifier baselineModifier;
    private final BlindPanicModifier panicModifier;

    private BlindEffectRevealConfig config;
    private float visibilityMeters;
    private float fogStartMeters;
    private float fogEndMeters;
    private float tierFogMultiplier = 1f;

    public BlindEffectController(BlindEffectRevealConfig config, PanicLevelProvider panicLevelProvider) {
        this.config = config == null ? new BlindEffectRevealConfig() : config;
        this.config.validate();

        baselineModifier = new BlindBaselineModifier(this.config.baselineVisibilityMeters);
        panicModifier = new BlindPanicModifier(
            panicLevelProvider,
            this.config.panicModifier.visibilityReductionFactor,
            this.config.panicModifier.triggerPanicThreshold
        );

        visibilityMeters = this.config.baselineVisibilityMeters;
        updateFogBounds();
    }

    public void reloadConfig(BlindEffectRevealConfig updatedConfig) {
        if (updatedConfig == null) {
            return;
        }
        config = updatedConfig;
        config.validate();
        baselineModifier.setBaselineMeters(config.baselineVisibilityMeters);
        panicModifier.setReductionFactor(config.panicModifier.visibilityReductionFactor);
        panicModifier.setTriggerThreshold(config.panicModifier.triggerPanicThreshold);
    }

    public void addModifier(BlindEffectModifier modifier) {
        if (modifier != null) {
            runtimeModifiers.add(modifier);
        }
    }

    public void triggerFlareReveal() {
        if (!config.flareModifier.enabled) {
            return;
        }

        float delta = Math.max(0f, config.flareModifier.visibilityRadiusMeters - config.baselineVisibilityMeters);
        addModifier(new BlindFlareModifier(delta, config.flareModifier.durationSeconds));
    }

    public boolean triggerSonarReveal() {
        if (!config.sonarReveal.enabled) {
            return false;
        }

        float delta = Math.max(0f, config.sonarReveal.clapShoutRadiusMeters - config.baselineVisibilityMeters);
        addModifier(new BlindSonarRevealModifier(delta, config.sonarReveal.durationSeconds));
        return true;
    }

    public boolean onSoundEvent(SoundEventData soundEventData) {
        if (soundEventData == null) {
            return false;
        }
        SoundEvent event = soundEventData.eventType();
        if ((event == SoundEvent.CLAP_SHOUT || event == SoundEvent.MIC_INPUT)
                && config.sonarReveal.enabled
                && config.sonarReveal.triggersAcousticVisualization) {
            return triggerSonarReveal();
        }
        return false;
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);

        baselineModifier.update(clampedDelta);
        panicModifier.update(clampedDelta);

        for (int i = runtimeModifiers.size - 1; i >= 0; i--) {
            BlindEffectModifier modifier = runtimeModifiers.get(i);
            modifier.update(clampedDelta);
            if (modifier.isExpired()) {
                runtimeModifiers.removeIndex(i);
            }
        }

        float total = baselineModifier.getVisibilityDeltaMeters();
        if (config.panicModifier.enabled) {
            total += panicModifier.getVisibilityDeltaMeters();
        }
        for (BlindEffectModifier modifier : runtimeModifiers) {
            total += modifier.getVisibilityDeltaMeters();
        }

        total = MathUtils.clamp(total, config.visibilityClampMinMeters, config.visibilityClampMaxMeters);
        visibilityMeters = MathUtils.lerp(visibilityMeters, total, MathUtils.clamp(clampedDelta * 10f, 0f, 1f));
        updateFogBounds();
        rebuildDebugLines();
    }

    private void updateFogBounds() {
        float effectiveVisibility = Math.max(0.1f, visibilityMeters * tierFogMultiplier);
        fogEndMeters = Math.max(0.5f, effectiveVisibility);
        float fogZoneWidth = Math.max(0.1f, fogEndMeters * config.fogZoneWidthFraction);
        fogStartMeters = Math.max(0.05f, fogEndMeters - fogZoneWidth);
    }

    private void rebuildDebugLines() {
        debugLines.clear();
        debugLines.add("[BLIND EFFECT]");
        debugLines.add("Baseline=" + String.format("%.2f", baselineModifier.getVisibilityDeltaMeters()) + "m");
        debugLines.add(panicModifier.debugSummary());

        for (BlindEffectModifier modifier : runtimeModifiers) {
            debugLines.add(modifier.debugSummary());
        }

        debugLines.add("Visibility=" + String.format("%.2f", visibilityMeters) + "m");
        debugLines.add("FogStart=" + String.format("%.2f", fogStartMeters) + " FogEnd=" + String.format("%.2f", fogEndMeters));
    }

    public float visibilityMeters() {
        return visibilityMeters;
    }

    public float fogStartMeters() {
        return fogStartMeters;
    }

    public float fogEndMeters() {
        return fogEndMeters;
    }

    public float fogStrength() {
        return config.fogStrength;
    }

    public Color fogColor() {
        return config.blindFogColor;
    }

    public Array<String> debugLines() {
        return debugLines;
    }

    /**
     * Applies an external visibility multiplier (for example, Director tier pressure).
     * Values less than 1 tighten visibility/fog, values greater than 1 relax it.
     */
    public void setTierFogMultiplier(float multiplier) {
        tierFogMultiplier = MathUtils.clamp(multiplier, 0.25f, 2.0f);
        updateFogBounds();
    }

    public String compactDebugText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < debugLines.size; i++) {
            builder.append(debugLines.get(i));
            if (i < debugLines.size - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
