package io.github.superteam.resonance.behavior;

/** The four archetypes used by the behavior classifier. */
public enum PlayerArchetype {
    PARANOID("Paranoid"),
    METHODICAL("Methodical"),
    IMPULSIVE("Impulsive"),
    PANICKED("Panicked");

    private final String displayName;

    PlayerArchetype(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static PlayerArchetype fromIndex(int index) {
        PlayerArchetype[] values = values();
        if (index < 0 || index >= values.length) {
            return METHODICAL;
        }
        return values[index];
    }
}
