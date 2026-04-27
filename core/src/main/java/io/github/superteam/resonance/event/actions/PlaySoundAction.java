package io.github.superteam.resonance.event.actions;

import com.badlogic.gdx.Gdx;
import io.github.superteam.resonance.event.EventAction;
import io.github.superteam.resonance.event.EventContext;

/**
 * Plays a one-shot sound through the event audio system.
 */
public final class PlaySoundAction implements EventAction {
    private final String soundPath;
    private final float volume;

    public PlaySoundAction(String soundPath, float volume) {
        if (soundPath == null || soundPath.isBlank()) {
            throw new IllegalArgumentException("Sound path must not be blank.");
        }
        this.soundPath = soundPath;
        this.volume = Math.max(0f, volume);
    }

    @Override
    public void execute(EventContext context) {
        if (context == null || context.audioSystem() == null) {
            return;
        }
        context.runWithSequenceDelay(() -> {
            if (Gdx.files == null || !Gdx.files.internal(soundPath).exists()) {
                Gdx.app.log("EventAction", "Skipping PLAY_SOUND, missing asset: " + soundPath);
                return;
            }
            context.audioSystem().playSound(soundPath, volume);
        });
    }
}