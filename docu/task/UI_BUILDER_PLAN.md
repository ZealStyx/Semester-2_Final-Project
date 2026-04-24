# LibGDX UI Builder — Team Plan

**Project:** Resonance (`io.github.superteam.resonance`)  
**LibGDX version:** 1.14.0  
**Java target:** 17  
**Backend:** LWJGL3  
**Assignees:** 2 members (Member A and Member B — split defined below)

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

## ⚠️ Using the UI Builder from `core/screen/MainMenuScreen.java`

### The circular dependency problem

`ui-builder` depends on `core`, so **`core` cannot import anything from `ui-builder`**. If `MainMenuScreen.java` tried to do this:

```java
// ❌ This will NOT compile — circular dependency
import io.github.superteam.resonance.uibuilder.runtime.UiBuilderIntegration;
```

the build will fail. `core` does not know `ui-builder` exists.

### The solution — `UiRegistrar` interface (defined in `core`)

The fix is a **dependency inversion**: define a tiny interface in `core` that `MainMenuScreen` can use without knowing anything about `ui-builder`. The `lwjgl3` launcher (which can see both modules) creates the real implementation and injects it into the screen.

---

### Step 1 — Create the interface in `core`

```
core/src/main/java/io/github/superteam/resonance/ui/UiRegistrar.java
```

```java
package io.github.superteam.resonance.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Thin interface so core screens can register their actors with the
 * UI Builder without depending on the ui-builder module directly.
 *
 * In release builds, pass a no-op implementation.
 * In dev builds, pass the real UiRegistry-backed implementation from lwjgl3.
 */
public interface UiRegistrar {

    /** Register an actor so it appears in the UI Builder editor panel. */
    void register(String id, String displayName, Actor actor);

    /** Unregister all actors registered by this screen (call from hide() or dispose()). */
    void unregisterAll();

    /** A do-nothing implementation safe to use in release builds or tests. */
    UiRegistrar NOOP = new UiRegistrar() {
        @Override public void register(String id, String displayName, Actor actor) {}
        @Override public void unregisterAll() {}
    };
}
```

---

### Step 2 — Use it in `MainMenuScreen.java`

```java
package io.github.superteam.resonance.core.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import io.github.superteam.resonance.ui.UiRegistrar;

public class MainMenuScreen implements Screen {

    private final Stage stage;
    private final UiRegistrar uiRegistrar;  // injected — never null

    private Image logo;
    private TextButton playButton;
    private TextButton settingsButton;

    /** Production constructor — no UI builder. */
    public MainMenuScreen(Stage stage) {
        this(stage, UiRegistrar.NOOP);
    }

    /** Dev constructor — receives the live registrar from the lwjgl3 launcher. */
    public MainMenuScreen(Stage stage, UiRegistrar uiRegistrar) {
        this.stage = stage;
        this.uiRegistrar = uiRegistrar;
    }

    @Override
    public void show() {
        // --- Build your actors normally ---
        logo           = new Image(/* your logo drawable */);
        playButton     = new TextButton("Play",     /* skin */);
        settingsButton = new TextButton("Settings", /* skin */);

        logo.setPosition(200, 400);
        playButton.setPosition(200, 250);
        settingsButton.setPosition(200, 150);

        stage.addActor(logo);
        stage.addActor(playButton);
        stage.addActor(settingsButton);

        // --- Register with the UI Builder (no-op in release builds) ---
        uiRegistrar.register("mainmenu.logo",           "Logo",            logo);
        uiRegistrar.register("mainmenu.playButton",     "Play Button",     playButton);
        uiRegistrar.register("mainmenu.settingsButton", "Settings Button", settingsButton);
    }

    @Override
    public void render(float delta) {
        // UiBridge.applyPending() is called by the launcher — not here
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide() {
        uiRegistrar.unregisterAll(); // clean up when leaving this screen
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); }
}
```

---

### Step 3 — Create the real `UiRegistrar` implementation in `ui-builder`

```
ui-builder/src/main/java/io/github/superteam/resonance/uibuilder/runtime/LiveUiRegistrar.java
```

