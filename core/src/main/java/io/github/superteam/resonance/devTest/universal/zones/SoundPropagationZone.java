package io.github.superteam.resonance.devTest.universal.zones;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.github.superteam.resonance.devTest.universal.BaseShellZone;

public final class SoundPropagationZone extends BaseShellZone {
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final ObjectMap<String, Vector3> nodePositions = new ObjectMap<>();
    private final Array<String> revealedIds = new Array<>();
    private final Vector3 tmpProjected = new Vector3();
    private final Matrix4 overlayProjection = new Matrix4();
    private final Vector3 lastPulseCenter = new Vector3();
    private float time;
    private float flashAlpha;

    public SoundPropagationZone(Vector3 center) {
        super("Sound Propagation", center, 8.0f);
    }

    public void setSoundData(ObjectMap<String, Vector3> sourceNodePositions, Array<String> sourceRevealedIds, float flashAlpha, Vector3 playerPosition) {
        nodePositions.clear();
        if (sourceNodePositions != null) {
            for (ObjectMap.Entry<String, Vector3> entry : sourceNodePositions.entries()) {
                nodePositions.put(entry.key, new Vector3(entry.value));
            }
        }

        revealedIds.clear();
        if (sourceRevealedIds != null) {
            for (String nodeId : sourceRevealedIds) {
                revealedIds.add(nodeId);
            }
        }

        this.flashAlpha = MathUtils.clamp(flashAlpha, 0f, 1f);

        if (playerPosition != null) {
            lastPulseCenter.set(playerPosition);
        } else if (revealedIds.size > 0) {
            int samples = 0;
            lastPulseCenter.setZero();
            for (String nodeId : revealedIds) {
                Vector3 nodePosition = nodePositions.get(nodeId);
                if (nodePosition != null) {
                    lastPulseCenter.add(nodePosition);
                    samples++;
                }
            }
            if (samples > 0) {
                lastPulseCenter.scl(1f / samples);
            } else {
                lastPulseCenter.set(getCenter().x, 0.05f, getCenter().z);
            }
        }
    }

    @Override
    public void setUp() {
        super.setUp();
        mutableState().put("graphHook", "SoundPropagationOrchestrator-ready");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        time += Math.max(0f, deltaSeconds);
        mutableState().put("graphNodes", "8");
        mutableState().put("graphEdges", "16");
        mutableState().put("propagationMs", String.format("%.2f", 0.18f + Math.abs((float) Math.sin(time)) * 0.24f));
    }

    @Override
    public void render(PerspectiveCamera camera) {
        if (camera == null || nodePositions.isEmpty()) {
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        overlayProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(overlayProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (ObjectMap.Entry<String, Vector3> entry : nodePositions.entries()) {
            tmpProjected.set(entry.value);
            camera.project(tmpProjected, 0, 0, screenWidth, screenHeight);
            if (tmpProjected.z < 0f || tmpProjected.z > 1f) {
                continue;
            }

            shapeRenderer.setColor(0.2f, 0.95f, 0.6f, 0.85f);
            shapeRenderer.circle(tmpProjected.x, tmpProjected.y, 3.8f, 14);

            if (flashAlpha > 0f && revealedIds.contains(entry.key, false)) {
                shapeRenderer.setColor(1.0f, 0.92f, 0.2f, flashAlpha);
                shapeRenderer.circle(tmpProjected.x, tmpProjected.y, 7.0f, 16);
            }
        }

        shapeRenderer.end();

        if (flashAlpha <= 0f) {
            return;
        }

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1.0f, 0.85f, 0.25f, flashAlpha);

        float baseRadius = 1.6f + (1f - flashAlpha) * 1.2f;
        drawWorldSphere(baseRadius, 26);
        drawWorldSphere(baseRadius + 1.4f, 30);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1.0f, 0.65f, 0.1f, flashAlpha);
        float markerSize = 0.12f;
        for (ObjectMap.Entry<String, Vector3> entry : nodePositions.entries()) {
            Vector3 nodePos = entry.value;
            shapeRenderer.box(
                nodePos.x - (markerSize * 0.5f),
                nodePos.y - (markerSize * 0.5f),
                nodePos.z - (markerSize * 0.5f),
                markerSize,
                markerSize,
                markerSize
            );
        }
        shapeRenderer.end();
    }

    private void drawWorldSphere(float radius, int segments) {
        float cx = lastPulseCenter.x;
        float cy = lastPulseCenter.y + 1.0f;
        float cz = lastPulseCenter.z;

        float[] latOffsets = {0f, radius * 0.6f, -radius * 0.6f};
        for (float lat : latOffsets) {
            float ringR = (float) Math.sqrt(Math.max(0f, (radius * radius) - (lat * lat)));
            float y = cy + lat;
            float px = cx + ringR;
            float pz = cz;
            for (int i = 1; i <= segments; i++) {
                float a = (i * MathUtils.PI2) / segments;
                float nx = cx + MathUtils.cos(a) * ringR;
                float nz = cz + MathUtils.sin(a) * ringR;
                shapeRenderer.line(px, y, pz, nx, y, nz);
                px = nx;
                pz = nz;
            }
        }

        for (int meridian = 0; meridian < 2; meridian++) {
            float rot = meridian * MathUtils.PI * 0.5f;
            float prevX = cx + MathUtils.cos(rot) * radius;
            float prevY = cy;
            float prevZ = cz + MathUtils.sin(rot) * radius;
            for (int i = 1; i <= segments; i++) {
                float a = (i * MathUtils.PI) / segments;
                float x = cx + MathUtils.cos(rot) * MathUtils.cos(a) * radius;
                float yy = cy + MathUtils.sin(a) * radius;
                float z = cz + MathUtils.sin(rot) * MathUtils.cos(a) * radius;
                shapeRenderer.line(prevX, prevY, prevZ, x, yy, z);
                prevX = x;
                prevY = yy;
                prevZ = z;
            }
        }
    }
}
