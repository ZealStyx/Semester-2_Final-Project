package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.Objects;

/**
 * Emits physics-driven noise events into the propagation orchestrator.
 */
public final class PhysicsNoiseEmitter {
    private static final float MIN_FORCE = 0f;
    private static final float MAX_FORCE = 1500f;

    private final SoundPropagationOrchestrator soundPropagationOrchestrator;

    public PhysicsNoiseEmitter(SoundPropagationOrchestrator soundPropagationOrchestrator) {
        this.soundPropagationOrchestrator = Objects.requireNonNull(soundPropagationOrchestrator, "Sound orchestrator must not be null.");
    }

    public PropagationResult emitPhysicsCollapse(String sourceNodeId, Vector3 worldPosition, float nowSeconds) {
        SoundEventData eventData = SoundEventData.atNode(SoundEvent.PHYSICS_COLLAPSE, sourceNodeId, worldPosition, nowSeconds);
        return soundPropagationOrchestrator.emitSoundEvent(eventData, nowSeconds);
    }

    public PropagationResult emitObjectDrop(String sourceNodeId, Vector3 worldPosition, float nowSeconds) {
        SoundEventData eventData = SoundEventData.atNode(SoundEvent.OBJECT_DROP_OR_BREAK, sourceNodeId, worldPosition, nowSeconds);
        return soundPropagationOrchestrator.emitSoundEvent(eventData, nowSeconds);
    }

    public PropagationResult emitImpact(String sourceNodeId, Vector3 worldPosition, float impactForceNewtons, float nowSeconds) {
        float clampedForce = clamp(impactForceNewtons, MIN_FORCE, MAX_FORCE);
        float normalizedForce = clampedForce / MAX_FORCE;
        SoundEvent eventType = normalizedForce >= 0.6f ? SoundEvent.PHYSICS_COLLAPSE : SoundEvent.OBJECT_HIT;
        float scaledIntensity = eventType.defaultBaseIntensity() * (0.35f + (normalizedForce * 0.65f));

        SoundEventData eventData = new SoundEventData(
            eventType,
            sourceNodeId,
            worldPosition,
            scaledIntensity,
            nowSeconds
        );
        return soundPropagationOrchestrator.emitSoundEvent(eventData, nowSeconds);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
