package com.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager;
import com.server.debug.DebugManager.DebugSystem;

public class DebugCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
    public DebugCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        DebugManager debugManager = DebugManager.getInstance();
        
        // If no arguments, show help
        if (args.length == 0) {
            showDebugStatus(sender);
            return true;
        }
        
        // Toggle specific system
        String systemName = args[0].toLowerCase();
        
        // Special case for "list"
        if (systemName.equals("list")) {
            listDebugSystems(sender);
            return true;
        }
        
        // Find the system to toggle
        DebugSystem system = DebugSystem.fromId(systemName);
        
        if (system == null) {
            sender.sendMessage(ChatColor.RED + "Unknown debug system: " + systemName);
            sender.sendMessage(ChatColor.RED + "Use '/debugmode list' to see available systems.");
            return true;
        }
        
        // Toggle the system
        boolean newState = debugManager.toggleDebug(system);
        
        // Inform sender
        if (system == DebugSystem.ALL) {
            sender.sendMessage(ChatColor.GREEN + "Debug mode (all systems) is now " + 
                              (newState ? ChatColor.YELLOW + "enabled" : ChatColor.RED + "disabled") + 
                              ChatColor.GREEN + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Debug mode for " + ChatColor.GOLD + system.getId() + 
                              ChatColor.GREEN + " is now " + 
                              (newState ? ChatColor.YELLOW + "enabled" : ChatColor.RED + "disabled") + 
                              ChatColor.GREEN + ".");
        }
        
        // Log to console
        plugin.getLogger().info("Debug mode for " + system.getId() + " was " + 
                               (newState ? "enabled" : "disabled") + 
                               " by " + (sender instanceof Player ? ((Player)sender).getName() : "CONSOLE"));
        
        return true;
    }
    
    private void showDebugStatus(CommandSender sender) {
        DebugManager debugManager = DebugManager.getInstance();
        boolean globalDebug = debugManager.isDebugEnabled(DebugSystem.ALL);
        
        sender.sendMessage(ChatColor.GOLD + "===== Debug Mode Status =====");
        sender.sendMessage(ChatColor.YELLOW + "Global debug mode: " + 
                          (globalDebug ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        
        if (!globalDebug) {
            // Show individual systems that are enabled
            boolean anyEnabled = false;
            StringBuilder enabledSystems = new StringBuilder();
            
            for (DebugSystem system : DebugSystem.values()) {
                if (system != DebugSystem.ALL && debugManager.isDebugEnabled(system)) {
                    if (anyEnabled) {
                        enabledSystems.append(ChatColor.GRAY).append(", ");
                    }
                    enabledSystems.append(ChatColor.GREEN).append(system.getId());
                    anyEnabled = true;
                }
            }
            
            if (anyEnabled) {
                sender.sendMessage(ChatColor.YELLOW + "Enabled systems: " + enabledSystems.toString());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No systems are currently enabled.");
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Use '/debugmode list' to see all available systems.");
        sender.sendMessage(ChatColor.YELLOW + "Use '/debugmode <system>' to toggle a specific system.");
    }
    
    private void listDebugSystems(CommandSender sender) {
        DebugManager debugManager = DebugManager.getInstance();
        
        sender.sendMessage(ChatColor.GOLD + "===== Available Debug Systems =====");
        
        for (DebugSystem system : DebugSystem.values()) {
            boolean enabled = debugManager.isDebugEnabled(system);
            sender.sendMessage(
                (enabled ? ChatColor.GREEN : ChatColor.RED) + system.getId() + ChatColor.GRAY + " - " + 
                ChatColor.YELLOW + system.getDescription() + 
                (enabled ? ChatColor.GREEN + " (ENABLED)" : ChatColor.RED + " (DISABLED)")
            );
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Add "list" option
            if ("list".startsWith(args[0].toLowerCase())) {
                completions.add("list");
            }
            
            // Add all system IDs that match the current input
            String input = args[0].toLowerCase();
            for (DebugSystem system : DebugSystem.values()) {
                if (system.getId().startsWith(input)) {
                    completions.add(system.getId());
                }
            }
        }
        
        return completions;
    }
}