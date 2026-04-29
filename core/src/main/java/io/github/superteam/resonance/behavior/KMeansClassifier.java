package io.github.superteam.resonance.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KMeansClassifier {
    private static final int K = 4;
    private static final int MAX_ITERS = 20;
    private static final int MIN_SAMPLES = 3;

    private static final BehaviorFeatureVector[] INITIAL_CENTROIDS = {
        new BehaviorFeatureVector(new float[] { 0.25f, 0.24f, 0.80f, 0.55f, 0.18f, 0.65f, 0.35f, 0.32f }),
        new BehaviorFeatureVector(new float[] { 0.18f, 0.12f, 0.72f, 0.70f, 0.08f, 0.18f, 0.18f, 0.72f }),
        new BehaviorFeatureVector(new float[] { 0.72f, 0.68f, 0.10f, 0.14f, 0.56f, 0.35f, 0.48f, 0.25f }),
        new BehaviorFeatureVector(new float[] { 0.88f, 0.82f, 0.05f, 0.10f, 0.77f, 0.52f, 0.86f, 0.18f })
    };

    public ClassificationResult classify(List<BehaviorFeatureVector> samples) {
        if (samples == null || samples.isEmpty()) {
            return ClassificationResult.neutral();
        }

        List<BehaviorFeatureVector> filtered = new ArrayList<>();
        for (BehaviorFeatureVector sample : samples) {
            if (sample != null) {
                filtered.add(sample);
            }
        }
        if (filtered.isEmpty()) {
            return ClassificationResult.neutral();
        }

        BehaviorFeatureVector[] centroids = Arrays.copyOf(INITIAL_CENTROIDS, INITIAL_CENTROIDS.length);
        if (filtered.size() < MIN_SAMPLES) {
            return classifyByNearestCentroid(BehaviorFeatureVector.average(filtered.toArray(new BehaviorFeatureVector[0])));
        }

        float inertia = 0f;
        for (int iteration = 0; iteration < MAX_ITERS; iteration++) {
            List<List<BehaviorFeatureVector>> clusters = new ArrayList<>(K);
            for (int i = 0; i < K; i++) {
                clusters.add(new ArrayList<>());
            }

            for (BehaviorFeatureVector sample : filtered) {
                int index = nearestCentroidIndex(sample, centroids);
                clusters.get(index).add(sample);
            }

            boolean changed = false;
            for (int i = 0; i < K; i++) {
                BehaviorFeatureVector updated = averageOrFallback(clusters.get(i), centroids[i]);
                if (updated.distanceSquared(centroids[i]) > 0.0001f) {
                    changed = true;
                }
                centroids[i] = updated;
            }

            inertia = computeInertia(filtered, centroids);
            if (!changed) {
                break;
            }
        }

        BehaviorFeatureVector mean = BehaviorFeatureVector.average(filtered.toArray(new BehaviorFeatureVector[0]));
        int winner = nearestCentroidIndex(mean, centroids);
        float[] scores = buildScores(mean, centroids);
        return new ClassificationResult(PlayerArchetype.fromIndex(winner), scores, filtered.size(), inertia);
    }

    private ClassificationResult classifyByNearestCentroid(BehaviorFeatureVector vector) {
        int winner = nearestCentroidIndex(vector, INITIAL_CENTROIDS);
        float[] scores = buildScores(vector, INITIAL_CENTROIDS);
        return new ClassificationResult(PlayerArchetype.fromIndex(winner), scores, 1, 0f);
    }

    private float[] buildScores(BehaviorFeatureVector sample, BehaviorFeatureVector[] centroids) {
        float[] scores = new float[K];
        float sum = 0f;
        for (int i = 0; i < K; i++) {
            float distance = sample.distanceSquared(centroids[i]);
            scores[i] = 1f / (1f + distance);
            sum += scores[i];
        }
        if (sum <= 0f) {
            Arrays.fill(scores, 0.25f);
            return scores;
        }
        for (int i = 0; i < K; i++) {
            scores[i] /= sum;
        }
        return scores;
    }

    private BehaviorFeatureVector averageOrFallback(List<BehaviorFeatureVector> cluster, BehaviorFeatureVector fallback) {
        if (cluster == null || cluster.isEmpty()) {
            return new BehaviorFeatureVector(fallback.raw());
        }
        return BehaviorFeatureVector.average(cluster.toArray(new BehaviorFeatureVector[0]));
    }

    private float computeInertia(List<BehaviorFeatureVector> samples, BehaviorFeatureVector[] centroids) {
        float sum = 0f;
        for (BehaviorFeatureVector sample : samples) {
            int index = nearestCentroidIndex(sample, centroids);
            sum += sample.distanceSquared(centroids[index]);
        }
        return sum;
    }

    private int nearestCentroidIndex(BehaviorFeatureVector sample, BehaviorFeatureVector[] centroids) {
        if (sample == null || centroids == null || centroids.length == 0) {
            return 0;
        }
        int winner = 0;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(centroids.length, K); i++) {
            float distance = sample.distanceSquared(centroids[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                winner = i;
            }
        }
        return winner;
    }
}
