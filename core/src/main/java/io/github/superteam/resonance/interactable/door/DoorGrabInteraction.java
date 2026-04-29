package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.player.PlayerController;

/** Minimal grab/drag state machine for the v2 door model. */
public final class DoorGrabInteraction {
    private static final float DRAG_SENSITIVITY = 120f;
    private static final float MAX_ANGULAR_VELOCITY = 360f;
    private static final float PHYSICS_DAMPING = 240f;

    private enum GrabState { IDLE, GRABBING, COASTING }

    private GrabState state = GrabState.IDLE;
    private float angularVelocity;
    private DoorController grabbedDoor;

    public void update(float delta, float mouseXDelta, boolean lmbDown, DoorController hoveredDoor, EventContext ctx) {
        switch (state) {
            case IDLE -> {
                if (lmbDown && hoveredDoor != null && hoveredDoor.canInteract(ctx)) {
                    grabbedDoor = hoveredDoor;
                    angularVelocity = 0f;
                    state = GrabState.GRABBING;
                }
            }
            case GRABBING -> {
                if (!lmbDown || grabbedDoor == null) {
                    state = GrabState.COASTING;
                    return;
                }

                float screenNorm = mouseXDelta / Math.max(1f, Gdx.graphics.getWidth());
                angularVelocity = MathUtils.clamp(screenNorm * DRAG_SENSITIVITY / Math.max(0.0001f, delta), -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
                float normSpeed = Math.abs(angularVelocity) / MAX_ANGULAR_VELOCITY;
                grabbedDoor.dragStep(delta, normSpeed, ctx);
            }
            case COASTING -> {
                if (grabbedDoor == null) {
                    state = GrabState.IDLE;
                    return;
                }

                float sign = Math.signum(angularVelocity);
                angularVelocity -= sign * PHYSICS_DAMPING * delta;
                if (sign != Math.signum(angularVelocity)) {
                    angularVelocity = 0f;
                    state = GrabState.IDLE;
                    grabbedDoor = null;
                    return;
                }

                float normSpeed = Math.abs(angularVelocity) / MAX_ANGULAR_VELOCITY;
                grabbedDoor.dragStep(delta, normSpeed, ctx);
            }
        }
    }

    public boolean isGrabbing() {
        return state == GrabState.GRABBING;
    }

    public DoorController grabbedDoor() {
        return grabbedDoor;
    }
}