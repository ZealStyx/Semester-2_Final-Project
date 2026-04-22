package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Renders expanding world-space sound pulses using a dedicated shader shell.
 */
public final class SoundPulseShaderRenderer implements Disposable {
    private static final float PULSE_SPEED_METERS_PER_SECOND = 12f;
    private static final float PULSE_MAX_RADIUS_METERS = 22f;
    private static final float PULSE_THICKNESS_METERS = 0.35f;
    private static final float BASE_TRANSMITTED_ALPHA_SCALE = 0.35f;

    private static final Color PRIMARY_COLOR = new Color(0.2f, 0.9f, 1.0f, 1.0f);
    private static final Color TRANSMITTED_COLOR = new Color(0.55f, 0.7f, 1.0f, 1.0f);

    private final Array<PulseInstance> activePulses = new Array<>();
    private final Matrix4 worldTransform = new Matrix4();

    private final ShaderProgram shaderProgram;
    private final Mesh sphereMesh;
    private final float pulseLifetimeSeconds;

    public SoundPulseShaderRenderer() {
        ShaderProgram.pedantic = false;
        shaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/sound_pulse.vert"),
            Gdx.files.internal("shaders/frag/sound_pulse.frag")
        );
        if (!shaderProgram.isCompiled()) {
            throw new GdxRuntimeException("Sound pulse shader failed to compile: " + shaderProgram.getLog());
        }

        sphereMesh = createSphereMesh(20, 20);
        pulseLifetimeSeconds = PULSE_MAX_RADIUS_METERS / PULSE_SPEED_METERS_PER_SECOND;
    }

    public void fire(Vector3 worldOrigin, float normalizedIntensity) {
        if (worldOrigin == null) {
            return;
        }

        float clampedIntensity = MathUtils.clamp(normalizedIntensity, 0.05f, 1f);
        activePulses.add(new PulseInstance(new Vector3(worldOrigin), clampedIntensity, PRIMARY_COLOR));
    }

    public void fireTransmitted(Vector3 worldOrigin, float normalizedIntensity, float transmissionCoefficient) {
        if (worldOrigin == null) {
            return;
        }

        float clampedIntensity = MathUtils.clamp(normalizedIntensity, 0.05f, 1f);
        float clampedTransmission = MathUtils.clamp(transmissionCoefficient, 0.01f, 1f);
        float transmittedAlphaScale = MathUtils.clamp(clampedTransmission * BASE_TRANSMITTED_ALPHA_SCALE, 0.05f, 0.75f);
        activePulses.add(new PulseInstance(new Vector3(worldOrigin), clampedIntensity * transmittedAlphaScale, TRANSMITTED_COLOR));
    }

    public void render(PerspectiveCamera camera, float deltaSeconds) {
        if (camera == null || activePulses.isEmpty()) {
            return;
        }

        float clampedDelta = Math.max(0f, deltaSeconds);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        Gdx.gl.glDepthMask(false);

        shaderProgram.bind();
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);

        for (int index = activePulses.size - 1; index >= 0; index--) {
            PulseInstance pulse = activePulses.get(index);
            pulse.elapsedSeconds += clampedDelta;
            if (pulse.elapsedSeconds >= pulseLifetimeSeconds) {
                activePulses.removeIndex(index);
                continue;
            }

            float radius = pulse.elapsedSeconds * PULSE_SPEED_METERS_PER_SECOND;
            float lifeNormalized = pulse.elapsedSeconds / pulseLifetimeSeconds;
            float alpha = (1f - lifeNormalized) * (1f - lifeNormalized) * pulse.intensityScale;

            worldTransform.idt();
            worldTransform.translate(pulse.origin);
            worldTransform.scale(radius, radius, radius);

            shaderProgram.setUniformMatrix("u_worldTransform", worldTransform);
            shaderProgram.setUniformf("u_pulseOrigin", pulse.origin);
            shaderProgram.setUniformf("u_pulseRadius", radius);
            shaderProgram.setUniformf("u_pulseThickness", PULSE_THICKNESS_METERS);
            shaderProgram.setUniformf("u_pulseAlpha", alpha);
            shaderProgram.setUniformf("u_pulseColor", PRIMARY_COLOR);
            shaderProgram.setUniformf("u_colorScale", pulse.color.r, pulse.color.g, pulse.color.b);
            shaderProgram.setUniformf("u_time", pulse.elapsedSeconds);

            sphereMesh.render(shaderProgram, GL20.GL_TRIANGLES);
        }

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        shaderProgram.dispose();
        sphereMesh.dispose();
    }

    private Mesh createSphereMesh(int latitudeSegments, int longitudeSegments) {
        int verticalSegments = Math.max(3, latitudeSegments);
        int horizontalSegments = Math.max(3, longitudeSegments);

        int vertexCount = (verticalSegments + 1) * (horizontalSegments + 1);
        int triangleCount = verticalSegments * horizontalSegments * 2;

        float[] vertices = new float[vertexCount * 3];
        short[] indices = new short[triangleCount * 3];

        int vertexOffset = 0;
        for (int lat = 0; lat <= verticalSegments; lat++) {
            float v = lat / (float) verticalSegments;
            float theta = v * MathUtils.PI;
            float sinTheta = MathUtils.sin(theta);
            float cosTheta = MathUtils.cos(theta);

            for (int lon = 0; lon <= horizontalSegments; lon++) {
                float u = lon / (float) horizontalSegments;
                float phi = u * MathUtils.PI2;
                float sinPhi = MathUtils.sin(phi);
                float cosPhi = MathUtils.cos(phi);

                vertices[vertexOffset++] = sinTheta * cosPhi;
                vertices[vertexOffset++] = cosTheta;
                vertices[vertexOffset++] = sinTheta * sinPhi;
            }
        }

        int indexOffset = 0;
        int stride = horizontalSegments + 1;
        for (int lat = 0; lat < verticalSegments; lat++) {
            for (int lon = 0; lon < horizontalSegments; lon++) {
                int first = (lat * stride) + lon;
                int second = first + stride;

                indices[indexOffset++] = (short) first;
                indices[indexOffset++] = (short) second;
                indices[indexOffset++] = (short) (first + 1);

                indices[indexOffset++] = (short) (first + 1);
                indices[indexOffset++] = (short) second;
                indices[indexOffset++] = (short) (second + 1);
            }
        }

        Mesh mesh = new Mesh(
            true,
            vertexCount,
            indices.length,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position")
        );
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    private static final class PulseInstance {
        private final Vector3 origin;
        private final float intensityScale;
        private final Color color;
        private float elapsedSeconds;

        private PulseInstance(Vector3 origin, float intensityScale, Color color) {
            this.origin = origin;
            this.intensityScale = intensityScale;
            this.color = new Color(color);
        }
    }
}
