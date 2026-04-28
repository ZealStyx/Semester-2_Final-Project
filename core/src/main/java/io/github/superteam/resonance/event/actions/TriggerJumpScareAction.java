package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Action that asks JumpScareDirector to trigger a scare. */
public final class TriggerJumpScareAction implements EventAction {
    private final String scareId;

    public TriggerJumpScareAction(String scareId) { this.scareId = scareId; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        var director = ctx.jumpScareDirector();
        if (director == null) return;
    }
}
