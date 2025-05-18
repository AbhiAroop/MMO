package com.server.entities.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

/**
 * Listens for interactions with NPCs and forwards them to the appropriate handlers
 */
public class NPCInteractionListener implements Listener {
    
    private final NPCManager manager;
    
    /**
     * Create a new NPCInteractionListener
     * 
     * @param manager The NPCManager instance
     */
    public NPCInteractionListener(NPCManager manager) {
        this.manager = manager;
    }
    
    /**
     * Handle right click interactions with NPCs
     * 
     * @param event The NPCRightClickEvent
     */
    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        NPC npc = event.getNPC();
        
        manager.handleInteraction(player, npc, true);
    }
    
    /**
     * Handle left click interactions with NPCs
     * 
     * @param event The NPCLeftClickEvent
     */
    @EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        Player player = event.getClicker();
        NPC npc = event.getNPC();
        
        manager.handleInteraction(player, npc, false);
    }
}