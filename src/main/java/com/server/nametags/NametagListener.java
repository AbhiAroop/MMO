package com.server.nametags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

/**
 * Listens for events that should trigger nametag updates
 */
public class NametagListener implements Listener {
    
    /**
     * Initialize nametag when player joins
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Initialize nametag system for this player
        NametagManager.getInstance().initializePlayer(player);
    }
    
    /**
     * Clean up nametag when player quits
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove nametag updates for this player
        NametagManager.getInstance().removePlayer(player);
    }
    
    /**
     * Update nametag when player takes damage (health changes)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Update immediately on damage
            NametagManager.getInstance().updateNametag(player);
        }
    }
    
    /**
     * Update nametag when player regains health
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Update immediately on heal
            NametagManager.getInstance().updateNametag(player);
        }
    }
}
