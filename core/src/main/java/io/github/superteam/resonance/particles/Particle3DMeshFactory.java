package io.github.superteam.resonance.particles;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;

public final class Particle3DMeshFactory {
    private Particle3DMeshFactory() {
    }

    public static Mesh build(ParticleMeshType meshType) {
        if (meshType == null) {
            meshType = ParticleMeshType.CUBE;
        }

        switch (meshType) {
            case QUAD:
                return buildQuad();
            case TETRAHEDRON:
                return buildTetrahedron();
            case OCTAHEDRON:
                return buildOctahedron();
            case ICOSAHEDRON:
                return buildIcosahedron();
            case CAPSULE:
                return buildCapsule();
            case PYRAMID:
                return buildPyramid();
            case CUSTOM:
            case CUBE:
            default:
                return buildCube();
        }
    }

    private static Mesh buildQuad() {
        float[] triangles = {
            -0.5f, -0.5f, 0f,   0.5f, -0.5f, 0f,   0.5f, 0.5f, 0f,
            -0.5f, -0.5f, 0f,   0.5f, 0.5f, 0f,    -0.5f, 0.5f, 0f
        };
        return buildFromTriangles(triangles);
    }

    private static Mesh buildCube() {
        float[] triangles = {
            // +Z
            -0.5f, -0.5f, 0.5f,  0.5f, -0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,
            // -Z
             0.5f, -0.5f,-0.5f, -0.5f, -0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
             0.5f, -0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            // -X
            -0.5f, -0.5f,-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
            // +X
             0.5f, -0.5f, 0.5f,  0.5f, -0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
             0.5f, -0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            // +Y
            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,
            // -Y
            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f,  -0.5f,-0.5f, 0.5f
        };
        return buildFromTriangles(triangles);
    }

    private static Mesh buildTetrahedron() {
        float s = 0.62f;
        Vector3 v0 = new Vector3( s, s, s);
        Vector3 v1 = new Vector3(-s,-s, s);
        Vector3 v2 = new Vector3(-s, s,-s);
        Vector3 v3 = new Vector3( s,-s,-s);
        float[] triangles = {
            v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z,
            v0.x, v0.y, v0.z, v3.x, v3.y, v3.z, v1.x, v1.y, v1.z,
            v0.x, v0.y, v0.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z,
            v1.x, v1.y, v1.z, v3.x, v3.y, v3.z, v2.x, v2.y, v2.z
        };
        return buildFromTriangles(triangles);
    }

    private static Mesh buildOctahedron() {
        Vector3 top = new Vector3(0f, 0.65f, 0f);
        Vector3 bottom = new Vector3(0f, -0.65f, 0f);
        Vector3 px = new Vector3(0.65f, 0f, 0f);
        Vector3 nx = new Vector3(-0.65f, 0f, 0f);
        Vector3 pz = new Vector3(0f, 0f, 0.65f);
        Vector3 nz = new Vector3(0f, 0f, -0.65f);
        float[] triangles = {
            top.x, top.y, top.z, px.x, px.y, px.z, pz.x, pz.y, pz.z,
            top.x, top.y, top.z, pz.x, pz.y, pz.z, nx.x, nx.y, nx.z,
            top.x, top.y, top.z, nx.x, nx.y, nx.z, nz.x, nz.y, nz.z,
            top.x, top.y, top.z, nz.x, nz.y, nz.z, px.x, px.y, px.z,
            bottom.x, bottom.y, bottom.z, pz.x, pz.y, pz.z, px.x, px.y, px.z,
            bottom.x, bottom.y, bottom.z, nx.x, nx.y, nx.z, pz.x, pz.y, pz.z,
            bottom.x, bottom.y, bottom.z, nz.x, nz.y, nz.z, nx.x, nx.y, nx.z,
            bottom.x, bottom.y, bottom.z, px.x, px.y, px.z, nz.x, nz.y, nz.z
        };
        return buildFromTriangles(triangles);
    }

