package io.github.superteam.resonance.rendering.blind;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Loads blind effect config from JSON while preserving safe defaults.
 */
public final class BlindEffectConfigStore {
    private BlindEffectConfigStore() {
    }

    public static BlindEffectRevealConfig loadOrDefault(String internalConfigPath) {
        BlindEffectRevealConfig config = new BlindEffectRevealConfig();

        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            config.validate();
            return config;
        }

        FileHandle file = Gdx.files.internal(internalConfigPath);
        if (!file.exists()) {
            config.validate();
            return config;
        }

        try {
            JsonValue root = new JsonReader().parse(file);
            config.baselineVisibilityMeters = root.getFloat("baseline_visibility_meters", config.baselineVisibilityMeters);
            config.baselineFadeEdgeSoftness = root.getFloat("baseline_fade_edge_softness", config.baselineFadeEdgeSoftness);
            config.fogStrength = root.getFloat("fog_strength", config.fogStrength);
            config.visibilityClampMinMeters = root.getFloat("visibility_clamp_min_meters", config.visibilityClampMinMeters);
            config.visibilityClampMaxMeters = root.getFloat("visibility_clamp_max_meters", config.visibilityClampMaxMeters);

            JsonValue fogColor = root.get("blind_fog_color");
            if (fogColor != null && fogColor.size >= 4) {
                config.blindFogColor.set(
                    fogColor.get(0).asFloat(),
                    fogColor.get(1).asFloat(),
                    fogColor.get(2).asFloat(),
                    fogColor.get(3).asFloat()
                );
            }

            JsonValue panic = root.get("panic_modifier");
            if (panic != null) {
                config.panicModifier.enabled = panic.getBoolean("enabled", config.panicModifier.enabled);
                config.panicModifier.visibilityReductionFactor = panic.getFloat("visibility_reduction_factor", config.panicModifier.visibilityReductionFactor);
                config.panicModifier.triggerPanicThreshold = panic.getFloat("trigger_panic_threshold", config.panicModifier.triggerPanicThreshold);
            }

            JsonValue flare = root.get("flare_modifier");
            if (flare != null) {
                config.flareModifier.enabled = flare.getBoolean("enabled", config.flareModifier.enabled);
                config.flareModifier.visibilityRadiusMeters = flare.getFloat("visibility_radius_meters", config.flareModifier.visibilityRadiusMeters);
                config.flareModifier.durationSeconds = flare.getFloat("duration_seconds", config.flareModifier.durationSeconds);
            }

            JsonValue sonar = root.get("sonar_reveal");
            if (sonar != null) {
                config.sonarReveal.enabled = sonar.getBoolean("enabled", config.sonarReveal.enabled);
                config.sonarReveal.clapShoutRadiusMeters = sonar.getFloat("clap_shout_radius_meters", config.sonarReveal.clapShoutRadiusMeters);
                config.sonarReveal.durationSeconds = sonar.getFloat("duration_seconds", config.sonarReveal.durationSeconds);
                config.sonarReveal.triggersAcousticVisualization = sonar.getBoolean("triggers_acoustic_visualization", config.sonarReveal.triggersAcousticVisualization);
            }
        } catch (Exception ignored) {
            // Defaults already set.
        }

        config.validate();
        return config;
    }
}
