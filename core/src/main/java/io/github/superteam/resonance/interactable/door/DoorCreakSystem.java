package io.github.superteam.resonance.interactable.door;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.sound.SoundEvent;

/**
 * Maps door angular speed to creak audio parameters.
 */
public final class DoorCreakSystem {
    private static final float CREAK_SLOW_LIMIT = 0.33f;
    private static final float CREAK_MEDIUM_LIMIT = 0.66f;
    private static final float CREAK_COOLDOWN_SLOW = 0.55f;
    private static final float CREAK_COOLDOWN_FAST = 0.10f;
    private static final float CREAK_SPEED_FLOOR = 0.05f;

    private final java.util.Map<String, Float> cooldowns = new java.util.HashMap<>();

    public void updateDrag(String doorId, float normSpeed, Vector3 doorPos, Vector3 playerPos, float delta, EventContext ctx) {
        cooldowns.merge(doorId, -Math.max(0f, delta), Float::sum);
        if (cooldowns.getOrDefault(doorId, 0f) > 0f || normSpeed < CREAK_SPEED_FLOOR || ctx == null || ctx.audioSystem() == null) {
            return;
        }

        float volume = MathUtils.lerp(0.10f, 1.0f, MathUtils.clamp(normSpeed, 0f, 1f));
        float pitch = MathUtils.lerp(0.75f, 1.5f, MathUtils.clamp(normSpeed, 0f, 1f));
        String wav = normSpeed < CREAK_SLOW_LIMIT ? "audio/sfx/door/creak_slow.wav"
            : normSpeed < CREAK_MEDIUM_LIMIT ? "audio/sfx/door/creak_medium.wav"
            : "audio/sfx/door/creak_fast.wav";

        Vector3 listener = playerPos == null ? doorPos : playerPos;
        if (listener != null) {
            ctx.audioSystem().playSpatialSfxPitched(wav, doorPos, listener, volume, pitch);
        }
        cooldowns.put(doorId, MathUtils.lerp(CREAK_COOLDOWN_SLOW, CREAK_COOLDOWN_FAST, MathUtils.clamp(normSpeed, 0f, 1f)));
    }

    public void fireOneShot(String doorId, float normSpeed, Vector3 doorPos, Vector3 playerPos, EventContext ctx) {
        updateDrag(doorId, normSpeed, doorPos, playerPos, 0f, ctx);
    }

    public CreakSample sample(float angularSpeedDegPerSecond) {
        float speed = Math.abs(angularSpeedDegPerSecond);
        if (speed < 5f) {
            return new CreakSample(0f, 0f);
        }

        float normalized = MathUtils.clamp(
            (speed - 5f) / (100f - 5f),
            0f,
            1f
        );
        float volume = 0.08f + (0.72f * normalized);
        float pitch = 0.78f + (0.45f * normalized);
        return new CreakSample(volume, pitch);
    }

    public record CreakSample(float volume, float pitch) {
    }
}
