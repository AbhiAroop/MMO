package com.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;

public class FlyCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
    public FlyCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.fly")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Handle self or target player
        Player target;
        boolean isSelf = false;
        
        if (args.length == 0) {
            // Self flight toggle
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /fly <player> [on|off]");
                return true;
            }
            
            target = (Player) sender;
            isSelf = true;
        } else {
            // Target player flight control
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
        }
        
        // Determine whether to enable or disable flight
        boolean enableFlight;
        
        if (args.length >= (isSelf ? 1 : 2)) {
            String action = args[isSelf ? 0 : 1].toLowerCase();
            if (action.equals("on") || action.equals("enable")) {
                enableFlight = true;
            } else if (action.equals("off") || action.equals("disable")) {
                enableFlight = false;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid option: " + action);
                sender.sendMessage(ChatColor.GRAY + "Usage: /fly [player] [on|off]");
                return true;
            }
        } else {
            // Toggle current state if no specific option
            enableFlight = !target.getAllowFlight();
        }
        
        // Apply flight setting
        target.setAllowFlight(enableFlight);
        if (!enableFlight) {
            target.setFlying(false); // Force player to stop flying if flight is disabled
            target.setAllowFlight(false);
        } else {
            // If they're in the air, enable flight mode immediately
            target.setFlying(true);
            target.setAllowFlight(true);
        }
        
        // Send feedback messages
        String status = enableFlight ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        
        if (isSelf) {
            sender.sendMessage(ChatColor.GOLD + "Flight mode " + status + ChatColor.GOLD + " for yourself.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "Flight mode " + status + ChatColor.GOLD + " for " + target.getName() + ".");
            target.sendMessage(ChatColor.GOLD + "Your flight mode was " + status + ChatColor.GOLD + " by an admin.");
        }
        
        // Log the action
        plugin.getLogger().info(sender.getName() + " " + (enableFlight ? "enabled" : "disabled") + 
                             " flight for " + target.getName());
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mmo.admin.fly")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument: player name
            String partialName = args[0].toLowerCase();
            
            // If it starts with "on" or "off" and sender is a player, offer those options
            if (sender instanceof Player && 
                (partialName.startsWith("on") || partialName.startsWith("of") || 
                 partialName.startsWith("en") || partialName.startsWith("di"))) {
                
                if ("on".startsWith(partialName) || "enable".startsWith(partialName)) {
                    completions.add("on");
                    completions.add("enable");
                }
                if ("off".startsWith(partialName) || "disable".startsWith(partialName)) {
                    completions.add("off");
                    completions.add("disable");
                }
            }
            
            // Add player names
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            });
            
        } else if (args.length == 2) {
            // Second argument: on/off options
            String partialOption = args[1].toLowerCase();
            
            if ("on".startsWith(partialOption) || "enable".startsWith(partialOption)) {
                completions.add("on");
                completions.add("enable");
            }
            if ("off".startsWith(partialOption) || "disable".startsWith(partialOption)) {
                completions.add("off");
                completions.add("disable");
            }
        }
        
        return completions;
    }
}