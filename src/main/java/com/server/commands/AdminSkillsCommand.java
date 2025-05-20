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
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.data.PlayerSkillData;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.events.SkillLevelUpEvent;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.stats.PlayerStats;

/**
 * Command to manage player skill levels and XP for testing and administrative purposes
 * Usage: /adminskills <player> <skill> <level|xp> <value>
 * Permission: mmo.admin.skills
 */
public class AdminSkillsCommand implements TabExecutor {
    
    private final Main plugin;
    
    // Track original skill levels for reset
    private Map<UUID, Map<String, SkillState>> originalSkills = new HashMap<>();
    
    public AdminSkillsCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.skills")) {
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
        
        // Regular skill commands need more arguments
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminskills <player> <skill> <level|xp> <value>");
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        // Get skill ID (case-insensitive)
        String skillArg = args[1].toLowerCase();
        Skill skill = getSkillFromInput(skillArg);
        
        if (skill == null) {
            sender.sendMessage(ChatColor.RED + "Unknown skill: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Use /adminskills list " + target.getName() + " to see available skills.");
            return true;
        }
        
        // Determine if we're setting level or XP
        String action = args[2].toLowerCase();
        if (!action.equals("level") && !action.equals("xp")) {
            sender.sendMessage(ChatColor.RED + "Invalid action: " + args[2] + ". Use 'level' or 'xp'.");
            return true;
        }
        
        // Validate value
        int value;
        try {
            value = Integer.parseInt(args[3]);
            if (value < 0) {
                sender.sendMessage(ChatColor.RED + "Value must be non-negative.");
                return true;
            }
            if (action.equals("level") && value > skill.getMaxLevel()) {
                sender.sendMessage(ChatColor.RED + "Maximum level for " + skill.getDisplayName() + " is " + skill.getMaxLevel() + ".");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid value: " + args[3] + ". Please provide a number.");
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
        
        // Get the skill data - CRITICAL FIX: Make sure we're getting the PlayerSkillData from the profile's getSkillData()
        PlayerSkillData skillData = profile.getSkillData();
        if (skillData == null) {
            // Get skill data from the registry if not available directly
            skillData = SkillRegistry.getInstance().getPlayerSkillData(profile);
            if (skillData == null) {
                sender.sendMessage(ChatColor.RED + "Failed to get skill data for " + target.getName());
                return true;
            }
        }
        
        // Store original value if not already tracked
        if (!originalSkills.containsKey(target.getUniqueId())) {
            originalSkills.put(target.getUniqueId(), new HashMap<>());
        }
        
        Map<String, SkillState> playerOriginals = originalSkills.get(target.getUniqueId());
        if (!playerOriginals.containsKey(skill.getId())) {
            // Store original value for potential reset
            SkillLevel currentLevel = skillData.getSkillLevel(skill);
            playerOriginals.put(skill.getId(), new SkillState(
                currentLevel.getLevel(),
                currentLevel.getCurrentXp(),
                currentLevel.getTotalXp()
            ));
        }
        
        // Perform the action - set level or xp
        if (action.equals("level")) {
            // Get current level before changes
            SkillLevel currentLevel = skillData.getSkillLevel(skill);
            int oldLevel = currentLevel.getLevel();
            
            // FIX: Check if we're trying to set it to the same level, and if so, just skip the command
            if (oldLevel == value) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s " + 
                        ChatColor.GOLD + skill.getDisplayName() + ChatColor.YELLOW + 
                        " is already at level " + value + ".");
                return true;
            }
            
            double totalXp = calculateTotalXpForLevel(skill, value);
            double currentXp = 0;
            
            if (value < skill.getMaxLevel()) {
                // Set current XP to 0 if not max level
                currentXp = 0;
            }
            
            // Create a new SkillLevel object with the updated values
            SkillLevel newLevel = new SkillLevel(value, currentXp, totalXp);
            
            // CRITICAL FIX: Directly update the skill level in player's profile data
            skillData.setSkillLevel(skill, newLevel);
            
            // Make sure we save the skill data back to the profile
            SkillRegistry.getInstance().getPlayerSkillData(profile);
            
            // Apply accumulated rewards for all levels between old and new level
            applyAccumulatedRewards(target, skill, oldLevel, value, profile);
            
            // Notify admin and player
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                    ChatColor.GOLD + skill.getDisplayName() + ChatColor.GREEN + " level to " + value);
            
