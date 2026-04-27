package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ray-based pulse visualiser that scans scene surfaces and spawns simple bounce legs.
 */
public final class SurfaceScanPulse {
    private static final int RAY_COUNT = 128;
    private static final float MAX_RAY_DISTANCE = 25f;
    private static final float TRAVEL_SPEED = 14f;
    private static final float BOUNCE_DECAY = 0.55f;
    private static final int MAX_BOUNCES = 2;
    private static final float RAY_START_OFFSET = 0.02f;
    private static final float FACE_EPSILON = 0.03f;
    private static final float FLOOR_Y = 0f;

    private final Array<BoundingBox> colliders;
    private final List<PulseState> activePulses = new ArrayList<>();

    public SurfaceScanPulse(Array<BoundingBox> colliders) {
        this.colliders = colliders;
    }

    public void fire(Vector3 origin, float intensity) {
        if (origin == null) {
            return;
        }

        PulseState pulse = new PulseState(new Vector3(origin), MathUtils.clamp(intensity, 0.05f, 1f));
        buildPrimaryRays(pulse);
        activePulses.add(pulse);
    }

    public void update(float deltaSeconds) {
        float delta = Math.max(0f, deltaSeconds);
        if (delta <= 0f || activePulses.isEmpty()) {
            return;
        }

        for (int pulseIndex = activePulses.size() - 1; pulseIndex >= 0; pulseIndex--) {
            PulseState pulse = activePulses.get(pulseIndex);
            pulse.elapsedSeconds += delta;

            for (int rayIndex = 0; rayIndex < pulse.rays.size(); rayIndex++) {
                RayState ray = pulse.rays.get(rayIndex);
                if (ray.finished) {
                    continue;
                }

                ray.travelledDistance += TRAVEL_SPEED * delta;
                float clampedDistance = Math.min(ray.travelledDistance, ray.maxTravelDistance);

                if (ray.hitDistance > 0f && clampedDistance >= ray.hitDistance) {
                    ray.finished = true;
                    ray.travelledDistance = ray.hitDistance;

                    if (ray.hitPoint != null && ray.hitNormal != null) {
                        float glowIntensity = ray.intensity * MathUtils.clamp(1f - ray.bounceCount * 0.3f, 0.2f, 1f);
                        pulse.glows.add(new SurfaceGlow(ray.hitPoint, ray.hitNormal, glowIntensity, ray.bounceCount));
                    }

                    if (ray.bounceCount < MAX_BOUNCES && ray.hitPoint != null && ray.hitNormal != null) {
                        RayState bounce = createBounceRay(ray);
                        if (bounce != null && bounce.intensity >= 0.04f) {
                            pulse.rays.add(bounce);
                        }
                    }
                } else if (clampedDistance >= ray.maxTravelDistance) {
                    ray.finished = true;
                    ray.travelledDistance = ray.maxTravelDistance;
                }
            }

            for (int glowIndex = pulse.glows.size() - 1; glowIndex >= 0; glowIndex--) {
                SurfaceGlow glow = pulse.glows.get(glowIndex);
                glow.age += delta;
                if (glow.expired()) {
                    pulse.glows.remove(glowIndex);
                }
            }

            if (pulse.elapsedSeconds > pulse.maxLifetimeSeconds) {
                activePulses.remove(pulseIndex);
            }
        }
    }

    public List<PulseState> pulseStates() {
        return Collections.unmodifiableList(activePulses);
    }

    private void buildPrimaryRays(PulseState pulse) {
        final float goldenAngle = MathUtils.PI * (3f - (float) Math.sqrt(5f));

        for (int i = 0; i < RAY_COUNT; i++) {
            float y = 1f - ((i + 0.5f) / RAY_COUNT) * 2f;
            float radius = (float) Math.sqrt(Math.max(0f, 1f - y * y));
            float theta = goldenAngle * i;
            Vector3 direction = new Vector3(
                MathUtils.cos(theta) * radius,
                y,
                MathUtils.sin(theta) * radius
            ).nor();

            if (direction.y > 0.55f) {
                continue;
            }

            RayState ray = createRay(pulse.origin, direction, pulse.baseIntensity, 0);
            pulse.rays.add(ray);
        }
    }

    private RayState createRay(Vector3 origin, Vector3 direction, float intensity, int bounceCount) {
        Vector3 start = new Vector3(origin).mulAdd(direction, RAY_START_OFFSET);
        HitResult hit = findNearestHit(start, direction, MAX_RAY_DISTANCE);

        RayState ray = new RayState(start, direction, intensity, bounceCount, MAX_RAY_DISTANCE);
        if (hit != null) {
            ray.hitDistance = hit.distance;
            ray.hitPoint = hit.point;
            ray.hitNormal = hit.normal;
        }
        return ray;
    }

    private RayState createBounceRay(RayState parent) {
        Vector3 reflectedDirection = new Vector3(parent.direction)
            .sub(new Vector3(parent.hitNormal).scl(2f * parent.direction.dot(parent.hitNormal)))
            .nor();
        Vector3 bounceOrigin = new Vector3(parent.hitPoint).mulAdd(parent.hitNormal, RAY_START_OFFSET);
        float bouncedIntensity = parent.intensity * BOUNCE_DECAY;
        return createRay(bounceOrigin, reflectedDirection, bouncedIntensity, parent.bounceCount + 1);
    }