```java
package io.github.superteam.resonance.uibuilder.runtime;

import com.badlogic.gdx.scenes.scene2d.Actor;
import io.github.superteam.resonance.ui.UiRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Real UiRegistrar implementation — lives in ui-builder, not in core.
 * Delegates to UiRegistry and tracks which IDs were registered so
 * unregisterAll() can clean them up when the screen hides.
 */
public final class LiveUiRegistrar implements UiRegistrar {

    private final UiRegistry registry;
    private final List<String> registeredIds = new ArrayList<>();

    public LiveUiRegistrar(UiRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void register(String id, String displayName, Actor actor) {
        registry.register(new UiComponent(id, displayName, actor));
        registeredIds.add(id);
    }

    @Override
    public void unregisterAll() {
        registeredIds.forEach(registry::unregister);
        registeredIds.clear();
    }
}
```

---

### Step 4 — Wire it all together in the `lwjgl3` launcher

This is the only place that can see both `core` and `ui-builder`. It creates a `LiveUiRegistrar`, gives it to the screen, and runs the per-frame bridge.

```java
package io.github.superteam.resonance.lwjgl3;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import io.github.superteam.resonance.core.screen.MainMenuScreen;
import io.github.superteam.resonance.uibuilder.runtime.*;

public class ResonanceGame extends Game {

    private UiBridge uiBridge;

    @Override
    public void create() {
        Stage stage = new Stage();

        // --- Dev-only UI Builder setup ---
        UiRegistry registry = UiRegistry.get();
        LiveUiRegistrar registrar = new LiveUiRegistrar(registry);
        uiBridge = UiBuilderIntegration.start(); // opens the Swing editor window

        // Pass the registrar into the screen
        setScreen(new MainMenuScreen(stage, registrar));
    }

    @Override
    public void render() {
        // Apply any pending UI Builder changes before drawing
        if (uiBridge != null) uiBridge.applyPending();
        super.render(); // delegates to current screen's render()
    }

    @Override
    public void dispose() {
        UiBuilderIntegration.stop();
        super.dispose();
    }
}
```

---

### How it all fits together — data flow diagram

```
lwjgl3 / ResonanceGame.create()
    │
    ├─ creates LiveUiRegistrar (knows about ui-builder)
    ├─ calls UiBuilderIntegration.start()  →  opens Swing editor window
    └─ passes registrar into MainMenuScreen(stage, registrar)
                │
                └─ MainMenuScreen.show()
                        │  creates logo, playButton, settingsButton
                        └─ uiRegistrar.register("mainmenu.logo", ...)
                                │
                                └─ LiveUiRegistrar.register()
                                        └─ UiRegistry.register(UiComponent)
                                                │
                                                ▼
                                     [appears in Swing editor list]
                                                │
                                    designer moves a slider
                                                │
                                     PropertyUpdate → ConcurrentLinkedQueue
                                                │
                              next frame: ResonanceGame.render()
                                                │
                                     uiBridge.applyPending()
                                                │
                                     actor.setX() / setY() / etc.
                                                │
                                     [actor moves in game instantly]
```

---

### Switching screens cleanly

When the player moves from `MainMenuScreen` to e.g. `GameScreen`, the old actors must be unregistered so the editor list stays accurate:

```
MainMenuScreen.hide()
    └─ uiRegistrar.unregisterAll()
            └─ UiRegistry.unregister("mainmenu.logo")
            └─ UiRegistry.unregister("mainmenu.playButton")
            └─ UiRegistry.unregister("mainmenu.settingsButton")
```

Each screen that wants UI Builder support gets its own `LiveUiRegistrar` instance (or shares one per-screen). The launcher creates and passes it in — the screen itself stays ignorant of `ui-builder`.

---

### Release builds — zero overhead

When `enableUiBuilder=false` in `gradle.properties`, the `ui-builder` module is not on the classpath at all. The launcher uses the no-arg `MainMenuScreen(stage)` constructor, which defaults to `UiRegistrar.NOOP`. Every `uiRegistrar.register(...)` call is a one-line no-op — the JIT will inline and eliminate it entirely.

---

## Member A — Runtime Bridge (in-game side)

### Deliverables

