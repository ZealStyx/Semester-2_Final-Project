package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.math.MathUtils;

/**
 * Runtime-tunable settings for the unified BodyCam + VHS post process.
 */
public final class BodyCamVHSSettings {
    public float fovDiagonalDegrees = 90.0f;
    public float barrelDistortionStrength = 0.35f;
    public float chromaticAberrationPixels = 2.4f;
    public float vignetteRadius = 0.85f;
    public float vignetteSoftness = 0.3f;
    public float vhsScanLineStrength = 0.15f;
    public float vhsTapeNoiseAmount = 0.08f;
    public float crtCurveAmount = 0.12f;
    public boolean enabled = true;

    public void validate() {
        fovDiagonalDegrees = MathUtils.clamp(fovDiagonalDegrees, 60.0f, 170.0f);
        barrelDistortionStrength = MathUtils.clamp(barrelDistortionStrength, 0.0f, 0.5f);
        chromaticAberrationPixels = MathUtils.clamp(chromaticAberrationPixels, 0.0f, 8.0f);
        vignetteRadius = MathUtils.clamp(vignetteRadius, 0.1f, 1.5f);
        vignetteSoftness = MathUtils.clamp(vignetteSoftness, 0.01f, 1.0f);
        vhsScanLineStrength = MathUtils.clamp(vhsScanLineStrength, 0.0f, 1.0f);
        vhsTapeNoiseAmount = MathUtils.clamp(vhsTapeNoiseAmount, 0.0f, 1.0f);
        crtCurveAmount = MathUtils.clamp(crtCurveAmount, 0.0f, 1.0f);
    }
}
