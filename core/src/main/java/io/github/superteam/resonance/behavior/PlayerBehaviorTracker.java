package io.github.superteam.resonance.behavior;

import java.util.ArrayList;
import java.util.List;

public final class PlayerBehaviorTracker {
    private static final float WINDOW_SECONDS = 60f;

    private final List<TimedSample> samples = new ArrayList<>();

    public void record(float timeSeconds, BehaviorSample sample) {
        if (sample == null) {
            return;
        }

        samples.add(new TimedSample(Math.max(0f, timeSeconds), BehaviorFeatureVector.from(sample)));
        trimOldSamples(timeSeconds);
    }

    public List<BehaviorFeatureVector> snapshot(float currentTimeSeconds) {
        trimOldSamples(currentTimeSeconds);
        List<BehaviorFeatureVector> result = new ArrayList<>(samples.size());
        for (TimedSample sample : samples) {
            result.add(sample.vector());
        }
        return result;
    }

    public int size() {
        return samples.size();
    }

    private void trimOldSamples(float currentTimeSeconds) {
        float cutoff = Math.max(0f, currentTimeSeconds) - WINDOW_SECONDS;
        while (!samples.isEmpty() && samples.get(0).timeSeconds() < cutoff) {
            samples.remove(0);
        }
    }

    private record TimedSample(float timeSeconds, BehaviorFeatureVector vector) {
    }
}
