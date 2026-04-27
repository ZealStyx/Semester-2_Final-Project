package io.github.superteam.resonance.audio;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.sound.HearingCategory;
import io.github.superteam.resonance.sound.SoundEvent;
import io.github.superteam.resonance.sound.SoundEventData;
import io.github.superteam.resonance.sound.SoundPropagationOrchestrator;
import java.util.function.Function;

/**
 * Plays spatialized WAV SFX and optionally injects to the propagation graph.
 */
public final class SpatialSfxEmitter {
    private static final float MAX_AUDIBLE_DISTANCE = 30f;

    private final SoundBank soundBank;
    private final SoundPropagationOrchestrator propagationOrchestrator;

    public SpatialSfxEmitter(SoundBank soundBank, SoundPropagationOrchestrator propagationOrchestrator) {
        this.soundBank = soundBank;
        this.propagationOrchestrator = propagationOrchestrator;
    }

    public void playAndPropagate(
        String wavPath,
        SoundEvent graphEvent,
        Vector3 emitterPos,
        Vector3 listenerPos,
        float baseVolume,
        float graphIntensity,
        float elapsedSeconds,
        Function<Vector3, String> nodeResolver
    ) {
        if (wavPath == null || wavPath.isBlank() || emitterPos == null || listenerPos == null) {
            return;
        }

        float distance = emitterPos.dst(listenerPos);
        float attenuation = Math.max(0f, 1f - (distance / MAX_AUDIBLE_DISTANCE));
        float finalVolume = Math.max(0f, baseVolume) * attenuation * attenuation;
        float pan = MathUtils.clamp((emitterPos.x - listenerPos.x) / 8f, -1f, 1f);
        if (finalVolume > 0.01f) {
            soundBank.sound(wavPath).play(finalVolume, 1f, pan);
        }

        boolean shouldPropagate = graphEvent != null
            && graphEvent.hearingCategory() != HearingCategory.AMBIENCE
            && nodeResolver != null
            && propagationOrchestrator != null;

        if (!shouldPropagate) {
            return;
        }

        String nodeId = nodeResolver.apply(emitterPos);
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }

        SoundEventData data = new SoundEventData(
            graphEvent,
            nodeId,
            emitterPos,
            Math.max(0f, graphIntensity),
            Math.max(0f, elapsedSeconds)
        );
        propagationOrchestrator.emitSoundEvent(data, elapsedSeconds);
    }
}
