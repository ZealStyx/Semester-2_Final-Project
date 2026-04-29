package io.github.superteam.resonance.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

/**
 * First-person player movement controller. Handles input, movement state management,
 * physics integration (gravity), and orientation. Strictly separated from audio, rendering,
 * and interaction logic.
 *
 * Core Responsibility: Movement input → state → physics → position updates
 * Single Responsibility Principle: This class ONLY handles locomotion and orientation.
 *         Audio events, interactions, features are handled by other classes.
 *
 * State Priority (enforced in order):
 *   1. Crouch (C held) — highest priority
 *   2. SlowWalk (Ctrl held)
 *   3. Run (Shift held)
 *   4. Walk (WASD held)
 *   5. Idle (no input) — lowest priority
 */
public class PlayerController {

    // ==================== Constants (Named, Not Magic) ====================

    private static final float GRAVITY = 9.8f;
    private static final float JUMP_FORCE = 5.0f;
    private static final float EYE_HEIGHT_STANDING = 1.6f;
    private static final float EYE_HEIGHT_CROUCH = 0.75f;
    private static final float CROUCH_LERP_SPEED = 4.0f;
    private static final float CEILING_CHECK_MARGIN = 0.05f;

    // Collision capsule dimensions
    public static final float CAPSULE_RADIUS = 0.3f;

    // Ceiling detection parameters
    private static final float JUMP_COOLDOWN_SECONDS = 0.26f;
    private static final float MIN_GROUNDED_TIME_BEFORE_JUMP_SECONDS = 0.12f;

    // Head bob tuning
    private static final float WALK_BOB_FREQUENCY = 7.0f;
    private static final float RUN_BOB_FREQUENCY = 10.0f;
    private static final float SLOW_WALK_BOB_FREQUENCY = 4.5f;
    private static final float WALK_BOB_AMPLITUDE = 0.03f;
    private static final float RUN_BOB_AMPLITUDE = 0.05f;
    private static final float SLOW_WALK_BOB_AMPLITUDE = 0.02f;
    private static final float HEAD_BOB_RECOVERY_SPEED = 8.0f;
    private static final float MAX_STAMINA = 100f;
    private static final float STAMINA_DRAIN_RATE = 22f;
    private static final float STAMINA_REGEN_RATE = 14f;
    private static final float STAMINA_REGEN_THRESHOLD = 25f;
    private static final float AIR_CONTROL_FACTOR = 0.08f;
    private static final float AIR_MAX_HORIZONTAL_SPEED = 3.5f;

    // ==================== Fields ====================

    private final PerspectiveCamera camera;

    // Position and velocity
    private final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();
    private final Vector3 movementForward = new Vector3();
    private final Vector3 movementRight = new Vector3();
    private final Vector3 movementDirection = new Vector3();
    private final Vector3 lookDirection = new Vector3();

    // Camera look angles
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private static final float MOUSE_SENSITIVITY = 0.15f;
    private static final float MAX_PITCH = 85.0f;

    // Movement state
    private MovementState currentState = MovementState.IDLE;
    private MovementState previousState = MovementState.IDLE;

    // Crouch state
    private boolean isCrouchPressed = false;
    private float crouchLerpT = 0.0f; // 0 = standing, 1 = crouched
    private float headBobPhase = 0.0f;
    private float headBobOffset = 0.0f;
    private float stamina = MAX_STAMINA;
    private boolean staminaDepleted = false;

    // Ground detection
    private boolean isGrounded = true;
    private boolean jumpTriggeredThisFrame;
    private float jumpCooldownRemainingSeconds;
    private float groundedTimeSeconds;
    private final Array<BoundingBox> worldColliders = new Array<>();
    private int collisionCount;

    // ==================== Constructor ====================

    /**
     * Construct a player controller bound to a camera.
     *
     * @param camera The PerspectiveCamera to control
     */
    public PlayerController(PerspectiveCamera camera) {
        this.camera = camera;
        this.position.set(camera.position);
    }

    PlayerController() {
        this.camera = null;
    }

    // ==================== Main Update ====================

    /**
     * Update the player state and position each frame.
     * Call this once per render frame from the game loop.
     *
     * @param delta Time elapsed since last frame, in seconds
     */
    public void update(float delta) {
        // Read input and update state
        updateMouseLook(delta);
        updateMovementState();
        updateStamina(delta);
        jumpTriggeredThisFrame = false;
        jumpCooldownRemainingSeconds = Math.max(0f, jumpCooldownRemainingSeconds - delta);
        groundedTimeSeconds = isGrounded ? groundedTimeSeconds + delta : 0f;
        handleJumpInput();
        updateCrouchHeight(delta);

        // Integrate physics
        integrateGravity(delta);
        integrateMovement(delta);
        updateHeadBob(delta);

        // Update camera
        updateCamera();
    }

