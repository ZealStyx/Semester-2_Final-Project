package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/**
 * Sets a session flag from an event.
 */
public final class SetFlagAction implements EventAction {
    private final String flagName;
    private final boolean value;

    public SetFlagAction(String flagName, boolean value) {
        if (flagName == null || flagName.isBlank()) {
            throw new IllegalArgumentException("Flag name must not be blank.");
        }
        this.flagName = flagName;
        this.value = value;
    }

    @Override
    public void execute(EventContext context) {
        if (context == null || context.eventState() == null) {
            return;
        }
        context.runWithSequenceDelay(() -> context.eventState().setFlag(flagName, value));
    }
}