package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

/**
 * Command to manage player stats for testing and administrative purposes
 * Usage: /adminstats <player> <stat> <value|default>
 * Permission: mmo.admin.stats
 */
public class AdminStatsCommand implements TabExecutor {
    
    private final Main plugin;
    
    // Track original stat values for reset
    private Map<UUID, Map<String, Object>> originalStats = new HashMap<>();
    
    // Define the available stats with their types and whether they're default values
    private static final Map<String, StatDefinition> availableStats = new HashMap<>();
    
    static {
        // Combat Stats
        availableStats.put("health", new StatDefinition(Integer.class, "defaultHealth"));
        availableStats.put("armor", new StatDefinition(Integer.class, "defaultArmor"));
        availableStats.put("magicresist", new StatDefinition(Integer.class, "defaultMR"));
        availableStats.put("physicaldamage", new StatDefinition(Integer.class, "defaultPhysicalDamage"));
        availableStats.put("magicdamage", new StatDefinition(Integer.class, "defaultMagicDamage"));
        availableStats.put("mana", new StatDefinition(Integer.class, "defaultMana"));
        availableStats.put("speed", new StatDefinition(Double.class, "defaultSpeed"));
        availableStats.put("criticaldamage", new StatDefinition(Double.class, "defaultCritDmg"));
        availableStats.put("criticalchance", new StatDefinition(Double.class, "defaultCritChance"));
        availableStats.put("burstdamage", new StatDefinition(Double.class, "defaultBurstDmg"));
        availableStats.put("burstchance", new StatDefinition(Double.class, "defaultBurstChance"));
        availableStats.put("cooldownreduction", new StatDefinition(Integer.class, "defaultCDR"));
        availableStats.put("lifesteal", new StatDefinition(Double.class, "defaultLifeSteal"));
        availableStats.put("rangeddamage", new StatDefinition(Integer.class, "defaultRangedDamage"));
        availableStats.put("attackspeed", new StatDefinition(Double.class, "defaultAttackSpeed"));
        availableStats.put("omnivamp", new StatDefinition(Double.class, "defaultOmnivamp"));
        availableStats.put("healthregen", new StatDefinition(Double.class, "defaultHealthRegen"));
        
        // Fortune Stats
        availableStats.put("miningfortune", new StatDefinition(Double.class, "defaultMiningFortune"));
        availableStats.put("farmingfortune", new StatDefinition(Double.class, "defaultFarmingFortune"));
        availableStats.put("lootingfortune", new StatDefinition(Double.class, "defaultLootingFortune"));
        availableStats.put("fishingfortune", new StatDefinition(Double.class, "defaultFishingFortune"));
        
        // Resource Stats
        availableStats.put("manaregen", new StatDefinition(Integer.class, "defaultManaRegen"));
        availableStats.put("luck", new StatDefinition(Integer.class, "defaultLuck"));
        
        // Size and Range
        availableStats.put("attackrange", new StatDefinition(Double.class, "defaultAttackRange"));
        availableStats.put("size", new StatDefinition(Double.class, "defaultSize"));

        // Mining Stats
        availableStats.put("miningspeed", new StatDefinition(Double.class, "defaultMiningSpeed"));
        availableStats.put("buildrange", new StatDefinition(Double.class, "defaultBuildRange"));
    }
    
    public AdminStatsCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.stats")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Display help if not enough arguments
        if (args.length < 2) {
            displayHelp(sender);
            return true;
        }
        
        // Handle special commands first
        if (args[0].equalsIgnoreCase("reset") && args.length > 1) {
            return handleReset(sender, args[1]);
        }
        
        if (args[0].equalsIgnoreCase("list") && args.length > 1) {
            return handleList(sender, args[1]);
        }
        
        // Regular stat commands
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminstats <player> <stat> <value|default>");
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        // Get stat name (case-insensitive)
        String statName = args[1].toLowerCase();
        if (!availableStats.containsKey(statName)) {
            sender.sendMessage(ChatColor.RED + "Unknown stat: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Use /adminstats list to see available stats.");
            return true;
        }
        
