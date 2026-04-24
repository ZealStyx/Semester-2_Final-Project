package io.github.superteam.resonance.enemy;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton-style registry that owns all {@link LightSource} instances for the scene.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Add / remove light sources at runtime (doors opening, lamps being destroyed).</li>
 *   <li>Compute the aggregate light exposure for a world position (used as feature F6
 *       in the Director's K-Means classifier).</li>
 *   <li>Provide a snapshot list for the {@link LightFlickerController} to iterate.</li>
 * </ul>
 *
 * <p>Thread-safety: not thread-safe. All calls must happen on the libGDX render thread.</p>
 */
public final class LightSourceRegistry {

    private static final float MAX_EXPOSURE = 1.0f;

    private final Map<String, LightSource> sourcesById = new LinkedHashMap<>();
    private final List<LightSource> sourceList = new ArrayList<>();
    private final List<LightSource> readOnlyView = Collections.unmodifiableList(sourceList);

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Registers a light source. Replaces any existing source with the same id.
     *
     * @param source light source to register; must not be null
     */
    public void register(LightSource source) {
        if (source == null) {
            throw new IllegalArgumentException("LightSource must not be null.");
        }
        LightSource existing = sourcesById.put(source.id(), source);
        if (existing != null) {
            sourceList.remove(existing);
        }
        sourceList.add(source);
    }

    /**
     * Removes the light source with the given id. No-op if the id is unknown.
     *
     * @param id light source id to remove
     */
    public void remove(String id) {
        if (id == null) {
            return;
        }
        LightSource removed = sourcesById.remove(id);
        if (removed != null) {
            sourceList.remove(removed);
        }
    }

    /** Removes all registered light sources. */
    public void clear() {
        sourcesById.clear();
        sourceList.clear();
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns the light source with the given id, or {@code null} if not found.
     *
     * @param id light source id
     * @return {@link LightSource} or {@code null}
     */
    public LightSource get(String id) {
        if (id == null) {
            return null;
        }
        return sourcesById.get(id);
    }

    /** Returns the number of registered light sources. */
    public int size() {
        return sourceList.size();
    }

    /** Returns {@code true} if no light sources are registered. */
    public boolean isEmpty() {
        return sourceList.isEmpty();
    }

    /**
     * Returns an unmodifiable live view of all registered light sources.
     * The list order matches insertion order.
     *
     * @return read-only list of {@link LightSource}
     */
    public List<LightSource> all() {
        return readOnlyView;
    }

    // -------------------------------------------------------------------------
    // Exposure computation
    // -------------------------------------------------------------------------

    /**
     * Computes the aggregate light exposure at {@code worldPosition} from all registered sources.
     *
     * <p>Each source contributes independently; contributions are summed and clamped to [0, 1].
     * This is the value fed into the K-Means classifier as feature F6 (light exposure).</p>
     *
     * @param worldPosition world-space position to evaluate — typically the player's eye position
     * @return aggregate exposure in [0, 1]; 1 = maximum (multiple bright lights at close range)
     */
    public float computeExposure(Vector3 worldPosition) {
        if (worldPosition == null || sourceList.isEmpty()) {
            return 0f;
        }
        float total = 0f;
        for (int i = 0; i < sourceList.size(); i++) {
            total += sourceList.get(i).exposureAt(worldPosition);
            if (total >= MAX_EXPOSURE) {
                return MAX_EXPOSURE;
            }
        }
        return Math.min(total, MAX_EXPOSURE);
    }

    /**
     * Convenience helper: creates and registers a standard scene lamp at the given position.
     *
     * @param id             unique id
     * @param x              world X
     * @param y              world Y (lamp height)
     * @param z              world Z
     * @param radius         influence radius in metres
     * @param baseIntensity  resting intensity [0, 1]
     * @param flickerEnabled whether the flicker controller should animate this lamp
     * @return the newly created and registered {@link LightSource}
     */
    public LightSource addLamp(String id, float x, float y, float z,
                                float radius, float baseIntensity, boolean flickerEnabled) {
        LightSource lamp = new LightSource(id, new Vector3(x, y, z), radius, baseIntensity, flickerEnabled);
        register(lamp);
        return lamp;
    }
}
