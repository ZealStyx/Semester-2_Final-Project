package io.github.superteam.resonance.scare;

import com.badlogic.gdx.math.MathUtils;

/**
 * Group B #11 runtime director that decides when a jump scare should fire.
 */
public final class JumpScareDirector {
    private static final float DEFAULT_MIN_INTERVAL_SECONDS = 50f;
    private static final float DEFAULT_TENSION_THRESHOLD = 70f;

    private final float[] scareTypeWeights = { 0.35f, 0.25f, 0.15f, 0.15f, 0.10f };

    private float minScareInterval = DEFAULT_MIN_INTERVAL_SECONDS;
    private float tensionThreshold = DEFAULT_TENSION_THRESHOLD;
    private float cooldownRemaining;
    private float tension;
    private JumpScare pendingScare;

    public void setMinInterval(float secs) {
        minScareInterval = Math.max(1f, secs);
    }

    public void setTensionThreshold(float threshold) {
        tensionThreshold = MathUtils.clamp(threshold, 1f, 200f);
    }

    public void setScareTypeWeights(float[] weights) {
        if (weights == null || weights.length < scareTypeWeights.length) {
            return;
        }
        float sum = 0f;
        for (int i = 0; i < scareTypeWeights.length; i++) {
            scareTypeWeights[i] = Math.max(0f, weights[i]);
            sum += scareTypeWeights[i];
        }
        if (sum <= 0f) {
            scareTypeWeights[0] = 1f;
            for (int i = 1; i < scareTypeWeights.length; i++) {
                scareTypeWeights[i] = 0f;
            }
        }
    }

    public void update(float deltaSeconds, float sanityValue, float tierFactor, float soundIntensity) {
        float dt = Math.max(0f, deltaSeconds);
        cooldownRemaining = Math.max(0f, cooldownRemaining - dt);

        float sanityPressure = MathUtils.clamp((100f - sanityValue) / 100f, 0f, 1f);
        float soundPressure = MathUtils.clamp(soundIntensity, 0f, 1f);
        float tierPressure = MathUtils.clamp(tierFactor, 0f, 1f);

        float buildRate = (sanityPressure * 22f) + (soundPressure * 35f) + (tierPressure * 16f);
        tension += buildRate * dt;
        tension = Math.max(0f, tension - (7f * dt));

        if (cooldownRemaining > 0f || pendingScare != null || tension < tensionThreshold) {
            return;
        }

        float intensity = MathUtils.clamp((tension - tensionThreshold) / Math.max(1f, tensionThreshold), 0.2f, 1f);
        ScareType type = pickType();
        pendingScare = new JumpScare(type, intensity, messageFor(type, intensity));
        cooldownRemaining = minScareInterval;
        tension *= 0.30f;
    }

    public JumpScare pollTriggeredScare() {
        JumpScare scare = pendingScare;
        pendingScare = null;
        return scare;
    }

    public float tension() {
        return tension;
    }

    public float cooldownRemaining() {
        return cooldownRemaining;
    }

    private ScareType pickType() {
        float total = 0f;
        for (float weight : scareTypeWeights) {
            total += Math.max(0f, weight);
        }
        if (total <= 0f) {
            return ScareType.AUDIO_ONLY;
        }

        float roll = MathUtils.random(total);
        float cursor = 0f;
        ScareType[] values = ScareType.values();
        for (int i = 0; i < values.length; i++) {
            cursor += Math.max(0f, scareTypeWeights[i]);
            if (roll <= cursor) {
                return values[i];
            }
        }
        return ScareType.ENVIRONMENT;
    }

    private String messageFor(ScareType type, float intensity) {
        String suffix = intensity >= 0.75f ? " (severe)" : intensity >= 0.45f ? " (moderate)" : " (light)";
        return switch (type) {
            case AUDIO_ONLY -> "Distant noise spike" + suffix;
            case FLASH -> "Flash scare pulse" + suffix;
            case ENEMY_APPEAR -> "Enemy silhouette" + suffix;
            case HALLUCINATION -> "Hallucination surge" + suffix;
            case ENVIRONMENT -> "Environment scare cue" + suffix;
        };
    }
}
