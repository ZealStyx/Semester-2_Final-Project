package io.github.superteam.resonance.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Session-scoped event flags and fired-state.
 */
public final class EventState {
    private final Set<String> firedEventIds = new HashSet<>();
    private final Map<String, Boolean> flags = new HashMap<>();

    public boolean hasFired(String eventId) {
        return eventId != null && firedEventIds.contains(eventId);
    }

    public void recordFired(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            firedEventIds.add(eventId);
        }
    }

    public boolean getFlag(String flagName) {
        return flagName != null && flags.getOrDefault(flagName, false);
    }

    public void setFlag(String flagName, boolean value) {
        if (flagName != null && !flagName.isBlank()) {
            flags.put(flagName, value);
        }
    }

    public void clear() {
        firedEventIds.clear();
        flags.clear();
    }
}