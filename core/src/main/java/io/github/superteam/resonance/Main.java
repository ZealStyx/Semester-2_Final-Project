package io.github.superteam.resonance;

import com.badlogic.gdx.Game;

import io.github.superteam.resonance.devTest.PlayerTestScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        setScreen(new PlayerTestScreen());
    }
}
