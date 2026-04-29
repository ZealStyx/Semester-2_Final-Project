package io.github.superteam.resonance.behavior;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.lighting.LightManager;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.player.PlayerFeatureExtractor;
import io.github.superteam.resonance.scare.JumpScareDirector;
import io.github.superteam.resonance.sanity.SanitySystem;
import java.util.List;

/** Runtime behavior classifier backed by a small K-means stack. */
public final class BehaviorSystem implements PlayerFeatureExtractor.PlayerFeaturesListener {
    private static final float[] AMBIENT_MULTIPLIER = { 0.92f, 1.00f, 1.05f, 0.88f };
    private static final float[] SANITY_DRAIN_MULTIPLIER = { 1.10f, 1.00f, 1.15f, 0.85f };
    private static final float[] MIN_SCARE_INTERVAL = { 60f, 50f, 30f, 75f };
    private static final float[] TENSION_THRESHOLD = { 60f, 75f, 55f, 85f };

    private final PlayerBehaviorTracker tracker = new PlayerBehaviorTracker();
    private final KMeansClassifier classifier = new KMeansClassifier();
    private final BehaviorChangeDetector changeDetector = new BehaviorChangeDetector();

    private PlayerFeatureExtractor.PlayerFeatures latestFeatures;
    private ClassificationResult currentResult = ClassificationResult.neutral();
    private MovementState movementState = MovementState.IDLE;
    private boolean flashlightOn;
    private boolean previousFlashlightOn;
    private float currentSanity = SanitySystem.MAX_SANITY;
    private float currentLightExposure;
    private float lastSanityDelta;
    private float elapsedSeconds;
    private float flashlightToggleCount;

    public void captureFrameState(
        MovementState movementState,
        boolean flashlightOn,
        float sanityValue,
        float lightExposure,
        float sanityDeltaThisFrame
    ) {
        this.movementState = movementState == null ? MovementState.IDLE : movementState;
        this.flashlightOn = flashlightOn;
        this.currentSanity = MathUtils.clamp(sanityValue, 0f, SanitySystem.MAX_SANITY);
        this.currentLightExposure = MathUtils.clamp(lightExposure, 0f, 1f);
        this.lastSanityDelta = sanityDeltaThisFrame;
        if (flashlightOn != previousFlashlightOn) {
            flashlightToggleCount += 1f;
            previousFlashlightOn = flashlightOn;
        }
    }

    public void update(float deltaSeconds, LightManager lightManager, SanitySystem sanitySystem, JumpScareDirector jumpScareDirector) {
        float dt = Math.max(0f, deltaSeconds);
        elapsedSeconds += dt;
        changeDetector.update(currentResult.archetype(), dt);

        PlayerArchetype archetype = currentResult.archetype();
        int archetypeIndex = archetype.ordinal();

        if (lightManager != null) {
            lightManager.setAmbientMultiplier(AMBIENT_MULTIPLIER[archetypeIndex]);
        }
        if (sanitySystem != null) {
            sanitySystem.setDrainMultiplier(SANITY_DRAIN_MULTIPLIER[archetypeIndex]);
        }
        if (jumpScareDirector != null) {
            jumpScareDirector.setMinInterval(MIN_SCARE_INTERVAL[archetypeIndex]);
            jumpScareDirector.setTensionThreshold(TENSION_THRESHOLD[archetypeIndex]);
        }
    }

    @Override
    public void onFeatures(PlayerFeatureExtractor.PlayerFeatures features) {
        latestFeatures = features;
        BehaviorSample sample = FeatureNormalizer.from(
            features,
            movementState,
            flashlightOn,
            flashlightToggleRatePerMinute(),
            lastSanityDelta,
            currentLightExposure
        );
        tracker.record(elapsedSeconds, sample);
        List<BehaviorFeatureVector> snapshot = tracker.snapshot(elapsedSeconds);
        currentResult = classifier.classify(snapshot);
    }

    public String currentArchetype() {
        return currentResult.archetype().displayName();
    }

    public PlayerArchetype currentArchetypeValue() {
        return currentResult.archetype();
    }

    public float[] scores() {
        return currentResult.scores();
    }

    public int sampleCount() {
        return currentResult.sampleCount();
    }

    public float inertia() {
        return currentResult.inertia();
    }

    public String debugLine() {
        float[] scores = scores();
        StringBuilder builder = new StringBuilder();
        builder.append("[Behavior] ").append(currentArchetype());
        builder.append("  Samples=").append(sampleCount());
        builder.append("  Inertia=").append(String.format("%.3f", inertia()));
        builder.append("  Scores=");
        PlayerArchetype[] archetypes = PlayerArchetype.values();
        for (int i = 0; i < archetypes.length && i < scores.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(archetypes[i].name().charAt(0)).append(':').append(String.format("%.2f", scores[i]));
        }
        return builder.toString();
    }

    public float flashlightToggleRatePerMinute() {
        if (elapsedSeconds <= 0f) {
            return 0f;
        }
        return flashlightToggleCount * 60f / elapsedSeconds;
    }

    public float lightExposure() {
        return currentLightExposure;
    }

    public float sanityValue() {
        return currentSanity;
    }

    public MovementState movementState() {
        return movementState;
    }

    public PlayerFeatureExtractor.PlayerFeatures latestFeatures() {
        return latestFeatures;
    }
}
