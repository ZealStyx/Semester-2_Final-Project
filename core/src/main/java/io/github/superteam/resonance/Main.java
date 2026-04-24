package io.github.superteam.resonance;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

import io.github.superteam.resonance.devTest.ModelDebugScreen;
import io.github.superteam.resonance.devTest.PlayerTestScreen;
import io.github.superteam.resonance.devTest.universal.UniversalTestScene;
import io.github.superteam.resonance.devTest.universal.MultiplayerTestMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        if (Boolean.getBoolean("resonance.modelDebug")) {
            setScreen(new ModelDebugScreen());
            return;
        }

        if (Boolean.getBoolean("resonance.universalTest")) {
            setScreen(new UniversalTestScene());
            return;
        }

        // Default to multiplayer menu
        setScreen(new MultiplayerTestMenuScreen());
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.hide();
            current.dispose();
            setScreen(null);
        }
    }
}
