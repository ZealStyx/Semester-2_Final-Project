package io.github.superteam.resonance.devTest.universal.diagnostics;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;

public final class TabCycler {
    private static final String[] TABS = {"PERFORMANCE", "SYSTEM_STATE", "MIC_STAMINA", "ZONE", "NETWORK", "CONTROLS"};
    private int activeIndex;

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
            activeIndex = (activeIndex + 1) % TABS.length;
        }
    }

    public String activeTab() {
        return TABS[activeIndex];
    }
}
