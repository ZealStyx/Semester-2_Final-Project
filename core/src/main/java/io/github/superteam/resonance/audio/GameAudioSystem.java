package io.github.superteam.resonance.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * Minimal WAV playback facade for future event-driven audio actions.
 */
public final class GameAudioSystem implements Disposable {
    private final SoundBank soundBank = new SoundBank();
    private final AudioChannelConfig channelConfig = new AudioChannelConfig();
    private final AmbienceTrack ambienceTrack = new AmbienceTrack(soundBank);
    private Music currentMusic;

    public long playSound(String path, float volume) {
        Sound sound = soundBank.sound(path);
        float finalVolume = Math.max(0f, volume)
            * channelConfig.volumeFor(AudioChannel.MASTER)
            * channelConfig.volumeFor(AudioChannel.SFX);
        return sound.play(finalVolume);
    }

    public long playSfx(String path, Vector3 emitterPosition, Vector3 listenerPosition, float volume) {
        if (emitterPosition == null || listenerPosition == null) {
            return playSound(path, volume);
        }

        float distance = emitterPosition.dst(listenerPosition);
        float attenuation = Math.max(0f, 1f - (distance / 30f));
        float pan = MathUtils.clamp((emitterPosition.x - listenerPosition.x) / 8f, -1f, 1f);
        float finalVolume = Math.max(0f, volume)
            * attenuation * attenuation
            * channelConfig.volumeFor(AudioChannel.MASTER)
            * channelConfig.volumeFor(AudioChannel.SFX);
        return soundBank.sound(path).play(finalVolume, 1f, pan);
    }

    public void playMusic(String path, boolean looping, float volume) {
        if (currentMusic != null) {
            currentMusic.stop();
        }

        currentMusic = soundBank.music(path);
        currentMusic.setLooping(looping);
        currentMusic.setVolume(Math.max(0f, volume)
            * channelConfig.volumeFor(AudioChannel.MASTER)
            * channelConfig.volumeFor(AudioChannel.MUSIC));
        currentMusic.play();
    }

    public void setAmbience(String path, float crossfadeSeconds) {
        ambienceTrack.setTargetVolume(
            channelConfig.volumeFor(AudioChannel.MASTER)
                * channelConfig.volumeFor(AudioChannel.AMBIENCE)
        );
        ambienceTrack.crossfadeTo(path, crossfadeSeconds);
    }

    public void update(float deltaSeconds) {
        ambienceTrack.setTargetVolume(
            channelConfig.volumeFor(AudioChannel.MASTER)
                * channelConfig.volumeFor(AudioChannel.AMBIENCE)
        );
        ambienceTrack.update(deltaSeconds);
    }

    public AudioChannelConfig channelConfig() {
        return channelConfig;
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    @Override
    public void dispose() {
        ambienceTrack.stopAll();
        stopMusic();
        soundBank.dispose();
    }
}