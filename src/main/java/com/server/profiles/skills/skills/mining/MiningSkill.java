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
import com.server.profiles.skills.rewards.rewards.ItemReward;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;

/**
 * The Mining skill - focused on mining ores and stone to collect valuable resources
 */
public class MiningSkill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(5, 10, 15, 20, 25, 30, 35, 40, 45, 50);
    
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
    
    /**
     * Initialize the rewards for each level
     */
    private void initializeRewards() {
        // Level 5: Mining Fortune +0.20, Currency +100 (Milestone level)
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.20));
        level5Rewards.add(new CurrencyReward(100));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Mining Fortune +0.30, Luck +1 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.30));
        level10Rewards.add(new StatReward(SkillRewardType.LUCK, 1));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 15: Mining Fortune +0.25 (Milestone level)
        List<SkillReward> level15Rewards = new ArrayList<>();
        level15Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.25));
        rewardsByLevel.put(15, level15Rewards);
        
        // Level 20: Mining Fortune +0.30, Currency +250 (Milestone level)
        List<SkillReward> level20Rewards = new ArrayList<>();
        level20Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.30));
        level20Rewards.add(new CurrencyReward(250));
        rewardsByLevel.put(20, level20Rewards);
        
        // Level 25: Mining Fortune +0.50, Currency +500 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.50));
        level25Rewards.add(new CurrencyReward(500));
        level25Rewards.add(new ItemReward("miners_toolkit", 1));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 30: Mining Fortune +0.35 (Milestone level)
        List<SkillReward> level30Rewards = new ArrayList<>();
        level30Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.35));
        rewardsByLevel.put(30, level30Rewards);
        
        // Level 35: Mining Fortune +0.40 (Milestone level)
        List<SkillReward> level35Rewards = new ArrayList<>();
        level35Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.40));
        rewardsByLevel.put(35, level35Rewards);
        
        // Level 40: Mining Fortune +0.45, Currency +750 (Milestone level)
        List<SkillReward> level40Rewards = new ArrayList<>();
        level40Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.45));
        level40Rewards.add(new CurrencyReward(750));
        rewardsByLevel.put(40, level40Rewards);
        
        // Level 45: Mining Fortune +0.50 (Milestone level)
        List<SkillReward> level45Rewards = new ArrayList<>();
        level45Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.50));
        rewardsByLevel.put(45, level45Rewards);
        
        // Level 50: Mining Fortune +1.00, Currency +1000 (Milestone level - max level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 1.00));
        level50Rewards.add(new CurrencyReward(1000));
        level50Rewards.add(new ItemReward("masterful_pickaxe", 1));
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
        if (level >= 10) bonus += 0.10;
        if (level >= 25) bonus += 0.15;
        if (level >= 50) bonus += 0.25;
        
        // Return with full precision
        return bonus;
    }
}