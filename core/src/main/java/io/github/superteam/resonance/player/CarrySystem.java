package io.github.superteam.resonance.player;

import com.badlogic.gdx.graphics.PerspectiveCamera;
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
    private static final float WORLD_GRAVITY = 9.8f;
    private static final float DROP_SETTLE_IMPULSE = -1.5f;

    private final PerspectiveCamera camera;
    private final Vector3 carryPoint = new Vector3();
    private final Vector3 scratchForward = new Vector3();
    private final Vector3 scratchImpulse = new Vector3();
    private final Vector3 normalGravity = new Vector3(0f, -WORLD_GRAVITY, 0f);
    private final Matrix4 scratchTransform = new Matrix4();

    private CarriableItem heldItem;

    private float carryDistance = DEFAULT_CARRY_DISTANCE;

    public CarrySystem(PerspectiveCamera camera) {
        this.camera = Objects.requireNonNull(camera, "camera must not be null");
    }

    public void update(float deltaSeconds) {
        updateCarryPoint();

        if (heldItem == null) {
            return;
        }

        if (heldItem.state() == CarriableItem.ItemState.BROKEN) {
            heldItem = null;
            return;
        }

        btRigidBody rigidBody = heldItem.rigidBody();
        if (rigidBody == null) {
            heldItem.setWorldPosition(carryPoint);
            return;
        }

        rigidBody.setGravity(Vector3.Zero);
        scratchTransform.setToTranslation(carryPoint);
        rigidBody.proceedToTransform(scratchTransform);
        rigidBody.setLinearVelocity(Vector3.Zero);
        rigidBody.setAngularVelocity(Vector3.Zero);
        rigidBody.activate();
        heldItem.setWorldPosition(carryPoint);
    }

    public boolean tryPickup(CarriableItem carriableItem) {
        if (heldItem != null || carriableItem == null || carriableItem.state() != CarriableItem.ItemState.WORLD) {
            return false;
        }

        heldItem = carriableItem;
        heldItem.setState(CarriableItem.ItemState.CARRIED);

        btRigidBody rigidBody = carriableItem.rigidBody();
        if (rigidBody != null) {
            rigidBody.setActivationState(Collision.DISABLE_DEACTIVATION);
            rigidBody.setGravity(Vector3.Zero);
            rigidBody.setDamping(0f, 0f);
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
            rigidBody.applyCentralImpulse(scratchImpulse.set(0f, DROP_SETTLE_IMPULSE, 0f));
            rigidBody.activate();
        }
        if (droppedItem != null && droppedItem.state() != CarriableItem.ItemState.BROKEN) {
            droppedItem.setState(CarriableItem.ItemState.WORLD);
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
        if (thrownItem != null && thrownItem.state() != CarriableItem.ItemState.BROKEN) {
            thrownItem.setState(CarriableItem.ItemState.WORLD);
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
        carryDistance = Math.max(MIN_CARRY_DISTANCE, Math.min(MAX_CARRY_DISTANCE, carryDistance + amount));
    }

    private void updateCarryPoint() {
        scratchForward.set(camera.direction).nor();
        carryPoint
            .set(camera.position)
            .mulAdd(scratchForward, carryDistance);
    }
}
