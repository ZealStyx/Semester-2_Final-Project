# LibGDX UI Builder — Team Plan

**Project:** Resonance (`io.github.superteam.resonance`)  
**LibGDX version:** 1.14.0  
**Java target:** 17  
**Backend:** LWJGL3  

---

## What You're Building

A **live UI editor** that runs alongside the game. The goal is to let the team design and adjust libGDX UI layouts (positions, sizes, colours, fonts, padding, etc.) without touching code or restarting the game. Changes made in the editor take effect in the running game in real time, and layouts can be saved/loaded as JSON files.

The tool has two parts:

1. **The editor panel** — a Swing or JavaFX window that runs in a separate thread alongside the LWJGL3 game window, listing all registered UI components and exposing their properties as live controls (sliders, colour pickers, text fields, checkboxes).
2. **The runtime bridge** — a small in-game system that holds references to UI components, receives property updates from the editor panel, and applies them immediately without a rebuild.

---

## Module Location

Create a new Gradle subproject called `ui-builder` inside the existing repo:

```
resonance/
├── core/
├── lwjgl3/
├── server/
├── shared/
├── ui-builder/          ← new
│   ├── build.gradle
│   └── src/main/java/io/github/superteam/resonance/uibuilder/
└── settings.gradle      ← add 'ui-builder' here
```

`ui-builder` depends on `core`. It is **never included in release builds** — it is a `debugImplementation`/`compileOnly` concern only. Add the dependency to `lwjgl3/build.gradle` gated behind a `enableUiBuilder` flag in `gradle.properties`, defaulting to `true` for dev and `false` for dist.

```groovy
// gradle.properties
enableUiBuilder=true

// lwjgl3/build.gradle
if (enableUiBuilder == 'true') {
    implementation project(':ui-builder')
}
```

---

## Architecture Overview

```
Game thread (LWJGL3)                 Editor thread (Swing)
─────────────────────                ──────────────────────
UiRegistry                           EditorPanel (JFrame)
  └─ holds Map<String, UiComponent>    └─ PropertySheet per component
       ↕  thread-safe queue                 ↕  fires PropertyChangeEvent
  UiBridge                            EditorBridge
  (polls queue each frame,              (wraps changes into
   applies to live Scene2D actors)       PropertyUpdate records,
                                         puts into ConcurrentLinkedQueue)
```

The queue is the only shared state between the two threads. No locks are held on the render thread — only `ConcurrentLinkedQueue.poll()`.

---

## Member A — Runtime Bridge (in-game side)

### Deliverables

| Class | Package | Purpose |
|-------|---------|---------|
| `UiComponent` | `uibuilder.runtime` | Wraps a Scene2D `Actor` and its metadata |
| `UiRegistry` | `uibuilder.runtime` | Singleton that holds all registered components |
| `PropertyUpdate` | `uibuilder.runtime` | Value-object: component id + property name + new value |
| `UiBridge` | `uibuilder.runtime` | Polls the update queue each frame, applies changes |
| `UiBuilderIntegration` | `uibuilder.runtime` | One-call setup: creates the Swing window, wires everything |

---

### `UiComponent.java`

```java
package io.github.superteam.resonance.uibuilder.runtime;

import com.badlogic.gdx.scenes.scene2d.Actor;

public final class UiComponent {
    public final String id;          // unique, e.g. "hud.healthBar"
    public final String displayName; // human label in editor
    public final Actor actor;        // live Scene2D actor

    public UiComponent(String id, String displayName, Actor actor) {
        this.id = id;
        this.displayName = displayName;
        this.actor = actor;
    }
}
```

---

### `UiRegistry.java`

