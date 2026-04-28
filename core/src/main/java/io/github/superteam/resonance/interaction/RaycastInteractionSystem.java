package io.github.superteam.resonance.interaction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.interactable.Interactable;
import io.github.superteam.resonance.interactable.InteractableRegistry;

/**
 * View-direction interaction selector and trigger.
 */
public final class RaycastInteractionSystem {
    private static final float DEFAULT_MAX_DISTANCE = 2.6f;
    private static final float DEFAULT_MIN_DOT = 0.78f;

    private final InteractableRegistry interactableRegistry;
    private float maxDistance = DEFAULT_MAX_DISTANCE;
    private float minFacingDot = DEFAULT_MIN_DOT;
    private int interactKeycode = Input.Keys.F;
    private Interactable focused;

    public RaycastInteractionSystem(InteractableRegistry interactableRegistry) {
        this.interactableRegistry = interactableRegistry;
    }

    public void setInteractKeycode(int interactKeycode) {
        this.interactKeycode = interactKeycode <= 0 ? Input.Keys.F : interactKeycode;
    }

    public void setRange(float maxDistance) {
        this.maxDistance = Math.max(0.5f, maxDistance);
    }

    public Interactable focused() {
        return focused;
    }

    public InteractionResult update(Vector3 origin, Vector3 forward, EventContext eventContext) {
        if (interactableRegistry == null || origin == null || forward == null) {
            focused = null;
            return InteractionResult.NONE;
        }

        focused = interactableRegistry.findBestTarget(origin, forward, maxDistance, minFacingDot);
        if (focused == null) {
            return InteractionResult.NONE;
        }

        boolean canInteract = focused.canInteract(eventContext);
        if (Gdx.input.isKeyJustPressed(interactKeycode) && canInteract) {
            focused.onInteract(eventContext);
        }

        return new InteractionResult(focused, canInteract, interactKeycode);
    }
}