    // ==================== Input Handling ====================

    /**
     * Update camera orientation from mouse input.
     * Yaw rotates left/right (Y axis), pitch tilts up/down (camera local X axis).
     */
    private void updateMouseLook(float delta) {
        float mouseDeltaX = Gdx.input.getDeltaX();
        float mouseDeltaY = Gdx.input.getDeltaY();

        yaw += mouseDeltaX * MOUSE_SENSITIVITY;
        pitch += mouseDeltaY * MOUSE_SENSITIVITY;

        // Clamp pitch to prevent camera flip
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
    }

    /**
     * Update movement state based on input, enforcing priority order.
     * Priority: Crouch > SlowWalk > Run > Walk > Idle
     */
    private void updateMovementState() {
        previousState = currentState;

        // Read input
        boolean slowWalkInput = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean runInput = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                           Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        boolean walkInput = Gdx.input.isKeyPressed(Input.Keys.W) ||
                            Gdx.input.isKeyPressed(Input.Keys.A) ||
                            Gdx.input.isKeyPressed(Input.Keys.S) ||
                            Gdx.input.isKeyPressed(Input.Keys.D);
        boolean cInput = Gdx.input.isKeyPressed(Input.Keys.C);

        isCrouchPressed = cInput;

        // Apply state priority logic (highest to lowest)
        if (cInput) {
            currentState = MovementState.CROUCH;
        } else if (slowWalkInput && walkInput) {
            currentState = MovementState.SLOW_WALK;
        } else if (runInput && walkInput && !staminaDepleted) {
            currentState = MovementState.RUN;
        } else if (walkInput) {
            currentState = MovementState.WALK;
        } else {
            currentState = MovementState.IDLE;
        }

        // Reset movement direction on state change for smooth transitions
        if (currentState != previousState && currentState == MovementState.IDLE) {
            velocity.x = 0;
            velocity.z = 0;
        }
    }

    /**
     * Apply upward velocity impulse when jump is pressed while grounded.
     */
    private void handleJumpInput() {
        if (!isGrounded || jumpCooldownRemainingSeconds > 0f || groundedTimeSeconds < MIN_GROUNDED_TIME_BEFORE_JUMP_SECONDS) {
            return;
        }

        if (!Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            return;
        }

        velocity.y = JUMP_FORCE;
        isGrounded = false;
        jumpTriggeredThisFrame = true;
        jumpCooldownRemainingSeconds = JUMP_COOLDOWN_SECONDS;
        headBobPhase = 0.0f;
    }

    /**
     * Smooth camera height lerp for crouch/stand transitions.
     * Blocks stand-up if ceiling is detected directly above player.
     */
    private void updateCrouchHeight(float delta) {
        float targetLerpT = isCrouchPressed ? 1.0f : 0.0f;
        
        // If player wants to stand but ceiling is in the way, keep crouched
        if (!isCrouchPressed && targetLerpT == 0.0f && isCeilingObstructed()) {
            targetLerpT = 1.0f; // Force stay crouched
        }

        float lerpDelta = delta * CROUCH_LERP_SPEED;

        if (targetLerpT > crouchLerpT) {
            crouchLerpT = Math.min(crouchLerpT + lerpDelta, targetLerpT);
        } else {
            crouchLerpT = Math.max(crouchLerpT - lerpDelta, targetLerpT);
        }
    }

    /**
     * Check if there is an obstruction directly above the player's head.
     * Used to block uncrouch when inside low spaces.
     * Simple AABB check against a vertical cylinder above player.
     *
     * @return True if ceiling/obstruction is detected
     */
    private boolean isCeilingObstructed() {
        float currentEyeHeight = getEyeHeight();
        float standingOffset = EYE_HEIGHT_STANDING - currentEyeHeight + CEILING_CHECK_MARGIN;
        if (standingOffset <= 0f) {
            return false;
        }

        float testY = position.y + standingOffset;
        return isCollidingAt(position.x, testY, position.z);
    }

    // ==================== Physics Integration ====================

    /**
     * Apply gravity to vertical velocity.
     */
    private void integrateGravity(float delta) {
        if (!isGrounded) {
            velocity.y -= GRAVITY * delta;
        }
    }