```java
package io.github.superteam.resonance.uibuilder.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class UiRegistry {

    private static final UiRegistry INSTANCE = new UiRegistry();
    public static UiRegistry get() { return INSTANCE; }

    private final Map<String, UiComponent> components = new LinkedHashMap<>();
    // Written by the editor thread, read+drained by the game thread
    public final ConcurrentLinkedQueue<PropertyUpdate> pendingUpdates = new ConcurrentLinkedQueue<>();

    private UiRegistry() {}

    public void register(UiComponent component) {
        components.put(component.id, component);
    }

    public void unregister(String id) {
        components.remove(id);
    }

    public Collection<UiComponent> all() {
        return components.values();
    }

    public UiComponent get(String id) {
        return components.get(id);
    }

    public void clear() {
        components.clear();
        pendingUpdates.clear();
    }
}
```

---

### `PropertyUpdate.java`

```java
package io.github.superteam.resonance.uibuilder.runtime;

public final class PropertyUpdate {
    public enum Property {
        X, Y, WIDTH, HEIGHT,
        SCALE_X, SCALE_Y,
        ROTATION,
        ALPHA,
        COLOR_R, COLOR_G, COLOR_B,
        VISIBLE
    }

    public final String componentId;
    public final Property property;
    public final float value; // boolean visible = value >= 0.5f

    public PropertyUpdate(String componentId, Property property, float value) {
        this.componentId = componentId;
        this.property = property;
        this.value = value;
    }
}
```

---

### `UiBridge.java`

Call `UiBridge.applyPending()` once per frame from inside your game's `render()` loop (on the game thread only).

```java
package io.github.superteam.resonance.uibuilder.runtime;

import com.badlogic.gdx.scenes.scene2d.Actor;

public final class UiBridge {

    private final UiRegistry registry;

    public UiBridge(UiRegistry registry) {
        this.registry = registry;
    }

    /** Call this once per frame on the game/render thread. */
    public void applyPending() {
        PropertyUpdate update;
        while ((update = registry.pendingUpdates.poll()) != null) {
            UiComponent comp = registry.get(update.componentId);
            if (comp == null) continue;
            applyToActor(comp.actor, update);
        }
    }

    private void applyToActor(Actor actor, PropertyUpdate u) {
        switch (u.property) {
            case X         -> actor.setX(u.value);
            case Y         -> actor.setY(u.value);
            case WIDTH     -> actor.setWidth(u.value);
            case HEIGHT    -> actor.setHeight(u.value);
            case SCALE_X   -> actor.setScaleX(u.value);
            case SCALE_Y   -> actor.setScaleY(u.value);
            case ROTATION  -> actor.setRotation(u.value);
            case ALPHA     -> actor.getColor().a = u.value;
            case COLOR_R   -> actor.getColor().r = u.value;
            case COLOR_G   -> actor.getColor().g = u.value;
            case COLOR_B   -> actor.getColor().b = u.value;
            case VISIBLE   -> actor.setVisible(u.value >= 0.5f);
        }
    }
}
```

---

### `UiBuilderIntegration.java`

This is the entry point the game calls to enable the editor. It should be called once, early in the game's `create()` or scene `show()` method.

```java
package io.github.superteam.resonance.uibuilder.runtime;

public final class UiBuilderIntegration {

    private static UiBridge bridge;
    private static boolean started = false;

    /**
     * Call once on the game thread (e.g. in create() or show()).
     * Launches the Swing editor window on its own thread.
     */
    public static UiBridge start() {
        if (started) return bridge;
        started = true;

        UiRegistry registry = UiRegistry.get();
        bridge = new UiBridge(registry);

        // Launch Swing on the EDT — never on the game thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Member B implements EditorPanel
            io.github.superteam.resonance.uibuilder.editor.EditorPanel panel =
                new io.github.superteam.resonance.uibuilder.editor.EditorPanel(registry);
            panel.show();
        });

        return bridge;
    }

    public static void stop() {
        started = false;
        bridge = null;
        UiRegistry.get().clear();
    }
}
```

---

### How to wire it into the game

In `UniversalTestScene.java` (or wherever Scene2D actors are created), add at the end of the constructor or `show()`:

