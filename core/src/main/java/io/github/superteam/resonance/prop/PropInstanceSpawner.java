package io.github.superteam.resonance.prop;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public final class PropInstanceSpawner {
    private final PropDefinitionRegistry definitionRegistry;
    private final List<PropInstance> instances = new ArrayList<>();

    public PropInstanceSpawner(PropDefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry == null ? new PropDefinitionRegistry() : definitionRegistry;
    }

    public PropInstance spawn(String definitionId, Vector3 position, float rotationYDegrees) {
        if (definitionRegistry.find(definitionId) == null) {
            return null;
        }
        PropInstance instance = new PropInstance(definitionId, position, rotationYDegrees);
        instances.add(instance);
        return instance;
    }

    public List<PropInstance> instances() {
        return new ArrayList<>(instances);
    }

    public void clear() {
        instances.clear();
    }
}
