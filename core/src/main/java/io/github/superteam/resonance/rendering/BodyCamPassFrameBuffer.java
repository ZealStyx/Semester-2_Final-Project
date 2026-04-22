package io.github.superteam.resonance.rendering;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Manages scene input and body-cam output render targets.
 */
public final class BodyCamPassFrameBuffer {
    private FrameBuffer sceneColorFbo;
    private int width = 1;
    private int height = 1;

    public void resize(int targetWidth, int targetHeight) {
        width = Math.max(1, targetWidth);
        height = Math.max(1, targetHeight);

        if (sceneColorFbo != null) {
            sceneColorFbo.dispose();
        }

        sceneColorFbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);

        configureColorTexture(sceneColorFbo.getColorBufferTexture());
    }

    private void configureColorTexture(Texture texture) {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public void beginScenePass() {
        sceneColorFbo.begin();
    }

    public void endScenePass() {
        sceneColorFbo.end();
    }

    public FrameBuffer sceneColorFbo() {
        return sceneColorFbo;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void dispose() {
        if (sceneColorFbo != null) {
            sceneColorFbo.dispose();
            sceneColorFbo = null;
        }
    }
}
