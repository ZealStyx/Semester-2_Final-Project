package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Array;

/**
 * TestMapLayout builds a procedural test map description used by the UniversalTestScene.
 * It provides zone definitions, door spawn points, surface zones, room boundaries,
 * and helpers to create static physics geometry into a Bullet world.
 */
public class TestMapLayout {

    public static class Zone {
        public final String name;
        public final float x, y, z;
        public final float width, depth, height;

        public Zone(String name, float x, float y, float z, float width, float depth, float height) {
            this.name = name;
            this.x = x; this.y = y; this.z = z;
            this.width = width; this.depth = depth; this.height = height;
        }

        public Vector3 center() { return new Vector3(x, y, z); }
        public BoundingBox toBoundingBox() {
            return new BoundingBox(
                new Vector3(x - (width * 0.5f), y - (height * 0.5f), z - (depth * 0.5f)),
                new Vector3(x + (width * 0.5f), y + (height * 0.5f), z + (depth * 0.5f))
            );
        }
    }

    public static final class DoorSpawn {
        public final String id;
        public final float x, y, z; // world-local within layout
        public final float hingeOffsetX, hingeOffsetY, hingeOffsetZ; // hinge relative to door center
        public final boolean locked;

        public DoorSpawn(String id, float x, float y, float z, float hingeOffsetX, float hingeOffsetY, float hingeOffsetZ, boolean locked) {
            this.id = id;
            this.x = x; this.y = y; this.z = z;
            this.hingeOffsetX = hingeOffsetX; this.hingeOffsetY = hingeOffsetY; this.hingeOffsetZ = hingeOffsetZ;
            this.locked = locked;
        }

        public Vector3 hingeWorldPosition(float hubX, float hubZ) {
            return new Vector3(hubX + x + hingeOffsetX, y + hingeOffsetY, hubZ + z + hingeOffsetZ);
        }
    }

    public static final class SurfaceZone {
        public final String id;
        public final float x, y, z, width, depth, height;
        public final io.github.superteam.resonance.footstep.SurfaceMaterial material;

        public SurfaceZone(String id, float x, float y, float z, float width, float depth, float height, io.github.superteam.resonance.footstep.SurfaceMaterial mat) {
            this.id = id; this.x = x; this.y = y; this.z = z; this.width = width; this.depth = depth; this.height = height; this.material = mat;
        }

        public BoundingBox toBoundingBox() {
            return new BoundingBox(
                new Vector3(x - (width * 0.5f), y - (height * 0.5f), z - (depth * 0.5f)),
                new Vector3(x + (width * 0.5f), y + (height * 0.5f), z + (depth * 0.5f))
            );
        }
    }

    public static final class RoomBoundaryDef {
        public final BoundingBox box;
        public final String eventId;

        public RoomBoundaryDef(BoundingBox box, String eventId) {
            this.box = new BoundingBox(box);
            this.eventId = eventId;
        }
    }

    // Instance data
    private final Array<Zone> zones = new Array<>();
    private final Array<DoorSpawn> doorSpawns = new Array<>();
    private final Array<SurfaceZone> surfaceZones = new Array<>();
    private final Array<RoomBoundaryDef> roomBoundaries = new Array<>();
    private final Array<Vector3> patrolWaypoints = new Array<>();
    private BoundingBox darkRoomVolume = null;

    // Physics bookkeeping for shapes/motionstates created by buildPhysics
    private final Array<com.badlogic.gdx.physics.bullet.collision.btCollisionShape> createdShapes = new Array<>();
    private final Array<btDefaultMotionState> createdMotionStates = new Array<>();
    private final Array<btRigidBody> createdBodies = new Array<>();
        private final Array<btRigidBody.btRigidBodyConstructionInfo> createdInfos = new Array<>();

    private TestMapLayout() {}

