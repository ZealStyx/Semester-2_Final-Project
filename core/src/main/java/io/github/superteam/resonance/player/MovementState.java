package io.github.superteam.resonance.player;

/**
 * Enumeration of player movement states.
 * Defines all possible locomotion modes and their associated properties.
 *
 * Single Responsibility: Define movement state constants and properties.
 */
public enum MovementState {
    IDLE(0.0f, "Idle"),
    WALK(3.5f, "Walk"),
    RUN(7.0f, "Run"),
    SLOW_WALK(1.2f, "Slow Walk"),
    CROUCH(1.8f, "Crouch");

    /** Horizontal movement speed in m/s */
    public final float speed;
    /** Human-readable state name */
    public final String displayName;

    MovementState(float speed, String displayName) {
        this.speed = speed;
        this.displayName = displayName;
    }
}
