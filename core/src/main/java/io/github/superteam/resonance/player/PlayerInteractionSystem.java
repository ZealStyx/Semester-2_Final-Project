package io.github.superteam.resonance.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;

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
    private final List<InteractionTarget> targets = new ArrayList<>();
    private final Vector3 playerPosition = new Vector3();
    private final Vector3 toTarget = new Vector3();
    private final Vector3 cameraDirection = new Vector3();

    private float interactionRange = DEFAULT_INTERACTION_RANGE;
    private int interactionKeyCode = Input.Keys.F;

    public PlayerInteractionSystem(PerspectiveCamera camera, PlayerController playerController) {
        this.camera = camera;
        this.playerController = playerController;
    }

    public void registerTarget(Interactable target, Vector3 targetPosition) {
        if (target == null || targetPosition == null) {
            return;
        }
        targets.add(new InteractionTarget(target, targetPosition));
    }

    public void setInteractionRange(float interactionRange) {
        this.interactionRange = Math.max(0.1f, interactionRange);
    }

    public void setInteractionKeyCode(int interactionKeyCode) {
        this.interactionKeyCode = interactionKeyCode;
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(interactionKeyCode)) {
            tryInteractWithBestTarget();
        }
    }

    public boolean hasTargetInRangeAndFacing() {
        return findBestTarget() != null;
    }

    private void tryInteractWithBestTarget() {
        InteractionTarget target = findBestTarget();
        if (target == null) {
            return;
        }

        target.interactable.onInteract(playerController);
    }

    private InteractionTarget findBestTarget() {
        if (targets.isEmpty()) {
            return null;
        }

        float rangeSquared = interactionRange * interactionRange;
        float bestFacing = MIN_FACING_DOT;
        float bestDistanceSquared = Float.POSITIVE_INFINITY;
        InteractionTarget bestTarget = null;

        playerController.getPosition(playerPosition);
        cameraDirection.set(camera.direction).nor();

        for (int i = 0; i < targets.size(); i++) {
            InteractionTarget target = targets.get(i);
            toTarget.set(target.position).sub(playerPosition);
            float distanceSquared = toTarget.len2();
            if (distanceSquared <= 0.00000001f || distanceSquared > rangeSquared) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            toTarget.scl(1f / distance);
            float facingDot = cameraDirection.dot(toTarget);
            if (facingDot < MIN_FACING_DOT) {
                continue;
            }

            if (facingDot > bestFacing || (Math.abs(facingDot - bestFacing) <= 0.0001f && distanceSquared < bestDistanceSquared)) {
                bestFacing = facingDot;
                bestDistanceSquared = distanceSquared;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static final class InteractionTarget {
        private final Interactable interactable;
        private final Vector3 position = new Vector3();

        private InteractionTarget(Interactable interactable, Vector3 position) {
            this.interactable = interactable;
            this.position.set(position);
        }
    }
}
