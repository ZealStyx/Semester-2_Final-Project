package io.github.superteam.resonance.model;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Runtime model data container with named animation index.
 */
public final class ModelData implements Disposable {

    private final String assetKey;
    private final String sourcePath;
    private final String sourceFormat;
    private final Model model;
    private final Disposable ownerDisposable;
    private final Array<ModelAnimationInfo> animationInfos = new Array<>();
    private final ObjectMap<String, Animation> animationByName = new ObjectMap<>();

    public ModelData(
        String assetKey,
        String sourcePath,
        String sourceFormat,
        Model model,
        Disposable ownerDisposable
    ) {
        if (assetKey == null || assetKey.isBlank()) {
            throw new IllegalArgumentException("assetKey must not be blank");
        }
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("sourcePath must not be blank");
        }
        if (sourceFormat == null || sourceFormat.isBlank()) {
            throw new IllegalArgumentException("sourceFormat must not be blank");
        }
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }

        this.assetKey = assetKey;
        this.sourcePath = sourcePath;
        this.sourceFormat = sourceFormat;
        this.model = model;
        this.ownerDisposable = ownerDisposable;

        indexAnimations(model);
    }

    public String assetKey() {
        return assetKey;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String sourceFormat() {
        return sourceFormat;
    }

    public Model model() {
        return model;
    }

    public Array<ModelAnimationInfo> animationInfos() {
        return animationInfos;
    }

    public Array<String> getAnimationNames() {
        Array<String> names = new Array<>();
        for (ModelAnimationInfo info : animationInfos) {
            names.add(info.name());
        }
        return names;
    }

    public boolean hasAnimation(String animationName) {
        return animationByName.containsKey(animationName);
    }

    public ModelAnimationInfo getAnimationInfo(String animationName) {
        if (animationName == null) {
            return null;
        }
        for (ModelAnimationInfo info : animationInfos) {
            if (animationName.equals(info.name())) {
                return info;
            }
        }
        return null;
    }

    public Animation getAnimation(String animationName) {
        return animationByName.get(animationName);
    }

    @Override
    public void dispose() {
        if (ownerDisposable != null) {
            ownerDisposable.dispose();
            return;
        }
        model.dispose();
    }

    private void indexAnimations(Model loadedModel) {
        for (Animation animation : loadedModel.animations) {
            if (animation == null || animation.id == null || animation.id.isBlank()) {
                continue;
            }
            animationByName.put(animation.id, animation);
            animationInfos.add(new ModelAnimationInfo(animation.id, animation.duration));
        }
    }
}