    private HitResult findNearestHit(Vector3 origin, Vector3 direction, float maxDistance) {
        float nearestDistance = Float.POSITIVE_INFINITY;
        HitResult nearest = null;

        if (colliders != null && !colliders.isEmpty()) {
            Ray ray = new Ray(origin, direction);
            Vector3 tmpHit = new Vector3();

            for (BoundingBox collider : colliders) {
                if (collider == null) {
                    continue;
                }
                if (!Intersector.intersectRayBounds(ray, collider, tmpHit)) {
                    continue;
                }

                float distance = origin.dst(tmpHit);
                if (distance <= maxDistance && distance < nearestDistance) {
                    nearestDistance = distance;
                    Vector3 hitPoint = new Vector3(tmpHit);
                    Vector3 hitNormal = estimateNormal(collider, hitPoint);
                    nearest = new HitResult(hitPoint, hitNormal, distance);
                }
            }
        }

        if (direction.y < -0.0001f) {
            float floorDistance = (FLOOR_Y - origin.y) / direction.y;
            if (floorDistance > RAY_START_OFFSET && floorDistance <= maxDistance && floorDistance < nearestDistance) {
                Vector3 floorHit = new Vector3(origin).mulAdd(direction, floorDistance);
                floorHit.y = FLOOR_Y;
                nearest = new HitResult(floorHit, new Vector3(0f, 1f, 0f), floorDistance);
            }
        }

        return nearest;
    }

    private Vector3 estimateNormal(BoundingBox bounds, Vector3 point) {
        if (Math.abs(point.x - bounds.min.x) <= FACE_EPSILON) {
            return new Vector3(-1f, 0f, 0f);
        }
        if (Math.abs(point.x - bounds.max.x) <= FACE_EPSILON) {
            return new Vector3(1f, 0f, 0f);
        }
        if (Math.abs(point.y - bounds.min.y) <= FACE_EPSILON) {
            return new Vector3(0f, -1f, 0f);
        }
        if (Math.abs(point.y - bounds.max.y) <= FACE_EPSILON) {
            return new Vector3(0f, 1f, 0f);
        }
        if (Math.abs(point.z - bounds.min.z) <= FACE_EPSILON) {
            return new Vector3(0f, 0f, -1f);
        }
        if (Math.abs(point.z - bounds.max.z) <= FACE_EPSILON) {
            return new Vector3(0f, 0f, 1f);
        }
        return new Vector3(0f, 1f, 0f);
    }

    public static final class PulseState {
        private final Vector3 origin;
        private final float baseIntensity;
        private final List<RayState> rays = new ArrayList<>();
        private final List<SurfaceGlow> glows = new ArrayList<>();
        private final float maxLifetimeSeconds;
        private float elapsedSeconds;

        private PulseState(Vector3 origin, float baseIntensity) {
            this.origin = origin;
            this.baseIntensity = baseIntensity;
            this.maxLifetimeSeconds = (((MAX_RAY_DISTANCE / TRAVEL_SPEED) * (MAX_BOUNCES + 1)) + 0.8f) * 1.35f;
        }

        public Vector3 origin() {
            return new Vector3(origin);
        }

        public List<RayState> rays() {
            return Collections.unmodifiableList(rays);
        }

        public List<SurfaceGlow> glows() {
            return Collections.unmodifiableList(glows);
        }
    }

    public static final class RayState {
        private final Vector3 origin;
        private final Vector3 direction;
        private final float intensity;
        private final int bounceCount;
        private final float maxTravelDistance;
        private Vector3 hitPoint;
        private Vector3 hitNormal;
        private float travelledDistance;
        private float hitDistance = -1f;
        private boolean finished;

        private RayState(Vector3 origin, Vector3 direction, float intensity, int bounceCount, float maxTravelDistance) {
            this.origin = origin;
            this.direction = direction;
            this.intensity = intensity;
            this.bounceCount = bounceCount;
            this.maxTravelDistance = maxTravelDistance;
        }

        public Vector3 origin() {
            return new Vector3(origin);
        }

        public Vector3 direction() {
            return new Vector3(direction);
        }

        public float intensity() {
            return intensity;
        }

        public int bounceCount() {
            return bounceCount;
        }

        public boolean hasHit() {
            return hitPoint != null;
        }

        public Vector3 hitPoint() {
            return hitPoint == null ? null : new Vector3(hitPoint);
        }

        public float travelledDistance() {
            return Math.min(travelledDistance, maxTravelDistance);
        }

        public float effectiveFrontDistance() {
            float front = travelledDistance();
            if (hitDistance > 0f) {
                front = Math.min(front, hitDistance);
            }
            return front;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    public static final class SurfaceGlow {
        private final Vector3 point;
        private final Vector3 normal;
        private final float peakIntensity;
        private final int bounceCount;
        private final float lifetimeSeconds;
        private float age;

        private SurfaceGlow(Vector3 point, Vector3 normal, float peakIntensity, int bounceCount) {
            this.point = new Vector3(point);
            this.normal = new Vector3(normal);
            this.peakIntensity = MathUtils.clamp(peakIntensity, 0f, 1f);
            this.bounceCount = bounceCount;
            this.lifetimeSeconds = (0.75f + (1f - this.peakIntensity) * 1.15f) * 1.2f;
            this.age = 0f;
        }

        public Vector3 point() {
            return new Vector3(point);
        }

        public Vector3 normal() {
            return new Vector3(normal);
        }

        public int bounceCount() {
            return bounceCount;
        }

        public float fadeProgress() {
            return MathUtils.clamp(age / lifetimeSeconds, 0f, 1f);
        }

        public float alpha() {
            float t = fadeProgress();
            return peakIntensity * (1f - t * t);
        }

        public boolean expired() {
            return age >= lifetimeSeconds;
        }
    }

    private record HitResult(Vector3 point, Vector3 normal, float distance) {
    }
}
