package io.github.superteam.resonance.interaction;

import com.badlogic.gdx.Input;
import io.github.superteam.resonance.interactable.Interactable;
import io.github.superteam.resonance.interactable.door.DoorController;

/**
 * Lightweight prompt string formatter for the currently focused interactable.
 */
public final class InteractionPromptRenderer {
    public String formatPrompt(InteractionResult result) {
        if (result == null || !result.hasTarget()) {
            return "";
        }

        Interactable target = result.target();
        String label = target.id() == null || target.id().isBlank() ? "object" : target.id();
        if (!result.canInteract()) {
            return "Unavailable: " + label;
        }
        // Door knob-first flow uses mouse drag (LMB) rather than the interact key.
        if (target instanceof DoorController) {
            return "Hold [LMB] and drag to open: " + label;
        }
        String keyLabel = result.interactKeycode() <= 0 ? "F" : Input.Keys.toString(result.interactKeycode());
        return "Press [" + keyLabel + "] to interact: " + label;
    }
}
