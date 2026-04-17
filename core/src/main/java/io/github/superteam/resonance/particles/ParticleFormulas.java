package io.github.superteam.resonance.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public final class ParticleFormulas {
    private ParticleFormulas() {
    }

    public static ParticleDefinition ring(Vector3 direction, float radius, float speed, Color color) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_Ring";
        definition.particleMeshType = ParticleMeshType.CAPSULE.name();
        definition.burstMode = true;
        definition.burstCount = 64;
        definition.emissionShape = ParticleEmissionShape.RING.name();
        definition.spawnRadius = Math.max(0f, radius);
        definition.ringAxisX = direction.x;
        definition.ringAxisY = direction.y;
        definition.ringAxisZ = direction.z;
        definition.directionMode = ParticleDirectionMode.OUTWARD.name();
        definition.spreadAngle = 5f;
        definition.speedMin = speed * 0.85f;
        definition.speedMax = speed * 1.15f;
        definition.lifetimeMin = 0.4f;
        definition.lifetimeMax = 0.9f;
        definition.startColor = new float[]{color.r, color.g, color.b, 1f};
        definition.endColor = new float[]{color.r, color.g, color.b, 0f};
        definition.blendMode = ParticleBlendMode.ADDITIVE.name();
        definition.velocityStretch = true;
        definition.velocityStretchFactor = 0.12f;
        definition.gravity = 0f;
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition explosion(
        float radius,
        float force,
        int count,
        Color innerColor,
        Color outerColor
    ) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_Explosion";
        definition.particleMeshType = ParticleMeshType.OCTAHEDRON.name();
        definition.burstMode = true;
        definition.burstCount = Math.max(1, count);
        definition.emissionShape = ParticleEmissionShape.SPHERE.name();
        definition.spawnRadius = radius * 0.1f;
        definition.directionMode = ParticleDirectionMode.OUTWARD.name();
        definition.speedMin = force * 0.5f;
        definition.speedMax = force;
        definition.lifetimeMin = 0.3f;
        definition.lifetimeMax = 1.0f;
        definition.startColor = new float[]{innerColor.r, innerColor.g, innerColor.b, 1f};
        definition.endColor = new float[]{outerColor.r, outerColor.g, outerColor.b, 0f};
        definition.blendMode = ParticleBlendMode.ADDITIVE.name();
        definition.gravity = -1.5f;
        definition.chaosStrength = 0.3f;
        definition.material.specularEnabled = true;
        definition.material.shininess = 18f;
        definition.material.specularStrength = 0.35f;
        definition.material.fresnelEnabled = true;
        definition.material.fresnelStrength = 0.45f;
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition cone(Vector3 direction, float spreadDeg, float speed, float lifetime, Color color) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_Cone";
        definition.particleMeshType = ParticleMeshType.TETRAHEDRON.name();
        definition.emissionShape = ParticleEmissionShape.POINT.name();
        definition.directionMode = ParticleDirectionMode.VECTOR.name();
        definition.directionX = direction.x;
        definition.directionY = direction.y;
        definition.directionZ = direction.z;
        definition.spreadAngle = spreadDeg;
        definition.speedMin = speed * 0.7f;
        definition.speedMax = speed;
        definition.lifetimeMin = lifetime * 0.8f;
        definition.lifetimeMax = lifetime;
        definition.startColor = new float[]{color.r, color.g, color.b, 1f};
        definition.endColor = new float[]{color.r * 0.3f, color.g * 0.3f, color.b * 0.3f, 0f};
        definition.blendMode = ParticleBlendMode.ADDITIVE.name();
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition sonarPulse(Color color) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_SonarPulse";
        definition.particleMeshType = ParticleMeshType.ICOSAHEDRON.name();
        definition.emissionShape = ParticleEmissionShape.RING.name();
        definition.ringAxisX = 0f;
        definition.ringAxisY = 1f;
        definition.ringAxisZ = 0f;
        definition.radialPulse = true;
        definition.radialPulseSpeed = 2.5f;
        definition.radialPulseMax = 5f;
        definition.directionMode = ParticleDirectionMode.OUTWARD.name();
        definition.emissionRate = 100f;
        definition.speedMin = 0.2f;
        definition.speedMax = 0.6f;
        definition.lifetimeMin = 0.5f;
        definition.lifetimeMax = 0.9f;
        definition.gravity = 0f;
        definition.waveEnabled = true;
        definition.waveFrequency = 2.5f;
        definition.waveAmplitude = 0.3f;
        definition.waveAxisX = 0f;
        definition.waveAxisY = 1f;
        definition.waveAxisZ = 0f;
        definition.startColor = new float[]{color.r, color.g, color.b, 0.9f};
        definition.endColor = new float[]{color.r, color.g, color.b, 0f};
        definition.blendMode = ParticleBlendMode.ADDITIVE.name();
        definition.material.edgeFadeStrength = 0.6f;
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition fountain(Color color, float upwardSpeed, float spreadDeg) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_Fountain";
        definition.particleMeshType = ParticleMeshType.ICOSAHEDRON.name();
        definition.emissionShape = ParticleEmissionShape.POINT.name();
        definition.directionMode = ParticleDirectionMode.VECTOR.name();
        definition.directionX = 0f;
        definition.directionY = 1f;
        definition.directionZ = 0f;
        definition.spreadAngle = spreadDeg;
        definition.emissionRate = 120f;
        definition.speedMin = upwardSpeed * 0.7f;
        definition.speedMax = upwardSpeed;
        definition.gravity = -3.5f;
        definition.startColor = new float[]{color.r, color.g, color.b, 0.9f};
        definition.endColor = new float[]{color.r * 0.2f, color.g * 0.2f, color.b * 0.2f, 0f};
        definition.blendMode = ParticleBlendMode.ALPHA.name();
        definition.material.fresnelEnabled = true;
        definition.material.fresnelStrength = 0.2f;
        definition.sanitize();
        return definition;
    }

    public static ParticleDefinition vortex(Color color, float radius, float chaos) {
        ParticleDefinition definition = new ParticleDefinition();
        definition.name = "Formula_Vortex";
        definition.particleMeshType = ParticleMeshType.TETRAHEDRON.name();
        definition.emissionShape = ParticleEmissionShape.RING.name();
        definition.spawnRadius = radius;
        definition.directionMode = ParticleDirectionMode.TANGENT.name();
        definition.emissionRate = 95f;
        definition.speedMin = 1.1f;
        definition.speedMax = 2.2f;
        definition.chaosStrength = chaos;
        definition.waveEnabled = true;
        definition.waveFrequency = 1.7f;
        definition.waveAmplitude = 0.45f;
        definition.waveAxisX = 0f;
        definition.waveAxisY = 1f;
        definition.waveAxisZ = 0f;
        definition.startColor = new float[]{color.r, color.g, color.b, 0.85f};
        definition.endColor = new float[]{color.r * 0.15f, color.g * 0.15f, color.b * 0.15f, 0f};
        definition.blendMode = ParticleBlendMode.ADDITIVE.name();
        definition.angularVelocityXMin = -120f;
        definition.angularVelocityXMax = 120f;
        definition.angularVelocityYMin = -120f;
        definition.angularVelocityYMax = 120f;
        definition.angularVelocityZMin = -120f;
        definition.angularVelocityZMax = 120f;
        definition.sanitize();
        return definition;
    }
}
