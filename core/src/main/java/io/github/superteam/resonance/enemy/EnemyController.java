package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Group A bootstrap enemy controller with perception + state transitions.
 */
public final class EnemyController {
    private final EnemyPerception perception = new EnemyPerception();
    private final EnemyStateMachine stateMachine = new EnemyStateMachine();

    private final Vector3 position = new Vector3();
    private final Vector3 forward = new Vector3(0f, 0f, -1f);
    private final Vector3 investigateTarget = new Vector3();

    public EnemyPerception perception() {
        return perception;
    }

    public EnemyStateMachine stateMachine() {
        return stateMachine;
    }

    public Vector3 position() {
        return new Vector3(position);
    }

    public void setPose(Vector3 position, Vector3 forward) {
        if (position != null) {
            this.position.set(position);
        }
        if (forward != null && !forward.isZero(0.0001f)) {
            this.forward.set(forward).nor();
        }
    }

    public void onSoundHeard(SoundEventData soundEventData) {
        perception.onSoundHeard(soundEventData);
        if (perception.consumeSoundTarget(investigateTarget)) {
            stateMachine.onSoundHeard();
        }
    }

    public void update(float deltaSeconds, Vector3 playerPosition) {
        if (playerPosition != null && perception.canSeePlayer(position, forward, playerPosition)) {
            stateMachine.onPlayerSeen();
        }

        if (stateMachine.current() == EnemyState.INVESTIGATE) {
            Vector3 toTarget = new Vector3(investigateTarget).sub(position);
            if (toTarget.len2() > 0.04f) {
                toTarget.nor().scl(Math.max(0f, deltaSeconds) * 1.8f);
                position.add(toTarget);
                if (!toTarget.isZero(0.0001f)) {
                    forward.set(toTarget).nor();
                }
            }
        }
    }
}
