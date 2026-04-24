package io.github.superteam.resonance.devTest.universal.diagnostics;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.superteam.resonance.devTest.universal.TestZone;
import io.github.superteam.resonance.player.MovementState;
import io.github.superteam.resonance.multiplayer.MultiplayerManager;
import io.github.superteam.resonance.multiplayer.RemotePlayer;

public final class DiagnosticOverlay {
    private final TabCycler tabCycler = new TabCycler();
    private final PerformanceCounter performanceCounter = new PerformanceCounter();
    private final SystemStatePanel systemStatePanel = new SystemStatePanel();

    public void update() {
        tabCycler.update();
    }

    public String activeTab() {
        return tabCycler.activeTab();
    }

    public void draw(
        SpriteBatch batch,
        BitmapFont font,
        ShapeRenderer shapeRenderer,
        Matrix4 hudProjection,
        TestZone activeZone,
        int width,
        int height,
        float[] micHistory,
        int micHistoryCursor,
        float micLevelNormalized,
        float staminaNormalized,
        MovementState movementState,
        boolean micActive,
        boolean recentVoiceOutput,
        MultiplayerManager multiplayerManager
    ) {
        String tab = tabCycler.activeTab();
        float x = 16.0f;
        float y = height - 16.0f;
        float line = 16.0f;

        batch.begin();
        font.draw(batch, "[UniversalTestScene] TAB=" + tab, x, y);

        if ("PERFORMANCE".equals(tab)) {
            font.draw(batch, performanceCounter.summary(), x, y - line);
        } else if ("SYSTEM_STATE".equals(tab)) {
            Array<String> lines = systemStatePanel.lines(activeZone == null ? null : activeZone.getSystemState());
            for (int i = 0; i < lines.size; i++) {
                font.draw(batch, lines.get(i), x, y - ((i + 1) * line));
            }
        } else if ("MIC_STAMINA".equals(tab)) {
            font.draw(batch, "Mic=" + (micActive ? "ON" : "OFF") + " level=" + String.format("%.2f", micLevelNormalized), x, y - line);
            font.draw(batch, "Stamina=" + String.format("%.2f", staminaNormalized) + " state=" + movementState, x, y - (line * 2.0f));
        } else if ("ZONE".equals(tab)) {
            font.draw(batch, "ActiveZone=" + (activeZone == null ? "none" : activeZone.getZoneName()), x, y - line);
        } else if ("NETWORK".equals(tab)) {
            font.draw(batch, "Role: " + multiplayerManager.getRole(), x, y - line);
            font.draw(batch, "Players: " + multiplayerManager.getKnownPlayerCount(), x, y - (line * 2.0f));
            String micStatus = micActive ? "ACTIVE" : "OFF";
            font.draw(batch, "Mic: " + micStatus, x, y - (line * 3.0f));
            font.draw(batch, "Voice output: " + (recentVoiceOutput ? "RECENT" : "IDLE"), x, y - (line * 4.0f));
            int i = 0;
            for (RemotePlayer rp : multiplayerManager.remotePlayers.values()) {
                if (rp.id == multiplayerManager.getLocalPlayerId()) {
                    continue;
                }
                font.draw(batch, "- " + rp.name + (rp.isSpeaking ? " [TALKING]" : ""), x + 8, y - (line * (5.0f + i)));
                i++;
            }
        } else {
            font.draw(batch, "WASD move | Shift run | Ctrl slow | C crouch | Space jump", x, y - line);
            font.draw(batch, "F10 from PlayerTestScreen enters this shell", x, y - (line * 2.0f));
            font.draw(batch, "F9 back to PlayerTestScreen", x, y - (line * 3.0f));
        }

        batch.end();

        if ("MIC_STAMINA".equals(tab)) {
            drawMicAndStaminaGraphs(shapeRenderer, hudProjection, width, height, micHistory, micHistoryCursor, micLevelNormalized, staminaNormalized);
        }
    }

    private void drawMicAndStaminaGraphs(
        ShapeRenderer shapeRenderer,
        Matrix4 hudProjection,
        int width,
        int height,
        float[] micHistory,
        int micHistoryCursor,
        float micLevelNormalized,
        float staminaNormalized
    ) {
        if (micHistory == null || micHistory.length < 2) {
            return;
        }

        float panelX = 16.0f;
        float panelY = height - 118.0f;
        float graphW = Math.min(320.0f, width * 0.45f);
        float graphH = 68.0f;

        shapeRenderer.setProjectionMatrix(hudProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.35f, 0.92f, 0.78f, 0.9f);
        shapeRenderer.rect(panelX, panelY, graphW, graphH);

        int sampleCount = micHistory.length;
        float stepX = graphW / (sampleCount - 1f);
        for (int i = 1; i < sampleCount; i++) {
            int prevIndex = (micHistoryCursor + i - 1) % sampleCount;
            int currIndex = (micHistoryCursor + i) % sampleCount;
            float prevValue = MathUtils.clamp(micHistory[prevIndex], 0f, 1f);
            float currValue = MathUtils.clamp(micHistory[currIndex], 0f, 1f);

            float x0 = panelX + ((i - 1) * stepX);
            float y0 = panelY + (prevValue * graphH);
            float x1 = panelX + (i * stepX);
            float y1 = panelY + (currValue * graphH);
            shapeRenderer.line(x0, y0, x1, y1);
        }
        shapeRenderer.end();

        float barY = panelY - 18.0f;
        float barW = graphW;
        float barH = 10.0f;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.13f, 0.20f, 0.22f, 0.75f);
        shapeRenderer.rect(panelX, barY, barW, barH);
        shapeRenderer.setColor(0.90f, 0.36f, 0.25f, 0.95f);
        shapeRenderer.rect(panelX, barY, barW * MathUtils.clamp(staminaNormalized, 0f, 1f), barH);

        float micMarkerX = panelX + (barW * MathUtils.clamp(micLevelNormalized, 0f, 1f));
        shapeRenderer.setColor(0.30f, 0.94f, 0.84f, 0.95f);
        shapeRenderer.rect(micMarkerX - 1.0f, panelY, 2.0f, graphH);
        shapeRenderer.end();
    }
}
