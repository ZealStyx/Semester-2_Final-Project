package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Minimal checkpoint action: triggers save via SaveSystem if present. */
public final class CheckpointAction implements EventAction {
    private final String checkpointId;

    public CheckpointAction(String checkpointId) { this.checkpointId = checkpointId; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        // Placeholder until SaveSystem is threaded through EventContext.
    }
}
