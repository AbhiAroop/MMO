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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.EnchantmentRegistry;
import com.server.enchantments.FragmentRegistry;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.FragmentTier;
import com.server.enchantments.items.EnchantmentTome;
import com.server.enchantments.structure.EnchantmentTableStructure;
import com.server.enchantments.utils.EquipmentTypeValidator;

/**
 * Admin command for enchantment system
 * /enchant <subcommand> [args...]
 */
public class EnchantCommand implements CommandExecutor, TabCompleter {
    
    private boolean debugMode = false;
    private final EnchantmentTableStructure structureManager;
    
    public EnchantCommand(EnchantmentTableStructure structureManager) {
        this.structureManager = structureManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mmo.admin.enchant")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                return handleAdd(sender, args);
            case "give":
                return handleGive(sender, args);
            case "test":
                return handleTest(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "inspect":
                return handleInspect(sender, args);
            case "list":
                return handleList(sender, args);
            case "debug":
                return handleDebug(sender, args);
            case "clear":
                return handleClear(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "spawn":
                return handleSpawn(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    /**
     * /enchant add <enchantmentId> [quality] [level]
     * Add an enchantment directly to held item
     */
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if holding an item
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must be holding an item!");
            return true;
        }
        
        // Parse arguments
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /enchant add <enchantmentId> [quality] [level]");
            player.sendMessage(ChatColor.GRAY + "Available qualities: POOR, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, GODLY");
            player.sendMessage(ChatColor.GRAY + "Available levels: 1-8 (I-VIII)");
            player.sendMessage(ChatColor.GRAY + "Example: /enchant add emberveil LEGENDARY 5");
            return true;
        }
        
        String enchantId = args[1].toLowerCase();
        EnchantmentQuality quality = EnchantmentQuality.COMMON; // Default
        com.server.enchantments.data.EnchantmentLevel level = 
            com.server.enchantments.data.EnchantmentLevel.I; // Default
        
        // Parse quality if provided
        if (args.length >= 3) {
            try {
                quality = EnchantmentQuality.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid quality: " + args[2]);
                player.sendMessage(ChatColor.GRAY + "Available: POOR, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, GODLY");
                return true;
            }
        }
        
        // Parse level if provided
        if (args.length >= 4) {
            try {
                int levelNum = Integer.parseInt(args[3]);
                if (levelNum < 1 || levelNum > 8) {
                    player.sendMessage(ChatColor.RED + "Invalid level: " + args[3]);
                    player.sendMessage(ChatColor.GRAY + "Level must be 1-8 (I-VIII)");
                    return true;
                }
                level = com.server.enchantments.data.EnchantmentLevel.fromNumeric(levelNum);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level: " + args[3]);
                player.sendMessage(ChatColor.GRAY + "Level must be a number 1-8");
                return true;
            }
        }
        
        // Get enchantment from registry
        EnchantmentRegistry registry = EnchantmentRegistry.getInstance();
        CustomEnchantment enchantment = registry.getEnchantment(enchantId);
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchantment: " + enchantId);
            player.sendMessage(ChatColor.GRAY + "Available enchantments:");
            for (CustomEnchantment e : registry.getAllEnchantments()) {
                player.sendMessage(ChatColor.GRAY + "  - " + e.getId() + " (" + e.getDisplayName() + ")");
            }
            return true;
        }
        
        // Validate equipment compatibility using custom model data
        if (!EquipmentTypeValidator.canEnchantmentApply(item, enchantment)) {
            player.sendMessage(EquipmentTypeValidator.getIncompatibilityMessage(item, enchantment));
            return true;
        }
        
        // Warn if level exceeds enchantment's max (admin bypass)
        if (level.getNumericLevel() > enchantment.getMaxLevel()) {
            player.sendMessage(ChatColor.YELLOW + "⚠ Warning: " + enchantment.getDisplayName() + 
                              " has a max level of " + enchantment.getMaxLevel() + 
                              " (I-" + EnchantmentLevel.fromNumeric(enchantment.getMaxLevel()).getRoman() + ")");
            player.sendMessage(ChatColor.YELLOW + "⚠ Applying level " + level.getDisplayName() + 
                              " as admin override.");
        }
        
        // Apply enchantment with level
        EnchantmentData.addEnchantmentToItem(item, enchantment, quality, level);
        
        player.sendMessage(ChatColor.GREEN + "✓ Added enchantment: " + enchantment.getDisplayName() + 
                          " " + level.getDisplayName() + " " +
                          quality.getColor() + "[" + quality.getDisplayName() + "]");
        player.sendMessage(ChatColor.GRAY + "Item has been updated in your hand.");
        
        return true;
    }
    
