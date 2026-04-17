package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Animates a talking signal as a moving pulse with visible wall hits and bounced wave fronts.
 */
public final class SoundPulseVisualizer {
    private static final float PULSE_SPEED = 3.2f;
    private static final float PULSE_LIFETIME_SECONDS = 1.8f;
    private static final float MIN_PULSE_SPEED_SCALE = 0.45f;
    private static final float MAX_PULSE_SPEED_SCALE = 1.35f;
    private static final float REFLECTION_DISTANCE = 1.4f;
    private static final float BOUNCE_FADE = 0.72f;

    private final float roomHalfWidth;
    private final float roomHalfDepth;
    private final List<PulseState> activePulses = new ArrayList<>();

    public SoundPulseVisualizer(float roomHalfWidth, float roomHalfDepth) {
        if (roomHalfWidth <= 0f || roomHalfDepth <= 0f) {
            throw new IllegalArgumentException("Room dimensions must be positive.");
        }
        this.roomHalfWidth = roomHalfWidth;
        this.roomHalfDepth = roomHalfDepth;
    }

    public void activate(Vector3 sourceWorldPosition) {
        activate(sourceWorldPosition, 1f);
    }

    public void activate(Vector3 sourceWorldPosition, float normalizedStrength) {
        Vector3 sourcePosition = new Vector3();
        if (sourceWorldPosition == null) {
            sourcePosition.set(0f, 0.05f, 0f);
        } else {
            sourcePosition.set(sourceWorldPosition.x, sourceWorldPosition.y, sourceWorldPosition.z);
        }

        float clampedStrength = MathUtils.clamp(normalizedStrength, 0f, 1f);
        float pulseSpeedScale = MathUtils.lerp(MIN_PULSE_SPEED_SCALE, MAX_PULSE_SPEED_SCALE, clampedStrength);

        PulseState pulseState = new PulseState(sourcePosition, pulseSpeedScale);
        buildSignalArcs(pulseState);
        activePulses.add(pulseState);
    }

    public void update(float deltaSeconds) {
        if (activePulses.isEmpty()) {
            return;
        }

        float clampedDeltaSeconds = Math.max(0f, deltaSeconds);
        for (int index = activePulses.size() - 1; index >= 0; index--) {
            PulseState pulseState = activePulses.get(index);
            pulseState.elapsedSeconds += clampedDeltaSeconds;
            pulseState.radius = pulseState.elapsedSeconds * pulseState.pulseSpeed;
            pulseState.signalArcs.forEach(signalArc -> signalArc.update(pulseState.elapsedSeconds, pulseState.pulseSpeed));

            if (pulseState.elapsedSeconds >= PULSE_LIFETIME_SECONDS) {
                activePulses.remove(index);
            }
        }
    }

    public boolean isActive() {
        return !activePulses.isEmpty();
    }

    public float getRadius() {
        if (activePulses.isEmpty()) {
            return 0f;
        }
        return activePulses.get(activePulses.size() - 1).radius;
    }

    public Vector3 getSourcePosition() {
        if (activePulses.isEmpty()) {
            return new Vector3(0f, 0.05f, 0f);
        }
        return new Vector3(activePulses.get(activePulses.size() - 1).sourcePosition);
    }

    public List<Vector3> getHitPoints() {
        List<Vector3> hitPoints = new ArrayList<>();
        if (activePulses.isEmpty()) {
            return Collections.unmodifiableList(hitPoints);
        }

        for (SignalArc signalArc : activePulses.get(activePulses.size() - 1).signalArcs) {
            if (signalArc.hasHitWall()) {
                hitPoints.add(signalArc.hitPoint());
            }
        }
        return Collections.unmodifiableList(hitPoints);
    }

    public List<ReflectionRay> getReflectionRays() {
        List<ReflectionRay> reflectionRays = new ArrayList<>();
        if (activePulses.isEmpty()) {
            return Collections.unmodifiableList(reflectionRays);
        }

        for (SignalArc signalArc : activePulses.get(activePulses.size() - 1).signalArcs) {
            reflectionRays.add(signalArc.toReflectionRay());
        }
        return Collections.unmodifiableList(reflectionRays);
    }