```java
// --- UI BUILDER (dev only) ---
import io.github.superteam.resonance.uibuilder.runtime.*;

// After creating actors, register them:
UiRegistry reg = UiRegistry.get();
reg.register(new UiComponent("hud.healthBar", "Health Bar", healthBarActor));
reg.register(new UiComponent("hud.staminaBar", "Stamina Bar", staminaBarActor));
// ...add as many as needed

uiBridge = UiBuilderIntegration.start(); // store reference in scene field
```

Then in `render()`, before drawing:

```java
if (uiBridge != null) uiBridge.applyPending();
```

And in `dispose()`:

```java
UiBuilderIntegration.stop();
```

---

### Member A — Acceptance Criteria

- [ ] `UiRegistry.register()` and `UiRegistry.all()` work correctly
- [ ] `UiBridge.applyPending()` drains the queue and applies all `PropertyUpdate` types to a live actor without throwing on the game thread
- [ ] No synchronization primitives other than `ConcurrentLinkedQueue` are used
- [ ] `UiBuilderIntegration.start()` launches the Swing window without blocking the game thread
- [ ] Tested manually: register a dummy actor, push a `PropertyUpdate` onto the queue from a test, verify the actor's property changes on the next `applyPending()` call

---

## Member B — Editor Panel (Swing window)

### Deliverables

| Class | Package | Purpose |
|-------|---------|---------|
| `EditorPanel` | `uibuilder.editor` | Main JFrame window |
| `ComponentListPanel` | `uibuilder.editor` | Left panel: tree/list of registered components |
| `PropertySheetPanel` | `uibuilder.editor` | Right panel: sliders/inputs for selected component |
| `ColorPickerField` | `uibuilder.editor` | Inline R/G/B colour picker widget |
| `LayoutSerializer` | `uibuilder.editor` | Save/load component property snapshots to JSON |

---

### `EditorPanel.java` — Main window

```java
package io.github.superteam.resonance.uibuilder.editor;

import io.github.superteam.resonance.uibuilder.runtime.UiRegistry;

import javax.swing.*;
import java.awt.*;

public final class EditorPanel {

    private final UiRegistry registry;
    private JFrame frame;
    private ComponentListPanel listPanel;
    private PropertySheetPanel sheetPanel;

    public EditorPanel(UiRegistry registry) {
        this.registry = registry;
    }

    /** Call on the Swing EDT only. */
    public void show() {
        frame = new JFrame("UI Builder — Resonance");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(520, 700);
        frame.setLayout(new BorderLayout());

        listPanel  = new ComponentListPanel(registry, this::onComponentSelected);
        sheetPanel = new PropertySheetPanel(registry);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, sheetPanel);
        split.setDividerLocation(180);

        JToolBar toolbar = buildToolbar();
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);

        // Refresh the component list every second (components may register after launch)
        Timer refreshTimer = new Timer(1000, e -> listPanel.refresh());
        refreshTimer.start();

        frame.setVisible(true);
    }

    private JToolBar buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton saveBtn = new JButton("Save Layout");
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                LayoutSerializer.save(registry, fc.getSelectedFile());
            }
        });

        JButton loadBtn = new JButton("Load Layout");
        loadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                LayoutSerializer.load(registry, fc.getSelectedFile());
            }
        });

        bar.add(saveBtn);
        bar.add(loadBtn);
        return bar;
    }

    private void onComponentSelected(String componentId) {
        sheetPanel.showComponent(componentId);
    }
}
```

---

### `ComponentListPanel.java`

Displays a `JList` of all registered component display names. Calls back with the component ID when one is selected. Has a `refresh()` method that repopulates the list from `UiRegistry.all()` without clearing the selection if the same item is still present.

