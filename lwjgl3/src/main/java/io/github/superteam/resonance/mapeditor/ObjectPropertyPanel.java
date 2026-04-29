package io.github.superteam.resonance.mapeditor;

import io.github.superteam.resonance.prop.PropDefinition;
import io.github.superteam.resonance.prop.PropInstance;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class ObjectPropertyPanel extends JPanel {
    private final JLabel titleValue = new JLabel("-");
    private final JLabel modelValue = new JLabel("-");
    private final JLabel positionValue = new JLabel("-");
    private final JLabel rotationValue = new JLabel("-");

    public ObjectPropertyPanel() {
        super(new GridLayout(0, 1, 4, 4));
        setBorder(BorderFactory.createTitledBorder("Object Properties"));
        add(new JLabel("Name"));
        add(titleValue);
        add(new JLabel("Model"));
        add(modelValue);
        add(new JLabel("Position"));
        add(positionValue);
        add(new JLabel("Rotation"));
        add(rotationValue);
    }

    public void showDefinition(PropDefinition definition) {
        if (definition == null) {
            clear();
            return;
        }
        titleValue.setText(definition.displayName);
        modelValue.setText(definition.modelPath);
        positionValue.setText("(definition)");
        rotationValue.setText(definition.category + " / " + definition.behavior);
    }

    public void showInstance(PropInstance instance) {
        if (instance == null) {
            clear();
            return;
        }
        titleValue.setText(instance.definitionId);
        modelValue.setText("instance");
        positionValue.setText(String.format("%.2f, %.2f, %.2f", instance.position.x, instance.position.y, instance.position.z));
        rotationValue.setText(String.format("%.1f°", instance.rotationYDegrees));
    }

    public void clear() {
        titleValue.setText("-");
        modelValue.setText("-");
        positionValue.setText("-");
        rotationValue.setText("-");
    }
}
