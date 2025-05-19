package com.server.entities.npc.types;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
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
        
        // CRITICAL FIX: Make dialogue NPCs invulnerable
        if (npc.isSpawned()) {
            npc.getEntity().setInvulnerable(true);
            npc.getEntity().setMetadata("invulnerable", new FixedMetadataValue(Main.getInstance(), true));
            npc.getEntity().setMetadata("dialogue_npc", new FixedMetadataValue(Main.getInstance(), true));
            
            // Set NPC metadata for better targeting prevention
            npc.data().setPersistent(NPC.Metadata.DEFAULT_PROTECTED, true);
        }
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
        
        return npc;
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        // CRITICAL FIX: Add debug logging to track interactions
        Main.getInstance().getLogger().info("DialogueNPC interaction: " + name + " with " + player.getName() + 
                                        " - right click: " + rightClick);
        
        // CRITICAL FIX: Always initiate dialogue on interact, regardless of click type
        // This ensures more reliable dialogue triggering
        startDialogue(player, dialogueId);
        
        // Also send a message for left clicks as a fallback
        if (!rightClick) {
            sendMessage(player, "Hello there! I'm " + name + ".");
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