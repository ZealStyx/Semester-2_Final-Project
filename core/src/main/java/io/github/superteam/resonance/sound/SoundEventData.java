package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.Objects;

/**
 * Runtime payload for a single emitted sound event.
 */
public final class SoundEventData {
    private final SoundEvent eventType;
    private final String sourceNodeId;
    private final Vector3 worldPosition;
    private final float baseIntensity;
    private final float timestampSeconds;

    public SoundEventData(
        SoundEvent eventType,
        String sourceNodeId,
        Vector3 worldPosition,
        float baseIntensity,
        float timestampSeconds
    ) {
        this.eventType = Objects.requireNonNull(eventType, "Event type must not be null.");
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id must not be blank.");
        }
        this.sourceNodeId = sourceNodeId;
        this.worldPosition = worldPosition == null ? new Vector3() : new Vector3(worldPosition);
        this.baseIntensity = Math.max(0f, baseIntensity);
        this.timestampSeconds = Math.max(0f, timestampSeconds);
    }

    public static SoundEventData atNode(SoundEvent eventType, String sourceNodeId, Vector3 worldPosition, float timestampSeconds) {
        return new SoundEventData(
            eventType,
            sourceNodeId,
            worldPosition,
            eventType.defaultBaseIntensity(),
            timestampSeconds
        );
    }

    public SoundEvent eventType() {
        return eventType;
    }

    public String sourceNodeId() {
        return sourceNodeId;
    }

    public Vector3 worldPosition() {
        return new Vector3(worldPosition);
    }

    public float baseIntensity() {
        return baseIntensity;
    }

    public float timestampSeconds() {
        return timestampSeconds;
    }
}
