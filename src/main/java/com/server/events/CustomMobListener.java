package com.server.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.entities.CustomMobStats;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Handles events related to custom mobs
 */
public class CustomMobListener implements Listener {
    
    private final Main plugin;
    private final Map<UUID, BukkitTask> attackTasks = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    
    public CustomMobListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle general damage to custom mobs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Update the nameplate after damage
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isDead()) {
                        plugin.getCustomEntityManager().updateEntityNameplate(entity);
                    }
                }
            }.runTaskLater(plugin, 1L);
            
            // For proper visual feedback, play hurt animation when mob takes damage
            // Note: This is now handled by the individual custom mob classes for specific behaviors
            // But we'll keep this as a fallback for general damage events
            if (!entity.isDead()) {
                plugin.getCustomEntityManager().playAnimation(entity, "hurt");
            }
        }
    }
    
    /**
     * Handle damage from players to custom mobs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle player attacking custom mob
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Get the player's active profile
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) return;
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                // Standard damage calculation already happens in the event
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info(player.getName() + " attacked a custom mob: " + 
                            entity.getCustomName() + " for " + event.getFinalDamage() + " damage");
                }
            }
        }
    }

    /**
     * Handle early entity death to play death animations
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeathEarly(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Play death animation and handle death sequence
            plugin.getCustomEntityManager().handleDeath(entity);
            
            // Clean up attack task if exists
            UUID entityId = entity.getUniqueId();
            if (attackTasks.containsKey(entityId)) {
                attackTasks.get(entityId).cancel();
                attackTasks.remove(entityId);
                lastAttackTime.remove(entityId);
            }
        }
    }

    /**
     * Handle entity death for loot and rewards
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            CustomMobStats stats = plugin.getCustomEntityManager().getMobStats(entity);
            if (stats == null) return;
            
            // Handle custom drops
            if (entity.getKiller() != null) {
                Player player = entity.getKiller();
                
                // Get the player's active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                
                if (profile != null) {
                    // Award XP
                    int expReward = calculateExpReward(stats);
                    player.sendMessage("§a+" + expReward + " XP from defeating " + stats.getName());
                    
                    // Award gold drops
                    int goldAmount = stats.getMinGoldDrop();
                    if (stats.getMaxGoldDrop() > stats.getMinGoldDrop()) {
                        goldAmount += (int)(Math.random() * (stats.getMaxGoldDrop() - stats.getMinGoldDrop()));
                    }
                    
                    // Add currency to player
                    profile.addUnits(goldAmount);
                    player.sendMessage("§6+" + goldAmount + " Gold");
                    
                    // Clear default drops and set custom ones
                    event.getDrops().clear();
                    
                    // Log the kill for debugging
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info(player.getName() + " killed custom mob: " + 
                                stats.getName() + " (Level " + stats.getLevel() + ")");
                    }
                }
            }
        }
    }
    
    /**
     * Handle entity targeting event
     * Used specifically to cancel targeting for custom mobs
     * since we handle targeting in their individual behavior classes
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // If this is a custom mob, cancel vanilla targeting behavior
        // Our custom implementations handle targeting
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Calculate experience rewards based on mob type and level
     * @param stats The mob stats
     * @return The calculated experience reward
     */
    private int calculateExpReward(CustomMobStats stats) {
        int baseExp = stats.getExpReward();
        int levelMultiplier = stats.getLevel();
        
        // Apply multipliers based on mob type
        switch(stats.getMobType()) {
            case BOSS:
                return baseExp * levelMultiplier * 5; // 5x multiplier for bosses
            case MINIBOSS:
                return baseExp * levelMultiplier * 3; // 3x multiplier for minibosses
            case ELITE:
                return baseExp * levelMultiplier * 2; // 2x multiplier for elite mobs
            case NORMAL:
            default:
                return baseExp * levelMultiplier;
        }
    }
}