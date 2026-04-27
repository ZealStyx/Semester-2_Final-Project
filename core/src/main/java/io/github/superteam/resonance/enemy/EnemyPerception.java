package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.HearingCategory;
import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Minimal hearing/vision perception surface for Group A kickoff.
 */
public final class EnemyPerception {
    private static final float VISION_FOV_DEGREES = 70f;
    private static final float VISION_RANGE = 12f;
    private static final float MIN_AUDIBLE_INTENSITY = 0.18f;

    private final Vector3 lastHeardPosition = new Vector3();
    private float lastHeardIntensity;
    private boolean hasUnhandledSound;

    public void onSoundHeard(SoundEventData data) {
        if (data == null || data.eventType().hearingCategory() == HearingCategory.AMBIENCE) {
            return;
        }

        float intensity = data.baseIntensity();
        if (intensity < MIN_AUDIBLE_INTENSITY) {
            return;
        }

        lastHeardPosition.set(data.worldPosition());
        lastHeardIntensity = intensity;
        hasUnhandledSound = true;
    }

    public boolean consumeSoundTarget(Vector3 outPosition) {
        if (!hasUnhandledSound) {
            return false;
        }
        hasUnhandledSound = false;
        if (outPosition != null) {
            outPosition.set(lastHeardPosition);
        }
        return true;
    }

    public float lastHeardIntensity() {
        return lastHeardIntensity;
    }

    public boolean canSeePlayer(Vector3 enemyPos, Vector3 enemyForward, Vector3 playerPos) {
        if (enemyPos == null || enemyForward == null || playerPos == null) {
            return false;
        }

        Vector3 toPlayer = new Vector3(playerPos).sub(enemyPos);
        float distance = toPlayer.len();
        if (distance > VISION_RANGE || distance <= 0.0001f) {
            return false;
        }

        toPlayer.scl(1f / distance);
        Vector3 forward = new Vector3(enemyForward);
        if (forward.isZero(0.0001f)) {
            return false;
        }
        forward.nor();

        float angle = MathUtils.radiansToDegrees * (float) Math.acos(MathUtils.clamp(forward.dot(toPlayer), -1f, 1f));
        return angle <= (VISION_FOV_DEGREES * 0.5f);
    }
}
