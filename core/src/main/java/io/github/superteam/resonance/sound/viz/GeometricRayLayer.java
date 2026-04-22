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
        for (int index = 0; index < rayCount; index++) {
            Vector3 direction = fibonacciDirection(index, rayCount);
            Vector3 rayStart = new Vector3(sourcePosition).mulAdd(direction, 0.1f);
            Vector3 rayEnd = new Vector3(rayStart).mulAdd(direction, maxDistance);

            Vector3 nearestHit = findNearestHit(rayStart, direction, maxDistance);
            if (nearestHit != null) {
                float travelDistance = rayStart.dst(nearestHit);
                float intensity = MathUtils.clamp(strength / ((travelDistance * travelDistance * 0.08f) + 1f), 0.05f, 1f);
                activeRays.add(new RayTraceSample(rayStart, nearestHit, intensity, config.fadeOutSeconds));
            } else {
                activeRays.add(new RayTraceSample(rayStart, rayEnd, MathUtils.clamp(strength, 0.05f, 1f), config.fadeOutSeconds));
            }
        }
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

        return nearestHit;
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

            shapes.setColor(1f, 0.94f, 0.35f, alpha * sample.intensity);
            shapes.line(sample.start.x, sample.start.y, sample.start.z, sample.end.x, sample.end.y, sample.end.z);

            float cross = config.bounceMarkerScale;
            shapes.setColor(markerColor.r, markerColor.g, markerColor.b, markerColor.a * alpha * sample.intensity);
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
        private final float initialSeconds;
        private float remainingSeconds;

        private RayTraceSample(Vector3 start, Vector3 end, float intensity, float lifetimeSeconds) {
            this.start = start;
            this.end = end;
            this.intensity = intensity;
            this.initialSeconds = Math.max(0.01f, lifetimeSeconds);
            this.remainingSeconds = this.initialSeconds;
        }
    }
}
