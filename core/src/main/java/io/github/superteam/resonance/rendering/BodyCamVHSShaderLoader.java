package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Compiles the unified body cam post-process shader.
 */
public final class BodyCamVHSShaderLoader {
    private final ShaderProgram shaderProgram;

    public BodyCamVHSShaderLoader(String vertexPath, String fragmentPath) {
        shaderProgram = new ShaderProgram(
            Gdx.files.internal(vertexPath),
            Gdx.files.internal(fragmentPath)
        );

        if (!shaderProgram.isCompiled()) {
            throw new GdxRuntimeException("BodyCam shader failed to compile: " + shaderProgram.getLog());
        }
    }

    public ShaderProgram shader() {
        return shaderProgram;
    }

    public void dispose() {
        shaderProgram.dispose();
    }
}
