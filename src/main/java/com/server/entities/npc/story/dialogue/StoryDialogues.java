package com.server.entities.npc.story.dialogue;

import org.bukkit.Sound;

import com.server.entities.npc.dialogue.DialogueAction;
import com.server.entities.npc.dialogue.DialogueManager;

/**
 * Central registry for all story-related dialogues
 */
public class StoryDialogues {
    
    private static StoryDialogues instance;
    private final DialogueManager dialogueManager;
    
    private StoryDialogues() {
        dialogueManager = DialogueManager.getInstance();
        initializeDialogues();
    }
    
    /**
     * Get the instance of the StoryDialogues registry
     * 
     * @return The single instance
     */
    public static StoryDialogues getInstance() {
        if (instance == null) {
            instance = new StoryDialogues();
        }
        return instance;
    }
    
    /**
     * Initialize all story dialogues
     */
    private void initializeDialogues() {
        // All NPC-specific dialogues are created in their respective classes
    }
    
    /**
     * Create a simple action to play a sound
     * 
     * @param sound The sound to play
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch (0.5 to 2.0)
     * @return The action
     */
    public static DialogueAction createSoundAction(Sound sound, float volume, float pitch) {
        return player -> player.playSound(player.getLocation(), sound, volume, pitch);
    }
}