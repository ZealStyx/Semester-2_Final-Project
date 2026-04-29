package io.github.superteam.resonance.mapeditor;

import io.github.superteam.resonance.prop.PropDefinitionRegistry;
import io.github.superteam.resonance.prop.PropInstance;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public final class MapEditorPanel extends JPanel {
    private final ObjectPalette objectPalette = new ObjectPalette();
    private final SceneOutlinePanel sceneOutlinePanel = new SceneOutlinePanel();
    private final ObjectPropertyPanel objectPropertyPanel = new ObjectPropertyPanel();
    private final PropLibraryPanel propLibraryPanel = new PropLibraryPanel();

    public MapEditorPanel() {
        super(new BorderLayout(8, 8));
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("Outline", sceneOutlinePanel);
        rightTabs.addTab("Properties", objectPropertyPanel);
        rightTabs.addTab("Library", propLibraryPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objectPalette, rightTabs);
        splitPane.setResizeWeight(0.30);
        add(splitPane, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public void setDefinitions(PropDefinitionRegistry registry) {
        objectPalette.setDefinitions(registry);
        propLibraryPanel.setDefinitions(registry);
    }

    public void setInstances(List<PropInstance> instances) {
        sceneOutlinePanel.setInstances(instances);
    }

    public ObjectPropertyPanel objectPropertyPanel() {
        return objectPropertyPanel;
    }

    public ObjectPalette objectPalette() {
        return objectPalette;
    }

    public PropLibraryPanel propLibraryPanel() {
        return propLibraryPanel;
    }
}
