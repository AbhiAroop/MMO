package com.server.entities.npc.types;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.npc.NPC;

/**
 * A basic NPC that provides dialogue options but cannot be attacked
 */
public class DialogueNPC extends BaseNPC {
    
    private final String dialogueId;
    
    /**
     * Create a new dialogue NPC
     * 
     * @param id The unique ID
     * @param name The display name
     * @param dialogueId The initial dialogue ID
     */
    public DialogueNPC(String id, String name, String dialogueId) {
        super(id, name, new NPCStats());
        this.dialogueId = dialogueId;
        
        // Setup a default dialogue if none exists
        setupDefaultDialogue(dialogueId, "Hello there! How can I help you today?");
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        this.npc = NPCManager.getInstance().createNPC(id, name, location, skin, true);
        applyBaseMetadata();
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
        
        return npc;
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        if (rightClick) {
            // Start dialogue on right-click
            startDialogue(player, dialogueId);
        } else {
            // Just a greeting on left-click
            sendMessage(player, "Hello there! Right-click to talk to me.");
        }
    }
    
    /**
     * Set the dialogue for this NPC
     * 
     * @param dialogueId The new dialogue ID
     */
    public void setDialogue(String dialogueId) {
        setupDefaultDialogue(dialogueId, "Hello there! How can I help you today?");
    }
}