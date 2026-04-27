package io.github.superteam.resonance.interactable;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Runtime registry for interactables in the active scene.
 */
public final class InteractableRegistry {
    private final Array<Interactable> entries = new Array<>();

    public void register(Interactable interactable) {
        if (interactable != null && !entries.contains(interactable, true)) {
            entries.add(interactable);
        }
    }

    public void unregister(Interactable interactable) {
        entries.removeValue(interactable, true);
    }

    public void clear() {
        entries.clear();
    }

    public Interactable findBestTarget(Vector3 origin, Vector3 direction, float maxDistance, float minDot) {
        if (origin == null || direction == null) {
            return null;
        }

        Vector3 dir = new Vector3(direction);
        if (dir.isZero(0.0001f)) {
            return null;
        }
        dir.nor();

        Interactable best = null;
        float bestDistance = Float.MAX_VALUE;
        Vector3 toTarget = new Vector3();

        for (Interactable interactable : entries) {
            if (interactable == null || interactable.worldPosition() == null) {
                continue;
            }

            toTarget.set(interactable.worldPosition()).sub(origin);
            float distance = toTarget.len();
            if (distance > maxDistance + Math.max(0f, interactable.interactionRadius())) {
                continue;
            }
            if (distance <= 0.0001f) {
                return interactable;
            }

            float dot = toTarget.scl(1f / distance).dot(dir);
            if (dot < minDot) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                best = interactable;
            }
        }

        return best;
    }
}
