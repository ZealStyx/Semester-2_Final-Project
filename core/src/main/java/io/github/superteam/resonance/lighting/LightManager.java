package io.github.superteam.resonance.lighting;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.sound.SoundEventData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Group B System 9 starter implementation.
 * Maintains runtime lights, applies tier-based flicker, and supports sound-reactive pulses.
 */
public final class LightManager {
    private static final float DEFAULT_PULSE_DURATION_SECONDS = 0.35f;
    private static final int MAX_SHADER_LIGHTS = 8;

    private final Map<String, GameLight> lightsById = new LinkedHashMap<>();
    private final Array<LightReactorListener> listeners = new Array<>();
    private final FlickerController flickerController;
    private final Map<String, Float> pulseBoostByLightId = new LinkedHashMap<>();
    private final Map<String, Float> pulseRemainingByLightId = new LinkedHashMap<>();

    private LightingTier tier = LightingTier.CALM;
    private float ambientMultiplier = 1.0f;
    private float elapsedSeconds;

    public LightManager(FlickerController flickerController) {
        this.flickerController = flickerController == null ? new FlickerController() : flickerController;
    }

    public void setAmbientMultiplier(float multiplier) {
        ambientMultiplier = MathUtils.clamp(multiplier, 0.1f, 2.0f);
    }

    public float ambientMultiplier() {
        return ambientMultiplier;
    }

    public void setTier(LightingTier newTier) {
        if (newTier == null || newTier == tier) {
            return;
        }
        tier = newTier;
        for (LightReactorListener listener : listeners) {
            listener.onLightingTierChanged(newTier);
        }
    }

    public LightingTier tier() {
        return tier;
    }

    public void register(GameLight light) {
        if (light == null) {
            return;
        }
        lightsById.put(light.id(), light);
    }

    public void remove(String lightId) {
        if (lightId == null) {
            return;
        }
        lightsById.remove(lightId);
        pulseBoostByLightId.remove(lightId);
        pulseRemainingByLightId.remove(lightId);
    }

    public GameLight find(String lightId) {
        return lightsById.get(lightId);
    }

    public Array<GameLight> snapshot() {
        Array<GameLight> result = new Array<>(lightsById.size());
        for (GameLight light : lightsById.values()) {
            result.add(light);
        }
        return result;
    }

    public int lightCount() {
        return lightsById.size();
    }

    public float exposureAt(Vector3 worldPosition) {
        if (worldPosition == null || lightsById.isEmpty()) {
            return 0f;
        }

        float maxExposure = 0f;
        for (GameLight light : lightsById.values()) {
            if (light.broken() || light.currentIntensity() <= 0f) {
                continue;
            }

            float radius = Math.max(0.001f, light.radius());
            float dist2 = light.position().dst2(worldPosition);
            float radius2 = radius * radius;
            if (dist2 >= radius2) {
                continue;
            }

            float normalized = 1f - (dist2 / radius2);
            float exposure = normalized * normalized * MathUtils.clamp(light.currentIntensity(), 0f, 1f);
            maxExposure = Math.max(maxExposure, exposure);
        }
        return MathUtils.clamp(maxExposure, 0f, 1f);
    }

    public void addListener(LightReactorListener listener) {
        if (listener != null && !listeners.contains(listener, true)) {
            listeners.add(listener);
        }
    }

    public void removeListener(LightReactorListener listener) {
        listeners.removeValue(listener, true);
    }

    public void onSoundEvent(SoundEventData soundEventData) {
        if (soundEventData == null) {
            return;
        }
        float intensity = MathUtils.clamp(soundEventData.baseIntensity(), 0f, 2f);
        for (GameLight light : lightsById.values()) {
            pulseBoostByLightId.put(light.id(), intensity * 0.35f);
            pulseRemainingByLightId.put(light.id(), DEFAULT_PULSE_DURATION_SECONDS);
        }
        for (LightReactorListener listener : listeners) {
            listener.onSoundEvent(soundEventData);
        }
    }

    public void update(float deltaSeconds) {
        float dt = Math.max(0f, deltaSeconds);
        elapsedSeconds += dt;

        int index = 0;
        for (GameLight light : lightsById.values()) {
            float flickerIntensity = flickerController.computeIntensity(light, tier, elapsedSeconds, index);
            float pulseIntensity = updatePulse(light.id(), dt);
            light.setCurrentIntensity((flickerIntensity + pulseIntensity) * ambientMultiplier);
            index++;
        }
    }

    public int writeShaderArrays(float[] lightPosXYZ, float[] lightColorRGBA, float[] lightRadius, float[] lightIntensity) {
        if (lightPosXYZ == null || lightColorRGBA == null || lightRadius == null || lightIntensity == null) {
            throw new IllegalArgumentException("Shader arrays must not be null.");
        }

        int count = 0;
        for (GameLight light : lightsById.values()) {
            if (count >= MAX_SHADER_LIGHTS) {
                break;
            }

            int p = count * 3;
            int c = count * 4;
            lightPosXYZ[p] = light.position().x;
            lightPosXYZ[p + 1] = light.position().y;
            lightPosXYZ[p + 2] = light.position().z;

            lightColorRGBA[c] = light.color().r;
            lightColorRGBA[c + 1] = light.color().g;
            lightColorRGBA[c + 2] = light.color().b;
            lightColorRGBA[c + 3] = light.color().a;

            lightRadius[count] = light.radius();
            lightIntensity[count] = light.currentIntensity();
            count++;
        }
        return count;
    }

    private float updatePulse(String lightId, float deltaSeconds) {
        Float remaining = pulseRemainingByLightId.get(lightId);
        if (remaining == null || remaining <= 0f) {
            pulseRemainingByLightId.remove(lightId);
            pulseBoostByLightId.remove(lightId);
            return 0f;
        }

        float next = Math.max(0f, remaining - deltaSeconds);
        pulseRemainingByLightId.put(lightId, next);

        float boost = pulseBoostByLightId.getOrDefault(lightId, 0f);
        float alpha = remaining <= 0f ? 0f : (next / remaining);
        return boost * alpha;
    }
}
