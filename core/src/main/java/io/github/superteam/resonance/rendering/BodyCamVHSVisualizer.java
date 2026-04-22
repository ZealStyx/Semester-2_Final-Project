package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.Gdx;
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
        shader.setUniformf("u_screenSize", (float) Gdx.graphics.getBackBufferWidth(), (float) Gdx.graphics.getBackBufferHeight());
        shader.setUniformf("u_time", elapsedSeconds);
        shader.setUniformf("u_vhsStrength", settings.vhsTapeNoiseAmount);
        shader.setUniformf("u_fovDiagonalDegrees", settings.fovDiagonalDegrees);
        shader.setUniformf("u_barrelDistortionStrength", settings.barrelDistortionStrength + animator.wobbleStrength());
        shader.setUniformf("u_chromaticAberrationPixels", settings.chromaticAberrationPixels);
        shader.setUniformf("u_vhsScanLineStrength", settings.vhsScanLineStrength);
        shader.setUniformf("u_vhsTapeNoiseAmount", settings.vhsTapeNoiseAmount);
        shader.setUniformf("u_crtCurveAmount", settings.crtCurveAmount);
        setOptionalUniform("u_vignetteRadius", settings.vignetteRadius);
        setOptionalUniform("u_vignetteSoftness", settings.vignetteSoftness);
    }

    private void drawTextureToCurrentTarget(Texture texture, boolean enableDepth) {
        if (!enableDepth) {
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        }
        texture.bind(0);
        shader.bind();
        shader.setUniformi("u_texture", 0);
        shader.setUniformf("u_screenSize", (float) Gdx.graphics.getBackBufferWidth(), (float) Gdx.graphics.getBackBufferHeight());
        shader.setUniformf("u_time", 0.0f);
        shader.setUniformf("u_vhsStrength", 0.0f);
        shader.setUniformf("u_fovDiagonalDegrees", 90.0f);
        shader.setUniformf("u_barrelDistortionStrength", 0.0f);
        shader.setUniformf("u_chromaticAberrationPixels", 0.0f);
        shader.setUniformf("u_vhsScanLineStrength", 0.0f);
        shader.setUniformf("u_vhsTapeNoiseAmount", 0.0f);
        shader.setUniformf("u_crtCurveAmount", 0.0f);
        setOptionalUniform("u_vignetteRadius", 0.0f);
        setOptionalUniform("u_vignetteSoftness", 0.0f);
        fullScreenQuad.render(shader, GL20.GL_TRIANGLES);
    }

    private void setOptionalUniform(String name, float value) {
        if (shader.hasUniform(name)) {
            shader.setUniformf(name, value);
        }
    }
}
