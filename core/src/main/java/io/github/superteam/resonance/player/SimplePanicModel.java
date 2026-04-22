package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.rendering.blind.PanicLevelProvider;

/**
 * Lightweight panic model used for blind-effect gameplay integration.
 */
public final class SimplePanicModel implements PanicLevelProvider {
    private float threatDistanceMeters = Float.POSITIVE_INFINITY;
    private float loudSoundIntensity;
    private float currentHealth = 100f;
    private float maxHealth = 100f;
    private float smoothedPanic;

    public void setThreatDistanceMeters(float threatDistanceMeters) {
        this.threatDistanceMeters = threatDistanceMeters;
    }

    public void setLoudSoundIntensity(float loudSoundIntensity) {
        this.loudSoundIntensity = MathUtils.clamp(loudSoundIntensity, 0f, 1f);
    }

    public void setHealth(float currentHealth, float maxHealth) {
        this.maxHealth = Math.max(1f, maxHealth);
        this.currentHealth = MathUtils.clamp(currentHealth, 0f, this.maxHealth);
    }

    public void update(float deltaSeconds) {
        float threatPanic = 0f;
        if (Float.isFinite(threatDistanceMeters) && threatDistanceMeters < 20f) {
            threatPanic = 1f - (threatDistanceMeters / 20f);
        }

        float soundPanic = loudSoundIntensity * 0.5f;
        float healthRatio = currentHealth / maxHealth;
        float healthPanic = (1f - healthRatio) * 0.3f;

        float targetPanic = MathUtils.clamp(threatPanic + soundPanic + healthPanic, 0f, 1f);
        float alpha = MathUtils.clamp(Math.max(0f, deltaSeconds) * 4f, 0f, 1f);
        smoothedPanic = MathUtils.lerp(smoothedPanic, targetPanic, alpha);
    }

    @Override
    public float currentPanicLevel() {
        return smoothedPanic;
    }
}
