# Model Animation System

## Purpose
This document describes the reusable model loading and named animation playback system implemented under `io.github.superteam.resonance.model`.

## Package Layout
- `ModelLoader`: format-agnostic loader contract.
- `GltfModelLoader`: optional `.gltf`/`.glb` loader using reflection against gdx-gltf runtime.
- `G3dModelLoaderAdapter`: fallback loader for `.g3dj` and `.g3db`.
- `ModelImportService`: orchestrates loader selection.
- `ModelAssetManager`: runtime cache, key lookup, ref counting, unload, and reload.
- `ModelData`: runtime model container with named animation lookup and metadata.
- `NamedAnimationController`: stable named animation API (play, stop, pause, resume, cross-fade).
- `SceneModel`: scene-level instance wrapper with transform + animation update + render.

## Unified Debug Harness
- Screen: `io.github.superteam.resonance.devTest.ModelDebugScreen`.
- This single screen consolidates model loading, model swap, animation selection, typed animation playback, cross-fade testing, and status diagnostics.

### Controls
- `M`: load next discovered model candidate.
- `R`: reload current model from source path.
- `N` / `P`: next / previous animation clip.
- `Enter`: play typed animation name (or selected clip if input empty).
- `C`: cross-fade to typed/selected animation.
- `Space`: pause/resume playback.
- `Backspace`: edit typed animation input.
- `ESC`: return to `PlayerTestScreen`.

## Runtime Asset Discovery
`ModelAssetManager` resolves in this order:
1. absolute path (`Gdx.files.absolute(path)`)
2. local exact path
3. local `models/` prefix
4. internal exact path
5. internal `models/` prefix

This supports both the asset pipeline (`assets/models/...`) and runtime-local model folders (`models/...`).

## Launching the Debug Screen
Main screen selection now supports a launch flag:
- Default: `PlayerTestScreen`
- Model debug: set JVM property `resonance.modelDebug=true`

Example command:
- `./gradlew lwjgl3:run --args="-Dresonance.modelDebug=true"`

(Equivalent JVM launch config is also valid.)

## glTF Support Notes
`GltfModelLoader` is implemented as an optional integration layer. It activates automatically when gdx-gltf classes are present on the classpath.

Expected runtime classes:
- `net.mgsx.gltf.loaders.gltf.GLTFLoader`
- `net.mgsx.gltf.loaders.glb.GLBLoader`

If the library is absent, `.gltf/.glb` requests fail with an explicit diagnostic while `.g3dj/.g3db` loading remains available.

## Extension Points
To add more formats:
1. Implement `ModelLoader` for the new extension.
2. Register it in `ModelImportService`.
3. Keep format-specific parsing in loader classes and preserve the `ModelData` runtime contract.

## API Usage Example
```java
ModelAssetManager modelAssetManager = new ModelAssetManager();
ModelData modelData = modelAssetManager.load("enemy", "models/enemy.gltf");

SceneModel sceneModel = new SceneModel(modelData);
sceneModel.setPosition(0f, 0f, 0f);
sceneModel.animationController().play("Idle", true);

// per-frame
sceneModel.update(deltaSeconds);
sceneModel.render(modelBatch, environment);

// reload while keeping key
modelAssetManager.reload("enemy");

// release
modelAssetManager.unload("enemy");
```

## Diagnostics Behavior
- Unknown animation names are no-op with warning logs.
- Missing paths and unsupported formats throw `ModelLoadException` with contextual messages.
- Model reload failures report explicit source path and exception details.
