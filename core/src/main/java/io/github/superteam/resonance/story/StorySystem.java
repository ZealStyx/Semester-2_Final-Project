package io.github.superteam.resonance.story;

/** Minimal StorySystem scaffold. */
public final class StorySystem {
    public boolean hasReached(String chapterId, String beatId) { return false; }
    public boolean isChapterActive(String chapterId) { return false; }
    public void onEventFired(String eventId, io.github.superteam.resonance.event.EventContext ctx) { }
    public void onInteraction(String interactableId, io.github.superteam.resonance.event.EventContext ctx) { }
    public void onZoneEntered(String zoneId, io.github.superteam.resonance.event.EventContext ctx) { }
    public String debugStatus() { return "story:empty"; }
}
