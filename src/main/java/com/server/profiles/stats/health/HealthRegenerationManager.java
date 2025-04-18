package com.server.profiles.stats.health;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

public class HealthRegenerationManager {
    private final Main plugin;
    private final Map<UUID, Integer> regenTasks = new HashMap<>();
    private final int TICKS_PER_SECOND = 20;
    
    public HealthRegenerationManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start tracking health regeneration for a player
     */
    public void startTracking(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Stop existing task if there is one
        stopTracking(player);
        
        // Create a new task for health regeneration
        int taskId = new BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                // If player is offline, cancel the task
                if (!player.isOnline()) {
                    this.cancel();
                    regenTasks.remove(playerId);
                    return;
                }
                
                // Get active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(playerId);
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(playerId)[activeSlot];
                if (profile == null) return;
                
                // Only apply regeneration every second (20 ticks)
                tickCounter++;
                if (tickCounter >= TICKS_PER_SECOND) {
                    tickCounter = 0;
                    
                    // Apply health regeneration
                    applyHealthRegeneration(player, profile.getStats());
                }
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();
        
        // Store the task ID
        regenTasks.put(playerId, taskId);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Started health regeneration tracking for " + player.getName());
        }
    }
    
    /**
     * Stop tracking health regeneration for a player
     */
    public void stopTracking(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task if there is one
        if (regenTasks.containsKey(playerId)) {
            plugin.getServer().getScheduler().cancelTask(regenTasks.get(playerId));
            regenTasks.remove(playerId);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Stopped health regeneration tracking for " + player.getName());
            }
        }
    }
    
    /**
     * Apply health regeneration based on player stats
     */
    private void applyHealthRegeneration(Player player, PlayerStats stats) {
        // Only apply if player is not at full health
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        
        if (currentHealth < maxHealth) {
            int regenAmount = stats.getHealthRegen();
            
            // Calculate new health value
            double newHealth = Math.min(currentHealth + regenAmount, maxHealth);
            
            // Apply the health change
            player.setHealth(newHealth);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Applied health regeneration to " + player.getName() + 
                                    ": +" + regenAmount + " (" + currentHealth + " -> " + newHealth + ")");
            }
        }
    }
    
    /**
     * Cleanup all tasks on plugin disable
     */
    public void cleanup() {
        for (int taskId : regenTasks.values()) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        regenTasks.clear();
        plugin.getLogger().info("Cleaned up all health regeneration tasks");
    }
}