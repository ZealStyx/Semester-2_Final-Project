package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.collision.BoundingBox;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.PropagationResult;
import io.github.superteam.resonance.sound.SoundEventData;

/**
 * Orchestrates graph and geometric bounce visual layers.
 */
public final class AcousticBounce3DVisualizer {
    private static final int MAX_THROTTLED_UPDATE_STEPS = 2;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final GraphRenderLayer graphRenderLayer;
    private final GeometricRayLayer geometricRayLayer;

    private AcousticBounceConfig config;
    private VisualizationMode mode = VisualizationMode.BOTH;
    private SoundEventData lastEventData;
    private SoundEventData pendingSoundEventData;
    private PropagationResult pendingPropagationResult;
    private float updateAccumulatorSeconds;

    public AcousticBounce3DVisualizer(AcousticGraphEngine acousticGraphEngine, Array<BoundingBox> colliders, AcousticBounceConfig config) {
        this.config = config == null ? new AcousticBounceConfig() : config;
        this.config.validate();
        this.graphRenderLayer = new GraphRenderLayer(acousticGraphEngine, this.config.graphLayer);
        this.geometricRayLayer = new GeometricRayLayer(colliders, this.config.geometricLayer);
    }

    public void reloadConfig(AcousticBounceConfig config) {
        if (config == null) {
            return;
        }
        this.config = config;
        this.config.validate();
        this.graphRenderLayer.setConfig(this.config.graphLayer);
        this.geometricRayLayer.setConfig(this.config.geometricLayer);
    }

    public void onSoundEvent(SoundEventData soundEventData, PropagationResult propagationResult) {
        if (!config.enabled || soundEventData == null) {
            return;
        }

        lastEventData = soundEventData;
        pendingSoundEventData = soundEventData;
        pendingPropagationResult = propagationResult;
    }

    private void flushPendingEvent() {
        if (pendingSoundEventData == null) {
            return;
        }

        float strength = pendingSoundEventData.baseIntensity();

        if (
            pendingPropagationResult != null
                && (mode == VisualizationMode.GRAPH_ONLY || mode == VisualizationMode.BOTH)
                && config.graphLayer.renderEdges
        ) {
            graphRenderLayer.activate(pendingPropagationResult, strength);
        }

        if ((mode == VisualizationMode.RAYS_ONLY || mode == VisualizationMode.BOTH) && config.geometricLayer.renderRays) {
            Vector3 sourcePosition = pendingSoundEventData.worldPosition();
            geometricRayLayer.fireRays(sourcePosition, strength);
        }

        pendingSoundEventData = null;
        pendingPropagationResult = null;
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        updateAccumulatorSeconds += clampedDelta;

        float updateStepSeconds = Math.max(1f / 240f, config.updateThrottleSeconds);
        int steps = 0;
        while (updateAccumulatorSeconds >= updateStepSeconds && steps < MAX_THROTTLED_UPDATE_STEPS) {
            updateAccumulatorSeconds -= updateStepSeconds;
            flushPendingEvent();
            graphRenderLayer.update(updateStepSeconds);
            geometricRayLayer.update(updateStepSeconds);
            steps++;
        }
    }

    public void render(PerspectiveCamera camera) {
        if (!config.enabled || mode == VisualizationMode.OFF) {
            return;
        }
        graphRenderLayer.render(shapeRenderer, camera);
        geometricRayLayer.render(shapeRenderer, camera);
    }

    public void cycleMode() {
        mode = switch (mode) {
            case OFF -> VisualizationMode.GRAPH_ONLY;
            case GRAPH_ONLY -> VisualizationMode.RAYS_ONLY;
            case RAYS_ONLY -> VisualizationMode.BOTH;
            case BOTH -> VisualizationMode.OFF;
        };
    }

    public String modeLabel() {
        return mode.name();
    }

    public int activeEdgeCount() {
        return graphRenderLayer.activeEdgeCount();
    }

    public int activeRayCount() {
        return geometricRayLayer.activeRayCount();
    }

    public SoundEventData lastEventData() {
        return lastEventData;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    private enum VisualizationMode {
        OFF,
        GRAPH_ONLY,
        RAYS_ONLY,
        BOTH
    }
}
