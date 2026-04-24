package io.github.superteam.resonance.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

public final class RemotePlayer {
    public final int id;
    public String name;
    public Color color;           // assigned on join
    public final Vector3 targetPosition = new Vector3();
    public final Vector3 currentPosition = new Vector3();
    public final Vector3 lookDir  = new Vector3(0, 0, -1);
    public boolean isSpeaking;    // true while sending voice audio
    public float speakingTimer;   // fade-out timer for speaking indicator

    // Render data — capsule (cylinder body + two sphere caps)
    private static final float RADIUS  = 0.4f;
    private static final float HEIGHT  = 1.6f;  // eye height
    private static final float LERP_FACTOR = 0.15f;

    public RemotePlayer(int id) {
        this.id = id;
    }

    public void update(float delta) {
        currentPosition.lerp(targetPosition, LERP_FACTOR);
    }

    public void render(ShapeRenderer shape) {
        shape.setColor(isSpeaking ? Color.YELLOW : color);

        // Body (using box proxy for multiplayer test character)
        shape.box(currentPosition.x - RADIUS, currentPosition.y - HEIGHT, currentPosition.z - RADIUS,
                  RADIUS * 2, HEIGHT, RADIUS * 2);

        // Small head marker so joined players are easier to spot at distance.
        shape.setColor(Color.WHITE);
        shape.box(currentPosition.x - 0.16f, currentPosition.y + 0.02f, currentPosition.z - 0.16f,
                0.32f, 0.32f, 0.32f);
    }
}
