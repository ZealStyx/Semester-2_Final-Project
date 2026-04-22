package io.github.superteam.resonance.devTest.universal.diagnostics;

import com.badlogic.gdx.Gdx;

public final class PerformanceCounter {
    public String summary() {
        return "FPS=" + Gdx.graphics.getFramesPerSecond();
    }
}
