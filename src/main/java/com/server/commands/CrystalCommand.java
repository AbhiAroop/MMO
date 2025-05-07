package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

import com.server.Main;
import com.server.utils.NamespacedKeyUtils;

/**
 * Command handler for crystal and gem-related functionality
 * for the GemCarving subskill
 */
public class CrystalCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    private final Random random = new Random();
    
    // List of available crystal types in ascending order of value/difficulty
    private final List<String> crystalTypes = Arrays.asList(
        "mooncrystal", "azuralite", "pyrethine", "solvanecrystal", 
        "nyxstone", "lucenthar", "veyrithcrystal", "drakthyst"
    );
    
    public CrystalCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("mmo.command.crystal")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "summon":
                return handleSummon(player, args);
            case "remove":
                return handleRemove(player, args);
            case "give":
                return handleGive(player, args);
            default:
                showHelp(player);
                return true;
        }
    }
    
    /**
     * Handle the summon subcommand - create a crystal at player's location
     */
    private boolean handleSummon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /crystal summon <type> [quality] [size] [rotation] [tiltX] [tiltZ]");
            return true;
        }
        
        String type = args[1].toLowerCase();
        if (!crystalTypes.contains(type)) {
            player.sendMessage(ChatColor.RED + "Invalid crystal type. Available types: " + 
                    String.join(", ", crystalTypes));
            return true;
        }
        
        // Get optional quality parameter (1-100, default 50)
        int quality = 50;
        if (args.length >= 3) {
            try {
                quality = Integer.parseInt(args[2]);
                quality = Math.max(1, Math.min(100, quality));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Quality must be a number between 1-100.");
                return true;
            }
        }
        
        // Get optional size parameter (0.5-2.0, default 1.0)
        double size = 1.0;
        if (args.length >= 4) {
            try {
                size = Double.parseDouble(args[3]);
                size = Math.max(0.5, Math.min(2.0, size));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Size must be a number between 0.5-2.0.");
                return true;
            }
        }
        
        // Get optional rotation parameter (0-360, default random)
        Float rotation = null; // null means random rotation
        if (args.length >= 5) {
            try {
                rotation = Float.parseFloat(args[4]);
                // Keep rotation within 0-360 range
                rotation = (rotation % 360 + 360) % 360;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Rotation must be a number.");
                return true;
            }
        }
        
        // Get optional tiltX parameter (any value, default random small tilt)
        Float tiltX = null; // null means random small tilt
        if (args.length >= 6) {
            try {
                tiltX = Float.parseFloat(args[5]);
                // No limitations on tilt angles now
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "TiltX must be a number.");
                return true;
            }
        }
        
        // Get optional tiltZ parameter (any value, default random small tilt)
        Float tiltZ = null; // null means random small tilt
        if (args.length >= 7) {
            try {
                tiltZ = Float.parseFloat(args[6]);
                // No limitations on tilt angles now
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "TiltZ must be a number.");
                return true;
            }
        }
        
        // Create the crystal
        Location location = player.getLocation();
        ArmorStand crystal = summonCrystal(location, type, quality, size, rotation, tiltX, tiltZ);
        
        if (crystal == null) {
            player.sendMessage(ChatColor.RED + "Failed to create crystal.");
            return true;
        }
        
        // Build a message with all the properties that were set
        StringBuilder message = new StringBuilder(ChatColor.GREEN + "Successfully summoned a " + 
                formatCrystalName(type) + ChatColor.GREEN + 
                " (Quality: " + ChatColor.YELLOW + quality + ChatColor.GREEN +
                ", Size: " + ChatColor.YELLOW + size + ChatColor.GREEN);
        
        if (rotation != null) {
            message.append(", Rotation: " + ChatColor.YELLOW + String.format("%.1f", rotation) + "°" + ChatColor.GREEN);
        }
        
        if (tiltX != null || tiltZ != null) {
            message.append(", Tilt: " + ChatColor.YELLOW);
            if (tiltX != null) {
                message.append("X=" + String.format("%.1f", tiltX) + "°");
            }
            if (tiltZ != null) {
                message.append(tiltX != null ? ", " : "");
                message.append("Z=" + String.format("%.1f", tiltZ) + "°");
            }
            message.append(ChatColor.GREEN);
        }
        
        message.append(")");
        player.sendMessage(message.toString());
        return true;
    }
    
    /**
     * Handle the remove subcommand - remove nearby crystals with optional range parameter
     */
    private boolean handleRemove(Player player, String[] args) {
        // Default range is 3.0 blocks
        double range = 3.0;
        
        // Allow custom range if specified
        if (args.length >= 2) {
            try {
                range = Double.parseDouble(args[1]);
                // Set reasonable limits
                range = Math.max(0.5, Math.min(20.0, range));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid range. Using default of 3.0 blocks.");
            }
        }
        
        int count = 0;
        
        for (ArmorStand entity : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (entity.getLocation().distance(player.getLocation()) <= range) {
                PersistentDataContainer container = entity.getPersistentDataContainer();
                if (container.has(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING)) {
                    entity.remove();
                    count++;
                }
            }
        }
        
        if (count > 0) {
            player.sendMessage(ChatColor.GREEN + "Removed " + count + " crystal(s) within " + String.format("%.1f", range) + " blocks.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "No crystals found within " + String.format("%.1f", range) + " blocks.");
        }
        return true;
    }
    
    /**
     * Handle the give subcommand - give a crystal item
     */
    private boolean handleGive(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /crystal give <type> [quality]");
            return true;
        }
        
        String type = args[1].toLowerCase();
        if (!crystalTypes.contains(type)) {
            player.sendMessage(ChatColor.RED + "Invalid crystal type. Available types: " + 
                    String.join(", ", crystalTypes));
            return true;
        }
        
        // Get optional quality parameter (1-100, default 50)
        int quality = 50;
        if (args.length >= 3) {
            try {
                quality = Integer.parseInt(args[2]);
                quality = Math.max(1, Math.min(100, quality));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Quality must be a number between 1-100.");
                return true;
            }
        }
        
        // Create and give the crystal item
        ItemStack crystalItem = createCrystalItem(type, quality);
        player.getInventory().addItem(crystalItem);
        
        player.sendMessage(ChatColor.GREEN + "You received a " + 
                formatCrystalName(type) + ChatColor.GREEN + 
                " (Quality: " + ChatColor.YELLOW + quality + ChatColor.GREEN + ")");
        return true;
    }
    
    /**
     * Create a crystal at the specified location
     */
    private ArmorStand summonCrystal(Location location, String type, int quality, double size) {
        // Call the expanded method with null for optional parameters
        return summonCrystal(location, type, quality, size, null, null, null);
    }

    /**
     * Create a crystal at the specified location with custom rotation and tilt
     */
    private ArmorStand summonCrystal(Location location, String type, int quality, double size, 
                                    Float customRotation, Float customTiltX, Float customTiltZ) {
        // Create the armor stand
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        // Configure armor stand properties
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSilent(true);
        stand.setCustomNameVisible(false);
        stand.setMarker(true); // Keep marker true to prevent movement and collision
        
        // Set the custom model data item as the head
        ItemStack head = createCrystalDisplayItem(type, quality);
        stand.getEquipment().setHelmet(head);
        
        // Apply rotation - either custom or random
        float rotation = (customRotation != null) ? customRotation : random.nextFloat() * 360;
        stand.setRotation(rotation, 0);
        
        // Apply tilt - either custom or random
        float tiltX = (customTiltX != null) ? 
                    (float)Math.toRadians(customTiltX) : 
                    (random.nextFloat() - 0.5f) * 0.3f;
        
        float tiltZ = (customTiltZ != null) ? 
                    (float)Math.toRadians(customTiltZ) : 
                    (random.nextFloat() - 0.5f) * 0.3f;
        
        stand.setHeadPose(new EulerAngle(tiltX, 0, tiltZ));
        
        // Apply size scaling through position adjustment
        if (size != 1.0) {
            // Adjust the Y position based on size to prevent floating/sinking
            Location adjustedLocation = stand.getLocation();
            
            // For larger sizes, raise the armor stand slightly
            if (size > 1.0) {
                adjustedLocation.add(0, (size - 1.0) * 0.5, 0); // Increased Y offset for normal sized stand
            } 
            // For smaller sizes, lower the armor stand slightly
            else if (size < 1.0) {
                adjustedLocation.subtract(0, (1.0 - size) * 0.25, 0); // Adjusted Y offset for normal sized stand
            }
            
            stand.teleport(adjustedLocation);
        }
        
        // Store crystal data in persistent data container
        PersistentDataContainer container = stand.getPersistentDataContainer();
        container.set(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING, type);
        container.set(NamespacedKeyUtils.getCrystalQualityKey(plugin), PersistentDataType.INTEGER, quality);
        container.set(NamespacedKeyUtils.getCrystalSizeKey(plugin), PersistentDataType.DOUBLE, size);
        
        // Store rotation and tilt data if specified
        if (customRotation != null) {
            container.set(NamespacedKeyUtils.getCrystalRotationKey(plugin), PersistentDataType.FLOAT, customRotation);
        }
        if (customTiltX != null) {
            container.set(NamespacedKeyUtils.getCrystalTiltXKey(plugin), PersistentDataType.FLOAT, customTiltX);
        }
        if (customTiltZ != null) {
            container.set(NamespacedKeyUtils.getCrystalTiltZKey(plugin), PersistentDataType.FLOAT, customTiltZ);
        }
        
        return stand;
    }

    /**
     * Show help information to the player
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "=== Crystal Command Help ===");
        player.sendMessage(ChatColor.YELLOW + "/crystal summon <type> [quality] [size] [rotation] [tiltX] [tiltZ]" + ChatColor.WHITE + " - Summon a crystal");
        player.sendMessage(ChatColor.YELLOW + "/crystal remove [range]" + ChatColor.WHITE + " - Remove nearby crystals");
        player.sendMessage(ChatColor.YELLOW + "/crystal give <type> [quality]" + ChatColor.WHITE + " - Get a crystal item");
        player.sendMessage(ChatColor.GREEN + "Available crystal types (in order of difficulty): ");
        player.sendMessage(ChatColor.GREEN + String.join(", ", crystalTypes));
    }
    
    /**
     * Create an item that represents the crystal for display on armor stand
     */
    private ItemStack createCrystalDisplayItem(String type, int quality) {
        ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data based on crystal type
        int customModelData = getCrystalModelData(type);
        meta.setCustomModelData(customModelData);
        
        // Store crystal data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING, type);
        container.set(NamespacedKeyUtils.getCrystalQualityKey(plugin), PersistentDataType.INTEGER, quality);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a crystal item that can be given to players
     */
    private ItemStack createCrystalItem(String type, int quality) {
        ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data based on crystal type
        int customModelData = getCrystalModelData(type);
        meta.setCustomModelData(customModelData);
        
        // Set display name based on quality and type
        String qualityText = getQualityPrefix(quality);
        String typeName = formatCrystalName(type);
        meta.setDisplayName(qualityText + typeName);
        
        // Create lore with description and quality indicator
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + getCrystalDescription(type));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Quality: " + getQualityColor(quality) + quality + "%");
        lore.add(ChatColor.YELLOW + "Difficulty: " + getDifficultyLabel(type));
        
        // Add special effects based on crystal type
        addSpecialEffectsLore(lore, type, quality);
        
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "Use with the GemCarving skill");
        meta.setLore(lore);
        
        // Store crystal data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING, type);
        container.set(NamespacedKeyUtils.getCrystalQualityKey(plugin), PersistentDataType.INTEGER, quality);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get the custom model data for a crystal type
     */
    private int getCrystalModelData(String type) {
        switch (type) {
            case "mooncrystal":
                return 10001; // Custom model for mooncrystal
            case "azuralite":
                return 10002; // Custom model for azuralite
            case "pyrethine":
                return 10003; // Custom model for pyrethine
            case "solvanecrystal":
                return 10004; // Custom model for solvane crystal
            case "nyxstone":
                return 10005; // Custom model for nyxstone
            case "lucenthar":
                return 10006; // Custom model for lucenthar
            case "veyrithcrystal":
                return 10007; // Custom model for veyrith crystal
            case "drakthyst":
                return 10008; // Custom model for drakthyst
            default:
                return 10001; // Default to mooncrystal
        }
    }
    
    /**
     * Get the difficulty label based on crystal type
     */
    private String getDifficultyLabel(String type) {
        switch (type) {
            case "mooncrystal":
                return ChatColor.GREEN + "Basic";
            case "azuralite":
                return ChatColor.GREEN + "Easy";
            case "pyrethine":
                return ChatColor.YELLOW + "Moderate";
            case "solvanecrystal":
                return ChatColor.YELLOW + "Moderate";
            case "nyxstone":
                return ChatColor.GOLD + "Challenging";
            case "lucenthar":
                return ChatColor.GOLD + "Challenging";
            case "veyrithcrystal":
                return ChatColor.RED + "Difficult";
            case "drakthyst":
                return ChatColor.RED + "Very Difficult";
            default:
                return ChatColor.GREEN + "Basic";
        }
    }
    
    /**
     * Get a description for a crystal type
     */
    private String getCrystalDescription(String type) {
        switch (type) {
            case "mooncrystal":
                return "A luminous crystal that glows with pale blue light";
            case "azuralite":
                return "An azure crystal with swirling blue patterns inside";
            case "pyrethine":
                return "A fiery crystal with crackling energy at its core";
            case "solvanecrystal":
                return "A golden crystal that radiates warmth and sunlight";
            case "nyxstone":
                return "A deep purple crystal that seems to absorb nearby shadows";
            case "lucenthar":
                return "A brilliant green crystal with shifting internal patterns";
            case "veyrithcrystal":
                return "A magenta crystal that pulses with strange energies";
            case "drakthyst":
                return "A blood-red crystal with a beating core of dark energy";
            default:
                return "A mysterious crystal";
        }
    }
    
    /**
     * Add special effects lore based on crystal type
     */
    private void addSpecialEffectsLore(List<String> lore, String type, int quality) {
        lore.add(ChatColor.AQUA + "✦ Special Effects:");
        
        switch (type) {
            case "mooncrystal":
                lore.add(ChatColor.BLUE + "• Night Vision (" + (quality / 20) + ")");
                lore.add(ChatColor.BLUE + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "azuralite":
                lore.add(ChatColor.AQUA + "• Water Breathing (" + (quality / 20) + ")");
                lore.add(ChatColor.AQUA + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "pyrethine":
                lore.add(ChatColor.RED + "• Fire Resistance (" + (quality / 20) + ")");
                lore.add(ChatColor.RED + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "solvanecrystal":
                lore.add(ChatColor.GOLD + "• Haste (" + (quality / 20) + ")");
                lore.add(ChatColor.GOLD + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "nyxstone":
                lore.add(ChatColor.DARK_PURPLE + "• Night Vision (" + (quality / 20) + ")");
                lore.add(ChatColor.DARK_PURPLE + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "lucenthar":
                lore.add(ChatColor.GREEN + "• Jump Boost (" + (quality / 20) + ")");
                lore.add(ChatColor.GREEN + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "veyrithcrystal":
                lore.add(ChatColor.LIGHT_PURPLE + "• Speed (" + (quality / 20) + ")");
                lore.add(ChatColor.LIGHT_PURPLE + "• " + calculateRewardXp(type) + " Base XP");
                break;
            case "drakthyst":
                lore.add(ChatColor.DARK_RED + "• Strength (" + (quality / 20) + ")");
                lore.add(ChatColor.DARK_RED + "• " + calculateRewardXp(type) + " Base XP");
                break;
        }
        
        // Add extraction success rate impact
        int index = crystalTypes.indexOf(type);
        if (index >= 0) {
            double difficultyImpact = 0.05 * index;
            lore.add(ChatColor.RED + "• -" + String.format("%.0f", difficultyImpact * 100) + "% Extraction Success");
        }
    }
    
    /**
     * Calculate the XP reward for a crystal type
     */
    private int calculateRewardXp(String type) {
        switch (type) {
            case "mooncrystal": return 100;
            case "azuralite": return 125;
            case "pyrethine": return 150;
            case "solvanecrystal": return 175;
            case "nyxstone": return 200;
            case "lucenthar": return 250;
            case "veyrithcrystal": return 300;
            case "drakthyst": return 400;
            default: return 100;
        }
    }
    
    /**
     * Format a crystal type name for display
     */
    private String formatCrystalName(String type) {
        switch (type) {
            case "mooncrystal":
                return ChatColor.BLUE + "Moon Crystal";
            case "azuralite":
                return ChatColor.AQUA + "Azuralite";
            case "pyrethine":
                return ChatColor.RED + "Pyrethine";
            case "solvanecrystal":
                return ChatColor.GOLD + "Solvane Crystal";
            case "nyxstone":
                return ChatColor.DARK_PURPLE + "Nyxstone";
            case "lucenthar":
                return ChatColor.GREEN + "Lucenthar";
            case "veyrithcrystal":
                return ChatColor.LIGHT_PURPLE + "Veyrith Crystal";
            case "drakthyst":
                return ChatColor.DARK_RED + "Drakthyst";
            default:
                return ChatColor.WHITE + type;
        }
    }
    
    /**
     * Get quality text prefix
     */
    private String getQualityPrefix(int quality) {
        if (quality >= 90) return ChatColor.GOLD + "★ Flawless ";
        if (quality >= 75) return ChatColor.LIGHT_PURPLE + "◈ Pristine ";
        if (quality >= 50) return ChatColor.AQUA + "◇ Quality ";
        if (quality >= 25) return ChatColor.GREEN + "○ Standard ";
        return ChatColor.GRAY + "▪ Crude ";
    }
    
    /**
     * Get color based on quality
     */
    private ChatColor getQualityColor(int quality) {
        if (quality >= 90) return ChatColor.GOLD;
        if (quality >= 75) return ChatColor.LIGHT_PURPLE;
        if (quality >= 50) return ChatColor.AQUA;
        if (quality >= 25) return ChatColor.GREEN;
        return ChatColor.GRAY;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subcommands = Arrays.asList("summon", "remove", "give");
            return filterStartingWith(subcommands, args[0]);
        } else if (args.length == 2) {
            // Second argument depends on first argument
            if (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("give")) {
                // For summon and give, suggest crystal types
                return filterStartingWith(crystalTypes, args[1]);
            } else if (args[0].equalsIgnoreCase("remove")) {
                // For remove, suggest range values
                return Arrays.asList("1", "3", "5", "10", "15");
            }
        } else if (args.length == 3) {
            // Third argument - quality for summon and give
            if (args[0].equalsIgnoreCase("summon") || args[0].equalsIgnoreCase("give")) {
                return Arrays.asList("25", "50", "75", "100");
            }
        } else if (args.length == 4) {
            // Fourth argument - size for summon only
            if (args[0].equalsIgnoreCase("summon")) {
                return Arrays.asList("0.5", "0.75", "1.0", "1.5", "2.0");
            }
        } else if (args.length == 5) {
            // Fifth argument - rotation for summon only
            if (args[0].equalsIgnoreCase("summon")) {
                return Arrays.asList("0", "45", "90", "135", "180", "225", "270", "315");
            }
        } else if (args.length == 6) {
            // Sixth argument - tiltX for summon only
            if (args[0].equalsIgnoreCase("summon")) {
                // Offer a wider range of tilt values
                return Arrays.asList("-180", "-90", "-45", "0", "45", "90", "180", "360");
            }
        } else if (args.length == 7) {
            // Seventh argument - tiltZ for summon only
            if (args[0].equalsIgnoreCase("summon")) {
                // Offer a wider range of tilt values
                return Arrays.asList("-180", "-90", "-45", "0", "45", "90", "180", "360");
            }
        }
        
        return completions;
    }
    
    /**
     * Filter a list of strings for those starting with a given prefix
     */
    private List<String> filterStartingWith(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}