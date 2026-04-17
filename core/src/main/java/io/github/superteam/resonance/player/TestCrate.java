package io.github.superteam.resonance.player;

import com.badlogic.gdx.Gdx;

/**
 * Minimal interactable crate used by PlayerTestScreen integration tests.
 *
 * Single Responsibility: Respond to player interaction for validation.
 */
public class TestCrate implements Interactable {
    @FunctionalInterface
    public interface InteractionListener {
        void onInteracted(PlayerController playerController);
    }

    private InteractionListener interactionListener;

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    @Override
    public void onInteract(PlayerController player) {
        Gdx.app.log("TestCrate", "Player interacted with crate at position: " + player.getPosition());
        if (interactionListener != null) {
            interactionListener.onInteracted(player);
        }
    }
}
