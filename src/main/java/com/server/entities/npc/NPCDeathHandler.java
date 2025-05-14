package com.server.entities.npc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.npc.NPC;

/**
 * Handles NPC death events
 */
public class NPCDeathHandler implements Listener {
    
    private final NPCManager npcManager;
    
    public NPCDeathHandler(NPCManager npcManager) {
        this.npcManager = npcManager;
    }
    
    @EventHandler
    public void onNPCDeath(NPCDeathEvent event) {
        NPC npc = event.getNPC();
        
        // Check if this is one of our managed NPCs
        String npcId = null;
        for (String id : npcManager.getIds()) {
            if (npcManager.getNPC(id) == npc) {
                npcId = id;
                break;
            }
        }
        
        if (npcId != null) {
            // Clean up combat behavior if this was a combat NPC
            NPCInteractionHandler handler = npcManager.getInteractionHandler(npcId);
            if (handler instanceof CombatHandler) {
                ((CombatHandler) handler).stopCombatBehavior(npc.getUniqueId());
            }
            
            // Respawn the NPC after a delay if needed
            // This code would go here if you want NPCs to respawn
        }
    }
}