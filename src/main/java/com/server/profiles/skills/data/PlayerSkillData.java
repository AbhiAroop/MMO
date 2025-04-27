package com.server.profiles.skills.data;

import java.util.HashMap;
import java.util.Map;

import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.core.SubskillType;

/**
 * Holds skill data for a player profile
 */
public class PlayerSkillData {
    private final Map<String, SkillLevel> skillLevels;
    
    public PlayerSkillData() {
        this.skillLevels = new HashMap<>();
    }
    
    /**
     * Get a skill's level data
     */
    public SkillLevel getSkillLevel(Skill skill) {
        return skillLevels.getOrDefault(skill.getId(), new SkillLevel(0, 0, 0)); // Changed to level 0
    }
    
    /**
     * Get a skill's level data by ID
     */
    public SkillLevel getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, new SkillLevel(0, 0, 0)); // Changed to level 0
    }
    
    /**
     * Get a skill's level data by type
     */
    public SkillLevel getSkillLevel(SkillType type) {
        return getSkillLevel(type.getId());
    }
    
    /**
     * Get a subskill's level data by type
     */
    public SkillLevel getSkillLevel(SubskillType type) {
        return getSkillLevel(type.getId());
    }
    
    /**
     * Set a skill's level data
     */
    public void setSkillLevel(Skill skill, SkillLevel level) {
        skillLevels.put(skill.getId(), level);
    }
    
    /**
     * Check if a skill is unlocked
     */
    public boolean isSkillUnlocked(Skill skill) {
        return getSkillLevel(skill).getLevel() > 0; // Still valid - skill is unlocked at level 1+
    }
    
    /**
     * Get all skill levels
     */
    public Map<String, SkillLevel> getAllSkillLevels() {
        return new HashMap<>(skillLevels);
    }
}