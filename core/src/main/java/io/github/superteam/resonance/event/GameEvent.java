package io.github.superteam.resonance.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definition of a runtime event and its actions.
 */
public final class GameEvent {
    public enum RepeatMode {
        ONCE,
        REPEATING,
        ONCE_PER_SESSION
    }

    private final String id;
    private final String displayName;
    private final List<EventAction> actions;
    private final RepeatMode repeatMode;
    private final float cooldownSeconds;
    private float cooldownRemaining;
    private boolean hasEverFired;

    public GameEvent(String id, String displayName, List<EventAction> actions, RepeatMode repeatMode, float cooldownSeconds) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Event id must not be blank.");
        }
        this.id = id;
        this.displayName = displayName == null ? id : displayName;
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions == null ? List.of() : actions));
        this.repeatMode = repeatMode == null ? RepeatMode.REPEATING : repeatMode;
        this.cooldownSeconds = Math.max(0f, cooldownSeconds);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<EventAction> actions() {
        return actions;
    }

    public RepeatMode repeatMode() {
        return repeatMode;
    }

    public float cooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean canFire(EventState eventState) {
        if (eventState == null) {
            return false;
        }
        if (repeatMode == RepeatMode.ONCE && eventState.hasFired(id)) {
            return false;
        }
        if (repeatMode == RepeatMode.ONCE_PER_SESSION && hasEverFired) {
            return false;
        }
        return cooldownRemaining <= 0f;
    }

    public void update(float deltaSeconds) {
        cooldownRemaining = Math.max(0f, cooldownRemaining - Math.max(0f, deltaSeconds));
    }

    public void fire(EventContext context, EventState eventState) {
        if (!canFire(eventState)) {
            return;
        }

        if (eventState != null) {
            eventState.recordFired(id);
        }
        hasEverFired = true;
        cooldownRemaining = cooldownSeconds;

        for (EventAction action : actions) {
            action.execute(context);
        }
    }
}