    /**
     * Integrate horizontal movement based on current state.
     * Direction is computed from camera forward/right, projected to XZ plane.
     */
    private void integrateMovement(float delta) {
        boolean collidedThisTick = false;
        Vector3 moveDir = computeMovementDirection();

        if (isGrounded) {
            if (moveDir.len2() > 0.001f) {
                moveDir.nor();
                float speed = currentState.speed;
                velocity.x = moveDir.x * speed;
                velocity.z = moveDir.z * speed;
            } else {
                velocity.x = 0;
                velocity.z = 0;
            }
        } else {
            if (moveDir.len2() > 0.001f) {
                moveDir.nor();
                float nudge = currentState.speed * AIR_CONTROL_FACTOR;
                velocity.x += moveDir.x * nudge;
                velocity.z += moveDir.z * nudge;
            }

            float hSpeed2 = (velocity.x * velocity.x) + (velocity.z * velocity.z);
            if (hSpeed2 > (AIR_MAX_HORIZONTAL_SPEED * AIR_MAX_HORIZONTAL_SPEED)) {
                float hSpeed = (float) Math.sqrt(hSpeed2);
                float scale = AIR_MAX_HORIZONTAL_SPEED / Math.max(hSpeed, 0.0001f);
                velocity.x *= scale;
                velocity.z *= scale;
            }
        }

        float nextX = position.x + (velocity.x * delta);
        float nextY = position.y + (velocity.y * delta);
        float nextZ = position.z + (velocity.z * delta);

        // Resolve horizontal collisions against static world colliders.
        if (isCollidingAt(nextX, position.y, position.z)) {
            nextX = position.x;
            velocity.x = 0f;
            collidedThisTick = true;
        }
        if (isCollidingAt(nextX, position.y, nextZ)) {
            nextZ = position.z;
            velocity.z = 0f;
            collidedThisTick = true;
        }
        if (collidedThisTick) {
            collisionCount++;
        }

        // Apply resolved movement.
        position.x = nextX;
        position.y = nextY;
        position.z = nextZ;

        // Simple ground detection (y = 0 is the floor)
        if (position.y <= getCollisionCapsuleRadius()) {
            position.y = getCollisionCapsuleRadius();
            velocity.y = 0;
            isGrounded = true;
        } else {
            isGrounded = false;
        }
    }

    private void updateHeadBob(float delta) {
        if (isGrounded && isHeadBobMovementState(currentState) && movementDirection.len2() > 0.0f) {
            float bobFrequency = getHeadBobFrequency(currentState);
            float bobAmplitude = getHeadBobAmplitude(currentState);
            headBobPhase += bobFrequency * delta;
            headBobOffset = MathUtils.sin(headBobPhase) * bobAmplitude;
            return;
        }

        headBobPhase = 0.0f;
        float recoveryAlpha = Math.min(1.0f, delta * HEAD_BOB_RECOVERY_SPEED);
        headBobOffset = MathUtils.lerp(headBobOffset, 0.0f, recoveryAlpha);
    }

