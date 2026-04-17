package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Moving source bound to a runtime position provider.
 */
public final class AttachedSoundSource implements SoundSource {
    @FunctionalInterface
    public interface PositionProvider {
        Vector3 currentPosition();
    }

    @FunctionalInterface
    public interface NodeResolver {
        String currentNodeId(Vector3 currentWorldPosition, String previousNodeId);
    }

    private final String id;
    private final SoundEvent soundEvent;
    private String sourceNodeId;
    private final PositionProvider positionProvider;
    private final NodeResolver nodeResolver;
    private final float baseIntensity;
    private final float emitIntervalSeconds;
    private float emitCooldownSeconds;
    private boolean active = true;

    public AttachedSoundSource(
        String id,
        SoundEvent soundEvent,
        String sourceNodeId,
        PositionProvider positionProvider,
        float baseIntensity,
        float emitIntervalSeconds
    ) {
        this(id, soundEvent, sourceNodeId, positionProvider, null, baseIntensity, emitIntervalSeconds);
    }

    public AttachedSoundSource(
        String id,
        SoundEvent soundEvent,
        String sourceNodeId,
        PositionProvider positionProvider,
        NodeResolver nodeResolver,
        float baseIntensity,
        float emitIntervalSeconds
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
        this.positionProvider = Objects.requireNonNull(positionProvider, "Position provider must not be null.");
        this.nodeResolver = nodeResolver;
        this.baseIntensity = Math.max(0f, baseIntensity);
        this.emitIntervalSeconds = Math.max(0.01f, emitIntervalSeconds);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SoundSourceType type() {
        return SoundSourceType.ATTACHED;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public String sourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            return;
        }
        this.sourceNodeId = sourceNodeId;
    }

    @Override
    public List<SoundSourceEmission> updateAndCollect(float deltaSeconds, float nowSeconds) {
        if (!active) {
            return List.of();
        }

        emitCooldownSeconds = Math.max(0f, emitCooldownSeconds - Math.max(0f, deltaSeconds));
        if (emitCooldownSeconds > 0f) {
            return List.of();
        }

        Vector3 currentPosition = positionProvider.currentPosition();
        if (currentPosition == null) {
            return List.of();
        }

        String resolvedSourceNodeId = sourceNodeId;
        if (nodeResolver != null) {
            String currentResolvedNodeId = nodeResolver.currentNodeId(currentPosition, sourceNodeId);
            if (currentResolvedNodeId != null && !currentResolvedNodeId.isBlank()) {
                sourceNodeId = currentResolvedNodeId;
                resolvedSourceNodeId = currentResolvedNodeId;
            }
        }

        emitCooldownSeconds = emitIntervalSeconds;
        SoundEventData soundEventData = new SoundEventData(
            soundEvent,
            resolvedSourceNodeId,
            new Vector3(currentPosition),
            baseIntensity,
            nowSeconds
        );
        List<SoundSourceEmission> emissions = new ArrayList<>(1);
        emissions.add(new SoundSourceEmission(id, soundEventData));
        return emissions;
    }
}
