package io.github.superteam.resonance.event;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import io.github.superteam.resonance.audio.GameAudioSystem;
import io.github.superteam.resonance.player.InventorySystem;
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
    // Optional systems (nullable)
    private final io.github.superteam.resonance.sanity.SanitySystem sanitySystem;
    private final io.github.superteam.resonance.scare.JumpScareDirector jumpScareDirector;
    private final io.github.superteam.resonance.dialogue.DialogueSystem dialogueSystem;
    private final io.github.superteam.resonance.debug.DebugConsole debugConsole;
    private final io.github.superteam.resonance.story.StorySystem storySystem;
    private final InventorySystem inventorySystem;
    private final io.github.superteam.resonance.lighting.FlashlightController flashlightController;
    private final io.github.superteam.resonance.behavior.BehaviorSystem behaviorSystem;
    private final io.github.superteam.resonance.transition.RoomTransitionSystem roomTransitionSystem;

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
        this(triggerPosition, playerPosition, elapsedSeconds, propagationOrchestrator, audioSystem, eventBus, eventState, subtitleSink,
            null, null, null, null, null, null, null, null, null);
    }

    public EventContext(
        Vector3 triggerPosition,
        Vector3 playerPosition,
        float elapsedSeconds,
        SoundPropagationOrchestrator propagationOrchestrator,
        GameAudioSystem audioSystem,
        EventBus eventBus,
        EventState eventState,
        BiConsumer<String, Float> subtitleSink,
        io.github.superteam.resonance.sanity.SanitySystem sanitySystem,
        io.github.superteam.resonance.scare.JumpScareDirector jumpScareDirector,
        io.github.superteam.resonance.dialogue.DialogueSystem dialogueSystem,
        io.github.superteam.resonance.debug.DebugConsole debugConsole,
        io.github.superteam.resonance.story.StorySystem storySystem,
        InventorySystem inventorySystem,
        io.github.superteam.resonance.lighting.FlashlightController flashlightController,
        io.github.superteam.resonance.behavior.BehaviorSystem behaviorSystem,
        io.github.superteam.resonance.transition.RoomTransitionSystem roomTransitionSystem
    ) {
        this.triggerPosition = triggerPosition == null ? new Vector3() : new Vector3(triggerPosition);
        this.playerPosition = playerPosition == null ? new Vector3() : new Vector3(playerPosition);
        this.elapsedSeconds = Math.max(0f, elapsedSeconds);
        this.propagationOrchestrator = propagationOrchestrator;
        this.audioSystem = audioSystem;
        this.eventBus = eventBus;
        this.eventState = eventState;
        this.subtitleSink = subtitleSink;
        this.sanitySystem = sanitySystem;
        this.jumpScareDirector = jumpScareDirector;
        this.dialogueSystem = dialogueSystem;
        this.debugConsole = debugConsole;
        this.storySystem = storySystem;
        this.inventorySystem = inventorySystem;
        this.flashlightController = flashlightController;
        this.behaviorSystem = behaviorSystem;
        this.roomTransitionSystem = roomTransitionSystem;
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
            subtitleSink,
            sanitySystem,
            jumpScareDirector,
            dialogueSystem,
            debugConsole,
            storySystem,
            inventorySystem,
            flashlightController,
            behaviorSystem,
            roomTransitionSystem
        );
    }

    // Nullable system accessors — added so EventActions can reference systems
    public io.github.superteam.resonance.sanity.SanitySystem sanitySystem() { return sanitySystem; }
    public io.github.superteam.resonance.scare.JumpScareDirector jumpScareDirector() { return jumpScareDirector; }
    public io.github.superteam.resonance.dialogue.DialogueSystem dialogueSystem() { return dialogueSystem; }
    public io.github.superteam.resonance.debug.DebugConsole debugConsole() { return debugConsole; }
    public io.github.superteam.resonance.story.StorySystem storySystem() { return storySystem; }
    public InventorySystem inventorySystem() { return inventorySystem; }
    public io.github.superteam.resonance.lighting.FlashlightController flashlightController() { return flashlightController; }
    public io.github.superteam.resonance.behavior.BehaviorSystem behaviorSystem() { return behaviorSystem; }
    public io.github.superteam.resonance.transition.RoomTransitionSystem roomTransitionSystem() { return roomTransitionSystem; }
}