            target.sendMessage(ChatColor.YELLOW + "Your " + ChatColor.GOLD + skill.getDisplayName() + 
                    ChatColor.YELLOW + " level has been set to " + ChatColor.WHITE + value);
        } else {
            // Set skill XP
            // Get current level before changes
            SkillLevel currentLevel = skillData.getSkillLevel(skill);
            int oldLevel = currentLevel.getLevel();
            
            // Calculate which level this XP would correspond to
            int level = calculateLevelFromXp(skill, value);
            if (level > skill.getMaxLevel()) {
                level = skill.getMaxLevel();
            }
            
            // FIX: Check if we're trying to set XP to a value that results in the same level and current XP
            if (oldLevel == level && Math.abs(currentLevel.getTotalXp() - value) < 0.01) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s " + 
                        ChatColor.GOLD + skill.getDisplayName() + ChatColor.YELLOW + 
                        " already has " + value + " XP (Level " + level + ").");
                return true;
            }
            
            // Calculate how much XP is needed for the current level
            double xpForCurrentLevel = level > 1 ? calculateTotalXpForLevel(skill, level - 1) : 0;
            
            // Calculate how much XP is needed for the next level
            double xpForNextLevel = level < skill.getMaxLevel() ? skill.getXpForLevel(level) : Double.MAX_VALUE;
            
            // Calculate current XP towards next level
            double currentXp = value - xpForCurrentLevel;
            
            // Create a new SkillLevel object
            SkillLevel newLevel = new SkillLevel(level, currentXp, value);
            
            // CRITICAL FIX: Directly update the skill level in player's profile data
            skillData.setSkillLevel(skill, newLevel);
            
            // Make sure we save the skill data back to the profile
            SkillRegistry.getInstance().getPlayerSkillData(profile);
            
            // Apply accumulated rewards for all levels between old and new level
            applyAccumulatedRewards(target, skill, oldLevel, level, profile);
            
            // Notify admin and player
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                    ChatColor.GOLD + skill.getDisplayName() + ChatColor.GREEN + " XP to " + value +
                    " (Level " + level + ")");
            
            target.sendMessage(ChatColor.YELLOW + "Your " + ChatColor.GOLD + skill.getDisplayName() + 
                    ChatColor.YELLOW + " XP has been set to " + ChatColor.WHITE + value +
                    ChatColor.YELLOW + " (Level " + level + ")");
        }
        
        // Force update of UI elements to show new skill level
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(target);
        }
        
        // CRITICAL FIX: Save the player's profile after making changes
        // We already have the profile object, so we can just save it directly
        profile.saveProfile(target);
        
        return true;
    }

    /**
     * Apply all rewards and benefits for levels between oldLevel and newLevel
     */
    private void applyAccumulatedRewards(Player player, Skill skill, int oldLevel, int newLevel, PlayerProfile profile) {
        // First, reset any stat-specific bonuses to avoid stacking
        if (skill instanceof OreExtractionSubskill) {
            PlayerStats stats = profile.getStats();
            stats.setMiningFortune(stats.getDefaultMiningFortune());
            stats.setMiningSpeed(stats.getDefaultMiningSpeed());
            
            // Apply stats to player
            stats.applyToPlayer(player);
        }
        
        // Apply all rewards from level 1 to newLevel
        // This ensures we don't miss any rewards and all appropriate bonuses are applied
        for (int level = 1; level <= newLevel; level++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(level);
            if (rewards != null && !rewards.isEmpty()) {
                for (SkillReward reward : rewards) {
                    reward.grantTo(player);
                    
                    if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                        plugin.debugLog(DebugSystem.SKILLS,"Applied reward for " + player.getName() + " from " + 
                            skill.getDisplayName() + " level " + level + ": " + reward.getClass().getSimpleName());
                    }
                }
            }
        }
        
        // CRITICAL FIX: Fire level up events for each level achieved
        // This ensures skill tokens are properly awarded
        if (newLevel > oldLevel) {
            // When leveling up, fire events for each level achieved
            for (int level = oldLevel + 1; level <= newLevel; level++) {
                SkillLevelUpEvent event = new SkillLevelUpEvent(player, skill, level);
                plugin.getServer().getPluginManager().callEvent(event);
                
                if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                    plugin.debugLog(DebugSystem.SKILLS,"Fired level up event for " + player.getName() + 
                        " reaching " + skill.getDisplayName() + " level " + level);
                }
            }
        } else if (newLevel < oldLevel) {
            // When leveling down to a specific level, fire just one event for the final level
            SkillLevelUpEvent event = new SkillLevelUpEvent(player, skill, newLevel);
            plugin.getServer().getPluginManager().callEvent(event);
        } else {
            // FIX: When setting to the same level, don't fire any level up events
            // This prevents duplicate tokens when setting a skill to the level it already has
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Skill level unchanged for " + player.getName() + "'s " + 
                    skill.getDisplayName() + " - staying at level " + newLevel);
            }
        }
        
        // Make sure we update the player's UI
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(player);
        }
        
        // Apply skill-specific stat bonuses
        if (skill instanceof OreExtractionSubskill) {
            OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
            PlayerStats stats = profile.getStats();
            
            // Add bonus from current level
            double fortuneBonus = oreSkill.getMiningFortuneBonus(newLevel);
            stats.setMiningFortune(stats.getDefaultMiningFortune() + fortuneBonus);
            
            // Apply mining speed bonuses too
            double speedMultiplier = oreSkill.getMiningSpeedMultiplier(newLevel);
            if (speedMultiplier > 1.0) {
                stats.setMiningSpeed(stats.getDefaultMiningSpeed() * speedMultiplier);
            }
            
            // Apply the changes to the player
            stats.applyToPlayer(player);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Applied mining fortune +" + String.format("%.2f", fortuneBonus) + 
                    " to " + player.getName() + " from " + skill.getDisplayName() + " level " + newLevel);
            }
        }
        
        // Notify the player of the admin command's effect
        player.sendMessage(ChatColor.GREEN + "An admin has adjusted your " + 
                        ChatColor.GOLD + skill.getDisplayName() + 
                        ChatColor.GREEN + " skill to level " + newLevel + ".");
    }
    
    private boolean handleReset(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Check if we have original values stored
        if (!originalSkills.containsKey(target.getUniqueId()) || 
            originalSkills.get(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No modified skills to reset for " + target.getName());
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
        
        // Get the skill data directly from the profile
        PlayerSkillData skillData = profile.getSkillData();
        if (skillData == null) {
            // Get skill data from the registry if not available directly
            skillData = SkillRegistry.getInstance().getPlayerSkillData(profile);
            if (skillData == null) {
                sender.sendMessage(ChatColor.RED + "Failed to get skill data for " + target.getName());
                return true;
            }
        }
        
        // Get the skill tree data for handling tokens
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get skill tree data for " + target.getName());
            return true;
        }
        
        // Reset all stored skills
        Map<String, SkillState> playerOriginals = originalSkills.get(target.getUniqueId());
        int resetCount = 0;
        
        for (Map.Entry<String, SkillState> entry : playerOriginals.entrySet()) {
            String skillId = entry.getKey();
            SkillState originalState = entry.getValue();
            
            // Find the skill using the extracted skillId
            Skill skill = getSkillFromInput(skillId);
            
            if (skill != null) {
                // Get current level before reset
                SkillLevel currentLevel = skillData.getSkillLevel(skill);
                int oldLevel = currentLevel.getLevel();
                
                // Create a fresh SkillLevel object with original values
                SkillLevel originalLevel = new SkillLevel(
                    originalState.level,
                    originalState.currentXp,
                    originalState.totalXp
                );
                
                // CRITICAL FIX: Directly update the skill level in player's profile data
                skillData.setSkillLevel(skill, originalLevel);
                
                // CRITICAL FIX: Reset tokens that were awarded for skill levels gained
                if (oldLevel > originalState.level) {
                    // Reset tokens to what they should be at the original level
                    int tokensToRemove = calculateTokensGained(originalState.level, oldLevel);
                    int currentTokens = treeData.getTokenCount(skill.getId());
                    
                    if (tokensToRemove > 0) {
                        // Set tokens to current minus what was gained from the admin command
                        int newTokenCount = Math.max(0, currentTokens - tokensToRemove);
                        treeData.setTokenCount(skill.getId(), newTokenCount);
                        
                        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                            plugin.debugLog(DebugSystem.SKILLS,"Reset " + tokensToRemove + " tokens for " + 
                                target.getName() + "'s " + skill.getDisplayName() + " skill");
                        }
                    }
                }
                
                // Apply accumulated rewards for all levels from 0 to the restored level
                applyAccumulatedRewards(target, skill, 0, originalState.level, profile);
                
                resetCount++;
            }
        }
        
        // CRITICAL FIX: Save the updated skill data back to the profile
        SkillRegistry.getInstance().getPlayerSkillData(profile);
        
        // CRITICAL FIX: Save the player's profile after making changes
        // We already have the profile object, so we can just save it directly
        profile.saveProfile(target);
        
        // Clear stored values
        originalSkills.remove(target.getUniqueId());
        
        // Force update of UI elements to show reset skill levels
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(target);
        }
        
        sender.sendMessage(ChatColor.GREEN + "Reset " + resetCount + " modified skills for " + 
                target.getName() + " to original values.");
        target.sendMessage(ChatColor.YELLOW + "Your skill levels and XP have been reset to their original values.");
        
        return true;
    }

    /**
     * Calculate the total number of tokens gained between two levels
     */
    private int calculateTokensGained(int oldLevel, int newLevel) {
        int totalTokens = 0;
        
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            totalTokens += calculateTokensForLevel(level);
        }
        
        return totalTokens;
    }

    /**
     * Calculate tokens for a specific level
     * This should match the formula in SkillLevelupListener
     */
    private int calculateTokensForLevel(int level) {
        // Special milestones get more tokens
        if (level % 25 == 0) {
            return 5; // Every 25 levels: 25, 50, 75, 100
        } else if (level % 10 == 0) {
            return 3; // Every 10 levels: 10, 20, 30, etc. (except those already covered)
        } else if (level % 5 == 0) {
            return 2; // Every 5 levels: 5, 15, 35, etc. (except those already covered)
        }
        
        // Regular levels
        return 0;
    }
    
    private boolean handleList(CommandSender sender, String playerName) {
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
        
        // Get the skill data
        PlayerSkillData skillData = SkillRegistry.getInstance().getPlayerSkillData(profile);
        if (skillData == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get skill data for " + target.getName());
            return true;
        }
        
        // Display all skills
        sender.sendMessage(ChatColor.GOLD + "===== " + target.getName() + "'s Skills =====");
        
        // Display main skills first
        sender.sendMessage(ChatColor.YELLOW + "Main Skills:");
        
        // List levels for main skills
        for (SkillType type : SkillType.values()) {
            Skill skill = SkillRegistry.getInstance().getSkill(type);
            if (skill != null) {
                SkillLevel level = skillData.getSkillLevel(skill);
                double progress = 0;
                
                if (level.getLevel() < skill.getMaxLevel()) {
                    double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
                    progress = level.getProgressPercentage(xpForNextLevel) * 100;
                }
                
                sender.sendMessage(ChatColor.GRAY + skill.getDisplayName() + ": " + 
                                  ChatColor.WHITE + "Level " + level.getLevel() + 
                                  ChatColor.GRAY + " (" + String.format("%.1f", level.getTotalXp()) + " total XP, " +
                                  String.format("%.1f", progress) + "% to next level)");
            }
        }
        
        // Display subskills
        sender.sendMessage(ChatColor.YELLOW + "Subskills:");
        
        // List levels for subskills (by parent skill for organization)
        for (SkillType parentType : SkillType.values()) {
            Skill parentSkill = SkillRegistry.getInstance().getSkill(parentType);
            if (parentSkill != null && !parentSkill.getSubskills().isEmpty()) {
                // Add parent header
                sender.sendMessage(ChatColor.GREEN + parentSkill.getDisplayName() + " Subskills:");
                
                // List each subskill
                for (Skill subskill : parentSkill.getSubskills()) {
                    SkillLevel level = skillData.getSkillLevel(subskill);
                    double progress = 0;
                    
                    if (level.getLevel() < subskill.getMaxLevel()) {
                        double xpForNextLevel = subskill.getXpForLevel(level.getLevel() + 1);
                        progress = level.getProgressPercentage(xpForNextLevel) * 100;
                    }
                    
                    sender.sendMessage(ChatColor.GRAY + "  " + subskill.getDisplayName() + ": " + 
                                      ChatColor.WHITE + "Level " + level.getLevel() + 
                                      ChatColor.GRAY + " (" + String.format("%.1f", level.getTotalXp()) + " total XP, " +
                                      String.format("%.1f", progress) + "% to next level)");
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculate the total XP needed to reach a specific level (inclusive)
     */
    private double calculateTotalXpForLevel(Skill skill, int level) {
        if (level <= 0) {
            return 0;
        }
        
        double totalXp = 0;
        for (int i = 1; i <= level; i++) {
            totalXp += skill.getXpForLevel(i);
        }
        
        return totalXp;
    }
    
    /**
     * Calculate the level that corresponds to a given amount of XP
     */
    private int calculateLevelFromXp(Skill skill, double totalXp) {
        if (totalXp <= 0) {
            return 0;
        }
        
        double accumulatedXp = 0;
        int level = 0;
        
        while (accumulatedXp <= totalXp && level < skill.getMaxLevel()) {
            level++;
            double xpForNextLevel = skill.getXpForLevel(level);
            
            if (accumulatedXp + xpForNextLevel > totalXp) {
                break;
            }
            
            accumulatedXp += xpForNextLevel;
        }
        
        return level;
    }
    
    /**
     * Get a skill from a flexible input format
     */
    private Skill getSkillFromInput(String input) {
        // First check for exact main skill ID match
        for (SkillType type : SkillType.values()) {
            if (type.getId().equalsIgnoreCase(input)) {
                return SkillRegistry.getInstance().getSkill(type);
            }
        }
        
        // Check for main skill name match (more user-friendly)
        for (SkillType type : SkillType.values()) {
            Skill skill = SkillRegistry.getInstance().getSkill(type);
            if (skill != null && skill.getDisplayName().replace(" ", "").equalsIgnoreCase(input)) {
                return skill;
            }
        }
        
        // Check subskills
        for (SkillType parentType : SkillType.values()) {
            Skill parentSkill = SkillRegistry.getInstance().getSkill(parentType);
            if (parentSkill != null && !parentSkill.getSubskills().isEmpty()) {
                for (Skill subskill : parentSkill.getSubskills()) {
                    // Check ID
                    if (subskill.getId().equalsIgnoreCase(input)) {
                        return subskill;
                    }
                    
                    // Check name
                    if (subskill.getDisplayName().replace(" ", "").equalsIgnoreCase(input)) {
                        return subskill;
                    }
                }
            }
        }
        
        // No match found
        return null;
    }
    
    /**
     * Format a skill ID to be more readable
     */
    private String formatSkillName(String skillId) {
        StringBuilder formatted = new StringBuilder();
        boolean nextUpper = true;
        
        for (char c : skillId.toCharArray()) {
            if (c == '_' || c == '.') {
                formatted.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                formatted.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== AdminSkills Command Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/adminskills <player> <skill> level <value> " + 
                          ChatColor.WHITE + "- Set a player's skill level");
        sender.sendMessage(ChatColor.YELLOW + "/adminskills <player> <skill> xp <value> " + 
                          ChatColor.WHITE + "- Set a player's skill XP");
        sender.sendMessage(ChatColor.YELLOW + "/adminskills reset <player> " + 
                          ChatColor.WHITE + "- Reset all modified skills to original values");
        sender.sendMessage(ChatColor.YELLOW + "/adminskills list <player> " + 
                          ChatColor.WHITE + "- Show all skills and levels for a player");
        
        // Display available skills
        sender.sendMessage(ChatColor.GOLD + "Available Main Skills:");
        StringBuilder mainSkills = new StringBuilder();
        for (SkillType type : SkillType.values()) {
            Skill skill = SkillRegistry.getInstance().getSkill(type);
            if (skill != null) {
                mainSkills.append(ChatColor.GRAY).append(skill.getDisplayName())
                          .append(ChatColor.DARK_GRAY).append(" (").append(skill.getId()).append(")")
                          .append(ChatColor.WHITE).append(", ");
            }
        }
        // Remove trailing comma and space
        if (mainSkills.length() > 2) {
            mainSkills.setLength(mainSkills.length() - 2);
        }
        sender.sendMessage(mainSkills.toString());
        
        // Display subskills by parent
        sender.sendMessage(ChatColor.GOLD + "Available Subskills (by parent):");
        for (SkillType parentType : SkillType.values()) {
            Skill parentSkill = SkillRegistry.getInstance().getSkill(parentType);
            if (parentSkill != null && !parentSkill.getSubskills().isEmpty()) {
                StringBuilder subSkills = new StringBuilder();
                subSkills.append(ChatColor.GREEN).append(parentSkill.getDisplayName()).append(": ");
                
                for (Skill subskill : parentSkill.getSubskills()) {
                    subSkills.append(ChatColor.GRAY).append(subskill.getDisplayName())
                            .append(ChatColor.DARK_GRAY).append(" (").append(subskill.getId()).append(")")
                            .append(ChatColor.WHITE).append(", ");
                }
                
                // Remove trailing comma and space
                if (subSkills.length() > 2) {
                    subSkills.setLength(subSkills.length() - 2);
                }
                
                sender.sendMessage(subSkills.toString());
            }
        }
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
                // Second argument is skill ID or name
                
                // Add main skills
                for (SkillType type : SkillType.values()) {
                    Skill skill = SkillRegistry.getInstance().getSkill(type);
                    if (skill != null) {
                        // Check if ID starts with input
                        if (skill.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(skill.getId());
                        }
                        // Check if name starts with input (for more intuitive use)
                        else if (skill.getDisplayName().toLowerCase().replace(" ", "").startsWith(args[1].toLowerCase())) {
                            completions.add(skill.getId());
                        }
                    }
                }
                
                // Add subskills
                for (SkillType parentType : SkillType.values()) {
                    Skill parentSkill = SkillRegistry.getInstance().getSkill(parentType);
                    if (parentSkill != null) {
                        for (Skill subskill : parentSkill.getSubskills()) {
                            // Check if ID starts with input
                            if (subskill.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(subskill.getId());
                            }
                            // Check if name starts with input
                            else if (subskill.getDisplayName().toLowerCase().replace(" ", "").startsWith(args[1].toLowerCase())) {
                                completions.add(subskill.getId());
                            }
                        }
                    }
                }
            }
        }
        else if (args.length == 3) {
            // Third argument is "level" or "xp"
            if (!args[0].equalsIgnoreCase("reset") && !args[0].equalsIgnoreCase("list")) {
                if ("level".startsWith(args[2].toLowerCase())) {
                    completions.add("level");
                }
                if ("xp".startsWith(args[2].toLowerCase())) {
                    completions.add("xp");
                }
            }
        }
        else if (args.length == 4) {
            // Fourth argument is the value
            if (!args[0].equalsIgnoreCase("reset") && !args[0].equalsIgnoreCase("list")) {
                // For level, suggest common values
                if (args[2].equalsIgnoreCase("level")) {
                    List<String> suggestions = Arrays.asList("0", "1", "5", "10", "25", "50", "75", "100");
                    for (String suggestion : suggestions) {
                        if (suggestion.startsWith(args[3])) {
                            completions.add(suggestion);
                        }
                    }
                }
                // For XP, suggest common values
                else if (args[2].equalsIgnoreCase("xp")) {
                    List<String> suggestions = Arrays.asList("0", "100", "500", "1000", "5000", "10000", "50000", "100000");
                    for (String suggestion : suggestions) {
                        if (suggestion.startsWith(args[3])) {
                            completions.add(suggestion);
                        }
                    }
                }
            }
        }
        
        return completions;
    }

    private void applySkillLevelBenefits(Player player, Skill skill, int level, PlayerProfile profile) {
        // Apply mining fortune bonuses if this is OreExtractionSubskill
        if (skill instanceof OreExtractionSubskill) {
            OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
            PlayerStats stats = profile.getStats();
            
            // Reset mining fortune to default first to avoid stacking
            stats.setMiningFortune(stats.getDefaultMiningFortune());
            
            // Add bonus from current level
            double fortuneBonus = oreSkill.getMiningFortuneBonus(level);
            stats.setMiningFortune(stats.getMiningFortune() + fortuneBonus);
            
            // Apply mining speed bonuses too
            double speedMultiplier = oreSkill.getMiningSpeedMultiplier(level);
            if (speedMultiplier > 1.0) {
                stats.setMiningSpeed(stats.getDefaultMiningSpeed() * speedMultiplier);
            }
            
            // Apply the changes to the player
            stats.applyToPlayer(player);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Applied mining fortune +" + String.format("%.2f", fortuneBonus) + 
                    " to " + player.getName() + " from " + skill.getDisplayName() + " level " + level);
            }
        }
        
        // Trigger skill rewards for the new level
        List<SkillReward> rewards = skill.getRewardsForLevel(level);
        if (rewards != null && !rewards.isEmpty()) {
            for (SkillReward reward : rewards) {
                // Fixed: Actually call the grantTo method
                reward.grantTo(player);
                
                if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                    plugin.debugLog(DebugSystem.SKILLS,"Applied reward for " + player.getName() + " from " + 
                        skill.getDisplayName() + " level " + level + ": " + reward.getClass().getSimpleName());
                }
            }
        }
        
        // Update related UI components
        if (plugin.getScoreboardManager() != null) {
            // Update scoreboard for player
            plugin.getScoreboardManager().startTracking(player);
        }
        
        // Notify the player of the admin command's effect
        player.sendMessage(ChatColor.GREEN + "An admin has adjusted your " + 
                        ChatColor.GOLD + skill.getDisplayName() + 
                        ChatColor.GREEN + " skill to level " + level + ".");
        
        // Fire a level up event to ensure any listeners can respond appropriately
        SkillLevelUpEvent event = new SkillLevelUpEvent(player, skill, level);
        plugin.getServer().getPluginManager().callEvent(event);
    }
    
    /**
     * Class to store skill state for resetting
     */
    private static class SkillState {
        final int level;
        final double currentXp;
        final double totalXp;
        
        public SkillState(int level, double currentXp, double totalXp) {
            this.level = level;
            this.currentXp = currentXp;
            this.totalXp = totalXp;
        }
    }
}