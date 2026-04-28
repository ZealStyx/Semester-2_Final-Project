package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Orchestrates body-cam shader pass from scene texture to output and finally to screen.
 */
public final class BodyCamVHSVisualizer {
    private final BodyCamPassFrameBuffer frameBuffer;
    private final ShaderProgram shader;
    private final Mesh fullScreenQuad;
    private final BodyCamVHSAnimator animator;

    public BodyCamVHSVisualizer(
        BodyCamPassFrameBuffer frameBuffer,
        ShaderProgram shader,
        Mesh fullScreenQuad,
        BodyCamVHSAnimator animator
    ) {
        this.frameBuffer = frameBuffer;
        this.shader = shader;
        this.fullScreenQuad = fullScreenQuad;
        this.animator = animator;
    }

    public void renderToBackBuffer(float elapsedSeconds, BodyCamVHSSettings settings) {
        if (!settings.enabled) {
            drawTextureToCurrentTarget(frameBuffer.sceneColorFbo().getColorBufferTexture(), false);
            return;
        }

        animator.update(elapsedSeconds);

        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        frameBuffer.sceneColorFbo().getColorBufferTexture().bind(0);
        bindShaderUniforms(elapsedSeconds, settings);
        fullScreenQuad.render(shader, GL20.GL_TRIANGLES);
    }

    private void bindShaderUniforms(float elapsedSeconds, BodyCamVHSSettings settings) {
        shader.bind();
        shader.setUniformi("u_texture", 0);
        float width = (float) Gdx.graphics.getBackBufferWidth();
        float height = (float) Gdx.graphics.getBackBufferHeight();
        shader.setUniformf("u_resolution", width, height);

        // convert diagonal FOV to vertical FOV based on aspect ratio
        float diagRad = (float) Math.toRadians(settings.fovDiagonalDegrees);
        float aspect = width / Math.max(1f, height);
        float tanDiag2 = (float) Math.tan(diagRad * 0.5f);
        float tanVert2 = tanDiag2 / (float) Math.sqrt(aspect * aspect + 1.0f);
        float verticalFovDeg = (float) Math.toDegrees(2.0f * Math.atan(tanVert2));
        shader.setUniformf("u_verticalFOV", verticalFovDeg);

        // time and motion for grain / dynamic vignette
        shader.setUniformf("u_time", elapsedSeconds);
        float motion = MathUtils.clamp(animator.wobbleStrength(), 0.0f, 1.0f);
        shader.setUniformf("u_motion", motion);

        // distortion parameters: keep the curve stable, vary the overall blend instead
        float strength = MathUtils.clamp(
            0.30f + (0.35f * settings.barrelDistortionStrength) + (0.05f * motion),
            0.35f,
            0.72f
        );
        shader.setUniformf("u_strength", strength);
        shader.setUniformf("u_k1", 0.24f);
        shader.setUniformf("u_k2", 0.06f);

        // chromatic aberration base amount (tuned multiplier)
        shader.setUniformf("u_caAmount", 0.0055f * settings.chromaticAberrationPixels);

        // vignette mapping (inner/outer radii)
        shader.setUniformf("u_vignetteInner", settings.vignetteRadius);
        shader.setUniformf("u_vignetteOuter", settings.vignetteRadius + settings.vignetteSoftness);
    }

    private void drawTextureToCurrentTarget(Texture texture, boolean enableDepth) {
        if (!enableDepth) {
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        }
        texture.bind(0);
        shader.bind();
        shader.setUniformi("u_texture", 0);
        float width = (float) Gdx.graphics.getBackBufferWidth();
        float height = (float) Gdx.graphics.getBackBufferHeight();
        shader.setUniformf("u_resolution", width, height);
        shader.setUniformf("u_verticalFOV", 75.0f);
        shader.setUniformf("u_strength", 0.0f);
        shader.setUniformf("u_k1", 0.32f);
        shader.setUniformf("u_k2", 0.10f);
        shader.setUniformf("u_caAmount", 0.0f);
        shader.setUniformf("u_vignetteInner", 1.0f);
        shader.setUniformf("u_vignetteOuter", 1.0f);
        shader.setUniformf("u_time", 0.0f);
        shader.setUniformf("u_motion", 0.0f);
        fullScreenQuad.render(shader, GL20.GL_TRIANGLES);
    }

    private void setOptionalUniform(String name, float value) {
        if (shader.hasUniform(name)) {
            shader.setUniformf(name, value);
        }
    }
}
