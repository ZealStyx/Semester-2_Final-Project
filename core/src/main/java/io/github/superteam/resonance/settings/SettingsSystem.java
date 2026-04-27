package io.github.superteam.resonance.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * Loads and saves SettingsData to a local JSON file.
 */
public final class SettingsSystem {
    private static final String SETTINGS_PATH = "settings/player_settings.json";

    private final Json json = new Json();
    private SettingsData data;

    public SettingsData loadOrCreate() {
        SettingsData loaded = readFromDisk();
        if (loaded == null) {
            data = SettingsData.defaults();
            save();
            return data;
        }

        data = loaded;
        KeybindRegistry.ensureDefaults(data);
        return data;
    }

    public SettingsData data() {
        if (data == null) {
            data = SettingsData.defaults();
        }
        return data;
    }

    public void save() {
        if (Gdx.files == null) {
            return;
        }

        FileHandle handle = Gdx.files.local(SETTINGS_PATH);
        String payload = json.prettyPrint(data());
        handle.writeString(payload, false, "UTF-8");
    }

    private SettingsData readFromDisk() {
        if (Gdx.files == null) {
            return null;
        }

        FileHandle handle = Gdx.files.local(SETTINGS_PATH);
        if (!handle.exists()) {
            return null;
        }

        try {
            String content = handle.readString("UTF-8");
            if (content == null || content.isBlank()) {
                return null;
            }
            return json.fromJson(SettingsData.class, content);
        } catch (Exception ignored) {
            return null;
        }
    }
}
