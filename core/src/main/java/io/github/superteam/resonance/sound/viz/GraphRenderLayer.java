package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.sound.AcousticGraphEngine;
import io.github.superteam.resonance.sound.GraphEdge;
import io.github.superteam.resonance.sound.GraphNode;
import io.github.superteam.resonance.sound.PropagationResult;

/**
 * Draws Dijkstra graph edges for recent sound events with fade-out.
 */
public final class GraphRenderLayer {
    private final AcousticGraphEngine acousticGraphEngine;
    private final Array<EdgeSample> activeSamples = new Array<>();

    private AcousticBounceConfig.GraphLayerConfig config;

    public GraphRenderLayer(AcousticGraphEngine acousticGraphEngine, AcousticBounceConfig.GraphLayerConfig config) {
        this.acousticGraphEngine = acousticGraphEngine;
        this.config = config;
    }

    public void setConfig(AcousticBounceConfig.GraphLayerConfig config) {
        this.config = config;
    }

    public void activate(PropagationResult propagationResult, float sourceIntensity) {
        if (propagationResult == null || !config.renderEdges) {
            return;
        }

        float clampedSourceIntensity = MathUtils.clamp(sourceIntensity, 0.05f, 1f);

        for (GraphEdge edge : acousticGraphEngine.getEdges()) {
            float fromDistance = propagationResult.getDistanceOrInfinity(edge.fromNodeId());
            float toDistance = propagationResult.getDistanceOrInfinity(edge.toNodeId());
            if (!Float.isFinite(fromDistance) || !Float.isFinite(toDistance)) {
                continue;
            }

            int bounceDepth = resolveBounceDepth(Math.min(fromDistance, toDistance));
            if (bounceDepth > config.maxBounceDepth) {
                continue;
            }
            GraphNode fromNode = acousticGraphEngine.requireNode(edge.fromNodeId());
            GraphNode toNode = acousticGraphEngine.requireNode(edge.toNodeId());
            Color color = colorForBounceDepth(bounceDepth);
            activeSamples.add(new EdgeSample(
                new Vector3(fromNode.position()),
                new Vector3(toNode.position()),
                new Color(color),
                clampedSourceIntensity,
                config.fadeOutSeconds
            ));
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

    private Color colorForBounceDepth(int bounceDepth) {
        if (bounceDepth <= 0) {
            return config.colorBounce0;
        }
        if (bounceDepth == 1) {
            return config.colorBounce1;
        }
        if (bounceDepth == 2) {
            return config.colorBounce2;
        }
        return config.colorBounce3;
    }

    public void update(float deltaSeconds) {
        float clampedDelta = Math.max(0f, deltaSeconds);
        for (int i = activeSamples.size - 1; i >= 0; i--) {
            EdgeSample sample = activeSamples.get(i);
            sample.remainingSeconds -= clampedDelta;
            if (sample.remainingSeconds <= 0f) {
                activeSamples.removeIndex(i);
            }
        }
    }

    public void render(ShapeRenderer shapes, PerspectiveCamera camera) {
        if (!config.renderEdges || activeSamples.isEmpty()) {
            return;
        }

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (EdgeSample sample : activeSamples) {
            float alpha = MathUtils.clamp(sample.remainingSeconds / Math.max(0.0001f, sample.initialSeconds), 0f, 1f);
            float strengthBoost = 0.35f + (sample.sourceIntensity * 0.65f);
            float red = MathUtils.clamp(sample.color.r * (0.75f + (sample.sourceIntensity * 0.25f)) + ((1f - sample.color.r) * sample.sourceIntensity * 0.45f), 0f, 1f);
            float green = MathUtils.clamp(sample.color.g * strengthBoost + (sample.sourceIntensity * 0.10f), 0f, 1f);
            float blue = MathUtils.clamp(sample.color.b * strengthBoost + (sample.sourceIntensity * 0.15f), 0f, 1f);
            float renderedAlpha = sample.color.a * alpha * strengthBoost;

            shapes.setColor(red, green, blue, renderedAlpha);
            shapes.line(sample.from.x, sample.from.y + 0.03f, sample.from.z, sample.to.x, sample.to.y + 0.03f, sample.to.z);
        }
        shapes.end();
    }

    public int activeEdgeCount() {
        return activeSamples.size;
    }

    private static final class EdgeSample {
        private final Vector3 from;
        private final Vector3 to;
        private final Color color;
        private final float sourceIntensity;
        private final float initialSeconds;
        private float remainingSeconds;

        private EdgeSample(Vector3 from, Vector3 to, Color color, float sourceIntensity, float lifetimeSeconds) {
            this.from = from;
            this.to = to;
            this.color = color;
            this.sourceIntensity = sourceIntensity;
            this.initialSeconds = Math.max(0.01f, lifetimeSeconds);
            this.remainingSeconds = this.initialSeconds;
        }
    }
}
