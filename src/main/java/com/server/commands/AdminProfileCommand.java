package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Admin command to manage player profile levels and XP
 * Usage: /adminprofile <player> <level|xp|add> <value>
 * Permission: mmo.admin.profile
 */
public class AdminProfileCommand implements TabExecutor {
    
    private final Main plugin;
    
    public AdminProfileCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.profile")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Display help if not enough arguments
        if (args.length < 3) {
            displayHelp(sender);
            return true;
        }
        
        // Handle special commands
        if (args[0].equalsIgnoreCase("list") && args.length >= 2) {
            return handleList(sender, args[1]);
        }
        
        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            return handleInfo(sender, args[1]);
        }
        
        // Regular profile commands need exactly 3 arguments
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminprofile <player> <level|xp|add> <value>");
            return true;
        }
        
        return handleProfileLevel(sender, args[0], args[1], args[2]);
    }
    
    /**
     * Handle profile level commands
     * Usage: /adminprofile <player> <level|xp|add> <value>
     */
    private boolean handleProfileLevel(CommandSender sender, String playerName, String action, String valueStr) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Get player's active profile
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
        
        // Parse the value
        double value;
        try {
            value = Double.parseDouble(valueStr);
            if (value < 0) {
                sender.sendMessage(ChatColor.RED + "Value must be non-negative.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value: " + valueStr + ". Please provide a number.");
            return true;
        }
        
        // Handle different actions
        switch (action.toLowerCase()) {
            case "level":
                return handleSetProfileLevel(sender, target, profile, (int) value);
            case "xp":
                return handleSetProfileXp(sender, target, profile, value);
            case "add":
                return handleAddProfileXp(sender, target, profile, value);
            default:
                sender.sendMessage(ChatColor.RED + "Invalid action: " + action + ". Use 'level', 'xp', or 'add'.");
                return true;
        }
    }
    
    /**
     * Set a player's profile level directly
     */
    private boolean handleSetProfileLevel(CommandSender sender, Player target, PlayerProfile profile, int level) {
        if (level < 1 || level > PlayerProfile.getMaxProfileLevel()) {
            sender.sendMessage(ChatColor.RED + "Profile level must be between 1 and " + PlayerProfile.getMaxProfileLevel() + ".");
            return true;
        }
        
        int oldLevel = profile.getProfileLevel();
        
        // Check if already at that level
        if (oldLevel == level) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s profile is already at level " + level + ".");
            return true;
        }
        
        // Set the level (reset XP to 0 for clean state)
        boolean success = profile.setProfileLevel(level, true);
        
        if (success) {
            // Notify admin and player
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s profile level to " + level + 
                              " (was " + oldLevel + ")");
            
            target.sendMessage(ChatColor.YELLOW + "An admin has set your profile level to " + 
                              ChatColor.WHITE + level + ChatColor.YELLOW + "!");
            
            // Play sound and show title
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            target.sendTitle(
                ChatColor.GOLD + "PROFILE LEVEL SET!",
                ChatColor.YELLOW + "Level " + level,
                10, 70, 20
            );
            
            // Update scoreboard
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().startTracking(target);
            }
            
            // Log the action
            plugin.getLogger().info("Admin " + sender.getName() + " set " + target.getName() + 
                                  "'s profile level to " + level);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set profile level.");
        }
        
        return true;
    }
    
    /**
     * Set a player's profile XP directly
     */
    private boolean handleSetProfileXp(CommandSender sender, Player target, PlayerProfile profile, double xp) {
        double oldTotalXp = profile.getProfileTotalXp();
        int oldLevel = profile.getProfileLevel();
        
        // Calculate what level this XP would correspond to
        int targetLevel = calculateProfileLevelFromXp(xp);
        
        // Set to level 1 first, then add the XP
        profile.setProfileLevel(1, true);
        boolean leveledUp = profile.addProfileExperienceWithNotification(target, xp, "Admin Command");
        
        int newLevel = profile.getProfileLevel();
        
        // Notify admin and player
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s profile XP to " + 
                          String.format("%.0f", xp) + " (Level " + newLevel + ")");
        
        target.sendMessage(ChatColor.YELLOW + "An admin has set your profile XP to " + 
                          ChatColor.WHITE + String.format("%.0f", xp) + 
                          ChatColor.YELLOW + " (Level " + newLevel + ")");
        
        // Update scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(target);
        }
        
        // Log the action
        plugin.getLogger().info("Admin " + sender.getName() + " set " + target.getName() + 
                              "'s profile XP to " + xp + " (level " + newLevel + ")");
        
        return true;
    }
    
    /**
     * Add profile XP to a player
     */
    private boolean handleAddProfileXp(CommandSender sender, Player target, PlayerProfile profile, double xp) {
        double oldTotalXp = profile.getProfileTotalXp();
        int oldLevel = profile.getProfileLevel();
        
        // Add the XP
        boolean leveledUp = profile.addProfileExperienceWithNotification(target, xp, "Admin Command");
        
        int newLevel = profile.getProfileLevel();
        double newTotalXp = profile.getProfileTotalXp();
        
        // Notify admin
        if (leveledUp) {
            sender.sendMessage(ChatColor.GREEN + "Added " + String.format("%.0f", xp) + 
                              " profile XP to " + target.getName() + ". Leveled up from " + 
                              oldLevel + " to " + newLevel + "!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Added " + String.format("%.0f", xp) + 
                              " profile XP to " + target.getName() + " (Level " + newLevel + ")");
        }
        
        // Notify player
        target.sendMessage(ChatColor.YELLOW + "You gained " + ChatColor.WHITE + 
                          String.format("%.0f", xp) + ChatColor.YELLOW + " profile XP from an admin!");
        
        // Update scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(target);
        }
        
        // Log the action
        plugin.getLogger().info("Admin " + sender.getName() + " added " + xp + 
                              " profile XP to " + target.getName() + 
                              " (level " + oldLevel + " -> " + newLevel + ")");
        
        return true;
    }
    
    /**
     * Handle the 'list' command to show profile info for a player
     */
    private boolean handleList(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Get all profiles for the player
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(target.getUniqueId());
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GOLD + "===== Profile Information for " + target.getName() + " =====");
        
        for (int i = 0; i < profiles.length; i++) {
            PlayerProfile profile = profiles[i];
            if (profile != null) {
                String activeIndicator = (activeSlot != null && activeSlot == i) ? ChatColor.GREEN + " [ACTIVE]" : "";
                sender.sendMessage(ChatColor.YELLOW + "Slot " + (i + 1) + ": " + ChatColor.WHITE + 
                                 profile.getName() + activeIndicator);
                sender.sendMessage("  " + ChatColor.GRAY + "Profile Level: " + ChatColor.WHITE + 
                                 profile.getFormattedProfileProgress());
                sender.sendMessage("  " + ChatColor.GRAY + "Total XP: " + ChatColor.WHITE + 
                                 String.format("%.0f", profile.getProfileTotalXp()));
            } else {
                sender.sendMessage(ChatColor.GRAY + "Slot " + (i + 1) + ": Empty");
            }
        }
        
        return true;
    }
    
    /**
     * Handle the 'info' command to show detailed profile info
     */
    private boolean handleInfo(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have an active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get " + target.getName() + "'s active profile.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "===== Profile Details for " + target.getName() + " =====");
        sender.sendMessage(ChatColor.YELLOW + "Profile Name: " + ChatColor.WHITE + profile.getName());
        sender.sendMessage(ChatColor.YELLOW + "Slot: " + ChatColor.WHITE + (activeSlot + 1));
        sender.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + profile.getProfileLevel() + 
                          "/" + PlayerProfile.getMaxProfileLevel());
        sender.sendMessage(ChatColor.YELLOW + "Current XP: " + ChatColor.WHITE + 
                          String.format("%.0f", profile.getProfileCurrentXp()) + "/" + 
                          String.format("%.0f", profile.getXpForNextProfileLevel()));
        sender.sendMessage(ChatColor.YELLOW + "Total XP: " + ChatColor.WHITE + 
                          String.format("%.0f", profile.getProfileTotalXp()));
        sender.sendMessage(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + 
                          String.format("%.1f%%", profile.getProfileLevelProgress() * 100));
        
        if (profile.isMaxProfileLevel()) {
            sender.sendMessage(ChatColor.GOLD + "✦ This profile is at maximum level! ✦");
        }
        
        return true;
    }
    
    /**
     * Calculate what profile level corresponds to a given amount of total XP
     */
    private int calculateProfileLevelFromXp(double totalXp) {
        if (totalXp <= 0) {
            return 1;
        }
        
        int level = 1;
        double accumulatedXp = 0;
        
        while (level < PlayerProfile.getMaxProfileLevel()) {
            double xpForNextLevel = PlayerProfile.getXpForProfileLevel(level + 1);
            
            if (accumulatedXp + xpForNextLevel > totalXp) {
                break;
            }
            
            accumulatedXp += xpForNextLevel;
            level++;
        }
        
        return level;
    }
    
    /**
     * Display help information
     */
    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== AdminProfile Command Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/adminprofile <player> level <value> " + 
                          ChatColor.WHITE + "- Set a player's profile level");
        sender.sendMessage(ChatColor.YELLOW + "/adminprofile <player> xp <value> " + 
                          ChatColor.WHITE + "- Set a player's profile XP");
        sender.sendMessage(ChatColor.YELLOW + "/adminprofile <player> add <value> " + 
                          ChatColor.WHITE + "- Add profile XP to a player");
        sender.sendMessage(ChatColor.YELLOW + "/adminprofile list <player> " + 
                          ChatColor.WHITE + "- Show all profiles for a player");
        sender.sendMessage(ChatColor.YELLOW + "/adminprofile info <player> " + 
                          ChatColor.WHITE + "- Show detailed info for active profile");
        
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.WHITE + "  /adminprofile Steve level 50");
        sender.sendMessage(ChatColor.WHITE + "  /adminprofile Steve xp 25000");
        sender.sendMessage(ChatColor.WHITE + "  /adminprofile Steve add 5000");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mmo.admin.profile")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument: player name or special commands
            List<String> specialCommands = Arrays.asList("list", "info");
            for (String special : specialCommands) {
                if (special.startsWith(args[0].toLowerCase())) {
                    completions.add(special);
                }
            }
            
            // Add online player names
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                             .map(Player::getName)
                             .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                             .collect(Collectors.toList()));
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("info")) {
                // Second argument is player name for special commands
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                                 .map(Player::getName)
                                 .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                 .collect(Collectors.toList()));
            } else {
                // Second argument is action type for regular commands
                List<String> actions = Arrays.asList("level", "xp", "add");
                completions.addAll(actions.stream()
                                 .filter(action -> action.startsWith(args[1].toLowerCase()))
                                 .collect(Collectors.toList()));
            }
        }
        else if (args.length == 3) {
            // Third argument is the value
            if (args[1].equalsIgnoreCase("level")) {
                List<String> suggestions = Arrays.asList("1", "5", "10", "25", "50", "75", "100");
                completions.addAll(suggestions.stream()
                                 .filter(suggestion -> suggestion.startsWith(args[2]))
                                 .collect(Collectors.toList()));
            } else if (args[1].equalsIgnoreCase("xp") || args[1].equalsIgnoreCase("add")) {
                List<String> suggestions = Arrays.asList("100", "1000", "5000", "10000", "50000", "100000");
                completions.addAll(suggestions.stream()
                                 .filter(suggestion -> suggestion.startsWith(args[2]))
                                 .collect(Collectors.toList()));
            }
        }
        
        return completions;
    }
}