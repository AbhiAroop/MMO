package com.server.nametags;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

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
     * Format chat messages with [Member] prefix and hover info
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Cancel the default message format
        event.setCancelled(true);
        
        // Get player profile data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        String rank = "§7[§fMember§7]";
        int level = 1;
        int units = 0;
        int premiumUnits = 0;
        
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                level = profile.getProfileLevel();
                units = profile.getUnits();
                premiumUnits = profile.getPremiumUnits();
            }
        }
        
        // Build the hover text with simple professional formatting
        String hoverText = ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━\n" +
                          ChatColor.WHITE + "  Player Information\n" +
                          ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━\n" +
                          ChatColor.GRAY + "  Real Name: " + ChatColor.WHITE + player.getName() + "\n" +
                          ChatColor.GRAY + "  Level: " + ChatColor.GOLD + "★ " + level + "\n" +
                          ChatColor.GRAY + "  Units: " + ChatColor.YELLOW + "⛃ " + CurrencyFormatter.formatUnits(units) + "\n" +
                          ChatColor.GRAY + "  Premium: " + ChatColor.LIGHT_PURPLE + "◆ " + CurrencyFormatter.formatPremiumUnits(premiumUnits);
        
        // Create text components with hover event
        TextComponent rankComponent = new TextComponent(rank + " ");
        
        TextComponent nameComponent = new TextComponent(player.getName());
        nameComponent.setColor(ChatColor.WHITE);
        nameComponent.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new Text(hoverText)
        ));
        
        TextComponent colonComponent = new TextComponent(ChatColor.GRAY + ": ");
        TextComponent messageComponent = new TextComponent(ChatColor.WHITE + message);
        
        // Combine components
        TextComponent fullMessage = new TextComponent(rankComponent, nameComponent, colonComponent, messageComponent);
        
        // Send to all players
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.spigot().sendMessage(fullMessage);
        }
    }
}