    /**
     * /enchant give <player> <element> <tier> [amount]
     * Give fragments to a player
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant give <player> <element/tome> <tier> [amount]");
            sender.sendMessage(ChatColor.YELLOW + "Special: /enchant give <player> tome");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        
        // Special case: "tome" gives an enchantment tome
        if (args[2].equalsIgnoreCase("tome")) {
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    amount = 1;
                }
            }
            
            for (int i = 0; i < amount; i++) {
                ItemStack tome = EnchantmentTome.createUnenchantedTome();
                target.getInventory().addItem(tome);
            }
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x Enchantment Tome to " + target.getName());
            return true;
        }
        
        // Special case: "all" gives all fragments
        if (args[2].equalsIgnoreCase("all")) {
            FragmentRegistry.giveAllFragments(target);
            sender.sendMessage(ChatColor.GREEN + "Gave all fragments to " + target.getName());
            return true;
        }
        
        ElementType element;
        try {
            element = ElementType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid element: " + args[2]);
            sender.sendMessage(ChatColor.YELLOW + "Valid: " + Arrays.toString(ElementType.values()));
            return true;
        }
        
        FragmentTier tier;
        try {
            tier = FragmentTier.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid tier: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "Valid: " + Arrays.toString(FragmentTier.values()));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[4]);
                return true;
            }
        }
        
        FragmentRegistry.giveFragment(target, element, tier, amount);
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + 
                         tier.getDisplayName() + " " + element.getDisplayName() + 
                         " Fragment to " + target.getName());
        return true;
    }
    
    /**
     * /enchant test <player> <enchantment> <quality>
     * Create a test item with enchantment
     */
    private boolean handleTest(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant test <player> <enchantment> <quality>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        
        CustomEnchantment enchant = EnchantmentRegistry.getInstance().getEnchantment(args[2]);
        if (enchant == null) {
            sender.sendMessage(ChatColor.RED + "Enchantment not found: " + args[2]);
            return true;
        }
        
        EnchantmentQuality quality;
        try {
            quality = EnchantmentQuality.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid quality: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "Valid: " + Arrays.toString(EnchantmentQuality.values()));
            return true;
        }
        
        // Create appropriate test item
        ItemStack testItem;
        if (enchant.getId().equals("ember_veil")) {
            testItem = new ItemStack(Material.DIAMOND_CHESTPLATE);
        } else if (enchant.getId().equals("inferno_strike")) {
            testItem = new ItemStack(Material.DIAMOND_SWORD);
        } else if (enchant.getId().equals("frostflow")) {
            testItem = new ItemStack(Material.DIAMOND_SWORD);
        } else {
            testItem = new ItemStack(Material.DIAMOND_SWORD);
        }
        
