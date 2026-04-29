package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Minimal TransitionLevelAction — triggers room transition via StorySystem or TransitionSystem when present. */
public final class TransitionLevelAction implements EventAction {
    private final String roomName;

    public TransitionLevelAction(String roomName) { this.roomName = roomName; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null || roomName == null || roomName.isBlank()) return;
        if (ctx.roomTransitionSystem() != null) {
            ctx.roomTransitionSystem().triggerRoomTransition(roomName);
        }
    }
}
