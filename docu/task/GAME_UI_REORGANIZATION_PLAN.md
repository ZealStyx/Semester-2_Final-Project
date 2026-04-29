# Game UI Reorganization & Main Game UI Plan

**Project:** Resonance (`io.github.superteam.resonance`)  
**LibGDX version:** 1.14.0  
**Java target:** 17  
**Backend:** LWJGL3  

---

## Overview

This plan addresses two UI-related concerns:
1. **Dev/Debug UI Reorganization** - The current debug information display has become scattered and confusing. Needs consolidation into a clean, organized system.
2. **Main Game UI Design** - Create the primary in-game HUD with player stats, hotbar, objectives, and debug toggle functionality.

---

## Part 1: Dev/Debug UI Reorganization

### Current State Analysis

Based on codebase inspection, debug information is currently scattered across:
- `BehaviorDebugOverlay.java` - Behavior classification display
- `DebugConsole.java` - Command system
- Various inline debug renders in test scenes
- Sound propagation debug displays
- Blind effect debug information

**Problems:**
- No centralized debug UI manager
- Debug renders mixed with game logic
- No consistent positioning or styling
- No toggle mechanism
- Information overload when multiple debug systems active

### Proposed Architecture

```
core/src/main/java/io/github/superteam/resonance/debug/
├── DebugUIManager.java              # Central manager for all debug overlays
├── DebugOverlay.java                # Interface for debug overlay components
├── overlays/
│   ├── BehaviorDebugOverlay.java    # Existing - refactor to implement interface
│   ├── SoundDebugOverlay.java       # Sound propagation info
│   ├── BlindEffectDebugOverlay.java # Blind effect state
│   ├── PerformanceDebugOverlay.java # FPS, memory, etc.
│   ├── CollisionDebugOverlay.java   # Collision visualization
│   └── EntityDebugOverlay.java      # Entity/position info
└── toggle/
    └── DebugToggleHandler.java      # Keyboard input handler for toggling
```

### 1.1 DebugOverlay Interface

```java
package io.github.superteam.resonance.debug;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * Interface for all debug overlay components.
 * Each overlay is responsible for rendering its specific debug information.
 */
public interface DebugOverlay {
    
    /**
     * Render this debug overlay.
     * @param batch SpriteBatch for rendering
     * @param font Font for text rendering
     * @param x X position (relative to overlay area)
     * @param y Y position (relative to overlay area)
     */
    void render(SpriteBatch batch, BitmapFont font, float x, float y);
    
    /**
     * Get the display name of this overlay (for UI menu).
     */
    String getDisplayName();
    
    /**
     * Get the unique ID of this overlay (for persistence).
     */
    String getId();
    
    /**
     * Check if this overlay is currently enabled.
     */
    boolean isEnabled();
    
    /**
     * Enable or disable this overlay.
     */
    void setEnabled(boolean enabled);
    
    /**
     * Get the estimated height this overlay will occupy when rendered.
     */
    float getHeight(BitmapFont font);
}
```

### 1.2 DebugUIManager

```java
package io.github.superteam.resonance.debug;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.*;

/**
 * Central manager for all debug overlays.
 * Handles positioning, toggling, and rendering of debug information.
 */
public final class DebugUIManager {
    
    private static final DebugUIManager INSTANCE = new DebugUIManager();
    public static DebugUIManager get() { return INSTANCE; }
    
    private final Map<String, DebugOverlay> overlays = new LinkedHashMap<>();
    private boolean globalEnabled = false;
    private float baseX = 10f;
    private float baseY = 50f;
    private float lineHeight = 16f;
    
    private DebugUIManager() {}
    
    public void register(DebugOverlay overlay) {
        overlays.put(overlay.getId(), overlay);
    }
    
    public void unregister(String id) {
        overlays.remove(id);
    }
    
    public void setGlobalEnabled(boolean enabled) {
        this.globalEnabled = enabled;
    }
    
    public boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    public void toggleOverlay(String id) {
        DebugOverlay overlay = overlays.get(id);
        if (overlay != null) {
            overlay.setEnabled(!overlay.isEnabled());
        }
    }
    
    public void renderAll(SpriteBatch batch, BitmapFont font) {
        if (!globalEnabled) return;
        
        float currentY = baseY;
        for (DebugOverlay overlay : overlays.values()) {
            if (overlay.isEnabled()) {
                overlay.render(batch, font, baseX, currentY);
                currentY += overlay.getHeight(font) + 10f; // 10px gap
            }
        }
    }
    
    public DebugOverlay getOverlay(String id) {
        return overlays.get(id);
    }
    
    public Collection<DebugOverlay> getAllOverlays() {
        return overlays.values();
    }
}
```

