package io.github.superteam.resonance.settings;

import com.badlogic.gdx.Input;

/**
 * Action constants and lookup helpers for runtime keybind queries.
 */
public final class KeybindRegistry {
    public static final String INTERACT = "INTERACT";
    public static final String CROUCH = "CROUCH";
    public static final String SPRINT = "SPRINT";
    public static final String FLASHLIGHT = "FLASHLIGHT";
    public static final String CONSOLE = "CONSOLE";
    public static final String HOLD_BREATH = "HOLD_BREATH";

    private final SettingsData settingsData;

    public KeybindRegistry(SettingsData settingsData) {
        this.settingsData = settingsData == null ? SettingsData.defaults() : settingsData;
        ensureDefaults(this.settingsData);
    }

    public int keyFor(String action) {
        if (action == null || action.isBlank()) {
            return Input.Keys.UNKNOWN;
        }
        Integer code = settingsData.keybinds.get(action);
        return code == null ? Input.Keys.UNKNOWN : code;
    }

    public void setKey(String action, int keycode) {
        if (action == null || action.isBlank()) {
            return;
        }
        settingsData.keybinds.put(action, keycode);
    }

    public SettingsData data() {
        return settingsData;
    }

    public static void ensureDefaults(SettingsData data) {
        if (data == null) {
            return;
        }
        if (data.keybinds == null) {
            data.keybinds = SettingsData.defaults().keybinds;
            return;
        }

        putIfMissing(data, INTERACT, Input.Keys.F);
        putIfMissing(data, CROUCH, Input.Keys.C);
        putIfMissing(data, SPRINT, Input.Keys.SHIFT_LEFT);
        putIfMissing(data, FLASHLIGHT, Input.Keys.L);
        putIfMissing(data, CONSOLE, Input.Keys.GRAVE);
        putIfMissing(data, HOLD_BREATH, Input.Keys.ALT_LEFT);
    }

    private static void putIfMissing(SettingsData data, String action, int keycode) {
        if (!data.keybinds.containsKey(action)) {
            data.keybinds.put(action, keycode);
        }
    }
}
