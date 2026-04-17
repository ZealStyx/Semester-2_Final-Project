# Model Animation Loader Task

## Task Overview
Build a reusable, extensible 3D model loading and animation playback system for the project. The system should support glTF (`.gltf`, `.glb`) as the primary format, deliver explicit named animation control, and be designed so additional libGDX-supported model formats can be added later.

The implementation should separate import, runtime model management, scene-level model data, and animation control so it can be reused across gameplay, debug tools, and future content.

## Objectives
- Load 3D models and animation metadata from `assets/` and runtime-local folders.
- Provide a stable API for named animation playback, cross-fades, looping, and invalid-key handling.
- Store model data, animation clips, and runtime state in a reusable model asset system.
- Build a debug/test screen for live validation of model import and animation playback.
- Document supported file types, loader extension points, and runtime asset behavior.

## Unified Debug Screen Task
- Create a single debug/test screen that consolidates model loading, animation selection, playback controls, validation, and runtime asset management.
- Avoid splitting debugging tools across different screens, test files, or utilities.
- Use this unified screen as the main example harness for the 3D model and animation system.

## Architecture Overview
### 1. Model import layer
- `ModelLoader` interface: format-agnostic contract for loading model files.
- `GltfModelLoader`: concrete loader for `.gltf` and `.glb` using libGDX glTF utilities.
- Support for file handles from both `assets/` and a local `models/` runtime folder.
- Extract mesh geometry, materials, skeletons, textures, and animation clip metadata.
- Keep path resolution and file discovery separate from model parsing.

### 2. Asset management layer
- `ModelAssetManager`: central registry and cache for loaded models.
- Reference counting or strong/weak handles to avoid duplicate loads.
- Safe unload and reload support for runtime swapping.
- Lookup by asset key, file path, or logical model name.
- Error reporting for missing files, invalid formats, and model load failures.

### 3. Runtime model representation
- `ModelData`: encapsulates loaded mesh, material, skeleton, and animation entries.
- Named animation index: map animation names to clip metadata and runtime handles.
- Metadata fields for source path, format, and available animation names.
- Support for model variants and model instances with shared underlying data.

### 4. Animation control layer
- `AnimationController`: play, stop, loop, blend, and query state by animation name.
- Smooth cross-fade support when switching between animations.
- Animation playback state: current animation, elapsed time, loop mode, and blend progress.
- Gracefully handle unknown animation names with clear diagnostics.
- Expose query APIs for animation list, active animation, and playback status.

### 5. Scene integration layer
- `SceneModel` / `ModelInstanceWrapper`: runtime object linking transform, render state, and animation controller.
- Update step that advances animation state and applies bone transforms.
- Support for skinned meshes and non-skinned model playback.
- Separation between model asset data and scene-specific instance state.

### 6. Validation / debug layer
- `ModelDebugScreen`: example scene to load a sample model and inspect animations.
- UI controls to select or type animation names and trigger playback immediately.
- Display loaded model metadata, available animation names, current state, and errors.
- Runtime model swap and reload controls for validation.

## Detailed Implementation Plan
### Phase 1: Design & package layout
1. Create a dedicated package path, e.g. `io.github.superteam.resonance.model`.
2. Define interfaces and core data structures before coding.
3. Create a design document or UML-style sketch for loader, asset manager, and controller interactions.
4. Add README notes or design comments in `docu/md/` to capture expected interfaces.

### Phase 2: glTF loader implementation
1. Implement `GltfModelLoader` as the primary format loader.
2. Use libGDX glTF utilities for mesh, material, texture, skeleton, and animation extraction.
3. Build a `ModelData` instance with a lookup table for named animations.
4. Validate that animation names are preserved exactly as defined in the source file.
5. Add support for both `.gltf` and `.glb`, and prefer runtime file handles.

### Phase 3: Named animation mapping
1. Parse animation names from glTF asset metadata.
2. Store animation clips by name with explicit indexing.
3. Provide query methods: `getAnimationNames()`, `hasAnimation(name)`, `getAnimationInfo(name)`.
4. Normalize names only when necessary; preserve original case and spaces for debugging.

### Phase 4: Animation playback API
1. Implement `AnimationController` with methods:
   - `play(name)`, `stop()`, `pause()`, `resume()`
   - `play(name, loop)`
   - `crossFadeTo(name, duration)`
   - `getCurrentAnimation()`, `isPlaying()`
2. Add internal state tracking for active clip, blend source, and elapsed time.
3. Make invalid-name requests no-op and log warnings.
4. Support both skinned skeleton animation and simple transform-only animation.

### Phase 5: Runtime asset management
1. Build `ModelAssetManager` to load/unload model assets.
2. Add support for local `models/` discovery in addition to `assets/models/`.
3. Add a fallback system: asset search in `assets/` then runtime file path.
4. Implement safe reload so a model can be swapped while scene instances remain stable.
5. Add diagnostic error messages for missing assets, unsupported formats, and empty animation lists.

### Phase 6: Debug/test screen
1. Create a debug screen or test scene that loads a sample glTF model.
2. Display available animation names, model path, and load status.
3. Add text input / selection controls for executing animation playback.
4. Show playback state, current animation, and timing progress.
5. Add buttons to reload the model or switch sample models.

### Phase 7: Documentation and handoff
1. Document supported file formats and naming conventions in `docu/md/`.
2. Add usage examples for loading models and playing named animations.
3. Document how to extend the loader for additional formats.
4. Add notes on asset structure, runtime folder rules, and async load expectations.
5. Optionally include a sample glTF model in `assets/models/` for manual validation.

## Acceptance Criteria
- The system loads glTF models from the project asset pipeline and runtime folder.
- Animation clips are exposed through an explicit named lookup.
- A controller API supports play, stop, loop, and smooth cross-fade transitions.
- Unknown animation names are handled safely with clear diagnostics.
- Runtime model asset management supports loading, caching, and reloads.
- A debug/test screen demonstrates model loading and live animation control.
- Documentation exists describing supported formats, extension points, and usage.

## Optional Enhancements
- Add model validation checks for missing meshes, invalid skeletons, and texture errors.
- Validate animation clips for zero duration, duplicate names, and unsupported interpolation types.
- Add runtime debug tooling: model browser, animation scrub slider, playback speed controls, and skeleton visualization toggles.
- Build lazy instancing and placeholder fallback support for async or heavy model loads.
- Support hot-reload of changed model files and save/load of last-used debug session state.
- Add helper API methods such as `playDefaultAnimation()`, `getFirstAnimationName()`, and batch animation commands.
- Plan for plugin-style loader registration and custom animation event callbacks.

## Notes
- Favor modular, testable code and single-responsibility classes.
- Keep model load operations asynchronous or non-blocking where practical.
- Preserve original animation names from source files for clarity.
- Prefer explicit names over numeric indices in runtime APIs.
- Verify skinned mesh bone transforms and animation playback behavior.
- Document loader requirements and project conventions in `docu/md/` for future team use.
