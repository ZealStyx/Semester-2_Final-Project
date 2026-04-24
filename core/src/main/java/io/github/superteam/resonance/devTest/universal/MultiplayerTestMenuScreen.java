package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.superteam.resonance.multiplayer.LanDiscoveryService;
import io.github.superteam.resonance.multiplayer.MultiplayerLaunchConfig;

import java.util.List;

/**
 * Simple multiplayer menu screen for selecting HOST, CLIENT, or OFFLINE mode.
 * Supports LAN discovery for client mode and manual IP input.
 */
public class MultiplayerTestMenuScreen extends ScreenAdapter {
    private static final String TITLE = "Resonance - Multiplayer Menu";
    private static final float BUTTON_WIDTH = 200f;
    private static final float BUTTON_HEIGHT = 60f;
    private static final float PADDING = 20f;

    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont smallFont;

    private enum MenuState {
        MODE_SELECT,           // Choose Host/Client/Offline
        CLIENT_CONNECT,        // Enter host IP or select discovered host
        DISCOVERING_HOSTS,     // Waiting for LAN discovery results
        LAUNCHING              // Transitioning to game scene
    }

    private MenuState state = MenuState.MODE_SELECT;
    private List<LanDiscoveryService.DiscoveredHost> discoveredHosts = null;
    private int selectedHostIndex = -1;
    private String manualIpInput = "";
    private boolean inputFocused = false;
    private StringBuilder discoveryStatus = new StringBuilder("Press D to scan for LAN hosts...");

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        );
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.7f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();

        batch.begin();
        renderMenuState();
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (state == MenuState.MODE_SELECT) {
            handleModeSelectInput();
        } else if (state == MenuState.CLIENT_CONNECT) {
            handleClientConnectInput();
        } else if (state == MenuState.DISCOVERING_HOSTS) {
            handleDiscoveringHostsInput();
        }
    }

    private void handleModeSelectInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            launchGame(new MultiplayerLaunchConfig(MultiplayerLaunchConfig.Role.HOST));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            state = MenuState.CLIENT_CONNECT;
            discoveredHosts = null;
            selectedHostIndex = -1;
            manualIpInput = "";
            discoveryStatus = new StringBuilder("Press D to scan for LAN hosts or type IP address");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            launchGame(new MultiplayerLaunchConfig(MultiplayerLaunchConfig.Role.OFFLINE));
        }
    }

    private void handleClientConnectInput() {
        // Back to mode select
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            state = MenuState.MODE_SELECT;
            discoveredHosts = null;
            manualIpInput = "";
            return;
        }

        // Discover LAN hosts
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            state = MenuState.DISCOVERING_HOSTS;
            discoveryStatus = new StringBuilder("Scanning for LAN hosts...");
            // Spawn discovery in background thread
            new Thread(() -> {
                try {
                    List<LanDiscoveryService.DiscoveredHost> hosts = LanDiscoveryService.discoverHosts(2000);
                    MultiplayerTestMenuScreen.this.discoveredHosts = hosts;
                    if (hosts.isEmpty()) {
                        discoveryStatus = new StringBuilder("No hosts found. Enter IP manually or press ENTER to use 127.0.0.1");
                    } else {
                        discoveryStatus = new StringBuilder("Found " + hosts.size() + " host(s). Use arrow keys to select, ENTER to connect.");
                        selectedHostIndex = 0;
                    }
                } finally {
                    state = MenuState.CLIENT_CONNECT;
                }
            }).start();
            return;
        }

        // If hosts were discovered, allow selection
        if (discoveredHosts != null && !discoveredHosts.isEmpty()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selectedHostIndex = Math.max(0, selectedHostIndex - 1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selectedHostIndex = Math.min(discoveredHosts.size() - 1, selectedHostIndex + 1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                LanDiscoveryService.DiscoveredHost host = discoveredHosts.get(selectedHostIndex);
                launchGame(new MultiplayerLaunchConfig(
                        MultiplayerLaunchConfig.Role.CLIENT,
                        host.hostAddress,
                        "ClientPlayer"
                ));
                return;
            }
        }

        // Manual IP input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (manualIpInput.isEmpty()) {
                manualIpInput = "127.0.0.1";
            }
            launchGame(new MultiplayerLaunchConfig(
                    MultiplayerLaunchConfig.Role.CLIENT,
                    manualIpInput,
                    "ClientPlayer"
            ));
            return;
        }

        // Backspace to delete last character
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (manualIpInput.length() > 0) {
                manualIpInput = manualIpInput.substring(0, manualIpInput.length() - 1);
            }
            return;
        }

        // Numeric input: check for number keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "0";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "1";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "2";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "3";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "4";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "5";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "6";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "7";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "8";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += "9";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            if (manualIpInput.length() < 15) {
                manualIpInput += ".";
            }
        }
    }

    private void handleDiscoveringHostsInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = MenuState.CLIENT_CONNECT;
        }
    }

    private void renderMenuState() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Title
        drawCenteredText(TITLE, centerY - 100, font);

        if (state == MenuState.MODE_SELECT) {
            renderModeSelect(centerX, centerY);
        } else if (state == MenuState.CLIENT_CONNECT) {
            renderClientConnect(centerX, centerY);
        } else if (state == MenuState.DISCOVERING_HOSTS) {
            drawCenteredText("Scanning for LAN hosts...", centerY, font);
            drawCenteredText(discoveryStatus.toString(), centerY - 40, smallFont);
        }
    }

    private void renderModeSelect(float centerX, float centerY) {
        drawCenteredText("Choose Mode:", centerY + 60, font);

        drawButton(centerX, centerY + 20, "H - HOST", isMouseOver(centerX, centerY + 20));
        drawButton(centerX, centerY - 40, "C - CLIENT", isMouseOver(centerX, centerY - 40));
        drawButton(centerX, centerY - 100, "O - OFFLINE", isMouseOver(centerX, centerY - 100));

        drawCenteredText("[ESC] Exit", 40, smallFont);
    }

    private void renderClientConnect(float centerX, float centerY) {
        drawCenteredText("Client Mode", centerY + 100, font);

        if (state == MenuState.CLIENT_CONNECT) {
            // Show discovered hosts or IP input
            if (discoveredHosts != null && !discoveredHosts.isEmpty()) {
                drawCenteredText("Available Hosts:", centerY + 50, font);
                float yPos = centerY + 20;
                for (int i = 0; i < discoveredHosts.size(); i++) {
                    String marker = (i == selectedHostIndex) ? "> " : "  ";
                    drawCenteredText(marker + discoveredHosts.get(i).toString(), yPos, smallFont);
                    yPos -= 25;
                }
                drawCenteredText("[UP/DOWN] Select  [ENTER] Connect  [D] Rescan  [B] Back", yPos - 20, smallFont);
            } else {
                drawCenteredText("Host IP Address:", centerY + 50, font);
                drawCenteredText(manualIpInput.isEmpty() ? "127.0.0.1 (default)" : manualIpInput, centerY + 10, font);
                drawCenteredText(discoveryStatus.toString(), centerY - 30, smallFont);
                drawCenteredText("[D] Scan LAN  [ENTER] Connect  [B] Back", centerY - 60, smallFont);
            }
        }
    }

    private void drawButton(float x, float y, String text, boolean highlighted) {
        if (highlighted) {
            font.setColor(1, 1, 0, 1);
        } else {
            font.setColor(1, 1, 1, 1);
        }
        font.draw(batch, text, x - BUTTON_WIDTH / 2, y + BUTTON_HEIGHT / 2);
        font.setColor(1, 1, 1, 1);
    }

    private void drawCenteredText(String text, float y, BitmapFont font) {
        float width = Gdx.graphics.getWidth();
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, text, width / 2 - layout.width / 2, y);
    }

    private boolean isMouseOver(float x, float y) {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return Math.abs(mouseX - x) < BUTTON_WIDTH / 2 && Math.abs(mouseY - y) < BUTTON_HEIGHT / 2;
    }

    private void launchGame(MultiplayerLaunchConfig config) {
        state = MenuState.LAUNCHING;
        System.out.println("Launching with config: " + config);

        // Create scene with launch config
        UniversalTestScene scene = new UniversalTestScene(config);

        // Transition to scene
        ((Game) Gdx.app.getApplicationListener()).setScreen(scene);
    }

    @Override
    public void resize(int width, int height) {
        // Handle resize if needed
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        smallFont.dispose();
    }
}
