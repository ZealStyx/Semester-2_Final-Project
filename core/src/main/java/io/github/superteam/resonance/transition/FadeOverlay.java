package io.github.superteam.resonance.transition;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Gdx;

public final class FadeOverlay {
    private final ShapeRenderer shapes = new ShapeRenderer();

    public void render(float alpha) {
        if (alpha <= 0f) return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0f, 0f, 0f, Math.min(1f, alpha)));
        shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.end();
    }
}
