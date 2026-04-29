package io.github.superteam.resonance.behavior;

public final class BehaviorChangeDetector {
    private PlayerArchetype lastArchetype = PlayerArchetype.METHODICAL;
    private float secondsSinceChange;

    public boolean update(PlayerArchetype archetype, float deltaSeconds) {
        float dt = Math.max(0f, deltaSeconds);
        secondsSinceChange += dt;
        if (archetype == null || archetype == lastArchetype) {
            return false;
        }

        lastArchetype = archetype;
        secondsSinceChange = 0f;
        return true;
    }

    public PlayerArchetype lastArchetype() {
        return lastArchetype;
    }

    public float secondsSinceChange() {
        return secondsSinceChange;
    }
}
