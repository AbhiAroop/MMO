package com.server.entities.npc.types;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.entities.npc.NPCInteractionHandler;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;
import com.server.entities.npc.dialogue.DialogueManager;
import com.server.entities.npc.dialogue.DialogueNode;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

/**
 * Base class for all NPC types
 */
public abstract class BaseNPC implements NPCInteractionHandler {
    protected final String id;
    protected final String name;
    protected final NPCStats stats;
    protected final Main plugin;
    protected NPC npc;
    
    /**
     * Create a new NPC
     * 
     * @param id The unique ID of this NPC
     * @param name The display name
     * @param stats The NPC stats
     */
    public BaseNPC(String id, String name, NPCStats stats) {
        this.id = id;
        this.name = name;
        this.stats = stats;
        this.plugin = Main.getInstance();
    }
    
    /**
     * Get the ID of this NPC
     * 
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the name of this NPC
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the NPC stats
     * 
     * @return The stats
     */
    public NPCStats getStats() {
        return stats;
    }
    
    /**
     * Get the Citizens NPC object
     * 
     * @return The NPC
     */
    public NPC getNPC() {
        return npc;
    }
    
    /**
     * Spawn the NPC at the given location
     * 
     * @param location The location to spawn at
     * @param skin The skin name to use
     * @return The spawned NPC
     */
    public abstract NPC spawn(Location location, String skin);
    
    /**
     * Set equipment for the NPC
     * 
     * @param slot The equipment slot
     * @param item The item
     */
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (npc != null) {
            NPCManager.getInstance().setEquipment(id, slot, item);
        }
    }

    /**
     * Set the Citizens NPC object directly
     * Used when creating NPCs with specific entity types
     * 
     * @param npc The Citizens NPC
     */
    public void setNPC(NPC npc) {
        this.npc = npc;
    }

    /**
     * Called after the NPC is set to complete initialization
     */
    public void finalizeSpawn() {
        applyBaseMetadata();
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
    }
    
    /**
     * Check if this NPC is spawned
     * 
     * @return True if spawned
     */
    public boolean isSpawned() {
        return npc != null && npc.isSpawned();
    }
    
    /**
     * Remove this NPC
     */
    public void remove() {
        NPCManager.getInstance().removeNPC(id);
        this.npc = null;
    }
    
    /**
     * Apply basic metadata to the NPC
     */
    protected void applyBaseMetadata() {
        if (npc != null && npc.isSpawned()) {
            // Regular metadata
            npc.getEntity().setMetadata("level", new FixedMetadataValue(plugin, stats.getLevel()));
            npc.getEntity().setMetadata("original_name", new FixedMetadataValue(plugin, name));
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(plugin, stats.getNpcType().name()));
            npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
            
            // Set custom stats in metadata
            npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(plugin, stats.getPhysicalDamage()));
            npc.getEntity().setMetadata("magic_damage", new FixedMetadataValue(plugin, stats.getMagicDamage()));
            npc.getEntity().setMetadata("armor", new FixedMetadataValue(plugin, stats.getArmor()));
            npc.getEntity().setMetadata("magic_resist", new FixedMetadataValue(plugin, stats.getMagicResist()));

            // CRITICAL: Set the NPC as vulnerable to trigger red damage flash
            npc.data().setPersistent(NPC.Metadata.DEFAULT_PROTECTED, false);
            
            // CRITICAL: Set living entity attributes if this is a player-type NPC
            if (npc.getEntity() instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) npc.getEntity();
                
                // Make sure they're not invulnerable (critical for red flash)
                livingEntity.setInvulnerable(false);
                
                // Set health attributes
                if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    // Set max health
                    livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getMaxHealth());
                    // Set current health to max
                    livingEntity.setHealth(stats.getMaxHealth());
                }
            }
        }
    }
    
    /**
     * Send a message from the NPC to a player
     * 
     * @param player The player
     * @param message The message
     */
    protected void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.YELLOW + name + ": " + ChatColor.WHITE + message);
    }
    
    /**
     * Start a dialogue with a player
     * 
     * @param player The player
     * @param dialogueId The dialogue ID
     */
    protected void startDialogue(Player player, String dialogueId) {
        DialogueNode dialogue = DialogueManager.getInstance().getDialogue(dialogueId);
        if (dialogue != null) {
            DialogueManager.getInstance().startDialogue(player, this, dialogue);
        } else {
            sendMessage(player, "Sorry, I have nothing to say right now.");
        }
    }
    
    /**
     * Setup a default dialogue for this NPC
     * 
     * @param dialogueId The dialogue ID
     * @param text The initial dialogue text
     */
    protected void setupDefaultDialogue(String dialogueId, String text) {
        DialogueManager dialogueManager = DialogueManager.getInstance();
        if (!dialogueManager.hasDialogue(dialogueId)) {
            DialogueNode node = new DialogueNode(text);
            dialogueManager.registerDialogue(dialogueId, node);
        }
    }
}