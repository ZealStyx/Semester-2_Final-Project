package io.github.superteam.resonance.model;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

/**
 * Scene-level model wrapper connecting transform, render, and named animation.
 */
public final class SceneModel {

    private final ModelData modelData;
    private final ModelInstance modelInstance;
    private final NamedAnimationController animationController;

    public SceneModel(ModelData modelData) {
        if (modelData == null) {
            throw new IllegalArgumentException("modelData must not be null");
        }

        this.modelData = modelData;
        this.modelInstance = new ModelInstance(modelData.model());
        this.animationController = new NamedAnimationController(modelData, modelInstance);
    }

    public void setPosition(float x, float y, float z) {
        modelInstance.transform.setToTranslation(x, y, z);
    }

    public void setScale(float x, float y, float z) {
        modelInstance.transform.scale(x, y, z);
    }

    public void lookAt(Vector3 worldTarget) {
        modelInstance.transform.setToLookAt(modelInstance.transform.getTranslation(new Vector3()), worldTarget, Vector3.Y);
    }

    public void update(float deltaSeconds) {
        animationController.update(deltaSeconds);
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }

    public ModelData modelData() {
        return modelData;
    }

    public ModelInstance modelInstance() {
        return modelInstance;
    }

    public NamedAnimationController animationController() {
        return animationController;
    }
}
