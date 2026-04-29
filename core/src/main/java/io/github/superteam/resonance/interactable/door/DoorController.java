package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.interactable.Interactable;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.story.StoryGate;

/**
 * Basic door interactable with lock states and animated angle target.
 */
public final class DoorController implements Interactable {
    private static final float MAX_OPEN_ANGLE = 90f;
    private static final float MIN_OPEN_ANGLE = 0f;
    private static final float KNOB_OFFSET_X = 0.85f;
    private static final float KNOB_HEIGHT = 1.0f;
    private static final float BASE_GRAB_ANGLE_DEGREES_PER_SECOND = 120f;

    private static final float OPEN_ANGLE_DEGREES = 90f;
    private static final float MOVE_INTERPOLATION = 5f;

    private final String id;
    private final Vector3 hingePosition;
    private final float interactionRadius;
    private final DoorCreakSystem creakSystem = new DoorCreakSystem();

    private DoorLockState lockState = DoorLockState.UNLOCKED;
    private StoryGate storyGate;
    private float currentAngle;
    private float targetAngle;
    private float creakCooldownSeconds;
    private float pendingNoiseIntensity;
    private float dragResponseMultiplier = 1f;
    private float slamNoiseMultiplier = 1f;
    private float interactNoiseMultiplier = 1f;

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

    public void setDragResponseMultiplier(float multiplier) {
        dragResponseMultiplier = Math.max(0.2f, multiplier);
    }

    public void setSlamNoiseMultiplier(float multiplier) {
        slamNoiseMultiplier = Math.max(0.2f, multiplier);
    }

    public void setInteractNoiseMultiplier(float multiplier) {
        interactNoiseMultiplier = Math.max(0.2f, multiplier);
    }

    public DoorLockState lockState() {
        return lockState;
    }

    public void setStoryGate(StoryGate storyGate) {
        this.storyGate = storyGate;
    }

    @Override
    public boolean canInteract(EventContext context) {
        if (storyGate != null && !storyGate.isOpen(context)) {
            return false;
        }
        return lockState == DoorLockState.UNLOCKED;
    }

    public float currentAngle() {
        return currentAngle;
    }

    public boolean isFullyOpen() {
        return currentAngle >= MAX_OPEN_ANGLE - 0.5f;
    }

    public boolean isFullyClosed() {
        return currentAngle <= MIN_OPEN_ANGLE + 0.5f;
    }

    public void applyAngleDelta(float angleDelta) {
        currentAngle = MathUtils.clamp(currentAngle + angleDelta, MIN_OPEN_ANGLE, MAX_OPEN_ANGLE);
        targetAngle = currentAngle;
    }

    public void bargeOpen(EventContext ctx) {
        if (lockState != DoorLockState.UNLOCKED) {
            return;
        }
        currentAngle = MAX_OPEN_ANGLE;
        targetAngle = MAX_OPEN_ANGLE;
        pendingNoiseIntensity = Math.max(pendingNoiseIntensity, slamNoiseMultiplier);
        if (ctx != null && ctx.audioSystem() != null) {
            ctx.audioSystem().playSfx("audio/sfx/door/slam_impact.wav", hingePosition, ctx.playerPosition(), slamNoiseMultiplier);
        }
    }

    public Vector3 knobWorldPosition() {
        Vector3 knob = new Vector3(KNOB_OFFSET_X, KNOB_HEIGHT, 0f);
        knob.rotate(Vector3.Y, currentAngle);
        return knob.add(hingePosition);
    }

    public BoundingBox computeBargeZone(float halfWidth, float halfHeight, float halfDepth) {
        Vector3 center = knobWorldPosition().add(new Vector3(0.35f, 0f, 0f));
        return new BoundingBox(
            new Vector3(center.x - halfWidth, center.y - halfHeight, center.z - halfDepth),
            new Vector3(center.x + halfWidth, center.y + halfHeight, center.z + halfDepth)
        );
    }

    public float consumeNoiseIntensity() {
        float value = pendingNoiseIntensity;
        pendingNoiseIntensity = 0f;
        return value;
    }

    @Override
    public void onInteract(EventContext ctx) {
        if (lockState != DoorLockState.UNLOCKED) {
            if (ctx != null && ctx.audioSystem() != null) {
                ctx.audioSystem().playSound("audio/sfx/door/locked.wav", 0.65f);
            }
            pendingNoiseIntensity = Math.max(pendingNoiseIntensity, 0.22f);
            return;
        }

        targetAngle = currentAngle < (OPEN_ANGLE_DEGREES * 0.5f) ? OPEN_ANGLE_DEGREES : 0f;
    }

    public void update(float deltaSeconds, EventContext eventContext) {
        creakCooldownSeconds = Math.max(0f, creakCooldownSeconds - Math.max(0f, deltaSeconds));

        float previousAngle = currentAngle;
        currentAngle = MathUtils.lerpAngleDeg(currentAngle, targetAngle, Math.min(1f, Math.max(0f, deltaSeconds) * MOVE_INTERPOLATION));
        float angularSpeed = deltaSeconds > 0.0001f
            ? Math.abs(currentAngle - previousAngle) / deltaSeconds
            : 0f;

        DoorCreakSystem.CreakSample sample = creakSystem.sample(angularSpeed);
        if (sample.volume() > 0.05f && creakCooldownSeconds <= 0f && eventContext != null && eventContext.audioSystem() != null) {
            if (eventContext.playerPosition() != null) {
                eventContext.audioSystem().playSpatialSfxPitched(
                    "audio/sfx/door/creak_soft.wav",
                    hingePosition,
                    eventContext.playerPosition(),
                    sample.volume(),
                    sample.pitch()
                );
            } else {
                eventContext.audioSystem().playSound("audio/sfx/door/creak_soft.wav", sample.volume());
            }
            pendingNoiseIntensity = Math.max(pendingNoiseIntensity, sample.volume());
            creakCooldownSeconds = MathUtils.lerp(0.35f, 0.1f, MathUtils.clamp(sample.volume(), 0f, 1f));
        }
    }

    public void dragStep(float deltaSeconds, float normSpeed, EventContext ctx) {
        float effectiveSpeed = MathUtils.clamp(normSpeed * dragResponseMultiplier, 0f, 1f);
        float angleDelta = effectiveSpeed * BASE_GRAB_ANGLE_DEGREES_PER_SECOND * Math.max(0f, deltaSeconds);
        applyAngleDelta(angleDelta);
        creakSystem.updateDrag(id, effectiveSpeed, hingePosition, ctx == null ? null : ctx.playerPosition(), deltaSeconds, ctx);
        pendingNoiseIntensity = Math.max(pendingNoiseIntensity, effectiveSpeed * interactNoiseMultiplier);
    }

    public void updateDrag(float deltaSeconds, float normSpeed, EventContext ctx) {
        dragStep(deltaSeconds, normSpeed, ctx);
    }
}
