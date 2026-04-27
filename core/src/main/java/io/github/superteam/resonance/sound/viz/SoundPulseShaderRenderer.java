package io.github.superteam.resonance.sound.viz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Renders directional surface-scan pulse rays and bounce markers.
 */
public final class SoundPulseShaderRenderer implements Disposable {
    private static final float BASE_TRANSMITTED_ALPHA_SCALE = 0.35f;
    private static final float FRONT_MARKER_HALF = 0.04f;
    private static final float GLOW_MARKER_HALF = 0.12f;
    private static final int RING_SEGMENTS = 12;
    private static final boolean RENDER_RAY_LINES = false;
    private static final float CONTACT_POINT_RADIUS = 0.22f;
    private static final float CONTACT_CORE_RADIUS = 0.08f;
    private static final float CONTACT_SURFACE_OFFSET = 0.03f;

    private static final Color PRIMARY_COLOR = new Color(0.2f, 0.9f, 1.0f, 1.0f);

    private final SurfaceScanPulse surfaceScanPulse;
    private final ShapeRenderer shapeRenderer;
    private final Mesh contactMesh;
    private final ShaderProgram contactShader;
    private final Vector3 tmpBasisUp = new Vector3(0f, 1f, 0f);
    private final Vector3 tmpBasisSide = new Vector3(1f, 0f, 0f);
    private final Vector3 tmpTangent = new Vector3();
    private final Vector3 tmpBitangent = new Vector3();
    private final Vector3 tmpCenter = new Vector3();

    public SoundPulseShaderRenderer(Array<BoundingBox> colliders) {
        this.surfaceScanPulse = new SurfaceScanPulse(colliders);
        this.shapeRenderer = new ShapeRenderer();
        this.contactMesh = createContactMesh();
        this.contactShader = createContactShader();
    }

    public void fire(Vector3 worldOrigin, float normalizedIntensity) {
        if (worldOrigin == null) {
            return;
        }

        float clampedIntensity = MathUtils.clamp(normalizedIntensity, 0.05f, 1f);
        surfaceScanPulse.fire(worldOrigin, clampedIntensity);
    }

    public void fireTransmitted(Vector3 worldOrigin, float normalizedIntensity, float transmissionCoefficient) {
        if (worldOrigin == null) {
            return;
        }

        float clampedIntensity = MathUtils.clamp(normalizedIntensity, 0.05f, 1f);
        float clampedTransmission = MathUtils.clamp(transmissionCoefficient, 0.01f, 1f);
        float transmittedAlphaScale = MathUtils.clamp(clampedTransmission * BASE_TRANSMITTED_ALPHA_SCALE, 0.05f, 0.75f);
        surfaceScanPulse.fire(worldOrigin, clampedIntensity * transmittedAlphaScale);
    }

