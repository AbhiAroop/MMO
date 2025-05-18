package com.server.entities.npc.dialogue;

import org.bukkit.entity.Player;

/**
 * Interface for actions that can be performed when a player selects a dialogue response
 */
@FunctionalInterface
public interface DialogueAction {
    /**
     * Execute the action
     * 
     * @param player The player who selected the response
     */
    void execute(Player player);
}