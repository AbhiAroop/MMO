package com.server.profiles.stats.health;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.server.Main;

/**
 * Listener for controlling health regeneration mechanics
 */
public class HealthRegenerationListener implements Listener {
    
    private final Main plugin;
    
    public HealthRegenerationListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Cancel natural (food-based) health regeneration to use our custom system instead
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        // Only handle player health regeneration
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Cancel only natural regeneration (food-based)
        if (event.getRegainReason() == RegainReason.SATIATED) {
            event.setCancelled(true);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Cancelled vanilla health regeneration for " + player.getName() + 
                                   " (amount: " + event.getAmount() + ")");
            }
        }
    }
}