package com.server.profiles.skills.skills.mining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.CurrencyReward;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;

/**
 * The Mining skill - focused on mining ores and stone to collect valuable resources
 */
public class MiningSkill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(10, 25, 50);
    
    // Cache for rewards by level
    private final Map<Integer, List<SkillReward>> rewardsByLevel = new HashMap<>();
    
    static {
        // Set up XP requirements for each level
        for (int level = 1; level <= 50; level++) {
            // Formula: Base 200 XP, increasing by 20% each level
            XP_REQUIREMENTS.put(level, 200.0 * Math.pow(1.2, level - 1));
        }
    }
    
    public MiningSkill() {
        super(SkillType.MINING.getId(), 
              SkillType.MINING.getDisplayName(), 
              SkillType.MINING.getDescription(), 
              50); // Max level of 50 for main skills
        
        // Initialize subskills
        initializeSubskills();
        
        // Initialize rewards
        initializeRewards();
    }
    
    private void initializeSubskills() {
        // Create the subskills
        OreExtractionSubskill oreExtraction = new OreExtractionSubskill(this);
        GemCarvingSubskill gemCarving = new GemCarvingSubskill(this);
        
        // Add them to the AbstractSkill's subskills list using the addSubskill method
        addSubskill(oreExtraction);
        addSubskill(gemCarving);
        
        // Future subskills
        // addSubskill(new OreEfficiencySubskill(this));
        // addSubskill(new RareFindsSubskill(this));
        // addSubskill(new StoneBreakerSubskill(this));
    }
    
    private void initializeRewards() {
        // Level 1: Mining Fortune +0.1
        List<SkillReward> level1Rewards = new ArrayList<>();
        level1Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.1));
        rewardsByLevel.put(1, level1Rewards);
        
        // Level 5: Mining Fortune +0.2, Currency +100
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.2));
        level5Rewards.add(new CurrencyReward(100));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Mining Fortune +0.3, Luck +1 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.3));
        level10Rewards.add(new StatReward(SkillRewardType.LUCK, 1));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Mining Fortune +0.5, Currency +500 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.5));
        level25Rewards.add(new CurrencyReward(500));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Mining Fortune +1.0, Currency +1000 (Milestone level - max level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 1.0));
        level50Rewards.add(new CurrencyReward(1000));
        rewardsByLevel.put(50, level50Rewards);
    }
    
    @Override
    public boolean isMainSkill() {
        return true;
    }
    
    @Override
    public Skill getParentSkill() {
        return null; // This is a main skill, no parent
    }
    
    @Override
    public List<SkillReward> getRewardsForLevel(int level) {
        return rewardsByLevel.getOrDefault(level, new ArrayList<>());
    }
    
    @Override
    public boolean hasMilestoneAt(int level) {
        return MILESTONE_LEVELS.contains(level);
    }
    
    @Override
    public List<Integer> getMilestones() {
        return new ArrayList<>(MILESTONE_LEVELS);
    }
    
    @Override
    public Map<Integer, Double> getXpRequirements() {
        return new HashMap<>(XP_REQUIREMENTS);
    }
    
    @Override
    public double getXpForLevel(int level) {
        return XP_REQUIREMENTS.getOrDefault(level, 9999999.0);
    }
    
    /**
     * Get the total mining fortune bonus from this skill (parent + subskills)
     * @param level The level of this main skill
     * @return A multiplier for mining fortune (1.0 = normal fortune)
     */
    public double getMiningFortuneBonus(int level) {
        // Base bonus from main skill level: 0.01 per level
        double bonus = 1.0 + (level * 0.01);
        
        // Additional bonuses for milestone levels
        if (level >= 10) bonus += 0.1;
        if (level >= 25) bonus += 0.15;
        if (level >= 50) bonus += 0.25;
        
        return bonus;
    }
}