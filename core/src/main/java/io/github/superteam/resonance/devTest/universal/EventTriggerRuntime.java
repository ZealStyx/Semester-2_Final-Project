package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.event.EventBus;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.event.EventLoader;
import io.github.superteam.resonance.event.EventState;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import io.github.superteam.resonance.trigger.TriggerEvaluationContext;
import io.github.superteam.resonance.trigger.TriggerEvaluator;
import io.github.superteam.resonance.trigger.TriggerLoader;
import java.util.function.BiConsumer;

/**
 * Shared trigger and event runtime for test scenes.
 */
public final class EventTriggerRuntime {
    private final EventState eventState = new EventState();
    private final EventBus eventBus;
    private final TriggerEvaluator triggerEvaluator;
    private final GameAudioSystem eventAudioSystem;
    private final BiConsumer<String, Float> subtitleSink;

    private EventTriggerRuntime(
        EventBus eventBus,
        TriggerEvaluator triggerEvaluator,
        GameAudioSystem eventAudioSystem,
        BiConsumer<String, Float> subtitleSink
    ) {
        this.eventBus = eventBus;
        this.triggerEvaluator = triggerEvaluator;
        this.eventAudioSystem = eventAudioSystem;
        this.subtitleSink = subtitleSink;
    }

    public static EventTriggerRuntime loadDefaults(
        String eventConfigPath,
        String triggerConfigPath,
        BiConsumer<String, Float> subtitleSink
    ) {
        return new EventTriggerRuntime(
            EventLoader.loadOrDefault(eventConfigPath),
            TriggerLoader.loadOrDefault(triggerConfigPath),
            new GameAudioSystem(),
            subtitleSink
        );
    }

    public void update(
        float deltaSeconds,
        Vector3 playerPosition,
        float elapsedSeconds,
        SoundPropagationOrchestrator propagationOrchestrator
    ) {
        if (eventAudioSystem != null) {
            eventAudioSystem.update(deltaSeconds);
        }
        if (eventBus != null) {
            eventBus.update(deltaSeconds);
        }
        if (triggerEvaluator == null || playerPosition == null) {
            return;
        }

        TriggerEvaluationContext triggerContext = new TriggerEvaluationContext(playerPosition, elapsedSeconds, eventState);
        triggerEvaluator.update(deltaSeconds, triggerContext,
            (eventId, triggerPosition) -> fireEvent(eventId, triggerPosition, playerPosition, elapsedSeconds, propagationOrchestrator));
    }

    public void fireEvent(
        String eventId,
        Vector3 triggerPosition,
        Vector3 playerPosition,
        float elapsedSeconds,
        SoundPropagationOrchestrator propagationOrchestrator
    ) {
        if (eventBus == null || eventId == null || eventId.isBlank()) {
            return;
        }

        Vector3 resolvedPlayer = playerPosition == null ? new Vector3() : playerPosition;
        Vector3 resolvedTrigger = triggerPosition == null ? resolvedPlayer : triggerPosition;
        EventContext context = new EventContext(
            resolvedTrigger,
            resolvedPlayer,
            elapsedSeconds,
            propagationOrchestrator,
            eventAudioSystem,
            eventBus,
            eventState,
            subtitleSink
        );
        eventBus.fire(eventId, context, eventState);
    }

    public boolean flag(String flagName) {
        return eventState.getFlag(flagName);
    }

    public void dispose() {
        if (eventAudioSystem != null) {
            eventAudioSystem.dispose();
        }
    }
}
