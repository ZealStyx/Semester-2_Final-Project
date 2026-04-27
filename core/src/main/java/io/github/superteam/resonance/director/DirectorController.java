package io.github.superteam.resonance.director;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;
import io.github.superteam.resonance.sound.EchoDirectorListener;
import io.github.superteam.resonance.sound.EnemyHearingTarget;
import io.github.superteam.resonance.sound.HearingCategory;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Tracks a lightweight session-adaptive tension tier and sound-memory scaffold for phase 3.
 */
public final class DirectorController implements PlayerFeatureExtractor.PlayerFeaturesListener, EchoDirectorListener, EnemyHearingTarget {
    private static final float DIRECTOR_TICK_SECONDS = 2.0f;
    private static final float HYSTERESIS_SECONDS = 1.0f;
    private static final float HEARING_THRESHOLD = 0.22f;
    private static final float HEARD_MEMORY_SECONDS = 3.0f;

    private final KMeansClassifier classifier = new KMeansClassifier();

    private PlayerFeatureExtractor.PlayerFeatures latestFeatures;
    private DirectorTier currentTier = DirectorTier.CALM;
    private DirectorTier pendingTier = DirectorTier.CALM;
    private float directorTickAccumulatorSeconds;
    private float pendingTierSeconds;

    private String currentNodeId = "center";
    private String lastHeardNodeId = "center";
    private float lastHeardIntensity;
    private float soundMemorySecondsRemaining;
    private SoundEventData lastSoundEventData;
    private float lastSoundPropagationPeak;

    @Override
    public void onFeatures(PlayerFeatureExtractor.PlayerFeatures playerFeatures) {
        latestFeatures = playerFeatures;
    }

    @Override
    public void onSoundEvent(SoundEventData soundEventData, PropagationResult propagationResult) {
        lastSoundEventData = soundEventData;
        lastSoundPropagationPeak = soundEventData == null ? 0.0f : soundEventData.baseIntensity();
        if (propagationResult != null && soundEventData != null) {
            lastSoundPropagationPeak = Math.max(lastSoundPropagationPeak, propagationResult.getIntensityOrZero(soundEventData.sourceNodeId()));
        }
    }

    @Override
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    @Override
    public void onSoundHeard(float propagatedIntensity, SoundEventData soundEventData) {
        if (soundEventData == null || propagatedIntensity < HEARING_THRESHOLD) {
            return;
        }

        if (soundEventData.eventType().hearingCategory() == HearingCategory.AMBIENCE) {
            return;
        }

        lastHeardNodeId = soundEventData.sourceNodeId();
        lastHeardIntensity = propagatedIntensity;
        soundMemorySecondsRemaining = HEARD_MEMORY_SECONDS;
    }

    public void setCurrentNodeId(String currentNodeId) {
        if (currentNodeId != null && !currentNodeId.isBlank()) {
            this.currentNodeId = currentNodeId;
        }
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0.0f, deltaSeconds);
        directorTickAccumulatorSeconds += clampedDelta;
        if (soundMemorySecondsRemaining > 0.0f) {
            soundMemorySecondsRemaining = Math.max(0.0f, soundMemorySecondsRemaining - clampedDelta);
        }

        if (pendingTier != currentTier) {
            pendingTierSeconds += clampedDelta;
            if (pendingTierSeconds >= HYSTERESIS_SECONDS) {
                currentTier = pendingTier;
                pendingTierSeconds = 0.0f;
            }
        }

