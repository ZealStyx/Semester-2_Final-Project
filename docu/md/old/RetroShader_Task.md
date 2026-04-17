# AI Model Task: Recreate Retro Shaders

## Task Overview
Create a simple retro shader workflow that can be tested in the project.

## Tasks
1. Create a 1x1x1 block model as a shader test object.
   - The block should be a basic cube mesh with dimensions `1 x 1 x 1`.
   - Use this object to verify that the shader renders correctly on a simple geometry.

2. Create the vertex shader file.
   - Save as `retro_shader.vert` or an appropriate vertex shader filename.
   - Implement the vertex transformation and any necessary UV coordinates or vertex color handling.

3. Create the fragment shader file.
   - Save as `retro_shader.frag` or an appropriate fragment shader filename.
   - Implement the retro visual effect, such as pixelation, color banding, or scanline styling.

4. Add a fog effect to the shader.
   - Implement distance-based fog in the fragment shader.
   - The fog should blend with the retro color palette and reduce scene visibility gradually.

5. Add a second "blind fog" effect.
   - Create a stronger fog mode that nearly occludes the player view.
   - This blind fog should be usable as a gameplay effect to temporarily impair visibility.

6. Add ambient lighting and shadow support.
   - Implement a retro-style ambient lighting component to give the scene depth.
   - Add a shadowing effect or darkened occlusion areas so the shader behaves like a proper custom material.

## Notes
- Keep the shader simple and focused on a retro aesthetic.
- Ensure the vertex and fragment files are compatible with the rendering pipeline used in the project.
- Use the 1x1x1 cube as the first visual test for the shader.
- Verify both the normal fog and blind fog behave distinctly in the scene.
- Verify ambient lighting and shadowing give visible shape and depth to the block.
