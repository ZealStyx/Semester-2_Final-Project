package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tracks temporary sonar node reveals spawned from propagation results.
 */
public final class SonarRenderer {
    private final List<SonarReveal> activeReveals = new ArrayList<>();

    public void spawnFromPropagation(
        SoundEventData soundEventData,
        PropagationResult propagationResult,
        SoundBalancingConfig soundBalancingConfig
    ) {
        Objects.requireNonNull(soundEventData, "Sound event data must not be null.");
        Objects.requireNonNull(propagationResult, "Propagation result must not be null.");
        Objects.requireNonNull(soundBalancingConfig, "Sound balancing config must not be null.");

        float lifetime = soundBalancingConfig.tuningFor(soundEventData.eventType()).pulseLifetimeSeconds();
        for (String nodeId : propagationResult.revealNodeIds()) {
            float intensity = propagationResult.getIntensityOrZero(nodeId);
            activeReveals.add(new SonarReveal(nodeId, intensity, lifetime));
        }
    }

    public void spawnReflectionReveal(String nodeId, float intensity, int bounceDepth) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }
        float depthScaledIntensity = Math.max(0f, intensity) * (1f / Math.max(1, bounceDepth));
        float lifetime = Math.max(0.1f, 0.35f * (1f / Math.max(1, bounceDepth)));
        activeReveals.add(new SonarReveal(nodeId, depthScaledIntensity, lifetime));
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        activeReveals.removeIf(reveal -> reveal.advance(clampedDelta));
    }

    public List<SonarRevealView> snapshot() {
        List<SonarRevealView> reveals = new ArrayList<>(activeReveals.size());
        for (SonarReveal reveal : activeReveals) {
            reveals.add(new SonarRevealView(reveal.nodeId, reveal.intensity, reveal.remainingLifetimeSeconds, reveal.initialLifetimeSeconds));
        }
        return Collections.unmodifiableList(reveals);
    }

    private static final class SonarReveal {
        private final String nodeId;
        private final float intensity;
        private final float initialLifetimeSeconds;
        private float remainingLifetimeSeconds;

        private SonarReveal(String nodeId, float intensity, float lifetimeSeconds) {
            this.nodeId = nodeId;
            this.intensity = intensity;
            this.initialLifetimeSeconds = lifetimeSeconds;
            this.remainingLifetimeSeconds = lifetimeSeconds;
        }

        private boolean advance(float deltaSeconds) {
            remainingLifetimeSeconds -= deltaSeconds;
            return remainingLifetimeSeconds <= 0f;
        }
    }

    public record SonarRevealView(String nodeId, float intensity, float remainingLifetimeSeconds, float initialLifetimeSeconds) {
        public float alpha() {
            if (initialLifetimeSeconds <= 0f) {
                return 0f;
            }
            return Math.max(0f, Math.min(1f, remainingLifetimeSeconds / initialLifetimeSeconds));
        }
    }
}
