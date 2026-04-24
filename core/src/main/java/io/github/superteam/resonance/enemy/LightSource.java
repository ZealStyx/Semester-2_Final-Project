package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.Vector3;

/**
 * Represents a single light source in the world that the enemy system
 * can query and the {@link LightFlickerController} can animate.
 *
 * <p>Light sources contribute to the player's "light exposure" feature fed into
 * the Director K-Means classifier — a player standing under a bright,
 * non-flickering light is more acoustically exposed than one in shadow.</p>
 */
public final class LightSource {

    /** Minimum valid intensity. Prevents division-by-zero in exposure calculations. */
    private static final float MIN_INTENSITY = 0.0f;
    /** Maximum supported intensity (pure white, fully on). */
    private static final float MAX_INTENSITY = 1.0f;

    private final String id;
    private final Vector3 position;
    private final float radius;

    /** Base (non-flickered) intensity in [0, 1]. */
    private float baseIntensity;
    /** Current rendered intensity, potentially modified by {@link LightFlickerController}. */
    private float currentIntensity;
    /** Whether this light participates in the flicker system at all. */
    private boolean flickerEnabled;

    /**
     * Creates a new light source.
     *
     * @param id             unique identifier (used by {@link LightSourceRegistry})
     * @param position       world-space position of the light emitter
     * @param radius         influence radius in metres — player within this radius receives exposure
     * @param baseIntensity  resting intensity in [0, 1]
     * @param flickerEnabled whether the {@link LightFlickerController} should animate this light
     */
    public LightSource(String id, Vector3 position, float radius, float baseIntensity, boolean flickerEnabled) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Light source id must not be null or blank.");
        }
        if (position == null) {
            throw new IllegalArgumentException("Light source position must not be null.");
        }
        if (radius <= 0f) {
            throw new IllegalArgumentException("Light source radius must be positive.");
        }
        this.id = id;
        this.position = new Vector3(position);
        this.radius = radius;
        this.baseIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, baseIntensity));
        this.currentIntensity = this.baseIntensity;
        this.flickerEnabled = flickerEnabled;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String id() {
        return id;
    }

    public Vector3 position() {
        return position;
    }

    public float radius() {
        return radius;
    }

    public float baseIntensity() {
        return baseIntensity;
    }

    public void setBaseIntensity(float baseIntensity) {
        this.baseIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, baseIntensity));
    }

    public float currentIntensity() {
        return currentIntensity;
    }

    /**
     * Called by {@link LightFlickerController} each frame to set the animated intensity.
     * Should only be called from the flicker system; external code reads {@link #currentIntensity()}.
     */
    public void setCurrentIntensity(float intensity) {
        this.currentIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, intensity));
    }

    public boolean isFlickerEnabled() {
        return flickerEnabled;
    }

    public void setFlickerEnabled(boolean flickerEnabled) {
        this.flickerEnabled = flickerEnabled;
    }

    /**
     * Returns the normalised exposure contribution (0–1) for a world position.
     * Uses inverse-square falloff within the radius, clamped to [0, 1].
     *
     * @param worldPosition position to test — typically the player's eye position
     * @return exposure in [0, 1]; 1 = standing directly under the light at full intensity
     */
    public float exposureAt(Vector3 worldPosition) {
        if (worldPosition == null || currentIntensity <= 0f) {
            return 0f;
        }
        float distSquared = position.dst2(worldPosition);
        float radiusSquared = radius * radius;
        if (distSquared >= radiusSquared) {
            return 0f;
        }
        // Smooth inverse-square falloff: 1 at origin, 0 at edge.
        float normalized = 1f - (distSquared / radiusSquared);
        return normalized * normalized * currentIntensity;
    }

    @Override
    public String toString() {
        return "LightSource{id='" + id + "', pos=" + position + ", r=" + radius
            + ", base=" + baseIntensity + ", cur=" + currentIntensity + '}';
    }
}
