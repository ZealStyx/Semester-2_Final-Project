package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Minimal StoryEventAction — notifies StorySystem when fired. */
public final class StoryEventAction implements EventAction {
    private final String beatId;

    public StoryEventAction(String beatId) { this.beatId = beatId; }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        io.github.superteam.resonance.story.StorySystem ss = ctx.storySystem();
        if (ss != null) ss.onEventFired(beatId, ctx);
    }
}
