package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

/**
 * Fires spherical ray casts against scene bounds and renders bounce markers.
 */
public final class GeometricRayLayer {
    private static final float FLOOR_Y = 0f;

    private final Array<BoundingBox> colliders;
    private final Array<RayTraceSample> activeRays = new Array<>();
    private final Vector3 tmpHit = new Vector3();

    private AcousticBounceConfig.GeometricLayerConfig config;

    public GeometricRayLayer(Array<BoundingBox> colliders, AcousticBounceConfig.GeometricLayerConfig config) {
        this.colliders = colliders;
        this.config = config;
    }

    public void setConfig(AcousticBounceConfig.GeometricLayerConfig config) {
        this.config = config;
    }

    public void fireRays(Vector3 sourcePosition, float strength) {
        if (!config.renderRays || sourcePosition == null) {
            return;
        }

        int rayCount = Math.max(1, config.rayCount);
        float maxDistance = Math.max(1f, config.rayMaxDistanceMeters);

        // Fix E — multi-height vertical spread: fire rays from three Y offsets so bounce markers
        // appear across the full wall surface (floor-sill, chest, head level).
        float[] heightOffsets = {0.1f, 0.9f, 1.7f};

        for (int heightIndex = 0; heightIndex < heightOffsets.length; heightIndex++) {
            float yOffset = heightOffsets[heightIndex];
            // Reduce ray count per height tier so total stays manageable.
            int tierRayCount = Math.max(1, rayCount / heightOffsets.length);

            for (int index = 0; index < tierRayCount; index++) {
                // Interleave directions across tiers so each tier covers unique angular sectors.
                int globalIndex = index * heightOffsets.length + heightIndex;
                int totalRays = tierRayCount * heightOffsets.length;
                Vector3 direction = fibonacciDirection(globalIndex, totalRays);

                // Suppress rays pointing steeply up or down so they hit walls, not ceiling/floor.
                if (Math.abs(direction.y) > 0.8f) {
                    continue;
                }

                Vector3 origin = new Vector3(sourcePosition.x, sourcePosition.y + yOffset, sourcePosition.z);
                Vector3 rayStart = new Vector3(origin).mulAdd(direction, 0.1f);
                Vector3 rayEnd = new Vector3(rayStart).mulAdd(direction, maxDistance);

                Vector3 nearestHit = findNearestHit(rayStart, direction, maxDistance);
                if (nearestHit != null) {
                    float travelDistance = rayStart.dst(nearestHit);
                    if (resolveBounceDepth(travelDistance) > config.maxBounceDepth) {
                        continue;
                    }
                    float intensity = MathUtils.clamp(strength / ((travelDistance * travelDistance * 0.08f) + 1f), 0.05f, 1f);
                    if (Math.abs(nearestHit.y - FLOOR_Y) < 0.001f) {
                        intensity *= MathUtils.clamp(1f - config.groundAbsorption, 0.05f, 1f);
                    }
                    activeRays.add(new RayTraceSample(rayStart, nearestHit, intensity, strength, config.fadeOutSeconds));
                } else {
                    if (resolveBounceDepth(maxDistance) > config.maxBounceDepth) {
                        continue;
                    }
                    activeRays.add(new RayTraceSample(rayStart, rayEnd, MathUtils.clamp(strength, 0.05f, 1f), strength, config.fadeOutSeconds));
                }
            }
        }
    }

    private int resolveBounceDepth(float travelDistance) {
        if (travelDistance < 4f) {
            return 0;
        }
        if (travelDistance < 9f) {
            return 1;
        }
        if (travelDistance < 15f) {
            return 2;
        }
        return 3;
    }

    private Vector3 findNearestHit(Vector3 rayStart, Vector3 direction, float maxDistance) {
        Ray ray = new Ray(rayStart, direction);
        float nearestDistance = Float.POSITIVE_INFINITY;
        Vector3 nearestHit = null;

        for (BoundingBox collider : colliders) {
            if (collider == null) {
                continue;
            }
            if (!Intersector.intersectRayBounds(ray, collider, tmpHit)) {
                continue;
            }
            float distance = rayStart.dst(tmpHit);
            if (distance <= maxDistance && distance < nearestDistance) {
                nearestDistance = distance;
                nearestHit = new Vector3(tmpHit);
            }
        }

        if (config.groundEnabled && direction.y < -0.0001f) {
            float floorDistance = distanceToFloor(rayStart, direction);
            if (floorDistance > 0f && floorDistance <= maxDistance && floorDistance < nearestDistance) {
                nearestHit = new Vector3(rayStart).mulAdd(direction, floorDistance);
                nearestHit.y = FLOOR_Y;
            }
        }

        return nearestHit;
    }

