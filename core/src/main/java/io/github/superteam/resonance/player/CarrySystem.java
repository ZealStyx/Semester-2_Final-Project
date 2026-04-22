package io.github.superteam.resonance.player;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import io.github.superteam.resonance.items.CarriableItem;
import java.util.Objects;

/**
 * Manages held-item state and carry-point physics targets.
 *
 * This class is intentionally agnostic of scene ownership and rendering.
 */
public final class CarrySystem {

    private static final float DEFAULT_CARRY_DISTANCE = 2.0f;
    private static final float MIN_CARRY_DISTANCE = 1.0f;
    private static final float MAX_CARRY_DISTANCE = 3.5f;

    private static final float DEFAULT_SPRING_STRENGTH = 300f;
    private static final float DEFAULT_DAMPING = 15f;
    private static final float DEFAULT_ROTATION_DAMPING = 8f;
    private static final float DEFAULT_MAX_CARRY_FORCE = 500f;
    private static final float WORLD_GRAVITY = 9.8f;
    private static final float CARRIED_GRAVITY_SCALE = 0.2f;

    private final PerspectiveCamera camera;
    private final Vector3 carryPoint = new Vector3();
    private final Vector3 scratchForward = new Vector3();
    private final Vector3 scratchPosition = new Vector3();
    private final Vector3 scratchError = new Vector3();
    private final Vector3 scratchVelocity = new Vector3();
    private final Vector3 scratchForce = new Vector3();
    private final Vector3 scratchSpring = new Vector3();
    private final Vector3 scratchDamping = new Vector3();
    private final Vector3 normalGravity = new Vector3(0f, -WORLD_GRAVITY, 0f);
    private final Vector3 carriedGravity = new Vector3(0f, -WORLD_GRAVITY * CARRIED_GRAVITY_SCALE, 0f);
    private final Matrix4 scratchTransform = new Matrix4();

    private CarriableItem heldItem;

    private float carryDistance = DEFAULT_CARRY_DISTANCE;
    private float springStrength = DEFAULT_SPRING_STRENGTH;
    private float damping = DEFAULT_DAMPING;
    private float rotationDamping = DEFAULT_ROTATION_DAMPING;
    private float maxCarryForce = DEFAULT_MAX_CARRY_FORCE;

    public CarrySystem(PerspectiveCamera camera) {
        this.camera = Objects.requireNonNull(camera, "camera must not be null");
    }

    public void update(float deltaSeconds) {
        updateCarryPoint();

        if (heldItem == null) {
            return;
        }

        btRigidBody rigidBody = heldItem.rigidBody();
        if (rigidBody == null) {
            heldItem.setWorldPosition(carryPoint);
            return;
        }

        rigidBody.getWorldTransform(scratchTransform);
        scratchTransform.setTranslation(carryPoint);
        rigidBody.setWorldTransform(scratchTransform);
        rigidBody.setLinearVelocity(Vector3.Zero);
        rigidBody.setAngularVelocity(Vector3.Zero);
        rigidBody.activate();
        heldItem.setWorldPosition(carryPoint);
    }

    public boolean tryPickup(CarriableItem carriableItem) {
        if (heldItem != null || carriableItem == null || carriableItem.isBroken()) {
            return false;
        }

        heldItem = carriableItem;

        btRigidBody rigidBody = carriableItem.rigidBody();
        if (rigidBody != null) {
            rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
            rigidBody.setDamping(damping, rotationDamping);
            rigidBody.activate();
        }

        return true;
    }

    public CarriableItem dropHeldItem() {
        CarriableItem droppedItem = heldItem;
        if (droppedItem != null && droppedItem.rigidBody() != null) {
            btRigidBody rigidBody = droppedItem.rigidBody();
            rigidBody.setGravity(normalGravity);
            rigidBody.setActivationState(Collision.WANTS_DEACTIVATION);
            rigidBody.activate();
        }
        heldItem = null;
        return droppedItem;
    }

    public CarriableItem throwHeldItem(Vector3 direction, float throwStrength) {
        CarriableItem thrownItem = heldItem;
        if (thrownItem != null && thrownItem.rigidBody() != null && direction != null) {
            btRigidBody rigidBody = thrownItem.rigidBody();
            scratchForward.set(direction).nor();
            scratchForward.y += 0.2f;
            scratchForward.nor();

            rigidBody.setGravity(normalGravity);
            rigidBody.setActivationState(Collision.WANTS_DEACTIVATION);
            rigidBody.activate();
            rigidBody.applyCentralImpulse(scratchForward.scl(Math.max(0f, throwStrength)));
        }
        heldItem = null;
        return thrownItem;
    }

    public boolean isHoldingItem() {
        return heldItem != null;
    }

    public CarriableItem heldItem() {
        return heldItem;
    }

    public Vector3 carryPoint(Vector3 out) {
        return out.set(carryPoint);
    }

    public float getCarryDistance() {
        return carryDistance;
    }

    public void adjustCarryDistance(float amount) {
        carryDistance = MathUtils.clamp(carryDistance + amount, MIN_CARRY_DISTANCE, MAX_CARRY_DISTANCE);
    }

    public float getSpringStrength() {
        return springStrength;
    }

    public void setSpringStrength(float springStrength) {
        this.springStrength = Math.max(0f, springStrength);
    }

    public float getDamping() {
        return damping;
    }

    public void setDamping(float damping) {
        this.damping = Math.max(0f, damping);
    }

    public float getRotationDamping() {
        return rotationDamping;
    }

    public void setRotationDamping(float rotationDamping) {
        this.rotationDamping = Math.max(0f, rotationDamping);
    }

    public float getMaxCarryForce() {
        return maxCarryForce;
    }

    public void setMaxCarryForce(float maxCarryForce) {
        this.maxCarryForce = Math.max(0f, maxCarryForce);
    }

    private void updateCarryPoint() {
        scratchForward.set(camera.direction).nor();
        carryPoint
            .set(camera.position)
            .mulAdd(scratchForward, carryDistance);
    }
}
