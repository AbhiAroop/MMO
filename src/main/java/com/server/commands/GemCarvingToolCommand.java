package com.server.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.skills.minigames.GemCarvingManager;

/**
 * Command handler for gem carving tools
 */
public class GemCarvingToolCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    private final GemCarvingManager gemCarvingManager;
    
    public GemCarvingToolCommand(Main plugin, GemCarvingManager gemCarvingManager) {
        this.plugin = plugin;
        this.gemCarvingManager = gemCarvingManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("mmo.command.gemtool")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player name when using from console.");
                return true;
            }
            
            Player player = (Player) sender;
            gemCarvingManager.giveGemCarvingTool(player);
            return true;
        }
        
        if (args.length >= 1) {
            // Check for target player
            Player target = Bukkit.getPlayer(args[0]);
            
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            gemCarvingManager.giveGemCarvingTool(target);
            sender.sendMessage(ChatColor.GREEN + "Gave a Gem Carving Tool to " + target.getName());
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest online players
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }
}