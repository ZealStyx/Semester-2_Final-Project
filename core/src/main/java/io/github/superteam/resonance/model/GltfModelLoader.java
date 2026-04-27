package io.github.superteam.resonance.model;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

/**
 * Direct glTF loader using gdx-gltf.
 */
public final class GltfModelLoader implements ModelLoader {

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

        try {
            SceneAsset sceneAsset = "glb".equals(extension)
                ? new GLBLoader().load(fileHandle, true)
                : new GLTFLoader().load(fileHandle, true);

            Model model = sceneAsset.scene.model;
            return new ModelData(assetKey, fileHandle.path(), extension, model, sceneAsset);
        } catch (Exception exception) {
            throw new ModelLoadException("Failed to load glTF model: " + fileHandle.path(), exception);
        }
    }
}
