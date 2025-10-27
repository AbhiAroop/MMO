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
        
        // Play XP gain sound (different sound per skill type)
        playXpSound(player, skill);
        
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
    
    /**
     * Play a sound effect when XP is gained
     * Uses note block chime sound with slight pitch variations per skill
     * 
     * @param player The player to play sound for
     * @param skill The skill that gained XP
     */
    private void playXpSound(Player player, Skill skill) {
        float pitch;
        
        // Determine pitch based on skill ID (all use same chime sound, just different pitches)
        String skillId = skill.getId();
        
        // Main skills - slight pitch variations for distinction
        if (skillId.equals("mining")) {
            pitch = 0.9f;
        } else if (skillId.equals("excavating")) {
            pitch = 1.0f;
        } else if (skillId.equals("fishing")) {
            pitch = 1.1f;
        } else if (skillId.equals("farming")) {
            pitch = 1.2f;
        } else if (skillId.equals("combat")) {
            pitch = 1.3f;
        } 
        // Subskills - higher pitches for distinction
        else if (skillId.contains("ore_extraction") || skillId.contains("gem_finding")) {
            pitch = 1.4f;
        } else if (skillId.contains("treasure_hunter")) {
            pitch = 1.5f;
        } else if (skillId.contains("harvesting") || skillId.contains("cultivating") || skillId.contains("botany")) {
            pitch = 1.6f;
        } else {
            // Default pitch for unknown skills
            pitch = 1.0f;
        }
        
        // Play the note block chime sound (pleasant "ding" for XP notifications)
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, pitch);
    }
}