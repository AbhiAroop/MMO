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
    
    // Define the available stats with their types
    private static final Map<String, Class<?>> availableStats = new HashMap<>();
    
    static {
        // Combat Stats
        availableStats.put("health", Integer.class);
        availableStats.put("armor", Integer.class);
        availableStats.put("magicresist", Integer.class);
        availableStats.put("physicaldamage", Integer.class);
        availableStats.put("magicdamage", Integer.class);
        availableStats.put("mana", Integer.class);
        availableStats.put("speed", Double.class);
        availableStats.put("criticaldamage", Double.class);
        availableStats.put("criticalchance", Double.class);
        availableStats.put("burstdamage", Double.class);
        availableStats.put("burstchance", Double.class);
        availableStats.put("cooldownreduction", Integer.class);
        availableStats.put("lifesteal", Double.class);
        availableStats.put("rangeddamage", Integer.class);
        availableStats.put("attackspeed", Double.class);
        availableStats.put("omnivamp", Double.class);
        availableStats.put("healthregen", Double.class);
        
        // Fortune Stats
        availableStats.put("miningfortune", Double.class);
        availableStats.put("farmingfortune", Double.class);
        availableStats.put("lootingfortune", Double.class);
        availableStats.put("fishingfortune", Double.class);
        
        // Resource Stats
        availableStats.put("manaregen", Integer.class);
        availableStats.put("luck", Integer.class);
        
        // Size and Range
        availableStats.put("attackrange", Double.class);
        availableStats.put("size", Double.class);

        //Mining Stats
        availableStats.put("miningspeed", Double.class);
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
            // Determine the type of the stat and parse accordingly
            Class<?> statType = availableStats.get(statName);
            
            // Store original value if not already tracked
            if (!originalStats.containsKey(target.getUniqueId())) {
                originalStats.put(target.getUniqueId(), new HashMap<>());
            }
            
            Map<String, Object> playerOriginals = originalStats.get(target.getUniqueId());
            if (!playerOriginals.containsKey(statName)) {
                // Store original value for potential reset
                playerOriginals.put(statName, getStatValue(stats, statName));
            }
            
            // Parse and set the new value
            if (statType == Integer.class) {
                int value = Integer.parseInt(args[2]);
                setStatValue(stats, statName, value);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                                  formatStatName(statName) + " to " + value);
            } 
            else if (statType == Double.class) {
                double value = Double.parseDouble(args[2]);
                setStatValue(stats, statName, value);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                                  formatStatName(statName) + " to " + value);
            }
            
            // Apply the changes to the player
            stats.applyToPlayer(target);
            
            return true;
        } 
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value format: " + args[2]);
            return true;
        }
        catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error setting stat: " + e.getMessage());
            plugin.getLogger().warning("Error in AdminStatsCommand: " + e.getMessage());
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
                setStatValue(stats, statName, (Integer) originalValue);
            } 
            else if (originalValue instanceof Double) {
                setStatValue(stats, statName, (Double) originalValue);
            }
        }
        
        // Apply changes and clear stored values
        stats.applyToPlayer(target);
        originalStats.remove(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GREEN + "Reset all modified stats for " + target.getName() + " to original values.");
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
        sender.sendMessage(ChatColor.GRAY + "Health: " + ChatColor.WHITE + stats.getHealth());
        sender.sendMessage(ChatColor.GRAY + "Armor: " + ChatColor.WHITE + stats.getArmor());
        sender.sendMessage(ChatColor.GRAY + "Magic Resist: " + ChatColor.WHITE + stats.getMagicResist());
        sender.sendMessage(ChatColor.GRAY + "Physical Damage: " + ChatColor.WHITE + stats.getPhysicalDamage());
        sender.sendMessage(ChatColor.GRAY + "Magic Damage: " + ChatColor.WHITE + stats.getMagicDamage());
        sender.sendMessage(ChatColor.GRAY + "Critical Chance: " + ChatColor.WHITE + 
                          String.format("%.1f", stats.getCriticalChance() * 100) + "%");
        sender.sendMessage(ChatColor.GRAY + "Critical Damage: " + ChatColor.WHITE + stats.getCriticalDamage() + "x");
        
        // Resource Stats
        sender.sendMessage(ChatColor.GREEN + "Resource Stats:");
        sender.sendMessage(ChatColor.GRAY + "Mana: " + ChatColor.WHITE + stats.getMana() + "/" + stats.getTotalMana());
        sender.sendMessage(ChatColor.GRAY + "Mana Regen: " + ChatColor.WHITE + stats.getManaRegen() + "/s");
        sender.sendMessage(ChatColor.GRAY + "Movement Speed: " + ChatColor.WHITE + stats.getSpeed() + "x");
        
        // Fortune Stats
        sender.sendMessage(ChatColor.YELLOW + "Fortune Stats:");
        sender.sendMessage(ChatColor.GRAY + "Mining Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getMiningFortune()) + "x");
        sender.sendMessage(ChatColor.GRAY + "Farming Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getFarmingFortune()) + "x");
        sender.sendMessage(ChatColor.GRAY + "Looting Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getLootingFortune()) + "x");
        sender.sendMessage(ChatColor.GRAY + "Fishing Fortune: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getFishingFortune()) + "x");

        //Mining Stats
        sender.sendMessage(ChatColor.GRAY + "Mining Speed: " + ChatColor.WHITE + 
                          String.format("%.2f", stats.getMiningSpeed()) + "x");
        
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
                playerOriginals.put(statName, getStatValue(stats, statName));
            }
            
            // Reset to default value
            switch (statName) {
                case "health":
                    stats.setHealth(stats.getDefaultHealth());
                    break;
                case "armor":
                    stats.setArmor(stats.getDefaultArmor());
                    break;
                case "magicresist":
                    stats.setMagicResist(stats.getDefaultMagicResist());
                    break;
                case "physicaldamage":
                    stats.setPhysicalDamage(stats.getDefaultPhysicalDamage());
                    break;
                case "magicdamage":
                    stats.setMagicDamage(stats.getDefaultMagicDamage());
                    break;
                case "mana":
                    stats.setMana(stats.getDefaultMana());
                    stats.setTotalMana(stats.getDefaultMana());
                    break;
                case "speed":
                    stats.setSpeed(stats.getDefaultSpeed());
                    break;
                case "criticaldamage":
                    stats.setCriticalDamage(stats.getDefaultCriticalDamage());
                    break;
                case "criticalchance":
                    stats.setCriticalChance(stats.getDefaultCriticalChance());
                    break;
                case "burstdamage":
                    stats.setBurstDamage(stats.getDefaultBurstDamage());
                    break;
                case "burstchance":
                    stats.setBurstChance(stats.getDefaultBurstChance());
                    break;
                case "cooldownreduction":
                    stats.setCooldownReduction(stats.getDefaultCooldownReduction());
                    break;
                case "lifesteal":
                    stats.setLifeSteal(stats.getDefaultLifeSteal());
                    break;
                case "rangeddamage":
                    stats.setRangedDamage(stats.getDefaultRangedDamage());
                    break;
                case "attackspeed":
                    stats.setAttackSpeed(stats.getDefaultAttackSpeed());
                    break;
                case "omnivamp":
                    stats.setOmnivamp(stats.getDefaultOmnivamp());
                    break;
                case "healthregen":
                    stats.setHealthRegen(stats.getDefaultHealthRegen());
                    break;
                case "miningfortune":
                    stats.setMiningFortune(stats.getDefaultMiningFortune());
                    break;
                case "farmingfortune":
                    stats.setFarmingFortune(stats.getDefaultFarmingFortune());
                    break;
                case "lootingfortune":
                    stats.setLootingFortune(stats.getDefaultLootingFortune());
                    break;
                case "fishingfortune":
                    stats.setFishingFortune(stats.getDefaultFishingFortune());
                    break;
                case "manaregen":
                    stats.setManaRegen(stats.getDefaultManaRegen());
                    break;
                case "luck":
                    stats.setLuck(stats.getDefaultLuck());
                    break;
                case "attackrange":
                    stats.setAttackRange(stats.getDefaultAttackRange());
                    break;
                case "size":
                    stats.setSize(stats.getDefaultSize());
                    break;
                case "miningspeed":
                    stats.setMiningSpeed(stats.getDefaultMiningSpeed());
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown stat: " + statName);
                    return;
            }
            
            // Apply changes
            stats.applyToPlayer(player);
            
            sender.sendMessage(ChatColor.GREEN + "Reset " + player.getName() + "'s " + 
                              formatStatName(statName) + " to default value.");
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error resetting stat: " + e.getMessage());
            plugin.getLogger().warning("Error in AdminStatsCommand: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Object getStatValue(PlayerStats stats, String statName) {
        switch (statName) {
            case "health": return stats.getHealth();
            case "armor": return stats.getArmor();
            case "magicresist": return stats.getMagicResist();
            case "physicaldamage": return stats.getPhysicalDamage();
            case "magicdamage": return stats.getMagicDamage();
            case "mana": return stats.getMana();
            case "speed": return stats.getSpeed();
            case "criticaldamage": return stats.getCriticalDamage();
            case "criticalchance": return stats.getCriticalChance();
            case "burstdamage": return stats.getBurstDamage();
            case "burstchance": return stats.getBurstChance();
            case "cooldownreduction": return stats.getCooldownReduction();
            case "lifesteal": return stats.getLifeSteal();
            case "rangeddamage": return stats.getRangedDamage();
            case "attackspeed": return stats.getAttackSpeed();
            case "omnivamp": return stats.getOmnivamp();
            case "healthregen": return stats.getHealthRegen();
            case "miningfortune": return stats.getMiningFortune();
            case "farmingfortune": return stats.getFarmingFortune();
            case "lootingfortune": return stats.getLootingFortune();
            case "fishingfortune": return stats.getFishingFortune();
            case "manaregen": return stats.getManaRegen();
            case "luck": return stats.getLuck();
            case "attackrange": return stats.getAttackRange();
            case "size": return stats.getSize();
            case "miningspeed": return stats.getMiningSpeed();
            default: return null;
        }
    }
    
    private void setStatValue(PlayerStats stats, String statName, Object value) {
        switch (statName) {
            case "health":
                stats.setHealth((Integer) value);
                break;
            case "armor":
                stats.setArmor((Integer) value);
                break;
            case "magicresist":
                stats.setMagicResist((Integer) value);
                break;
            case "physicaldamage":
                stats.setPhysicalDamage((Integer) value);
                break;
            case "magicdamage":
                stats.setMagicDamage((Integer) value);
                break;
            case "mana":
                stats.setMana((Integer) value);
                stats.setTotalMana((Integer) value);
                break;
            case "speed":
                stats.setSpeed((Double) value);
                break;
            case "criticaldamage":
                stats.setCriticalDamage((Double) value);
                break;
            case "criticalchance":
                stats.setCriticalChance((Double) value);
                break;
            case "burstdamage":
                stats.setBurstDamage((Double) value);
                break;
            case "burstchance":
                stats.setBurstChance((Double) value);
                break;
            case "cooldownreduction":
                stats.setCooldownReduction((Integer) value);
                break;
            case "lifesteal":
                stats.setLifeSteal((Double) value);
                break;
            case "rangeddamage":
                stats.setRangedDamage((Integer) value);
                break;
            case "attackspeed":
                stats.setAttackSpeed((Double) value);
                break;
            case "omnivamp":
                stats.setOmnivamp((Double) value);
                break;
            case "healthregen":
                stats.setHealthRegen((Double) value);
                break;
            case "miningfortune":
                stats.setMiningFortune((Double) value);
                break;
            case "farmingfortune":
                stats.setFarmingFortune((Double) value);
                break;
            case "lootingfortune":
                stats.setLootingFortune((Double) value);
                break;
            case "fishingfortune":
                stats.setFishingFortune((Double) value);
                break;
            case "manaregen":
                stats.setManaRegen((Integer) value);
                break;
            case "luck":
                stats.setLuck((Integer) value);
                break;
            case "attackrange":
                stats.setAttackRange((Double) value);
                break;
            case "size":
                stats.setSize((Double) value);
                break;
            case "miningspeed":
                stats.setMiningSpeed((Double) value);
                break;
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
                          ChatColor.WHITE + "- Set a player's stat");
        sender.sendMessage(ChatColor.YELLOW + "/adminstats <player> <stat> default " + 
                          ChatColor.WHITE + "- Reset stat to default value");
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
                statsText.append(", ");
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
            if (args[1].equalsIgnoreCase("miningfortune")) {
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
}