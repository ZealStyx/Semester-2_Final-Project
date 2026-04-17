package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Interval-driven fixed-position sound source.
 */
public final class StaticSoundSource implements SoundSource {
    private final String id;
    private final SoundEvent soundEvent;
    private final String sourceNodeId;
    private final Vector3 worldPosition;
    private final float baseIntensity;
    private final float minIntervalSeconds;
    private final float intervalRangeSeconds;
    private float remainingTimerSeconds;
    private boolean active = true;

    public StaticSoundSource(
        String id,
        SoundEvent soundEvent,
        String sourceNodeId,
        Vector3 worldPosition,
        float baseIntensity,
        float minIntervalSeconds,
        float intervalRangeSeconds
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Source id must not be blank.");
        }
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id must not be blank.");
        }

        this.id = id;
        this.soundEvent = Objects.requireNonNull(soundEvent, "Sound event must not be null.");
        this.sourceNodeId = sourceNodeId;
        this.worldPosition = worldPosition == null ? new Vector3() : new Vector3(worldPosition);
        this.baseIntensity = Math.max(0f, baseIntensity);
        this.minIntervalSeconds = Math.max(0.01f, minIntervalSeconds);
        this.intervalRangeSeconds = Math.max(0f, intervalRangeSeconds);
        resetTimer();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SoundSourceType type() {
        return SoundSourceType.STATIC;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    @Override
    public List<SoundSourceEmission> updateAndCollect(float deltaSeconds, float nowSeconds) {
        if (!active) {
            return List.of();
        }

        remainingTimerSeconds -= Math.max(0f, deltaSeconds);
        if (remainingTimerSeconds > 0f) {
            return List.of();
        }

        resetTimer();
        SoundEventData soundEventData = new SoundEventData(
            soundEvent,
            sourceNodeId,
            worldPosition,
            baseIntensity,
            nowSeconds
        );
        List<SoundSourceEmission> emissions = new ArrayList<>(1);
        emissions.add(new SoundSourceEmission(id, soundEventData));
        return emissions;
    }

    private void resetTimer() {
        remainingTimerSeconds = minIntervalSeconds + (MathUtils.random() * intervalRangeSeconds);
    }
}
