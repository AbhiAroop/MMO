package com.server.entities.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

/**
 * Listens for interactions with NPCs
 */
public class NPCInteractionListener implements Listener {
    
    private final NPCManager npcManager;
    
    /**
     * Create a new NPCInteractionListener
     * 
     * @param npcManager The NPCManager instance
     */
    public NPCInteractionListener(NPCManager npcManager) {
        this.npcManager = npcManager;
    }
    
    /**
     * Handle left click interactions with NPCs
     */
    @EventHandler
    public void onNPCLeftClick(NPCLeftClickEvent event) {
        Player player = event.getClicker();
        NPC npc = event.getNPC();
        
        npcManager.handleInteraction(player, npc, false);
    }
    
    /**
     * Handle right click interactions with NPCs
     */
    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        NPC npc = event.getNPC();
        
        npcManager.handleInteraction(player, npc, true);
    }
}