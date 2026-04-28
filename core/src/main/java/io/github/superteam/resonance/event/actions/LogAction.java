package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Simple logging action. */
public final class LogAction implements EventAction {
    private final String message;

    public LogAction(String message) { this.message = message; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        // Placeholder: intentionally no-op to avoid noisy stdout on the render thread.
    }
}
