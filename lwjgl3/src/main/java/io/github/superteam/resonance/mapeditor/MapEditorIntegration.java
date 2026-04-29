package io.github.superteam.resonance.mapeditor;

import io.github.superteam.resonance.prop.PropDefinitionRegistry;
import io.github.superteam.resonance.prop.PropInstance;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class MapEditorIntegration {
    private final MapEditorPanel panel = new MapEditorPanel();
    private JFrame frame;

    public MapEditorPanel panel() {
        return panel;
    }

    public void setDefinitions(PropDefinitionRegistry registry) {
        panel.setDefinitions(registry);
    }

    public void setInstances(List<PropInstance> instances) {
        panel.setInstances(instances);
    }

    public void showWindow() {
        if (frame == null) {
            frame = new JFrame("Resonance Map Editor");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.setPreferredSize(new Dimension(1280, 800));
            frame.pack();
        }
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
