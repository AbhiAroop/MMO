package com.server.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Kicks all players when a reload command is detected to prevent island issues.
 */
public class ServerReloadListener implements Listener {
    
    private final JavaPlugin plugin;
    
    public ServerReloadListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        
        // Check if it's a reload command
        if (command.equals("/reload") || command.equals("/rl") || 
            command.startsWith("/reload ") || command.startsWith("/rl ")) {
            
            if (event.getPlayer().hasPermission("bukkit.command.reload")) {
                plugin.getLogger().info("Reload command detected from player. Kicking all players...");
                kickAllPlayers();
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();
        
        // Check if it's a reload command
        if (command.equals("reload") || command.equals("rl") || 
            command.startsWith("reload ") || command.startsWith("rl ")) {
            
            plugin.getLogger().info("Reload command detected from console. Kicking all players...");
            kickAllPlayers();
        }
    }
    
    private void kickAllPlayers() {
        // Schedule the kick to happen immediately
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kick(Component.text("Server is reloading. Please reconnect in a moment!")
                    .color(NamedTextColor.YELLOW));
            }
            plugin.getLogger().info("All players kicked before reload");
        });
    }
}