        EnchantmentData.addEnchantmentToItem(testItem, enchant, quality);
        target.getInventory().addItem(testItem);
        
        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " a test " + 
                         enchant.getDisplayName() + " " + quality.getDisplayName() + " item");
        return true;
    }
    
    /**
     * /enchant info <enchantment_id>
     * Display enchantment details
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant info <enchantment_id>");
            return true;
        }
        
        CustomEnchantment enchant = EnchantmentRegistry.getInstance().getEnchantment(args[1]);
        if (enchant == null) {
            sender.sendMessage(ChatColor.RED + "Enchantment not found: " + args[1]);
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== " + enchant.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + enchant.getId());
        sender.sendMessage(ChatColor.YELLOW + "Element: " + ChatColor.WHITE + 
                         (enchant.isHybrid() ? enchant.getHybridElement().getDisplayName() : 
                          enchant.getElement().getDisplayName()));
        sender.sendMessage(ChatColor.YELLOW + "Rarity: " + enchant.getRarity().getColor() + 
                         enchant.getRarity().getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "Max Level: " + ChatColor.WHITE + 
                         enchant.getMaxLevel() + " " + ChatColor.GRAY + 
                         "(I-" + EnchantmentLevel.fromNumeric(enchant.getMaxLevel()).getRoman() + ")");
        sender.sendMessage(ChatColor.YELLOW + "Trigger: " + ChatColor.WHITE + 
                         enchant.getTriggerType().name());
        sender.sendMessage(ChatColor.YELLOW + "Equipment: " + ChatColor.WHITE + 
                         EquipmentTypeValidator.getEquipmentDescription(enchant));
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.GRAY + 
                         enchant.getDescription());
        
        double[] baseStats = enchant.getBaseStats();
        sender.sendMessage(ChatColor.YELLOW + "Base Stats: " + ChatColor.WHITE + 
                         Arrays.toString(baseStats));
        
        sender.sendMessage(ChatColor.YELLOW + "Quality Scaling:");
        for (EnchantmentQuality quality : EnchantmentQuality.values()) {
            double[] scaled = enchant.getScaledStats(quality);
            sender.sendMessage("  " + quality.getColor() + quality.getDisplayName() + 
                             ChatColor.GRAY + ": " + Arrays.toString(scaled));
        }
        
        // Display anti-synergy information
        String[] conflicts = enchant.getConflictingEnchantments();
        if (conflicts.length > 0) {
            sender.sendMessage(ChatColor.RED + "⚠ Conflicts With: " + ChatColor.GRAY + 
                             String.join(", ", conflicts));
            sender.sendMessage(ChatColor.DARK_RED + "  (Cannot be on the same item)");
        }
        
        return true;
    }
    
    /**
     * /enchant inspect [player]
     * View enchantments on held item or player's gear
     */
    private boolean handleInspect(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must specify a player from console");
                return true;
            }
            target = (Player) sender;
        }
        
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is not holding an item");
            return true;
        }
        
        List<EnchantmentData> enchants = EnchantmentData.getEnchantmentsFromItem(item);
        if (enchants.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No custom enchantments on this item");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== " + target.getName() + "'s " + 
                         item.getType().name() + " ===");
        for (int i = 0; i < enchants.size(); i++) {
            EnchantmentData data = enchants.get(i);
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i+1) + ". " + data.getEnchantmentId() + 
                             " " + data.getQuality().getColor() + 
                             data.getQuality().getDisplayName());
            sender.sendMessage(ChatColor.GRAY + "   Stats: " + Arrays.toString(data.getScaledStats()));
            sender.sendMessage(ChatColor.GRAY + "   Affinity: " + data.getAffinityValue());
        }
        
        return true;
    }
    
    /**
     * /enchant list [element|rarity]
     * List all enchantments or filter by element/rarity
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // List all
            sender.sendMessage(ChatColor.GOLD + "=== All Enchantments ===");
            for (CustomEnchantment enchant : EnchantmentRegistry.getInstance().getAllEnchantments()) {
                sender.sendMessage(enchant.getRarity().getColor() + enchant.getDisplayName() + 
                                 ChatColor.GRAY + " (" + enchant.getId() + ")");
            }
            sender.sendMessage(ChatColor.YELLOW + "Total: " + 
                             EnchantmentRegistry.getInstance().getEnchantmentCount());
            return true;
        }
        
        String filter = args[1].toUpperCase();
        
        // Try element filter
        try {
            ElementType element = ElementType.valueOf(filter);
            List<CustomEnchantment> enchants = EnchantmentRegistry.getInstance()
                    .getEnchantmentsByElement(element);
            sender.sendMessage(ChatColor.GOLD + "=== " + element.getDisplayName() + 
                             " Enchantments ===");
            for (CustomEnchantment enchant : enchants) {
                sender.sendMessage(enchant.getRarity().getColor() + enchant.getDisplayName() + 
                                 ChatColor.GRAY + " (" + enchant.getId() + ")");
            }
            sender.sendMessage(ChatColor.YELLOW + "Total: " + enchants.size());
            return true;
        } catch (IllegalArgumentException ignored) {}
        
        // Try rarity filter
        try {
            EnchantmentRarity rarity = EnchantmentRarity.valueOf(filter);
            List<CustomEnchantment> enchants = EnchantmentRegistry.getInstance()
                    .getEnchantmentsByRarity(rarity);
            sender.sendMessage(ChatColor.GOLD + "=== " + rarity.getColor() + 
                             rarity.getDisplayName() + " Enchantments ===");
            for (CustomEnchantment enchant : enchants) {
                sender.sendMessage(enchant.getRarity().getColor() + enchant.getDisplayName() + 
                                 ChatColor.GRAY + " (" + enchant.getId() + ")");
            }
            sender.sendMessage(ChatColor.YELLOW + "Total: " + enchants.size());
            return true;
        } catch (IllegalArgumentException ignored) {}
        
        sender.sendMessage(ChatColor.RED + "Invalid filter: " + filter);
        return true;
    }
    
    /**
     * /enchant debug [on|off]
     * Toggle debug mode
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            debugMode = !debugMode;
        } else {
            debugMode = args[1].equalsIgnoreCase("on");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Debug mode: " + 
                         (debugMode ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        return true;
    }
    
    /**
     * /enchant clear <player> [slot]
     * Remove enchantments from item
     */
    private boolean handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant clear <player> [slot]");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is not holding an item");
            return true;
        }
        
        int count = EnchantmentData.getEnchantmentCount(item);
        EnchantmentData.clearEnchantments(item);
        
        sender.sendMessage(ChatColor.GREEN + "Cleared " + count + " enchantments from " + 
                         target.getName() + "'s item");
        target.sendMessage(ChatColor.YELLOW + "Your item's enchantments were cleared");
        return true;
    }
    
    /**
     * /enchant spawn
     * Spawn an enchantment altar at player's location
     */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        Location spawnLoc = player.getLocation();
        
        // Position armor stand lower so enchanting table helmet is at floor level
        // Armor stands are 1.975 blocks tall, we want the helmet at ground level
        Location armorStandLoc = spawnLoc.clone().subtract(0, 1.5, 0);
        
        // Spawn armor stand
        ArmorStand altar = (ArmorStand) player.getWorld().spawnEntity(armorStandLoc, EntityType.ARMOR_STAND);
        
        // Configure armor stand to match old altar setup
        altar.setGravity(false);
        altar.setVisible(false);  // Invisible armor stand
        altar.setArms(false);
        altar.setBasePlate(false);  // No base plate
        altar.setMarker(true);  // Marker mode - no hitbox, can't be pushed
        altar.setInvulnerable(true);  // Can't be damaged/killed
        altar.setCustomName(ChatColor.DARK_PURPLE + "Enchantment Altar");
        altar.setCustomNameVisible(true);
        altar.setPersistent(true);  // Don't despawn
        
        // Set enchanting table as helmet (positioned at floor level)
        ItemStack helmet = new ItemStack(Material.ENCHANTING_TABLE);
        altar.getEquipment().setHelmet(helmet, true);
        
        // Lock equipment slots so helmet can't be removed
        for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
            altar.addEquipmentLock(slot, org.bukkit.entity.ArmorStand.LockType.REMOVING_OR_CHANGING);
        }
        
        // Register the altar
        structureManager.registerAltar(altar);
        
        player.sendMessage(ChatColor.GREEN + "Spawned enchantment altar at your location!");
        player.sendMessage(ChatColor.GRAY + "Right-click the enchanting table to use it.");
        return true;
    }
    
    /**
     * /enchant reload
     * Reload enchantment system
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading enchantment system...");
        // Force registry recreation
        EnchantmentRegistry.getInstance();
        sender.sendMessage(ChatColor.GREEN + "Enchantment system reloaded!");
        sender.sendMessage(ChatColor.GRAY + "Registered: " + 
                         EnchantmentRegistry.getInstance().getEnchantmentCount() + " enchantments");
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Enchantment Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/enchant add <enchantmentId> [quality]" + 
                         ChatColor.GRAY + " - Add enchantment to held item");
        sender.sendMessage(ChatColor.YELLOW + "/enchant give <player> <element> <tier> [amount]" + 
                         ChatColor.GRAY + " - Give fragments");
        sender.sendMessage(ChatColor.YELLOW + "/enchant test <player> <enchantment> <quality>" + 
                         ChatColor.GRAY + " - Create test item");
        sender.sendMessage(ChatColor.YELLOW + "/enchant info <enchantment_id>" + 
                         ChatColor.GRAY + " - View enchantment details");
        sender.sendMessage(ChatColor.YELLOW + "/enchant inspect [player]" + 
                         ChatColor.GRAY + " - View item enchantments");
        sender.sendMessage(ChatColor.YELLOW + "/enchant list [element|rarity]" + 
                         ChatColor.GRAY + " - List enchantments");
        sender.sendMessage(ChatColor.YELLOW + "/enchant debug [on|off]" + 
                         ChatColor.GRAY + " - Toggle debug mode");
        sender.sendMessage(ChatColor.YELLOW + "/enchant clear <player>" + 
                         ChatColor.GRAY + " - Clear item enchantments");
        sender.sendMessage(ChatColor.YELLOW + "/enchant spawn" + 
                         ChatColor.GRAY + " - Spawn enchantment altar");
        sender.sendMessage(ChatColor.YELLOW + "/enchant reload" + 
                         ChatColor.GRAY + " - Reload system");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            completions.addAll(Arrays.asList("add", "give", "test", "info", "inspect", "list", 
                                            "debug", "clear", "reload", "spawn"));
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            
            // Second argument depends on subcommand
            if (subCmd.equals("add")) {
                // Suggest enchantment IDs for /enchant add
                completions.addAll(EnchantmentRegistry.getInstance().getAllEnchantments()
                        .stream().map(CustomEnchantment::getId).collect(Collectors.toList()));
            } else if (subCmd.equals("give") || subCmd.equals("test") || subCmd.equals("clear") || 
                       subCmd.equals("inspect")) {
                // Suggest player names
                return null; // Bukkit will provide player names
            } else if (subCmd.equals("info")) {
                // Suggest enchantment IDs for info
                completions.addAll(EnchantmentRegistry.getInstance().getAllEnchantments()
                        .stream().map(CustomEnchantment::getId).collect(Collectors.toList()));
            } else if (subCmd.equals("list")) {
                // Suggest element types or rarities
                for (ElementType e : ElementType.values()) completions.add(e.name().toLowerCase());
                for (EnchantmentRarity r : EnchantmentRarity.values()) completions.add(r.name().toLowerCase());
            } else if (subCmd.equals("debug")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            
            if (subCmd.equals("add")) {
                // Third argument for /enchant add - quality
                for (EnchantmentQuality q : EnchantmentQuality.values()) {
                    completions.add(q.name().toLowerCase());
                }
            } else if (subCmd.equals("give")) {
                // Third argument for give - element type or "tome"
                completions.add("all");
                completions.add("tome");
                for (ElementType e : ElementType.values()) completions.add(e.name().toLowerCase());
            } else if (subCmd.equals("test")) {
                // Third argument for test - enchantment ID
                completions.addAll(EnchantmentRegistry.getInstance().getAllEnchantments()
                        .stream().map(CustomEnchantment::getId).collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            String subCmd = args[0].toLowerCase();
            
            if (subCmd.equals("add")) {
                // Fourth argument for /enchant add - level (1-8)
                completions.addAll(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8"));
            } else if (subCmd.equals("give")) {
                // Fourth argument for give - tier
                for (FragmentTier t : FragmentTier.values()) completions.add(t.name().toLowerCase());
            } else if (subCmd.equals("test")) {
                // Fourth argument for test - quality
                for (EnchantmentQuality q : EnchantmentQuality.values()) completions.add(q.name().toLowerCase());
            }
        }
        
        // Filter completions based on what user has typed
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
}