    private void updateStamina(float delta) {
        if (currentState == MovementState.RUN && isGrounded) {
            stamina = Math.max(0f, stamina - STAMINA_DRAIN_RATE * delta);
            if (stamina <= 0f) {
                staminaDepleted = true;
            }
        } else {
            stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_RATE * delta);
            if (stamina >= STAMINA_REGEN_THRESHOLD) {
                staminaDepleted = false;
            }
        }
    }

    private boolean isHeadBobMovementState(MovementState movementState) {
        return movementState == MovementState.WALK
            || movementState == MovementState.RUN
            || movementState == MovementState.SLOW_WALK;
    }

    private float getHeadBobFrequency(MovementState movementState) {
        if (movementState == MovementState.RUN) {
            return RUN_BOB_FREQUENCY;
        }
        if (movementState == MovementState.SLOW_WALK) {
            return SLOW_WALK_BOB_FREQUENCY;
        }
        return WALK_BOB_FREQUENCY;
    }

    private float getHeadBobAmplitude(MovementState movementState) {
        if (movementState == MovementState.RUN) {
            return RUN_BOB_AMPLITUDE;
        }
        if (movementState == MovementState.SLOW_WALK) {
            return SLOW_WALK_BOB_AMPLITUDE;
        }
        return WALK_BOB_AMPLITUDE;
    }

    /**
     * Replace static world colliders used for simple player-vs-world collision checks.
     */
    public void setWorldColliders(Array<BoundingBox> colliders) {
        worldColliders.clear();
        if (colliders == null) {
            return;
        }

        for (int i = 0; i < colliders.size; i++) {
            worldColliders.add(new BoundingBox(colliders.get(i)));
        }
    }

    private boolean isCollidingAt(float worldX, float worldY, float worldZ) {
        if (worldColliders.isEmpty()) {
            return false;
        }

        float playerMinY = worldY - CAPSULE_RADIUS;
        float playerMaxY = worldY + EYE_HEIGHT_STANDING;
        float collisionRadiusSquared = CAPSULE_RADIUS * CAPSULE_RADIUS;

        for (int i = 0; i < worldColliders.size; i++) {
            BoundingBox collider = worldColliders.get(i);

            if (playerMaxY < collider.min.y || playerMinY > collider.max.y) {
                continue;
            }

            float nearestX = clamp(worldX, collider.min.x, collider.max.x);
            float nearestZ = clamp(worldZ, collider.min.z, collider.max.z);
            float deltaX = worldX - nearestX;
            float deltaZ = worldZ - nearestZ;
            float distanceSquared = (deltaX * deltaX) + (deltaZ * deltaZ);
            if (distanceSquared < collisionRadiusSquared) {
                return true;
            }
        }

        return false;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Compute horizontal movement direction from camera orientation and WASD input.
     * Direction is normalized, projected to XZ plane (no vertical component).
     *
     * @return Movement direction vector (in world space, XZ plane)
     */
    private Vector3 computeMovementDirection() {
        movementForward.set(camera.direction).nor();
        movementForward.y = 0f;
        if (movementForward.len2() <= 0.0001f) {
            movementDirection.setZero();
            return movementDirection;
        }
        movementForward.nor();

        movementRight.set(movementForward).crs(Vector3.Y).nor();
        movementDirection.setZero();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            movementDirection.add(movementForward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            movementDirection.sub(movementForward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            movementDirection.sub(movementRight);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            movementDirection.add(movementRight);
        }

        return movementDirection;
    }

    /**
     * Update camera position and orientation to match player.
     * Camera is positioned at player position + eye height offset.
     */
    private void updateCamera() {
        float eyeHeight = getEyeHeight();
        camera.position.set(position.x, position.y + eyeHeight + headBobOffset, position.z);

        // Build forward direction from yaw (rotation around Y axis)
        float yawRad = yaw * com.badlogic.gdx.math.MathUtils.degreesToRadians;
        float pitchRad = pitch * com.badlogic.gdx.math.MathUtils.degreesToRadians;

        lookDirection.set(
            (float) Math.sin(yawRad),
            -(float) Math.sin(pitchRad),
            -(float) Math.cos(yawRad)
        );
        lookDirection.nor();
        camera.direction.set(lookDirection);

        // FPS camera intentionally stays roll-free; fixed world-up avoids gimbal degeneracy.
        camera.up.set(0f, 1f, 0f);

        camera.update();
    }

    // ==================== Getters (for downstream systems) ====================

    /**
     * @return Current movement state
     */
    public MovementState getMovementState() {
        return currentState;
    }

    /**
     * @return World position of the player's center
     */
    public Vector3 getPosition() {
        return position.cpy();
    }

    /**
     * Fill caller-provided vector with player position to avoid allocations.
     *
     * @param outPosition destination vector (must not be null)
     * @return same destination vector for chaining
     */
    public Vector3 getPosition(Vector3 outPosition) {
        return outPosition.set(position);
    }

    /**
     * @return Current velocity vector
     */
    public Vector3 getVelocity() {
        return velocity.cpy();
    }

    /**
     * Fill caller-provided vector with player velocity to avoid allocations.
     *
     * @param outVelocity destination vector (must not be null)
     * @return same destination vector for chaining
     */
    public Vector3 getVelocity(Vector3 outVelocity) {
        return outVelocity.set(velocity);
    }

    /**
     * @return Current horizontal speed (magnitude of XZ velocity)
     */
    public float getHorizontalSpeed() {
        return (float) Math.sqrt((velocity.x * velocity.x) + (velocity.z * velocity.z));
    }

    public float getStamina() {
        return stamina;
    }

    public float getMaxStamina() {
        return MAX_STAMINA;
    }

    /**
     * @return Yaw angle in degrees
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * @return Pitch angle in degrees, clamped to ±MAX_PITCH
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * @return True if player is standing on ground (collision enabled)
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    public boolean consumeJumpTriggered() {
        boolean triggered = jumpTriggeredThisFrame;
        jumpTriggeredThisFrame = false;
        return triggered;
    }

    /**
     * @return True if crouch is currently active
     */
    public boolean isCrouching() {
        return crouchLerpT > 0.5f;
    }

    /**
     * Returns movement-tick collision count since last read and resets it.
     */
    public int getAndResetCollisionCount() {
        int count = collisionCount;
        collisionCount = 0;
        return count;
    }

    /**
     * @return Current eye height (interpolated between standing and crouch)
     */
    private float getEyeHeight() {
        return EYE_HEIGHT_STANDING + (EYE_HEIGHT_CROUCH - EYE_HEIGHT_STANDING) * crouchLerpT;
    }

    /**
     * @return Current collision capsule radius
     */
    private float getCollisionCapsuleRadius() {
        return CAPSULE_RADIUS;
    }

}
