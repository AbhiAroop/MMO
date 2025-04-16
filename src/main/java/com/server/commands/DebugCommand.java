package com.server.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.server.Main;

public class DebugCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        /** 
        if (!sender.hasPermission("mmo.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        **/
        Main plugin = Main.getInstance();
        
        // Toggle debug mode
        boolean currentDebugMode = plugin.isDebugMode();
        plugin.setDebugMode(!currentDebugMode);
        
        // Inform sender
        sender.sendMessage("§aDebug mode is now " + (plugin.isDebugMode() ? "§eenabled" : "§cdisabled") + "§a.");
        
        // Log to console
        plugin.getLogger().info("Debug mode was " + (plugin.isDebugMode() ? "enabled" : "disabled") + 
                               " by " + (sender instanceof Player ? ((Player)sender).getName() : "CONSOLE"));
        
        return true;
    }
}