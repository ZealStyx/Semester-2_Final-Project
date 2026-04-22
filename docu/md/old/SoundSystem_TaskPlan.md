# Spatial Sound System (Horror 3D Game) — Design Plan

## 1. Overview

This document describes a universal spatial sound system designed for a horror 3D game. It applies to **all sound sources**, including items, environmental objects, entities, and any interactive or dynamic events.

The system is not just about audio realism — it is designed to enhance:
- Fear
- Uncertainty
- Spatial confusion
- Psychological tension

Sound is treated as a **physical phenomenon inside the game world**, not just a playback effect.

---

## 2. Core Concept

Every sound exists as a **moving or static 3D event source**.

Key idea:
> Sound is an object in the world with position, behavior, and environmental interaction.

Each sound source has:
- Position (x, y, z)
- Lifetime (instant, looping, or event-based)
- Behavior rules (static, moving, attached, or reactive)
- Environmental interaction properties

---

## 3. System Structure

### 3.1 Sound Source Types

#### Static Sources
- Do not move once created
- Examples: dripping water, creaking door, alarms
- Used for environmental tension

#### Attached Sources
- Follow a moving object or entity
- Examples: footsteps, carried item noises, moving machinery

#### Event-Based Sources
- Triggered once or intermittently
- Examples: explosion, door slam, monster roar

#### Reactive Sources
- Change behavior based on conditions
- Examples: flickering radio, unstable whispers, broken electronics

---

### 3.2 Spatial Audio Placement
- Each sound exists in 3D world space
- Sound position is continuously evaluated relative to the listener
- If attached, position updates every frame or tick

Key behavior:
> Sound behaves like a floating point in space that may move, persist, or vanish.

---

## 4. Movement Behavior

### 4.1 Moving Sound Sources
When a sound source moves:
- Position updates continuously
- Audio follows a smooth path through space
- Direction changes gradually based on velocity

Examples:
- dragging object
- crawling entity
- rolling item

---

### 4.2 Speed-Based Audio Effect
Movement affects sound characteristics:

- Slow movement → stable, clear audio trail
- Fast movement → distorted, less readable direction
- Sudden movement → directional “snap” effect

---

## 5. Environmental Interaction System

Sound reacts to environment using acoustic rules:

### 5.1 Occlusion (Blocking)
- Solid objects reduce clarity
- Thick materials heavily muffle sound
- Partial occlusion allows faint transmission

Effect goal:
> Sound feels trapped or hidden behind surfaces

---

### 5.2 Reverb Zones
Different spaces modify sound:

- Small rooms → tight echo
- Corridors → directional bounce
- Open spaces → weak reflection
- Underground areas → deep resonance

---

### 5.3 Material Influence
Surfaces affect sound character:
- Metal → sharp reflections
- Wood → warm muffling
- Stone → heavy echo
- Cloth → heavy dampening

---

## 6. Distance System

As distance increases:

- Volume decreases gradually
- Detail fades progressively
- High-frequency content disappears first
- At extreme distance → sound becomes indistinct or silent

### Horror Extension:
- At extreme range, partial or misleading fragments may still be perceived
- This creates uncertainty about exact location or meaning

---

## 7. Temporal Sound Behavior

Sound is not just instant playback — it has time-based structure:

### 7.1 Continuous Sounds
- Looping or sustained events
- Example: machinery hum, wind, breathing-like effects

### 7.2 Bursting Sounds
- Short events
- Example: footsteps, impacts, clicks

### 7.3 Delayed Echo Memory
- Some sounds may briefly reappear after delay
- Used for psychological tension (optional horror layer)

---

## 8. Occlusion & Direction Confusion (Horror Layer)

Under certain conditions:
- Sound direction may feel slightly unstable
- Echoes may appear from incorrect surfaces
- Reflections may exaggerate presence in nearby rooms

Important:
> This effect should be subtle, not constant, to maintain tension without breaking trust in audio.

---

## 9. Multi-Sound Interaction

When multiple sounds overlap:

- System prioritizes by intensity and proximity
- Important sounds may mask weaker ones
- Overlapping spatial sources create confusion in direction perception

Horror intention:
> Too much sound can become unreadable noise

---

## 10. Edge Cases

### 10.1 Rapid Movement of Sound Source
- Produces streak-like audio perception
- Direction may lag slightly behind movement

---

### 10.2 Sudden Sound Emergence
- Instant spawn of sound in space
- May feel like “appearing behind” effect

---

### 10.3 Sound Obstruction Event
- A previously clear sound becomes suddenly muffled
- Can indicate environmental change or hidden transition

---

### 10.4 Sound Cut-Off
- Abrupt ending of audio event
- Can be used for tension (e.g., silence after impact)

---

## 11. Horror Design Principles

- Sound should be **informative but not fully reliable**
- Silence is as important as noise
- Direction should feel mostly correct but occasionally unsettling
- Environment should feel like it is “hiding” sound
- Audio should support paranoia, not clarity

---

## 12. Player Experience Goals

The system should create moments where:
- A sound feels closer than it is
- A distant sound feels nearby
- A clear sound suddenly becomes unclear
- Silence feels unsafe
- Environment feels “listening”

---

## 13. Summary

This spatial sound system treats audio as a **living world-layered simulation**, where every sound behaves like an object influenced by space, movement, and environment.

It is designed to:
- enhance immersion
- reinforce horror atmosphere
- create uncertainty through audio perception