### 1.3 DebugToggleHandler

```java
package io.github.superteam.resonance.debug.toggle;

import com.badlogic.gdx.InputProcessor;
import io.github.superteam.resonance.debug.DebugUIManager;

/**
 * Handles keyboard input for toggling debug UI.
 * Default toggle: Ctrl + F12
 */
public final class DebugToggleHandler implements InputProcessor {
    
    private final DebugUIManager debugManager;
    private boolean ctrlPressed = false;
    
    public DebugToggleHandler(DebugUIManager debugManager) {
        this.debugManager = debugManager;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.CONTROL_LEFT || 
            keycode == com.badlogic.gdx.Input.Keys.CONTROL_RIGHT) {
            ctrlPressed = true;
        }
        
        if (ctrlPressed && keycode == com.badlogic.gdx.Input.Keys.F12) {
            debugManager.setGlobalEnabled(!debugManager.isGlobalEnabled());
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.CONTROL_LEFT || 
            keycode == com.badlogic.gdx.Input.Keys.CONTROL_RIGHT) {
            ctrlPressed = false;
        }
        return false;
    }
    
    // Other InputProcessor methods (no-op)
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int x, int y, int pointer, int button) { return false; }
    @Override public boolean touchUp(int x, int y, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int x, int y, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
```

### 1.4 Refactor Existing BehaviorDebugOverlay

Update existing `BehaviorDebugOverlay.java` to implement the new interface:

```java
package io.github.superteam.resonance.behavior.debug;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.superteam.resonance.behavior.BehaviorSystem;
import io.github.superteam.resonance.behavior.PlayerArchetype;
import io.github.superteam.resonance.debug.DebugOverlay;

public final class BehaviorDebugOverlay implements DebugOverlay {
    
    private boolean enabled = true;
    private final BehaviorSystem behaviorSystem;
    
    public BehaviorDebugOverlay(BehaviorSystem behaviorSystem) {
        this.behaviorSystem = behaviorSystem;
    }
    
    @Override
    public void render(SpriteBatch batch, BitmapFont font, float x, float y) {
        if (!enabled || batch == null || font == null || behaviorSystem == null) {
            return;
        }
        
        float lineHeight = 14f;
        batch.begin();
        font.setColor(0.92f, 0.95f, 1f, 0.80f);
        font.draw(batch, "[Behavior] " + behaviorSystem.currentArchetype(), x, y);
        font.draw(batch, "[Behavior] Samples=" + behaviorSystem.sampleCount() + "  Inertia=" + String.format("%.3f", behaviorSystem.inertia()), x, y - lineHeight);
        
        float[] scores = behaviorSystem.scores();
        PlayerArchetype[] archetypes = PlayerArchetype.values();
        for (int i = 0; i < archetypes.length && i < scores.length; i++) {
            font.draw(batch, archetypes[i].displayName() + "  " + String.format("%.2f", scores[i]), x, y - ((i + 2) * lineHeight));
        }
        batch.end();
    }
    
    @Override public String getDisplayName() { return "Behavior Classification"; }
    @Override public String getId() { return "behavior"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public float getHeight(BitmapFont font) { return 14f * (2 + PlayerArchetype.values().length); }
}
```

### 1.5 New Overlay: PerformanceDebugOverlay

```java
package io.github.superteam.resonance.debug.overlays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.superteam.resonance.debug.DebugOverlay;

public final class PerformanceDebugOverlay implements DebugOverlay {
    
    private boolean enabled = true;
    
    @Override
    public void render(SpriteBatch batch, BitmapFont font, float x, float y) {
        if (!enabled) return;
        
        batch.begin();
        font.setColor(0.5f, 1f, 0.5f, 0.90f);
        
        int fps = Gdx.graphics.getFramesPerSecond();
        long javaHeap = Gdx.app.getJavaHeap() / 1024 / 1024;
        long nativeHeap = Gdx.app.getNativeHeap() / 1024 / 1024;
        
        font.draw(batch, String.format("[Performance] FPS: %d", fps), x, y);
        font.draw(batch, String.format("[Performance] Java Heap: %d MB", javaHeap), x, y - 16);
        font.draw(batch, String.format("[Performance] Native Heap: %d MB", nativeHeap), x, y - 32);
        
        batch.end();
    }
    
    @Override public String getDisplayName() { return "Performance"; }
    @Override public String getId() { return "performance"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public float getHeight(BitmapFont font) { return 48f; }
}
```

