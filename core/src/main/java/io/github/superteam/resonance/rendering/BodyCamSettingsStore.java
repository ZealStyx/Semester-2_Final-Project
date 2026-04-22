package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Loads body cam settings from JSON while preserving safe defaults.
 */
public final class BodyCamSettingsStore {
    private BodyCamSettingsStore() {
    }

    public static BodyCamVHSSettings loadOrDefault(String internalConfigPath) {
        BodyCamVHSSettings settings = new BodyCamVHSSettings();

        if (Gdx.files == null || internalConfigPath == null || internalConfigPath.isBlank()) {
            settings.validate();
            return settings;
        }

        FileHandle configFile = Gdx.files.internal(internalConfigPath);
        if (!configFile.exists()) {
            settings.validate();
            return settings;
        }

        try {
            JsonValue root = new JsonReader().parse(configFile);
            settings.fovDiagonalDegrees = root.getFloat("fov_diagonal_degrees", settings.fovDiagonalDegrees);
            settings.barrelDistortionStrength = root.getFloat("barrel_distortion_strength", settings.barrelDistortionStrength);
            settings.chromaticAberrationPixels = root.getFloat("chromatic_aberration_pixels", settings.chromaticAberrationPixels);
            settings.vignetteRadius = root.getFloat("vignette_radius", settings.vignetteRadius);
            settings.vignetteSoftness = root.getFloat("vignette_softness", settings.vignetteSoftness);
            settings.vhsScanLineStrength = root.getFloat("vhs_scan_line_strength", settings.vhsScanLineStrength);
            settings.vhsTapeNoiseAmount = root.getFloat("vhs_tape_noise_amount", settings.vhsTapeNoiseAmount);
            settings.crtCurveAmount = root.getFloat("crt_curve_amount", settings.crtCurveAmount);
            settings.enabled = root.getBoolean("enabled", settings.enabled);
        } catch (Exception ignored) {
            // Defaults are already populated.
        }

        settings.validate();
        return settings;
    }
}
