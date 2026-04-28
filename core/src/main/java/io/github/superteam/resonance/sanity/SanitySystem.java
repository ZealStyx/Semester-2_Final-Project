package io.github.superteam.resonance.sanity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Runtime sanity meter (0..100) with pluggable drains and effects.
 */
public final class SanitySystem {
    public static final float MAX_SANITY = 100f;

    private final Array<SanityDrainSource> drainSources = new Array<>();
    private final Array<SanityEffect> effects = new Array<>();

    private float sanity = MAX_SANITY;
    private float drainMultiplier = 1f;
    private float lastDeltaThisFrame;

    public void addDrainSource(SanityDrainSource source) {
        if (source != null && !drainSources.contains(source, true)) {
            drainSources.add(source);
        }
    }

    public void addEffect(SanityEffect effect) {
        if (effect != null && !effects.contains(effect, true)) {
            effects.add(effect);
        }
    }

    public void setDrainMultiplier(float mult) {
        drainMultiplier = Math.max(0f, mult);
    }

    public void setSanity(float value) {
        sanity = MathUtils.clamp(value, 0f, MAX_SANITY);
    }

    public float getSanity() {
        return sanity;
    }

    public void applyDelta(float delta) {
        sanity = MathUtils.clamp(sanity + delta, 0f, MAX_SANITY);
        lastDeltaThisFrame += delta;
    }

    public float getLastDeltaThisFrame() {
        return lastDeltaThisFrame;
    }

    public void update(float deltaSeconds, Context context) {
        float dt = Math.max(0f, deltaSeconds);
        lastDeltaThisFrame = 0f;

        float aggregateDelta = 0f;
        for (SanityDrainSource source : drainSources) {
            float perSecond = Math.max(0f, source.drainPerSecond(context));
            aggregateDelta -= perSecond * drainMultiplier * dt;
            aggregateDelta += source.immediateDelta(context);
        }

        if (aggregateDelta != 0f) {
            applyDelta(aggregateDelta);
        }

        for (SanityEffect effect : effects) {
            effect.apply(this, context, dt);
        }
    }

    public Tier tier() {
        if (sanity >= 75f) {
            return Tier.STABLE;
        }
        if (sanity >= 50f) {
            return Tier.LOW;
        }
        if (sanity >= 25f) {
            return Tier.MEDIUM;
        }
        if (sanity >= 10f) {
            return Tier.HIGH;
        }
        return Tier.CRITICAL;
    }

    public enum Tier {
        STABLE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static final class Context {
        private final Vector3 playerPosition;
        private final float lightExposure;
        private final float enemyDistanceMeters;
        private final float loudSoundIntensity;
        private final float elapsedSeconds;

        public Context(
            Vector3 playerPosition,
            float lightExposure,
            float enemyDistanceMeters,
            float loudSoundIntensity,
            float elapsedSeconds
        ) {
            this.playerPosition = playerPosition == null ? new Vector3() : new Vector3(playerPosition);
            this.lightExposure = MathUtils.clamp(lightExposure, 0f, 1f);
            this.enemyDistanceMeters = Math.max(0f, enemyDistanceMeters);
            this.loudSoundIntensity = MathUtils.clamp(loudSoundIntensity, 0f, 1f);
            this.elapsedSeconds = Math.max(0f, elapsedSeconds);
        }

        public Vector3 playerPosition() {
            return new Vector3(playerPosition);
        }

        public float lightExposure() {
            return lightExposure;
        }

        public float enemyDistanceMeters() {
            return enemyDistanceMeters;
        }

        public float loudSoundIntensity() {
            return loudSoundIntensity;
        }

        public float elapsedSeconds() {
            return elapsedSeconds;
        }
    }
}
