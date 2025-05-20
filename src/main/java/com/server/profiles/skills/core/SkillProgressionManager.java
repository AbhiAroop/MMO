package com.server.profiles.skills.core;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.PlayerSkillData;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.events.SkillLevelUpEvent;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.stats.PlayerStats;

/**
 * Manages progression of skills and subskills, including handling parent-child relationships
 */
public class SkillProgressionManager {
    private static SkillProgressionManager instance;
    private final Main plugin;
    
    private SkillProgressionManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the skill progression manager
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new SkillProgressionManager(plugin);
        }
    }
    
    /**
     * Get the skill progression manager instance
     */
    public static SkillProgressionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkillProgressionManager not initialized");
        }
        return instance;
    }
    
    /**
     * Add experience to a skill for a player
     * 
     * @param player The player to add XP to
     * @param skill The skill to add XP to
     * @param amount The amount of XP to add
     * @return true if leveled up, false otherwise
     */
    public boolean addExperience(Player player, Skill skill, double amount) {
        if (player == null || skill == null || amount <= 0) return false;
        
        // Get player profile
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return false;
        
        // Get skill data
        PlayerSkillData skillData = SkillRegistry.getInstance().getPlayerSkillData(profile);
        SkillLevel currentLevel = skillData.getSkillLevel(skill);
        
        // Stop if already max level
        if (currentLevel.getLevel() >= skill.getMaxLevel()) {
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.getLogger().info(player.getName() + " is already at max level for " + skill.getDisplayName());
            }
            return false;
        }
        
        // Add XP to skill
        boolean leveledUp = skill.addExperience(player, amount);
        
        // If leveled up, check for milestones and parent skill updates
        if (leveledUp) {
    SkillLevel newLevel = skillData.getSkillLevel(skill);
    
    // Apply mining fortune bonus for OreExtractionSubskill levels
        if (skill instanceof OreExtractionSubskill) {
            OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
            PlayerStats stats = profile.getStats();
            
            // Calculate the change in mining fortune from the level up
            double previousBonus = oreSkill.getMiningFortuneBonus(newLevel.getLevel() - 1);
            double newBonus = oreSkill.getMiningFortuneBonus(newLevel.getLevel());
            double fortuneIncrease = newBonus - previousBonus;
            
            // Apply the increase to player's default mining fortune stats
            stats.increaseDefaultMiningFortune(fortuneIncrease);
            
            // Apply the changes to the player
            stats.applyToPlayer(player);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.getLogger().info("Added " + String.format("%.2f", fortuneIncrease) + 
                    " to default mining fortune for " + player.getName() + " from " + 
                    skill.getDisplayName() + " level up (now level " + newLevel.getLevel() + ")");
            }
        }
        
        // Fire levelup event and give rewards
        SkillLevelUpEvent levelUpEvent = new SkillLevelUpEvent(player, skill, newLevel.getLevel());
        plugin.getServer().getPluginManager().callEvent(levelUpEvent);
        
        // Notify the player
        notifyLevelUp(player, skill, newLevel.getLevel());
    }
        
        return leveledUp;
    }
    
    /**
     * Calculate XP bonus for reaching a milestone level in a subskill
     * 
     * @param milestoneLevel The milestone level reached
     * @return The XP bonus for the parent skill
     */
    private double calculateMilestoneBonus(int milestoneLevel) {
        // Simple formula: milestone level * 100
        return milestoneLevel * 100;
    }
    
    /**
     * Notify a player of a skill level up
     * 
     * @param player The player to notify
     * @param skill The skill that leveled up
     * @param newLevel The new level
     */
    private void notifyLevelUp(Player player, Skill skill, int newLevel) {
        // Ensure we're passing the correct level in the notification
        // Get the most recent level data from the player's profile
        PlayerProfile profile = null;
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                SkillLevel skillLevel = profile.getSkillData().getSkillLevel(skill);
                // Use the level from the player's profile to ensure accuracy
                newLevel = skillLevel.getLevel();
            }
        }
        
        // Send message
        player.sendMessage(ChatColor.GREEN + "✦ " + ChatColor.GOLD + "SKILL LEVEL UP" + 
                        ChatColor.GREEN + " ✦ " + ChatColor.YELLOW + skill.getDisplayName() + 
                        ChatColor.GREEN + " is now level " + ChatColor.YELLOW + newLevel);
        
        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        // Show title
        player.sendTitle(
            ChatColor.GOLD + "LEVEL UP!",
            ChatColor.YELLOW + skill.getDisplayName() + ChatColor.WHITE + " reached level " + ChatColor.YELLOW + newLevel,
            10, 70, 20
        );
    }
}