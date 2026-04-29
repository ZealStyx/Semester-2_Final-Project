package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import io.github.superteam.resonance.screens.GameScreen;
import com.badlogic.gdx.ScreenAdapter;
import io.github.superteam.resonance.multiplayer.MultiplayerLaunchConfig;

public class UniversalTestScreen extends ScreenAdapter {
    private final UniversalTestScene scene;

    public UniversalTestScreen() {
        this.scene = new UniversalTestScene();
    }

    public UniversalTestScreen(MultiplayerLaunchConfig config) {
        this.scene = new UniversalTestScene(config);
    }

    @Override
    public void show() {
        scene.show();
    }

    @Override
    public void render(float delta) {
        scene.render(delta);

        // Quick dev shortcut: press G to open the full GameScreen (gameplay test)
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            Game game = (Game) Gdx.app.getApplicationListener();
            game.setScreen(new GameScreen());
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
    }

    public UniversalTestScene scene() {
        return scene;
    }
}