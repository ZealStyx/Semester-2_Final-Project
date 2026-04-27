package io.github.superteam.resonance.enemy;

/**
 * Core enemy state ids used by the state machine.
 */
public enum EnemyState {
    IDLE,
    PATROL,
    INVESTIGATE,
    CHASE,
    ATTACK,
    STUNNED
}
