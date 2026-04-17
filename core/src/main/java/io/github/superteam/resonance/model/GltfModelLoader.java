package io.github.superteam.resonance.model;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional glTF loader using reflection against gdx-gltf runtime classes.
 *
 * This avoids hard dependency coupling while still enabling .gltf/.glb when
 * the glTF library is present on the classpath.
 */
public final class GltfModelLoader implements ModelLoader {

    private static final String GLTF_LOADER_CLASS = "net.mgsx.gltf.loaders.gltf.GLTFLoader";
    private static final String GLB_LOADER_CLASS = "net.mgsx.gltf.loaders.glb.GLBLoader";

    @Override
    public String loaderName() {
        return "gltf-loader";
    }

    @Override
    public boolean supports(FileHandle fileHandle) {
        if (fileHandle == null) {
            return false;
        }
        String extension = fileHandle.extension().toLowerCase();
        return "gltf".equals(extension) || "glb".equals(extension);
    }

    @Override
    public ModelData load(String assetKey, FileHandle fileHandle) {
        String extension = fileHandle.extension().toLowerCase();
        String loaderClassName = "glb".equals(extension) ? GLB_LOADER_CLASS : GLTF_LOADER_CLASS;

        try {
            Class<?> loaderClass = Class.forName(loaderClassName);
            Object loader = loaderClass.getDeclaredConstructor().newInstance();
            Method loadMethod = loaderClass.getMethod("load", FileHandle.class);
            Object loadResult = loadMethod.invoke(loader, fileHandle);

            Model model = extractModel(loadResult);
            Disposable ownerDisposable = (loadResult instanceof Disposable && !(loadResult instanceof Model))
                ? (Disposable) loadResult
                : null;

            return new ModelData(assetKey, fileHandle.path(), extension, model, ownerDisposable);
        } catch (ClassNotFoundException notFound) {
            throw new ModelLoadException(
                "glTF loader library not found. Add gdx-gltf dependency before loading: " + fileHandle.path(),
                notFound
            );
        } catch (Exception exception) {
            throw new ModelLoadException("Failed to load glTF model: " + fileHandle.path(), exception);
        }
    }

    private Model extractModel(Object loadResult) {
        if (loadResult == null) {
            throw new ModelLoadException("glTF loader returned null result.");
        }

        if (loadResult instanceof Model) {
            return (Model) loadResult;
        }

        Model directModel = readModelField(loadResult);
        if (directModel != null) {
            return directModel;
        }

        Object scene = readObjectField(loadResult, "scene");
        if (scene != null) {
            Object modelInstanceObject = readObjectField(scene, "modelInstance");
            if (modelInstanceObject instanceof ModelInstance) {
                return ((ModelInstance) modelInstanceObject).model;
            }

            Model sceneModel = readModelField(scene);
            if (sceneModel != null) {
                return sceneModel;
            }
        }

        Object sceneModel = readObjectField(loadResult, "sceneModel");
        if (sceneModel != null) {
            Model sceneModelValue = readModelField(sceneModel);
            if (sceneModelValue != null) {
                return sceneModelValue;
            }
        }

        throw new ModelLoadException("Unable to extract Model from glTF loader result type: " + loadResult.getClass().getName());
    }

    private Model readModelField(Object source) {
        Object fieldValue = readObjectField(source, "model");
        return fieldValue instanceof Model ? (Model) fieldValue : null;
    }

    private Object readObjectField(Object source, String fieldName) {
        try {
            Field field = source.getClass().getField(fieldName);
            return field.get(source);
        } catch (Exception ignored) {
            return null;
        }
    }
}
