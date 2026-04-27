package io.github.superteam.resonance.settings;

import com.badlogic.gdx.Input;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializable player settings payload.
 */
public final class SettingsData {
    public float masterVolume = 1.0f;
    public float musicVolume = 0.8f;
    public float sfxVolume = 0.9f;
    public float voiceVolume = 0.9f;

    public float mouseSensitivity = 1.0f;
    public boolean invertY = false;

    public boolean fullscreen = false;
    public int targetFov = 75;

    public Map<String, Integer> keybinds = new LinkedHashMap<>();

    public static SettingsData defaults() {
        SettingsData data = new SettingsData();
        data.keybinds.put(KeybindRegistry.INTERACT, Input.Keys.F);
        data.keybinds.put(KeybindRegistry.CROUCH, Input.Keys.C);
        data.keybinds.put(KeybindRegistry.SPRINT, Input.Keys.SHIFT_LEFT);
        data.keybinds.put(KeybindRegistry.FLASHLIGHT, Input.Keys.L);
        data.keybinds.put(KeybindRegistry.CONSOLE, Input.Keys.GRAVE);
        data.keybinds.put(KeybindRegistry.HOLD_BREATH, Input.Keys.ALT_LEFT);
        return data;
    }
}
