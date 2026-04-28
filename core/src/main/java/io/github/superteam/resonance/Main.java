package io.github.superteam.resonance;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

import io.github.superteam.resonance.devTest.FilePicker;
import io.github.superteam.resonance.devTest.ModelDebugScreen;
import io.github.superteam.resonance.devTest.universal.UniversalTestScreen;
import io.github.superteam.resonance.screens.GameScreen;
import io.github.superteam.resonance.devTest.universal.MultiplayerTestMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private final FilePicker filePicker;

    public Main() {
        this(FilePicker.NOOP);
    }

    public Main(FilePicker filePicker) {
        this.filePicker = filePicker == null ? FilePicker.NOOP : filePicker;
    }

    @Override
    public void create() {
        if (Boolean.getBoolean("resonance.modelDebug")) {
            setScreen(new ModelDebugScreen(filePicker));
            return;
        }

        if (Boolean.getBoolean("resonance.universalTest")) {
            setScreen(new UniversalTestScreen());
            return;
        }

        // Default to model debug screen
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
