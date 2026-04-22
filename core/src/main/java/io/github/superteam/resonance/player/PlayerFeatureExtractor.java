package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;

/**
 * Samples movement metrics into a 5-second rolling window and emits features every 2 seconds.
 */
public final class PlayerFeatureExtractor {
    private static final float SAMPLE_PERIOD_SECONDS = 0.2f;
    private static final float WINDOW_SECONDS = 5.0f;
    private static final float EMIT_PERIOD_SECONDS = 2.0f;
    private static final float STATIONARY_SPEED_THRESHOLD = 0.1f;
    private static final int MAX_SAMPLES = 25;

    public interface PlayerFeaturesListener {
        void onFeatures(PlayerFeatures playerFeatures);
    }

    public static final class PlayerFeatures {
        public final float avgSpeed;
        public final float rotationRate;
        public final float stationaryRatio;
        public final float collisionRate;
        public final float backtrackRatio;

        public PlayerFeatures(float avgSpeed, float rotationRate, float stationaryRatio, float collisionRate, float backtrackRatio) {
            this.avgSpeed = avgSpeed;
            this.rotationRate = rotationRate;
            this.stationaryRatio = stationaryRatio;
            this.collisionRate = collisionRate;
            this.backtrackRatio = backtrackRatio;
        }
    }

    private final float[] sampleTimes = new float[MAX_SAMPLES];
    private final float[] speedSamples = new float[MAX_SAMPLES];
    private final float[] yawRateSamples = new float[MAX_SAMPLES];
    private final byte[] stationarySamples = new byte[MAX_SAMPLES];
    private final byte[] backtrackSamples = new byte[MAX_SAMPLES];
    private final int[] collisionSamples = new int[MAX_SAMPLES];
    private final List<PlayerFeaturesListener> listeners = new ArrayList<>();

    private final Vector3 currentPosition = new Vector3();
    private final Vector3 previousPosition = new Vector3();
    private final Vector3 movementDirection = new Vector3();
    private final Vector3 lastKnownPosition = new Vector3();
    private final Vector3 toLastKnownDirection = new Vector3();

    private float elapsedSeconds;
    private float sampleAccumulatorSeconds;
    private float emitAccumulatorSeconds;
    private float previousYaw;

    private int headIndex;
    private int sampleCount;
    private boolean hasPreviousPosition;
    private boolean hasLastKnownPosition;

    public void addListener(PlayerFeaturesListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void setLastKnownPosition(Vector3 position) {
        if (position == null) {
            return;
        }
        lastKnownPosition.set(position);
        hasLastKnownPosition = true;
    }

    public void update(float deltaSeconds, PlayerController playerController) {
        if (playerController == null) {
            return;
        }

        float clampedDelta = Math.max(0f, deltaSeconds);
        elapsedSeconds += clampedDelta;
        sampleAccumulatorSeconds += clampedDelta;
        emitAccumulatorSeconds += clampedDelta;

        while (sampleAccumulatorSeconds >= SAMPLE_PERIOD_SECONDS) {
            sampleAccumulatorSeconds -= SAMPLE_PERIOD_SECONDS;
            recordSample(playerController);
        }

        if (emitAccumulatorSeconds >= EMIT_PERIOD_SECONDS) {
            emitAccumulatorSeconds -= EMIT_PERIOD_SECONDS;
            notifyListeners(computeFeatures());
        }
    }

    private void recordSample(PlayerController playerController) {
        playerController.getPosition(currentPosition);
        float horizontalSpeed = playerController.getHorizontalSpeed();
        float currentYaw = playerController.getYaw();
        float yawRate = deltaAngleDegrees(previousYaw, currentYaw) / SAMPLE_PERIOD_SECONDS;
        previousYaw = currentYaw;

        if (!hasLastKnownPosition) {
            lastKnownPosition.set(currentPosition);
            hasLastKnownPosition = true;
        }

        byte stationary = horizontalSpeed < STATIONARY_SPEED_THRESHOLD ? (byte) 1 : (byte) 0;
        byte backtrack = 0;
        int collisions = playerController.getAndResetCollisionCount();

        if (hasPreviousPosition) {
            movementDirection.set(currentPosition).sub(previousPosition);
            toLastKnownDirection.set(lastKnownPosition).sub(currentPosition);
            if (movementDirection.len2() > 0.000001f && toLastKnownDirection.len2() > 0.000001f) {
                backtrack = movementDirection.dot(toLastKnownDirection) > 0f ? (byte) 1 : (byte) 0;
            }
        }

        previousPosition.set(currentPosition);
        hasPreviousPosition = true;

        appendSample(elapsedSeconds, horizontalSpeed, yawRate, stationary, backtrack, collisions);
        evictExpiredSamples();
    }

    private void appendSample(float sampleTime, float speed, float yawRate, byte stationary, byte backtrack, int collisions) {
        int writeIndex;
        if (sampleCount < MAX_SAMPLES) {
            writeIndex = (headIndex + sampleCount) % MAX_SAMPLES;
            sampleCount++;
        } else {
            writeIndex = headIndex;
            headIndex = (headIndex + 1) % MAX_SAMPLES;
        }

        sampleTimes[writeIndex] = sampleTime;
        speedSamples[writeIndex] = speed;
        yawRateSamples[writeIndex] = yawRate;
        stationarySamples[writeIndex] = stationary;
        backtrackSamples[writeIndex] = backtrack;
        collisionSamples[writeIndex] = collisions;
    }

    private void evictExpiredSamples() {
        while (sampleCount > 0 && (elapsedSeconds - sampleTimes[headIndex]) > WINDOW_SECONDS) {
            headIndex = (headIndex + 1) % MAX_SAMPLES;
            sampleCount--;
        }
    }

    private PlayerFeatures computeFeatures() {
        if (sampleCount == 0) {
            return new PlayerFeatures(0f, 0f, 1f, 0f, 0f);
        }

        float speedSum = 0f;
        float yawRateSum = 0f;
        int stationaryCount = 0;
        int backtrackCount = 0;
        int collisionCount = 0;

        for (int i = 0; i < sampleCount; i++) {
            int index = (headIndex + i) % MAX_SAMPLES;
            speedSum += speedSamples[index];
            yawRateSum += Math.abs(yawRateSamples[index]);
            stationaryCount += stationarySamples[index];
            backtrackCount += backtrackSamples[index];
            collisionCount += collisionSamples[index] > 0 ? 1 : 0;
        }

        float invCount = 1f / sampleCount;
        return new PlayerFeatures(
            speedSum * invCount,
            yawRateSum * invCount,
            stationaryCount * invCount,
            collisionCount * invCount,
            backtrackCount * invCount
        );
    }

    private void notifyListeners(PlayerFeatures features) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onFeatures(features);
        }
    }

    private float deltaAngleDegrees(float from, float to) {
        float delta = (to - from) % 360f;
        if (delta > 180f) {
            delta -= 360f;
        } else if (delta < -180f) {
            delta += 360f;
        }
        return delta;
    }
}
