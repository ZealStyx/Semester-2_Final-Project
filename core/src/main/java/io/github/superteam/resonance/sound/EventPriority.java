package io.github.superteam.resonance.sound;

/**
 * Priority bands used for sound event gating and scheduling.
 */
public enum EventPriority {
    AMBIENT(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int priorityRank;

    EventPriority(int priorityRank) {
        if (priorityRank < 0) {
            throw new IllegalArgumentException("Priority rank must be non-negative.");
        }
        this.priorityRank = priorityRank;
    }

    public int priorityRank() {
        return priorityRank;
    }
}
