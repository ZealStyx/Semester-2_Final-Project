package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;

/**
 * Emits player locomotion sound events based on movement state and grounded state.
 * This class owns cadence timing; PlayerController remains movement-only.
 */
public final class PlayerFootstepSoundEmitter {
    private static final float RUN_INTERVAL_SECONDS = 0.30f;
    private static final float WALK_INTERVAL_SECONDS = 0.55f;
    private static final float SLOW_WALK_INTERVAL_SECONDS = 0.90f;
    private static final float CROUCH_INTERVAL_SECONDS = 1.10f;

    private static final float RUN_INTENSITY = 0.60f;
    private static final float WALK_INTENSITY = 0.30f;
    private static final float SLOW_WALK_INTENSITY = 0.10f;
    private static final float CROUCH_INTENSITY = 0.05f;

    @FunctionalInterface
    public interface NodeIdResolver {
        String resolveNodeId(Vector3 worldPosition);
    }

    private final PlayerController playerController;
    private final SoundPropagationOrchestrator soundPropagationOrchestrator;
    private final NodeIdResolver nodeIdResolver;
    private final Vector3 worldPosition = new Vector3();

    private MovementState previousState = MovementState.IDLE;
    private float accumulatorSeconds;

    public PlayerFootstepSoundEmitter(
        PlayerController playerController,
        SoundPropagationOrchestrator soundPropagationOrchestrator,
        NodeIdResolver nodeIdResolver
    ) {
        this.playerController = playerController;
        this.soundPropagationOrchestrator = soundPropagationOrchestrator;
        this.nodeIdResolver = nodeIdResolver;
    }

    public void update(float deltaSeconds, float nowSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        MovementState currentState = playerController.getMovementState();

        if (currentState != previousState) {
            accumulatorSeconds = 0f;
            previousState = currentState;
        }

        if (!playerController.isGrounded() || currentState == MovementState.IDLE) {
            accumulatorSeconds = 0f;
            return;
        }

        float intervalSeconds = intervalFor(currentState);
        accumulatorSeconds += clampedDelta;
        if (accumulatorSeconds < intervalSeconds) {
            return;
        }

        accumulatorSeconds -= intervalSeconds;
        emitFootstep(currentState, nowSeconds);
    }

    private void emitFootstep(MovementState state, float nowSeconds) {
        playerController.getPosition(worldPosition);
        String sourceNodeId = nodeIdResolver.resolveNodeId(worldPosition);
        SoundEvent eventType = state == MovementState.RUN ? SoundEvent.FOOTSTEP_RUN : SoundEvent.FOOTSTEP;

        SoundEventData soundEventData = new SoundEventData(
            eventType,
            sourceNodeId,
            worldPosition,
            intensityFor(state),
            nowSeconds
        );
        soundPropagationOrchestrator.emitSoundEvent(soundEventData, nowSeconds);
    }

    private float intervalFor(MovementState state) {
        return switch (state) {
            case RUN -> RUN_INTERVAL_SECONDS;
            case WALK -> WALK_INTERVAL_SECONDS;
            case SLOW_WALK -> SLOW_WALK_INTERVAL_SECONDS;
            case CROUCH -> CROUCH_INTERVAL_SECONDS;
            default -> Float.POSITIVE_INFINITY;
        };
    }

    private float intensityFor(MovementState state) {
        return switch (state) {
            case RUN -> RUN_INTENSITY;
            case WALK -> WALK_INTENSITY;
            case SLOW_WALK -> SLOW_WALK_INTENSITY;
            case CROUCH -> CROUCH_INTENSITY;
            default -> 0f;
        };
    }
}
