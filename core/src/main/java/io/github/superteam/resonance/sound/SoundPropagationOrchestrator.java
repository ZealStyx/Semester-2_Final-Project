package io.github.superteam.resonance.sound;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Coordinates propagation, sonar visuals, spatial audio, and hearing listeners.
 */
public final class SoundPropagationOrchestrator {
    private static final float ECHO_INTENSITY_SCALE = 0.55f;
    private static final float REVERB_DELAY_STEP_SECONDS = 0.035f;

    private final AcousticGraphEngine acousticGraphEngine;
    private final DijkstraPathfinder dijkstraPathfinder;
    private final SonarRenderer sonarRenderer;
    private final SpatialCueController spatialCueController;
    private final SoundBalancingConfig soundBalancingConfig;
    private final PropagationCache propagationCache;
    private final ReflectionEngine reflectionEngine;
    private final AmbientSoundScheduler ambientSoundScheduler;
    private final SoundSourceRegistry soundSourceRegistry;

    private final List<EnemyHearingTarget> enemyHearingTargets = new ArrayList<>();
    private final List<EchoDirectorListener> echoDirectorListeners = new ArrayList<>();
    private final Map<SoundEvent, Float> eventCooldownUntilSeconds = new EnumMap<>(SoundEvent.class);
    private final PriorityQueue<DelayedAudioCue> delayedAudioCueQueue = new PriorityQueue<>((left, right) -> Float.compare(left.triggerAtSeconds(), right.triggerAtSeconds()));
    private float elapsedSeconds;
    private RoomAcousticProfile listenerRoomAcousticProfile = RoomAcousticProfile.ANECHOIC;

    public SoundPropagationOrchestrator(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SonarRenderer sonarRenderer,
        SpatialCueController spatialCueController,
        SoundBalancingConfig soundBalancingConfig
    ) {
        this(acousticGraphEngine, dijkstraPathfinder, sonarRenderer, spatialCueController, soundBalancingConfig, new PropagationCache());
    }

    public SoundPropagationOrchestrator(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SonarRenderer sonarRenderer,
        SpatialCueController spatialCueController,
        SoundBalancingConfig soundBalancingConfig,
        PropagationCache propagationCache
    ) {
        this(acousticGraphEngine, dijkstraPathfinder, sonarRenderer, spatialCueController, soundBalancingConfig, propagationCache, new ReflectionEngine());
    }

    public SoundPropagationOrchestrator(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SonarRenderer sonarRenderer,
        SpatialCueController spatialCueController,
        SoundBalancingConfig soundBalancingConfig,
        PropagationCache propagationCache,
        ReflectionEngine reflectionEngine
    ) {
        this(
            acousticGraphEngine,
            dijkstraPathfinder,
            sonarRenderer,
            spatialCueController,
            soundBalancingConfig,
            propagationCache,
            reflectionEngine,
            new AmbientSoundScheduler()
        );
    }

    public SoundPropagationOrchestrator(
        AcousticGraphEngine acousticGraphEngine,
        DijkstraPathfinder dijkstraPathfinder,
        SonarRenderer sonarRenderer,
        SpatialCueController spatialCueController,
        SoundBalancingConfig soundBalancingConfig,
        PropagationCache propagationCache,
        ReflectionEngine reflectionEngine,
        AmbientSoundScheduler ambientSoundScheduler
    ) {
        this.acousticGraphEngine = Objects.requireNonNull(acousticGraphEngine, "Acoustic graph engine must not be null.");
        this.dijkstraPathfinder = Objects.requireNonNull(dijkstraPathfinder, "Dijkstra pathfinder must not be null.");
        this.sonarRenderer = Objects.requireNonNull(sonarRenderer, "Sonar renderer must not be null.");
        this.spatialCueController = Objects.requireNonNull(spatialCueController, "Spatial cue controller must not be null.");
        this.soundBalancingConfig = Objects.requireNonNull(soundBalancingConfig, "Sound balancing config must not be null.");
        this.propagationCache = Objects.requireNonNull(propagationCache, "Propagation cache must not be null.");
        this.reflectionEngine = Objects.requireNonNull(reflectionEngine, "Reflection engine must not be null.");
        this.ambientSoundScheduler = Objects.requireNonNull(ambientSoundScheduler, "Ambient scheduler must not be null.");
        this.soundSourceRegistry = new SoundSourceRegistry();
    }

    public void registerSoundSource(SoundSource soundSource) {
        soundSourceRegistry.register(soundSource);
    }

    public void removeSoundSource(String sourceId) {
        soundSourceRegistry.remove(sourceId);
    }

    public void clearSoundSources() {
        soundSourceRegistry.clear();
    }

    public void registerEnemyListener(EnemyHearingTarget enemyHearingTarget) {
        if (enemyHearingTarget != null) {
            enemyHearingTargets.add(enemyHearingTarget);
        }
    }

    public void registerDirectorListener(EchoDirectorListener echoDirectorListener) {
        if (echoDirectorListener != null) {
            echoDirectorListeners.add(echoDirectorListener);
        }
    }