### 1.6 Integration in GameScreen

```java
// In GameScreen.java
private DebugUIManager debugManager;
private DebugToggleHandler debugToggleHandler;
private BitmapFont debugFont;

@Override
public void show() {
    // ... existing code ...
    
    // Initialize debug UI
    debugManager = DebugUIManager.get();
    debugFont = new BitmapFont();
    
    // Register overlays
    debugManager.register(new BehaviorDebugOverlay(behaviorSystem));
    debugManager.register(new PerformanceDebugOverlay());
    debugManager.register(new SoundDebugOverlay(soundSystem));
    debugManager.register(new BlindEffectDebugOverlay(blindEffectController));
    
    // Setup input handling
    debugToggleHandler = new DebugToggleHandler(debugManager);
    Gdx.input.setInputProcessor(debugToggleHandler);
}

@Override
public void render(float delta) {
    // ... existing game render code ...
    
    // Render debug overlays
    debugManager.renderAll(batch, debugFont);
}
```

---

## Part 2: Main Game UI Design

### UI Layout Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    [GAME VIEWPORT]                              │
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ [STAMINA BAR]████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
│                                                                 │
│ [HOTBAR] [1][2][3][4][5][6]                                     │
│                                                                 │
│ [VOICE METER] ▂▄▆█  [SANITY METER] ████████░░░░░░░░░░░░░░░░░░░│
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                                                        [OBJECTIVE]│
│                                              Find the keycard     │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
core/src/main/java/io/github/superteam/resonance/ui/
├── hud/                              # Heads-Up Display components
│   ├── GameHUD.java                 # Main HUD container
│   ├── StaminaBar.java              # Stamina display
│   ├── Hotbar.java                  # Item hotbar
│   ├── VoiceMeter.java              # Voice/sound meter
│   ├── SanityMeter.java             # Sanity display
│   └── ObjectiveDisplay.java       # Objective tracker (top-right)
├── widgets/                         # Reusable UI widgets
│   ├── ProgressBar.java             # Generic progress bar widget
│   ├── HotbarSlot.java              # Individual hotbar slot
│   └── MeterWidget.java             # Generic meter widget
└── GameUISkin.java                   # Skin definition for UI styling
```

### 2.1 GameHUD - Main Container

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.superteam.resonance.ui.widgets.GameUISkin;

/**
 * Main HUD container for the game.
 * Manages all in-game UI elements.
 */
public final class GameHUD {
    
    private final Stage stage;
    private final Table rootTable;
    private final StaminaBar staminaBar;
    private final Hotbar hotbar;
    private final VoiceMeter voiceMeter;
    private final SanityMeter sanityMeter;
    private final ObjectiveDisplay objectiveDisplay;
    
    public GameHUD(GameUISkin skin) {
        this.stage = new Stage(new ScreenViewport());
        this.rootTable = new Table(skin);
        rootTable.setFillParent(true);
        
        // Create components
        staminaBar = new StaminaBar(skin);
        hotbar = new Hotbar(skin);
        voiceMeter = new VoiceMeter(skin);
        sanityMeter = new SanityMeter(skin);
        objectiveDisplay = new ObjectiveDisplay(skin);
        
        // Layout - Bottom section (left to right)
        Table bottomSection = new Table(skin);
        bottomSection.bottom().left().pad(20f);
        
        bottomSection.add(staminaBar).width(300f).height(20f).padBottom(10f).row();
        bottomSection.add(hotbar).width(400f).height(50f).padBottom(10f).row();
        
        Table meters = new Table(skin);
        meters.add(voiceMeter).width(150f).height(30f).padRight(20f);
        meters.add(sanityMeter).width(150f).height(30f);
        bottomSection.add(meters).row();
        
        rootTable.add(bottomSection).expand().bottom().left();
        
        // Layout - Top right (objective)
        rootTable.add(objectiveDisplay).expand().top().right().pad(20f);
        
        stage.addActor(rootTable);
    }
    
    public void act(float delta) {
        stage.act(delta);
    }
    
    public void draw() {
        stage.draw();
    }
    
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    public void dispose() {
        stage.dispose();
    }
    
    // Component accessors
    public StaminaBar getStaminaBar() { return staminaBar; }
    public Hotbar getHotbar() { return hotbar; }
    public VoiceMeter getVoiceMeter() { return voiceMeter; }
    public SanityMeter getSanityMeter() { return sanityMeter; }
    public ObjectiveDisplay getObjectiveDisplay() { return objectiveDisplay; }
}
```

