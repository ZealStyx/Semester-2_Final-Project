package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import io.github.superteam.resonance.screens.GameScreen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.ScreenAdapter;
import io.github.superteam.resonance.multiplayer.MultiplayerLaunchConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UniversalTestScreen extends ScreenAdapter {
    private final UniversalTestScene scene;
    private SpriteBatch overlayBatch;
    private BitmapFont overlayFont;
    private List<String> masterReportLines;

    public UniversalTestScreen() {
        this.scene = new UniversalTestScene();
    }

    public UniversalTestScreen(MultiplayerLaunchConfig config) {
        this.scene = new UniversalTestScene(config);
    }

    @Override
    public void show() {
        scene.show();
        overlayBatch = new SpriteBatch();
        overlayFont = new BitmapFont();
        overlayFont.setColor(Color.WHITE);
        loadMasterReport();
    }

    @Override
    public void render(float delta) {
        scene.render(delta);

        // Draw a compact audit overlay in the top-left for quick reference
        if (overlayBatch != null && masterReportLines != null) {
            overlayBatch.begin();
            float x = 12f;
            float y = Gdx.graphics.getHeight() - 12f;
            int maxLines = Math.min(masterReportLines.size(), 12);
            GlyphLayout layout = new GlyphLayout();
            overlayFont.setColor(Color.YELLOW);
            overlayFont.draw(overlayBatch, "Master Plan Audit (top lines):", x, y);
            overlayFont.setColor(Color.WHITE);
            for (int i = 0; i < maxLines; i++) {
                String line = masterReportLines.get(i);
                float lineY = y - 18f - (i * 14f);
                overlayFont.draw(overlayBatch, trimToWidth(line, 100), x, lineY);
            }
            overlayBatch.end();
        }

        // Quick dev shortcut: press G to open the full GameScreen (gameplay test)
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            Game game = (Game) Gdx.app.getApplicationListener();
            game.setScreen(new GameScreen());
        }
    }

    private String trimToWidth(String s, int maxChars) {
        if (s == null) return "";
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars - 3) + "...";
    }

    private void loadMasterReport() {
        Path p = Path.of("docu", "md", "MASTER_PLAN_REPORT.md");
        if (!Files.exists(p)) {
            masterReportLines = List.of("MASTER_PLAN_REPORT.md not found at docu/md/");
            return;
        }
        try {
            masterReportLines = Files.readAllLines(p);
        } catch (IOException e) {
            masterReportLines = List.of("Error reading MASTER_PLAN_REPORT.md: " + e.getMessage());
        }
    }

    @Override
    public void resize(int width, int height) {
        scene.resize(width, height);
    }

    @Override
    public void pause() {
        scene.pause();
    }

    @Override
    public void resume() {
        scene.resume();
    }

    @Override
    public void hide() {
        scene.hide();
    }

    @Override
    public void dispose() {
        scene.dispose();
        if (overlayBatch != null) overlayBatch.dispose();
        if (overlayFont != null) overlayFont.dispose();
    }

    public UniversalTestScene scene() {
        return scene;
    }
}