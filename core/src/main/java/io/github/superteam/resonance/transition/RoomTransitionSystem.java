package io.github.superteam.resonance.transition;

import io.github.superteam.resonance.dialogue.DialogueSystem;

/** Minimal room transition system implementing fade logic. */
public final class RoomTransitionSystem {
    private static final float FADE_DURATION = 1.2f;
    private float fadeAlpha = 0f;
    private boolean fadingOut = false;
    private boolean fadingIn = false;
    private String pendingRoomName;

    public void triggerRoomTransition(String enteringRoom) {
        pendingRoomName = enteringRoom;
        fadingOut = true;
    }

    public void update(float delta, DialogueSystem dialogue) {
        if (fadingOut) {
            fadeAlpha = Math.min(1f, fadeAlpha + delta / FADE_DURATION);
            if (fadeAlpha >= 1f) {
                fadingOut = false;
                fadingIn = true;
                if (dialogue != null && pendingRoomName != null) dialogue.showSubtitle(pendingRoomName, 2.0f);
            }
        }
        if (fadingIn) {
            fadeAlpha = Math.max(0f, fadeAlpha - delta / FADE_DURATION);
            if (fadeAlpha <= 0f) fadingIn = false;
        }
    }

    public float fadeAlpha() { return fadeAlpha; }
}
