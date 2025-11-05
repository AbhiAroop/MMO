package com.server.nametags;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        
        // Set custom join message: [Member] Name joined the game
        String rank = "§7[§fMember§7]";
        event.setJoinMessage(rank + " §f" + player.getName() + " §7joined the game");
        
        // Initialize nametag system for this player
        NametagManager.getInstance().initializePlayer(player);
    }
    
    /**
     * Clean up nametag when player quits
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Set custom quit message: [Member] Name left the game
        String rank = "§7[§fMember§7]";
        event.setQuitMessage(rank + " §f" + player.getName() + " §7left the game");
        
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
    
    /**
     * Format chat messages with [Member] prefix
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Get the rank prefix (same as tablist)
        String rank = "§7[§fMember§7]";
        
        // Format: [Member] PlayerName: message
        event.setFormat(rank + " §f%s§7: §f%s");
    }
}
