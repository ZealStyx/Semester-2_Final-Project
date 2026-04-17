package io.github.superteam.resonance.player;

/**
 * Interface for world objects that can be interacted with via player F-key interaction.
 * Implementations should handle their own logic when the player initiates interaction.
 *
 * Single Responsibility: Define the contract for interactive objects.
 * Separation of Concern: World object types implement this independently; no manager class.
 */
public interface Interactable {
    /**
     * Called when the player presses F while targeting this object within interaction range.
     *
     * @param player The PlayerController that initiated the interaction.
     */
    void onInteract(PlayerController player);
}
