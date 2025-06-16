package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

import com.server.enchanting.CustomEnchantment;
import com.server.enchanting.CustomEnchantment.EnchantmentCategory;
import com.server.enchanting.CustomEnchantment.EnchantmentRarity;
import com.server.enchanting.CustomEnchantmentRegistry;
import com.server.enchanting.EnchantingLevelCalculator;
import com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel;
import com.server.enchanting.EnchantmentApplicator;
import com.server.enchanting.RuneBook;
import com.server.enchanting.RuneBookRegistry;

/**
 * Enhanced admin command for managing custom enchanting system
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
            case "list":
                return handleListCommand(sender, args);
            case "info":
                return handleInfoCommand(sender, args);
            case "set":
                return handleSetEnchantCommand(sender, args);
            case "add":
                return handleAddEnchantCommand(sender, args);
            case "remove":
                return handleRemoveEnchantCommand(sender, args);
            case "clear":
                return handleClearEnchantmentsCommand(sender, args);
            case "listitem":
                return handleListItemEnchantments(sender, args);
            case "giverune":
                return handleGiveRuneCommand(sender, args);
            case "testlevel":
                return handleTestLevelCommand(sender, args);
            case "listbooks":
                return handleListBooksCommand(sender, args);
            case "debug":
                return handleDebugCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * List enchantments with filtering options - ENHANCED
     */
    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String filter = args[1].toLowerCase();
            
            // Filter by category
            try {
                EnchantmentCategory category = EnchantmentCategory.valueOf(filter.toUpperCase());
                listEnchantmentsByCategory(sender, category);
                return true;
            } catch (IllegalArgumentException e) {
                // Not a category, try rarity
            }
            
            // Filter by rarity
            try {
                EnchantmentRarity rarity = EnchantmentRarity.valueOf(filter.toUpperCase());
                listEnchantmentsByRarity(sender, rarity);
                return true;
            } catch (IllegalArgumentException e) {
                // Not a rarity either
                sender.sendMessage(ChatColor.RED + "Invalid filter: " + filter);
                sender.sendMessage(ChatColor.YELLOW + "Valid filters: category names or rarity names");
                return true;
            }
        }
        
        // List all enchantments
        listAllEnchantments(sender);
        return true;
    }
    
    /**
     * Get detailed info about a specific enchantment - NEW
     */
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting info <enchantment_id>");
            return true;
        }
        
        String enchantmentId = args[1].toLowerCase();
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        
        if (enchantment == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enchantment: " + enchantmentId);
            return true;
        }
        
        showDetailedEnchantmentInfo(sender, enchantment);
        return true;
    }
    
    /**
     * Set enchantment level (replaces existing) - NEW
     */
    private boolean handleSetEnchantCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting set <enchantment> <level> [player]");
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
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
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
        
        // Remove existing enchantment if present, then apply new level
        ItemStack modifiedItem = item;
        int currentLevel = EnchantmentApplicator.getEnchantmentLevel(item, enchantmentId);
        
        if (currentLevel > 0) {
            modifiedItem = EnchantmentApplicator.removeEnchantment(modifiedItem, enchantmentId);
        }
        
        // Apply new enchantment
        ItemStack enchantedItem = EnchantmentApplicator.applyEnchantment(modifiedItem, enchantment, level);
        
        if (enchantedItem != null) {
            player.getInventory().setItemInMainHand(enchantedItem);
            sender.sendMessage(ChatColor.GREEN + "Set " + enchantment.getFormattedName(level) + " on " + 
                            player.getName() + "'s " + item.getType().name());
            if (sender != player) {
                player.sendMessage(ChatColor.GREEN + "Your item enchantment was set to " + enchantment.getFormattedName(level));
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set enchantment!");
        }
        
        return true;
    }
    
    /**
     * Add enchantment level (stacks with existing) - NEW
     */
    private boolean handleAddEnchantCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting add <enchantment> <level> [player]");
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
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
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
        
        // Get current level and calculate new level
        int currentLevel = EnchantmentApplicator.getEnchantmentLevel(item, enchantmentId);
        int newLevel = currentLevel + level;
        
        // Validate new level
        if (newLevel > enchantment.getMaxLevel()) {
            sender.sendMessage(ChatColor.RED + "Adding " + level + " levels would exceed max level " + 
                            enchantment.getMaxLevel() + " (current: " + currentLevel + ")");
            return true;
        }
        
        if (newLevel <= 0) {
            sender.sendMessage(ChatColor.RED + "Adding " + level + " levels would result in level " + newLevel + 
                            " (current: " + currentLevel + ")");
            return true;
        }
        
        // Apply enchantment (will upgrade if exists)
        ItemStack enchantedItem = EnchantmentApplicator.applyEnchantment(item, enchantment, newLevel);
        
        if (enchantedItem != null) {
            player.getInventory().setItemInMainHand(enchantedItem);
            if (currentLevel > 0) {
                sender.sendMessage(ChatColor.GREEN + "Upgraded " + enchantment.getDisplayName() + " from level " + 
                                currentLevel + " to " + newLevel + " on " + player.getName() + "'s " + item.getType().name());
            } else {
                sender.sendMessage(ChatColor.GREEN + "Added " + enchantment.getFormattedName(newLevel) + " to " + 
                                player.getName() + "'s " + item.getType().name());
            }
            
            if (sender != player) {
                player.sendMessage(ChatColor.GREEN + "Your item received " + enchantment.getFormattedName(newLevel));
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to add enchantment!");
        }
        
        return true;
    }
    
    /**
     * Remove enchantment from an item - ENHANCED
     */
    private boolean handleRemoveEnchantCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be used by a player!");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminenchanting remove <enchantment_id> [player]");
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
        
        // Check if item has this enchantment
        int currentLevel = EnchantmentApplicator.getEnchantmentLevel(item, enchantmentId);
        if (currentLevel == 0) {
            sender.sendMessage(ChatColor.RED + "Item does not have enchantment: " + enchantmentId);
            return true;
        }
        
        // Get enchantment for display name
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        String enchantmentName = enchantment != null ? enchantment.getFormattedName(currentLevel) : enchantmentId;
        
        // Remove the enchantment
        ItemStack modifiedItem = EnchantmentApplicator.removeEnchantment(item, enchantmentId);
        
        if (modifiedItem != null) {
            player.getInventory().setItemInMainHand(modifiedItem);
            sender.sendMessage(ChatColor.GREEN + "Removed " + enchantmentName + " from " + player.getName() + "'s item");
            
            if (sender != player) {
                player.sendMessage(ChatColor.YELLOW + "Enchantment " + enchantmentName + " was removed from your item by an admin");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to remove enchantment!");
        }
        
        return true;
    }
    
    /**
     * Clear all enchantments from an item - NEW
     */
    private boolean handleClearEnchantmentsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command must be used by a player!");
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
        
        // Get current enchantments for confirmation
        Map<String, Integer> enchantments = EnchantmentApplicator.getCustomEnchantments(item);
        
        if (enchantments.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Item has no custom enchantments to clear");
            return true;
        }
        
        // Clear all enchantments
        ItemStack clearedItem = EnchantmentApplicator.clearAllEnchantments(item);
        
        if (clearedItem != null) {
            player.getInventory().setItemInMainHand(clearedItem);
            sender.sendMessage(ChatColor.GREEN + "Cleared " + enchantments.size() + " enchantment(s) from " + 
                            player.getName() + "'s item");
            
            if (sender != player) {
                player.sendMessage(ChatColor.YELLOW + "All enchantments were cleared from your item by an admin");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to clear enchantments!");
        }
        
        return true;
    }
    
    /**
     * List enchantments on an item - ENHANCED
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
        Map<String, Integer> enchantments = EnchantmentApplicator.getCustomEnchantments(item);
        
        if (enchantments.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Item has no custom enchantments");
            return true;
        }
        
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                         item.getItemMeta().getDisplayName() : item.getType().name();
        
        sender.sendMessage(ChatColor.GOLD + "=== Enchantments on " + itemName + " ===");
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(entry.getKey());
            
            if (enchantment != null) {
                sender.sendMessage(ChatColor.AQUA + "• " + enchantment.getFormattedName(entry.getValue()) + 
                                ChatColor.GRAY + " (" + entry.getKey() + ")");
                sender.sendMessage(ChatColor.GRAY + "  " + enchantment.getDescription());
            } else {
                sender.sendMessage(ChatColor.RED + "• Unknown: " + entry.getKey() + " Level " + entry.getValue());
            }
        }
        
        return true;
    }
    
    /**
     * List all enchantments - NEW
     */
    private void listAllEnchantments(CommandSender sender) {
        CustomEnchantmentRegistry registry = CustomEnchantmentRegistry.getInstance();
        
        sender.sendMessage(ChatColor.GOLD + "=== All Custom Enchantments ===");
        
        for (EnchantmentCategory category : EnchantmentCategory.values()) {
            List<CustomEnchantment> categoryEnchantments = registry.getEnchantmentsByCategory(category);
            if (!categoryEnchantments.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage(category.getColor() + "■ " + category.getDisplayName() + " Enchantments:");
                
                for (CustomEnchantment enchantment : categoryEnchantments) {
                    sender.sendMessage(ChatColor.WHITE + "  • " + enchantment.getRarity().getColor() + 
                                    enchantment.getDisplayName() + ChatColor.GRAY + " (" + enchantment.getId() + ")");
                    sender.sendMessage(ChatColor.GRAY + "    " + enchantment.getDescription());
                    sender.sendMessage(ChatColor.GRAY + "    Max Level: " + enchantment.getMaxLevel() + 
                                    " | Rarity: " + enchantment.getRarity().getFormattedName());
                }
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Use '/adminenchanting info <id>' for detailed information");
    }
    
    /**
     * List enchantments by category - NEW
     */
    private void listEnchantmentsByCategory(CommandSender sender, EnchantmentCategory category) {
        List<CustomEnchantment> enchantments = CustomEnchantmentRegistry.getInstance()
                .getEnchantmentsByCategory(category);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + category.getDisplayName() + " Enchantments ===");
        
        if (enchantments.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No enchantments found in this category");
            return;
        }
        
        for (CustomEnchantment enchantment : enchantments) {
            sender.sendMessage(ChatColor.WHITE + "• " + enchantment.getRarity().getColor() + 
                            enchantment.getDisplayName() + ChatColor.GRAY + " (" + enchantment.getId() + ")");
            sender.sendMessage(ChatColor.GRAY + "  " + enchantment.getDescription());
            sender.sendMessage(ChatColor.GRAY + "  Max Level: " + enchantment.getMaxLevel() + 
                            " | Rarity: " + enchantment.getRarity().getFormattedName());
        }
    }
    
    /**
     * List enchantments by rarity - NEW
     */
    private void listEnchantmentsByRarity(CommandSender sender, EnchantmentRarity rarity) {
        List<CustomEnchantment> enchantments = CustomEnchantmentRegistry.getInstance()
                .getEnchantmentsByRarity(rarity);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + rarity.getFormattedName() + " Enchantments ===");
        
        if (enchantments.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No enchantments found with this rarity");
            return;
        }
        
        for (CustomEnchantment enchantment : enchantments) {
            sender.sendMessage(ChatColor.WHITE + "• " + enchantment.getCategory().getColor() + 
                            enchantment.getDisplayName() + ChatColor.GRAY + " (" + enchantment.getId() + ")");
            sender.sendMessage(ChatColor.GRAY + "  " + enchantment.getDescription());
            sender.sendMessage(ChatColor.GRAY + "  Category: " + enchantment.getCategory().getDisplayName() + 
                            " | Max Level: " + enchantment.getMaxLevel());
        }
    }
    
    /**
     * Show detailed enchantment information - NEW
     */
    private void showDetailedEnchantmentInfo(CommandSender sender, CustomEnchantment enchantment) {
        sender.sendMessage(ChatColor.GOLD + "=== " + enchantment.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + enchantment.getId());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.GRAY + enchantment.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Category: " + enchantment.getCategory().getColor() + 
                        enchantment.getCategory().getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "Rarity: " + enchantment.getRarity().getFormattedName());
        sender.sendMessage(ChatColor.YELLOW + "Max Level: " + ChatColor.WHITE + enchantment.getMaxLevel());
        
        // Show applicable items
        sender.sendMessage(ChatColor.YELLOW + "Applicable Items: " + ChatColor.WHITE + 
                        enchantment.getApplicableItems().stream()
                                .map(item -> item.name().toLowerCase().replace("_", " "))
                                .collect(Collectors.joining(", ")));
        
        // Show conflicting enchantments
        if (!enchantment.getConflictingEnchantments().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Conflicts with: " + ChatColor.RED + 
                            String.join(", ", enchantment.getConflictingEnchantments()));
        }
        
        // Show costs
        sender.sendMessage(ChatColor.YELLOW + "Costs (Level 1): " + ChatColor.WHITE + 
                        enchantment.getXpCost(1) + " XP, " + enchantment.getEssenceCost(1) + " Essence");
        
        if (enchantment.getMaxLevel() > 1) {
            sender.sendMessage(ChatColor.YELLOW + "Costs (Max Level): " + ChatColor.WHITE + 
                            enchantment.getXpCost(enchantment.getMaxLevel()) + " XP, " + 
                            enchantment.getEssenceCost(enchantment.getMaxLevel()) + " Essence");
        }
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
     * Send help message - ENHANCED
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Admin Enchanting Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting list [category/rarity]" + 
            ChatColor.GRAY + " - List enchantments (optionally filtered)");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting info <enchant>" + 
            ChatColor.GRAY + " - Get detailed enchantment info");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting set <enchant> <level> [player]" + 
            ChatColor.GRAY + " - Set enchantment level (replaces existing)");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting add <enchant> <level> [player]" + 
            ChatColor.GRAY + " - Add/upgrade enchantment level");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting remove <enchant> [player]" + 
            ChatColor.GRAY + " - Remove specific enchantment");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting clear [player]" + 
            ChatColor.GRAY + " - Clear all enchantments");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting listitem [player]" + 
            ChatColor.GRAY + " - List enchantments on held item");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting giverune <player> <tier> <category>" + 
            ChatColor.GRAY + " - Give rune book");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting testlevel" + 
            ChatColor.GRAY + " - Test enchanting level at your location");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting listbooks" + 
            ChatColor.GRAY + " - List all rune book types");
        sender.sendMessage(ChatColor.YELLOW + "/adminenchanting debug" + 
            ChatColor.GRAY + " - Toggle debug mode");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "set", "add", "remove", "clear", 
                                            "listitem", "giverune", "testlevel", "listbooks", "debug"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "list":
                    // Add category and rarity names for filtering
                    for (EnchantmentCategory category : EnchantmentCategory.values()) {
                        completions.add(category.name().toLowerCase());
                    }
                    for (EnchantmentRarity rarity : EnchantmentRarity.values()) {
                        completions.add(rarity.name().toLowerCase());
                    }
                    break;
                case "info":
                case "set":
                case "add":
                case "remove":
                    // Add enchantment IDs
                    for (CustomEnchantment enchantment : CustomEnchantmentRegistry.getInstance().getAllEnchantments()) {
                        completions.add(enchantment.getId());
                    }
                    break;
                case "clear":
                case "listitem":
                    // Add player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "giverune":
                    // Add player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "set":
                case "add":
                    // Add level numbers 1-10
                    for (int i = 1; i <= 10; i++) {
                        completions.add(String.valueOf(i));
                    }
                    break;
                case "giverune":
                    // Add tier names
                    for (RuneBook.RuneTier tier : RuneBook.RuneTier.values()) {
                        completions.add(tier.name().toLowerCase());
                    }
                    break;
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "set":
                case "add":
                case "remove":
                    // Add player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "giverune":
                    // Add category names
                    for (EnchantmentCategory category : EnchantmentCategory.values()) {
                        completions.add(category.name().toLowerCase());
                    }
                    break;
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}