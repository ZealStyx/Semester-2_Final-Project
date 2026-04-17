package io.github.superteam.resonance.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

/**
 * Named animation playback wrapper with pause/resume and cross-fade behavior.
 */
public final class NamedAnimationController {

    private final ModelData modelData;
    private final AnimationController animationController;

    private String currentAnimationName;
    private boolean playing;
    private boolean paused;

    public NamedAnimationController(ModelData modelData, ModelInstance modelInstance) {
        if (modelData == null) {
            throw new IllegalArgumentException("modelData must not be null");
        }
        if (modelInstance == null) {
            throw new IllegalArgumentException("modelInstance must not be null");
        }

        this.modelData = modelData;
        this.animationController = new AnimationController(modelInstance);
    }

    public void update(float deltaSeconds) {
        if (!playing || paused) {
            return;
        }
        animationController.update(Math.max(0f, deltaSeconds));
    }

    public void play(String animationName) {
        play(animationName, true);
    }

    public void play(String animationName, boolean loop) {
        if (!modelData.hasAnimation(animationName)) {
            Gdx.app.log("ModelAnimation", "Unknown animation name: " + animationName);
            return;
        }

        int loopCount = loop ? -1 : 1;
        animationController.setAnimation(animationName, loopCount, null);
        currentAnimationName = animationName;
        playing = true;
        paused = false;
    }

    public void crossFadeTo(String animationName, float durationSeconds, boolean loop) {
        if (!modelData.hasAnimation(animationName)) {
            Gdx.app.log("ModelAnimation", "Unknown animation name for cross-fade: " + animationName);
            return;
        }

        int loopCount = loop ? -1 : 1;
        animationController.animate(animationName, loopCount, Math.max(0f, durationSeconds), null, 0f);
        currentAnimationName = animationName;
        playing = true;
        paused = false;
    }

    public void stop() {
        playing = false;
        paused = false;
        currentAnimationName = null;
        animationController.current = null;
        animationController.previous = null;
    }

    public void pause() {
        if (playing) {
            paused = true;
        }
    }

    public void resume() {
        if (playing) {
            paused = false;
        }
    }

    public String getCurrentAnimation() {
        return currentAnimationName;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }
}
