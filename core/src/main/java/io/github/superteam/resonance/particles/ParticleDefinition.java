package io.github.superteam.resonance.particles;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import java.util.Arrays;

public class ParticleDefinition {
    public int presetVersion = 1;

    public String name = "Default";
    public String particleMeshType = ParticleMeshType.CUBE.name();
    public String customMeshPath = "";
    public float emissionRate = 80f;
    public float[][] emissionCurve = null;
    public float emissionDuration = 0f;
    public float emissionStartDelay = 0f;

    public boolean burstMode = false;
    public int burstCount = 64;
    public boolean burstLoop = false;
    public float burstInterval = 1.5f;
    public float burstStaggerDuration = 0f;

    public float warmupDuration = 0f;
    public float warmupTimeStep = 0.05f;

    public int maxParticles = 600;

    public float lifetimeMin = 1.2f;
    public float lifetimeMax = 2.4f;

    // Legacy uniform scale values remain for backward compatibility.
    public float startSizeMin = 0.06f;
    public float startSizeMax = 0.18f;
    public float endSizeMin = 0.01f;
    public float endSizeMax = 0.08f;

    public float startScaleXMin = 0.06f;
    public float startScaleXMax = 0.18f;
    public float startScaleYMin = 0.06f;
    public float startScaleYMax = 0.18f;
    public float startScaleZMin = 0.06f;
    public float startScaleZMax = 0.18f;

    public float endScaleXMin = 0.01f;
    public float endScaleXMax = 0.08f;
    public float endScaleYMin = 0.01f;
    public float endScaleYMax = 0.08f;
    public float endScaleZMin = 0.01f;
    public float endScaleZMax = 0.08f;

    public float angularVelocityXMin = -90f;
    public float angularVelocityXMax = 90f;
    public float angularVelocityYMin = -90f;
    public float angularVelocityYMax = 90f;
    public float angularVelocityZMin = -90f;
    public float angularVelocityZMax = 90f;
    public float angularDamping = 0f;

    public boolean lockRotationX = false;
    public boolean lockRotationY = false;
    public boolean lockRotationZ = false;

    public float speedMin = 1.5f;
    public float speedMax = 3.8f;
    public float spreadAngle = 80f;
    public float[][] velocityCurve = null;

    public float randomStrength = 0.35f;
    public float chaosStrength = 0.0f;
    public int multiDirectionCount = 6;
    public boolean turbulenceEnabled = false;
    public float turbulenceStrength = 0.5f;
    public float turbulenceScale = 0.4f;
    public float turbulenceSpeed = 0.2f;

    public boolean waveEnabled = false;
    public float waveFrequency = 2f;
    public float waveAmplitude = 1f;
    public float waveAxisX = 1f;
    public float waveAxisY = 0f;
    public float waveAxisZ = 0f;

    public boolean radialPulse = false;
    public float radialPulseSpeed = 1.5f;
    public float radialPulseMax = 3f;

    public String emissionShape = "POINT";
    public float spawnRadius = 0.35f;
    public float shapeScaleX = 1f;
    public float shapeScaleY = 1f;
    public float shapeScaleZ = 1f;

    public float ringAxisX = 0f;
    public float ringAxisY = 1f;
    public float ringAxisZ = 0f;

    public float lineEndX = 1f;
    public float lineEndY = 0f;
    public float lineEndZ = 0f;
    public float coneHalfAngle = 30f;
    public float torusMajorRadius = 0.5f;
    public float torusMinorRadius = 0.15f;

    public String directionMode = "UP";
    public float directionX = 0f;
    public float directionY = 1f;
    public float directionZ = 0f;
    public float inheritVelocityFactor = 0f;

    public float gravity = -3.5f;
    public float windX = 0f;
    public float windY = 0f;
    public float windZ = 0f;
    public float drag = 0f;
    public float gravityScaleMin = 1f;
    public float gravityScaleMax = 1f;

    public boolean lodEnabled = false;
    public float lodDistanceMin = 10f;
    public float lodDistanceMax = 50f;

    public boolean collisionEnabled = false;
    public ParticleCollisionResponse collisionResponse = ParticleCollisionResponse.DIE;
    public float bounceDamping = 0.35f;
    public float stickLifeExtension = 0f;