```java
package io.github.superteam.resonance.uibuilder.editor;

import io.github.superteam.resonance.uibuilder.runtime.UiComponent;
import io.github.superteam.resonance.uibuilder.runtime.UiRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class ComponentListPanel extends JPanel {

    private final UiRegistry registry;
    private final Consumer<String> onSelect;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> jList = new JList<>(listModel);
    // Maps display index → component id
    private final java.util.List<String> idOrder = new java.util.ArrayList<>();

    public ComponentListPanel(UiRegistry registry, Consumer<String> onSelect) {
        this.registry = registry;
        this.onSelect = onSelect;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(180, 0));
        setBorder(BorderFactory.createTitledBorder("Components"));

        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jList.getSelectedIndex() >= 0) {
                onSelect.accept(idOrder.get(jList.getSelectedIndex()));
            }
        });

        add(new JScrollPane(jList), BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        String previouslySelectedId = jList.getSelectedIndex() >= 0
            ? idOrder.get(jList.getSelectedIndex()) : null;

        listModel.clear();
        idOrder.clear();
        for (UiComponent comp : registry.all()) {
            listModel.addElement(comp.displayName);
            idOrder.add(comp.id);
        }

        if (previouslySelectedId != null) {
            int idx = idOrder.indexOf(previouslySelectedId);
            if (idx >= 0) jList.setSelectedIndex(idx);
        }
    }
}
```

---

### `PropertySheetPanel.java`

This is the main work. When a component is selected, read the actor's current property values and populate a panel of controls. Each control, on change, pushes a `PropertyUpdate` to `UiRegistry.pendingUpdates`.

Structure the panel as a `GridBagLayout` or `GroupLayout` with labelled rows:

```
Label       Control
─────────   ──────────────────────────────────
X           [slider -2000 to 2000] [spinner]
Y           [slider -2000 to 2000] [spinner]
Width       [slider 0 to 2000]    [spinner]
Height      [slider 0 to 2000]    [spinner]
Scale X     [slider 0.0 to 5.0]   [spinner]
Scale Y     [slider 0.0 to 5.0]   [spinner]
Rotation    [slider -360 to 360]  [spinner]
Alpha       [slider 0.0 to 1.0]   [spinner]
Color       [R slider] [G slider] [B slider]
Visible     [checkbox]
```

**Important implementation detail:** sliders and spinners must stay in sync with each other. Use a shared `ChangeListener` that updates both controls when either fires. Gate the update so that programmatic changes to one don't cause the other to fire a second `PropertyUpdate`.

