package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Stubbed action that would inject a sound into the propagation graph. */
public final class PropagateGraphAction implements EventAction {
    private final String soundEventId;

    public PropagateGraphAction(String soundEventId) { this.soundEventId = soundEventId; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        // Placeholder until sound event emission is wired into the runtime context.
    }
}
