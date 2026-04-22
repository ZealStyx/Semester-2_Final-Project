package io.github.superteam.resonance.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds an acoustic graph by sampling world colliders.
 *
 * This places nodes on wall faces and links nearby nodes with simple line-of-sight checks.
 */
public final class GraphPopulator {
    private static final float WALL_NODE_SPACING_METERS = 2.0f;
    private static final float WALL_NODE_INSET_METERS = 0.06f;
    private static final float NODE_MERGE_DISTANCE_METERS = 0.35f;
    private static final float EDGE_MAX_DISTANCE_METERS = 4.0f;
    private static final float WALL_MIN_HEIGHT_METERS = 0.5f;
    private static final float MIN_TRANSMISSION_COEFFICIENT = 0.05f;
    private static final float MAX_TRANSMISSION_WEIGHT = 25.0f;

    private final Vector3 tmpIntersection = new Vector3();
    private final Ray tmpRay = new Ray(new Vector3(), new Vector3());

    public AcousticGraphEngine populate(Array<BoundingBox> colliders) {
        if (colliders == null || colliders.isEmpty()) {
            return createEmergencyGraph(40f, 1.2f, 40f, "Collider set is empty");
        }

        List<NodeSeed> seeds = new ArrayList<>();
        for (int colliderIndex = 0; colliderIndex < colliders.size; colliderIndex++) {
            BoundingBox collider = colliders.get(colliderIndex);
            if (collider == null) {
                continue;
            }
            addWallFaceSamples(seeds, collider, colliderIndex);
        }

        if (seeds.isEmpty()) {
            Vector3 fallbackCenter = estimateCollidersCenter(colliders);
            return createEmergencyGraph(fallbackCenter.x, fallbackCenter.y, fallbackCenter.z, "No acoustic wall seeds generated");
        }

        List<GraphNode> nodes = new ArrayList<>(seeds.size());
        for (int index = 0; index < seeds.size(); index++) {
            nodes.add(new GraphNode("dyn_" + index, seeds.get(index).position));
        }

        List<GraphEdge> edges = new ArrayList<>();
        int[] degree = new int[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                float distance = nodes.get(i).position().dst(nodes.get(j).position());
                if (distance > EDGE_MAX_DISTANCE_METERS) {
                    continue;
                }

                BlockingHit blockingHit = findFirstBlockingHit(seeds.get(i), seeds.get(j), colliders);
                if (blockingHit == null) {
                    AcousticMaterial edgeMaterial = mergePathMaterial(seeds.get(i).material, seeds.get(j).material);
                    edges.add(GraphEdge.between(nodes.get(i), nodes.get(j), edgeMaterial, 0.05f, 0.0f));
                    degree[i]++;
                    degree[j]++;
                    continue;
                }

                AcousticMaterial wallMaterial = inferWallMaterial(blockingHit.collisionBoxThicknessMeters);
                if (wallMaterial.transmissionCoefficient() <= MIN_TRANSMISSION_COEFFICIENT) {
                    continue;
                }

                GraphEdge transmissionEdge = GraphEdge.throughTransmission(
                    nodes.get(i),
                    nodes.get(j),
                    wallMaterial,
                    blockingHit.collisionBoxThicknessMeters,
                    0.08f,
                    0f
                );
                if (transmissionEdge.weight() > MAX_TRANSMISSION_WEIGHT) {
                    continue;
                }

                edges.add(transmissionEdge);
                degree[i]++;
                degree[j]++;
            }
        }

        // Keep isolated nodes useful by attaching each to its nearest visible neighbor.
        for (int i = 0; i < nodes.size(); i++) {
            if (degree[i] > 0) {
                continue;
            }

            int nearestIndex = -1;
            float nearestDistance = Float.POSITIVE_INFINITY;
            for (int j = 0; j < nodes.size(); j++) {
                if (i == j) {
                    continue;
                }

                float distance = nodes.get(i).position().dst(nodes.get(j).position());
                if (distance < nearestDistance && hasLineOfSight(seeds.get(i), seeds.get(j), colliders)) {
                    nearestDistance = distance;
                    nearestIndex = j;
                }
            }

            if (nearestIndex >= 0) {
                AcousticMaterial edgeMaterial = mergePathMaterial(seeds.get(i).material, seeds.get(nearestIndex).material);
                edges.add(GraphEdge.between(nodes.get(i), nodes.get(nearestIndex), edgeMaterial, 0.06f, 0.0f));
                degree[i]++;
                degree[nearestIndex]++;
            }
        }