    public boolean useNormals = true;
    public float emissiveStrength = 1.4f;
    public float emissiveFalloff = 0.18f;
    public float[][] emissiveCurve = null;
    public float[][] rotationCurve = null;
    public float[][] sizeCurve = null;

    public float[] startColor = {1f, 0.65f, 0.2f, 1f};
    public float[] endColor = {0.95f, 0.15f, 0.05f, 0f};
    public float[][] colorGradient = null;

    public float[] emissiveColor = {1f, 0.8f, 0.3f};
    public String blendMode = "ADDITIVE";
    public String depthMode = DepthMode.WRITE.name();
    public boolean depthSort = false;
    public boolean retroEffect = false;
    public String billboardMode = ParticleBillboardMode.SPHERICAL.name();

    public boolean velocityStretch = false;
    public float velocityStretchFactor = 0.25f;
    public float velocityStretchMin = 0f;
    public float velocityStretchMax = 3f;
    public int velocityStretchAxis = 1;

    public boolean softParticlesEnabled = false;
    public float softParticleFadeDistance = 0.45f;

    public boolean gpuInstancingEnabled = true;

    public ParticleMaterial material = new ParticleMaterial();

    public String onDeathPreset = "";
    public String onBouncePreset = "";
    public int subEmitterBurst = 4;

