package io.github.superteam.resonance.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

/**
 * Minimal WAV playback facade for future event-driven audio actions.
 */
public final class GameAudioSystem implements Disposable {
    private final SoundBank soundBank = new SoundBank();
    private Music currentMusic;

    public long playSound(String path, float volume) {
        Sound sound = soundBank.sound(path);
        return sound.play(Math.max(0f, volume));
    }

    public void playMusic(String path, boolean looping, float volume) {
        if (currentMusic != null) {
            currentMusic.stop();
        }

        currentMusic = soundBank.music(path);
        currentMusic.setLooping(looping);
        currentMusic.setVolume(Math.max(0f, volume));
        currentMusic.play();
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    @Override
    public void dispose() {
        stopMusic();
        soundBank.dispose();
    }
}