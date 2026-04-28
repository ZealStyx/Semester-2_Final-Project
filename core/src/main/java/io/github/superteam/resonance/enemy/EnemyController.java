package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.MathUtils;
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
    private final Vector3 patrolAnchor = new Vector3();

    private float patrolPhase;
    private float lostPlayerSeconds;
    private boolean hasPatrolAnchor;

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
            if (!hasPatrolAnchor) {
                patrolAnchor.set(position);
                hasPatrolAnchor = true;
            }
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
        float dt = Math.max(0f, deltaSeconds);
        if (stateMachine.current() == EnemyState.IDLE) {
            stateMachine.to(EnemyState.PATROL);
        }

        boolean canSeePlayer = playerPosition != null && perception.canSeePlayer(position, forward, playerPosition);
        if (canSeePlayer) {
            stateMachine.onPlayerSeen();
            lostPlayerSeconds = 0f;
        } else if (stateMachine.current() == EnemyState.CHASE) {
            lostPlayerSeconds += dt;
            if (lostPlayerSeconds >= 5f) {
                stateMachine.onLostPlayer();
                investigateTarget.set(position);
            }
        }

        switch (stateMachine.current()) {
            case PATROL -> {
                patrolPhase += dt * 0.6f;
                Vector3 patrolTarget = new Vector3(
                    patrolAnchor.x + MathUtils.cos(patrolPhase) * 1.5f,
                    patrolAnchor.y,
                    patrolAnchor.z + MathUtils.sin(patrolPhase) * 1.5f
                );
                moveToward(patrolTarget, dt, 1.2f);
            }
            case INVESTIGATE -> {
                moveToward(investigateTarget, dt, 1.8f);
                if (position.dst2(investigateTarget) <= 0.04f) {
                    stateMachine.to(EnemyState.PATROL);
                }
            }
            case CHASE -> {
                if (playerPosition != null) {
                    moveToward(playerPosition, dt, 2.4f);
                    if (position.dst2(playerPosition) <= 1.44f) {
                        stateMachine.to(EnemyState.ATTACK);
                    }
                }
            }
            case ATTACK -> {
                if (playerPosition != null && position.dst2(playerPosition) > 2.25f) {
                    stateMachine.to(EnemyState.CHASE);
                }
            }
            default -> {
                // Intentionally no-op for STUNNED and IDLE fallback.
            }
        }
    }

    private void moveToward(Vector3 target, float deltaSeconds, float speed) {
        if (target == null || deltaSeconds <= 0f) {
            return;
        }

        Vector3 step = new Vector3(target).sub(position);
        if (step.len2() <= 0.0001f) {
            return;
        }
        step.nor().scl(speed * deltaSeconds);
        position.add(step);
        if (!step.isZero(0.0001f)) {
            forward.set(step).nor();
        }
    }
}
