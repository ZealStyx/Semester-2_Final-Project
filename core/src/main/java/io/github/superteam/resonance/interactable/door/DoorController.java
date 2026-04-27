package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.interactable.Interactable;

/**
 * Basic door interactable with lock states and animated angle target.
 */
public final class DoorController implements Interactable {
    private static final float OPEN_ANGLE_DEGREES = 90f;
    private static final float MOVE_SPEED_DEGREES_PER_SECOND = 120f;

    private final String id;
    private final Vector3 hingePosition;
    private final float interactionRadius;
    private final DoorCreakSystem creakSystem = new DoorCreakSystem();

    private DoorLockState lockState = DoorLockState.UNLOCKED;
    private float currentAngle;
    private float targetAngle;

    public DoorController(String id, Vector3 hingePosition, float interactionRadius) {
        this.id = id == null ? "door" : id;
        this.hingePosition = hingePosition == null ? new Vector3() : new Vector3(hingePosition);
        this.interactionRadius = Math.max(0.2f, interactionRadius);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Vector3 worldPosition() {
        return new Vector3(hingePosition);
    }

    @Override
    public float interactionRadius() {
        return interactionRadius;
    }

    public void setLockState(DoorLockState lockState) {
        this.lockState = lockState == null ? DoorLockState.UNLOCKED : lockState;
    }

    public DoorLockState lockState() {
        return lockState;
    }

    public float currentAngle() {
        return currentAngle;
    }

    @Override
    public void onInteract(EventContext ctx) {
        if (lockState != DoorLockState.UNLOCKED) {
            if (ctx != null && ctx.audioSystem() != null) {
                ctx.audioSystem().playSound("audio/sfx/door/locked.wav", 0.65f);
            }
            return;
        }

        targetAngle = currentAngle < (OPEN_ANGLE_DEGREES * 0.5f) ? OPEN_ANGLE_DEGREES : 0f;
    }

    public void update(float deltaSeconds, EventContext eventContext) {
        float previousAngle = currentAngle;
        currentAngle = MathUtils.lerpAngleDeg(currentAngle, targetAngle, Math.min(1f, deltaSeconds * 5f));
        float angularSpeed = Math.abs(currentAngle - previousAngle) * MOVE_SPEED_DEGREES_PER_SECOND;

        DoorCreakSystem.CreakSample sample = creakSystem.sample(angularSpeed);
        if (sample.volume() > 0.05f && eventContext != null && eventContext.audioSystem() != null) {
            eventContext.audioSystem().playSound("audio/sfx/door/creak_soft.wav", sample.volume());
        }
    }
}