### 2.2 StaminaBar

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.superteam.resonance.ui.widgets.ProgressBar;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Displays player stamina as a progress bar.
 */
public final class StaminaBar extends Table {
    
    private final ProgressBar progressBar;
    private float currentStamina = 100f;
    private float maxStamina = 100f;
    
    public StaminaBar(GameUISkin skin) {
        super(skin);
        
        progressBar = new ProgressBar(skin, "stamina");
        progressBar.setValue(currentStamina / maxStamina);
        
        add(progressBar).grow();
    }
    
    public void setStamina(float stamina) {
        this.currentStamina = Math.max(0f, Math.min(maxStamina, stamina));
        progressBar.setValue(currentStamina / maxStamina);
    }
    
    public void setMaxStamina(float max) {
        this.maxStamina = max;
        progressBar.setValue(currentStamina / maxStamina);
    }
    
    public float getStamina() { return currentStamina; }
}
```

### 2.3 Hotbar

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.superteam.resonance.ui.widgets.HotbarSlot;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * 6-slot hotbar for inventory items.
 */
public final class Hotbar extends Table {
    
    private final HotbarSlot[] slots = new HotbarSlot[6];
    
    public Hotbar(GameUISkin skin) {
        super(skin);
        
        for (int i = 0; i < 6; i++) {
            slots[i] = new HotbarSlot(skin, i + 1);
            add(slots[i]).size(40f).pad(2f);
        }
    }
    
    public void setItem(int slotIndex, Object item) {
        if (slotIndex >= 0 && slotIndex < slots.length) {
            slots[slotIndex].setItem(item);
        }
    }
    
    public void setSelectedSlot(int slotIndex) {
        for (int i = 0; i < slots.length; i++) {
            slots[i].setSelected(i == slotIndex);
        }
    }
    
    public HotbarSlot getSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slots.length) {
            return slots[slotIndex];
        }
        return null;
    }
}
```

### 2.4 VoiceMeter

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.superteam.resonance.ui.widgets.MeterWidget;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Displays current voice/sound level (for stealth mechanics).
 */
public final class VoiceMeter extends Table {
    
    private final MeterWidget meter;
    private final Label label;
    private float currentLevel = 0f;
    
    public VoiceMeter(GameUISkin skin) {
        super(skin);
        
        label = new Label("VOICE", skin);
        meter = new MeterWidget(skin, "voice");
        meter.setValue(0f);
        
        add(label).padRight(5f);
        add(meter).width(80f).height(20f);
    }
    
    public void setVoiceLevel(float level) {
        this.currentLevel = Math.max(0f, Math.min(1f, level));
        meter.setValue(currentLevel);
    }
    
    public float getVoiceLevel() { return currentLevel; }
}
```

### 2.5 SanityMeter

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.superteam.resonance.ui.widgets.MeterWidget;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Displays player sanity level (for horror mechanics).
 */
public final class SanityMeter extends Table {
    
    private final MeterWidget meter;
    private final Label label;
    private float currentSanity = 100f;
    private float maxSanity = 100f;
    
    public SanityMeter(GameUISkin skin) {
        super(skin);
        
        label = new Label("SANITY", skin);
        meter = new MeterWidget(skin, "sanity");
        meter.setValue(1f);
        
        add(label).padRight(5f);
        add(meter).width(80f).height(20f);
    }
    
    public void setSanity(float sanity) {
        this.currentSanity = Math.max(0f, Math.min(maxSanity, sanity));
        meter.setValue(currentSanity / maxSanity);
    }
    
    public void setMaxSanity(float max) {
        this.maxSanity = max;
        meter.setValue(currentSanity / maxSanity);
    }
    
    public float getSanity() { return currentSanity; }
}
```

### 2.6 ObjectiveDisplay

