package io.github.superteam.resonance.particles;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class ParticleEffect {
    private static final class EmitterBinding {
        private final ParticleEmitter emitter;
        private final Vector3 localOffset = new Vector3();
        private float localScale = 1f;

        private EmitterBinding(ParticleEmitter emitter, Vector3 offset, float localScale) {
            this.emitter = emitter;
            if (offset != null) {
                this.localOffset.set(offset);
            }
            this.localScale = Math.max(0.001f, localScale);
        }
    }

    private final Array<EmitterBinding> emitterBindings = new Array<>(false, 4);
    private final Vector3 position = new Vector3();
    private final Quaternion orientation = new Quaternion().idt();
    private final Vector3 transformedOffset = new Vector3();
    private final String name;

    private boolean active = true;
    private float scale = 1f;

    public ParticleEffect() {
        this("ParticleEffect");
    }

    public ParticleEffect(String name) {
        if (name == null || name.isBlank()) {
            this.name = "ParticleEffect";
        } else {
            this.name = name;
        }
    }

    public ParticleEffect addEmitter(ParticleEmitter emitter) {
        return addEmitter(emitter, 0f, 0f, 0f, 1f);
    }

    public ParticleEffect addEmitter(ParticleEmitter emitter, Vector3 localOffset) {
        return addEmitter(emitter, localOffset, 1f);
    }

    public ParticleEffect addEmitter(ParticleEmitter emitter, Vector3 localOffset, float localScale) {
        if (localOffset == null) {
            return addEmitter(emitter, 0f, 0f, 0f, localScale);
        }
        return addEmitter(emitter, localOffset.x, localOffset.y, localOffset.z, localScale);
    }

    public ParticleEffect addEmitter(ParticleEmitter emitter, float offsetX, float offsetY, float offsetZ) {
        return addEmitter(emitter, offsetX, offsetY, offsetZ, 1f);
    }

    public ParticleEffect addEmitter(
        ParticleEmitter emitter,
        float offsetX,
        float offsetY,
        float offsetZ,
        float localScale
    ) {
        if (emitter == null) {
            throw new IllegalArgumentException("Particle emitter must not be null");
        }

        EmitterBinding binding = new EmitterBinding(emitter, new Vector3(offsetX, offsetY, offsetZ), localScale);
        emitterBindings.add(binding);
        syncEmitterTransform(binding);
        return this;
    }

    public boolean removeEmitter(ParticleEmitter emitter) {
        for (int i = 0; i < emitterBindings.size; i++) {
            if (emitterBindings.get(i).emitter == emitter) {
                emitterBindings.removeIndex(i);
                return true;
            }
        }
        return false;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        syncEmitterTransforms();
    }

    public void setPosition(Vector3 newPosition) {
        if (newPosition == null) {
            position.setZero();
        } else {
            position.set(newPosition);
        }
        syncEmitterTransforms();
    }

    public void translate(float x, float y, float z) {
        position.add(x, y, z);
        syncEmitterTransforms();
    }

    public void setOrientation(Quaternion newOrientation) {
        if (newOrientation == null) {
            orientation.idt();
        } else {
            orientation.set(newOrientation);
        }
        syncEmitterTransforms();
    }

    public void setRotation(float axisX, float axisY, float axisZ, float degrees) {
        if (Math.abs(degrees) < 0.0001f || Math.abs(axisX) + Math.abs(axisY) + Math.abs(axisZ) < 0.0001f) {
            orientation.idt();
        } else {
            orientation.idt().setFromAxisRad(axisX, axisY, axisZ, degrees * MathUtils.degreesToRadians);
        }
        syncEmitterTransforms();
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.001f, scale);
        syncEmitterTransforms();
    }

    public void setLodFactor(float lodFactor) {
        float clampedLodFactor = MathUtils.clamp(lodFactor, 0f, 1f);
        for (int i = 0; i < emitterBindings.size; i++) {
            emitterBindings.get(i).emitter.setLodFactor(clampedLodFactor);
        }
    }

    public float applyLodForDistance(float distanceToCamera) {
        float maxLodFactor = 0f;
        for (int i = 0; i < emitterBindings.size; i++) {
            ParticleEmitter emitter = emitterBindings.get(i).emitter;
            float emitterLodFactor = emitter.computeLodFactor(distanceToCamera);
            emitter.setLodFactor(emitterLodFactor);
            if (emitterLodFactor > maxLodFactor) {
                maxLodFactor = emitterLodFactor;
            }
        }
        return maxLodFactor;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }

    public Vector3 getPosition() {
        return position;
    }

    public int getEmitterCount() {
        return emitterBindings.size;
    }

    public int getActiveParticleCount() {
        int activeParticleCount = 0;
        for (int i = 0; i < emitterBindings.size; i++) {
            activeParticleCount += emitterBindings.get(i).emitter.getActiveParticleCount();
        }
        return activeParticleCount;
    }

    public float getApproximateBoundsRadius() {
        float boundsRadius = 1f;
        for (int i = 0; i < emitterBindings.size; i++) {
            EmitterBinding binding = emitterBindings.get(i);
            float offsetRadius = binding.localOffset.len() * scale;
            float emitterRadius = binding.emitter.getApproximateBoundsRadius() * scale * binding.localScale;
            boundsRadius = Math.max(boundsRadius, offsetRadius + emitterRadius);
        }
        return boundsRadius;
    }

    public void reset() {
        for (int i = 0; i < emitterBindings.size; i++) {
            emitterBindings.get(i).emitter.clear();
        }
        syncEmitterTransforms();
    }

    public void update(float delta) {
        update(delta, null);
    }

    public void update(float delta, ForceFieldProvider forceFieldProvider) {
        if (!active) {
            return;
        }

        syncEmitterTransforms();
        for (int i = 0; i < emitterBindings.size; i++) {
            emitterBindings.get(i).emitter.update(delta, forceFieldProvider);
        }
    }

    public void render(ShaderProgram shaderProgram, PerspectiveCamera camera) {
        render(shaderProgram, camera, null, 0f, 0f);
    }

    public void render(ShaderProgram shaderProgram, PerspectiveCamera camera, Texture depthTexture, float viewportWidth, float viewportHeight) {
        if (!active) {
            return;
        }

        for (int i = 0; i < emitterBindings.size; i++) {
            emitterBindings.get(i).emitter.render(shaderProgram, camera, depthTexture, viewportWidth, viewportHeight);
        }
    }

    public void dispose() {
        for (int i = emitterBindings.size - 1; i >= 0; i--) {
            emitterBindings.get(i).emitter.dispose();
        }
        emitterBindings.clear();
    }

    private void syncEmitterTransforms() {
        for (int i = 0; i < emitterBindings.size; i++) {
            syncEmitterTransform(emitterBindings.get(i));
        }
    }

    private void syncEmitterTransform(EmitterBinding binding) {
        transformedOffset.set(binding.localOffset).scl(scale * binding.localScale).mul(orientation).add(position);
        binding.emitter.setEmitterPosition(transformedOffset.x, transformedOffset.y, transformedOffset.z);
    }
}