    private static Mesh buildIcosahedron() {
        float phi = (1f + (float) Math.sqrt(5f)) * 0.5f;
        Vector3[] vertices = {
            new Vector3(-1,  phi, 0), new Vector3(1,  phi, 0), new Vector3(-1, -phi, 0), new Vector3(1, -phi, 0),
            new Vector3(0, -1,  phi), new Vector3(0, 1,  phi), new Vector3(0, -1, -phi), new Vector3(0, 1, -phi),
            new Vector3( phi, 0, -1), new Vector3( phi, 0, 1), new Vector3(-phi, 0, -1), new Vector3(-phi, 0, 1)
        };
        for (Vector3 v : vertices) {
            v.nor().scl(0.65f);
        }

        int[] faces = {
            0,11,5,  0,5,1,   0,1,7,   0,7,10,  0,10,11,
            1,5,9,   5,11,4, 11,10,2, 10,7,6,  7,1,8,
            3,9,4,   3,4,2,   3,2,6,   3,6,8,   3,8,9,
            4,9,5,   2,4,11,  6,2,10,  8,6,7,   9,8,1
        };

        float[] triangles = new float[faces.length * 3];
        int cursor = 0;
        for (int i = 0; i < faces.length; i++) {
            Vector3 v = vertices[faces[i]];
            triangles[cursor++] = v.x;
            triangles[cursor++] = v.y;
            triangles[cursor++] = v.z;
        }

        return buildFromTriangles(triangles);
    }

    private static Mesh buildCapsule() {
        // Initial implementation keeps the mesh lightweight while we migrate the full 3D stack.
        return buildOctahedron();
    }

    private static Mesh buildPyramid() {
        Vector3 top = new Vector3(0f, 0.75f, 0f);
        Vector3 a = new Vector3(-0.5f, -0.5f, -0.5f);
        Vector3 b = new Vector3(0.5f, -0.5f, -0.5f);
        Vector3 c = new Vector3(0.5f, -0.5f, 0.5f);
        Vector3 d = new Vector3(-0.5f, -0.5f, 0.5f);
        float[] triangles = {
            top.x, top.y, top.z, a.x, a.y, a.z, b.x, b.y, b.z,
            top.x, top.y, top.z, b.x, b.y, b.z, c.x, c.y, c.z,
            top.x, top.y, top.z, c.x, c.y, c.z, d.x, d.y, d.z,
            top.x, top.y, top.z, d.x, d.y, d.z, a.x, a.y, a.z,
            a.x, a.y, a.z, d.x, d.y, d.z, c.x, c.y, c.z,
            a.x, a.y, a.z, c.x, c.y, c.z, b.x, b.y, b.z
        };
        return buildFromTriangles(triangles);
    }

    private static Mesh buildFromTriangles(float[] triangles) {
        int triangleCount = triangles.length / 9;
        float[] vertices = new float[triangleCount * 3 * 6];
        short[] indices = new short[triangleCount * 3];

        Vector3 p0 = new Vector3();
        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();
        Vector3 edge1 = new Vector3();
        Vector3 edge2 = new Vector3();
        Vector3 normal = new Vector3();

        int vertexCursor = 0;
        int triangleCursor = 0;
        short index = 0;
        for (int i = 0; i < triangles.length; i += 9) {
            p0.set(triangles[i], triangles[i + 1], triangles[i + 2]);
            p1.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]);
            p2.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]);

            edge1.set(p1).sub(p0);
            edge2.set(p2).sub(p0);
            normal.set(edge1).crs(edge2).nor();
            if (normal.isZero(0.0001f)) {
                normal.set(Vector3.Y);
            }

            vertexCursor = writeVertex(vertices, vertexCursor, p0, normal);
            vertexCursor = writeVertex(vertices, vertexCursor, p1, normal);
            vertexCursor = writeVertex(vertices, vertexCursor, p2, normal);

            indices[triangleCursor++] = index++;
            indices[triangleCursor++] = index++;
            indices[triangleCursor++] = index++;
        }

        Mesh mesh = new Mesh(
            true,
            triangleCount * 3,
            triangleCount * 3,
            new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal")
            )
        );
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        return mesh;
    }

    private static int writeVertex(float[] vertices, int cursor, Vector3 position, Vector3 normal) {
        vertices[cursor++] = position.x;
        vertices[cursor++] = position.y;
        vertices[cursor++] = position.z;
        vertices[cursor++] = normal.x;
        vertices[cursor++] = normal.y;
        vertices[cursor++] = normal.z;
        return cursor;
    }
}