```java
package io.github.superteam.resonance.ui.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Displays current objective in top-right corner.
 */
public final class ObjectiveDisplay extends Table {
    
    private final Label titleLabel;
    private final Label objectiveLabel;
    private String currentObjective = "";
    
    public ObjectiveDisplay(GameUISkin skin) {
        super(skin);
        
        titleLabel = new Label("OBJECTIVE", skin, "objective-title");
        objectiveLabel = new Label("", skin, "objective-text");
        
        right().top();
        add(titleLabel).right().padBottom(5f).row();
        add(objectiveLabel).right().width(300f);
    }
    
    public void setObjective(String objective) {
        this.currentObjective = objective != null ? objective : "";
        objectiveLabel.setText(currentObjective);
    }
    
    public String getObjective() { return currentObjective; }
}
```

### 2.7 ProgressBar Widget

```java
package io.github.superteam.resonance.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Generic progress bar widget.
 */
public final class ProgressBar extends Widget {
    
    private final Drawable background;
    private final Drawable fill;
    private float value = 1f;
    
    public ProgressBar(GameUISkin skin, String styleName) {
        this.background = skin.getDrawable(styleName + "-background");
        this.fill = skin.getDrawable(styleName + "-fill");
    }
    
    public void setValue(float value) {
        this.value = Math.max(0f, Math.min(1f, value));
    }
    
    public float getValue() { return value; }
    
    @Override
    public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        // Draw background
        background.draw(batch, x, y, width, height);
        
        // Draw fill
        float fillWidth = width * value;
        fill.draw(batch, x, y, fillWidth, height);
    }
}
```

### 2.8 MeterWidget

```java
package io.github.superteam.resonance.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Generic meter widget (for voice/sanity meters).
 * Uses segmented display style.
 */
public final class MeterWidget extends Widget {
    
    private final Drawable[] segments;
    private float value = 0f;
    private final int segmentCount = 5;
    
    public MeterWidget(GameUISkin skin, String styleName) {
        this.segments = new Drawable[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = skin.getDrawable(styleName + "-segment-" + i);
        }
    }
    
    public void setValue(float value) {
        this.value = Math.max(0f, Math.min(1f, value));
    }
    
    public float getValue() { return value; }
    
    @Override
    public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float segmentWidth = getWidth() / segmentCount;
        float segmentHeight = getHeight();
        
        int activeSegments = (int) (value * segmentCount);
        
        for (int i = 0; i < segmentCount; i++) {
            if (i < activeSegments) {
                segments[i].draw(batch, x + (i * segmentWidth), y, segmentWidth, segmentHeight);
            }
        }
    }
}
```

### 2.9 HotbarSlot Widget

```java
package io.github.superteam.resonance.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.superteam.resonance.ui.GameUISkin;

/**
 * Individual hotbar slot.
 */
public final class HotbarSlot extends Table {
    
    private final Image background;
    private final Image itemIcon;
    private final Label keyLabel;
    private final int slotNumber;
    private boolean selected = false;
    private Object item = null;
    
    public HotbarSlot(GameUISkin skin, int slotNumber) {
        super(skin);
        this.slotNumber = slotNumber;
        
        background = new Image(skin.getDrawable("hotbar-slot"));
        itemIcon = new Image();
        keyLabel = new Label(String.valueOf(slotNumber), skin, "hotbar-key");
        
        add(background).size(40f);
        itemIcon.setPosition(5f, 5f);
        itemIcon.setSize(30f, 30f);
        addActor(itemIcon);
        keyLabel.setPosition(2f, 22f);
        addActor(keyLabel);
    }
    
    public void setItem(Object item) {
        this.item = item;
        // Update icon based on item type
        // itemIcon.setDrawable(...);
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            background.setDrawable(getSkin().getDrawable("hotbar-slot-selected"));
        } else {
            background.setDrawable(getSkin().getDrawable("hotbar-slot"));
        }
    }
    
    public boolean isSelected() { return selected; }
    public Object getItem() { return item; }
    public int getSlotNumber() { return slotNumber; }
}
```

### 2.10 GameUISkin