    public void setListenerRoomAcousticProfile(RoomAcousticProfile listenerRoomAcousticProfile) {
        this.listenerRoomAcousticProfile = listenerRoomAcousticProfile == null
            ? RoomAcousticProfile.ANECHOIC
            : listenerRoomAcousticProfile;
    }

    public PropagationResult emitSoundEvent(SoundEventData soundEventData, float nowSeconds) {
        Objects.requireNonNull(soundEventData, "Sound event data must not be null.");
        if (isInCooldown(soundEventData.eventType(), nowSeconds)) {
            return null;
        }

        SoundBalancingConfig.EventTuning tuning = soundBalancingConfig.tuningFor(soundEventData.eventType());
        float tunedBaseIntensity = soundEventData.baseIntensity() > 0f
            ? soundEventData.baseIntensity()
            : tuning.baseIntensity();
        SoundEventData tunedEvent = new SoundEventData(
            soundEventData.eventType(),
            soundEventData.sourceNodeId(),
            soundEventData.worldPosition(),
            tunedBaseIntensity,
            nowSeconds
        );

        DualBandResult dualBandResult = onSoundEventDualBand(acousticGraphEngine, tunedEvent, nowSeconds);
        PropagationResult mergedPropagationResult = mergeDualBandPropagation(dualBandResult);

        sonarRenderer.spawnFromPropagation(tunedEvent, mergedPropagationResult, soundBalancingConfig);
        spatialCueController.playEvent(tunedEvent, mergedPropagationResult);
        scheduleReflectionCues(tunedEvent, mergedPropagationResult);
        queueRoomReverbTail(tunedEvent);
        notifyEnemyListeners(tunedEvent, mergedPropagationResult);
        notifyDirectorListeners(tunedEvent, mergedPropagationResult);
        eventCooldownUntilSeconds.put(tunedEvent.eventType(), nowSeconds + tuning.cooldownSeconds());
        return mergedPropagationResult;
    }

    public void update(float deltaSeconds) {
        elapsedSeconds += Math.max(0f, deltaSeconds);
        List<SoundSourceEmission> sourceEmissions = soundSourceRegistry.updateAndCollect(deltaSeconds, elapsedSeconds);
        for (SoundSourceEmission sourceEmission : sourceEmissions) {
            emitSoundEvent(sourceEmission.soundEventData(), elapsedSeconds);
        }
        ambientSoundScheduler.update(deltaSeconds, elapsedSeconds, this);
        drainDelayedAudioCues();
        sonarRenderer.update(deltaSeconds);
    }

    private boolean isInCooldown(SoundEvent soundEvent, float nowSeconds) {
        float cooldownEnd = eventCooldownUntilSeconds.getOrDefault(soundEvent, 0f);
        return nowSeconds < cooldownEnd;
    }

    private void notifyEnemyListeners(SoundEventData soundEventData, PropagationResult propagationResult) {
        for (EnemyHearingTarget enemyHearingTarget : enemyHearingTargets) {
            String currentNodeId = enemyHearingTarget.getCurrentNodeId();
            float propagatedIntensity = propagationResult.getIntensityOrZero(currentNodeId);
            enemyHearingTarget.onSoundHeard(propagatedIntensity, soundEventData);
        }
    }

    private void notifyDirectorListeners(SoundEventData soundEventData, PropagationResult propagationResult) {
        for (EchoDirectorListener echoDirectorListener : echoDirectorListeners) {
            echoDirectorListener.onSoundEvent(soundEventData, propagationResult);
        }
    }

    private PropagationResult mergeDualBandPropagation(DualBandResult dualBandResult) {
        PropagationResult lowBandPropagation = dualBandResult.lowBandResult();
        PropagationResult highBandPropagation = dualBandResult.highBandResult();

        Map<String, Float> mergedDistanceByNodeId = new LinkedHashMap<>();
        Map<String, Float> mergedIntensityByNodeId = new LinkedHashMap<>();
        List<String> mergedRevealNodeIds = new ArrayList<>();

        for (Map.Entry<String, Float> lowBandDistanceEntry : lowBandPropagation.distanceByNodeId().entrySet()) {
            String nodeId = lowBandDistanceEntry.getKey();
            float lowBandDistance = lowBandDistanceEntry.getValue();
            float highBandDistance = highBandPropagation.getDistanceOrInfinity(nodeId);
            float mergedDistance = Math.min(lowBandDistance, highBandDistance);
            mergedDistanceByNodeId.put(nodeId, mergedDistance);

            float lowBandIntensity = lowBandPropagation.getIntensityOrZero(nodeId);
            float highBandIntensity = highBandPropagation.getIntensityOrZero(nodeId);
            float mergedIntensity = Math.max(lowBandIntensity, highBandIntensity);
            mergedIntensityByNodeId.put(nodeId, mergedIntensity);

            if (mergedIntensity >= soundBalancingConfig.revealThreshold()) {
                mergedRevealNodeIds.add(nodeId);
            }
        }

        return new PropagationResult(
            lowBandPropagation.sourceNodeId(),
            lowBandPropagation.baseIntensity(),
            mergedDistanceByNodeId,
            mergedIntensityByNodeId,
            mergedRevealNodeIds
        );
    }

