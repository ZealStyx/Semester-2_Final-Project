package io.github.superteam.resonance.behavior;

import java.util.Arrays;

/** Simple fixed-size vector wrapper used by the classifier. */
public final class BehaviorFeatureVector {
    private final float[] values;

    public BehaviorFeatureVector(float... values) {
        this.values = values == null ? new float[0] : Arrays.copyOf(values, values.length);
    }

    public static BehaviorFeatureVector from(BehaviorSample sample) {
        if (sample == null) {
            return new BehaviorFeatureVector();
        }
        return new BehaviorFeatureVector(
            sample.averageVelocity,
            sample.averageNoiseRms,
            sample.crouchFraction,
            sample.stationaryFraction,
            sample.cameraAngularVariance,
            sample.flashlightToggleRate,
            sample.sanityLossThisWindow,
            sample.lightExposure
        );
    }

    public int size() {
        return values.length;
    }

    public float get(int index) {
        return values[index];
    }

    public float[] raw() {
        return Arrays.copyOf(values, values.length);
    }

    public float distanceSquared(BehaviorFeatureVector other) {
        if (other == null || other.size() != size()) {
            return Float.POSITIVE_INFINITY;
        }
        float sum = 0f;
        for (int i = 0; i < values.length; i++) {
            float delta = values[i] - other.values[i];
            sum += delta * delta;
        }
        return sum;
    }

    public static BehaviorFeatureVector average(BehaviorFeatureVector[] vectors) {
        if (vectors == null || vectors.length == 0) {
            return new BehaviorFeatureVector();
        }
        int size = vectors[0] == null ? 0 : vectors[0].size();
        float[] sums = new float[size];
        int count = 0;
        for (BehaviorFeatureVector vector : vectors) {
            if (vector == null || vector.size() != size) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                sums[i] += vector.get(i);
            }
            count++;
        }
        if (count == 0) {
            return new BehaviorFeatureVector();
        }
        for (int i = 0; i < size; i++) {
            sums[i] /= count;
        }
        return new BehaviorFeatureVector(sums);
    }
}