```java
package io.github.superteam.resonance.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Central skin definition for all game UI elements.
 * Provides consistent styling across HUD components.
 */
public final class GameUISkin extends Skin {
    
    public GameUISkin() {
        // Load base skin (can use default LibGDX skin or custom)
        // addDefault();
        
        // Register custom drawables (these would be loaded from texture atlas)
        registerDrawables();
        
        // Register fonts
        registerFonts();
        
        // Register styles
        registerStyles();
    }
    
    private void registerDrawables() {
        // Stamina bar
        add("stamina-background", createColorDrawable(Color.DARK_GRAY));
        add("stamina-fill", createColorDrawable(Color.GREEN));
        
        // Voice meter segments
        for (int i = 0; i < 5; i++) {
            float alpha = 0.2f + (i * 0.2f);
            add("voice-segment-" + i, createColorDrawable(new Color(0.5f, 0.8f, 1f, alpha)));
        }
        
        // Sanity meter segments
        for (int i = 0; i < 5; i++) {
            float alpha = 0.2f + (i * 0.2f);
            add("sanity-segment-" + i, createColorDrawable(new Color(0.8f, 0.4f, 0.8f, alpha)));
        }
        
        // Hotbar slots
        add("hotbar-slot", createColorDrawable(new Color(0.3f, 0.3f, 0.3f, 0.8f)));
        add("hotbar-slot-selected", createColorDrawable(new Color(0.6f, 0.6f, 0.4f, 0.9f)));
    }
    
    private void registerFonts() {
        // Use default font for now, can replace with custom font
        BitmapFont defaultFont = new BitmapFont();
        add("default", defaultFont);
        
        // Style-specific fonts
        add("objective-title", new Label.LabelStyle(defaultFont, Color.WHITE));
        add("objective-text", new Label.LabelStyle(defaultFont, new Color(0.9f, 0.9f, 0.9f, 1f)));
        add("hotbar-key", new Label.LabelStyle(defaultFont, new Color(0.7f, 0.7f, 0.7f, 1f)));
    }
    
    private void registerStyles() {
        // Additional style registrations as needed
    }
    
    private Drawable createColorDrawable(Color color) {
        // In production, load from texture atlas
        // For now, return a placeholder
        return new com.badlogic.gdx.scenes.scene2d.utils.Drawable() {
            @Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float x, float y, float width, float height) {
                com.badlogic.gdx.graphics.Color oldColor = batch.getColor();
                batch.setColor(color);
                // Draw placeholder rectangle
                batch.setColor(oldColor);
            }
            @Override public float getLeftWidth() { return 0; }
            @Override public float getRightWidth() { return 0; }
            @Override public float getTopHeight() { return 0; }
            @Override public float getBottomHeight() { return 0; }
            @Override public float getMinWidth() { return 10; }
            @Override public float getMinHeight() { return 10; }
        };
    }
}
```

---

## Part 3: Integration in GameScreen

### 3.1 GameScreen Modifications

```java
package io.github.superteam.resonance.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import io.github.superteam.resonance.debug.DebugUIManager;
import io.github.superteam.resonance.debug.toggle.DebugToggleHandler;
import io.github.superteam.resonance.ui.hud.GameHUD;
import io.github.superteam.resonance.ui.GameUISkin;

public class GameScreen implements Screen {
    
    private GameHUD gameHUD;
    private DebugUIManager debugManager;
    private DebugToggleHandler debugToggleHandler;
    private BitmapFont debugFont;
    
    // Existing game state...
    
    @Override
    public void show() {
        // Initialize game HUD
        gameHUD = new GameHUD(new GameUISkin());
        
        // Initialize debug UI
        debugManager = DebugUIManager.get();
        debugFont = new BitmapFont();
        
        // Register debug overlays
        debugManager.register(new BehaviorDebugOverlay(behaviorSystem));
        debugManager.register(new PerformanceDebugOverlay());
        // ... other overlays ...
        
        // Setup input handling
        debugToggleHandler = new DebugToggleHandler(debugManager);
        Gdx.input.setInputProcessor(debugToggleHandler);
        
        // ... existing initialization ...
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // Render game world
        // ... existing game render code ...
        
        // Render game HUD
        gameHUD.act(delta);
        gameHUD.draw();
        
        // Render debug overlays (if enabled)
        debugManager.renderAll(batch, debugFont);
    }
    
    @Override
    public void resize(int width, int height) {
        gameHUD.resize(width, height);
        // ... existing resize code ...
    }
    
    @Override
    public void dispose() {
        gameHUD.dispose();
        debugFont.dispose();
        // ... existing dispose code ...
    }
    
    // Accessors for game systems to update HUD
    public void updateStamina(float stamina) {
        gameHUD.getStaminaBar().setStamina(stamina);
    }
    
    public void updateVoiceLevel(float level) {
        gameHUD.getVoiceMeter().setVoiceLevel(level);
    }
    
    public void updateSanity(float sanity) {
        gameHUD.getSanityMeter().setSanity(sanity);
    }
    
    public void setObjective(String objective) {
        gameHUD.getObjectiveDisplay().setObjective(objective);
    }
    
    public void setHotbarItem(int slot, Object item) {
        gameHUD.getHotbar().setItem(slot, item);
    }
    
    public void setSelectedHotbarSlot(int slot) {
        gameHUD.getHotbar().setSelectedSlot(slot);
    }
}
```

