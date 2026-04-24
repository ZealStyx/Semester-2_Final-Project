package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.MathUtils;
import java.util.List;

/**
 * Animates {@link LightSource} intensities using configurable flicker patterns driven by the
 * current {@link io.github.superteam.resonance.director.DirectorController.DirectorTier}.
 *
 * <p>Design principles:</p>
 * <ul>
 *   <li><strong>CALM tier:</strong> lights are stable — near-zero flicker. Comfortable baseline.</li>
 *   <li><strong>TENSE tier:</strong> lights pulse slowly (0.8 Hz), ±20% depth. Subtle unease.</li>
 *   <li><strong>PANICKED tier:</strong> rapid irregular flicker (4–8 Hz), ±50% depth. Disorienting.</li>
 *   <li><strong>EXPOSED tier:</strong> lights strobe briefly then cut to 30% — enemy is close.</li>
 * </ul>
 *
 * <p>Each light source's {@link LightSource#isFlickerEnabled()} flag controls whether it
 * participates. Non-flicker lights (e.g. ambient sky fill) retain their base intensity.</p>
 *
 * <p>Thread-safety: not thread-safe. Update must be called on the libGDX render thread.</p>
 */
public final class LightFlickerController {

    // Tier-specific flicker parameters
    private static final float CALM_AMPLITUDE    = 0.02f;  // ±2% — barely perceptible
    private static final float CALM_FREQUENCY    = 0.20f;  // Hz — very slow drift

    private static final float TENSE_AMPLITUDE   = 0.18f;  // ±18%
    private static final float TENSE_FREQUENCY   = 0.80f;  // Hz

    private static final float PANICKED_AMPLITUDE = 0.48f; // ±48%
    private static final float PANICKED_FREQUENCY = 6.00f; // Hz (fast, irregular)
    private static final float PANICKED_CHAOS     = 0.40f; // random offset per light

    private static final float EXPOSED_STROBE_FREQUENCY = 12.0f; // Hz — very fast strobe
    private static final float EXPOSED_STROBE_SECONDS   = 0.60f; // strobe then cut
    private static final float EXPOSED_CUT_INTENSITY    = 0.25f; // resting intensity after cut

    // Tier enum mirror — kept as int to avoid coupling to DirectorController's class loader order
    private static final int TIER_CALM    = 0;
    private static final int TIER_TENSE   = 1;
    private static final int TIER_PANICKED = 2;
    private static final int TIER_EXPOSED  = 3;

    private final LightSourceRegistry registry;

    /** Elapsed seconds since this controller was created — drives the oscillation phase. */
    private float elapsedSeconds;
    /** Current active tier ordinal matching {@code DirectorController.DirectorTier.ordinal()}. */
    private int currentTierOrdinal = TIER_CALM;
    /** How long the controller has been in the EXPOSED tier (for the strobe-then-cut sequence). */
    private float exposedSecondsAccumulated;

    public LightFlickerController(LightSourceRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("LightSourceRegistry must not be null.");
        }
        this.registry = registry;
    }

    // -------------------------------------------------------------------------
    // Tier updates
    // -------------------------------------------------------------------------

    /**
     * Called by the scene whenever the Director tier changes.
     *
     * @param tierOrdinal ordinal of the new {@code DirectorController.DirectorTier}
     *                    (0=CALM, 1=TENSE, 2=PANICKED, 3=EXPOSED)
     */
    public void onTierChanged(int tierOrdinal) {
        if (tierOrdinal != currentTierOrdinal) {
            currentTierOrdinal = MathUtils.clamp(tierOrdinal, TIER_CALM, TIER_EXPOSED);
            exposedSecondsAccumulated = 0f;
        }
    }

    // -------------------------------------------------------------------------
    // Per-frame update
    // -------------------------------------------------------------------------

    /**
     * Updates all registered flicker-enabled light sources according to the current tier.
     *
     * @param deltaSeconds frame delta time in seconds; must be non-negative
     */
    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        elapsedSeconds += clampedDelta;

        if (currentTierOrdinal == TIER_EXPOSED) {
            exposedSecondsAccumulated += clampedDelta;
        }

        List<LightSource> lights = registry.all();
        for (int i = 0; i < lights.size(); i++) {
            LightSource light = lights.get(i);
            if (!light.isFlickerEnabled()) {
                // Non-flickering lights: restore base intensity if it drifted.
                light.setCurrentIntensity(light.baseIntensity());
                continue;
            }
            float newIntensity = computeIntensity(light, i);
            light.setCurrentIntensity(newIntensity);
        }
    }

    // -------------------------------------------------------------------------
    // Intensity computation
    // -------------------------------------------------------------------------

    private float computeIntensity(LightSource light, int lightIndex) {
        float base = light.baseIntensity();
        switch (currentTierOrdinal) {
            case TIER_CALM:
                return calmIntensity(base, lightIndex);
            case TIER_TENSE:
                return tenseIntensity(base, lightIndex);
            case TIER_PANICKED:
                return panickedIntensity(base, lightIndex);
            case TIER_EXPOSED:
                return exposedIntensity(base, lightIndex);
            default:
                return base;
        }
    }

    private float calmIntensity(float base, int index) {
        // Very gentle sine drift — each light has a unique phase offset.
        float phase = index * 0.61803f; // golden-ratio offset
        float sine = MathUtils.sin(MathUtils.PI2 * CALM_FREQUENCY * elapsedSeconds + phase);
        return MathUtils.clamp(base + sine * CALM_AMPLITUDE, 0f, 1f);
    }

    private float tenseIntensity(float base, int index) {
        float phase = index * 1.10f;
        float sine = MathUtils.sin(MathUtils.PI2 * TENSE_FREQUENCY * elapsedSeconds + phase);
        return MathUtils.clamp(base + sine * TENSE_AMPLITUDE, 0f, 1f);
    }

    private float panickedIntensity(float base, int index) {
        // Fast sine + a lower-frequency chaos term per light (makes each light feel independent).
        float phase    = index * 2.39996f; // another irrational offset
        float chaos    = MathUtils.sin(MathUtils.PI2 * (PANICKED_FREQUENCY * 0.31f) * elapsedSeconds + phase * 0.7f);
        float fast     = MathUtils.sin(MathUtils.PI2 * PANICKED_FREQUENCY * elapsedSeconds + phase);
        float combined = fast * (1f - PANICKED_CHAOS) + chaos * PANICKED_CHAOS;
        return MathUtils.clamp(base + combined * PANICKED_AMPLITUDE, 0.05f, 1f);
    }

    private float exposedIntensity(float base, int index) {
        if (exposedSecondsAccumulated < EXPOSED_STROBE_SECONDS) {
            // Rapid strobe during the initial window.
            float phase = index * 0.88f;
            float strobe = MathUtils.sin(MathUtils.PI2 * EXPOSED_STROBE_FREQUENCY * exposedSecondsAccumulated + phase);
            // Strobe: clamp hard — either full on or cut.
            return strobe > 0f ? base : EXPOSED_CUT_INTENSITY;
        }
        // After the strobe window, hold at the cut intensity (enemy is near — dim for tension).
        return EXPOSED_CUT_INTENSITY;
    }

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    /** Returns elapsed seconds since construction (useful for diagnostics). */
    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    /** Returns the current tier ordinal (0=CALM … 3=EXPOSED). */
    public int currentTierOrdinal() {
        return currentTierOrdinal;
    }
}
