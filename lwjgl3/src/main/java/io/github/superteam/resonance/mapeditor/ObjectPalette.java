package io.github.superteam.resonance.mapeditor;

import io.github.superteam.resonance.prop.PropDefinition;
import io.github.superteam.resonance.prop.PropDefinitionRegistry;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public final class ObjectPalette extends JPanel {
    private final DefaultListModel<PropDefinition> model = new DefaultListModel<>();
    private final JList<PropDefinition> list = new JList<>(model);

    public ObjectPalette() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Object Palette"));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setDefinitions(PropDefinitionRegistry registry) {
        model.clear();
        if (registry == null) {
            return;
        }
        List<PropDefinition> definitions = registry.all();
        for (PropDefinition definition : definitions) {
            model.addElement(definition);
        }
    }

    public PropDefinition selectedDefinition() {
        return list.getSelectedValue();
    }
}