```java
package io.github.superteam.resonance.uibuilder.editor;

import io.github.superteam.resonance.uibuilder.runtime.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

public final class PropertySheetPanel extends JPanel {

    private final UiRegistry registry;
    private String currentId;
    private boolean updating = false; // guard flag: stops listener re-entrancy

    public PropertySheetPanel(UiRegistry registry) {
        this.registry = registry;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Properties"));
        showEmpty();
    }

    public void showComponent(String componentId) {
        currentId = componentId;
        UiComponent comp = registry.get(componentId);
        if (comp == null) { showEmpty(); return; }

        removeAll();
        Actor actor = comp.actor;
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        row = addFloatRow(grid, c, row, "X",        actor.getX(),        -2000, 2000, PropertyUpdate.Property.X);
        row = addFloatRow(grid, c, row, "Y",        actor.getY(),        -2000, 2000, PropertyUpdate.Property.Y);
        row = addFloatRow(grid, c, row, "Width",    actor.getWidth(),        0, 2000, PropertyUpdate.Property.WIDTH);
        row = addFloatRow(grid, c, row, "Height",   actor.getHeight(),       0, 2000, PropertyUpdate.Property.HEIGHT);
        row = addFloatRow(grid, c, row, "Scale X",  actor.getScaleX(),    0.0f,  5.0f, PropertyUpdate.Property.SCALE_X);
        row = addFloatRow(grid, c, row, "Scale Y",  actor.getScaleY(),    0.0f,  5.0f, PropertyUpdate.Property.SCALE_Y);
        row = addFloatRow(grid, c, row, "Rotation", actor.getRotation(), -360,   360,  PropertyUpdate.Property.ROTATION);
        row = addFloatRow(grid, c, row, "Alpha",    actor.getColor().a,   0.0f,  1.0f, PropertyUpdate.Property.ALPHA);
        row = addFloatRow(grid, c, row, "Color R",  actor.getColor().r,   0.0f,  1.0f, PropertyUpdate.Property.COLOR_R);
        row = addFloatRow(grid, c, row, "Color G",  actor.getColor().g,   0.0f,  1.0f, PropertyUpdate.Property.COLOR_G);
        row = addFloatRow(grid, c, row, "Color B",  actor.getColor().b,   0.0f,  1.0f, PropertyUpdate.Property.COLOR_B);
        row = addBoolRow(grid, c, row,  "Visible",  actor.isVisible(),              PropertyUpdate.Property.VISIBLE);

        add(new JScrollPane(grid), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showEmpty() {
        removeAll();
        add(new JLabel("Select a component", SwingConstants.CENTER), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private int addFloatRow(JPanel grid, GridBagConstraints c, int row,
                             String label, float initial, float min, float max,
                             PropertyUpdate.Property property) {
        int steps = 1000;
        float range = max - min;

        JSlider slider = new JSlider(0, steps, toSliderVal(initial, min, range, steps));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
            (double) initial, (double) min, (double) max, 0.1));

        ChangeListener onSlider = e -> {
            if (updating) return;
            updating = true;
            float val = fromSliderVal(slider.getValue(), min, range, steps);
            spinner.setValue((double) val);
            push(property, val);
            updating = false;
        };

        ChangeListener onSpinner = e -> {
            if (updating) return;
            updating = true;
            float val = ((Number) spinner.getValue()).floatValue();
            slider.setValue(toSliderVal(val, min, range, steps));
            push(property, val);
            updating = false;
        };

        slider.addChangeListener(onSlider);
        spinner.addChangeListener(onSpinner);

        c.gridy = row; c.gridx = 0; c.weightx = 0.0;
        grid.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 0.7;
        grid.add(slider, c);
        c.gridx = 2; c.weightx = 0.3;
        grid.add(spinner, c);

        return row + 1;
    }

    private int addBoolRow(JPanel grid, GridBagConstraints c, int row,
                            String label, boolean initial,
                            PropertyUpdate.Property property) {
        JCheckBox box = new JCheckBox();
        box.setSelected(initial);
        box.addActionListener(e -> push(property, box.isSelected() ? 1f : 0f));

        c.gridy = row; c.gridx = 0; c.weightx = 0.0;
        grid.add(new JLabel(label), c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1.0;
        grid.add(box, c);
        c.gridwidth = 1;

        return row + 1;
    }

    private void push(PropertyUpdate.Property property, float value) {
        if (currentId != null) {
            registry.pendingUpdates.add(new PropertyUpdate(currentId, property, value));
        }
    }

    private int toSliderVal(float v, float min, float range, int steps) {
        if (range == 0) return 0;
        return Math.round(((v - min) / range) * steps);
    }

    private float fromSliderVal(int sliderVal, float min, float range, int steps) {
        return min + (sliderVal / (float) steps) * range;
    }
}
```

---

### `LayoutSerializer.java`

Saves the current actor state for all registered components to a JSON file, and loads it back by pushing `PropertyUpdate` records onto the queue.

Use only `java.util` — no external JSON libraries. Write minimal hand-rolled JSON since the schema is flat.

