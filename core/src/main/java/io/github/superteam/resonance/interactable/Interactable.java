package io.github.superteam.resonance.interactable;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.event.EventContext;

/**
 * Contract for objects that can be targeted and interacted with by the player.
 */
public interface Interactable {
    String id();

    Vector3 worldPosition();

    float interactionRadius();

    default boolean canInteract(EventContext context) {
        return true;
    }

    void onInteract(EventContext context);
}
