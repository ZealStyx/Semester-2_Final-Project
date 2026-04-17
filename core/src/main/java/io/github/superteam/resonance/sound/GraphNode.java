package io.github.superteam.resonance.sound;

import com.badlogic.gdx.math.Vector3;
import java.util.Objects;

/**
 * Immutable graph node used by the acoustic propagation graph.
 */
public record GraphNode(String id, Vector3 position) {
    public GraphNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Graph node id must not be blank.");
        }
        Objects.requireNonNull(position, "Graph node position must not be null.");
        position = new Vector3(position);
    }

    @Override
    public Vector3 position() {
        return new Vector3(position);
    }
}
