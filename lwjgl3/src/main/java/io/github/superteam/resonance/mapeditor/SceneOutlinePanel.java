package io.github.superteam.resonance.mapeditor;

import io.github.superteam.resonance.prop.PropInstance;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class SceneOutlinePanel extends JPanel {
    private final DefaultListModel<PropInstance> model = new DefaultListModel<>();
    private final JList<PropInstance> list = new JList<>(model);

    public SceneOutlinePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Scene Outline"));
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setInstances(List<PropInstance> instances) {
        model.clear();
        if (instances == null) {
            return;
        }
        for (PropInstance instance : instances) {
            model.addElement(instance);
        }
    }

    public PropInstance selectedInstance() {
        return list.getSelectedValue();
    }
}
