package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds test maps from JSON definitions.
 * Generates walls, floors, ceilings, colliders, and doors.
 */
public class TestMapBuilder {

    private static final float DEFAULT_WALL_THICKNESS = 0.3f;
    private static final float DEFAULT_FLOOR_THICKNESS = 0.1f;
    private static final float DEFAULT_DOOR_HEIGHT = 2.1f;
    private static final float DEFAULT_DOOR_WIDTH = 2.0f;
    private static final float WALL_HEIGHT_NORMAL = 2.8f;

    private final Array<BoundingBox> worldColliders = new Array<>();
    private final List<Mesh> meshes = new ArrayList<>();
    private final List<Matrix4> transforms = new ArrayList<>();
    private final List<DoorDefinition> doors = new ArrayList<>();
    private float originX = 40f;
    private float originZ = 40f;

    private static class DoorDefinition {
        Vector3 hingePosition;
        float width;
        float height;
        boolean locked;
        String id;

        DoorDefinition(Vector3 hinge, float width, float height, boolean locked, String id) {
            this.hingePosition = hinge;
            this.width = width;
            this.height = height;
            this.locked = locked;
            this.id = id;
        }
    }

    /**
     * Loads and parses a JSON map file.
     */
    public void loadFromJson(String jsonFilePath) {
        FileHandle file = Gdx.files.internal(jsonFilePath);
        if (!file.exists()) {
            file = Gdx.files.local(jsonFilePath);
        }

        if (!file.exists()) {
            Gdx.app.error("TestMapBuilder", "Map file not found: " + jsonFilePath);
            return;
        }

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(file);

        // Parse origin
        JsonValue origin = root.get("origin");
        if (origin != null) {
            originX = origin.getFloat("x", 40f);
            originZ = origin.getFloat("z", 40f);
        }

        // Parse elements
        JsonValue elements = root.get("elements");
        if (elements != null) {
            for (JsonValue element : elements) {
                parseElement(element);
            }
        }

        Gdx.app.log("TestMapBuilder", "Loaded map with " + worldColliders.size + " colliders and " + doors.size() + " doors");
    }

    private void parseElement(JsonValue element) {
        String type = element.getString("type", "");

        switch (type) {
            case "wall":
                parseWall(element);
                break;
            case "floor":
                parseFloor(element);
                break;
            case "ceiling":
                parseCeiling(element);
                break;
            default:
                Gdx.app.log("TestMapBuilder", "Unknown element type: " + type);
        }
    }

    private void parseWall(JsonValue element) {
        float x1 = element.getFloat("x1", 0f);
        float z1 = element.getFloat("z1", 0f);
        float x2 = element.getFloat("x2", 0f);
        float z2 = element.getFloat("z2", 0f);

        // Convert to world coordinates
        x1 += originX;
        z1 += originZ;
        x2 += originX;
        z2 += originZ;

        float height = element.getFloat("height", WALL_HEIGHT_NORMAL);
        float thickness = element.getFloat("thickness", DEFAULT_WALL_THICKNESS);

        // Check for door
        JsonValue doorJson = element.get("door");
        if (doorJson != null) {
            parseWallWithDoor(x1, z1, x2, z2, height, thickness, doorJson);
        } else {
            createWallSegment(x1, z1, x2, z2, height, thickness);
        }
    }

    private void parseWallWithDoor(float x1, float z1, float x2, float z2, float wallHeight,
                                   float thickness, JsonValue doorJson) {
        float hingePosition = doorJson.getFloat("position", 0f);
        String orientation = doorJson.getString("orientation", "forward");
        float doorWidth = DEFAULT_DOOR_WIDTH;
        float doorHeight = DEFAULT_DOOR_HEIGHT;
        boolean locked = doorJson.getBoolean("locked", false);

        // Calculate wall vector and length
        float dx = x2 - x1;
        float dz = z2 - z1;
        float wallLength = (float) Math.sqrt(dx * dx + dz * dz);

        if (wallLength < doorWidth + 0.5f) {
            // Wall too short for door, create solid wall
            createWallSegment(x1, z1, x2, z2, wallHeight, thickness);
            return;
        }

        // Normalize direction
        float dirX = dx / wallLength;
        float dirZ = dz / wallLength;

        float maxStartDist = Math.max(0f, wallLength - doorWidth);
        float doorStartDist = MathUtils.clamp(hingePosition, 0f, wallLength);
        if ("reverse".equalsIgnoreCase(orientation) || "end".equalsIgnoreCase(orientation)) {
            doorStartDist -= doorWidth;
        }
        doorStartDist = MathUtils.clamp(doorStartDist, 0f, maxStartDist);
        float doorEndDist = doorStartDist + doorWidth;

        float doorStartX = x1 + dirX * doorStartDist;
        float doorStartZ = z1 + dirZ * doorStartDist;
        float doorEndX = x1 + dirX * doorEndDist;
        float doorEndZ = z1 + dirZ * doorEndDist;

        // Left segment (from x1,z1 to door start)
        createWallSegment(x1, z1, doorStartX, doorStartZ, wallHeight, thickness);

        // Right segment (from door end to x2,z2)
        createWallSegment(doorEndX, doorEndZ, x2, z2, wallHeight, thickness);

        // Header above door (if wall is taller than door)
        if (wallHeight > doorHeight) {
            float headerHeight = wallHeight - doorHeight;
            float headerY = doorHeight + headerHeight / 2f;
            createHorizontalSegment(doorStartX, doorStartZ, doorEndX, doorEndZ,
                headerY, headerHeight, thickness);
        }

        // Register door with hinge at the door start point along the wall.
        String doorId = doorJson.getString("id", "door_" + doors.size());
        Vector3 hingePos = new Vector3(doorStartX, doorHeight / 2f, doorStartZ);
        doors.add(new DoorDefinition(hingePos, doorWidth, doorHeight, locked, doorId));
    }

