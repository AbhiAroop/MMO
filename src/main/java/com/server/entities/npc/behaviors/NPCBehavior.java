package com.server.entities.npc.behaviors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.npc.NPC;

/**
 * Base interface for NPC behavior
 */
public interface NPCBehavior {
    /**
     * Initialize behavior for an NPC
     * 
     * @param npc The NPC
     */
    void initialize(NPC npc);
    
    /**
     * Update the behavior (called each tick)
     */
    void update();
    
    /**
     * Handle when this NPC receives damage
     * 
     * @param source The source of damage
     * @param amount The amount of damage
     * @return Whether the damage event should be cancelled
     */
    boolean onDamage(Entity source, double amount);
    
    /**
     * Handle interactions from players
     * 
     * @param player The player interacting
     * @param isRightClick Whether this was a right-click interaction
     */
    void onInteract(Player player, boolean isRightClick);
    
    /**
     * Clean up resources when the behavior is no longer needed
     */
    void cleanup();
    
    /**
     * Get the priority of this behavior (higher priority behaviors run first)
     * 
     * @return The priority
     */
    int getPriority();
}