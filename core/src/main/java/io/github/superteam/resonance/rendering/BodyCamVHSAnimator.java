package io.github.superteam.resonance.rendering;

/**
 * Provides subtle temporal modulation for the body-cam effect.
 */
public final class BodyCamVHSAnimator {
    private float wobbleStrength;
    private float vignettePulse;

    public void update(float elapsedSeconds) {
        wobbleStrength = 0.018f * (float) Math.sin(elapsedSeconds * 1.2f);
        vignettePulse = 0.015f * (float) Math.sin(elapsedSeconds * 0.9f);
    }

    public float wobbleStrength() {
        return wobbleStrength;
    }

    public float vignettePulse() {
        return vignettePulse;
    }
}