    private void createWallSegment(float x1, float z1, float x2, float z2, float height, float thickness) {
        // Calculate center
        float centerX = (x1 + x2) / 2f;
        float centerZ = (z1 + z2) / 2f;
        float centerY = height / 2f;

        // Calculate dimensions
        float dx = x2 - x1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dz * dz);

        if (length < 0.01f) return; // Too small

        // For axis-aligned walls, use simple box
        if (Math.abs(dx) < 0.01f) {
            // Z-aligned wall
            addCollider(centerX, centerY, centerZ, thickness, height, length);
        } else if (Math.abs(dz) < 0.01f) {
            // X-aligned wall
            addCollider(centerX, centerY, centerZ, length, height, thickness);
        } else {
            // Diagonal wall - approximate with box aligned to wall direction
            // This is simplified; for diagonal walls we'd need rotated boxes
            // For now, use average dimension
            float avgDim = Math.max(length, thickness);
            addCollider(centerX, centerY, centerZ, avgDim, height, thickness);
        }
    }

    private void createHorizontalSegment(float x1, float z1, float x2, float z2,
                                         float y, float height, float thickness) {
        float centerX = (x1 + x2) / 2f;
        float centerZ = (z1 + z2) / 2f;

        float dx = x2 - x1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dz * dz);

        if (Math.abs(dx) < 0.01f) {
            addCollider(centerX, y, centerZ, thickness, height, length);
        } else if (Math.abs(dz) < 0.01f) {
            addCollider(centerX, y, centerZ, length, height, thickness);
        } else {
            addCollider(centerX, y, centerZ, length, height, thickness);
        }
    }

    private void parseFloor(JsonValue element) {
        float x1 = element.getFloat("x1", 0f);
        float z1 = element.getFloat("z1", 0f);
        float x2 = element.getFloat("x2", 0f);
        float z2 = element.getFloat("z2", 0f);
        float thickness = element.getFloat("thickness", DEFAULT_FLOOR_THICKNESS);

        // Convert to world coordinates
        x1 += originX;
        z1 += originZ;
        x2 += originX;
        z2 += originZ;

        float centerX = (x1 + x2) / 2f;
        float centerZ = (z1 + z2) / 2f;
        float width = Math.abs(x2 - x1);
        float depth = Math.abs(z2 - z1);
        float y = -0.1f + thickness / 2f; // Floor sits slightly below 0

        addCollider(centerX, y, centerZ, width, thickness, depth);
    }

    private void parseCeiling(JsonValue element) {
        float x1 = element.getFloat("x1", 0f);
        float z1 = element.getFloat("z1", 0f);
        float x2 = element.getFloat("x2", 0f);
        float z2 = element.getFloat("z2", 0f);
        float height = element.getFloat("height", WALL_HEIGHT_NORMAL);
        float thickness = element.getFloat("thickness", DEFAULT_FLOOR_THICKNESS);

        // Convert to world coordinates
        x1 += originX;
        z1 += originZ;
        x2 += originX;
        z2 += originZ;

        float centerX = (x1 + x2) / 2f;
        float centerZ = (z1 + z2) / 2f;
        float width = Math.abs(x2 - x1);
        float depth = Math.abs(z2 - z1);
        float y = height + thickness / 2f;

        addCollider(centerX, y, centerZ, width, thickness, depth);
    }

    private void addCollider(float centerX, float centerY, float centerZ,
                             float width, float height, float depth) {
        Vector3 min = new Vector3(
            centerX - width / 2f,
            centerY - height / 2f,
            centerZ - depth / 2f
        );
        Vector3 max = new Vector3(
            centerX + width / 2f,
            centerY + height / 2f,
            centerZ + depth / 2f
        );
        worldColliders.add(new BoundingBox(min, max));

        // Create visual mesh
        Mesh mesh = createCubeMesh(width, height, depth);
        meshes.add(mesh);
        transforms.add(new Matrix4().setToTranslation(centerX, centerY, centerZ));
    }

    private Mesh createCubeMesh(float width, float height, float depth) {
        Mesh mesh = new Mesh(
            true,
            24,
            36,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"));

        float w = width * 0.5f;
        float h = height * 0.5f;
        float d = depth * 0.5f;

        float[] vertices = {
            -w, -h, d, 0, 0, 1, 1, 1, 1, 1,
            w, -h, d, 0, 0, 1, 1, 1, 1, 1,
            w, h, d, 0, 0, 1, 1, 1, 1, 1,
            -w, h, d, 0, 0, 1, 1, 1, 1, 1,

            w, -h, -d, 0, 0, -1, 1, 1, 1, 1,
            -w, -h, -d, 0, 0, -1, 1, 1, 1, 1,
            -w, h, -d, 0, 0, -1, 1, 1, 1, 1,
            w, h, -d, 0, 0, -1, 1, 1, 1, 1,

            w, -h, d, 1, 0, 0, 1, 1, 1, 1,
            w, -h, -d, 1, 0, 0, 1, 1, 1, 1,
            w, h, -d, 1, 0, 0, 1, 1, 1, 1,
            w, h, d, 1, 0, 0, 1, 1, 1, 1,

            -w, -h, -d, -1, 0, 0, 1, 1, 1, 1,
            -w, -h, d, -1, 0, 0, 1, 1, 1, 1,
            -w, h, d, -1, 0, 0, 1, 1, 1, 1,
            -w, h, -d, -1, 0, 0, 1, 1, 1, 1,

            -w, h, -d, 0, 1, 0, 1, 1, 1, 1,
            -w, h, d, 0, 1, 0, 1, 1, 1, 1,
            w, h, d, 0, 1, 0, 1, 1, 1, 1,
            w, h, -d, 0, 1, 0, 1, 1, 1, 1,

            -w, -h, d, 0, -1, 0, 1, 1, 1, 1,
            -w, -h, -d, 0, -1, 0, 1, 1, 1, 1,
            w, -h, -d, 0, -1, 0, 1, 1, 1, 1,
            w, -h, d, 0, -1, 0, 1, 1, 1, 1
        };

        short[] indices = {
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        };

        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    /**
     * Returns all generated colliders for acoustic graph population.
     */
    public Array<BoundingBox> getWorldColliders() {
        return worldColliders;
    }

    /**
     * Returns all generated meshes for rendering.
     */
    public List<Mesh> getMeshes() {
        return meshes;
    }

    /**
     * Returns all transforms for meshes.
     */
    public List<Matrix4> getTransforms() {
        return transforms;
    }

    /**
     * Adds all meshes and transforms to the scene arrays.
     */
    public void addMeshesToScene(Array<Mesh> sceneMeshes, Array<Matrix4> sceneTransforms) {
        for (Mesh mesh : meshes) {
            sceneMeshes.add(mesh);
        }
        for (Matrix4 transform : transforms) {
            sceneTransforms.add(transform);
        }
    }

    /**
     * Spawn parsed doors directly into the provided scene using the scene's
     * `spawnMapDoor` helper so meshes and interactables are registered
     * consistently.
     */
    public void spawnDoorsIntoScene(UniversalTestScene scene) {
        int idx = 0;
        for (DoorDefinition def : doors) {
            String id = def.id != null ? def.id : "json-door-" + (idx++);
            // use hinge position (y is stored in hingePosition.y already)
            scene.spawnMapDoor(id, def.hingePosition.x, def.hingePosition.y, def.hingePosition.z, def.locked);
        }
    }

    /**
     * Convenience static method to build a JSON map and integrate it into a scene.
     */
    public static void buildFromJson(FileHandle fh, UniversalTestScene scene) {
        TestMapBuilder builder = new TestMapBuilder();
        builder.loadFromJson(fh.path());
        // Integrate builder data into the provided scene via its integration helper
        scene.integrateTestMapBuilder(builder);
    }

    /**
     * Clears all generated data.
     */
    public void clear() {
        // Dispose meshes to avoid memory leaks
        for (Mesh mesh : meshes) {
            mesh.dispose();
        }
        worldColliders.clear();
        meshes.clear();
        transforms.clear();
        doors.clear();
    }

    public float getOriginX() { return originX; }
    public float getOriginZ() { return originZ; }
}
