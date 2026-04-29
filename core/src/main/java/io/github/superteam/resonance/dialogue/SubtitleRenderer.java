package io.github.superteam.resonance.dialogue;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;

/** Very small subtitle renderer used by DialogueSystem while scaffolding. */
public final class SubtitleRenderer {
    private final BitmapFont font;

    public SubtitleRenderer() {
        font = new BitmapFont();
        font.getData().setScale(1f);
    }

    public void render(SpriteBatch batch, String text) {
        if (text == null || text.isBlank()) return;

        boolean wasDrawing = batch.isDrawing();
        if (!wasDrawing) batch.begin();

        // Drop shadow for legibility
        font.setColor(0f, 0f, 0f, 0.65f);
        font.draw(batch, text, 21f, Gdx.graphics.getHeight() - 41f);

        font.setColor(1f, 1f, 1f, 1f);
        font.draw(batch, text, 20f, Gdx.graphics.getHeight() - 40f);

        if (!wasDrawing) batch.end();
        font.setColor(1f, 1f, 1f, 1f);
    }
}
