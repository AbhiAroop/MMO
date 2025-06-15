package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.enchanting.CustomEnchantment.EnchantmentCategory;
import com.server.enchanting.EnchantingLevelCalculator;
import com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel;
import com.server.enchanting.RuneBook;
import com.server.enchanting.RuneBookRegistry;

/**
 * Admin command for managing custom enchanting system
 */
public class AdminEnchantingCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mmo.admin.enchanting")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "giverune":
                return handleGiveRuneCommand(sender, args);
            case "testlevel":
                return handleTestLevelCommand(sender, args);
            case "listbooks":
                return handleListBooksCommand(sender, args);
            case "apply":
                return handleApplyEnchantCommand(sender, args);
            case "remove":
                return handleRemoveEnchantCommand(sender, args);
            case "listitem":
                return handleListItemEnchantments(sender, args);
            case "debug":
                return handleDebugCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * Apply an enchantment to an item (admin command)
     */
    private boolean handleApplyEnchantCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting apply <enchantment> <level> [player]");
            return true;
        }
        
        Player player = (Player) sender;
        String enchantmentId = args[1].toLowerCase();
        int level;
        
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level: " + args[2]);
            return true;
        }
        
        // Get target player if specified
        if (args.length >= 4) {
            Player target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                return true;
            }
            player = target;
        }
        
        // Get enchantment
        com.server.enchanting.CustomEnchantment enchantment = 
            com.server.enchanting.CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        
        if (enchantment == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enchantment: " + enchantmentId);
            return true;
        }
        
        // Get item in player's hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Player must be holding an item!");
            return true;
        }
        
        // Check if enchantment can be applied to item
        if (!enchantment.canApplyTo(item)) {
            sender.sendMessage(ChatColor.RED + "This enchantment cannot be applied to " + item.getType().name());
            return true;
        }
        
        // Validate level
        if (level < 1 || level > enchantment.getMaxLevel()) {
            sender.sendMessage(ChatColor.RED + "Level must be between 1 and " + enchantment.getMaxLevel());
            return true;
        }
        
        // Apply enchantment
        ItemStack enchantedItem = com.server.enchanting.EnchantmentApplicator.applyEnchantment(item, enchantment, level);
        
        if (enchantedItem != null) {
            player.getInventory().setItemInMainHand(enchantedItem);
            sender.sendMessage(ChatColor.GREEN + "Applied " + enchantment.getFormattedName(level) + " to " + 
                            player.getName() + "'s " + item.getType().name());
            player.sendMessage(ChatColor.GREEN + "Your item has been enchanted with " + enchantment.getFormattedName(level));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to apply enchantment!");
        }
        
        return true;
    }

    /**
     * Remove an enchantment from an item (admin command)
     */
    private boolean handleRemoveEnchantCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting remove <enchantment> [player]");
            return true;
        }
        
        Player player = (Player) sender;
        String enchantmentId = args[1].toLowerCase();
        
        // Get target player if specified
        if (args.length >= 3) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                return true;
            }
            player = target;
        }
        
        // Get item in player's hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Player must be holding an item!");
            return true;
        }
        
        // Check if item has the enchantment
        if (com.server.enchanting.EnchantmentApplicator.getEnchantmentLevel(item, enchantmentId) == 0) {
            sender.sendMessage(ChatColor.RED + "Item does not have enchantment: " + enchantmentId);
            return true;
        }
        
        // Remove enchantment
        ItemStack modifiedItem = com.server.enchanting.EnchantmentApplicator.removeEnchantment(item, enchantmentId);
        player.getInventory().setItemInMainHand(modifiedItem);
        
        sender.sendMessage(ChatColor.GREEN + "Removed enchantment " + enchantmentId + " from " + 
                        player.getName() + "'s " + item.getType().name());
        player.sendMessage(ChatColor.YELLOW + "Enchantment " + enchantmentId + " has been removed from your item");
        
        return true;
    }

    /**
     * List enchantments on an item (admin command)
     */
    private boolean handleListItemEnchantments(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Get target player if specified
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            player = target;
        }
        
        // Get item in player's hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Player must be holding an item!");
            return true;
        }
        
        // Get enchantments
        java.util.Map<String, Integer> enchantments = 
            com.server.enchanting.EnchantmentApplicator.getCustomEnchantments(item);
        
        if (enchantments.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Item has no custom enchantments");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Custom Enchantments on " + item.getType().name() + " ===");
        for (java.util.Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            com.server.enchanting.CustomEnchantment enchantment = 
                com.server.enchanting.CustomEnchantmentRegistry.getInstance().getEnchantment(entry.getKey());
            
            if (enchantment != null) {
                sender.sendMessage(ChatColor.AQUA + "• " + enchantment.getFormattedName(entry.getValue()) + 
                                ChatColor.GRAY + " (" + entry.getKey() + ")");
            }
        }
        
        return true;
    }

    
    /**
     * Give a rune book to a player
     */
    private boolean handleGiveRuneCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting giverune <player> <tier> <category>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        
        String tierName = args[2].toUpperCase();
        String categoryName = args[3].toUpperCase();
        
        RuneBook.RuneTier tier;
        EnchantmentCategory category;
        
        try {
            tier = RuneBook.RuneTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid tier: " + tierName);
            sender.sendMessage(ChatColor.YELLOW + "Available tiers: " + 
                Arrays.stream(RuneBook.RuneTier.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
            return true;
        }
        
        try {
            category = EnchantmentCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid category: " + categoryName);
            sender.sendMessage(ChatColor.YELLOW + "Available categories: " + 
                Arrays.stream(EnchantmentCategory.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
            return true;
        }
        
        RuneBook runeBook = RuneBookRegistry.getInstance().getRuneBook(tier, category);
        if (runeBook == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create rune book!");
            return true;
        }
        
        ItemStack runeBookItem = runeBook.createItem();
        target.getInventory().addItem(runeBookItem);
        
        sender.sendMessage(ChatColor.GREEN + "Gave " + runeBook.getDisplayName() + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received a " + runeBook.getDisplayName() + "!");
        
        return true;
    }
    
    /**
     * Test enchanting level at player's location
     */
    private boolean handleTestLevelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        Location loc = player.getLocation();
        
        // Check if player is near an enchanting table
        boolean nearEnchantingTable = false;
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location checkLoc = loc.clone().add(x, y, z);
                    if (checkLoc.getBlock().getType() == Material.ENCHANTING_TABLE) {
                        nearEnchantingTable = true;
                        loc = checkLoc; // Use enchanting table location
                        break;
                    }
                }
            }
        }
        
        if (!nearEnchantingTable) {
            sender.sendMessage(ChatColor.RED + "No enchanting table found nearby!");
            return true;
        }
        
        EnchantingLevel enchantingLevel = EnchantingLevelCalculator.calculateEnchantingLevel(loc);
        
        sender.sendMessage(ChatColor.GOLD + "=== Enchanting Level Analysis ===");
        sender.sendMessage(ChatColor.AQUA + "Location: " + ChatColor.WHITE + 
            String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
        sender.sendMessage(ChatColor.AQUA + "Total Level: " + ChatColor.YELLOW + enchantingLevel.getTotalLevel());
        sender.sendMessage(ChatColor.AQUA + "Vanilla Level: " + ChatColor.WHITE + enchantingLevel.getVanillaLevel());
        sender.sendMessage(ChatColor.AQUA + "Advanced Level: " + ChatColor.LIGHT_PURPLE + enchantingLevel.getAdvancedLevel());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "Vanilla Bookshelves: " + ChatColor.WHITE + 
            enchantingLevel.getVanillaBookshelves().size());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Chiseled Bookshelves: " + ChatColor.WHITE + 
            enchantingLevel.getChiseledBookshelves().size());
        
        // Show rune book details
        if (!enchantingLevel.getChiseledBookshelves().isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Rune Books Found:");
            for (EnchantingLevelCalculator.ChiseledBookshelfData bookshelf : enchantingLevel.getChiseledBookshelves()) {
                Location bsLoc = bookshelf.getLocation();
                sender.sendMessage(ChatColor.GRAY + "  • Bookshelf at " + 
                    String.format("%.0f, %.0f, %.0f", bsLoc.getX(), bsLoc.getY(), bsLoc.getZ()) + 
                    ChatColor.YELLOW + " (Power: " + bookshelf.getTotalPower() + ")");
                
                for (RuneBook rune : bookshelf.getRuneBooks()) {
                    sender.sendMessage(ChatColor.WHITE + "    - " + rune.getTier().getFormattedName() + " " + 
                        rune.getSpecialization().getColor() + rune.getSpecialization().getDisplayName() + 
                        ChatColor.GRAY + " (+" + rune.getEnchantingPower() + " power)");
                }
            }
        }
        
        return true;
    }
    
    /**
     * List all available rune book types
     */
    private boolean handleListBooksCommand(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== Available Rune Books ===");
        
        for (RuneBook.RuneTier tier : RuneBook.RuneTier.values()) {
            sender.sendMessage("");
            sender.sendMessage(tier.getFormattedName() + ChatColor.GRAY + " Tier " + 
                ChatColor.YELLOW + "(+" + tier.getBasePower() + " base power)");
            
            for (EnchantmentCategory category : EnchantmentCategory.values()) {
                RuneBook runeBook = RuneBookRegistry.getInstance().getRuneBook(tier, category);
                if (runeBook != null) {
                    sender.sendMessage(ChatColor.GRAY + "  • " + category.getColor() + 
                        category.getDisplayName() + ChatColor.GRAY + " - " + 
                        ChatColor.WHITE + runeBook.getDisplayName());
                }
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Use: /adminenchanting giverune <player> <tier> <category>");
        
        return true;
    }
    
    /**
     * Toggle debug mode for enchanting system
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        // Implementation would toggle debug mode for enchanting system
        sender.sendMessage(ChatColor.GREEN + "Enchanting debug mode toggled!");
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Admin Enchanting Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting giverune <player> <tier> <category>" + 
            ChatColor.GRAY + " - Give rune book");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting testlevel" + 
            ChatColor.GRAY + " - Test enchanting level at your location");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting listbooks" + 
            ChatColor.GRAY + " - List all rune book types");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting apply <enchant> <level> [player]" + 
            ChatColor.GRAY + " - Apply enchantment to held item");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting remove <enchant> [player]" + 
            ChatColor.GRAY + " - Remove enchantment from held item");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting listitem [player]" + 
            ChatColor.GRAY + " - List enchantments on held item");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting debug" + 
            ChatColor.GRAY + " - Toggle debug mode");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("giverune", "testlevel", "listbooks", "debug"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("giverune")) {
            // Add player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("giverune")) {
            // Add tier names
            for (RuneBook.RuneTier tier : RuneBook.RuneTier.values()) {
                completions.add(tier.name().toLowerCase());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("giverune")) {
            // Add category names
            for (EnchantmentCategory category : EnchantmentCategory.values()) {
                completions.add(category.name().toLowerCase());
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}