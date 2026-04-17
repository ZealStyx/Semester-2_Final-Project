package io.github.superteam.resonance.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class TrailEmitter {
    private static final int VERTEX_STRIDE = 9;

    private final Array<TrailPoint> points = new Array<>(false, 128);
    private final Mesh trailMesh;
    private final ShaderProgram shaderProgram;
    private final Texture texture;
    private final Vector3 headPosition = new Vector3();
    private final Vector3 lastSamplePosition = new Vector3();
    private final Vector3 trailDirectionBuffer = new Vector3();
    private final Vector3 cameraDirectionBuffer = new Vector3();
    private final Vector3 sideBuffer = new Vector3();
    private final float[] vertexData;

    private final Color startColor = new Color(1f, 0.85f, 0.4f, 0.95f);
    private final Color endColor = new Color(1f, 0.2f, 0.05f, 0f);

    private boolean active = true;
    private float pointLifetime = 0.8f;
    private float minSampleDistance = 0.05f;
    private float widthStart = 0.22f;
    private float widthEnd = 0.02f;
    private float elapsedTime;
    private boolean hasLastSamplePosition;

    public TrailEmitter() {
        this(128);
    }

    public TrailEmitter(int maxPoints) {
        int safeMaxPoints = Math.max(2, maxPoints);
        trailMesh = new Mesh(
            false,
            safeMaxPoints * 2,
            0,
            new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
            )
        );
        vertexData = new float[safeMaxPoints * 2 * VERTEX_STRIDE];
        texture = createWhiteTexture();
        shaderProgram = new ShaderProgram(
            Gdx.files.internal("shaders/vert/trail_shader.vert"),
            Gdx.files.internal("shaders/frag/trail_shader.frag")
        );
        if (!shaderProgram.isCompiled()) {
            throw new IllegalStateException("Trail shader failed to compile: " + shaderProgram.getLog());
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setPointLifetime(float pointLifetime) {
        this.pointLifetime = Math.max(0.05f, pointLifetime);
    }

    public void setSampleDistance(float sampleDistance) {
        minSampleDistance = Math.max(0.001f, sampleDistance);
    }

    public void setWidth(float startWidth, float endWidth) {
        widthStart = Math.max(0.001f, startWidth);
        widthEnd = Math.max(0.001f, endWidth);
    }

    public void setColors(Color startColor, Color endColor) {
        if (startColor != null) {
            this.startColor.set(startColor);
        }
        if (endColor != null) {
            this.endColor.set(endColor);
        }
    }

    public void setHeadPosition(Vector3 position) {
        if (position == null) {
            return;
        }

        headPosition.set(position);
        if (!hasLastSamplePosition) {
            lastSamplePosition.set(position);
            hasLastSamplePosition = true;
            addPoint(position);
        }
    }

    public void addPoint(Vector3 position) {
        if (position == null) {
            return;
        }

        TrailPoint point = new TrailPoint();
        point.position.set(position);
        point.age = 0f;
        points.add(point);
    }

    public void clear() {
        points.clear();
        lastSamplePosition.setZero();
        hasLastSamplePosition = false;
    }

    public int getActivePointCount() {
        return points.size;
    }

    public float getApproximateBoundsRadius() {
        float maxWidth = Math.max(widthStart, widthEnd);
        return maxWidth + (pointLifetime * 12f);
    }

    public Vector3 getPosition() {
        return headPosition;
    }

    public void update(float delta) {
        if (!active || delta <= 0f) {
            return;
        }

        elapsedTime += delta;

        for (int i = points.size - 1; i >= 0; i--) {
            TrailPoint point = points.get(i);
            point.age += delta;
            if (point.age >= pointLifetime) {
                points.removeIndex(i);
            }
        }

        if (hasLastSamplePosition && headPosition.dst2(lastSamplePosition) >= minSampleDistance * minSampleDistance) {
            lastSamplePosition.set(headPosition);
            addPoint(headPosition);
        }
    }

    public void render(PerspectiveCamera camera) {
        if (!active || camera == null || points.size < 2) {
            return;
        }

        buildVertexData(camera);
        if (vertexDataLength <= 0) {
            return;
        }

        shaderProgram.bind();
        texture.bind(0);
        shaderProgram.setUniformi("u_texture", 0);
        shaderProgram.setUniformMatrix("u_projViewTrans", camera.combined);
        shaderProgram.setUniformf("u_time", elapsedTime);

        trailMesh.setVertices(vertexData, 0, vertexDataLength);
        trailMesh.render(shaderProgram, GL20.GL_TRIANGLE_STRIP);
    }

    public void dispose() {
        trailMesh.dispose();
        shaderProgram.dispose();
        texture.dispose();
    }

    private int vertexDataLength;

    private void buildVertexData(PerspectiveCamera camera) {
        vertexDataLength = 0;
        Vector3 cameraPosition = camera.position;

        for (int i = 0; i < points.size; i++) {
            TrailPoint point = points.get(i);
            float normalizedAge = MathUtils.clamp(point.age / pointLifetime, 0f, 1f);
            float width = MathUtils.lerp(widthStart, widthEnd, normalizedAge);
            Color color = interpolateColor(normalizedAge);

            Vector3 direction = sampleDirection(i);
            cameraDirectionBuffer.set(cameraPosition).sub(point.position);
            sideBuffer.set(direction).crs(cameraDirectionBuffer);
            if (sideBuffer.isZero(0.0001f)) {
                sideBuffer.set(direction).crs(Vector3.Y);
            }
            if (sideBuffer.isZero(0.0001f)) {
                sideBuffer.set(1f, 0f, 0f);
            } else {
                sideBuffer.nor();
            }

            Vector3 left = new Vector3(point.position).mulAdd(sideBuffer, width * 0.5f);
            Vector3 right = new Vector3(point.position).mulAdd(sideBuffer, -width * 0.5f);

            float u = normalizedAge;
            vertexDataLength = writeVertex(left, color, u, 0f, vertexDataLength);
            vertexDataLength = writeVertex(right, color, u, 1f, vertexDataLength);
        }
    }

    private Vector3 sampleDirection(int index) {
        if (points.size <= 1) {
            return trailDirectionBuffer.set(0f, 1f, 0f);
        }

        if (index == points.size - 1) {
            trailDirectionBuffer.set(points.get(index).position).sub(points.get(index - 1).position);
        } else {
            trailDirectionBuffer.set(points.get(index + 1).position).sub(points.get(index).position);
        }

        if (trailDirectionBuffer.isZero(0.0001f)) {
            trailDirectionBuffer.set(0f, 1f, 0f);
        } else {
            trailDirectionBuffer.nor();
        }
        return trailDirectionBuffer;
    }

    private Color interpolateColor(float normalizedAge) {
        float inverseAge = 1f - normalizedAge;
        Color interpolated = new Color();
        interpolated.r = MathUtils.lerp(endColor.r, startColor.r, inverseAge);
        interpolated.g = MathUtils.lerp(endColor.g, startColor.g, inverseAge);
        interpolated.b = MathUtils.lerp(endColor.b, startColor.b, inverseAge);
        interpolated.a = MathUtils.lerp(endColor.a, startColor.a, inverseAge);
        return interpolated;
    }

    private int writeVertex(Vector3 position, Color color, float u, float v, int cursor) {
        if (cursor + VERTEX_STRIDE > vertexData.length) {
            return cursor;
        }

        vertexData[cursor++] = position.x;
        vertexData[cursor++] = position.y;
        vertexData[cursor++] = position.z;
        vertexData[cursor++] = color.r;
        vertexData[cursor++] = color.g;
        vertexData[cursor++] = color.b;
        vertexData[cursor++] = color.a;
        vertexData[cursor++] = u;
        vertexData[cursor++] = v;
        return cursor;
    }

    private Texture createWhiteTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture createdTexture = new Texture(pixmap);
        createdTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        pixmap.dispose();
        return createdTexture;
    }

    private static final class TrailPoint {
        private final Vector3 position = new Vector3();
        private float age;
    }
}
