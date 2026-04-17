package io.github.superteam.resonance.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

/**
 * Handles player interaction checks and dispatches interaction callbacks.
 *
 * Single Responsibility: F-key interaction intent + target validation.
 */
public class PlayerInteractionSystem {

    private static final float DEFAULT_INTERACTION_RANGE = 2.5f;
    private static final float MIN_FACING_DOT = 0.70f;

    private final PerspectiveCamera camera;
    private final PlayerController playerController;

    private Interactable primaryTarget;
    private final Vector3 primaryTargetPosition = new Vector3();
    private float interactionRange = DEFAULT_INTERACTION_RANGE;
    private int interactionKeyCode = Input.Keys.F;

    public PlayerInteractionSystem(PerspectiveCamera camera, PlayerController playerController) {
        this.camera = camera;
        this.playerController = playerController;
    }

    public void setPrimaryTarget(Interactable target, Vector3 targetPosition) {
        this.primaryTarget = target;
        this.primaryTargetPosition.set(targetPosition);
    }

    public void setInteractionRange(float interactionRange) {
        this.interactionRange = Math.max(0.1f, interactionRange);
    }

    public void setInteractionKeyCode(int interactionKeyCode) {
        this.interactionKeyCode = interactionKeyCode;
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(interactionKeyCode)) {
            tryInteractWithPrimaryTarget();
        }
    }

    private void tryInteractWithPrimaryTarget() {
        if (primaryTarget == null) {
            return;
        }

        Vector3 playerPos = playerController.getPosition();
        Vector3 toTarget = primaryTargetPosition.cpy().sub(playerPos);
        float distance = toTarget.len();
        if (distance > interactionRange || distance <= 0.0001f) {
            return;
        }

        toTarget.nor();
        float facingDot = camera.direction.cpy().nor().dot(toTarget);
        if (facingDot < MIN_FACING_DOT) {
            return;
        }

        primaryTarget.onInteract(playerController);
    }
}
