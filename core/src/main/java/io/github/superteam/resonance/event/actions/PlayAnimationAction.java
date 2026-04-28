package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/** Stub for playing an animation on an entity or node. */
public final class PlayAnimationAction implements EventAction {
    private final String targetId;
    private final String animationName;

    public PlayAnimationAction(String targetId, String animationName) {
        this.targetId = targetId;
        this.animationName = animationName;
    }

    @Override
    public void execute(EventContext ctx) {
        if (ctx == null) return;
        // Placeholder until animation playback is wired to an actual controller.
    }
}
