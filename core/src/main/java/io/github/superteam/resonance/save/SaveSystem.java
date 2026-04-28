package io.github.superteam.resonance.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/** Minimal SaveSystem stub — writes/reads a single save file. */
public final class SaveSystem {
    private static final String SAVE_PATH = "saves/autosave.json";
    private final Json json = new Json();

    public void save(SaveData data) {
        try {
            FileHandle fh = Gdx.files.local(SAVE_PATH);
            fh.writeString(json.toJson(data), false);
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public SaveData load() {
        try {
            FileHandle fh = Gdx.files.local(SAVE_PATH);
            if (!fh.exists()) return null;
            return json.fromJson(SaveData.class, fh.readString());
        } catch (Exception e) {
            System.err.println("Load failed: " + e.getMessage());
            return null;
        }
    }
}
