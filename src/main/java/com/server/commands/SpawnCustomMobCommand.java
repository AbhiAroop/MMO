package com.server.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpawnCustomMobCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final List<String> mobTypes = Arrays.asList("runemarkcolossus");
    
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
        
        switch (mobType) {
            case "runemarkcolossus":
                plugin.getCustomEntityManager().spawnRunemarkColossus(player.getLocation());
                player.sendMessage("§aSpawned a Runemark Colossus.");
                break;
            default:
                player.sendMessage("§cUnknown mob type: " + mobType);
                return true;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String mobType : mobTypes) {
                if (mobType.startsWith(args[0].toLowerCase())) {
                    completions.add(mobType);
                }
            }
            return completions;
        }
        return null;
    }
}