    public List<SignalArc> getSignalArcs() {
        if (activePulses.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(activePulses.get(activePulses.size() - 1).signalArcs);
    }

    public List<PulseView> pulseViews() {
        List<PulseView> pulseViews = new ArrayList<>(activePulses.size());
        for (PulseState pulseState : activePulses) {
            List<SignalArc> signalArcs = Collections.unmodifiableList(new ArrayList<>(pulseState.signalArcs));
            pulseViews.add(new PulseView(new Vector3(pulseState.sourcePosition), pulseState.radius, signalArcs));
        }
        return Collections.unmodifiableList(pulseViews);
    }

    private void buildSignalArcs(PulseState pulseState) {
        float[] angles = {0f, 32f, 64f, 96f, 128f, 160f, 192f, 224f, 256f, 288f, 320f, 352f};
        for (float angle : angles) {
            Vector3 direction = new Vector3(MathUtils.cosDeg(angle), 0f, MathUtils.sinDeg(angle)).nor();
            Vector3 hitPoint = computeWallHit(pulseState.sourcePosition, direction);
            Vector3 reflectedDirection = computeReflection(direction, hitPoint);
            Vector3 reflectionEnd = new Vector3(hitPoint).mulAdd(reflectedDirection, REFLECTION_DISTANCE);
            pulseState.signalArcs.add(new SignalArc(new Vector3(pulseState.sourcePosition), direction, hitPoint, reflectionEnd));
        }
    }

    private Vector3 computeWallHit(Vector3 sourcePosition, Vector3 direction) {
        float tMin = Float.POSITIVE_INFINITY;

        if (Math.abs(direction.x) > 0.0001f) {
            float wallX = direction.x > 0f ? roomHalfWidth : -roomHalfWidth;
            float tx = (wallX - sourcePosition.x) / direction.x;
            if (tx > 0f) {
                tMin = Math.min(tMin, tx);
            }
        }

        if (Math.abs(direction.z) > 0.0001f) {
            float wallZ = direction.z > 0f ? roomHalfDepth : -roomHalfDepth;
            float tz = (wallZ - sourcePosition.z) / direction.z;
            if (tz > 0f) {
                tMin = Math.min(tMin, tz);
            }
        }

        if (!Float.isFinite(tMin)) {
            tMin = 0f;
        }

        return new Vector3(sourcePosition).mulAdd(direction, tMin);
    }

    private Vector3 computeReflection(Vector3 direction, Vector3 hitPoint) {
        Vector3 normal = Math.abs(direction.x) >= Math.abs(direction.z)
            ? new Vector3(Math.signum(direction.x) > 0f ? -1f : 1f, 0f, 0f)
            : new Vector3(0f, 0f, Math.signum(direction.z) > 0f ? -1f : 1f);
        float dot = direction.dot(normal);
        return new Vector3(direction).mulAdd(normal, -2f * dot).nor();
    }

    public static final class SignalArc {
        private final Vector3 start;
        private final Vector3 direction;
        private final Vector3 hitPoint;
        private final Vector3 reflectionEnd;
        private final float wallDistance;
        private final float reflectionDistance;
        private float travelDistance;
        private float bounceDistance;
        private boolean bounced;

        SignalArc(Vector3 start, Vector3 direction, Vector3 hitPoint, Vector3 reflectionEnd) {
            this.start = new Vector3(start);
            this.direction = new Vector3(direction);
            this.hitPoint = new Vector3(hitPoint);
            this.reflectionEnd = new Vector3(reflectionEnd);
            this.wallDistance = start.dst(hitPoint);
            this.reflectionDistance = hitPoint.dst(reflectionEnd);
            this.travelDistance = 0f;
        }

        void update(float elapsedSeconds, float pulseSpeed) {
            travelDistance = elapsedSeconds * pulseSpeed;
            bounced = travelDistance >= wallDistance;
            if (bounced) {
                bounceDistance = Math.min(reflectionDistance, travelDistance - wallDistance);
            }
        }

        public Vector3 getCurrentPosition() {
            if (!bounced) {
                return new Vector3(start).mulAdd(direction, travelDistance);
            }

            Vector3 bounceDirection = new Vector3(reflectionEnd).sub(hitPoint).nor();
            return new Vector3(hitPoint).mulAdd(bounceDirection, bounceDistance);
        }

        public boolean hasHitWall() {
            return travelDistance >= wallDistance;
        }

        public Vector3 hitPoint() {
            return new Vector3(hitPoint);
        }

        public float wallAlpha() {
            float impactProgress = MathUtils.clamp(travelDistance / Math.max(0.0001f, wallDistance), 0f, 1f);
            return 1f - (impactProgress * 0.4f);
        }

        public float bounceAlpha() {
            if (!bounced) {
                return 0f;
            }
            float bounceProgress = MathUtils.clamp(bounceDistance / Math.max(0.0001f, reflectionDistance), 0f, 1f);
            return BOUNCE_FADE * (1f - bounceProgress * 0.55f);
        }

        ReflectionRay toReflectionRay() {
            return new ReflectionRay(start, hitPoint, reflectionEnd);
        }
    }

    public static final class ReflectionRay {
        private final Vector3 start;
        private final Vector3 hitPoint;
        private final Vector3 reflectionEnd;

        ReflectionRay(Vector3 start, Vector3 hitPoint, Vector3 reflectionEnd) {
            this.start = new Vector3(start);
            this.hitPoint = new Vector3(hitPoint);
            this.reflectionEnd = new Vector3(reflectionEnd);
        }

        public Vector3 getStart() {
            return new Vector3(start);
        }

        public Vector3 getHitPoint() {
            return new Vector3(hitPoint);
        }

        public Vector3 getReflectionEnd() {
            return new Vector3(reflectionEnd);
        }
    }

    private static final class PulseState {
        private final Vector3 sourcePosition;
        private final List<SignalArc> signalArcs = new ArrayList<>();
        private final float pulseSpeed;
        private float elapsedSeconds;
        private float radius;

        private PulseState(Vector3 sourcePosition, float pulseSpeedScale) {
            this.sourcePosition = new Vector3(sourcePosition);
            this.pulseSpeed = PULSE_SPEED * pulseSpeedScale;
        }
    }

    public record PulseView(Vector3 sourcePosition, float radius, List<SignalArc> signalArcs) {
    }
}
