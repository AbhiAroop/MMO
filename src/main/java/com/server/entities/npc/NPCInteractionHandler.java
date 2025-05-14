package com.server.entities.npc;

import org.bukkit.entity.Player;

import net.citizensnpcs.api.npc.NPC;

/**
 * Interface for handling NPC interactions
 */
public interface NPCInteractionHandler {
    
    /**
     * Handle an interaction with an NPC
     * 
     * @param player The player who interacted
     * @param npc The NPC that was interacted with
     * @param rightClick Whether this was a right click interaction
     */
    void onInteract(Player player, NPC npc, boolean rightClick);
}