package io.github.superteam.resonance.event.actions;

import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/**
 * Displays a short HUD subtitle message.
 */
public final class ShowSubtitleAction implements EventAction {
    private final String text;
    private final float durationSeconds;

    public ShowSubtitleAction(String text, float durationSeconds) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Subtitle text must not be blank.");
        }
        this.text = text;
        this.durationSeconds = Math.max(0.1f, durationSeconds);
    }

    @Override
    public void execute(EventContext context) {
        if (context == null) {
            return;
        }
        context.runWithSequenceDelay(() -> context.showSubtitle(text, durationSeconds));
    }
}
