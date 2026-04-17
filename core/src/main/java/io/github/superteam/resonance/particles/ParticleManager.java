package io.github.superteam.resonance.particles;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncResult;
import com.badlogic.gdx.utils.async.AsyncTask;

public class ParticleManager implements ForceFieldProvider {
    private final Array<ParticleEffect> effects = new Array<>(false, 4);
    private final Array<ForceField> forceFields = new Array<>(false, 4);
    private final Array<AsyncResult<Void>> pendingUpdates = new Array<>(false, 4);
    private final Array<TrailEmitter> trailEmitters = new Array<>(false, 4);
    private final ThreadLocal<Vector3> forceBuffer = ThreadLocal.withInitial(Vector3::new);
    private final AsyncExecutor asyncExecutor = new AsyncExecutor(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

    private boolean cullingEnabled = true;
    private boolean asyncUpdatesEnabled = true;
    private Texture depthTexture;
    private float viewportWidth;
    private float viewportHeight;

    public void addEffect(ParticleEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("Particle effect must not be null");
        }

        if (!effects.contains(effect, true)) {
            effects.add(effect);
        }
    }

    public boolean removeEffect(ParticleEffect effect) {
        return effects.removeValue(effect, true);
    }

    public void addTrailEmitter(TrailEmitter trailEmitter) {
        if (trailEmitter == null) {
            throw new IllegalArgumentException("Trail emitter must not be null");
        }

        if (!trailEmitters.contains(trailEmitter, true)) {
            trailEmitters.add(trailEmitter);
        }
    }

    public boolean removeTrailEmitter(TrailEmitter trailEmitter) {
        return trailEmitters.removeValue(trailEmitter, true);
    }

    public void addForceField(ForceField forceField) {
        if (forceField == null) {
            throw new IllegalArgumentException("Force field must not be null");
        }

        if (!forceFields.contains(forceField, true)) {
            forceFields.add(forceField);
        }
    }

    public boolean removeForceField(ForceField forceField) {
        return forceFields.removeValue(forceField, true);
    }

    public void clearForceFields() {
        forceFields.clear();
    }

    public void setCullingEnabled(boolean cullingEnabled) {
        this.cullingEnabled = cullingEnabled;
    }

    public void setAsyncUpdatesEnabled(boolean asyncUpdatesEnabled) {
        this.asyncUpdatesEnabled = asyncUpdatesEnabled;
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

    public boolean isCullingEnabled() {
        return cullingEnabled;
    }

    public boolean isAsyncUpdatesEnabled() {
        return asyncUpdatesEnabled;
    }

    public int getEffectCount() {
        return effects.size;
    }

    public int getActiveParticleCount() {
        int activeParticleCount = 0;
        for (int i = 0; i < effects.size; i++) {
            activeParticleCount += effects.get(i).getActiveParticleCount();
        }
        for (int i = 0; i < trailEmitters.size; i++) {
            activeParticleCount += trailEmitters.get(i).getActivePointCount();
        }
        return activeParticleCount;
    }

    public void update(float delta, PerspectiveCamera camera) {
        waitForPendingUpdates();

        Array<ParticleEffect> visibleEffects = collectVisibleEffects(camera);
        if (asyncUpdatesEnabled && visibleEffects.size > 1) {
            submitAsyncUpdates(delta, visibleEffects);
            return;
        }

        for (int i = 0; i < visibleEffects.size; i++) {
            visibleEffects.get(i).update(delta, this);
        }

        for (int i = 0; i < trailEmitters.size; i++) {
            trailEmitters.get(i).update(delta);
        }
    }

    public void render(ShaderProgram shaderProgram, PerspectiveCamera camera) {
        waitForPendingUpdates();

        for (int i = 0; i < effects.size; i++) {
            ParticleEffect effect = effects.get(i);
            if (!effect.isActive()) {
                continue;
            }

            if (cullingEnabled && camera != null && !isVisible(effect, camera)) {
                continue;
            }

            effect.render(shaderProgram, camera, depthTexture, viewportWidth, viewportHeight);
        }

        for (int i = 0; i < trailEmitters.size; i++) {
            TrailEmitter trailEmitter = trailEmitters.get(i);
            if (!trailEmitter.isActive()) {
                continue;
            }

            if (cullingEnabled && camera != null && !isVisible(trailEmitter, camera)) {
                continue;
            }

            trailEmitter.render(camera);
        }
    }

    @Override
    public void sampleForce(Vector3 position, float delta, Vector3 outForce) {
        outForce.setZero();
        Vector3 localForce = forceBuffer.get();
        for (int i = 0; i < forceFields.size; i++) {
            forceFields.get(i).sample(position, delta, localForce);
            outForce.add(localForce);
        }
    }

    public void clear() {
        waitForPendingUpdates();
        for (int i = effects.size - 1; i >= 0; i--) {
            effects.get(i).dispose();
        }
        effects.clear();
        for (int i = trailEmitters.size - 1; i >= 0; i--) {
            trailEmitters.get(i).dispose();
        }
        trailEmitters.clear();
        forceFields.clear();
    }

    public void dispose() {
        clear();
        asyncExecutor.dispose();
    }

    private Array<ParticleEffect> collectVisibleEffects(PerspectiveCamera camera) {
        Array<ParticleEffect> visibleEffects = new Array<>(false, effects.size);
        for (int i = 0; i < effects.size; i++) {
            ParticleEffect effect = effects.get(i);
            if (!effect.isActive()) {
                continue;
            }

            if (cullingEnabled && camera != null && !isVisible(effect, camera)) {
                continue;
            }

            float maxLodFactor = 1f;
            if (camera != null) {
                float distanceToCamera = effect.getPosition().dst(camera.position);
                maxLodFactor = effect.applyLodForDistance(distanceToCamera);
            } else {
                effect.setLodFactor(1f);
            }

            if (maxLodFactor <= 0f && effect.getActiveParticleCount() == 0) {
                continue;
            }

            visibleEffects.add(effect);
        }
        return visibleEffects;
    }

    private void submitAsyncUpdates(float delta, Array<ParticleEffect> visibleEffects) {
        pendingUpdates.clear();
        for (int i = 0; i < visibleEffects.size; i++) {
            ParticleEffect effect = visibleEffects.get(i);
            pendingUpdates.add(asyncExecutor.submit(new AsyncTask<Void>() {
                @Override
                public Void call() {
                    effect.update(delta, ParticleManager.this);
                    return null;
                }
            }));
        }
    }

    private void waitForPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            return;
        }

        for (int i = 0; i < pendingUpdates.size; i++) {
            pendingUpdates.get(i).get();
        }
        pendingUpdates.clear();
    }

    private boolean isVisible(ParticleEffect effect, PerspectiveCamera camera) {
        float boundsRadius = effect.getApproximateBoundsRadius();
        return camera.frustum.sphereInFrustum(effect.getPosition(), boundsRadius);
    }

    private boolean isVisible(TrailEmitter trailEmitter, PerspectiveCamera camera) {
        float boundsRadius = trailEmitter.getApproximateBoundsRadius();
        return camera.frustum.sphereInFrustum(trailEmitter.getPosition(), boundsRadius);
    }
}
