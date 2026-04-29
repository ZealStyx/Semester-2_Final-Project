package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;

/** Small sensor collider for a door handle. */
public final class DoorKnobCollider implements Disposable {
    private static final float KNOB_RADIUS = 0.12f;

    private final btSphereShape shape;
    private final btDefaultMotionState motionState;
    private final btRigidBody body;
    private btDiscreteDynamicsWorld attachedWorld;

    public DoorKnobCollider(Vector3 knobWorldPosition, btDiscreteDynamicsWorld world) {
        shape = new btSphereShape(KNOB_RADIUS);
        motionState = new btDefaultMotionState(new Matrix4().setToTranslation(knobWorldPosition == null ? new Vector3() : knobWorldPosition));
        btRigidBody.btRigidBodyConstructionInfo ci = new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, shape, Vector3.Zero);
        body = new btRigidBody(ci);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
        ci.dispose();
        attachToWorld(world);
    }

    public void attachToWorld(btDiscreteDynamicsWorld world) {
        if (world == null || attachedWorld == world) {
            return;
        }
        if (attachedWorld != null) {
            attachedWorld.removeRigidBody(body);
        }
        attachedWorld = world;
        attachedWorld.addRigidBody(body);
    }

    public void syncPosition(Vector3 knobWorldPosition) {
        if (knobWorldPosition == null) {
            return;
        }
        motionState.setWorldTransform(new Matrix4().setToTranslation(knobWorldPosition));
        body.setWorldTransform(new Matrix4().setToTranslation(knobWorldPosition));
    }

    public btRigidBody body() {
        return body;
    }

    @Override
    public void dispose() {
        if (attachedWorld != null) {
            attachedWorld.removeRigidBody(body);
            attachedWorld = null;
        }
        body.dispose();
        motionState.dispose();
        shape.dispose();
    }
}