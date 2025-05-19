package com.server.entities.npc;

import org.bukkit.Location;

import com.server.entities.npc.types.CombatNPC;
import com.server.entities.npc.types.DialogueNPC;
import com.server.entities.npc.types.HostileNPC;
import com.server.entities.npc.types.PassiveNPC;

/**
 * Factory for creating different types of NPCs
 */
public class NPCFactory {
    
    private static NPCFactory instance;
    
    private NPCFactory() {
        // Private constructor for singleton
    }
    
    /**
     * Get the NPCFactory instance
     * 
     * @return The singleton instance
     */
    public static NPCFactory getInstance() {
        if (instance == null) {
            instance = new NPCFactory();
        }
        return instance;
    }
    
    /**
     * Create a dialogue NPC that can't be attacked
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use
     * @return The created NPC
     */
    public DialogueNPC createDialogueNPC(String id, String name, Location location, String skin) {
        DialogueNPC npc = new DialogueNPC(id, name, "dialogue_" + id);
        npc.spawn(location, skin);
        return npc;
    }
    
    /**
     * Create a passive NPC that runs away when attacked
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use
     * @return The created NPC
     */
    public PassiveNPC createPassiveNPC(String id, String name, Location location, String skin) {
        NPCStats stats = new NPCStats();
        stats.setMaxHealth(50); // Lower default health for passive NPCs
        stats.setNpcType(NPCType.NORMAL);
        
        PassiveNPC npc = new PassiveNPC(id, name, stats);
        npc.spawn(location, skin);
        return npc;
    }
    
    /**
     * Create a combat NPC that fights back when attacked
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use
     * @param health The max health
     * @param damage The physical damage
     * @return The created NPC
     */
    public CombatNPC createCombatNPC(String id, String name, Location location, String skin, double health, int damage) {
        NPCStats stats = new NPCStats();
        stats.setMaxHealth(health);
        stats.setPhysicalDamage(damage);
        stats.setNpcType(NPCType.NORMAL);
        
        CombatNPC npc = new CombatNPC(id, name, stats);
        
        // Critical fix: Create NPC with proper player entity type
        net.citizensnpcs.api.npc.NPC citizensNPC = NPCManager.getInstance().getNPCRegistry().createNPC(
            org.bukkit.entity.EntityType.PLAYER, 
            name, 
            location
        );
        
        // Apply skin if provided
        if (skin != null && !skin.isEmpty()) {
            net.citizensnpcs.trait.SkinTrait skinTrait = citizensNPC.getOrAddTrait(net.citizensnpcs.trait.SkinTrait.class);
            skinTrait.setSkinName(skin, true);
        }
        
        // Apply look close trait
        citizensNPC.getOrAddTrait(net.citizensnpcs.trait.LookClose.class).lookClose(true);
        
        // IMPORTANT: Make sure the NPC is not invulnerable (crucial for damage effects)
        if (citizensNPC.isSpawned()) {
            citizensNPC.getEntity().setInvulnerable(false);
        }
        
        // Manually assign the Citizens NPC to our CombatNPC
        npc.setNPC(citizensNPC);
        
        // Finish initializing our CombatNPC
        npc.finalizeSpawn();
        
        // Register the interaction handler
        NPCManager.getInstance().registerInteractionHandler(id, npc);
        
        return npc;
    }
    
    /**
     * Create a hostile NPC that attacks players and other NPCs
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use
     * @param health The max health
     * @param damage The physical damage
     * @return The created NPC
     */
    public HostileNPC createHostileNPC(String id, String name, Location location, String skin, double health, int damage) {
        NPCStats stats = new NPCStats();
        stats.setMaxHealth(health);
        stats.setPhysicalDamage(damage);
        stats.setNpcType(NPCType.ELITE); // Hostile NPCs are ELITE by default
        
        HostileNPC npc = new HostileNPC(id, name, stats);
        npc.spawn(location, skin);
        return npc;
    }
    
    /**
     * Create a custom hostile NPC with advanced stats
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use
     * @param stats The custom stats
     * @return The created NPC
     */
    public HostileNPC createCustomHostileNPC(String id, String name, Location location, String skin, NPCStats stats) {
        HostileNPC npc = new HostileNPC(id, name, stats);
        npc.spawn(location, skin);
        return npc;
    }
    
    /**
     * Create a custom combat NPC with advanced stats
     * 
     * @param id The unique ID
     * @param name The display name
     * @param location The spawn location
     * @param skin The skin to use 
     * @param stats The custom stats
     * @return The created NPC
     */
    public CombatNPC createCustomCombatNPC(String id, String name, Location location, String skin, NPCStats stats) {
        CombatNPC npc = new CombatNPC(id, name, stats);
        npc.spawn(location, skin);
        return npc;
    }
}