    public static ParticleDefinition createDefault() {
        ParticleDefinition definition = new ParticleDefinition();
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition load(FileHandle file) {
        Json json = new Json();
        json.setIgnoreUnknownFields(true);
        if (!file.exists()) {
            ParticleDefinition definition = createDefault();
            definition.save(file);
            return definition;
        }
        ParticleDefinition definition;
        try {
            definition = json.fromJson(ParticleDefinition.class, file);
        } catch (SerializationException exception) {
            // Preserve startup when local presets are stale/corrupt by falling back to defaults.
            ParticleDefinition fallback = createDefault();
            fallback.save(file);
            return fallback;
        }
        if (definition == null) {
            return createDefault();
        }
        definition.sanitize();
        return definition;
    }

    public void save(FileHandle file) {
        sanitize();
        Json json = new Json();
        if (file.parent() != null) {
            file.parent().mkdirs();
        }
        file.writeString(json.prettyPrint(this), false);
    }

    public void sanitize() {
        presetVersion = Math.max(1, presetVersion);

        if (name == null || name.isBlank()) {
            name = "Default";
        }
        particleMeshType = ParticleMeshType.fromName(particleMeshType).name();
        if (customMeshPath == null) {
            customMeshPath = "";
        }

        emissionRate = Math.max(0f, emissionRate);
        emissionDuration = Math.max(0f, emissionDuration);
        emissionStartDelay = Math.max(0f, emissionStartDelay);
        emissionCurve = sanitizeCurve(emissionCurve, false);

        burstCount = Math.max(1, burstCount);
        burstInterval = Math.max(0.01f, burstInterval);
        burstStaggerDuration = Math.max(0f, burstStaggerDuration);

        warmupDuration = Math.max(0f, warmupDuration);
        warmupTimeStep = Math.max(0.01f, warmupTimeStep);

        maxParticles = Math.max(1, maxParticles);

        lifetimeMin = Math.max(0.02f, lifetimeMin);
        lifetimeMax = Math.max(lifetimeMin, lifetimeMax);

        startSizeMin = Math.max(0.001f, startSizeMin);
        startSizeMax = Math.max(startSizeMin, startSizeMax);
        endSizeMin = Math.max(0.001f, endSizeMin);
        endSizeMax = Math.max(endSizeMin, endSizeMax);

        startScaleXMin = Math.max(0.001f, startScaleXMin);
        startScaleXMax = Math.max(startScaleXMin, startScaleXMax);
        startScaleYMin = Math.max(0.001f, startScaleYMin);
        startScaleYMax = Math.max(startScaleYMin, startScaleYMax);
        startScaleZMin = Math.max(0.001f, startScaleZMin);
        startScaleZMax = Math.max(startScaleZMin, startScaleZMax);

        endScaleXMin = Math.max(0.001f, endScaleXMin);
        endScaleXMax = Math.max(endScaleXMin, endScaleXMax);
        endScaleYMin = Math.max(0.001f, endScaleYMin);
        endScaleYMax = Math.max(endScaleYMin, endScaleYMax);
        endScaleZMin = Math.max(0.001f, endScaleZMin);
        endScaleZMax = Math.max(endScaleZMin, endScaleZMax);

        angularVelocityXMax = Math.max(angularVelocityXMin, angularVelocityXMax);
        angularVelocityYMax = Math.max(angularVelocityYMin, angularVelocityYMax);
        angularVelocityZMax = Math.max(angularVelocityZMin, angularVelocityZMax);
        angularDamping = clampRange(angularDamping, 0f, 1f);

        speedMin = Math.max(0f, speedMin);
        speedMax = Math.max(speedMin, speedMax);
        spreadAngle = Math.max(0f, Math.min(179f, spreadAngle));
        velocityCurve = sanitizeCurve(velocityCurve, true);

        randomStrength = clampRange(randomStrength, 0f, 1f);
        chaosStrength = clampRange(chaosStrength, 0f, 3f);
        multiDirectionCount = Math.max(1, Math.min(24, multiDirectionCount));
        turbulenceStrength = Math.max(0f, turbulenceStrength);
        turbulenceScale = Math.max(0.001f, turbulenceScale);
        turbulenceSpeed = Math.max(0f, turbulenceSpeed);

        waveFrequency = Math.max(0f, waveFrequency);
        waveAmplitude = Math.max(0f, waveAmplitude);
        normalizeWaveAxis();

        spawnRadius = Math.max(0f, spawnRadius);
        shapeScaleX = Math.max(0.001f, shapeScaleX);
        shapeScaleY = Math.max(0.001f, shapeScaleY);
        shapeScaleZ = Math.max(0.001f, shapeScaleZ);

        radialPulseSpeed = Math.max(0f, radialPulseSpeed);
        radialPulseMax = Math.max(spawnRadius, radialPulseMax);

        normalizeRingAxis();

        coneHalfAngle = clampRange(coneHalfAngle, 0f, 89f);
        torusMajorRadius = Math.max(0f, torusMajorRadius);
        torusMinorRadius = Math.max(0f, torusMinorRadius);

        bounceDamping = Math.max(0f, Math.min(1f, bounceDamping));
        emissiveStrength = Math.max(0f, emissiveStrength);
        emissiveFalloff = Math.max(0f, emissiveFalloff);

        startColor = normalizeArray(startColor, 4, new float[]{1f, 0.65f, 0.2f, 1f});
        endColor = normalizeArray(endColor, 4, new float[]{0.95f, 0.15f, 0.05f, 0f});
        colorGradient = sanitizeGradient(colorGradient);
        emissiveColor = normalizeArray(emissiveColor, 3, new float[]{1f, 0.8f, 0.3f});

        if (onDeathPreset == null) {
            onDeathPreset = "";
        }
        if (onBouncePreset == null) {
            onBouncePreset = "";
        }
        subEmitterBurst = Math.max(1, subEmitterBurst);
        stickLifeExtension = Math.max(0f, stickLifeExtension);

        if (collisionResponse == null) {
            collisionResponse = ParticleCollisionResponse.DIE;
        }
        collisionResponse = ParticleCollisionResponse.fromName(collisionResponse.name());

        if (blendMode == null || blendMode.isBlank()) {
            blendMode = ParticleBlendMode.ADDITIVE.name();
        }
        blendMode = ParticleBlendMode.fromName(blendMode).name();
        depthMode = DepthMode.fromName(depthMode).name();
        billboardMode = ParticleBillboardMode.fromName(billboardMode).name();

        velocityStretchFactor = Math.max(0f, velocityStretchFactor);
        velocityStretchMin = Math.max(0f, velocityStretchMin);
        velocityStretchMax = Math.max(velocityStretchMin, velocityStretchMax);
        velocityStretchAxis = Math.max(0, Math.min(2, velocityStretchAxis));

        if (emissionShape == null || emissionShape.isBlank()) {
            emissionShape = ParticleEmissionShape.POINT.name();
        }
        emissionShape = ParticleEmissionShape.fromName(emissionShape).name();

        if (directionMode == null || directionMode.isBlank()) {
            directionMode = ParticleDirectionMode.UP.name();
        }
        directionMode = ParticleDirectionMode.fromName(directionMode).name();
        inheritVelocityFactor = clampRange(inheritVelocityFactor, 0f, 4f);
        normalizeDirectionVector();

        drag = Math.max(0f, drag);

        gravityScaleMin = Math.max(0f, gravityScaleMin);
        gravityScaleMax = Math.max(gravityScaleMin, gravityScaleMax);

        lodDistanceMin = Math.max(0f, lodDistanceMin);
        lodDistanceMax = Math.max(lodDistanceMin, lodDistanceMax);

        emissiveCurve = sanitizeCurve(emissiveCurve, true);
        rotationCurve = sanitizeCurve(rotationCurve, true);
        sizeCurve = sanitizeCurve(sizeCurve, true);
        softParticleFadeDistance = Math.max(0.001f, softParticleFadeDistance);

        if (material == null) {
            material = new ParticleMaterial();
        }
        material.sanitize();
    }

    public float sampleEmissionRate(float elapsedSeconds) {
        if (emissionDuration > 0f && elapsedSeconds > emissionDuration) {
            return 0f;
        }
        return sampleCurveValue(emissionCurve, Math.max(0f, elapsedSeconds), emissionRate);
    }

    public float sampleVelocityMultiplier(float ageRatio) {
        return sampleCurveValue(velocityCurve, clamp01(ageRatio), 1f);
    }

    public float sampleEmissiveMultiplier(float ageRatio) {
        return sampleCurveValue(emissiveCurve, clamp01(ageRatio), 1f);
    }

    public float sampleRotationMultiplier(float ageRatio) {
        return sampleCurveValue(rotationCurve, clamp01(ageRatio), 1f);
    }

    public float sampleSizeMultiplier(float ageRatio) {
        return sampleCurveValue(sizeCurve, clamp01(ageRatio), 1f);
    }

    public float[] sampleGradient(float ageRatio) {
        float[] color = new float[4];
        sampleGradient(ageRatio, color);
        return color;
    }

    public void sampleGradient(float ageRatio, float[] outColor) {
        if (outColor == null || outColor.length < 4) {
            throw new IllegalArgumentException("Output color buffer must have length >= 4");
        }

        float clampedAge = clamp01(ageRatio);
        if (colorGradient == null || colorGradient.length < 2) {
            outColor[0] = MathUtils.lerp(startColor[0], endColor[0], clampedAge);
            outColor[1] = MathUtils.lerp(startColor[1], endColor[1], clampedAge);
            outColor[2] = MathUtils.lerp(startColor[2], endColor[2], clampedAge);
            outColor[3] = MathUtils.lerp(startColor[3], endColor[3], clampedAge);
            return;
        }

        if (clampedAge <= colorGradient[0][0]) {
            copyGradientStop(colorGradient[0], outColor);
            return;
        }

        for (int i = 0; i < colorGradient.length - 1; i++) {
            float[] left = colorGradient[i];
            float[] right = colorGradient[i + 1];
            if (clampedAge <= right[0]) {
                float range = Math.max(0.0001f, right[0] - left[0]);
                float t = clamp01((clampedAge - left[0]) / range);
                outColor[0] = MathUtils.lerp(left[1], right[1], t);
                outColor[1] = MathUtils.lerp(left[2], right[2], t);
                outColor[2] = MathUtils.lerp(left[3], right[3], t);
                outColor[3] = MathUtils.lerp(left[4], right[4], t);
                return;
            }
        }

        copyGradientStop(colorGradient[colorGradient.length - 1], outColor);
    }

    private static float[] normalizeArray(float[] values, int expectedLength, float[] fallback) {
        float[] output = new float[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            float fallbackValue = fallback[i];
            if (values != null && i < values.length) {
                output[i] = clamp01(values[i]);
            } else {
                output[i] = fallbackValue;
            }
        }
        return output;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float clampRange(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float[][] sanitizeGradient(float[][] gradient) {
        if (gradient == null || gradient.length < 2) {
            return null;
        }

        int validCount = 0;
        for (float[] stop : gradient) {
            if (stop != null && stop.length >= 5) {
                validCount++;
            }
        }

        if (validCount < 2) {
            return null;
        }

        float[][] sanitized = new float[validCount][5];
        int index = 0;
        for (float[] stop : gradient) {
            if (stop == null || stop.length < 5) {
                continue;
            }
            sanitized[index][0] = clamp01(stop[0]);
            sanitized[index][1] = clamp01(stop[1]);
            sanitized[index][2] = clamp01(stop[2]);
            sanitized[index][3] = clamp01(stop[3]);
            sanitized[index][4] = clamp01(stop[4]);
            index++;
        }

        Arrays.sort(sanitized, (left, right) -> Float.compare(left[0], right[0]));
        return sanitized;
    }

    private static float[][] sanitizeCurve(float[][] curve, boolean clampXToUnitRange) {
        if (curve == null || curve.length < 2) {
            return null;
        }

        int validCount = 0;
        for (float[] stop : curve) {
            if (stop != null && stop.length >= 2) {
                validCount++;
            }
        }

        if (validCount < 2) {
            return null;
        }

        float[][] sanitized = new float[validCount][2];
        int index = 0;
        for (float[] stop : curve) {
            if (stop == null || stop.length < 2) {
                continue;
            }

            float x = stop[0];
            if (clampXToUnitRange) {
                x = clamp01(x);
            } else {
                x = Math.max(0f, x);
            }

            sanitized[index][0] = x;
            sanitized[index][1] = Math.max(0f, stop[1]);
            index++;
        }

        Arrays.sort(sanitized, (left, right) -> Float.compare(left[0], right[0]));
        return sanitized;
    }

    private static void copyGradientStop(float[] stop, float[] outColor) {
        outColor[0] = stop[1];
        outColor[1] = stop[2];
        outColor[2] = stop[3];
        outColor[3] = stop[4];
    }

    private static float sampleCurveValue(float[][] curve, float sampleX, float fallback) {
        if (curve == null || curve.length == 0) {
            return fallback;
        }

        if (curve.length == 1 || sampleX <= curve[0][0]) {
            return curve[0][1];
        }

        for (int i = 0; i < curve.length - 1; i++) {
            float[] left = curve[i];
            float[] right = curve[i + 1];
            if (sampleX <= right[0]) {
                float range = Math.max(0.0001f, right[0] - left[0]);
                float t = clamp01((sampleX - left[0]) / range);
                return MathUtils.lerp(left[1], right[1], t);
            }
        }

        return curve[curve.length - 1][1];
    }

    private void normalizeDirectionVector() {
        double length = Math.sqrt(
            (directionX * directionX)
                + (directionY * directionY)
                + (directionZ * directionZ)
        );

        if (length < 0.00001d) {
            directionX = 0f;
            directionY = 1f;
            directionZ = 0f;
            return;
        }

        directionX = (float) (directionX / length);
        directionY = (float) (directionY / length);
        directionZ = (float) (directionZ / length);
    }

    private void normalizeRingAxis() {
        double length = Math.sqrt(
            (ringAxisX * ringAxisX)
                + (ringAxisY * ringAxisY)
                + (ringAxisZ * ringAxisZ)
        );

        if (length < 0.00001d) {
            ringAxisX = 0f;
            ringAxisY = 1f;
            ringAxisZ = 0f;
            return;
        }

        ringAxisX = (float) (ringAxisX / length);
        ringAxisY = (float) (ringAxisY / length);
        ringAxisZ = (float) (ringAxisZ / length);
    }

    private void normalizeWaveAxis() {
        double length = Math.sqrt(
            (waveAxisX * waveAxisX)
                + (waveAxisY * waveAxisY)
                + (waveAxisZ * waveAxisZ)
        );

        if (length < 0.00001d) {
            waveAxisX = 1f;
            waveAxisY = 0f;
            waveAxisZ = 0f;
            return;
        }

        waveAxisX = (float) (waveAxisX / length);
        waveAxisY = (float) (waveAxisY / length);
        waveAxisZ = (float) (waveAxisZ / length);
    }
}
