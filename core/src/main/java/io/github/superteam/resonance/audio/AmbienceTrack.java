package io.github.superteam.resonance.audio;

import com.badlogic.gdx.audio.Music;

/**
 * Handles a single ambience bed with optional crossfade transitions.
 */
public final class AmbienceTrack {
    private final SoundBank soundBank;

    private Music currentTrack;
    private Music nextTrack;
    private float crossfadeRemaining;
    private float crossfadeDuration;
    private float targetVolume = 1f;

    public AmbienceTrack(SoundBank soundBank) {
        this.soundBank = soundBank;
    }

    public void setTargetVolume(float targetVolume) {
        this.targetVolume = Math.max(0f, targetVolume);
        if (currentTrack != null && nextTrack == null) {
            currentTrack.setVolume(this.targetVolume);
        }
    }

    public void play(String path, boolean loop) {
        stopAll();
        currentTrack = soundBank.music(path);
        currentTrack.setLooping(loop);
        currentTrack.setVolume(targetVolume);
        currentTrack.play();
    }

    public void crossfadeTo(String path, float durationSeconds) {
        if (path == null || path.isBlank()) {
            return;
        }

        float duration = Math.max(0.01f, durationSeconds);
        Music incoming = soundBank.music(path);
        incoming.setLooping(true);
        incoming.setVolume(0f);
        incoming.play();

        if (currentTrack == null) {
            currentTrack = incoming;
            incoming.setVolume(targetVolume);
            return;
        }

        nextTrack = incoming;
        crossfadeDuration = duration;
        crossfadeRemaining = duration;
    }

    public void update(float deltaSeconds) {
        if (nextTrack == null) {
            return;
        }

        crossfadeRemaining = Math.max(0f, crossfadeRemaining - Math.max(0f, deltaSeconds));
        float progress = 1f - (crossfadeRemaining / crossfadeDuration);
        float inVolume = targetVolume * progress;
        float outVolume = targetVolume * (1f - progress);

        if (currentTrack != null) {
            currentTrack.setVolume(outVolume);
        }
        nextTrack.setVolume(inVolume);

        if (crossfadeRemaining <= 0f) {
            if (currentTrack != null) {
                currentTrack.stop();
            }
            currentTrack = nextTrack;
            nextTrack = null;
        }
    }

    public void stopAll() {
        if (currentTrack != null) {
            currentTrack.stop();
        }
        if (nextTrack != null) {
            nextTrack.stop();
        }
        currentTrack = null;
        nextTrack = null;
        crossfadeRemaining = 0f;
        crossfadeDuration = 0f;
    }
}
