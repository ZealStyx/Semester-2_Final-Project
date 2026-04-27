package io.github.superteam.resonance.map;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import io.github.superteam.resonance.model.ModelData;

/**
 * Builds map collision bounds and triangle snapshots from a loaded GLTF model.
 */
public final class MapCollisionBuilder {
    private MapCollisionBuilder() {
    }

    public static CollisionData build(
        ModelData mapData,
        float mapScale,
        float mapYOffset,
        float minColliderSize,
        float minColliderHeight
    ) {
        Array<BoundingBox> colliders = new Array<>();
        FloatArray triangleSnapshot = new FloatArray();

        Model model = mapData == null ? null : mapData.model();
        if (model == null || model.meshParts == null || model.meshParts.size == 0) {
            return new CollisionData(colliders, triangleSnapshot.toArray());
        }

        Vector3 v0 = new Vector3();
        Vector3 v1 = new Vector3();
        Vector3 v2 = new Vector3();

        for (MeshPart meshPart : model.meshParts) {
            if (meshPart == null || meshPart.mesh == null || meshPart.size <= 0) {
                continue;
            }

            Mesh mesh = meshPart.mesh;
            VertexAttribute positionAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Position);
            if (positionAttr == null) {
                continue;
            }

            int strideFloats = mesh.getVertexSize() / 4;
            int positionOffset = positionAttr.offset / 4;
            float[] vertices = new float[mesh.getNumVertices() * strideFloats];
            mesh.getVertices(vertices);

            short[] indices = new short[mesh.getNumIndices()];
            mesh.getIndices(indices);

            int start = Math.max(0, meshPart.offset);
            int end = Math.min(indices.length, start + meshPart.size);
            if (start >= end) {
                continue;
            }

            BoundingBox partBounds = new BoundingBox();
            boolean hasPoint = false;
            for (int i = start; i < end; i++) {
                int vertexIndex = indices[i] & 0xFFFF;
                int base = (vertexIndex * strideFloats) + positionOffset;
                if (base + 2 >= vertices.length) {
                    continue;
                }

                float x = vertices[base] * mapScale;
                float y = (vertices[base + 1] * mapScale) + mapYOffset;
                float z = vertices[base + 2] * mapScale;
                partBounds.ext(x, y, z);
                hasPoint = true;
            }

            for (int i = start; i + 2 < end; i += 3) {
                int index0 = indices[i] & 0xFFFF;
                int index1 = indices[i + 1] & 0xFFFF;
                int index2 = indices[i + 2] & 0xFFFF;

                int base0 = (index0 * strideFloats) + positionOffset;
                int base1 = (index1 * strideFloats) + positionOffset;
                int base2 = (index2 * strideFloats) + positionOffset;
                if (base0 + 2 >= vertices.length || base1 + 2 >= vertices.length || base2 + 2 >= vertices.length) {
                    continue;
                }

                v0.set(vertices[base0] * mapScale, (vertices[base0 + 1] * mapScale) + mapYOffset, vertices[base0 + 2] * mapScale);
                v1.set(vertices[base1] * mapScale, (vertices[base1 + 1] * mapScale) + mapYOffset, vertices[base1 + 2] * mapScale);
                v2.set(vertices[base2] * mapScale, (vertices[base2 + 1] * mapScale) + mapYOffset, vertices[base2 + 2] * mapScale);

                triangleSnapshot.addAll(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);
            }

            if (!hasPoint) {
                continue;
            }

            Vector3 dimensions = partBounds.getDimensions(new Vector3());
            if (dimensions.x < 0.02f || dimensions.z < 0.02f) {
                continue;
            }

            Vector3 center = partBounds.getCenter(new Vector3());
            float width = Math.max(dimensions.x, minColliderSize);
            float depth = Math.max(dimensions.z, minColliderSize);
            float height = Math.max(dimensions.y, minColliderHeight);
            colliders.add(new BoundingBox(
                new Vector3(center.x - (width * 0.5f), center.y - (height * 0.5f), center.z - (depth * 0.5f)),
                new Vector3(center.x + (width * 0.5f), center.y + (height * 0.5f), center.z + (depth * 0.5f))
            ));
        }

        return new CollisionData(colliders, triangleSnapshot.toArray());
    }

    public record CollisionData(Array<BoundingBox> colliders, float[] debugTriangles) {
    }
}