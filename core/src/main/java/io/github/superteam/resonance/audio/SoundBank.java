package io.github.superteam.resonance.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches sound and music assets for runtime playback.
 */
public final class SoundBank implements Disposable {
    private final Map<String, Sound> sounds = new HashMap<>();
    private final Map<String, Music> tracks = new HashMap<>();

    public Sound sound(String path) {
        Sound cached = sounds.get(path);
        if (cached != null) {
            return cached;
        }

        FileHandle fileHandle = Gdx.files.internal(path);
        Sound loaded = Gdx.audio.newSound(fileHandle);
        sounds.put(path, loaded);
        return loaded;
    }

    public Music music(String path) {
        Music cached = tracks.get(path);
        if (cached != null) {
            return cached;
        }

        FileHandle fileHandle = Gdx.files.internal(path);
        Music loaded = Gdx.audio.newMusic(fileHandle);
        tracks.put(path, loaded);
        return loaded;
    }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();

        for (Music music : tracks.values()) {
            music.dispose();
        }
        tracks.clear();
    }
}