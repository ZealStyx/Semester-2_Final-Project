package io.github.superteam.resonance.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class ParticleEmitter {
    private static final VertexAttribute[] INSTANCE_ATTRIBUTES = {
        new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_instancePosition"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_instanceScaleXYZ"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_instanceOrientationQuat"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_instanceColor"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_instanceNormal"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_instanceAgeRatio"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_instanceEmissiveStrength")
    };

    private static final int INSTANCE_STRIDE = 19;

    private static final Vector3[] MULTI_DIRECTIONS = {
        new Vector3(1f, 0f, 0f),
        new Vector3(-1f, 0f, 0f),
        new Vector3(0f, 1f, 0f),
        new Vector3(0f, -1f, 0f),
        new Vector3(0f, 0f, 1f),
        new Vector3(0f, 0f, -1f),
        new Vector3(1f, 1f, 0f).nor(),
        new Vector3(-1f, 1f, 0f).nor(),
        new Vector3(0f, 1f, 1f).nor(),
        new Vector3(0f, 1f, -1f).nor(),
        new Vector3(1f, 0f, 1f).nor(),
        new Vector3(-1f, 0f, 1f).nor()
    };

    private final Array<ParticleInstance> activeParticles = new Array<>(false, 512);
    private final Pool<ParticleInstance> particlePool = new Pool<>(64, 2048) {
        @Override
        protected ParticleInstance newObject() {
            return new ParticleInstance();
        }
    };

    private Mesh particleMesh;
    private final Vector3 emitterPosition = new Vector3(0f, 0.35f, 0f);
    private final Vector3 lightDirection = new Vector3(0.35f, 1f, 0.2f).nor();
    private final Vector3 lightColor = new Vector3(1f, 1f, 1f);

    private final Vector3 spawnOffset = new Vector3();
    private final Vector3 directionBuffer = new Vector3();
    private final Vector3 randomBuffer = new Vector3();
    private final Vector3 forceBuffer = new Vector3();
    private final Vector3 collisionPointBuffer = new Vector3();
    private final Vector3 collisionNormalBuffer = new Vector3();
    private final Vector3 ringAxisBuffer = new Vector3(0f, 1f, 0f);
    private final Vector3 billboardToCamera = new Vector3();
    private final Vector3 billboardDirection = new Vector3();
    private final Vector3 billboardDirectionXZ = new Vector3();
    private final Vector3 velocityDirectionBuffer = new Vector3();
    private final Vector3 angularDeltaEulerDegrees = new Vector3();
    private final Quaternion orientationQuaternion = new Quaternion();
    private final Quaternion orientationDeltaQuaternion = new Quaternion();
    private final Quaternion renderOrientation = new Quaternion();
    private final Quaternion horizontalOrientation = new Quaternion().setEulerAngles(0f, 0f, -90f);
    private final Matrix4 identityTransform = new Matrix4();
    private final float[] gradientSample = new float[4];
    private final Vector3 sortCameraPos = new Vector3();
    private final Vector3 lastEmitterPosition = new Vector3();
    private final Vector3 emitterVelocity = new Vector3();
    private final TurbulenceField inlineTurbulenceField = new TurbulenceField();

    private ParticleDefinition definition;
    private Texture depthTexture;
    private SubEmitterListener subListener;
    private final Array<CollisionPlane> collisionPlanes = new Array<>(false, 4);

    private float emissionAccumulator;
    private float elapsedTime;
    private float emissionElapsed;
    private float burstTimer;
    private float pulseTimer;
    private float liveSpawnRadius;
    private float burstStaggerAccumulator;
    private float lodFactor = 1f;
    private boolean burstFired;
    private int burstRemainingCount;
    private boolean firstVelocitySample = true;
    private boolean emissionStoppedNotified;
    private boolean emitterCompleteNotified;
    private int multiDirectionCursor;
    private boolean instancedRenderingEnabled;
    private float[] instanceData;
    private float softParticleFadeDistance;
    private float viewportWidth;
    private float viewportHeight;
    private EmitterLifecycleListener lifecycleListener;

    public interface SubEmitterListener {
        void onParticleDeath(Vector3 position, String presetName, int count);

        void onParticleBounce(Vector3 position, String presetName, int count);
    }

    public interface EmitterLifecycleListener {
        void onEmitterComplete(ParticleEmitter emitter);

        void onEmissionStopped(ParticleEmitter emitter);
    }

    public ParticleEmitter(ParticleDefinition definition) {
        setDefinition(definition);
    }

    public void setDefinition(ParticleDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Particle definition must not be null");
        }

        this.definition = definition;
        this.definition.sanitize();

        rebuildParticleMesh();

        emissionAccumulator = 0f;
        emissionElapsed = 0f;
        burstTimer = 0f;
        pulseTimer = 0f;
        burstFired = false;
        burstStaggerAccumulator = 0f;
        burstRemainingCount = 0;
        firstVelocitySample = true;
        emissionStoppedNotified = false;
        emitterCompleteNotified = false;
        lodFactor = 1f;
        emitterVelocity.setZero();
        lastEmitterPosition.set(emitterPosition);
        liveSpawnRadius = this.definition.spawnRadius;
        multiDirectionCursor = 0;
        softParticleFadeDistance = this.definition.softParticleFadeDistance;
        inlineTurbulenceField
            .setStrength(this.definition.turbulenceStrength)
            .setScale(this.definition.turbulenceScale)
            .setSpeed(this.definition.turbulenceSpeed);
        configureRenderingState();

        if (this.definition.warmupDuration > 0f) {
            warmup(this.definition.warmupDuration, null);
        }
    }

    public void setLight(Vector3 direction, Vector3 color) {
        if (direction != null && !direction.isZero(0.0001f)) {
            lightDirection.set(direction).nor();
        }
        if (color != null) {
            lightColor.set(color);
        }
    }

    public void setEmitterPosition(float x, float y, float z) {
        emitterPosition.set(x, y, z);
    }

    public void addCollisionPlane(CollisionPlane collisionPlane) {
        if (collisionPlane == null) {
            throw new IllegalArgumentException("Collision plane must not be null");
        }

        collisionPlanes.add(new CollisionPlane(collisionPlane.getNormal(new Vector3()), collisionPlane.getDistance()));
    }

    public void clearCollisionPlanes() {
        collisionPlanes.clear();
    }

    public void setCollisionPlanes(Array<CollisionPlane> planes) {
        collisionPlanes.clear();
        if (planes == null) {
            return;
        }

        for (int i = 0; i < planes.size; i++) {
            addCollisionPlane(planes.get(i));
        }
    }

    public void setDepthTexture(Texture depthTexture, float viewportWidth, float viewportHeight) {
        this.depthTexture = depthTexture;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void clearDepthTexture() {
        depthTexture = null;
        viewportWidth = 0f;
        viewportHeight = 0f;
    }

    public void setSubEmitterListener(SubEmitterListener listener) {
        this.subListener = listener;
    }

    public void setLifecycleListener(EmitterLifecycleListener lifecycleListener) {
        this.lifecycleListener = lifecycleListener;
    }

    public void setLodFactor(float lodFactor) {
        this.lodFactor = MathUtils.clamp(lodFactor, 0f, 1f);
    }

    public float getLodFactor() {
        return lodFactor;
    }

    public float computeLodFactor(float distanceToCamera) {
        if (!definition.lodEnabled) {
            return 1f;
        }

        if (distanceToCamera <= definition.lodDistanceMin) {
            return 1f;
        }

        float lodRange = Math.max(0.0001f, definition.lodDistanceMax - definition.lodDistanceMin);
        float normalizedDistance = (distanceToCamera - definition.lodDistanceMin) / lodRange;
        return 1f - MathUtils.clamp(normalizedDistance, 0f, 1f);
    }

    public void warmup(float seconds, ForceFieldProvider forceFieldProvider) {
        if (seconds <= 0f) {
            return;
        }

        float step = Math.max(0.01f, definition.warmupTimeStep);
        int ticks = (int) (seconds / step);
        for (int i = 0; i < ticks; i++) {
            update(step, forceFieldProvider);
        }
    }

    public void update(float delta) {
        update(delta, null);
    }

    public void update(float delta, ForceFieldProvider forceFieldProvider) {
        if (delta <= 0f) {
            return;
        }

        elapsedTime += delta;
        emissionElapsed += delta;
        updateEmitterVelocity(delta);
        emitNewParticles(delta);
        updateActiveParticles(delta, forceFieldProvider);
        updateLifecycleState();
    }

    public void render(ShaderProgram shaderProgram, PerspectiveCamera camera) {
        render(shaderProgram, camera, depthTexture, viewportWidth, viewportHeight);
    }

    public void render(ShaderProgram shaderProgram, PerspectiveCamera camera, Texture depthTexture, float viewportWidth, float viewportHeight) {
        if (activeParticles.size == 0) {
            return;
        }

        ParticleBillboardMode billboardMode = getActiveBillboardMode();

        ParticleBlendMode blendMode = ParticleBlendMode.fromName(definition.blendMode);
        if (blendMode == ParticleBlendMode.ALPHA && definition.depthSort && activeParticles.size > 1 && camera != null) {
            sortParticlesByDepth(camera);
        }

        blendMode.apply();
        DepthMode depthMode = DepthMode.fromName(definition.depthMode);
        Gdx.gl.glDepthMask(depthMode == DepthMode.WRITE);

        shaderProgram.bind();
        shaderProgram.setUniformMatrix("u_projViewTrans", camera.combined);
        shaderProgram.setUniformf("u_cameraPos", camera.position.x, camera.position.y, camera.position.z);
        shaderProgram.setUniformf("u_lightDirection", lightDirection.x, lightDirection.y, lightDirection.z);
        shaderProgram.setUniformf("u_lightColor", lightColor.x, lightColor.y, lightColor.z);
        shaderProgram.setUniformf(
            "u_emissiveColor",
            definition.emissiveColor[0],
            definition.emissiveColor[1],
            definition.emissiveColor[2]
        );
        shaderProgram.setUniformf("u_emissiveFalloff", definition.emissiveFalloff);
        shaderProgram.setUniformf("u_useNormals", definition.useNormals ? 1f : 0f);
        shaderProgram.setUniformf("u_fogColor", 0.12f, 0.08f, 0.18f);
        shaderProgram.setUniformf("u_blindFogColor", 0.03f, 0.02f, 0.04f);
        shaderProgram.setUniformf("u_fogStart", 3.2f);
        shaderProgram.setUniformf("u_fogEnd", 8.2f);
        shaderProgram.setUniformf("u_blindFogStart", 2.8f);
        shaderProgram.setUniformf("u_blindFogEnd", 5.2f);
        shaderProgram.setUniformf("u_blindFogStrength", 0.95f);
        shaderProgram.setUniformf("u_time", elapsedTime);
        shaderProgram.setUniformf("u_softParticlesEnabled", definition.softParticlesEnabled ? 1f : 0f);
        shaderProgram.setUniformf("u_softParticleFadeDistance", softParticleFadeDistance);
        shaderProgram.setUniformf("u_hasDepthTexture", depthTexture != null ? 1f : 0f);
        shaderProgram.setUniformf("u_screenSize", Math.max(1f, viewportWidth), Math.max(1f, viewportHeight));
        shaderProgram.setUniformf("u_useInstancing", definition.gpuInstancingEnabled && instancedRenderingEnabled ? 1f : 0f);
        shaderProgram.setUniformf("u_retroEnabled", definition.retroEffect ? 1f : 0f);

        ParticleMaterial material = definition.material;
        shaderProgram.setUniformf("u_diffuseStrength", material.diffuseStrength);
        shaderProgram.setUniformf("u_ambientStrength", material.ambientStrength);
        shaderProgram.setUniformf("u_specularEnabled", material.specularEnabled ? 1f : 0f);
        shaderProgram.setUniformf("u_specularStrength", material.specularStrength);
        shaderProgram.setUniformf("u_shininess", material.shininess);
        shaderProgram.setUniformf(
            "u_specularColor",
            material.specularColor[0] * material.specularTint[0],
            material.specularColor[1] * material.specularTint[1],
            material.specularColor[2] * material.specularTint[2]
        );
        shaderProgram.setUniformf("u_fresnelEnabled", material.fresnelEnabled ? 1f : 0f);
        shaderProgram.setUniformf("u_fresnelStrength", material.fresnelStrength);
        shaderProgram.setUniformf("u_fresnelPower", material.fresnelPower);
        shaderProgram.setUniformf(
            "u_fresnelColor",
            material.fresnelColor[0],
            material.fresnelColor[1],
            material.fresnelColor[2]
        );
        shaderProgram.setUniformf("u_edgeFadeStrength", material.edgeFadeStrength);
        shaderProgram.setUniformf("u_edgeFadePower", material.edgeFadePower);

        if (depthTexture != null) {
            depthTexture.bind(1);
            shaderProgram.setUniformi("u_depthTexture", 1);
        }

        if (definition.gpuInstancingEnabled && instancedRenderingEnabled && instanceData != null && activeParticles.size > 0) {
            renderInstanced(shaderProgram, camera);
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }

        for (int i = 0; i < activeParticles.size; i++) {
            ParticleInstance particle = activeParticles.get(i);
            float ageRatio = particle.age / particle.life;
            resolveRenderOrientation(particle, camera, billboardMode, renderOrientation);
            particle.modelTransform.idt()
                .translate(particle.position)
                .rotate(renderOrientation)
                .scale(particle.currentScaleX, particle.currentScaleY, particle.currentScaleZ);
            shaderProgram.setUniformMatrix("u_modelTrans", particle.modelTransform);
            shaderProgram.setUniformf(
                "u_color",
                particle.currentColor.r * material.diffuseTint[0],
                particle.currentColor.g * material.diffuseTint[1],
                particle.currentColor.b * material.diffuseTint[2],
                particle.currentColor.a
            );
            shaderProgram.setUniformf("u_particleNormal", particle.normal.x, particle.normal.y, particle.normal.z);
            shaderProgram.setUniformf("u_emissiveStrength", particle.emissiveStrength);
            shaderProgram.setUniformf("u_ageRatio", ageRatio);
            particleMesh.render(shaderProgram, GL20.GL_TRIANGLES);
        }

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public int getActiveParticleCount() {
        return activeParticles.size;
    }

    public float getApproximateBoundsRadius() {
        if (definition == null) {
            return 0f;
        }

        float maxShapeScale = Math.max(definition.shapeScaleX, Math.max(definition.shapeScaleY, definition.shapeScaleZ));
        float shapeRadius = definition.spawnRadius * maxShapeScale;

        switch (ParticleEmissionShape.fromName(definition.emissionShape)) {
            case BOX:
                shapeRadius = definition.spawnRadius * (float) Math.sqrt(3f) * maxShapeScale;
                break;
            case RING:
            case DISC:
                shapeRadius = definition.spawnRadius * Math.max(definition.shapeScaleX, definition.shapeScaleZ);
                break;
            case LINE:
                shapeRadius = (float) Math.sqrt(
                    definition.lineEndX * definition.lineEndX
                        + definition.lineEndY * definition.lineEndY
                        + definition.lineEndZ * definition.lineEndZ
                );
                break;
            case TORUS:
                shapeRadius = (definition.torusMajorRadius + definition.torusMinorRadius) * maxShapeScale;
                break;
            case CONE:
                shapeRadius = Math.max(shapeRadius, definition.spawnRadius * maxShapeScale);
                break;
            case POINT:
            case SPHERE:
            case SURFACE_SPHERE:
            default:
                break;
        }

        float travelRadius = definition.speedMax * definition.lifetimeMax;
        float particleSize = Math.max(
            Math.max(definition.startScaleXMax, Math.max(definition.startScaleYMax, definition.startScaleZMax)),
            Math.max(definition.endScaleXMax, Math.max(definition.endScaleYMax, definition.endScaleZMax))
        );
        return Math.max(1f, shapeRadius + travelRadius + particleSize);
    }

    public void clear() {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            particlePool.free(activeParticles.removeIndex(i));
        }

        emissionAccumulator = 0f;
        emissionElapsed = 0f;
        elapsedTime = 0f;
        burstTimer = 0f;
        pulseTimer = 0f;
        burstFired = false;
        burstStaggerAccumulator = 0f;
        burstRemainingCount = 0;
        firstVelocitySample = true;
        emitterVelocity.setZero();
        lastEmitterPosition.set(emitterPosition);
        emissionStoppedNotified = false;
        emitterCompleteNotified = false;
        liveSpawnRadius = definition != null ? definition.spawnRadius : 0f;
    }

    public void dispose() {
        clear();
        if (particleMesh != null) {
            particleMesh.dispose();
            particleMesh = null;
        }
    }

    private void rebuildParticleMesh() {
        if (particleMesh != null) {
            if (particleMesh.isInstanced()) {
                particleMesh.disableInstancedRendering();
            }
            particleMesh.dispose();
        }

        ParticleMeshType meshType = ParticleMeshType.fromName(definition.particleMeshType);
        particleMesh = Particle3DMeshFactory.build(meshType);
    }

    private void emitNewParticles(float delta) {
        liveSpawnRadius = getLiveSpawnRadius(delta);

        if (emissionElapsed < definition.emissionStartDelay) {
            return;
        }

        if (lodFactor <= 0f) {
            return;
        }

        int availableCapacity = definition.maxParticles - activeParticles.size;

        if (definition.burstMode) {
            if (!burstFired) {
                if (availableCapacity <= 0) {
                    return;
                }

                if (definition.burstStaggerDuration > 0f) {
                    if (burstRemainingCount <= 0) {
                        burstRemainingCount = definition.burstCount;
                    }

                    float staggerRate = definition.burstCount / definition.burstStaggerDuration;
                    burstStaggerAccumulator += staggerRate * delta;
                    int spawnCount = Math.min((int) burstStaggerAccumulator, Math.min(burstRemainingCount, availableCapacity));
                    if (spawnCount > 0) {
                        burstStaggerAccumulator -= spawnCount;
                        burstRemainingCount -= spawnCount;
                        for (int i = 0; i < spawnCount; i++) {
                            activeParticles.add(createParticle());
                        }
                    }

                    if (burstRemainingCount <= 0) {
                        burstFired = true;
                        burstTimer = 0f;
                        burstStaggerAccumulator = 0f;
                    }
                } else {
                    int count = Math.min(definition.burstCount, availableCapacity);
                    for (int i = 0; i < count; i++) {
                        activeParticles.add(createParticle());
                    }
                    burstFired = true;
                    burstTimer = 0f;
                }
            } else if (definition.burstLoop) {
                burstTimer += delta;
                if (burstTimer >= definition.burstInterval) {
                    burstFired = false;
                    burstTimer = 0f;
                    burstRemainingCount = 0;
                    burstStaggerAccumulator = 0f;
                }
            }
            return;
        }

        if (availableCapacity <= 0) {
            return;
        }

        float effectiveEmissionTime = Math.max(0f, emissionElapsed - definition.emissionStartDelay);
        float liveEmissionRate = definition.sampleEmissionRate(effectiveEmissionTime) * lodFactor;
        if (liveEmissionRate <= 0f) {
            return;
        }

        emissionAccumulator += liveEmissionRate * delta;

        int spawnCount = Math.min((int) emissionAccumulator, availableCapacity);
        if (spawnCount <= 0) {
            return;
        }

        emissionAccumulator -= spawnCount;
        for (int i = 0; i < spawnCount; i++) {
            activeParticles.add(createParticle());
        }
    }

    private void updateActiveParticles(float delta, ForceFieldProvider forceFieldProvider) {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            ParticleInstance particle = activeParticles.get(i);
            particle.age += delta;
            if (particle.age >= particle.life) {
                notifyDeathSubEmitter(particle);
                particlePool.free(activeParticles.removeIndex(i));
                continue;
            }

            float ageRatio = particle.age / particle.life;

            if (particle.stuck) {
                particle.position.set(particle.stuckPosition);
                particle.velocity.setZero();
            } else {
                applyChaos(particle, delta);
                applyInlineTurbulence(particle, delta);
                applyWaveMotion(particle, delta);
                applyVelocityCurve(particle, ageRatio, delta);

                if (forceFieldProvider != null) {
                    forceFieldProvider.sampleForce(particle.position, delta, forceBuffer);
                    particle.velocity.mulAdd(forceBuffer, 1f);
                }

                particle.velocity.x += definition.windX * delta;
                particle.velocity.y += ((definition.gravity * particle.gravityScale) + definition.windY) * delta;
                particle.velocity.z += definition.windZ * delta;
                particle.position.mulAdd(particle.velocity, delta);

                if (definition.collisionEnabled) {
                    if (resolveCollisions(particle)) {
                        notifyDeathSubEmitter(particle);
                        particlePool.free(activeParticles.removeIndex(i));
                        continue;
                    }
                }
            }

            if (definition.drag > 0f) {
                float dragFactor = 1f - MathUtils.clamp(definition.drag * delta, 0f, 0.99f);
                particle.velocity.scl(dragFactor);
            }

            if (definition.lockRotationX) {
                particle.angularVelocity.x = 0f;
            }
            if (definition.lockRotationY) {
                particle.angularVelocity.y = 0f;
            }
            if (definition.lockRotationZ) {
                particle.angularVelocity.z = 0f;
            }

            float rotationMultiplier = definition.sampleRotationMultiplier(ageRatio);
            angularDeltaEulerDegrees.set(particle.angularVelocity).scl(rotationMultiplier * delta);
            orientationDeltaQuaternion.idt().setEulerAngles(
                angularDeltaEulerDegrees.y,
                angularDeltaEulerDegrees.x,
                angularDeltaEulerDegrees.z
            );
            particle.orientation.mul(orientationDeltaQuaternion).nor();

            if (definition.angularDamping > 0f) {
                float dampFactor = 1f - MathUtils.clamp(definition.angularDamping * delta, 0f, 1f);
                particle.angularVelocity.scl(dampFactor);
            }

            float sizeMultiplier = definition.sampleSizeMultiplier(ageRatio);
            particle.currentScaleX = Math.max(0.0001f, MathUtils.lerp(particle.startScaleX, particle.endScaleX, ageRatio) * sizeMultiplier);
            particle.currentScaleY = Math.max(0.0001f, MathUtils.lerp(particle.startScaleY, particle.endScaleY, ageRatio) * sizeMultiplier);
            particle.currentScaleZ = Math.max(0.0001f, MathUtils.lerp(particle.startScaleZ, particle.endScaleZ, ageRatio) * sizeMultiplier);

            if (definition.velocityStretch) {
                float speed = particle.velocity.len();
                float stretchValue = 1f + MathUtils.clamp(
                    speed * definition.velocityStretchFactor,
                    definition.velocityStretchMin,
                    definition.velocityStretchMax
                );
                if (definition.velocityStretchAxis == 0) {
                    particle.currentScaleX *= stretchValue;
                } else if (definition.velocityStretchAxis == 2) {
                    particle.currentScaleZ *= stretchValue;
                } else {
                    particle.currentScaleY *= stretchValue;
                }

                if (!particle.velocity.isZero(0.0001f)) {
                    velocityDirectionBuffer.set(particle.velocity).nor();
                    particle.orientation.setFromCross(Vector3.Y, velocityDirectionBuffer);
                }
            }

            definition.sampleGradient(ageRatio, gradientSample);
            particle.currentColor.r = gradientSample[0];
            particle.currentColor.g = gradientSample[1];
            particle.currentColor.b = gradientSample[2];
            particle.currentColor.a = gradientSample[3];
            particle.emissiveStrength = definition.emissiveStrength * definition.sampleEmissiveMultiplier(ageRatio);

            if (definition.useNormals) {
                if (particle.velocity.isZero(0.0001f)) {
                    particle.normal.set(Vector3.Y);
                } else {
                    particle.normal.set(particle.velocity).nor();
                }
            } else {
                particle.normal.set(Vector3.Y);
            }

            particle.modelTransform.idt()
                .translate(particle.position)
                .rotate(particle.orientation)
                .scale(particle.currentScaleX, particle.currentScaleY, particle.currentScaleZ);
        }
    }

    private ParticleInstance createParticle() {
        ParticleInstance particle = particlePool.obtain();

        particle.life = MathUtils.random(definition.lifetimeMin, definition.lifetimeMax);
        particle.age = 0f;

        particle.startScaleX = MathUtils.random(definition.startScaleXMin, definition.startScaleXMax);
        particle.startScaleY = MathUtils.random(definition.startScaleYMin, definition.startScaleYMax);
        particle.startScaleZ = MathUtils.random(definition.startScaleZMin, definition.startScaleZMax);
        particle.endScaleX = MathUtils.random(definition.endScaleXMin, definition.endScaleXMax);
        particle.endScaleY = MathUtils.random(definition.endScaleYMin, definition.endScaleYMax);
        particle.endScaleZ = MathUtils.random(definition.endScaleZMin, definition.endScaleZMax);

        if (definition.startSizeMax > 0f && definition.endSizeMax > 0f) {
            float startUniformScale = MathUtils.random(definition.startSizeMin, definition.startSizeMax);
            float endUniformScale = MathUtils.random(definition.endSizeMin, definition.endSizeMax);
            particle.startScaleX = startUniformScale;
            particle.startScaleY = startUniformScale;
            particle.startScaleZ = startUniformScale;
            particle.endScaleX = endUniformScale;
            particle.endScaleY = endUniformScale;
            particle.endScaleZ = endUniformScale;
        }

        float startScaleMultiplier = definition.sampleSizeMultiplier(0f);
        particle.currentScaleX = Math.max(0.0001f, particle.startScaleX * startScaleMultiplier);
        particle.currentScaleY = Math.max(0.0001f, particle.startScaleY * startScaleMultiplier);
        particle.currentScaleZ = Math.max(0.0001f, particle.startScaleZ * startScaleMultiplier);

        sampleSpawnOffset(spawnOffset, ParticleEmissionShape.fromName(definition.emissionShape));
        particle.position.set(emitterPosition).add(spawnOffset);
        if (spawnOffset.isZero(0.0001f)) {
            particle.surfaceNormal.set(Vector3.Y);
        } else {
            particle.surfaceNormal.set(spawnOffset).nor();
        }

        resolveBaseDirection(
            directionBuffer,
            spawnOffset,
            particle.surfaceNormal,
            ParticleDirectionMode.fromName(definition.directionMode)
        );
        applySpreadAndRandomness(directionBuffer);

        float speed = MathUtils.random(definition.speedMin, definition.speedMax);
        speed *= 1f + (MathUtils.random(-definition.randomStrength, definition.randomStrength) * 0.45f);
        speed = Math.max(0f, speed);
        particle.velocity.set(directionBuffer).scl(speed);
        particle.velocity.mulAdd(emitterVelocity, definition.inheritVelocityFactor);

        particle.orientation.idt().setEulerAngles(
            MathUtils.random(0f, 360f),
            MathUtils.random(0f, 360f),
            MathUtils.random(0f, 360f)
        );
        particle.angularVelocity.set(
            MathUtils.random(definition.angularVelocityXMin, definition.angularVelocityXMax),
            MathUtils.random(definition.angularVelocityYMin, definition.angularVelocityYMax),
            MathUtils.random(definition.angularVelocityZMin, definition.angularVelocityZMax)
        );
        particle.currentColor.set(
            definition.startColor[0],
            definition.startColor[1],
            definition.startColor[2],
            definition.startColor[3]
        );
        particle.stuck = false;
        particle.stuckPosition.set(particle.position);
        particle.gravityScale = MathUtils.random(definition.gravityScaleMin, definition.gravityScaleMax);

        particle.emissiveStrength = definition.emissiveStrength * definition.sampleEmissiveMultiplier(0f);
        if (definition.useNormals) {
            particle.normal.set(particle.velocity).nor();
        } else {
            particle.normal.set(Vector3.Y);
        }

        particle.modelTransform.idt()
            .translate(particle.position)
            .rotate(particle.orientation)
            .scale(particle.currentScaleX, particle.currentScaleY, particle.currentScaleZ);

        return particle;
    }

    private void sampleSpawnOffset(Vector3 outOffset, ParticleEmissionShape shape) {
        float radius = liveSpawnRadius;
        switch (shape) {
            case SPHERE:
                randomUnitVector(outOffset).scl(MathUtils.random() * radius);
                outOffset.x *= definition.shapeScaleX;
                outOffset.y *= definition.shapeScaleY;
                outOffset.z *= definition.shapeScaleZ;
                break;
            case SURFACE_SPHERE:
                randomUnitVector(outOffset).scl(radius);
                outOffset.x *= definition.shapeScaleX;
                outOffset.y *= definition.shapeScaleY;
                outOffset.z *= definition.shapeScaleZ;
                break;
            case BOX:
                outOffset.set(
                    MathUtils.random(-radius, radius) * definition.shapeScaleX,
                    MathUtils.random(-radius, radius) * definition.shapeScaleY,
                    MathUtils.random(-radius, radius) * definition.shapeScaleZ
                );
                break;
            case RING:
                float ringAngle = MathUtils.random(0f, MathUtils.PI2);
                outOffset.set(
                    MathUtils.cos(ringAngle) * radius * definition.shapeScaleX,
                    0f,
                    MathUtils.sin(ringAngle) * radius * definition.shapeScaleZ
                );
                orientOffsetToRingAxis(outOffset);
                break;
            case DISC:
                float discAngle = MathUtils.random(0f, MathUtils.PI2);
                float discRadius = (float) Math.sqrt(MathUtils.random()) * radius;
                outOffset.set(
                    MathUtils.cos(discAngle) * discRadius * definition.shapeScaleX,
                    0f,
                    MathUtils.sin(discAngle) * discRadius * definition.shapeScaleZ
                );
                orientOffsetToRingAxis(outOffset);
                break;
            case CONE:
                float theta = MathUtils.random(0f, MathUtils.PI2);
                float cosHalfAngle = MathUtils.cosDeg(definition.coneHalfAngle);
                float cosPhi = MathUtils.random(cosHalfAngle, 1f);
                float sinPhi = (float) Math.sqrt(Math.max(0f, 1f - (cosPhi * cosPhi)));
                float coneDistance = radius * (float) Math.cbrt(MathUtils.random());
                outOffset.set(
                    MathUtils.cos(theta) * sinPhi,
                    cosPhi,
                    MathUtils.sin(theta) * sinPhi
                ).scl(coneDistance);
                outOffset.x *= definition.shapeScaleX;
                outOffset.y *= definition.shapeScaleY;
                outOffset.z *= definition.shapeScaleZ;
                break;
            case LINE:
                float lineT = MathUtils.random();
                outOffset.set(
                    definition.lineEndX * lineT,
                    definition.lineEndY * lineT,
                    definition.lineEndZ * lineT
                );
                break;
            case TORUS:
                float major = Math.max(0.001f, definition.torusMajorRadius);
                float minor = Math.max(0f, definition.torusMinorRadius);
                float torusTheta = MathUtils.random(0f, MathUtils.PI2);
                float torusPhi = MathUtils.random(0f, MathUtils.PI2);
                float minorDistance = minor * (float) Math.sqrt(MathUtils.random());
                float ringDistance = major + (MathUtils.cos(torusPhi) * minorDistance);
                outOffset.set(
                    ringDistance * MathUtils.cos(torusTheta),
                    MathUtils.sin(torusPhi) * minorDistance,
                    ringDistance * MathUtils.sin(torusTheta)
                );
                outOffset.x *= definition.shapeScaleX;
                outOffset.y *= definition.shapeScaleY;
                outOffset.z *= definition.shapeScaleZ;
                break;
            case POINT:
            default:
                outOffset.setZero();
                break;
        }
    }

    private void orientOffsetToRingAxis(Vector3 outOffset) {
        ringAxisBuffer.set(definition.ringAxisX, definition.ringAxisY, definition.ringAxisZ);
        if (ringAxisBuffer.isZero(0.0001f)) {
            return;
        }

        ringAxisBuffer.nor();
        float dot = ringAxisBuffer.dot(Vector3.Y);
        if (dot >= 0.9999f) {
            return;
        }

        if (dot <= -0.9999f) {
            orientationQuaternion.setFromAxisRad(Vector3.X, MathUtils.PI);
        } else {
            orientationQuaternion.setFromCross(Vector3.Y, ringAxisBuffer);
        }
        outOffset.mul(orientationQuaternion);
    }

    private void resolveBaseDirection(
        Vector3 outDirection,
        Vector3 sampledOffset,
        Vector3 sampledNormal,
        ParticleDirectionMode mode
    ) {
        switch (mode) {
            case OUTWARD:
                if (sampledOffset.isZero(0.0001f)) {
                    outDirection.set(sampledNormal);
                    if (outDirection.isZero(0.0001f)) {
                        outDirection.set(0f, 1f, 0f);
                    }
                } else {
                    outDirection.set(sampledOffset).nor();
                }
                break;
            case INWARD:
                if (sampledOffset.isZero(0.0001f)) {
                    outDirection.set(sampledNormal).scl(-1f);
                    if (outDirection.isZero(0.0001f)) {
                        outDirection.set(0f, -1f, 0f);
                    }
                } else {
                    outDirection.set(sampledOffset).nor().scl(-1f);
                }
                break;
            case VECTOR:
                outDirection.set(definition.directionX, definition.directionY, definition.directionZ).nor();
                break;
            case RANDOM:
                randomUnitVector(outDirection);
                break;
            case CHAOS:
                outDirection.set(definition.directionX, definition.directionY, definition.directionZ).nor();
                randomUnitVector(randomBuffer);
                outDirection.mulAdd(randomBuffer, definition.chaosStrength).nor();
                break;
            case MULTI:
                int validCount = Math.min(definition.multiDirectionCount, MULTI_DIRECTIONS.length);
                if (validCount <= 0) {
                    validCount = 1;
                }
                outDirection.set(MULTI_DIRECTIONS[multiDirectionCursor % validCount]).nor();
                multiDirectionCursor++;
                break;
            case TANGENT:
                if (sampledOffset.isZero(0.0001f)) {
                    outDirection.set(sampledNormal);
                    if (outDirection.isZero(0.0001f)) {
                        outDirection.set(0f, 1f, 0f);
                    }
                } else {
                    outDirection.set(sampledOffset).nor();
                }
                outDirection.crs(Vector3.Y);
                if (outDirection.isZero(0.0001f)) {
                    outDirection.set(sampledOffset.isZero(0.0001f) ? sampledNormal : sampledOffset).crs(Vector3.X);
                }
                if (outDirection.isZero(0.0001f)) {
                    outDirection.set(1f, 0f, 0f);
                } else {
                    outDirection.nor();
                }
                break;
            case SURFACE_NORMAL:
                outDirection.set(sampledNormal);
                if (outDirection.isZero(0.0001f)) {
                    outDirection.set(0f, 1f, 0f);
                } else {
                    outDirection.nor();
                }
                break;
            case UP:
            default:
                outDirection.set(0f, 1f, 0f);
                break;
        }
    }

    private void applySpreadAndRandomness(Vector3 direction) {
        float spreadBlend = MathUtils.clamp(definition.spreadAngle / 180f, 0f, 0.95f);
        float randomBlend = MathUtils.clamp(definition.randomStrength, 0f, 1f);

        if (spreadBlend > 0f) {
            randomUnitVector(randomBuffer);
            direction.scl(1f - spreadBlend).mulAdd(randomBuffer, spreadBlend).nor();
        }

        if (randomBlend > 0f) {
            randomUnitVector(randomBuffer);
            direction.scl(1f - (randomBlend * 0.5f)).mulAdd(randomBuffer, randomBlend * 0.5f).nor();
        }

        if (direction.isZero(0.0001f)) {
            direction.set(0f, 1f, 0f);
        }
    }

    private void applyChaos(ParticleInstance particle, float delta) {
        if (definition.chaosStrength <= 0f) {
            return;
        }

        randomUnitVector(randomBuffer);
        float chaosScale = definition.chaosStrength * delta * 2.3f;
        particle.velocity.mulAdd(randomBuffer, chaosScale);
    }

    private void applyInlineTurbulence(ParticleInstance particle, float delta) {
        if (!definition.turbulenceEnabled || definition.turbulenceStrength <= 0f) {
            return;
        }

        inlineTurbulenceField.sample(particle.position, delta, forceBuffer);
        particle.velocity.add(forceBuffer);
    }

    private void updateEmitterVelocity(float delta) {
        if (delta <= 0f) {
            emitterVelocity.setZero();
            return;
        }

        if (firstVelocitySample) {
            lastEmitterPosition.set(emitterPosition);
            emitterVelocity.setZero();
            firstVelocitySample = false;
            return;
        }

        emitterVelocity.set(emitterPosition).sub(lastEmitterPosition).scl(1f / delta);
        lastEmitterPosition.set(emitterPosition);
    }

    private void updateLifecycleState() {
        boolean emissionDone = isEmissionDone();
        if (emissionDone && !emissionStoppedNotified) {
            emissionStoppedNotified = true;
            if (lifecycleListener != null) {
                lifecycleListener.onEmissionStopped(this);
            }
        }

        if (emissionDone && activeParticles.size == 0 && !emitterCompleteNotified) {
            emitterCompleteNotified = true;
            if (lifecycleListener != null) {
                lifecycleListener.onEmitterComplete(this);
            }
        }
    }

    private boolean isEmissionDone() {
        if (definition.burstMode) {
            return burstFired && !definition.burstLoop;
        }

        if (definition.emissionDuration <= 0f) {
            return false;
        }

        float effectiveEmissionTime = Math.max(0f, emissionElapsed - definition.emissionStartDelay);
        return effectiveEmissionTime >= definition.emissionDuration;
    }

    private void sortParticlesByDepth(PerspectiveCamera camera) {
        sortCameraPos.set(camera.position);

        for (int i = 1; i < activeParticles.size; i++) {
            ParticleInstance key = activeParticles.get(i);
            float keyDistance = key.position.dst2(sortCameraPos);
            int j = i - 1;

            while (j >= 0 && activeParticles.get(j).position.dst2(sortCameraPos) < keyDistance) {
                activeParticles.set(j + 1, activeParticles.get(j));
                j--;
            }

            activeParticles.set(j + 1, key);
        }
    }

    private void applyWaveMotion(ParticleInstance particle, float delta) {
        if (!definition.waveEnabled || definition.waveAmplitude <= 0f || definition.waveFrequency <= 0f) {
            return;
        }

        float waveForce = definition.waveAmplitude * MathUtils.sin(MathUtils.PI2 * definition.waveFrequency * particle.age);
        particle.velocity.x += definition.waveAxisX * waveForce * delta;
        particle.velocity.y += definition.waveAxisY * waveForce * delta;
        particle.velocity.z += definition.waveAxisZ * waveForce * delta;
    }

    private void applyVelocityCurve(ParticleInstance particle, float ageRatio, float delta) {
        float multiplier = definition.sampleVelocityMultiplier(ageRatio);
        if (Math.abs(multiplier - 1f) < 0.0001f) {
            return;
        }

        float currentSpeed = particle.velocity.len();
        if (currentSpeed <= 0.0001f) {
            return;
        }

        float targetSpeed = currentSpeed * multiplier;
        float blend = MathUtils.clamp(delta * 5f, 0f, 1f);
        float adjustedSpeed = MathUtils.lerp(currentSpeed, targetSpeed, blend);
        particle.velocity.scl(adjustedSpeed / currentSpeed);
    }

    private void notifyDeathSubEmitter(ParticleInstance particle) {
        if (subListener == null || definition.onDeathPreset.isBlank()) {
            return;
        }

        subListener.onParticleDeath(particle.position, definition.onDeathPreset, definition.subEmitterBurst);
    }

    private void notifyBounceSubEmitter(ParticleInstance particle) {
        if (subListener == null || definition.onBouncePreset.isBlank()) {
            return;
        }

        subListener.onParticleBounce(particle.position, definition.onBouncePreset, definition.subEmitterBurst);
    }

    private boolean resolveCollisions(ParticleInstance particle) {
        if (!definition.collisionEnabled) {
            return false;
        }

        if (collisionPlanes.size > 0) {
            for (int i = 0; i < collisionPlanes.size; i++) {
                if (resolveCollisionAgainstPlane(particle, collisionPlanes.get(i))) {
                    return true;
                }
            }
            return false;
        }

        if (particle.position.y < 0f) {
            collisionNormalBuffer.set(0f, 1f, 0f);
            collisionPointBuffer.set(particle.position.x, 0f, particle.position.z);
            return applyCollisionResponse(particle, collisionNormalBuffer, collisionPointBuffer);
        }

        return false;
    }

    private boolean resolveCollisionAgainstPlane(ParticleInstance particle, CollisionPlane collisionPlane) {
        if (collisionPlane == null) {
            return false;
        }

        float signedDistance = collisionPlane.signedDistance(particle.position);
        if (signedDistance >= 0f) {
            return false;
        }

        collisionPlane.project(particle.position, collisionPointBuffer);
        collisionPlane.getNormal(collisionNormalBuffer);
        return applyCollisionResponse(particle, collisionNormalBuffer, collisionPointBuffer);
    }

    private boolean applyCollisionResponse(ParticleInstance particle, Vector3 collisionNormal, Vector3 contactPoint) {
        particle.position.set(contactPoint);
        ParticleCollisionResponse collisionResponse = ParticleCollisionResponse.fromName(definition.collisionResponse.name());

        switch (collisionResponse) {
            case STICK:
                particle.stuck = true;
                particle.stuckPosition.set(contactPoint);
                particle.velocity.setZero();
                particle.normal.set(collisionNormal).nor();
                particle.life += definition.stickLifeExtension;
                notifyBounceSubEmitter(particle);
                return false;
            case BOUNCE:
                float velocityAlongNormal = particle.velocity.dot(collisionNormal);
                if (velocityAlongNormal < 0f) {
                    particle.velocity.mulAdd(collisionNormal, -2f * velocityAlongNormal);
                }
                particle.velocity.scl(definition.bounceDamping);
                particle.normal.set(collisionNormal).nor();
                notifyBounceSubEmitter(particle);
                return false;
            case DIE:
            default:
                return true;
        }
    }

    private void configureRenderingState() {
        if (particleMesh == null) {
            return;
        }

        instancedRenderingEnabled = definition.gpuInstancingEnabled && isInstancedRenderingSupported();
        softParticleFadeDistance = definition.softParticleFadeDistance;

        if (particleMesh.isInstanced()) {
            particleMesh.disableInstancedRendering();
        }

        if (instancedRenderingEnabled) {
            particleMesh.enableInstancedRendering(false, Math.max(1, definition.maxParticles), INSTANCE_ATTRIBUTES);
            ensureInstanceDataCapacity();
        } else {
            instanceData = null;
        }
    }

    private boolean isInstancedRenderingSupported() {
        if (Gdx.gl30 == null || Gdx.graphics == null) {
            return false;
        }

        GLVersion glVersion = Gdx.graphics.getGLVersion();
        return glVersion != null && glVersion.isVersionEqualToOrHigher(3, 3);
    }

    private void ensureInstanceDataCapacity() {
        int requiredLength = Math.max(1, definition.maxParticles) * INSTANCE_STRIDE;
        if (instanceData == null || instanceData.length < requiredLength) {
            instanceData = new float[requiredLength];
        }
    }

    private void renderInstanced(ShaderProgram shaderProgram, PerspectiveCamera camera) {
        ensureInstanceDataCapacity();
        ParticleBillboardMode billboardMode = getActiveBillboardMode();

        shaderProgram.setUniformMatrix("u_modelTrans", identityTransform.idt());

        int cursor = 0;
        for (int i = 0; i < activeParticles.size; i++) {
            ParticleInstance particle = activeParticles.get(i);
            float ageRatio = particle.age / particle.life;

            instanceData[cursor++] = particle.position.x;
            instanceData[cursor++] = particle.position.y;
            instanceData[cursor++] = particle.position.z;
            instanceData[cursor++] = particle.currentScaleX;
            instanceData[cursor++] = particle.currentScaleY;
            instanceData[cursor++] = particle.currentScaleZ;
            resolveRenderOrientation(particle, camera, billboardMode, renderOrientation);
            instanceData[cursor++] = renderOrientation.x;
            instanceData[cursor++] = renderOrientation.y;
            instanceData[cursor++] = renderOrientation.z;
            instanceData[cursor++] = renderOrientation.w;
            instanceData[cursor++] = particle.currentColor.r;
            instanceData[cursor++] = particle.currentColor.g;
            instanceData[cursor++] = particle.currentColor.b;
            instanceData[cursor++] = particle.currentColor.a;
            instanceData[cursor++] = particle.normal.x;
            instanceData[cursor++] = particle.normal.y;
            instanceData[cursor++] = particle.normal.z;
            instanceData[cursor++] = ageRatio;
            instanceData[cursor++] = particle.emissiveStrength;
        }

        particleMesh.setInstanceData(instanceData, 0, cursor);
        particleMesh.render(shaderProgram, GL20.GL_TRIANGLES);
    }

    private ParticleBillboardMode getActiveBillboardMode() {
        ParticleBillboardMode mode = ParticleBillboardMode.fromName(definition.billboardMode);
        if (mode == ParticleBillboardMode.SPHERICAL && definition.velocityStretch) {
            return ParticleBillboardMode.VELOCITY_STRETCHED;
        }
        return mode;
    }

    private void resolveRenderOrientation(
        ParticleInstance particle,
        PerspectiveCamera camera,
        ParticleBillboardMode mode,
        Quaternion outOrientation
    ) {
        if (mode == null || mode == ParticleBillboardMode.WORLD_FIXED || camera == null) {
            outOrientation.set(particle.orientation);
            return;
        }

        switch (mode) {
            case Y_AXIS_LOCKED:
                billboardToCamera.set(camera.position).sub(particle.position);
                billboardToCamera.y = 0f;
                if (billboardToCamera.isZero(0.0001f)) {
                    outOrientation.set(particle.orientation);
                } else {
                    outOrientation.setFromCross(Vector3.Z, billboardToCamera.nor());
                }
                return;
            case VELOCITY_ALIGNED:
            case VELOCITY_STRETCHED:
                if (particle.velocity.isZero(0.0001f)) {
                    outOrientation.set(particle.orientation);
                } else {
                    billboardDirection.set(particle.velocity).nor();
                    outOrientation.setFromCross(Vector3.Y, billboardDirection);
                }
                return;
            case HORIZONTAL:
                outOrientation.set(horizontalOrientation);
                return;
            case SPHERICAL:
            default:
                billboardToCamera.set(camera.position).sub(particle.position);
                if (billboardToCamera.isZero(0.0001f)) {
                    outOrientation.set(particle.orientation);
                    return;
                }

                billboardDirection.set(billboardToCamera).nor();
                billboardDirectionXZ.set(billboardDirection.x, 0f, billboardDirection.z);
                if (billboardDirectionXZ.isZero(0.0001f)) {
                    outOrientation.set(particle.orientation);
                    return;
                }

                outOrientation.setFromCross(Vector3.Z, billboardDirectionXZ.nor());
                return;
        }
    }

    private float getLiveSpawnRadius(float delta) {
        if (!definition.radialPulse) {
            pulseTimer = 0f;
            return definition.spawnRadius;
        }

        pulseTimer += delta;
        float pulseRadius = definition.spawnRadius + (pulseTimer * definition.radialPulseSpeed);
        if (pulseRadius > definition.radialPulseMax) {
            pulseTimer = 0f;
            return definition.spawnRadius;
        }
        return pulseRadius;
    }

    private Vector3 randomUnitVector(Vector3 output) {
        output.set(
            MathUtils.random(-1f, 1f),
            MathUtils.random(-1f, 1f),
            MathUtils.random(-1f, 1f)
        );

        if (output.isZero(0.0001f)) {
            output.set(0f, 1f, 0f);
        }
        return output.nor();
    }

    private static class ParticleInstance implements Pool.Poolable {
        final Vector3 position = new Vector3();
        final Vector3 velocity = new Vector3();
        final Vector3 angularVelocity = new Vector3();
        final Vector3 normal = new Vector3(0f, 1f, 0f);
        final Vector3 surfaceNormal = new Vector3(0f, 1f, 0f);
        final Vector3 stuckPosition = new Vector3();
        final Quaternion orientation = new Quaternion().idt();
        final Matrix4 modelTransform = new Matrix4();
        final Color currentColor = new Color(1f, 1f, 1f, 1f);
        float life;
        float age;
        float startScaleX;
        float startScaleY;
        float startScaleZ;
        float endScaleX;
        float endScaleY;
        float endScaleZ;
        float currentScaleX;
        float currentScaleY;
        float currentScaleZ;
        float emissiveStrength;
        float gravityScale;
        boolean stuck;

        @Override
        public void reset() {
            position.setZero();
            velocity.setZero();
            angularVelocity.setZero();
            normal.set(Vector3.Y);
            surfaceNormal.set(Vector3.Y);
            stuckPosition.setZero();
            orientation.idt();
            modelTransform.idt();
            currentColor.set(1f, 1f, 1f, 1f);
            life = 0f;
            age = 0f;
            startScaleX = 0f;
            startScaleY = 0f;
            startScaleZ = 0f;
            endScaleX = 0f;
            endScaleY = 0f;
            endScaleZ = 0f;
            currentScaleX = 0f;
            currentScaleY = 0f;
            currentScaleZ = 0f;
            emissiveStrength = 0f;
            gravityScale = 1f;
            stuck = false;
        }
    }
}
