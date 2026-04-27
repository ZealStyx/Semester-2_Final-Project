package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/**
 * Adds delay before subsequent actions in the same event chain.
 */
public final class WaitAction implements EventAction {
    private final float delaySeconds;

    public WaitAction(float delaySeconds) {
        this.delaySeconds = Math.max(0f, delaySeconds);
    }

    @Override
    public void execute(EventContext context) {
        if (context == null) {
            return;
        }
        context.addSequenceDelay(delaySeconds);
    }
}
