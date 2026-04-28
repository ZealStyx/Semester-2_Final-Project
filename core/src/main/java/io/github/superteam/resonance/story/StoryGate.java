package io.github.superteam.resonance.story;

import io.github.superteam.resonance.event.EventContext;

/** Very small StoryGate helper used by interactables and triggers. */
public final class StoryGate {
    private final String requiredChapterId;
    private final String requiredBeatId;
    private final String blockedMessage;

    public StoryGate(String chapterId, String beatId, String blockedMessage) {
        this.requiredChapterId = chapterId;
        this.requiredBeatId = beatId;
        this.blockedMessage = blockedMessage;
    }

    public boolean isOpen(EventContext ctx) {
        StorySystem ss = ctx.storySystem();
        if (ss == null) return true;
        boolean ok = ss.hasReached(requiredChapterId, requiredBeatId);
        if (!ok && blockedMessage != null && ctx != null) ctx.dialogueSystem().showSubtitle(blockedMessage, 2.5f);
        return ok;
    }
}
