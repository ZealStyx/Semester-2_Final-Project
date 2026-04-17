package io.github.superteam.resonance.sound;

import java.util.List;

/**
 * One-shot or externally-triggered event source.
 */
public final class EventSoundSource implements SoundSource {
    private final String id;
    private SoundEventData pendingEvent;
    private boolean active = true;

    public EventSoundSource(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Source id must not be blank.");
        }
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SoundSourceType type() {
        return SoundSourceType.EVENT;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
        pendingEvent = null;
    }

    public void queue(SoundEventData soundEventData) {
        if (!active) {
            return;
        }
        pendingEvent = soundEventData;
    }

    @Override
    public List<SoundSourceEmission> updateAndCollect(float deltaSeconds, float nowSeconds) {
        if (!active || pendingEvent == null) {
            return List.of();
        }
        SoundEventData emission = pendingEvent;
        pendingEvent = null;
        return List.of(new SoundSourceEmission(id, emission));
    }
}
