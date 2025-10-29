package com.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.profiles.skills.skills.fishing.baits.BaitManager;
import com.server.profiles.skills.skills.fishing.baits.FishingBait;

/**
 * Admin command for Fishing system management
 */
public class FishingCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("mmo.admin.fishing")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "bait":
                handleBait(player, args);
                break;
                
            case "listbaits":
                handleListBaits(player);
                break;
                
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Fishing Admin Commands ===");
        player.sendMessage("§e/fishing bait <type> [amount] [player]§7 - Give fishing bait");
        player.sendMessage("§e/fishing listbaits§7 - List all bait types");
    }
    
    private void handleBait(Player player, String[] args) {
        // /fishing bait <type> [amount] [player]
        if (args.length < 2) {
            player.sendMessage("§cUsage: /fishing bait <type> [amount] [player]");
            player.sendMessage("§7Available types: worm, cricket, minnow, leech, magic_lure");
            return;
        }
        
        String baitTypeName = args[1].toUpperCase();
        FishingBait baitType;
        
        try {
            baitType = FishingBait.valueOf(baitTypeName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid bait type: " + args[1]);
            player.sendMessage("§7Available types: worm, cricket, minnow, leech, magic_lure");
            return;
        }
        
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    player.sendMessage("§cAmount must be between 1 and 64!");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount: " + args[2]);
                return;
            }
        }
        
        Player target = player;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                player.sendMessage("§cPlayer not found: " + args[3]);
                return;
            }
        }
        
        BaitManager.giveBait(target, baitType, amount);
        
        if (target.equals(player)) {
            player.sendMessage("§aYou received " + amount + "x " + baitType.getDisplayName());
        } else {
            player.sendMessage("§aGave " + amount + "x " + baitType.getDisplayName() + " §ato " + target.getName());
            target.sendMessage("§aYou received " + amount + "x " + baitType.getDisplayName());
        }
    }
    
    private void handleListBaits(Player player) {
        player.sendMessage("§6§l=== Available Fishing Baits ===");
        for (FishingBait bait : FishingBait.values()) {
            player.sendMessage("§e" + bait.name().toLowerCase() + " §7- " + bait.getDisplayName());
            StringBuilder fishNames = new StringBuilder();
            for (int i = 0; i < bait.getAttractedFish().size(); i++) {
                if (i > 0) fishNames.append(", ");
                fishNames.append(bait.getAttractedFish().get(i).getDisplayName());
            }
            player.sendMessage("  §8Attracts: §f" + fishNames.toString());
            player.sendMessage("  §8Quality Boost: §e+" + bait.getQualityBoost() + "%");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            completions.add("bait");
            completions.add("listbaits");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bait")) {
            // Bait types
            for (FishingBait bait : FishingBait.values()) {
                completions.add(bait.name().toLowerCase());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("bait")) {
            // Amount suggestions
            completions.add("1");
            completions.add("8");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("bait")) {
            // Online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        
        // Filter completions based on what the user has typed
        String current = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(current));
        
        return completions;
    }
}