        return new AcousticGraphEngine().buildGraphFromGeometry(nodes, edges);
    }

    private void addWallFaceSamples(List<NodeSeed> seeds, BoundingBox collider, int colliderIndex) {
        float minX = collider.min.x;
        float maxX = collider.max.x;
        float minY = collider.min.y;
        float maxY = collider.max.y;
        float minZ = collider.min.z;
        float maxZ = collider.max.z;

        float width = maxX - minX;
        float height = maxY - minY;
        float depth = maxZ - minZ;

        if (height < WALL_MIN_HEIGHT_METERS) {
            return;
        }

        AcousticMaterial colliderMaterial = inferColliderMaterial(width, height, depth);

        float[] ySamples = wallHeightSamples(minY, maxY);

        // X-aligned wall faces
        if (depth >= 0.4f) {
            for (float y : ySamples) {
                for (float z = minZ + (WALL_NODE_SPACING_METERS * 0.5f); z < maxZ; z += WALL_NODE_SPACING_METERS) {
                    addSeed(seeds, new Vector3(minX + WALL_NODE_INSET_METERS, y, z), colliderIndex, colliderMaterial);
                    addSeed(seeds, new Vector3(maxX - WALL_NODE_INSET_METERS, y, z), colliderIndex, colliderMaterial);
                }
            }
        }

        // Z-aligned wall faces
        if (width >= 0.4f) {
            for (float y : ySamples) {
                for (float x = minX + (WALL_NODE_SPACING_METERS * 0.5f); x < maxX; x += WALL_NODE_SPACING_METERS) {
                    addSeed(seeds, new Vector3(x, y, minZ + WALL_NODE_INSET_METERS), colliderIndex, colliderMaterial);
                    addSeed(seeds, new Vector3(x, y, maxZ - WALL_NODE_INSET_METERS), colliderIndex, colliderMaterial);
                }
            }
        }

        // Add a center probe for large walkable shells so single-mesh worlds still get interior connectivity.
        if (width >= 6f && depth >= 6f) {
            float centerY = Math.min(maxY - WALL_NODE_INSET_METERS, minY + 1.1f);
            addSeed(seeds, new Vector3((minX + maxX) * 0.5f, centerY, (minZ + maxZ) * 0.5f), colliderIndex, colliderMaterial);
        }
    }

    private float[] wallHeightSamples(float minY, float maxY) {
        float low = Math.min(maxY - WALL_NODE_INSET_METERS, minY + 0.9f);
        float mid = Math.min(maxY - WALL_NODE_INSET_METERS, minY + 1.6f);
        if (Math.abs(low - mid) < 0.15f) {
            return new float[] {low};
        }
        return new float[] {low, mid};
    }

    private void addSeed(List<NodeSeed> seeds, Vector3 candidate, int colliderIndex, AcousticMaterial material) {
        for (NodeSeed existing : seeds) {
            if (existing.position.dst2(candidate) <= (NODE_MERGE_DISTANCE_METERS * NODE_MERGE_DISTANCE_METERS)) {
                return;
            }
        }
        seeds.add(new NodeSeed(candidate, colliderIndex, material));
    }

    private AcousticMaterial inferColliderMaterial(float width, float height, float depth) {
        float minThickness = Math.min(width, depth);
        if (minThickness <= 0.18f) {
            return AcousticMaterial.GLASS;
        }
        if (minThickness <= 0.30f) {
            return AcousticMaterial.WOOD;
        }
        if (height >= 3.2f) {
            return AcousticMaterial.CONCRETE;
        }
        return AcousticMaterial.METAL;
    }

    private AcousticMaterial mergePathMaterial(AcousticMaterial left, AcousticMaterial right) {
        if (left == null) {
            return right == null ? AcousticMaterial.CONCRETE : right;
        }
        if (right == null) {
            return left;
        }
        return left.transmissionCoefficient() <= right.transmissionCoefficient() ? left : right;
    }

    private Vector3 estimateCollidersCenter(Array<BoundingBox> colliders) {
        Vector3 center = new Vector3();
        int sampleCount = 0;
        for (int i = 0; i < colliders.size; i++) {
            BoundingBox collider = colliders.get(i);
            if (collider == null) {
                continue;
            }
            center.add((collider.min.x + collider.max.x) * 0.5f, (collider.min.y + collider.max.y) * 0.5f, (collider.min.z + collider.max.z) * 0.5f);
            sampleCount++;
        }
        if (sampleCount == 0) {
            return center.set(40f, 1.2f, 40f);
        }
        return center.scl(1f / sampleCount);
    }

    private AcousticGraphEngine createEmergencyGraph(float centerX, float centerY, float centerZ, String reason) {
        Gdx.app.error("GraphPopulator", reason + "; falling back to emergency local graph at " + centerX + ", " + centerY + ", " + centerZ);

        float reach = 4.0f;
        GraphNode center = new GraphNode("dyn_emergency_center", new Vector3(centerX, centerY, centerZ));
        GraphNode north = new GraphNode("dyn_emergency_north", new Vector3(centerX, centerY, centerZ - reach));
        GraphNode south = new GraphNode("dyn_emergency_south", new Vector3(centerX, centerY, centerZ + reach));
        GraphNode east = new GraphNode("dyn_emergency_east", new Vector3(centerX + reach, centerY, centerZ));
        GraphNode west = new GraphNode("dyn_emergency_west", new Vector3(centerX - reach, centerY, centerZ));

        List<GraphNode> nodes = List.of(center, north, south, east, west);
        List<GraphEdge> edges = new ArrayList<>();
        edges.add(GraphEdge.between(center, north, AcousticMaterial.CONCRETE, 0.05f, 0f));
        edges.add(GraphEdge.between(center, south, AcousticMaterial.CONCRETE, 0.05f, 0f));
        edges.add(GraphEdge.between(center, east, AcousticMaterial.CONCRETE, 0.05f, 0f));
        edges.add(GraphEdge.between(center, west, AcousticMaterial.CONCRETE, 0.05f, 0f));

        return new AcousticGraphEngine().buildGraphFromGeometry(nodes, edges);
    }

    private boolean hasLineOfSight(NodeSeed from, NodeSeed to, Array<BoundingBox> colliders) {
        return findFirstBlockingHit(from, to, colliders) == null;
    }

    private BlockingHit findFirstBlockingHit(NodeSeed from, NodeSeed to, Array<BoundingBox> colliders) {
        Vector3 start = new Vector3(from.position).lerp(to.position, 0.01f);
        Vector3 end = new Vector3(to.position).lerp(from.position, 0.01f);
        Vector3 direction = new Vector3(end).sub(start);
        float segmentLength = direction.len();
        if (segmentLength <= 0.0001f) {
            return null;
        }
        direction.scl(1f / segmentLength);

        tmpRay.origin.set(start);
        tmpRay.direction.set(direction);

        float nearestHitDistance = Float.POSITIVE_INFINITY;
        float nearestHitThickness = 0f;

        for (int colliderIndex = 0; colliderIndex < colliders.size; colliderIndex++) {
            if (colliderIndex == from.colliderIndex || colliderIndex == to.colliderIndex) {
                continue;
            }
            BoundingBox collider = colliders.get(colliderIndex);
            if (collider == null) {
                continue;
            }
            if (Intersector.intersectRayBounds(tmpRay, collider, tmpIntersection)) {
                float hitDistance = start.dst(tmpIntersection);
                if (hitDistance <= segmentLength && hitDistance < nearestHitDistance) {
                    nearestHitDistance = hitDistance;
                    nearestHitThickness = estimateThicknessAlongDirection(collider, direction);
                }
            }
            if (collider.contains(start) || collider.contains(end)) {
                return new BlockingHit(estimateThicknessAlongDirection(collider, direction));
            }
        }

        if (Float.isFinite(nearestHitDistance)) {
            return new BlockingHit(nearestHitThickness);
        }
        return null;
    }

    private float estimateThicknessAlongDirection(BoundingBox collider, Vector3 direction) {
        float width = collider.max.x - collider.min.x;
        float height = collider.max.y - collider.min.y;
        float depth = collider.max.z - collider.min.z;
        float projection = (Math.abs(direction.x) * width)
            + (Math.abs(direction.y) * height)
            + (Math.abs(direction.z) * depth);
        return Math.max(0.05f, projection);
    }

    private AcousticMaterial inferWallMaterial(float estimatedThicknessMeters) {
        if (estimatedThicknessMeters <= 0.18f) {
            return AcousticMaterial.GLASS;
        }
        if (estimatedThicknessMeters <= 0.35f) {
            return AcousticMaterial.WOOD;
        }
        if (estimatedThicknessMeters <= 0.55f) {
            return AcousticMaterial.METAL;
        }
        return AcousticMaterial.CONCRETE;
    }

    private static final class NodeSeed {
        private final Vector3 position;
        private final int colliderIndex;
        private final AcousticMaterial material;

        private NodeSeed(Vector3 position, int colliderIndex, AcousticMaterial material) {
            this.position = position;
            this.colliderIndex = colliderIndex;
            this.material = material;
        }
    }

    private static final class BlockingHit {
        private final float collisionBoxThicknessMeters;

        private BlockingHit(float collisionBoxThicknessMeters) {
            this.collisionBoxThicknessMeters = collisionBoxThicknessMeters;
        }
    }
}
