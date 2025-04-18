package com.server.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

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
                    
                    // Schedule health and other attribute updates after respawn
                    schedulePostRespawnUpdates(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * Schedule updates to health and other attributes after a player respawns
     */
    private void schedulePostRespawnUpdates(Player player) {
        // Schedule multiple update attempts to ensure everything is properly applied
        for (int delay : new int[] {1, 5, 10, 20}) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Get the player's active profile using ProfileManager directly
                        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                        if (activeSlot == null) return;
                        
                        // Get the profile
                        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                        if (profile == null) return;
                        
                        // Apply stats with proper health setting
                        PlayerStats stats = profile.getStats();
                        
                        // Set max health 
                        try {
                            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            if (maxHealth != null) {
                                // Set the base value to vanilla default
                                maxHealth.setBaseValue(20.0);
                                
                                // Apply health boost modifier
                                double healthBonus = stats.getHealth() - 20.0;
                                if (healthBonus != 0) {
                                    AttributeModifier healthMod = new AttributeModifier(
                                        UUID.randomUUID(),
                                        "mmo.health",
                                        healthBonus,
                                        AttributeModifier.Operation.ADD_NUMBER
                                    );
                                    
                                    // Remove existing modifiers first
                                    Set<AttributeModifier> existingModifiers = new HashSet<>(maxHealth.getModifiers());
                                    for (AttributeModifier mod : existingModifiers) {
                                        maxHealth.removeModifier(mod);
                                    }
                                    
                                    // Add the new modifier
                                    maxHealth.addModifier(healthMod);
                                }
                                
                                // Set health to max on respawn
                                player.setHealth(maxHealth.getValue());
                                
                                // Ensure health display is scaled to show 10 hearts
                                player.setHealthScaled(true);
                                player.setHealthScale(20.0);
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Post-respawn health update: " + 
                                                    "Max: " + maxHealth.getValue() + 
                                                    ", Current: " + player.getHealth());
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error updating health after respawn: " + e.getMessage());
                        }
                        
                        // Update other attributes
                        plugin.getRangedCombatManager().updatePlayerAttributes(player);
                    }
                }
            }.runTaskLater(plugin, delay);
        }
    }
}