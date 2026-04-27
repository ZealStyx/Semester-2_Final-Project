package io.github.superteam.resonance.event;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;

/**
 * Shared runtime context supplied to event actions.
 */
public final class EventContext {
    private final Vector3 triggerPosition;
    private final Vector3 playerPosition;
    private final float elapsedSeconds;
    private final SoundPropagationOrchestrator propagationOrchestrator;
    private final GameAudioSystem audioSystem;
    private final EventBus eventBus;
    private final EventState eventState;

    public EventContext(
        Vector3 triggerPosition,
        Vector3 playerPosition,
        float elapsedSeconds,
        SoundPropagationOrchestrator propagationOrchestrator,
        GameAudioSystem audioSystem,
        EventBus eventBus,
        EventState eventState
    ) {
        this.triggerPosition = triggerPosition == null ? new Vector3() : new Vector3(triggerPosition);
        this.playerPosition = playerPosition == null ? new Vector3() : new Vector3(playerPosition);
        this.elapsedSeconds = Math.max(0f, elapsedSeconds);
        this.propagationOrchestrator = propagationOrchestrator;
        this.audioSystem = audioSystem;
        this.eventBus = eventBus;
        this.eventState = eventState;
    }

    public Vector3 triggerPosition() {
        return new Vector3(triggerPosition);
    }

    public Vector3 playerPosition() {
        return new Vector3(playerPosition);
    }

    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    public SoundPropagationOrchestrator propagationOrchestrator() {
        return propagationOrchestrator;
    }

    public GameAudioSystem audioSystem() {
        return audioSystem;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public EventState eventState() {
        return eventState;
    }
}