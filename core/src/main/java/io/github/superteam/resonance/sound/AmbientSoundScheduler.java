package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Emits ambient sound events on independent timers.
 */
public final class AmbientSoundScheduler {
    private final List<AmbientSource> ambientSources = new ArrayList<>();

    public void clear() {
        ambientSources.clear();
    }

    public void addAmbientSource(AmbientSource ambientSource) {
        if (ambientSource != null) {
            ambientSources.add(ambientSource);
        }
    }

    public void update(float deltaSeconds, float nowSeconds, SoundPropagationOrchestrator soundPropagationOrchestrator) {
        if (soundPropagationOrchestrator == null) {
            return;
        }
        float clampedDeltaSeconds = Math.max(0f, deltaSeconds);

        for (AmbientSource ambientSource : ambientSources) {
            ambientSource.remainingTimerSeconds -= clampedDeltaSeconds;
            if (ambientSource.remainingTimerSeconds > 0f) {
                continue;
            }

            soundPropagationOrchestrator.emitSoundEvent(ambientSource.buildEvent(nowSeconds), nowSeconds);
            ambientSource.resetTimer();
        }
    }

    public static final class AmbientSource {
        private final SoundEvent eventType;
        private final String sourceNodeId;
        private final Vector3 worldPosition;
        private final float baseIntensity;
        private final float minIntervalSeconds;
        private final float intervalRangeSeconds;
        private float remainingTimerSeconds;

        public AmbientSource(
            SoundEvent eventType,
            String sourceNodeId,
            Vector3 worldPosition,
            float baseIntensity,
            float minIntervalSeconds,
            float intervalRangeSeconds
        ) {
            this.eventType = Objects.requireNonNull(eventType, "Ambient event type must not be null.");
            if (sourceNodeId == null || sourceNodeId.isBlank()) {
                throw new IllegalArgumentException("Ambient source node id must not be blank.");
            }
            this.sourceNodeId = sourceNodeId;
            this.worldPosition = worldPosition == null ? new Vector3() : new Vector3(worldPosition);
            this.baseIntensity = Math.max(0f, baseIntensity);
            this.minIntervalSeconds = Math.max(0.05f, minIntervalSeconds);
            this.intervalRangeSeconds = Math.max(0f, intervalRangeSeconds);
            resetTimer();
        }

        public SoundEventData buildEvent(float nowSeconds) {
            return new SoundEventData(eventType, sourceNodeId, worldPosition, baseIntensity, nowSeconds);
        }

        public StaticSoundSource toStaticSoundSource(String sourceIdPrefix) {
            String safePrefix = (sourceIdPrefix == null || sourceIdPrefix.isBlank()) ? "ambient" : sourceIdPrefix;
            return new StaticSoundSource(
                safePrefix + ":" + sourceNodeId + ":" + eventType.name(),
                eventType,
                sourceNodeId,
                worldPosition,
                baseIntensity,
                minIntervalSeconds,
                intervalRangeSeconds
            );
        }

        private void resetTimer() {
            remainingTimerSeconds = minIntervalSeconds + (MathUtils.random() * intervalRangeSeconds);
        }
    }
}