    private DualBandResult onSoundEventDualBand(
        AcousticGraphEngine acousticGraphEngine,
        SoundEventData tunedEvent,
        float nowSeconds
    ) {
        float lowBandAttenuationAlpha = soundBalancingConfig.attenuationAlphaForBand(FrequencyBand.LOW);
        float highBandAttenuationAlpha = soundBalancingConfig.attenuationAlphaForBand(FrequencyBand.HIGH);
        float revealThreshold = soundBalancingConfig.revealThreshold();

        PropagationCache.CacheKey cacheKey = new PropagationCache.CacheKey(
            tunedEvent.sourceNodeId(),
            tunedEvent.baseIntensity(),
            lowBandAttenuationAlpha,
            highBandAttenuationAlpha,
            revealThreshold
        );

        DualBandResult cachedDualBandResult = propagationCache.getOrNull(cacheKey, nowSeconds);
        if (cachedDualBandResult != null) {
            return cachedDualBandResult;
        }

        DualBandResult computedDualBandResult = dijkstraPathfinder.onSoundEventDualBand(
            acousticGraphEngine,
            tunedEvent.sourceNodeId(),
            tunedEvent.baseIntensity(),
            lowBandAttenuationAlpha,
            highBandAttenuationAlpha,
            revealThreshold
        );
        propagationCache.put(cacheKey, computedDualBandResult, nowSeconds);
        return computedDualBandResult;
    }

    private void scheduleReflectionCues(SoundEventData tunedEvent, PropagationResult mergedPropagationResult) {
        List<ReflectionEvent> reflectionEvents = reflectionEngine.computeReflections(
            acousticGraphEngine,
            dijkstraPathfinder,
            soundBalancingConfig,
            mergedPropagationResult
        );

        for (ReflectionEvent reflectionEvent : reflectionEvents) {
            GraphNode bounceNode = acousticGraphEngine.requireNode(reflectionEvent.bounceNodeId());
            SoundEventData reflectedSoundEventData = SoundEventData.atNode(
                tunedEvent.eventType(),
                reflectionEvent.bounceNodeId(),
                bounceNode.position(),
                elapsedSeconds
            );

            delayedAudioCueQueue.offer(
                new DelayedAudioCue(
                    reflectedSoundEventData,
                    reflectionEvent.intensity() * ECHO_INTENSITY_SCALE,
                    elapsedSeconds + reflectionEvent.delaySeconds(),
                    reflectionEvent.bounceDepth()
                )
            );
        }
    }

    private void drainDelayedAudioCues() {
        while (!delayedAudioCueQueue.isEmpty() && delayedAudioCueQueue.peek().triggerAtSeconds() <= elapsedSeconds) {
            DelayedAudioCue delayedAudioCue = delayedAudioCueQueue.poll();
            spatialCueController.playEvent(delayedAudioCue.soundEventData(), delayedAudioCue.intensity());
            if (delayedAudioCue.bounceDepth() > 0) {
                sonarRenderer.spawnReflectionReveal(
                    delayedAudioCue.soundEventData().sourceNodeId(),
                    delayedAudioCue.intensity(),
                    delayedAudioCue.bounceDepth()
                );
            }
        }
    }

    private void queueRoomReverbTail(SoundEventData tunedEvent) {
        float wetGain = listenerRoomAcousticProfile.wetDryRatio();
        if (wetGain <= 0f) {
            return;
        }

        float minReverbIntensity = soundBalancingConfig.minReverbIntensity();
        float baseTailIntensity = tunedEvent.baseIntensity() * wetGain * 0.5f;
        if (baseTailIntensity < minReverbIntensity) {
            return;
        }

        int maxEchoesByDecay = Math.max(1, Math.round(listenerRoomAcousticProfile.reverbDecaySeconds() / REVERB_DELAY_STEP_SECONDS));
        int echoCount = Math.min(soundBalancingConfig.maxReverbEchoes(), maxEchoesByDecay);
        for (int echoIndex = 1; echoIndex <= echoCount; echoIndex++) {
            float scheduledDelay = listenerRoomAcousticProfile.earlyReflectionDelaySeconds() + (echoIndex * REVERB_DELAY_STEP_SECONDS);
            float decayFactor = (float) Math.exp(-(scheduledDelay / listenerRoomAcousticProfile.reverbDecaySeconds()));
            float reverbIntensity = baseTailIntensity * decayFactor;
            if (reverbIntensity < minReverbIntensity) {
                break;
            }

            delayedAudioCueQueue.offer(
                new DelayedAudioCue(
                    tunedEvent,
                    reverbIntensity,
                    elapsedSeconds + scheduledDelay,
                    0
                )
            );
        }
    }

    private record DelayedAudioCue(
        SoundEventData soundEventData,
        float intensity,
        float triggerAtSeconds,
        int bounceDepth
    ) {
    }
}
