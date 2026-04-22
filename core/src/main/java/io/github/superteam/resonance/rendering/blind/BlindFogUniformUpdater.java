package io.github.superteam.resonance.rendering.blind;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Applies blind fog uniforms consistently across shaders.
 */
public final class BlindFogUniformUpdater {
    public void updateBlindUniforms(
        ShaderProgram shader,
        float fogStart,
        float fogEnd,
        float fogStrength,
        Color fogColor
    ) {
        if (shader == null || fogColor == null) {
            return;
        }

        shader.setUniformf("u_blindFogStart", fogStart);
        shader.setUniformf("u_blindFogEnd", fogEnd);
        shader.setUniformf("u_blindFogStrength", fogStrength);
        shader.setUniformf("u_blindFogColor", fogColor.r, fogColor.g, fogColor.b);
    }
}
