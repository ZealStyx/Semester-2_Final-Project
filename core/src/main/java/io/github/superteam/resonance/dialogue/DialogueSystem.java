package io.github.superteam.resonance.dialogue;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayDeque;
import java.util.Queue;

/** Minimal DialogueSystem stub for master-plan scaffolding. */
public final class DialogueSystem {
    private final Queue<DialogueLine> queue = new ArrayDeque<>();
    private final SubtitleRenderer renderer = new SubtitleRenderer();

    public void showSubtitle(String text, float durationSeconds) {
        if (text == null || text.isBlank()) return;
        queue.add(new DialogueLine(text, Math.max(0.1f, durationSeconds)));
    }

    public void update(float delta) {
        if (!queue.isEmpty()) {
            DialogueLine current = queue.peek();
            current.timeLeft -= delta;
            if (current.timeLeft <= 0f) queue.poll();
        }
    }

    public void render(SpriteBatch batch) {
        DialogueLine current = queue.peek();
        if (current != null) renderer.render(batch, current.text);
    }

    public void clear() { queue.clear(); }

    private static final class DialogueLine {
        private final String text;
        private float timeLeft;

        private DialogueLine(String text, float timeLeft) {
            this.text = text;
            this.timeLeft = timeLeft;
        }
    }
}