    public static TestMapLayout buildDefaultLayoutInstance() {
        TestMapLayout lm = new TestMapLayout();

        // ─── Zone Detection Volumes ─────────────────────────────────────────────
        // Zone(name, localX, localY, localZ, width, depth, height)
        // localY = 0 centers the volume at floor; used for zone detection, not walls.
        lm.zones.add(new Zone("SPAWN",         0f,   0f,   0f,   7.0f,  6.0f,  3.0f));
        lm.zones.add(new Zone("CORRIDOR_1",    0f,   0f,   7.0f, 3.0f,  8.0f,  2.8f));
        lm.zones.add(new Zone("DARK_ROOM",     0f,   0f,  15.5f, 9.0f,  9.0f,  3.2f));
        lm.zones.add(new Zone("CROUCH_ALCOVE",-5.5f, 0f,   7.0f, 6.0f,  3.0f,  1.55f));

        // ─── Door Spawns ─────────────────────────────────────────────────────────
        // Door behaviour (creak speed, barge) is 100% driven by player input:
        //   • Mouse drag speed  → DoorCreakSystem selects creak_slow / medium / fast wav
        //   • Sprint into door  → DoorBargeDetector fires bargeOpen() (instant 90°)

        // SPAWN → CORRIDOR_1 door
        //   World position: (40.0, 0, 43.15)  →  local: (0.0, 0, 3.15)
        //   Hinge on the west side of the 1.2 m gap (X = 39.4 world → offset = −0.6)
        lm.doorSpawns.add(new DoorSpawn(
            "door-spawn-north",
             0.0f, 0.0f, 3.15f,   // localX, y, localZ  (world: 40, 0, 43.15)
            -0.6f, 1.05f, 0f,     // hingeOffsetX, hingeOffsetY, hingeOffsetZ
             false                 // locked
        ));

        // CORRIDOR_1 → CROUCH_ALCOVE door
        //   World position: (38.5, 0, 46.25)  →  local: (−1.5, 0, 6.25)
        //   Hinge on the south side of the 1.5 m gap (Z = 46.25 world → offset = +0.6 in Z)
        lm.doorSpawns.add(new DoorSpawn(
            "door-corridor-alcove",
            -1.5f, 0.0f, 6.25f,   // localX, y, localZ  (world: 38.5, 0, 46.25)
             0f,   1.05f, 0.6f,   // hingeOffsetX, hingeOffsetY, hingeOffsetZ
             false                 // locked
        ));

        // ─── Surface Zones ───────────────────────────────────────────────────────
        // Spawn room: concrete
        lm.surfaceZones.add(new SurfaceZone("surf_spawn",     0f, 0f,   0f,  7.0f, 0.2f, 6.0f,
            io.github.superteam.resonance.footstep.SurfaceMaterial.CONCRETE));
        // Corridor: metal grating
        lm.surfaceZones.add(new SurfaceZone("surf_corridor",  0f, 0f,  7.0f, 3.0f, 0.2f, 8.0f,
            io.github.superteam.resonance.footstep.SurfaceMaterial.METAL));
        // Dark room: wood
        lm.surfaceZones.add(new SurfaceZone("surf_darkroom",  0f, 0f, 15.5f, 9.0f, 0.2f, 9.0f,
            io.github.superteam.resonance.footstep.SurfaceMaterial.WOOD));
        // Crouch alcove: carpet (muffled)
        lm.surfaceZones.add(new SurfaceZone("surf_alcove", -5.5f, 0f, 7.0f,  6.0f, 0.2f, 3.0f,
            io.github.superteam.resonance.footstep.SurfaceMaterial.CARPET));

        // ─── Room Boundaries ─────────────────────────────────────────────────────
        lm.roomBoundaries.add(new RoomBoundaryDef(
            new BoundingBox(new Vector3(36.5f, -1f, 37f), new Vector3(43.5f, 4f, 43f)),
            "spawn_boundary"));
        lm.roomBoundaries.add(new RoomBoundaryDef(
            new BoundingBox(new Vector3(35.5f, -1f, 51f), new Vector3(44.5f, 4f, 60f)),
            "darkroom_boundary"));

        // ─── Dark Room Volume (for sanity drain) ─────────────────────────────────
        lm.darkRoomVolume = new BoundingBox(
            new Vector3(35.5f, -1f, 51.0f),
            new Vector3(44.5f,  4f, 60.0f));

        // ─── Patrol Waypoints ────────────────────────────────────────────────────
        lm.patrolWaypoints.add(new Vector3(40f, 0f, 53f));
        lm.patrolWaypoints.add(new Vector3(37f, 0f, 57f));
        lm.patrolWaypoints.add(new Vector3(43f, 0f, 57f));

        return lm;
    }

    public Array<Zone> zones() { return zones; }
    public Array<DoorSpawn> doorSpawns() { return doorSpawns; }
    public Array<SurfaceZone> surfaceZones() { return surfaceZones; }
    public Array<RoomBoundaryDef> boundaryVolumes() { return roomBoundaries; }
    public Array<Vector3> patrolWaypoints() { return patrolWaypoints; }
    public BoundingBox darkRoomVolume() { return darkRoomVolume; }

    /**
     * Build static physics geometry into the provided Bullet world for all zones.
     * This creates btBoxShape bodies (mass = 0) and adds them to the world. It
     * stores created shapes/motionstates/bodies for later disposal via {@link #disposePhysics()}
     */
    public void buildPhysics(btDiscreteDynamicsWorld world, float hubX, float hubZ) {
        if (world == null) return;
        for (Zone z : zones) {
            Vector3 center = new Vector3(hubX + z.x, z.y, hubZ + z.z);
            Vector3 halfExtents = new Vector3(z.width * 0.5f, z.height * 0.5f, z.depth * 0.5f);
            btBoxShape shape = new btBoxShape(halfExtents);
            createdShapes.add(shape);
            com.badlogic.gdx.math.Matrix4 transform = new com.badlogic.gdx.math.Matrix4().setToTranslation(center);
            btDefaultMotionState ms = new btDefaultMotionState(transform);
            createdMotionStates.add(ms);
                btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0f, ms, shape, new Vector3(0f,0f,0f));
                btRigidBody body = new btRigidBody(info);
                createdBodies.add(body);
                createdInfos.add(info);
                world.addRigidBody(body);
        }
    }

    /** Dispose any physics objects created by buildPhysics. */
    public void disposePhysics(btDiscreteDynamicsWorld world) {
        if (world != null) {
            for (btRigidBody b : createdBodies) {
                try { world.removeRigidBody(b); } catch (Exception ignored) {}
                b.dispose();
            }
        }
        for (btDefaultMotionState ms : createdMotionStates) ms.dispose();
        for (com.badlogic.gdx.physics.bullet.collision.btCollisionShape s : createdShapes) s.dispose();
            for (btRigidBody.btRigidBodyConstructionInfo info : createdInfos) info.dispose();
            createdBodies.clear(); createdMotionStates.clear(); createdShapes.clear(); createdInfos.clear();
    }
}
