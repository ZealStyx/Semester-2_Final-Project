package io.github.superteam.resonance.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public enum ParticleBlendMode {
    ADDITIVE,
    ALPHA,
    SCREEN,
    MULTIPLY;

    public static ParticleBlendMode fromName(String value) {
        if (value == null) {
            return ADDITIVE;
        }
        for (ParticleBlendMode blendMode : values()) {
            if (blendMode.name().equalsIgnoreCase(value)) {
                return blendMode;
            }
        }
        return ADDITIVE;
    }

    public void apply() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        switch (this) {
            case ALPHA:
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                break;
            case SCREEN:
                Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
                break;
            case MULTIPLY:
                Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO);
                break;
            case ADDITIVE:
            default:
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                break;
        }
    }
}