---

## Part 4: Implementation Checklist

### Phase 1: Debug UI Reorganization
- [ ] Create `DebugOverlay` interface
- [ ] Create `DebugUIManager` singleton
- [ ] Create `DebugToggleHandler` with Ctrl+F12 toggle
- [ ] Refactor `BehaviorDebugOverlay` to implement interface
- [ ] Create `PerformanceDebugOverlay`
- [ ] Create `SoundDebugOverlay` (if not exists)
- [ ] Create `BlindEffectDebugOverlay` (if not exists)
- [ ] Integrate debug manager in `GameScreen`
- [ ] Test toggle functionality

### Phase 2: Game HUD Components
- [ ] Create `GameUISkin` with base drawables
- [ ] Create `ProgressBar` widget
- [ ] Create `MeterWidget` widget
- [ ] Create `HotbarSlot` widget
- [ ] Create `StaminaBar` component
- [ ] Create `Hotbar` component
- [ ] Create `VoiceMeter` component
- [ ] Create `SanityMeter` component
- [ ] Create `ObjectiveDisplay` component
- [ ] Create `GameHUD` container

### Phase 3: Integration
- [ ] Integrate `GameHUD` in `GameScreen`
- [ ] Connect game systems to HUD update methods
- [ ] Add hotbar key bindings (1-9)
- [ ] Test all HUD components render correctly
- [ ] Test debug toggle doesn't interfere with HUD

### Phase 4: Polish
- [ ] Create proper texture assets for UI
- [ ] Add animations for meter changes
- [ ] Add hover effects for hotbar
- [ ] Add sound effects for hotbar selection
- [ ] Tune colors and spacing
- [ ] Add accessibility options (scale, colorblind mode)

---

## Part 5: Asset Requirements

### UI Texture Atlas
The following assets should be created (placeholder colors used in code):

**Stamina Bar:**
- `stamina-background.png` - Dark gray background
- `stamina-fill.png` - Green fill (gradient optional)

**Voice Meter:**
- `voice-segment-0.png` through `voice-segment-4.png` - 5 segments with increasing opacity

**Sanity Meter:**
- `sanity-segment-0.png` through `sanity-segment-4.png` - 5 segments with increasing opacity

**Hotbar:**
- `hotbar-slot.png` - Default slot background (semi-transparent dark)
- `hotbar-slot-selected.png` - Selected slot background (highlighted)
- Item icons for each inventory item type

**Fonts:**
- UI font (can use default BitmapFont or custom TTF)

---

## Part 6: Key Bindings

| Action | Key Combination |
|--------|-----------------|
| Toggle Debug UI | Ctrl + F12 |
| Hotbar Slot 1-6 | 1, 2, 3, 4, 5, 6 |
| Use Item | Left Click (when slot selected) |

---

## Part 7: Future Enhancements

### Potential additions after initial implementation:
- Minimap integration
- Inventory screen (accessible via I key)
- Settings menu overlay
- Notification system for events
- Interactive tooltips for hotbar items
- Drag-and-drop hotbar customization
- Multiple objective tracking
- Voice meter threshold indicators
- Sanity effect visual feedback (screen effects at low sanity)

---

## Summary

This plan provides:
1. **Clean separation** between debug UI and game HUD
2. **Centralized debug management** with easy toggle (Ctrl+F12)
3. **Modular HUD components** that can be updated independently
4. **Consistent styling** through centralized skin system
5. **Clear integration points** in GameScreen
6. **Scalable architecture** for future UI additions

The debug UI reorganization addresses the current confusion by:
- Creating a single `DebugUIManager` to coordinate all debug displays
- Providing a consistent interface for all debug overlays
- Adding a global toggle to show/hide all debug info
- Separating debug rendering from game logic

The main game UI provides all requested elements:
- Stamina bar (bottom-left)
- Hotbar (below stamina)
- Voice meter (bottom-left, below hotbar)
- Sanity meter (bottom-left, next to voice meter)
- Objective display (top-right)
- Debug toggle via Ctrl+F12