```java
package io.github.superteam.resonance.uibuilder.editor;

import io.github.superteam.resonance.uibuilder.runtime.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class LayoutSerializer {

    private LayoutSerializer() {}

    public static void save(UiRegistry registry, File file) {
        StringBuilder sb = new StringBuilder("{\n");
        boolean firstComp = true;
        for (UiComponent comp : registry.all()) {
            if (!firstComp) sb.append(",\n");
            firstComp = false;
            Actor a = comp.actor;
            sb.append("  \"").append(comp.id).append("\": {\n");
            sb.append("    \"x\": ").append(a.getX()).append(",\n");
            sb.append("    \"y\": ").append(a.getY()).append(",\n");
            sb.append("    \"width\": ").append(a.getWidth()).append(",\n");
            sb.append("    \"height\": ").append(a.getHeight()).append(",\n");
            sb.append("    \"scaleX\": ").append(a.getScaleX()).append(",\n");
            sb.append("    \"scaleY\": ").append(a.getScaleY()).append(",\n");
            sb.append("    \"rotation\": ").append(a.getRotation()).append(",\n");
            sb.append("    \"alpha\": ").append(a.getColor().a).append(",\n");
            sb.append("    \"colorR\": ").append(a.getColor().r).append(",\n");
            sb.append("    \"colorG\": ").append(a.getColor().g).append(",\n");
            sb.append("    \"colorB\": ").append(a.getColor().b).append(",\n");
            sb.append("    \"visible\": ").append(a.isVisible()).append("\n");
            sb.append("  }");
        }
        sb.append("\n}");
        try {
            Files.writeString(file.toPath(), sb.toString());
        } catch (IOException e) {
            System.err.println("UI Builder: failed to save layout: " + e.getMessage());
        }
    }

    public static void load(UiRegistry registry, File file) {
        // Minimal JSON parse — reads key:value pairs per component block
        try {
            String json = Files.readString(file.toPath());
            // Split by component blocks: find "id": { ... }
            // Simple line-by-line parser — assumes the format written by save()
            String currentId = null;
            Map<String, Float> props = new LinkedHashMap<>();

            for (String raw : json.split("\n")) {
                String line = raw.trim();
                if (line.startsWith("\"") && line.endsWith("{")) {
                    currentId = line.replace("\"", "").replace(":", "").replace("{", "").trim();
                    props.clear();
                } else if (line.startsWith("}") && currentId != null) {
                    pushProps(registry, currentId, props);
                    currentId = null;
                } else if (currentId != null && line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].replace("\"", "").trim();
                    String val = parts[1].replace(",", "").trim();
                    try {
                        props.put(key, Float.parseFloat(val));
                    } catch (NumberFormatException ignored) {
                        // boolean
                        props.put(key, "true".equals(val) ? 1f : 0f);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("UI Builder: failed to load layout: " + e.getMessage());
        }
    }

    private static void pushProps(UiRegistry registry, String id, Map<String, Float> props) {
        var q = registry.pendingUpdates;
        push(q, id, "x",        PropertyUpdate.Property.X,        props);
        push(q, id, "y",        PropertyUpdate.Property.Y,        props);
        push(q, id, "width",    PropertyUpdate.Property.WIDTH,     props);
        push(q, id, "height",   PropertyUpdate.Property.HEIGHT,    props);
        push(q, id, "scaleX",   PropertyUpdate.Property.SCALE_X,   props);
        push(q, id, "scaleY",   PropertyUpdate.Property.SCALE_Y,   props);
        push(q, id, "rotation", PropertyUpdate.Property.ROTATION,  props);
        push(q, id, "alpha",    PropertyUpdate.Property.ALPHA,     props);
        push(q, id, "colorR",   PropertyUpdate.Property.COLOR_R,   props);
        push(q, id, "colorG",   PropertyUpdate.Property.COLOR_G,   props);
        push(q, id, "colorB",   PropertyUpdate.Property.COLOR_B,   props);
        push(q, id, "visible",  PropertyUpdate.Property.VISIBLE,   props);
    }

    private static void push(java.util.Queue<PropertyUpdate> q, String id,
                              String key, PropertyUpdate.Property prop,
                              Map<String, Float> props) {
        Float v = props.get(key);
        if (v != null) q.add(new PropertyUpdate(id, prop, v));
    }
}
```

