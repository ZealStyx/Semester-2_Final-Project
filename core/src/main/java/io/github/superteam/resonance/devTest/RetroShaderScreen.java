package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Minimal shader sandbox for the 1x1x1 retro block test. */
public class RetroShaderScreen extends ScreenAdapter {
    private static final float[] CUBE_VERTICES = {
        // Front face
        -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
         0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
         0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
        -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,

        // Back face
         0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
        -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
        -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
         0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,

        // Left face
        -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,
        -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
        -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,
        -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,

        // Right face
         0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,
         0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
         0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,
         0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,

        // Top face
        -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
         0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,
         0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,
        -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,

        // Bottom face
        -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
         0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,
         0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,
        -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f
    };

    private static final short[] CUBE_INDICES = {
         0,  1,  2,  2,  3,  0,
         4,  5,  6,  6,  7,  4,
         8,  9, 10, 10, 11,  8,
        12, 13, 14, 14, 15, 12,
        16, 17, 18, 18, 19, 16,
        20, 21, 22, 22, 23, 20
    };

    private final PerspectiveCamera camera;
    private final Mesh cubeMesh;
    private final Mesh screenQuadMesh;
    private final ShaderProgram objectShaderProgram;
    private final ShaderProgram vhsShaderProgram;
    private FrameBuffer sceneFrameBuffer;
    private final Matrix4 modelMatrix = new Matrix4();
    private final Vector3 lightDirection = new Vector3(0.35f, 1.0f, 0.25f).nor();
    private final float shadowStrength = 0.7f;
    private final float scanlineStrength = 0.18f;
    private final float ditherLevels = 8.0f;
    private final float phosphorMaskStrength = 0.4f;
    private final float vhsStrength = 1.0f;
    private float elapsedTime;

    public RetroShaderScreen() {
        ShaderProgram.pedantic = false;

        camera = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(2.6f, 2.2f, 3.6f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        cubeMesh = new Mesh(
            true,
            24,
            36,
            new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal")
            )
        );
        cubeMesh.setVertices(CUBE_VERTICES);
        cubeMesh.setIndices(CUBE_INDICES);

        screenQuadMesh = new Mesh(
            true,
            4,
            6,
            new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
            )
        );
        screenQuadMesh.setVertices(new float[] {
            -1f, -1f, 0f, 0f, 0f,
             1f, -1f, 0f, 1f, 0f,
             1f,  1f, 0f, 1f, 1f,
            -1f,  1f, 0f, 0f, 1f
        });
        screenQuadMesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});

        objectShaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/retro_shader.vert"),
            Gdx.files.internal("shaders/frag/retro_shader.frag")
        );
        if (!objectShaderProgram.isCompiled()) {
            throw new GdxRuntimeException("Retro shader failed to compile: " + objectShaderProgram.getLog());
        }

        vhsShaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/vhs_postprocess.vert"),
            Gdx.files.internal("shaders/frag/vhs_postprocess.frag")
        );
        if (!vhsShaderProgram.isCompiled()) {
            throw new GdxRuntimeException("VHS shader failed to compile: " + vhsShaderProgram.getLog());
        }

        rebuildFrameBuffer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;

        camera.update();
        sceneFrameBuffer.begin();
        Gdx.gl.glViewport(0, 0, sceneFrameBuffer.getWidth(), sceneFrameBuffer.getHeight());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glClearColor(0.05f, 0.03f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelMatrix.idt()
            .rotate(Vector3.Y, elapsedTime * 35f)
            .rotate(Vector3.X, 20f);

        objectShaderProgram.bind();
        objectShaderProgram.setUniformMatrix("u_projViewTrans", camera.combined);
        objectShaderProgram.setUniformMatrix("u_modelTrans", modelMatrix);
        objectShaderProgram.setUniformf("u_cameraPos", camera.position.x, camera.position.y, camera.position.z);
        objectShaderProgram.setUniformf("u_lightDirection", lightDirection);
        objectShaderProgram.setUniformf("u_baseColor", 0.72f, 0.38f, 0.22f);
        objectShaderProgram.setUniformf("u_ambientColor", 0.24f, 0.16f, 0.32f);
        objectShaderProgram.setUniformf("u_shadowColor", 0.04f, 0.03f, 0.06f);
        objectShaderProgram.setUniformf("u_shadowStrength", shadowStrength);
        objectShaderProgram.setUniformf("u_fogColor", 0.12f, 0.08f, 0.18f);
        objectShaderProgram.setUniformf("u_blindFogColor", 0.03f, 0.02f, 0.04f);
        objectShaderProgram.setUniformf("u_fogStart", 4.0f);
        objectShaderProgram.setUniformf("u_fogEnd", 8.0f);
        objectShaderProgram.setUniformf("u_blindFogStart", 3.0f);
        objectShaderProgram.setUniformf("u_blindFogEnd", 5.5f);
        objectShaderProgram.setUniformf("u_blindFogStrength", 0.95f);
        objectShaderProgram.setUniformf("u_time", elapsedTime);
        objectShaderProgram.setUniformf("u_scanlineStrength", scanlineStrength);
        objectShaderProgram.setUniformf("u_ditherLevels", ditherLevels);
        objectShaderProgram.setUniformf("u_phosphorMaskStrength", phosphorMaskStrength);

        cubeMesh.render(objectShaderProgram, GL20.GL_TRIANGLES);

        sceneFrameBuffer.end();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        vhsShaderProgram.bind();
        sceneFrameBuffer.getColorBufferTexture().bind(0);
        vhsShaderProgram.setUniformi("u_texture", 0);
        vhsShaderProgram.setUniformf("u_screenSize", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        vhsShaderProgram.setUniformf("u_time", elapsedTime);
        vhsShaderProgram.setUniformf("u_vhsStrength", vhsStrength);

        screenQuadMesh.render(vhsShaderProgram, GL20.GL_TRIANGLES);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        rebuildFrameBuffer(width, height);
    }

    @Override
    public void dispose() {
        if (sceneFrameBuffer != null) {
            sceneFrameBuffer.dispose();
        }
        cubeMesh.dispose();
        screenQuadMesh.dispose();
        objectShaderProgram.dispose();
        vhsShaderProgram.dispose();
    }

    private void rebuildFrameBuffer(int width, int height) {
        if (sceneFrameBuffer != null) {
            sceneFrameBuffer.dispose();
        }

        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        sceneFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, safeWidth, safeHeight, true);
        Texture frameBufferTexture = sceneFrameBuffer.getColorBufferTexture();
        frameBufferTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }
}