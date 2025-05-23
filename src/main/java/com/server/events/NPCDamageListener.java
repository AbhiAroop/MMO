package com.server.events;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.behaviors.CombatBehavior;
import com.server.entities.npc.types.CombatNPC;
import com.server.entities.npc.types.PassiveNPC;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

/**
 * Listener for damage events involving NPCs
 */
public class NPCDamageListener implements Listener {
    
    private final Main plugin;
    private final java.util.Map<UUID, Long> lastDamageTimeMap = new java.util.HashMap<>();
    private static final long DAMAGE_COOLDOWN = 500; // 500ms cooldown between damage events
    
    public NPCDamageListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Process damage for a PassiveNPC
     */
    private void processDamageForPassiveNPC(PassiveNPC passiveNPC, Player player, double damage) {
        if (passiveNPC == null || !passiveNPC.isSpawned()) return;
        
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"Processing passive NPC damage: " + 
                    passiveNPC.getName() + " from player " + player.getName() + 
                    " for " + damage + " damage");
        }
        
        // Get NPC instance
        NPC npc = passiveNPC.getNPC();
        if (npc == null || !npc.isSpawned()) return;
        
        // Apply vanilla damage effect (this will trigger the red flash)
        if (npc.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) npc.getEntity();
            living.damage(0.1);
            living.playEffect(EntityEffect.HURT);
        }
        
        // Let the passive NPC handle the damage with our custom system
        passiveNPC.onDamage(player, damage);
        
        // CRITICAL FIX: Also update the Combat Handler's centralized health tracking
        // This ensures consistent health storage across the system
        double currentHealth = 0;
        if (npc.getEntity().hasMetadata("current_health")) {
            currentHealth = npc.getEntity().getMetadata("current_health").get(0).asDouble();
            // Update the CombatHandler's health map with this value
            NPCManager.getInstance().getCombatHandler().setHealth(npc.getUniqueId(), currentHealth);
            
            if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                plugin.debugLog(DebugSystem.NPC,"Updated CombatHandler health tracking for " + 
                    passiveNPC.getName() + " to " + currentHealth);
            }
        }
        
        // Apply knockback
        applyKnockback(npc.getEntity(), player, 0.4);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onNPCDamage(EntityDamageByEntityEvent event) {
        // Print debug information
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"Entity damaged: " + event.getEntity().getName() + 
                                " by " + event.getDamager().getName());
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
        
        // CRITICAL FIX: Check for invulnerable NPCs
        if (npc.getEntity().hasMetadata("invulnerable") || npc.getEntity().isInvulnerable()) {
            // Cancel damage for invulnerable NPCs like dialogue NPCs
            event.setCancelled(true);
            plugin.debugLog(DebugSystem.NPC,"Cancelled damage to invulnerable NPC: " + npc.getName());
            return;
        }
        
        // Get the damager
        Entity damager = event.getDamager();
        
        // Only handle damage from players for now
        if (!(damager instanceof Player)) {
            return;
        }
        
        Player player = (Player) damager;
        
        // Check for damage cooldown
        UUID npcUUID = npc.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (lastDamageTimeMap.containsKey(npcUUID)) {
            long lastDamageTime = lastDamageTimeMap.get(npcUUID);
            long timeSinceLastDamage = currentTime - lastDamageTime;
            
            if (timeSinceLastDamage < DAMAGE_COOLDOWN) {
                // Still in immunity period
                if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                    plugin.debugLog(DebugSystem.NPC,"NPC in damage cooldown: " + 
                        timeSinceLastDamage + "ms since last hit (needs " + DAMAGE_COOLDOWN + "ms)");
                }
                return;
            }
        }
        
        // Record this damage time
        lastDamageTimeMap.put(npcUUID, currentTime);
        
        // CRITICAL: Allow actual damage to go through for Citizens to handle the red damage effect
        double originalDamage = event.getDamage();
        
        // Don't modify the damage - let Citizens handle it naturally
        // This is key for getting the red flash effect
        
        // Get current health and entity
        LivingEntity livingEntity = null;
        double currentHealth = 0;
        
        if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            livingEntity = (LivingEntity) npc.getEntity();
            currentHealth = livingEntity.getHealth();
        }
        
        // Create final copies for use in the lambda
        final LivingEntity finalLivingEntity = livingEntity;
        final double finalCurrentHealth = currentHealth;
        
        // Get the actual damage amount from the player's stats
        double playerDamage = getDamageFromPlayer(player);
        
        // CRITICAL FIX: Apply attack charge scaling to damage
        float attackCharge = player.getAttackCooldown();
        
        // Apply the same damage scaling formula as in CombatListener
        double scaledDamage;
        if (attackCharge <= 0.01) {
            scaledDamage = Math.max(0.5, playerDamage * 0.01); // Minimum 0.5 damage or 1% of full damage
        } else {
            scaledDamage = Math.max(0.5, playerDamage * attackCharge); // Scale with attack charge
        }
        
        // Apply critical hit if applicable (same conditions as CombatListener)
        boolean isCritical = attackCharge >= 0.9f && !player.isOnGround() && player.getFallDistance() > 0.0f;
        if (isCritical) {
            scaledDamage *= 2.0; // Apply critical hit multiplier
        }
        
        // Debug logging
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"Player " + player.getName() + " attacking NPC with charge: " + 
                attackCharge + ", base damage: " + playerDamage + 
                ", scaled damage: " + scaledDamage + ", critical: " + isCritical);
        }
        
        // Use this scaled damage in the following code
        final double finalPlayerDamage = scaledDamage;
        final boolean finalIsCritical = isCritical;
        
        // Process the damage in our custom system right after the vanilla system
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Make sure the NPC is still valid
            if (!npc.isSpawned() || npc.getEntity() == null) return;
            
            // CRITICAL FIX: Restore health after vanilla damage is applied
            if (finalLivingEntity != null && finalLivingEntity.isValid() && !finalLivingEntity.isDead()) {
                // Reset health back to where it was before damage
                finalLivingEntity.setHealth(Math.min(finalLivingEntity.getMaxHealth(), finalCurrentHealth));
            }
            
            // Process our custom damage
            String npcId = findNpcIdByUuid(npc.getUniqueId());
            if (npcId != null) {
                Object handler = NPCManager.getInstance().getInteractionHandler(npcId);
                
                if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                    plugin.debugLog(DebugSystem.NPC,"Processing custom damage for NPC " + npcId + 
                        ", handler type: " + (handler != null ? handler.getClass().getSimpleName() : "null"));
                }
                
                // Handle damage differently based on NPC type
                if (handler instanceof PassiveNPC) {
                    PassiveNPC passiveNPC = (PassiveNPC) handler;
                    processDamageForPassiveNPC(passiveNPC, player, finalPlayerDamage);
                } 
                else if (handler instanceof CombatNPC) {
                    CombatNPC combatNPC = (CombatNPC) handler;
                    CombatBehavior combatBehavior = (CombatBehavior) combatNPC.getBehavior("combat");
                    
                    if (combatBehavior != null) {
                        combatBehavior.onDamage(player, finalPlayerDamage);
                    } else {
                        combatNPC.onDamage(player, finalPlayerDamage);
                    }
                    
                    // Apply knockback
                    applyKnockback(npc.getEntity(), player, 0.3);
                    
                    // Add critical hit visual effects
                    if (finalIsCritical) {
                        addCriticalHitEffects(npc.getEntity().getLocation());
                    }
                }
            } else {
                // If no NPC ID found, apply generic damage
                plugin.debugLog(DebugSystem.NPC,"NPC ID not found for UUID " + npc.getUniqueId() + 
                    ". Applying generic damage process.");
                
                // Get current health from metadata or default
                double health = 100.0;
                if (npc.getEntity().hasMetadata("current_health")) {
                    health = npc.getEntity().getMetadata("current_health").get(0).asDouble();
                }
                
                // Apply damage
                health = Math.max(0, health - finalPlayerDamage);
                npc.getEntity().setMetadata("current_health", 
                    new FixedMetadataValue(plugin, health));
                
                // Update nameplate
                NPCManager.getInstance().updateNameplate(npc, health, 100.0);
            }
        }, 1L); // Just 1 tick later
    }

    // Add this new helper method for critical hit effects
    private void addCriticalHitEffects(Location location) {
        location.getWorld().playSound(
            location,
            Sound.ENTITY_PLAYER_ATTACK_CRIT,
            1.0f, 1.0f
        );
        
        location.getWorld().spawnParticle(
            org.bukkit.Particle.CRIT,
            location.add(0, 1, 0),
            10, 0.5, 0.5, 0.5, 0.1
        );
    }

    

    /**
     * Apply knockback to an entity
     */
    private void applyKnockback(Entity entity, Entity source, double strength) {
        if (entity instanceof LivingEntity) {
            // Calculate knockback direction away from the source
            Vector knockback = entity.getLocation().subtract(source.getLocation()).toVector().normalize();
            
            // Add some upward component for better visual effect
            knockback.setY(Math.max(0.2, knockback.getY() * 0.6));
            
            // Apply the knockback with the specified strength
            knockback.multiply(strength);
            
            // Use a delayed task to apply knockback after damage is processed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    entity.setVelocity(entity.getVelocity().add(knockback));
                }
            }, 1L);
            
            if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                plugin.debugLog(DebugSystem.NPC,"Applied knockback with strength " + strength + " and vector " + knockback);
            }
        }
    }

    /**
     * Get damage from a player based on their stats
     */
    private double getDamageFromPlayer(Player player) {
        // Default damage if we can't get player's stats
        double damage = 5.0;
        
        // Try to get player's actual damage from their stats
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                PlayerProfile profile = profileManager.getProfiles(player.getUniqueId())[activeSlot];
                if (profile != null) {
                    damage = profile.getStats().getPhysicalDamage();
                    
                    // Account for charge level if available
                    if (player.hasMetadata("last_attack_charge")) {
                        float chargePercent = player.getMetadata("last_attack_charge").get(0).asFloat();
                        damage *= Math.max(0.2, chargePercent); // Min 20% damage
                    }
                }
            }
        }
        
        return damage;
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