        // Get active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have an active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get " + target.getName() + "'s profile.");
            return true;
        }
        
        PlayerStats stats = profile.getStats();
        
        // Check for "default" value
        if (args[2].equalsIgnoreCase("default")) {
            resetStatToDefault(target, stats, statName, sender);
            return true;
        }
        
        // Set the new value
        try {
            // Determine the type of the stat
            StatDefinition statDef = availableStats.get(statName);
            
            // Store original value if not already tracked
            if (!originalStats.containsKey(target.getUniqueId())) {
                originalStats.put(target.getUniqueId(), new HashMap<>());
            }
            
            Map<String, Object> playerOriginals = originalStats.get(target.getUniqueId());
            if (!playerOriginals.containsKey(statName)) {
                // Store original value for potential reset
                playerOriginals.put(statName, getDefaultStatValue(stats, statName));
            }
            
            // Parse and set the new value
            if (statDef.type == Integer.class) {
                int value = Integer.parseInt(args[2]);
                setDefaultStatValue(stats, statName, value);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s default " + 
                                  formatStatName(statName) + " to " + value);
            } 
            else if (statDef.type == Double.class) {
                double value = Double.parseDouble(args[2]);
                setDefaultStatValue(stats, statName, value);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s default " + 
                                  formatStatName(statName) + " to " + value);
            }
            
            // After modifying the default values, recalculate and apply current stats
            // This will use the new default values + equipment bonuses
            plugin.getStatScanManager().scanAndUpdatePlayerStats(target);
            
            return true;
        } 
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value format: " + args[2]);
            return true;
        }
        catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error setting stat: " + e.getMessage());
            plugin.debugLog(DebugSystem.STATS,"Error in AdminStatsCommand: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    private boolean handleReset(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Check if we have original values stored
        if (!originalStats.containsKey(target.getUniqueId()) || 
            originalStats.get(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No modified stats to reset for " + target.getName());
            return true;
        }
        
        // Get the profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have an active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get " + target.getName() + "'s profile.");
            return true;
        }
        
        PlayerStats stats = profile.getStats();
        
        // Reset all stored stats
        Map<String, Object> playerOriginals = originalStats.get(target.getUniqueId());
        for (Map.Entry<String, Object> entry : playerOriginals.entrySet()) {
            String statName = entry.getKey();
            Object originalValue = entry.getValue();
            
            if (originalValue instanceof Integer) {
                setDefaultStatValue(stats, statName, (Integer) originalValue);
            } 
            else if (originalValue instanceof Double) {
                setDefaultStatValue(stats, statName, (Double) originalValue);
            }
        }
        
        // Force a scan and update of stats
        plugin.getStatScanManager().scanAndUpdatePlayerStats(target);
        
        // Clear stored values
        originalStats.remove(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GREEN + "Reset all modified stats for " + target.getName() + " to original default values.");
        return true;
    }
    
    private boolean handleList(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Get the profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have an active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get " + target.getName() + "'s profile.");
            return true;
        }
        
        PlayerStats stats = profile.getStats();
        
        // Display all stats
        sender.sendMessage(ChatColor.GOLD + "===== " + target.getName() + "'s Stats =====");
        
        // Combat Stats
        sender.sendMessage(ChatColor.RED + "Combat Stats:");
        sender.sendMessage(ChatColor.GRAY + "Health: " + ChatColor.WHITE + stats.getHealth() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultHealth() + ")");
        sender.sendMessage(ChatColor.GRAY + "Armor: " + ChatColor.WHITE + stats.getArmor() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultArmor() + ")");
        sender.sendMessage(ChatColor.GRAY + "Magic Resist: " + ChatColor.WHITE + stats.getMagicResist() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultMagicResist() + ")");
        sender.sendMessage(ChatColor.GRAY + "Physical Damage: " + ChatColor.WHITE + stats.getPhysicalDamage() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultPhysicalDamage() + ")");
        sender.sendMessage(ChatColor.GRAY + "Magic Damage: " + ChatColor.WHITE + stats.getMagicDamage() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultMagicDamage() + ")");
        sender.sendMessage(ChatColor.GRAY + "Ranged Damage: " + ChatColor.WHITE + stats.getRangedDamage() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultRangedDamage() + ")");
        sender.sendMessage(ChatColor.GRAY + "Critical Chance: " + ChatColor.WHITE + 
                          String.format("%.1f", stats.getCriticalChance() * 100) + "%" + 
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.1f", stats.getDefaultCriticalChance() * 100) + "%)");
        sender.sendMessage(ChatColor.GRAY + "Critical Damage: " + ChatColor.WHITE + stats.getCriticalDamage() + "x" + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultCriticalDamage() + "x)");
        
        // Resource Stats
        sender.sendMessage(ChatColor.GREEN + "Resource Stats:");
        sender.sendMessage(ChatColor.GRAY + "Mana: " + ChatColor.WHITE + stats.getMana() + "/" + stats.getTotalMana() + 
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultMana() + ")");
        sender.sendMessage(ChatColor.GRAY + "Mana Regen: " + ChatColor.WHITE + stats.getManaRegen() + "/s" +
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultManaRegen() + "/s)");
        sender.sendMessage(ChatColor.GRAY + "Health Regen: " + ChatColor.WHITE + stats.getHealthRegen() + "/s" +
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultHealthRegen() + "/s)");
        sender.sendMessage(ChatColor.GRAY + "Movement Speed: " + ChatColor.WHITE + stats.getSpeed() + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + stats.getDefaultSpeed() + "x)");
        
        // Fortune Stats
        sender.sendMessage(ChatColor.YELLOW + "Fortune Stats:");
        sender.sendMessage(ChatColor.GRAY + "Mining Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getMiningFortune()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultMiningFortune()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Farming Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getFarmingFortune()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultFarmingFortune()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Looting Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getLootingFortune()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultLootingFortune()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Fishing Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getFishingFortune()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultFishingFortune()) + "x)");

        // Mining Stats
        sender.sendMessage(ChatColor.AQUA + "Other Stats:");
        sender.sendMessage(ChatColor.GRAY + "Mining Speed: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getMiningSpeed()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultMiningSpeed()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Size: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getSize()) + "x" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultSize()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Attack Range: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getAttackRange()) + "m" +
                          ChatColor.DARK_GRAY + " (Default: " + String.format("%.2f", stats.getDefaultAttackRange()) + "m)");
        
        return true;
    }
    
    private void resetStatToDefault(Player player, PlayerStats stats, String statName, CommandSender sender) {
        try {
            // Store current value if not already tracked
            if (!originalStats.containsKey(player.getUniqueId())) {
                originalStats.put(player.getUniqueId(), new HashMap<>());
            }
            
            Map<String, Object> playerOriginals = originalStats.get(player.getUniqueId());
            if (!playerOriginals.containsKey(statName)) {
                // Store original value for potential reset to original
                playerOriginals.put(statName, getDefaultStatValue(stats, statName));
            }
            
            // Reset to vanilla default value (not player's default)
            switch (statName) {
                case "health":
                    setDefaultStatValue(stats, statName, 100);
                    break;
                case "armor":
                    setDefaultStatValue(stats, statName, 0);
                    break;
                case "magicresist":
                    setDefaultStatValue(stats, statName, 0);
                    break;
                case "physicaldamage":
                    setDefaultStatValue(stats, statName, 5);
                    break;
                case "magicdamage":
                    setDefaultStatValue(stats, statName, 5);
                    break;
                case "mana":
                    setDefaultStatValue(stats, statName, 100);
                    break;
                case "speed":
                    setDefaultStatValue(stats, statName, 0.1);
                    break;
                case "criticaldamage":
                    setDefaultStatValue(stats, statName, 1.5);
                    break;
                case "criticalchance":
                    setDefaultStatValue(stats, statName, 0.0);
                    break;
                case "burstdamage":
                    setDefaultStatValue(stats, statName, 2.0);
                    break;
                case "burstchance":
                    setDefaultStatValue(stats, statName, 0.01);
                    break;
                case "cooldownreduction":
                    setDefaultStatValue(stats, statName, 0);
                    break;
                case "lifesteal":
                    setDefaultStatValue(stats, statName, 0.0);
                    break;
                case "rangeddamage":
                    setDefaultStatValue(stats, statName, 5);
                    break;
                case "attackspeed":
                    setDefaultStatValue(stats, statName, 0.5);
                    break;
                case "omnivamp":
                    setDefaultStatValue(stats, statName, 0.0);
                    break;
                case "healthregen":
                    setDefaultStatValue(stats, statName, 0.3);
                    break;
                case "miningfortune":
                    setDefaultStatValue(stats, statName, 1.0);
                    break;
                case "farmingfortune":
                    setDefaultStatValue(stats, statName, 1.0);
                    break;
                case "lootingfortune":
                    setDefaultStatValue(stats, statName, 1.0);
                    break;
                case "fishingfortune":
                    setDefaultStatValue(stats, statName, 1.0);
                    break;
                case "manaregen":
                    setDefaultStatValue(stats, statName, 1);
                    break;
                case "luck":
                    setDefaultStatValue(stats, statName, 0);
                    break;
                case "attackrange":
                    setDefaultStatValue(stats, statName, 3.0);
                    break;
                case "size":
                    setDefaultStatValue(stats, statName, 1.0);
                    break;
                case "miningspeed":
                    setDefaultStatValue(stats, statName, 0.5);
                    break;
                case "buildrange":
                    setDefaultStatValue(stats, statName, 5.0); 
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown stat: " + statName);
                    return;
            }
            
            // Force a scan and update
            plugin.getStatScanManager().scanAndUpdatePlayerStats(player);
            
            sender.sendMessage(ChatColor.GREEN + "Reset " + player.getName() + "'s default " + 
                              formatStatName(statName) + " to vanilla default value.");
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error resetting stat: " + e.getMessage());
            plugin.debugLog(DebugSystem.STATS,"Error in AdminStatsCommand: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the default value for a stat
     */
    private Object getDefaultStatValue(PlayerStats stats, String statName) {
        StatDefinition statDef = availableStats.get(statName);
        
        // Using reflection to get the default field value
        try {
            java.lang.reflect.Field field = PlayerStats.class.getDeclaredField(statDef.fieldName);
            field.setAccessible(true);
            return field.get(stats);
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error accessing default stat field: " + e.getMessage());
            
            // Fallback to regular getters if reflection fails
            switch (statName) {
                case "health": return stats.getDefaultHealth();
                case "armor": return stats.getDefaultArmor();
                case "magicresist": return stats.getDefaultMagicResist();
                case "physicaldamage": return stats.getDefaultPhysicalDamage();
                case "magicdamage": return stats.getDefaultMagicDamage();
                case "mana": return stats.getDefaultMana();
                case "speed": return stats.getDefaultSpeed();
                case "criticaldamage": return stats.getDefaultCriticalDamage();
                case "criticalchance": return stats.getDefaultCriticalChance();
                case "burstdamage": return stats.getDefaultBurstDamage();
                case "burstchance": return stats.getDefaultBurstChance();
                case "cooldownreduction": return stats.getDefaultCooldownReduction();
                case "lifesteal": return stats.getDefaultLifeSteal();
                case "rangeddamage": return stats.getDefaultRangedDamage();
                case "attackspeed": return stats.getDefaultAttackSpeed();
                case "omnivamp": return stats.getDefaultOmnivamp();
                case "healthregen": return stats.getDefaultHealthRegen();
                case "miningfortune": return stats.getDefaultMiningFortune();
                case "farmingfortune": return stats.getDefaultFarmingFortune();
                case "lootingfortune": return stats.getDefaultLootingFortune();
                case "fishingfortune": return stats.getDefaultFishingFortune();
                case "manaregen": return stats.getDefaultManaRegen();
                case "luck": return stats.getDefaultLuck();
                case "attackrange": return stats.getDefaultAttackRange();
                case "size": return stats.getDefaultSize();
                case "miningspeed": return stats.getDefaultMiningSpeed();
                case "buildrange": return stats.getDefaultBuildRange();
                default: return null;
            }
        }
    }
    
    /**
     * Set the default value for a stat
     */
    private void setDefaultStatValue(PlayerStats stats, String statName, Object value) {
        StatDefinition statDef = availableStats.get(statName);
        
        // Using reflection to set the default field value
        try {
            java.lang.reflect.Field field = PlayerStats.class.getDeclaredField(statDef.fieldName);
            field.setAccessible(true);
            field.set(stats, value);
            
            // We need to also update the current value to match the new default
            // This ensures the stat is immediately updated
            switch (statName) {
                case "health":
                    stats.setHealth((Integer)value);
                    stats.setCurrentHealth(Math.min(stats.getCurrentHealth(), (Integer)value));
                    break;
                case "armor":
                    stats.setArmor((Integer)value);
                    break;
                case "magicresist":
                    stats.setMagicResist((Integer)value);
                    break;
                case "physicaldamage":
                    stats.setPhysicalDamage((Integer)value);
                    break;
                case "magicdamage":
                    stats.setMagicDamage((Integer)value);
                    break;
                case "mana":
                    stats.setTotalMana((Integer)value);
                    stats.setMana(Math.min(stats.getMana(), (Integer)value));
                    break;
                case "rangeddamage":
                    stats.setRangedDamage((Integer)value);
                    break;
                case "speed":
                    stats.setSpeed((Double)value);
                    break;
                case "criticaldamage":
                    stats.setCriticalDamage((Double)value);
                    break;
                case "criticalchance":
                    stats.setCriticalChance((Double)value);
                    break;
                case "burstdamage":
                    stats.setBurstDamage((Double)value);
                    break;
                case "burstchance":
                    stats.setBurstChance((Double)value);
                    break;
                case "cooldownreduction":
                    stats.setCooldownReduction((Integer)value);
                    break;
                case "lifesteal":
                    stats.setLifeSteal((Double)value);
                    break;
                case "attackspeed":
                    stats.setAttackSpeed((Double)value);
                    break;
                case "omnivamp":
                    stats.setOmnivamp((Double)value);
                    break;
                case "healthregen":
                    stats.setHealthRegen((Double)value);
                    break;
                case "miningfortune":
                    stats.setMiningFortune((Double)value);
                    break;
                case "farmingfortune":
                    stats.setFarmingFortune((Double)value);
                    break;
                case "lootingfortune":
                    stats.setLootingFortune((Double)value);
                    break;
                case "fishingfortune":
                    stats.setFishingFortune((Double)value);
                    break;
                case "manaregen":
                    stats.setManaRegen((Integer)value);
                    break;
                case "luck":
                    stats.setLuck((Integer)value);
                    break;
                case "attackrange":
                    stats.setAttackRange((Double)value);
                    break;
                case "size":
                    stats.setSize((Double)value);
                    break;
                case "miningspeed":
                    stats.setMiningSpeed((Double)value);
                    break;
                case "buildrange":
                    stats.setBuildRange((Double)value);
                    break;
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error setting default stat field: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String formatStatName(String statName) {
        StringBuilder formatted = new StringBuilder();
        boolean nextUpper = true;
        
        for (char c : statName.toCharArray()) {
            if (nextUpper) {
                formatted.append(Character.toUpperCase(c));
                nextUpper = false;
            } else if (Character.isUpperCase(c)) {
                formatted.append(' ').append(c);
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== AdminStats Command Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/adminstats <player> <stat> <value> " + 
                          ChatColor.WHITE + "- Set a player's default stat");
        sender.sendMessage(ChatColor.YELLOW + "/adminstats <player> <stat> default " + 
                          ChatColor.WHITE + "- Reset stat to vanilla default value");
        sender.sendMessage(ChatColor.YELLOW + "/adminstats reset <player> " + 
                          ChatColor.WHITE + "- Reset all modified stats to original values");
        sender.sendMessage(ChatColor.YELLOW + "/adminstats list <player> " + 
                          ChatColor.WHITE + "- Show all stats for a player");
        
        // Display available stats
        sender.sendMessage(ChatColor.GOLD + "Available Stats:");
        
        List<String> statNames = new ArrayList<>(availableStats.keySet());
        statNames.sort(String::compareTo);
        
        StringBuilder statsText = new StringBuilder(ChatColor.GRAY.toString());
        for (int i = 0; i < statNames.size(); i++) {
            statsText.append(formatStatName(statNames.get(i)));
            if (i < statNames.size() - 1) {
                statsText.append(ChatColor.GRAY).append(", ");
            }
            
            // Add line breaks for readability
            if (i > 0 && i % 5 == 0) {
                statsText.append("\n").append(ChatColor.GRAY);
            }
        }
        
        sender.sendMessage(statsText.toString());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument is player name or special command
            List<String> specialCommands = Arrays.asList("reset", "list");
            for (String special : specialCommands) {
                if (special.startsWith(args[0].toLowerCase())) {
                    completions.add(special);
                }
            }
            
            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("list")) {
                // Second argument is player name for special commands
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else {
                // Second argument is stat name
                for (String stat : availableStats.keySet()) {
                    if (stat.startsWith(args[1].toLowerCase())) {
                        completions.add(stat);
                    }
                }
            }
        }
        else if (args.length == 3) {
            // Third argument could be "default"
            if ("default".startsWith(args[2].toLowerCase())) {
                completions.add("default");
            }
            
            // For boolean stats, suggest true/false
            if (args[1].equalsIgnoreCase("criticalchance") || 
                args[1].equalsIgnoreCase("burstchance")) {
                if ("0".startsWith(args[2])) completions.add("0");
                if ("0.5".startsWith(args[2])) completions.add("0.5");
                if ("1".startsWith(args[2])) completions.add("1");
            }
            
            // For common values in some stats
            if (args[1].equalsIgnoreCase("miningfortune") ||
                args[1].equalsIgnoreCase("farmingfortune") ||
                args[1].equalsIgnoreCase("fishingfortune") ||
                args[1].equalsIgnoreCase("lootingfortune")) {
                List<String> suggestions = Arrays.asList("1", "1.5", "2", "2.5", "3", "4", "5");
                for (String suggestion : suggestions) {
                    if (suggestion.startsWith(args[2])) {
                        completions.add(suggestion);
                    }
                }
            } else {
                List<String> suggestions = Arrays.asList("1", "50", "100", "150", "200", "250", "500", "1000");
                for (String suggestion : suggestions) {
                    if (suggestion.startsWith(args[2])) {
                        completions.add(suggestion);
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Inner class to store stat definition information
     */
    private static class StatDefinition {
        final Class<?> type;
        final String fieldName;
        
        StatDefinition(Class<?> type, String fieldName) {
            this.type = type;
            this.fieldName = fieldName;
        }
    }
}