---

### Member B — Acceptance Criteria

- [ ] Swing window opens alongside the game without freezing or crashing the game thread
- [ ] Component list populates within 1 second of actors being registered
- [ ] Adjusting a slider or spinner pushes exactly one `PropertyUpdate` per drag tick — not two (no re-entrant listener firing)
- [ ] Selecting a different component immediately re-renders the property sheet with that component's current values
- [ ] Save layout writes a valid JSON file; loading it pushes updates for every property of every component
- [ ] Window closing does not crash the game (use `HIDE_ON_CLOSE` not `DISPOSE_ON_CLOSE`)

---

## Gradle Setup

### `ui-builder/build.gradle`

```groovy
[compileJava]*.options*.encoding = 'UTF-8'
eclipse.project.name = appName + '-ui-builder'
java.sourceCompatibility = 17
java.targetCompatibility = 17

dependencies {
    implementation project(':core')
    // Swing is part of the JDK — no extra dependency needed
}
```

### `settings.gradle` — add one line

```groovy
include 'ui-builder'
```

---

## Thread Safety Rules — Read These Before Writing Any Code

1. **Never call `Gdx.*` from the Swing thread.** The libGDX context is owned by the game thread. Calling `Gdx.app`, `Gdx.gl`, `Gdx.input`, etc. from the EDT will crash or corrupt state.
2. **Never call Swing APIs from the game thread.** All Swing calls must go through `SwingUtilities.invokeLater()`.
3. **The only bridge between threads is `UiRegistry.pendingUpdates`.** It is a `ConcurrentLinkedQueue` — safe for one producer (Swing) and one consumer (game thread) without any extra locking.
4. **Do not read actor properties from the Swing thread.** They are only safe to read on the game thread. The initial values shown in `PropertySheetPanel.showComponent()` are a snapshot taken when the user clicks a component. They may be stale by milliseconds — that is acceptable.
5. **Do not hold a reference to `Actor` in any Swing class.** Pass values only. The only place actors are touched is inside `UiBridge.applyPending()`, which runs on the game thread.

---

## Workflow for Designers Using the Tool

1. Run the game normally via `./gradlew lwjgl3:run`
2. The editor window opens automatically alongside the game window
3. Click a component in the left list
4. Adjust any property — the game updates in real time with no restart
5. When happy with the layout, click **Save Layout** and save as e.g. `hud_layout.json`
6. The saved JSON can be committed to the repo; the game can optionally load it at startup via `LayoutSerializer.load()`

---

## Out of Scope (for now)

- Scene2D Table/Layout constraints — only free Actor properties for now
- Undo/redo
- Multi-select
- Text/font editing (BitmapFont sizing is separate — add later)
- Android/iOS backend — this tool is LWJGL3 desktop only

---

## File Checklist

**Member A creates:**
- [ ] `ui-builder/build.gradle`
- [ ] `UiComponent.java`
- [ ] `UiRegistry.java`
- [ ] `PropertyUpdate.java`
- [ ] `UiBridge.java`
- [ ] `UiBuilderIntegration.java`
- [ ] Wire into `UniversalTestScene.java` (register actors, call `start()`, call `applyPending()`)
- [ ] Update `settings.gradle`
- [ ] Update `gradle.properties` with `enableUiBuilder=true`
- [ ] Update `lwjgl3/build.gradle` with conditional dependency

**Member B creates:**
- [ ] `EditorPanel.java`
- [ ] `ComponentListPanel.java`
- [ ] `PropertySheetPanel.java`
- [ ] `LayoutSerializer.java`
- [ ] (Optional) `ColorPickerField.java` — consolidated R/G/B with a colour preview swatch

**Integration test (both members):**
- [ ] Run game, confirm editor window appears
- [ ] Move a slider, confirm actor moves in game immediately
- [ ] Save layout, reload layout, confirm properties restore correctly