        while (directorTickAccumulatorSeconds >= DIRECTOR_TICK_SECONDS) {
            directorTickAccumulatorSeconds -= DIRECTOR_TICK_SECONDS;
            if (latestFeatures == null) {
                continue;
            }

            DirectorTier classifiedTier = classifier.classify(latestFeatures);
            if (classifiedTier == currentTier) {
                pendingTier = currentTier;
                pendingTierSeconds = 0.0f;
                continue;
            }

            if (pendingTier != classifiedTier) {
                pendingTier = classifiedTier;
                pendingTierSeconds = 0.0f;
            }
        }
    }

    public DirectorSnapshot snapshot() {
        return new DirectorSnapshot(
            currentTier,
            pendingTier,
            soundMemorySecondsRemaining,
            lastHeardNodeId,
            lastHeardIntensity,
            lastSoundEventData,
            lastSoundPropagationPeak,
            currentNodeId
        );
    }

    public enum DirectorTier {
        CALM,
        TENSE,
        PANICKED;

        public float fogMultiplier() {
            return switch (this) {
                case CALM -> 0.60f;
                case TENSE -> 0.80f;
                case PANICKED -> 1.00f;
            };
        }

        public float ambientHeartbeatBpm() {
            return switch (this) {
                case CALM -> 0.0f;
                case TENSE -> 90.0f;
                case PANICKED -> 120.0f;
            };
        }

        public float footstepMultiplier() {
            return switch (this) {
                case CALM -> 0.90f;
                case TENSE -> 1.05f;
                case PANICKED -> 1.25f;
            };
        }

        public float enemyAggression() {
            return switch (this) {
                case CALM -> 0.35f;
                case TENSE -> 0.70f;
                case PANICKED -> 1.0f;
            };
        }
    }

    public static final class DirectorSnapshot {
        private final DirectorTier currentTier;
        private final DirectorTier pendingTier;
        private final float soundMemorySecondsRemaining;
        private final String lastHeardNodeId;
        private final float lastHeardIntensity;
        private final SoundEventData lastSoundEventData;
        private final float lastSoundPropagationPeak;
        private final String currentNodeId;

        private DirectorSnapshot(
            DirectorTier currentTier,
            DirectorTier pendingTier,
            float soundMemorySecondsRemaining,
            String lastHeardNodeId,
            float lastHeardIntensity,
            SoundEventData lastSoundEventData,
            float lastSoundPropagationPeak,
            String currentNodeId
        ) {
            this.currentTier = currentTier;
            this.pendingTier = pendingTier;
            this.soundMemorySecondsRemaining = soundMemorySecondsRemaining;
            this.lastHeardNodeId = lastHeardNodeId;
            this.lastHeardIntensity = lastHeardIntensity;
            this.lastSoundEventData = lastSoundEventData;
            this.lastSoundPropagationPeak = lastSoundPropagationPeak;
            this.currentNodeId = currentNodeId;
        }

        public DirectorTier currentTier() {
            return currentTier;
        }

        public DirectorTier pendingTier() {
            return pendingTier;
        }

        public float soundMemorySecondsRemaining() {
            return soundMemorySecondsRemaining;
        }

        public String lastHeardNodeId() {
            return lastHeardNodeId;
        }

        public float lastHeardIntensity() {
            return lastHeardIntensity;
        }

        public SoundEventData lastSoundEventData() {
            return lastSoundEventData;
        }

        public float lastSoundPropagationPeak() {
            return lastSoundPropagationPeak;
        }

        public String currentNodeId() {
            return currentNodeId;
        }

        public String hudLine() {
            String heardText = soundMemorySecondsRemaining > 0.0f
                ? lastHeardNodeId + " @" + String.format("%.2f", lastHeardIntensity)
                : "idle";
            return "Director=" + currentTier + "  Heartbeat=" + String.format("%.0f", currentTier.ambientHeartbeatBpm()) + " BPM  Hunter=" + heardText;
        }
    }

    private static final class KMeansClassifier {
        private static final int FEATURE_COUNT = 5;
        private static final float EMA_ALPHA = 0.20f;
        private static final float[] FEATURE_MIN = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        private static final float[] FEATURE_MAX = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

        private final float[][] centroids = {
            {0.05f, 0.05f, 0.82f, 0.05f, 0.10f},
            {0.38f, 0.35f, 0.42f, 0.18f, 0.28f},
            {0.74f, 0.70f, 0.14f, 0.42f, 0.56f}
        };

        private DirectorTier classify(PlayerFeatureExtractor.PlayerFeatures features) {
            float[] featureVector = normalize(features);
            int closestCentroidIndex = 0;
            float closestDistance = Float.POSITIVE_INFINITY;

            for (int centroidIndex = 0; centroidIndex < centroids.length; centroidIndex++) {
                float distance = distanceSquared(featureVector, centroids[centroidIndex]);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestCentroidIndex = centroidIndex;
                }
            }

            updateCentroid(closestCentroidIndex, featureVector);
            return DirectorTier.values()[closestCentroidIndex];
        }

        private float[] normalize(PlayerFeatureExtractor.PlayerFeatures features) {
            float[] vector = new float[FEATURE_COUNT];
            vector[0] = clamp((features.avgSpeed / 5.0f), FEATURE_MIN[0], FEATURE_MAX[0]);
            vector[1] = clamp((features.rotationRate / 180.0f), FEATURE_MIN[1], FEATURE_MAX[1]);
            vector[2] = clamp(features.stationaryRatio, FEATURE_MIN[2], FEATURE_MAX[2]);
            vector[3] = clamp(features.collisionRate, FEATURE_MIN[3], FEATURE_MAX[3]);
            vector[4] = clamp(features.backtrackRatio, FEATURE_MIN[4], FEATURE_MAX[4]);
            return vector;
        }

        private void updateCentroid(int centroidIndex, float[] featureVector) {
            float[] centroid = centroids[centroidIndex];
            for (int featureIndex = 0; featureIndex < FEATURE_COUNT; featureIndex++) {
                centroid[featureIndex] = clamp(
                    MathUtils.lerp(centroid[featureIndex], featureVector[featureIndex], EMA_ALPHA),
                    FEATURE_MIN[featureIndex],
                    FEATURE_MAX[featureIndex]
                );
            }
        }

        private float distanceSquared(float[] left, float[] right) {
            float sum = 0.0f;
            for (int featureIndex = 0; featureIndex < FEATURE_COUNT; featureIndex++) {
                float delta = left[featureIndex] - right[featureIndex];
                sum += delta * delta;
            }
            return sum;
        }

        private float clamp(float value, float minValue, float maxValue) {
            return MathUtils.clamp(value, minValue, maxValue);
        }
    }
}
