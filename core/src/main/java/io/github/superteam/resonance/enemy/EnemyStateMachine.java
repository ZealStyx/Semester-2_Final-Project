package io.github.superteam.resonance.enemy;

/**
 * Lightweight state transition helper for enemy behavior.
 */
public final class EnemyStateMachine {
    private EnemyState current = EnemyState.IDLE;

    public EnemyState current() {
        return current;
    }

    public void to(EnemyState next) {
        if (next != null) {
            current = next;
        }
    }

    public void onSoundHeard() {
        if (current == EnemyState.IDLE || current == EnemyState.PATROL) {
            current = EnemyState.INVESTIGATE;
        }
    }

    public void onPlayerSeen() {
        current = EnemyState.CHASE;
    }

    public void onLostPlayer() {
        if (current == EnemyState.CHASE || current == EnemyState.ATTACK) {
            current = EnemyState.INVESTIGATE;
        }
    }
}
