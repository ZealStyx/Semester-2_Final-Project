package io.github.superteam.resonance.behavior.debug;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.superteam.resonance.behavior.BehaviorSystem;
import io.github.superteam.resonance.behavior.PlayerArchetype;

/** Small HUD panel that exposes the current behavior classification. */
public final class BehaviorDebugOverlay {
    public void render(SpriteBatch batch, BitmapFont font, BehaviorSystem behaviorSystem, float x, float y) {
        if (batch == null || font == null || behaviorSystem == null) {
            return;
        }

        float lineHeight = 14f;
        batch.begin();
        font.setColor(0.92f, 0.95f, 1f, 0.80f);
        font.draw(batch, "[Behavior] " + behaviorSystem.currentArchetype(), x, y);
        font.draw(batch, "[Behavior] Samples=" + behaviorSystem.sampleCount() + "  Inertia=" + String.format("%.3f", behaviorSystem.inertia()), x, y - lineHeight);
        float[] scores = behaviorSystem.scores();
        PlayerArchetype[] archetypes = PlayerArchetype.values();
        for (int i = 0; i < archetypes.length && i < scores.length; i++) {
            font.draw(batch, archetypes[i].displayName() + "  " + String.format("%.2f", scores[i]), x, y - ((i + 2) * lineHeight));
        }
        batch.end();
    }
}