    private float distanceToFloor(Vector3 origin, Vector3 direction) {
        if (direction.y >= 0f) {
            return Float.POSITIVE_INFINITY;
        }
        return (FLOOR_Y - origin.y) / direction.y;
    }

    private Vector3 fibonacciDirection(int index, int count) {
        final float goldenAngle = MathUtils.PI * (3f - (float) Math.sqrt(5f));
        float y = 1f - ((index + 0.5f) / count) * 2f;
        float radius = (float) Math.sqrt(Math.max(0f, 1f - (y * y)));
        float theta = goldenAngle * index;
        return new Vector3(MathUtils.cos(theta) * radius, y, MathUtils.sin(theta) * radius).nor();
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        for (int i = activeRays.size - 1; i >= 0; i--) {
            RayTraceSample sample = activeRays.get(i);
            sample.remainingSeconds -= clampedDelta;
            if (sample.remainingSeconds <= 0f) {
                activeRays.removeIndex(i);
            }
        }
    }

    public void render(ShapeRenderer shapes, PerspectiveCamera camera) {
        if (!config.renderRays || activeRays.isEmpty()) {
            return;
        }

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (RayTraceSample sample : activeRays) {
            float alpha = MathUtils.clamp(sample.remainingSeconds / Math.max(0.0001f, sample.initialSeconds), 0f, 1f);
            Color markerColor = config.bounceMarkerColor;

            float strengthBoost = 0.25f + (sample.sourceIntensity * 0.75f);
            float lineRed = MathUtils.clamp(0.35f + (sample.intensity * 0.65f), 0f, 1f);
            float lineGreen = MathUtils.clamp(0.30f + (sample.intensity * 0.55f), 0f, 1f);
            float lineBlue = MathUtils.clamp(0.20f + (sample.sourceIntensity * 0.80f), 0f, 1f);

            shapes.setColor(lineRed, lineGreen, lineBlue, alpha * sample.intensity * strengthBoost);
            shapes.line(sample.start.x, sample.start.y, sample.start.z, sample.end.x, sample.end.y, sample.end.z);

            float cross = config.bounceMarkerScale;
            shapes.setColor(
                MathUtils.clamp(markerColor.r * (0.6f + (sample.sourceIntensity * 0.4f)), 0f, 1f),
                MathUtils.clamp(markerColor.g * (0.6f + (sample.sourceIntensity * 0.4f)), 0f, 1f),
                MathUtils.clamp(markerColor.b * (0.6f + (sample.sourceIntensity * 0.4f)), 0f, 1f),
                markerColor.a * alpha * sample.intensity * strengthBoost
            );
            shapes.line(sample.end.x - cross, sample.end.y, sample.end.z, sample.end.x + cross, sample.end.y, sample.end.z);
            shapes.line(sample.end.x, sample.end.y - cross, sample.end.z, sample.end.x, sample.end.y + cross, sample.end.z);
            shapes.line(sample.end.x, sample.end.y, sample.end.z - cross, sample.end.x, sample.end.y, sample.end.z + cross);
        }
        shapes.end();
    }

    public int activeRayCount() {
        return activeRays.size;
    }

    private static final class RayTraceSample {
        private final Vector3 start;
        private final Vector3 end;
        private final float intensity;
        private final float sourceIntensity;
        private final float initialSeconds;
        private float remainingSeconds;

        private RayTraceSample(Vector3 start, Vector3 end, float intensity, float sourceIntensity, float lifetimeSeconds) {
            this.start = start;
            this.end = end;
            this.intensity = intensity;
            this.sourceIntensity = sourceIntensity;
            this.initialSeconds = Math.max(0.01f, lifetimeSeconds);
            this.remainingSeconds = this.initialSeconds;
        }
    }
}
