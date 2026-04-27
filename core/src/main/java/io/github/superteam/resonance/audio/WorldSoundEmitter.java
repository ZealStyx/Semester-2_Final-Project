package io.github.superteam.resonance.audio;

import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.SoundEvent;
import java.util.function.Function;

/**
 * Convenience wrapper for world-authored sound emitters.
 */
public final class WorldSoundEmitter {
    private final SpatialSfxEmitter spatialSfxEmitter;

    public WorldSoundEmitter(SpatialSfxEmitter spatialSfxEmitter) {
        this.spatialSfxEmitter = spatialSfxEmitter;
    }

    public void emit(
        String wavPath,
        SoundEvent graphEvent,
        Vector3 emitterPosition,
        Vector3 listenerPosition,
        float baseVolume,
        float graphIntensity,
        float elapsedSeconds,
        Function<Vector3, String> nodeResolver
    ) {
        if (spatialSfxEmitter == null) {
            return;
        }
        spatialSfxEmitter.playAndPropagate(
            wavPath,
            graphEvent,
            emitterPosition,
            listenerPosition,
            baseVolume,
            graphIntensity,
            elapsedSeconds,
            nodeResolver
        );
    }
}
