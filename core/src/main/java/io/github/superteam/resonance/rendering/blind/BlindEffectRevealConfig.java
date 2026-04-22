package io.github.superteam.resonance.rendering.blind;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * Tunable runtime config for blind baseline and reveal modifiers.
 */
public final class BlindEffectRevealConfig {
    public float baselineVisibilityMeters = 1.8f;
    public float baselineFadeEdgeSoftness = 0.3f;
    public float fogStrength = 0.82f;
    public float visibilityClampMinMeters = 0.1f;
    public float visibilityClampMaxMeters = 10.0f;
    public final Color blindFogColor = new Color(0.04f, 0.04f, 0.09f, 1f);

    public final PanicModifierConfig panicModifier = new PanicModifierConfig();
    public final FlareModifierConfig flareModifier = new FlareModifierConfig();
    public final SonarRevealConfig sonarReveal = new SonarRevealConfig();

    public void validate() {
        baselineVisibilityMeters = MathUtils.clamp(baselineVisibilityMeters, 0.1f, 10f);
        baselineFadeEdgeSoftness = MathUtils.clamp(baselineFadeEdgeSoftness, 0.05f, 2f);
        fogStrength = MathUtils.clamp(fogStrength, 0f, 1f);
        visibilityClampMinMeters = MathUtils.clamp(visibilityClampMinMeters, 0.05f, 4f);
        visibilityClampMaxMeters = MathUtils.clamp(visibilityClampMaxMeters, visibilityClampMinMeters + 0.05f, 15f);
        blindFogColor.r = MathUtils.clamp(blindFogColor.r, 0f, 1f);
        blindFogColor.g = MathUtils.clamp(blindFogColor.g, 0f, 1f);
        blindFogColor.b = MathUtils.clamp(blindFogColor.b, 0f, 1f);
        blindFogColor.a = MathUtils.clamp(blindFogColor.a, 0f, 1f);
        panicModifier.validate();
        flareModifier.validate();
        sonarReveal.validate();
    }

    public static final class PanicModifierConfig {
        public boolean enabled = true;
        public float visibilityReductionFactor = 0.5f;
        public float triggerPanicThreshold = 0.7f;

        void validate() {
            visibilityReductionFactor = MathUtils.clamp(visibilityReductionFactor, 0f, 1f);
            triggerPanicThreshold = MathUtils.clamp(triggerPanicThreshold, 0f, 1f);
        }
    }

    public static final class FlareModifierConfig {
        public boolean enabled = true;
        public float visibilityRadiusMeters = 4.0f;
        public float durationSeconds = 12.0f;

        void validate() {
            visibilityRadiusMeters = MathUtils.clamp(visibilityRadiusMeters, 0.1f, 10f);
            durationSeconds = MathUtils.clamp(durationSeconds, 0.1f, 60f);
        }
    }

    public static final class SonarRevealConfig {
        public boolean enabled = true;
        public float clapShoutRadiusMeters = 2.5f;
        public float durationSeconds = 1.2f;
        public boolean triggersAcousticVisualization = true;

        void validate() {
            clapShoutRadiusMeters = MathUtils.clamp(clapShoutRadiusMeters, 0.1f, 10f);
            durationSeconds = MathUtils.clamp(durationSeconds, 0.05f, 10f);
        }
    }
}
