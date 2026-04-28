package io.github.superteam.resonance.interaction;

import io.github.superteam.resonance.interactable.Interactable;

/**
 * Result for a single interaction query/update tick.
 */
public final class InteractionResult {
    public static final InteractionResult NONE = new InteractionResult(null, false, 0);

    private final Interactable target;
    private final boolean canInteract;
    private final int interactKeycode;

    public InteractionResult(Interactable target, boolean canInteract, int interactKeycode) {
        this.target = target;
        this.canInteract = canInteract;
        this.interactKeycode = interactKeycode;
    }

    public Interactable target() {
        return target;
    }

    public boolean canInteract() {
        return canInteract;
    }

    public int interactKeycode() {
        return interactKeycode;
    }

    public boolean hasTarget() {
        return target != null;
    }
}
