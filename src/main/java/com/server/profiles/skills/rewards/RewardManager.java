package com.server.profiles.skills.rewards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.rewards.CurrencyReward;
import com.server.profiles.skills.rewards.rewards.ItemReward;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.rewards.rewards.UnlockReward;

/**
 * Manages and distributes rewards for skill levels
 */
public class RewardManager {
    private static RewardManager instance;
    
    private final Map<String, Map<Integer, List<SkillReward>>> skillRewards;
    
    private RewardManager() {
        this.skillRewards = new HashMap<>();
    }
    
    /**
     * Get the reward manager instance
     */
    public static RewardManager getInstance() {
        if (instance == null) {
            instance = new RewardManager();
        }
        return instance;
    }
    
    /**
     * Register a reward for a skill level
     * 
     * @param skill The skill to add the reward to
     * @param level The level at which the reward is given
     * @param reward The reward to give
     */
    public void registerReward(Skill skill, int level, SkillReward reward) {
        if (skill == null || level <= 0 || level > skill.getMaxLevel() || reward == null) return;
        
        String skillId = skill.getId();
        
        if (!skillRewards.containsKey(skillId)) {
            skillRewards.put(skillId, new HashMap<>());
        }
        
        Map<Integer, List<SkillReward>> levelRewards = skillRewards.get(skillId);
        
        if (!levelRewards.containsKey(level)) {
            levelRewards.put(level, new ArrayList<>());
        }
        
        levelRewards.get(level).add(reward);
    }
    
    /**
     * Get all rewards for a skill level
     * 
     * @param skill The skill to get rewards for
     * @param level The level to get rewards for
     * @return A list of rewards
     */
    public List<SkillReward> getRewards(Skill skill, int level) {
        if (skill == null || level <= 0 || level > skill.getMaxLevel()) {
            return new ArrayList<>();
        }
        
        String skillId = skill.getId();
        
        if (!skillRewards.containsKey(skillId)) {
            return new ArrayList<>();
        }
        
        Map<Integer, List<SkillReward>> levelRewards = skillRewards.get(skillId);
        
        if (!levelRewards.containsKey(level)) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(levelRewards.get(level));
    }
    
    /**
     * Grant all rewards for a skill level to a player
     * 
     * @param player The player to grant rewards to
     * @param skill The skill being leveled up
     * @param level The new level
     */
    public void grantRewards(Player player, Skill skill, int level) {
        List<SkillReward> rewards = getRewards(skill, level);
        
        for (SkillReward reward : rewards) {
            reward.grantTo(player);
        }
    }
    
    /**
     * Create a new stat reward
     * 
     * @param statName The name of the stat to boost
     * @param amount The amount to boost the stat by
     * @return A new StatReward
     */
    public StatReward createStatReward(String statName, double amount) {
        return new StatReward(statName, amount);
    }
    
    /**
     * Create a new item reward
     * 
     * @param itemId The ID of the item to give
     * @param amount The amount of the item to give
     * @return A new ItemReward
     */
    public ItemReward createItemReward(String itemId, int amount) {
        return new ItemReward(itemId, amount);
    }
    
    /**
     * Create a new currency reward
     * 
     * @param currencyType The type of currency to give
     * @param amount The amount of currency to give
     * @return A new CurrencyReward
     */
    public CurrencyReward createCurrencyReward(String currencyType, int amount) {
        return new CurrencyReward(currencyType, amount);
    }
    
    /**
     * Create a new unlock reward
     * 
     * @param unlockId The ID of the feature to unlock
     * @param description The description of the unlocked feature
     * @return A new UnlockReward
     */
    public UnlockReward createUnlockReward(String unlockId, String description) {
        return new UnlockReward(unlockId, description);
    }
}