    public void render(PerspectiveCamera camera, float deltaSeconds) {
        if (camera == null) {
            return;
        }

        surfaceScanPulse.update(deltaSeconds);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (SurfaceScanPulse.PulseState pulseState : surfaceScanPulse.pulseStates()) {
            // Pass A: moving wave-front markers.
            for (SurfaceScanPulse.RayState ray : pulseState.rays()) {
                Vector3 start = ray.origin();
                Vector3 front = new Vector3(start).mulAdd(ray.direction(), ray.effectiveFrontDistance());

                float depthTint = MathUtils.clamp(1f - (ray.bounceCount() * 0.22f), 0.35f, 1f);
                float alpha = MathUtils.clamp(ray.intensity(), 0.1f, 1f) * depthTint;

                shapeRenderer.setColor(
                    PRIMARY_COLOR.r * depthTint,
                    MathUtils.clamp(PRIMARY_COLOR.g * depthTint + 0.05f, 0f, 1f),
                    MathUtils.clamp(PRIMARY_COLOR.b * depthTint + 0.1f, 0f, 1f),
                    alpha
                );

                if (RENDER_RAY_LINES) {
                    shapeRenderer.line(start, front);
                }

                if (!ray.isFinished()) {
                    shapeRenderer.line(front.x - FRONT_MARKER_HALF, front.y, front.z, front.x + FRONT_MARKER_HALF, front.y, front.z);
                    shapeRenderer.line(front.x, front.y - FRONT_MARKER_HALF, front.z, front.x, front.y + FRONT_MARKER_HALF, front.z);
                    shapeRenderer.line(front.x, front.y, front.z - FRONT_MARKER_HALF, front.x, front.y, front.z + FRONT_MARKER_HALF);
                }
            }

            // Pass B: persistent surface glow markers and rings.
            for (SurfaceScanPulse.SurfaceGlow glow : pulseState.glows()) {
                float glowAlpha = glow.alpha();
                if (glowAlpha < 0.02f) {
                    continue;
                }

                float depthTint = MathUtils.clamp(1f - glow.bounceCount() * 0.28f, 0.3f, 1f);
                float fadeT = glow.fadeProgress();

                float r = MathUtils.lerp(1.0f, 0.1f, fadeT) * depthTint;
                float g = MathUtils.lerp(1.0f, 0.85f, fadeT) * depthTint;
                float b = MathUtils.lerp(1.0f, 0.95f, fadeT) * depthTint;
                shapeRenderer.setColor(r, g, b, glowAlpha);

                Vector3 p = glow.point();
                float cross = GLOW_MARKER_HALF * (1f + (1f - fadeT) * 0.6f);

                shapeRenderer.line(p.x - cross, p.y, p.z, p.x + cross, p.y, p.z);
                shapeRenderer.line(p.x, p.y - cross, p.z, p.x, p.y + cross, p.z);
                shapeRenderer.line(p.x, p.y, p.z - cross, p.x, p.y, p.z + cross);

                drawSurfaceRing(shapeRenderer, p, glow.normal(), cross * 1.8f, glowAlpha * 0.55f, r, g, b);
            }
        }

        shapeRenderer.end();

        renderContactGlows(camera, surfaceScanPulse.pulseStates());
    }

