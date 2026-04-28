package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerController;

/** Minimal sprint-barge detector for the v2 door model. */
public final class DoorBargeDetector {
    private static final float BARGE_SPEED_THRESHOLD = 4.8f;

    public boolean check(PlayerController player, DoorController door, EventContext ctx) {
        if (player == null || door == null || door.isFullyOpen() || door.lockState() != DoorLockState.UNLOCKED) {
            return false;
        }

        MovementState state = player.getMovementState();
        if (state != MovementState.RUN) {
            return false;
        }
        if (player.getHorizontalSpeed() < BARGE_SPEED_THRESHOLD) {
            return false;
        }

        Vector3 playerPos = player.getPosition();
        BoundingBox zone = door.computeBargeZone(0.6f, 1.2f, 0.4f);
        if (!zone.contains(playerPos)) {
            return false;
        }

        door.bargeOpen(ctx);
        return true;
    }
}