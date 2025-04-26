package com.server.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;

public class SpawnCustomMobCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    
    public SpawnCustomMobCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("mmo.spawnmob")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        // Check arguments
        if (args.length < 1) {
            player.sendMessage("§cUsage: /spawnmob <type>");
            return true;
        }
        
        String mobType = args[0].toLowerCase();
        
        // Spawn the mob
        if (plugin.getCustomEntityManager().spawnCustomMobByType(mobType, player.getLocation()) != null) {
            player.sendMessage("§aSpawned a " + mobType + ".");
        } else {
            player.sendMessage("§cUnknown mob type: " + mobType);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            // Get all registered mob types from the registry
            Map<String, ?> mobTypes = plugin.getCustomEntityManager().getMobRegistry().getMobTypes();
            
            // Return matching mob types
            String prefix = args[0].toLowerCase();
            return mobTypes.keySet().stream()
                    .filter(type -> type.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}