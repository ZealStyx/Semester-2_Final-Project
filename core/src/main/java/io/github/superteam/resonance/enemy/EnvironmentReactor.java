package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.MathUtils;
import io.github.superteam.resonance.director.DirectorController;
import io.github.superteam.resonance.director.DirectorController.DirectorTier;
import io.github.superteam.resonance.rendering.blind.BlindEffectController;

/**
 * Bridges the {@link DirectorController} with environment systems that respond to tension tier.
 *
 * <p>This is Phase AI-0/AI-1 of the enemy plan: when the Director's tier changes, the
 * {@code EnvironmentReactor} propagates that change to:</p>
 * <ul>
 *   <li>{@link LightFlickerController} — changes flicker intensity/frequency per tier.</li>
 *   <li>{@link BlindEffectController} — adjusts fog visibility based on tier.</li>
 *   <li>VHS strength modifier (future: {@code VHSScratchModulator}) — placeholder for now.</li>
 * </ul>
 *
 * <p>The reactor uses a smooth {@code lerpSpeed} so transitions are perceptible but not jarring.
 * Only the fog multiplier is applied smoothly; flicker responds immediately on tier change.</p>
 *
 * <h3>Tier → Environment mapping</h3>
 * <pre>
 * CALM     : fog at 60% baseline   | lights stable    | VHS at 0%
 * TENSE    : fog at 80% baseline   | lights pulse     | VHS at 15%
 * PANICKED : fog at 100% baseline  | lights flicker   | VHS at 35%
 * EXPOSED  : fog at 110% baseline  | lights strobe    | VHS at 60%
 * </pre>
 *
 * <p>Thread-safety: not thread-safe; must be called on the libGDX render thread.</p>
 */
public final class EnvironmentReactor {

    // How fast the fog multiplier lerps toward the target value (units per second).
    private static final float FOG_LERP_SPEED = 1.2f;

    // VHS scratch strength per tier [0, 1] — applied to a future VHSScratchModulator.
    private static final float[] VHS_STRENGTH_BY_TIER = {0.00f, 0.15f, 0.35f, 0.60f};

    private final LightFlickerController flickerController;
    private final BlindEffectController blindEffectController;

    private DirectorTier lastAppliedTier = DirectorTier.CALM;

    // Current and target fog multiplier driven by the Director tier.
    private float currentFogMultiplier = DirectorTier.CALM.fogMultiplier();
    private float targetFogMultiplier  = DirectorTier.CALM.fogMultiplier();

    // Placeholder for VHS scratch strength — consumed by VHSScratchModulator in AI-2.
    private float currentVhsStrength = 0f;

    /**
     * Constructs an {@code EnvironmentReactor}.
     *
     * @param flickerController    the light flicker controller to drive per tier; must not be null
     * @param blindEffectController the fog/blind controller to adjust visibility; must not be null
     */
    public EnvironmentReactor(LightFlickerController flickerController,
                               BlindEffectController blindEffectController) {
        if (flickerController == null) {
            throw new IllegalArgumentException("LightFlickerController must not be null.");
        }
        if (blindEffectController == null) {
            throw new IllegalArgumentException("BlindEffectController must not be null.");
        }
        this.flickerController = flickerController;
        this.blindEffectController = blindEffectController;
    }

    // -------------------------------------------------------------------------
    // Per-frame update
    // -------------------------------------------------------------------------

    /**
     * Called each frame. Reads the current Director snapshot, updates targets, and smoothly
     * applies environment changes.
     *
     * @param deltaSeconds    frame delta time in seconds
     * @param directorSnapshot current snapshot from {@link DirectorController#snapshot()}
     */
    public void update(float deltaSeconds, DirectorController.DirectorSnapshot directorSnapshot) {
        float clampedDelta = Math.max(0f, deltaSeconds);

        DirectorTier tier = directorSnapshot == null ? DirectorTier.CALM : directorSnapshot.currentTier();

        // Immediate tier-change reactions (flicker, VHS target).
        if (tier != lastAppliedTier) {
            flickerController.onTierChanged(tier.ordinal());
            targetFogMultiplier = tier.fogMultiplier();
            currentVhsStrength  = vhsStrengthFor(tier);
            lastAppliedTier = tier;
        }

        // Smooth fog transition.
        currentFogMultiplier = MathUtils.lerp(
            currentFogMultiplier, targetFogMultiplier,
            Math.min(1f, FOG_LERP_SPEED * clampedDelta)
        );

        // Apply fog multiplier to the blind effect controller.
        blindEffectController.setTierFogMultiplier(currentFogMultiplier);

        // Update light flicker each frame.
        flickerController.update(clampedDelta);
    }

    // -------------------------------------------------------------------------
    // State accessors (for diagnostics / future AI-2 VHS wiring)
    // -------------------------------------------------------------------------

    /**
     * Returns the smoothed fog multiplier currently applied to the scene.
     * In [0.6, 1.1] depending on tier.
     */
    public float currentFogMultiplier() {
        return currentFogMultiplier;
    }

    /**
     * Returns the target VHS scratch strength for the current tier.
     * Range [0, 0.6]. Will be consumed by {@code VHSScratchModulator} in AI Phase AI-2.
     */
    public float currentVhsStrength() {
        return currentVhsStrength;
    }

    /** Returns the last tier for which environment changes were applied. */
    public DirectorTier lastAppliedTier() {
        return lastAppliedTier;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static float vhsStrengthFor(DirectorTier tier) {
        int ordinal = MathUtils.clamp(tier.ordinal(), 0, VHS_STRENGTH_BY_TIER.length - 1);
        return VHS_STRENGTH_BY_TIER[ordinal];
    }
}
