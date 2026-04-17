package io.github.superteam.resonance.player;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.items.CarriableItem;
import io.github.superteam.resonance.sound.PhysicsNoiseEmitter;
import io.github.superteam.resonance.sound.PropagationResult;
import java.util.Objects;
import java.util.function.Function;

/**
 * Collision-impact adapter for carriable items.
 *
 * This listener is engine-agnostic and can be called from Bullet callbacks.
 */
public final class ImpactListener {

    private static final float DEFAULT_MIN_IMPACT_THRESHOLD = 5.0f;
    private static final float DEFAULT_MAX_IMPACT_FOR_MAPPING = 150.0f;

    private final PhysicsNoiseEmitter physicsNoiseEmitter;
    private final Function<Vector3, String> sourceNodeResolver;
    private final Array<CarriableItem> pendingBreakItems = new Array<>();

    private float minimumImpactThreshold = DEFAULT_MIN_IMPACT_THRESHOLD;
    private float maxImpactForMapping = DEFAULT_MAX_IMPACT_FOR_MAPPING;

    public ImpactListener(PhysicsNoiseEmitter physicsNoiseEmitter, Function<Vector3, String> sourceNodeResolver) {
        this.physicsNoiseEmitter = Objects.requireNonNull(physicsNoiseEmitter, "physicsNoiseEmitter must not be null");
        this.sourceNodeResolver = Objects.requireNonNull(sourceNodeResolver, "sourceNodeResolver must not be null");
    }

    public PropagationResult onCarriableImpact(CarriableItem carriableItem, Vector3 impactWorldPosition, float appliedImpulse, float nowSeconds) {
        if (carriableItem == null || impactWorldPosition == null) {
            return null;
        }

        if (appliedImpulse < minimumImpactThreshold) {
            return null;
        }

        float mappedIntensity = mapImpulseToIntensity(appliedImpulse, carriableItem.definition().noiseMultiplier());
        String sourceNodeId = sourceNodeResolver.apply(impactWorldPosition);

        PropagationResult result = physicsNoiseEmitter.emitImpact(
            sourceNodeId,
            impactWorldPosition,
            mappedIntensity * 150f,
            nowSeconds
        );

        if (appliedImpulse >= carriableItem.definition().breakThreshold()) {
            pendingBreakItems.add(carriableItem);
        }

        return result;
    }

    public float mapImpulseToIntensity(float impulse, float noiseMultiplier) {
        float clampedImpulse = MathUtils.clamp(impulse, minimumImpactThreshold, maxImpactForMapping);
        float normalized = (clampedImpulse - minimumImpactThreshold) / (maxImpactForMapping - minimumImpactThreshold);
        return MathUtils.clamp(normalized * Math.max(0f, noiseMultiplier), 0f, 1f);
    }

    public Array<CarriableItem> consumePendingBreakItems() {
        Array<CarriableItem> copy = new Array<>(pendingBreakItems);
        pendingBreakItems.clear();
        return copy;
    }

    public void setMinimumImpactThreshold(float minimumImpactThreshold) {
        this.minimumImpactThreshold = Math.max(0f, minimumImpactThreshold);
    }

    public void setMaxImpactForMapping(float maxImpactForMapping) {
        this.maxImpactForMapping = Math.max(this.minimumImpactThreshold + 0.001f, maxImpactForMapping);
    }
}
