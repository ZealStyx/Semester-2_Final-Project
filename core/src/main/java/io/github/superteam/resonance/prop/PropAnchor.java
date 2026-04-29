package io.github.superteam.resonance.prop;

import com.badlogic.gdx.math.Vector3;

public final class PropAnchor {
    public String name = "anchor";
    public Vector3 position = new Vector3();
    public Vector3 normal = new Vector3(0f, 1f, 0f);
    public boolean primary;

    public PropAnchor() {
    }

    public PropAnchor(String name, Vector3 position, Vector3 normal, boolean primary) {
        this.name = name;
        this.position = position == null ? new Vector3() : new Vector3(position);
        this.normal = normal == null ? new Vector3(0f, 1f, 0f) : new Vector3(normal);
        this.primary = primary;
    }
}
