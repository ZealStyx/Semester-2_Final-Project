package io.github.superteam.resonance.event;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry and dispatcher for game events.
 */
public final class EventBus {
    private final Map<String, GameEvent> events = new LinkedHashMap<>();

    public void register(GameEvent event) {
        if (event != null) {
            events.put(event.id(), event);
        }
    }

    public GameEvent get(String eventId) {
        return events.get(eventId);
    }

    public void fire(String eventId, EventContext context, EventState eventState) {
        GameEvent event = events.get(eventId);
        if (event != null) {
            event.fire(context, eventState);
        }
    }

    public void update(float deltaSeconds) {
        for (GameEvent event : events.values()) {
            event.update(deltaSeconds);
        }
    }

    public Collection<GameEvent> all() {
        return events.values();
    }
}