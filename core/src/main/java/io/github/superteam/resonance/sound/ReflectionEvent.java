package io.github.superteam.resonance.sound;

import java.util.Objects;

/**
 * A delayed reflection propagation payload emitted from a bounce node.
 */
public final class ReflectionEvent {
    private final String bounceNodeId;
    private final float intensity;
    private final float delaySeconds;
    private final int bounceDepth;
    private final PropagationResult propagationFromBounce;

    public ReflectionEvent(
        String bounceNodeId,
        float intensity,
        float delaySeconds,
        int bounceDepth,
        PropagationResult propagationFromBounce
    ) {
        if (bounceNodeId == null || bounceNodeId.isBlank()) {
            throw new IllegalArgumentException("Bounce node id must not be blank.");
        }
        if (intensity < 0f) {
            throw new IllegalArgumentException("Reflection intensity must be non-negative.");
        }
        if (delaySeconds < 0f) {
            throw new IllegalArgumentException("Reflection delay seconds must be non-negative.");
        }
        if (bounceDepth <= 0) {
            throw new IllegalArgumentException("Bounce depth must be positive.");
        }
        this.bounceNodeId = bounceNodeId;
        this.intensity = intensity;
        this.delaySeconds = delaySeconds;
        this.bounceDepth = bounceDepth;
        this.propagationFromBounce = Objects.requireNonNull(propagationFromBounce, "Bounce propagation result must not be null.");
    }

    public String bounceNodeId() {
        return bounceNodeId;
    }

    public float intensity() {
        return intensity;
    }

    public float delaySeconds() {
        return delaySeconds;
    }

    public int bounceDepth() {
        return bounceDepth;
    }

    public PropagationResult propagationFromBounce() {
        return propagationFromBounce;
    }
}
