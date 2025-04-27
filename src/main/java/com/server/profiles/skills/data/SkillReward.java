package com.server.profiles.skills.data;

import org.bukkit.entity.Player;

import com.server.profiles.skills.rewards.SkillRewardType;

/**
 * Represents a reward given for reaching a skill level
 */
public abstract class SkillReward {
    protected final SkillRewardType type;
    protected final String description;
    
    public SkillReward(SkillRewardType type, String description) {
        this.type = type;
        this.description = description;
    }
    
    /**
     * Get the reward type
     */
    public SkillRewardType getType() {
        return type;
    }
    
    /**
     * Get the reward description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Grant the reward to a player
     */
    public abstract void grantTo(Player player);
}