| Class | Package | Purpose |
|-------|---------|---------|
| `UiRegistrar` *(interface)* | `core` → `ui` | Thin interface so core screens can register actors |
| `UiComponent` | `uibuilder.runtime` | Wraps a Scene2D `Actor` and its metadata |
| `UiRegistry` | `uibuilder.runtime` | Singleton that holds all registered components |
| `PropertyUpdate` | `uibuilder.runtime` | Value-object: component id + property name + new value |
| `UiBridge` | `uibuilder.runtime` | Polls the update queue each frame, applies changes |
| `LiveUiRegistrar` | `uibuilder.runtime` | Implements `UiRegistrar` using the real `UiRegistry` |
| `UiBuilderIntegration` | `uibuilder.runtime` | One-call setup: creates the Swing window, wires everything |

---

### `UiComponent.java`

```java
package io.github.superteam.resonance.uibuilder.runtime;

import com.badlogic.gdx.scenes.scene2d.Actor;

public final class UiComponent {
    public final String id;          // unique, e.g. "mainmenu.playButton"
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

Call `UiBridge.applyPending()` once per frame from inside the `lwjgl3` game's `render()` loop (on the game thread only).

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

```java
package io.github.superteam.resonance.uibuilder.runtime;

public final class UiBuilderIntegration {

    private static UiBridge bridge;
    private static boolean started = false;

    /**
     * Call once on the game thread (e.g. in create()).
     * Launches the Swing editor window on its own thread.
     */
    public static UiBridge start() {
        if (started) return bridge;
        started = true;

        UiRegistry registry = UiRegistry.get();
        bridge = new UiBridge(registry);

        // Launch Swing on the EDT — never on the game thread
        javax.swing.SwingUtilities.invokeLater(() -> {
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

### Member A — Acceptance Criteria

- [ ] `UiRegistrar` interface created in `core` under package `ui`
- [ ] `LiveUiRegistrar` created in `ui-builder`, implements `UiRegistrar`, delegates to `UiRegistry`
- [ ] `UiRegistry.register()` and `UiRegistry.all()` work correctly
- [ ] `UiBridge.applyPending()` drains the queue and applies all `PropertyUpdate` types to a live actor without throwing on the game thread
- [ ] No synchronization primitives other than `ConcurrentLinkedQueue` are used
- [ ] `UiBuilderIntegration.start()` launches the Swing window without blocking the game thread
- [ ] Tested manually: register a dummy actor via `LiveUiRegistrar`, push a `PropertyUpdate` onto the queue, verify the actor's property changes on the next `applyPending()` call

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
    private boolean updating = false;

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
        try {
            String json = Files.readString(file.toPath());
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
3. Click a component in the left list (e.g. "Play Button", "Logo")
4. Adjust any property — the game updates in real time with no restart
5. When happy with the layout, click **Save Layout** and save as e.g. `mainmenu_layout.json`
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
- [ ] `core/src/.../ui/UiRegistrar.java` ← **new interface, lives in core**
- [ ] `ui-builder/build.gradle`
- [ ] `UiComponent.java`
- [ ] `UiRegistry.java`
- [ ] `PropertyUpdate.java`
- [ ] `UiBridge.java`
- [ ] `LiveUiRegistrar.java` ← **new, implements UiRegistrar**
- [ ] `UiBuilderIntegration.java`
- [ ] Wire into `lwjgl3` launcher (`ResonanceGame.java`): create `LiveUiRegistrar`, pass to screen, call `start()`, call `applyPending()` in render
- [ ] Update `settings.gradle`
- [ ] Update `gradle.properties` with `enableUiBuilder=true`
- [ ] Update `lwjgl3/build.gradle` with conditional dependency

**Member B creates:**
- [ ] `EditorPanel.java`
- [ ] `ComponentListPanel.java`
- [ ] `PropertySheetPanel.java`
- [ ] `LayoutSerializer.java`
- [ ] (Optional) `ColorPickerField.java` — consolidated R/G/B with a colour preview swatch

**Screen authors (any screen that wants UI Builder support):**
- [ ] Accept `UiRegistrar` in constructor (default to `UiRegistrar.NOOP`)
- [ ] Call `uiRegistrar.register(...)` for each actor in `show()`
- [ ] Call `uiRegistrar.unregisterAll()` in `hide()`

**Integration test (both members):**
- [ ] Run game, confirm editor window appears
- [ ] `MainMenuScreen` actors appear in the component list by name
- [ ] Move a slider, confirm actor moves in game immediately
- [ ] Switch screens, confirm old actors disappear from the list
- [ ] Save layout, reload layout, confirm properties restore correctly
