package com.server.profiles.skills.core;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;

/**
 * Base interface for all skills in the system
 */
public interface Skill {
    /**
     * Get the unique identifier for this skill
     */
    String getId();
    
    /**
     * Get the display name of this skill
     */
    String getDisplayName();
    
    /**
     * Get the description of this skill
     */
    String getDescription();
    
    /**
     * Get the maximum level this skill can reach
     */
    int getMaxLevel();
    
    /**
     * Check if this is a main skill (parent) or a subskill
     */
    boolean isMainSkill();
    
    /**
     * Get the parent skill if this is a subskill
     */
    Skill getParentSkill();
    
    /**
     * Get all subskills if this is a main skill
     */
    List<Skill> getSubskills();
    
    /**
     * Get the rewards for reaching a specific level
     */
    List<SkillReward> getRewardsForLevel(int level);
    
    /**
     * Get the skill level data for a player
     */
    SkillLevel getSkillLevel(Player player);
    
    /**
     * Add experience to this skill for a player
     * @return true if leveled up, false otherwise
     */
    boolean addExperience(Player player, double amount);
    
    /**
     * Check if a milestone level has been reached, which contributes
     * to the parent skill's level
     */
    boolean hasMilestoneAt(int level);
    
    /**
     * Get all milestone levels for this skill
     */
    List<Integer> getMilestones();
    
    /**
     * Get XP requirements for each level
     */
    Map<Integer, Double> getXpRequirements();
    
    /**
     * Calculate the XP needed to reach a specific level
     */
    double getXpForLevel(int level);
}