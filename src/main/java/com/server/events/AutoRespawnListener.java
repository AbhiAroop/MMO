package com.server.events;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Handles automatic respawning of players when they die
 */
public class AutoRespawnListener implements Listener {
    
    private final Main plugin;
    
    public AutoRespawnListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        // Store death location for potential use later
        final double deathX = player.getLocation().getX();
        final double deathY = player.getLocation().getY();
        final double deathZ = player.getLocation().getZ();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Player died: " + player.getName() + " at " + 
                              deathX + ", " + deathY + ", " + deathZ);
        }
        
        // Auto respawn after 1 tick (immediately after death processing is complete)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isDead()) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Auto-respawning player: " + player.getName());
                    }
                    
                    // Use the respawn method to properly respawn the player
                    player.spigot().respawn();
                    
                    // Force stat scanning after respawn with multiple delays to ensure it takes effect
                    scheduleStatScanning(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * Schedule stat scanning after respawn with multiple delays to ensure attributes are correctly applied
     */
    private void scheduleStatScanning(Player player) {
        // Only need one simple scan attempt after a reasonable delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Get the player's active profile
                    Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            // First apply stats to ensure correct max health
                            plugin.getStatScanManager().scanAndUpdatePlayerStats(player);
                            
                            // Then set health to the default health stat value
                            int defaultHealth = profile.getStats().getHealth();
                            
                            // Ensure we don't exceed max health
                            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            if (maxHealthAttr != null) {
                                double maxHealth = maxHealthAttr.getValue();
                                double healthToSet = Math.min(defaultHealth, maxHealth);
                                
                                // Set player's health and update the stats object
                                player.setHealth(healthToSet);
                                profile.getStats().setCurrentHealth(healthToSet);
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Set " + player.getName() + 
                                                    "'s health to default value (" + healthToSet + 
                                                    ") after respawn");
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 5L); // Small delay to ensure respawn is complete
    }
}