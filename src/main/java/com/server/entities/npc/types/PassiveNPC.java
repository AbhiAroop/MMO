package com.server.entities.npc.types;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.npc.NPC;
// Remove incorrect import
// import net.citizensnpcs.trait.Pushable;

/**
 * A passive NPC that will run away when attacked
 */
public class PassiveNPC extends BaseNPC {
    
    private String dialogueId;
    private boolean isRunning = false;
    
    /**
     * Create a new passive NPC
     * 
     * @param id The unique ID
     * @param name The display name
     * @param stats The NPC stats
     */
    public PassiveNPC(String id, String name, NPCStats stats) {
        super(id, name, stats);
        this.dialogueId = "passive_" + id;
        
        // Setup a default dialogue
        setupDefaultDialogue(dialogueId, "Hello there! Please don't hurt me.");
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        this.npc = NPCManager.getInstance().createNPC(id, name, location, skin, true);
        
        // Make it vulnerable to attack
        if (npc.isSpawned()) {
            npc.getEntity().setInvulnerable(false);
        }
        
        // Instead of using Pushable trait which doesn't exist,
        // we'll make the NPC move when collided with using our own logic
        // This will be implemented in a separate collision handler
        
        // Set as passive NPC
        if (npc.isSpawned()) {
            npc.getEntity().setMetadata("passive_npc", new FixedMetadataValue(plugin, true));
            
            // Set other metadata to control behavior
            npc.getEntity().setMetadata("can_be_pushed", new FixedMetadataValue(plugin, true));
        }
        
        applyBaseMetadata();
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
        
        return npc;
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        if (rightClick) {
            // Start dialogue on right-click if not running
            if (!isRunning) {
                startDialogue(player, dialogueId);
            } else {
                sendMessage(player, "Leave me alone!");
            }
        } else if (!isRunning) {
            // Just a greeting on left-click
            sendMessage(player, "Hello there! Right-click to talk to me.");
        }
    }
    
    /**
     * Called when this NPC is damaged
     * 
     * @param player The attacking player
     * @param damage The damage amount
     */
    public void onDamage(Player player, double damage) {
        if (isRunning || !npc.isSpawned()) return;
        
        isRunning = true;
        
        // Apply visual damage indicators
        if (plugin.getDamageIndicatorManager() != null) {
            plugin.getDamageIndicatorManager().spawnDamageIndicator(
                npc.getEntity().getLocation().add(0, 1, 0),
                (int) damage,
                false);
        }
        
        // Play hurt sound and animation
        npc.getEntity().getWorld().playSound(
            npc.getEntity().getLocation(),
            org.bukkit.Sound.ENTITY_PLAYER_HURT,
            0.8f, 1.0f
        );
        
        // Run away from the player
        sendMessage(player, "Aaaah! Help!");
        
        // Calculate direction away from player
        Vector direction = npc.getEntity().getLocation().subtract(player.getLocation()).toVector().normalize();
        
        // Run away for 5 seconds
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 5 * 20; // 5 seconds
            
            @Override
            public void run() {
                if (!npc.isSpawned() || ticks++ > maxTicks) {
                    isRunning = false;
                    this.cancel();
                    return;
                }
                
                // Only update movement every few ticks to reduce jitter
                if (ticks % 10 == 0) {
                    // Calculate new run away position
                    Location npcLoc = npc.getEntity().getLocation();
                    Location runTo = npcLoc.clone().add(direction.clone().multiply(10));
                    
                    // Use Citizens pathfinding
                    npc.getNavigator().getLocalParameters().speedModifier(1.5f);
                    npc.getNavigator().setTarget(runTo);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Called when a player gets close to this NPC
     * This simulates the "pushable" behavior without using the trait
     * 
     * @param player The player who is close
     */
    public void onPlayerNearby(Player player) {
        if (isRunning || !npc.isSpawned()) return;
        
        // Calculate direction away from player
        Vector direction = npc.getEntity().getLocation().subtract(player.getLocation()).toVector().normalize();
        
        // Step slightly aside
        Location npcLoc = npc.getEntity().getLocation();
        Location stepAside = npcLoc.clone().add(direction.clone().multiply(1.5));
        
        // Make the NPC step aside briefly
        npc.getNavigator().getLocalParameters().speedModifier(1.0f);
        npc.getNavigator().setTarget(stepAside);
    }
    
    /**
     * Set the dialogue for this NPC
     * 
     * @param dialogueId The dialogue ID
     */
    public void setDialogue(String dialogueId) {
        this.dialogueId = dialogueId;
        setupDefaultDialogue(dialogueId, "Hello there! Please don't hurt me.");
    }
}