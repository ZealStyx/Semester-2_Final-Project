package io.github.superteam.resonance.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.EnumMap;
import java.util.Map;

/**
 * Handles directional 3D audio cues for emitted sound events.
 */
public final class SpatialCueController {
    private static final float MAX_DISTANCE_METERS = 15f;
    private static final float OCCLUSION_PER_EXTRA_METER = 0.05f;

    private final Map<SoundEvent, String> soundAssetPathByEvent = new EnumMap<>(SoundEvent.class);
    private final Map<SoundEvent, Sound> loadedSoundByEvent = new EnumMap<>(SoundEvent.class);
    private Vector3 listenerPosition = new Vector3();
    private String listenerNodeId = "";

    public void setListenerPosition(Vector3 listenerPosition) {
        this.listenerPosition = listenerPosition == null ? new Vector3() : new Vector3(listenerPosition);
    }

    public void setListenerNode(String listenerNodeId, Vector3 listenerWorldPosition) {
        if (listenerNodeId == null || listenerNodeId.isBlank()) {
            this.listenerNodeId = "";
        } else {
            this.listenerNodeId = listenerNodeId;
        }
        setListenerPosition(listenerWorldPosition);
    }

    public void registerSoundAsset(SoundEvent soundEvent, String internalAssetPath) {
        if (soundEvent == null || internalAssetPath == null || internalAssetPath.isBlank()) {
            return;
        }
        soundAssetPathByEvent.put(soundEvent, internalAssetPath);
    }

    public void playEvent(SoundEventData soundEventData, float perceivedIntensity) {
        if (soundEventData == null || Gdx.audio == null || Gdx.files == null) {
            return;
        }
        if (perceivedIntensity <= 0f) {
            return;
        }

        String assetPath = soundAssetPathByEvent.get(soundEventData.eventType());
        if (assetPath == null || !Gdx.files.internal(assetPath).exists()) {
            return;
        }

        Sound sound = loadedSoundByEvent.computeIfAbsent(soundEventData.eventType(), ignored -> Gdx.audio.newSound(Gdx.files.internal(assetPath)));
        Vector3 eventPosition = soundEventData.worldPosition();
        float deltaX = eventPosition.x - listenerPosition.x;
        float deltaY = eventPosition.y - listenerPosition.y;
        float deltaZ = eventPosition.z - listenerPosition.z;
        float distance = (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        float volumeFalloff = MathUtils.clamp(1f - (distance / MAX_DISTANCE_METERS), 0f, 1f);
        float volume = MathUtils.clamp(perceivedIntensity * volumeFalloff, 0f, 1f);
        float pan = MathUtils.clamp(deltaX / MAX_DISTANCE_METERS, -1f, 1f);
        sound.play(volume, 1f, pan);
    }

    public void playEvent(SoundEventData soundEventData, PropagationResult propagationResult) {
        if (soundEventData == null || propagationResult == null || Gdx.audio == null || Gdx.files == null) {
            return;
        }

        String assetPath = soundAssetPathByEvent.get(soundEventData.eventType());
        if (assetPath == null || !Gdx.files.internal(assetPath).exists()) {
            return;
        }

        Sound sound = loadedSoundByEvent.computeIfAbsent(soundEventData.eventType(), ignored -> Gdx.audio.newSound(Gdx.files.internal(assetPath)));
        Vector3 sourcePosition = soundEventData.worldPosition();
        float euclideanDistance = sourcePosition.dst(listenerPosition);
        float graphIntensity = resolveGraphIntensity(propagationResult, soundEventData, euclideanDistance);
        if (graphIntensity <= 0f) {
            return;
        }

        float occlusionFactor = computeOcclusionFactor(propagationResult, euclideanDistance);
        float pan = computePan(sourcePosition);
        float volume = MathUtils.clamp(graphIntensity, 0f, 1f);
        float pitch = MathUtils.clamp(occlusionFactor, 0.6f, 1f);
        sound.play(volume, pitch, pan);
    }

    private float resolveGraphIntensity(
        PropagationResult propagationResult,
        SoundEventData soundEventData,
        float euclideanDistance
    ) {
        if (listenerNodeId != null && !listenerNodeId.isBlank()) {
            return propagationResult.getIntensityOrZero(listenerNodeId);
        }

        float localFalloff = MathUtils.clamp(1f - (euclideanDistance / MAX_DISTANCE_METERS), 0f, 1f);
        return MathUtils.clamp(soundEventData.baseIntensity() * localFalloff, 0f, 1f);
    }

    private float computeOcclusionFactor(PropagationResult propagationResult, float euclideanDistance) {
        if (listenerNodeId == null || listenerNodeId.isBlank()) {
            return 1f;
        }
        float graphDistance = propagationResult.getDistanceOrInfinity(listenerNodeId);
        if (!Float.isFinite(graphDistance)) {
            return 0.6f;
        }
        float extraPathDistance = Math.max(0f, graphDistance - euclideanDistance);
        float occlusionFactor = 1f - (extraPathDistance * OCCLUSION_PER_EXTRA_METER);
        float materialOcclusion = propagationResult.getMaterialOcclusionOrDefault(listenerNodeId, 1f);
        occlusionFactor *= MathUtils.clamp(materialOcclusion, 0f, 1f);
        return MathUtils.clamp(occlusionFactor, 0.6f, 1f);
    }

    private float computePan(Vector3 sourcePosition) {
        float deltaX = sourcePosition.x - listenerPosition.x;
        float deltaZ = sourcePosition.z - listenerPosition.z;
        float horizontalDistance = (float) Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        if (horizontalDistance <= 0.0001f) {
            return 0f;
        }
        float normalizedDirection = MathUtils.clamp(deltaX / horizontalDistance, -1f, 1f);
        float distanceWeight = MathUtils.clamp(horizontalDistance / MAX_DISTANCE_METERS, 0f, 1f);
        return MathUtils.clamp(normalizedDirection * distanceWeight, -1f, 1f);
    }

    public void dispose() {
        for (Sound sound : loadedSoundByEvent.values()) {
            sound.dispose();
        }
        loadedSoundByEvent.clear();
    }
}
