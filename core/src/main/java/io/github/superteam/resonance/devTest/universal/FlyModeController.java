package io.github.superteam.resonance.devTest.universal;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;

/**
 * Fly-mode controller for developer-only test screens.
 */
public class FlyModeController {
    private boolean enabled = false;
    private final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();
    private float moveSpeed = 6f;
    private float sprintMultiplier = 2.0f;

    public FlyModeController() {}

    public void setPosition(Vector3 p) { position.set(p); }
    public Vector3 position() { return position; }
    public boolean enabled() { return enabled; }
    public void toggle() { enabled = !enabled; }

    public void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            toggle();
        }
        if (!enabled) return;

        float forward = 0f, right = 0f, up = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) forward += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) forward -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) right += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) right -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) up += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) up -= 1f;

        float speed = moveSpeed * (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ? sprintMultiplier : 1f);
        velocity.set(right * speed, up * speed, forward * speed);
        position.mulAdd(velocity, delta);
    }
}