    private void renderContactGlows(PerspectiveCamera camera, Iterable<SurfaceScanPulse.PulseState> pulseStates) {
        if (contactShader == null || contactShader.isCompiled() == false) {
            return;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        contactShader.bind();
        contactShader.setUniformMatrix("u_projViewTrans", camera.combined);
        contactShader.setUniformf("u_time", Gdx.graphics.getDeltaTime());

        for (SurfaceScanPulse.PulseState pulseState : pulseStates) {
            for (SurfaceScanPulse.SurfaceGlow glow : pulseState.glows()) {
                float alpha = glow.alpha();
                if (alpha < 0.01f) {
                    continue;
                }

                Vector3 point = glow.point();
                Vector3 normal = glow.normal().nor();
                float depthTint = MathUtils.clamp(1f - glow.bounceCount() * 0.2f, 0.35f, 1f);
                float size = CONTACT_POINT_RADIUS * (0.85f + alpha * 1.4f);
                float core = CONTACT_CORE_RADIUS * (0.8f + alpha);

                Vector3 referenceAxis = Math.abs(normal.dot(tmpBasisUp)) < 0.95f ? tmpBasisUp : tmpBasisSide;
                tmpTangent.set(referenceAxis).crs(normal).nor();
                tmpBitangent.set(normal).crs(tmpTangent).nor();
                tmpCenter.set(point).mulAdd(normal, CONTACT_SURFACE_OFFSET);

                contactShader.setUniformf("u_center", tmpCenter);
                contactShader.setUniformf("u_radius", size);
                contactShader.setUniformf("u_coreRadius", core);
                contactShader.setUniformf("u_tangent", tmpTangent);
                contactShader.setUniformf("u_bitangent", tmpBitangent);
                contactShader.setUniformf("u_color", PRIMARY_COLOR.r * depthTint, PRIMARY_COLOR.g, PRIMARY_COLOR.b, alpha);

                contactMesh.render(contactShader, GL20.GL_TRIANGLES);
            }
        }
    }

    private Mesh createContactMesh() {
        Mesh mesh = new Mesh(
            true,
            4,
            6,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")
        );
        mesh.setVertices(new float[] {
            -1f, -1f,
             1f, -1f,
             1f,  1f,
            -1f,  1f
        });
        mesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});
        return mesh;
    }

    private ShaderProgram createContactShader() {
        ShaderProgram.pedantic = false;
        String vertexShader = """
            attribute vec2 a_position;

            uniform mat4 u_projViewTrans;
            uniform vec3 u_center;
            uniform vec3 u_tangent;
            uniform vec3 u_bitangent;
            uniform float u_radius;

            varying vec2 v_uv;

            void main() {
                vec3 worldPos = u_center + (u_tangent * a_position.x * u_radius) + (u_bitangent * a_position.y * u_radius);
                v_uv = a_position * 0.5 + 0.5;
                gl_Position = u_projViewTrans * vec4(worldPos, 1.0);
            }
            """;

        String fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            uniform vec4 u_color;
            uniform float u_coreRadius;
            uniform float u_radius;
            uniform float u_time;

            varying vec2 v_uv;

            void main() {
                vec2 centered = v_uv - vec2(0.5);
                float dist = length(centered);
                float outer = 1.0 - smoothstep(0.25, 0.5, dist);
                float core = 1.0 - smoothstep(0.0, 0.16, dist);
                float pulse = 0.82 + 0.18 * sin(u_time * 9.0 + dist * 24.0);
                float alpha = max(outer * 0.9, core * 1.4) * u_color.a * pulse;
                if (alpha < 0.015) {
                    discard;
                }
                vec3 color = mix(u_color.rgb * 1.9, vec3(1.0, 1.0, 0.95), core * 0.45);
                gl_FragColor = vec4(color, alpha);
            }
            """;

        ShaderProgram shaderProgram = new ShaderProgram(vertexShader, fragmentShader);
        if (!shaderProgram.isCompiled()) {
            throw new IllegalStateException("Contact pulse shader failed to compile: " + shaderProgram.getLog());
        }
        return shaderProgram;
    }

    private void drawSurfaceRing(
        ShapeRenderer shapes,
        Vector3 center,
        Vector3 normal,
        float radius,
        float alpha,
        float r,
        float g,
        float b
    ) {
        Vector3 up = Math.abs(normal.y) < 0.9f ? new Vector3(0f, 1f, 0f) : new Vector3(1f, 0f, 0f);
        Vector3 tangent = new Vector3(normal).crs(up).nor();
        Vector3 bitangent = new Vector3(normal).crs(tangent).nor();

        shapes.setColor(r, g, b, alpha);
        float stepAngle = MathUtils.PI2 / RING_SEGMENTS;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = stepAngle * i;
            float a1 = stepAngle * (i + 1);

            Vector3 p0 = new Vector3(center).add(
                tangent.x * MathUtils.cos(a0) * radius + bitangent.x * MathUtils.sin(a0) * radius,
                tangent.y * MathUtils.cos(a0) * radius + bitangent.y * MathUtils.sin(a0) * radius,
                tangent.z * MathUtils.cos(a0) * radius + bitangent.z * MathUtils.sin(a0) * radius
            );
            Vector3 p1 = new Vector3(center).add(
                tangent.x * MathUtils.cos(a1) * radius + bitangent.x * MathUtils.sin(a1) * radius,
                tangent.y * MathUtils.cos(a1) * radius + bitangent.y * MathUtils.sin(a1) * radius,
                tangent.z * MathUtils.cos(a1) * radius + bitangent.z * MathUtils.sin(a1) * radius
            );

            shapes.line(p0, p1);
        }
    }

    @Override
    public void dispose() {
        contactMesh.dispose();
        contactShader.dispose();
        shapeRenderer.dispose();
    }
}
