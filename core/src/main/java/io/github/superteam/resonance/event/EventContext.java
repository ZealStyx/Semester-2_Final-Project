package io.github.superteam.resonance.event;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import java.util.function.BiConsumer;

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
    private final BiConsumer<String, Float> subtitleSink;
    private float sequenceDelaySeconds;

    public EventContext(
        Vector3 triggerPosition,
        Vector3 playerPosition,
        float elapsedSeconds,
        SoundPropagationOrchestrator propagationOrchestrator,
        GameAudioSystem audioSystem,
        EventBus eventBus,
        EventState eventState,
        BiConsumer<String, Float> subtitleSink
    ) {
        this.triggerPosition = triggerPosition == null ? new Vector3() : new Vector3(triggerPosition);
        this.playerPosition = playerPosition == null ? new Vector3() : new Vector3(playerPosition);
        this.elapsedSeconds = Math.max(0f, elapsedSeconds);
        this.propagationOrchestrator = propagationOrchestrator;
        this.audioSystem = audioSystem;
        this.eventBus = eventBus;
        this.eventState = eventState;
        this.subtitleSink = subtitleSink;
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

    public void addSequenceDelay(float delaySeconds) {
        sequenceDelaySeconds += Math.max(0f, delaySeconds);
    }

    public float sequenceDelaySeconds() {
        return Math.max(0f, sequenceDelaySeconds);
    }

    public void runWithSequenceDelay(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        float delay = sequenceDelaySeconds();
        if (delay <= 0f) {
            runnable.run();
            return;
        }

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
    }

    public void showSubtitle(String text, float durationSeconds) {
        if (subtitleSink == null || text == null || text.isBlank()) {
            return;
        }
        subtitleSink.accept(text, Math.max(0.1f, durationSeconds));
    }

    public EventContext withTriggerPosition(Vector3 newTriggerPosition) {
        return new EventContext(
            newTriggerPosition,
            playerPosition,
            elapsedSeconds,
            propagationOrchestrator,
            audioSystem,
            eventBus,
            eventState,
            subtitleSink
        );
    }
}