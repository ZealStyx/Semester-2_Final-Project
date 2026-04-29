package io.github.superteam.resonance.prop;

import com.badlogic.gdx.math.Vector3;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PropInstance {
    public String definitionId = "prop";
    public Vector3 position = new Vector3();
    public float rotationYDegrees;
    public Vector3 scale = new Vector3(1f, 1f, 1f);
    public Map<String, String> overrides = new LinkedHashMap<>();

    public PropInstance() {
    }

    public PropInstance(String definitionId, Vector3 position, float rotationYDegrees) {
        this.definitionId = definitionId;
        this.position = position == null ? new Vector3() : new Vector3(position);
        this.rotationYDegrees = rotationYDegrees;
    }
}
