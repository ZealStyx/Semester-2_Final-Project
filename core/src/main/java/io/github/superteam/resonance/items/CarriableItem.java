package io.github.superteam.resonance.items;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import java.util.Objects;

/**
 * Runtime wrapper around a carriable item definition and its optional Bullet rigid body.
 */
public final class CarriableItem {

    private final ItemDefinition definition;
    private final Vector3 worldPosition = new Vector3();

    private btRigidBody rigidBody;
    private boolean broken;

    public CarriableItem(ItemDefinition definition, Vector3 initialWorldPosition) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        if (!definition.isCarriable()) {
            throw new IllegalArgumentException("CarriableItem requires an item definition with isCarriable=true");
        }
        this.worldPosition.set(Objects.requireNonNull(initialWorldPosition, "initialWorldPosition must not be null"));
    }

    public ItemDefinition definition() {
        return definition;
    }

    public btRigidBody rigidBody() {
        return rigidBody;
    }

    public void setRigidBody(btRigidBody rigidBody) {
        this.rigidBody = rigidBody;
    }

    public Vector3 worldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(Vector3 updatedPosition) {
        this.worldPosition.set(Objects.requireNonNull(updatedPosition, "updatedPosition must not be null"));
    }

    public boolean isBroken() {
        return broken;
    }

    public void markBroken() {
        broken = true;
    }
}
