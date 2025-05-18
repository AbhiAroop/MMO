package com.server.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.entities.npc.CombatHandler;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.behaviors.CombatBehavior;
import com.server.entities.npc.types.BaseNPC;
import com.server.entities.npc.types.CombatNPC;
import com.server.entities.npc.types.PassiveNPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

/**
 * Listener for damage events involving NPCs
 */
public class NPCDamageListener implements Listener {
    
    private final Main plugin;
    
    public NPCDamageListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNPCDamage(EntityDamageByEntityEvent event) {
        // Print debug information
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("NPCDamageEvent: Entity=" + event.getEntity().getType() + 
                                ", Damager=" + event.getDamager().getType() +
                                ", Damage=" + event.getDamage());
        }

        Entity target = event.getEntity();
        
        // Check if the damaged entity is an NPC
        if (!CitizensAPI.getNPCRegistry().isNPC(target)) {
            return;
        }
        
        // Get the NPC
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(target);
        if (npc == null) {
            return;
        }
        
        // Print additional debug
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("NPC damage detected: NPC=" + npc.getName() +
                                ", IsSpawned=" + npc.isSpawned());
        }
        
        // Get the damager
        Entity damager = event.getDamager();
        
        // Only handle damage from players for now
        if (!(damager instanceof Player)) {
            return;
        }
        
        Player player = (Player) damager;
        
        // Always cancel the vanilla damage event, we'll handle it manually
        event.setCancelled(true);
        
        // Print more debug information about NPC type
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("NPC metadata check: " + 
                                "passive=" + target.hasMetadata("passive_npc") +
                                ", combat=" + target.hasMetadata("combat_npc") + 
                                ", hostile=" + target.hasMetadata("hostile_npc"));
        }
        
        double damage = event.getDamage();
        boolean processed = false;
        
        // Get the correct reference to the CombatHandler
        CombatHandler sharedCombatHandler = NPCManager.getInstance().getCombatHandler();
        
        // Try to find the NPC handler based on ID first
        String npcId = findNpcIdByUuid(npc.getUniqueId());
        if (npcId != null) {
            // Get the interaction handler which should be the BaseNPC instance
            Object handler = NPCManager.getInstance().getInteractionHandler(npcId);
            
            // Handle damage based on NPC type using handlers
            if (handler instanceof BaseNPC) {
                BaseNPC baseNpc = (BaseNPC) handler;
                
                if (handler instanceof PassiveNPC) {
                    // Passive NPC - run away when attacked
                    PassiveNPC passiveNPC = (PassiveNPC) baseNpc;
                    passiveNPC.onDamage(player, damage);
                    processed = true;
                    
                    // Apply knockback
                    applyKnockback(npc.getEntity(), player, 0.4);
                } 
                else if (handler instanceof CombatNPC) {
                    // Combat NPC - fight back
                    CombatNPC combatNPC = (CombatNPC) baseNpc;
                    
                    // First try to use behavior
                    CombatBehavior combatBehavior = (CombatBehavior) combatNPC.getBehavior("combat");
                    if (combatBehavior != null) {
                        // Let the behavior handle the damage
                        combatBehavior.onDamage(player, damage);
                        processed = true;
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Processed damage using CombatBehavior for NPC " + npc.getName());
                        }
                    } else {
                        // Fallback to direct handler method
                        combatNPC.onDamage(player, damage);
                        processed = true;
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Processed damage using CombatNPC.onDamage for NPC " + npc.getName());
                        }
                    }
                    
                    // Apply knockback
                    applyKnockback(npc.getEntity(), player, 0.5);
                }
            }
        }
        
        // If not processed yet, try a direct metadata approach
        if (!processed) {
            if (target.hasMetadata("passive_npc")) {
                // Try direct metadata-based processing for passive NPCs
                PassiveNPC passiveNPC = findPassiveNPCById(npcId);
                if (passiveNPC != null) {
                    passiveNPC.onDamage(player, damage);
                    processed = true;
                    
                    // Apply knockback
                    applyKnockback(npc.getEntity(), player, 0.4);
                }
            } 
            else if (target.hasMetadata("combat_npc") || target.hasMetadata("hostile_npc")) {
                // Try direct metadata-based processing for combat NPCs
                CombatNPC combatNPC = findCombatNPCById(npcId);
                if (combatNPC != null) {
                    combatNPC.onDamage(player, damage);
                    processed = true;
                    
                    // Apply knockback
                    applyKnockback(npc.getEntity(), player, 0.5);
                    
                    // Debug log that we processed through the handler
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Processed damage through CombatNPC handler: " + npcId);
                    }
                } else {
                    // As a fallback, use the shared combat handler
                    if (target.hasMetadata("combat_npc") || target.hasMetadata("hostile_npc")) {
                        // Apply damage
                        sharedCombatHandler.applyDamageToNPC(null, npc, damage, false);
                        
                        // IMPORTANT: The reason NPCs weren't fighting back is we were missing this crucial line:
                        // Start combat behavior with the player as target
                        if (target.hasMetadata("hostile_npc")) {
                            sharedCombatHandler.startHostileCombatBehavior(npc, player);
                        } else {
                            sharedCombatHandler.startCombatBehavior(npc, player);
                        }
                        
                        processed = true;
                        
                        // Apply knockback
                        applyKnockback(npc.getEntity(), player, 0.5);
                        
                        // Debug log that we used fallback
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Used fallback CombatHandler for NPC: " + npc.getName());
                        }
                    }
                }
            }
        }
        
        // If we couldn't process through handlers, use a direct approach as fallback
        if (!processed) {
            // Log this fallback case
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Using fallback damage handling for NPC: " + npc.getName());
            }
            
            // Force basic damage indicator
            if (plugin.getDamageIndicatorManager() != null) {
                plugin.getDamageIndicatorManager().spawnDamageIndicator(
                    target.getLocation().add(0, 1, 0), 
                    (int) damage, 
                    false);
            }
            
            // Apply knockback
            applyKnockback(npc.getEntity(), player, 0.5);
            
            // Play hurt sound
            target.getWorld().playSound(
                target.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_HURT,
                0.8f, 1.0f
            );
            
            // Last resort - use direct damage application through combat handler
            sharedCombatHandler.applyDamageToNPC(null, npc, damage, false);
            sharedCombatHandler.startCombatBehavior(npc, player);
        }
    }
    
    /**
     * Apply knockback to an entity
     * 
     * @param entity The entity to knock back
     * @param source The source of the knockback
     * @param strength The strength of the knockback
     */
    private void applyKnockback(Entity entity, Entity source, double strength) {
        if (entity instanceof LivingEntity) {
            Vector knockback = entity.getLocation().subtract(source.getLocation()).toVector().normalize();
            knockback.multiply(strength);
            knockback.setY(0.2); // Add slight vertical component
            
            entity.setVelocity(entity.getVelocity().add(knockback));
        }
    }
    
    /**
     * Find an NPC ID by its UUID
     * 
     * @param uuid The UUID of the NPC
     * @return The NPC ID, or null if not found
     */
    private String findNpcIdByUuid(java.util.UUID uuid) {
        NPCManager manager = NPCManager.getInstance();
        for (String id : manager.getIds()) {
            NPC npc = manager.getNPC(id);
            if (npc != null && npc.getUniqueId().equals(uuid)) {
                return id;
            }
        }
        return null;
    }
    
    /**
     * Find a PassiveNPC instance by its ID
     * 
     * @param id The NPC ID
     * @return The PassiveNPC, or null if not found or not a PassiveNPC
     */
    private PassiveNPC findPassiveNPCById(String id) {
        NPCManager manager = NPCManager.getInstance();
        NPC npc = manager.getNPC(id);
        if (npc != null && npc.isSpawned()) {
            Object handler = manager.getInteractionHandler(id);
            if (handler instanceof PassiveNPC) {
                return (PassiveNPC) handler;
            }
        }
        return null;
    }
    
    /**
     * Find a CombatNPC instance by its ID
     * 
     * @param id The NPC ID
     * @return The CombatNPC, or null if not found or not a CombatNPC
     */
    private CombatNPC findCombatNPCById(String id) {
        NPCManager manager = NPCManager.getInstance();
        NPC npc = manager.getNPC(id);
        if (npc != null && npc.isSpawned()) {
            Object handler = manager.getInteractionHandler(id);
            if (handler instanceof CombatNPC) {
                return (CombatNPC) handler;
            }
        }
        return null;
    }
}