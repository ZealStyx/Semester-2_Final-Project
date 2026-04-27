package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.event.EventState;

/**
 * Fires another event through EventBus, preserving the single dispatch path.
 */
public final class FireEventAction implements EventAction {
    private final String targetEventId;

    public FireEventAction(String targetEventId) {
        if (targetEventId == null || targetEventId.isBlank()) {
            throw new IllegalArgumentException("Target event id must not be blank.");
        }
        this.targetEventId = targetEventId;
    }

    @Override
    public void execute(EventContext context) {
        if (context == null || context.eventBus() == null) {
            return;
        }

        context.runWithSequenceDelay(() -> {
            EventState state = context.eventState();
            EventContext childContext = context.withTriggerPosition(context.triggerPosition());
            context.eventBus().fire(targetEventId, childContext, state);
        });
    }
}
