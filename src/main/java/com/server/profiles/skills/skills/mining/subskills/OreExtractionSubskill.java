package com.server.profiles.skills.skills.mining.subskills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.StatReward;

/**
 * Ore Extraction Subskill - Focused on fast, efficient mining of ores (bulk harvests)
 * Heavy-duty pickaxes, stamina/resource management, breaking large clusters fast, watch out for cave-ins.
 */
public class OreExtractionSubskill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(10, 25, 50, 75, 100);
    
    // Materials that benefit from this skill
    private static final List<Material> AFFECTED_MATERIALS = Arrays.asList(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    );
    
    // Cache for rewards by level
    private final Map<Integer, List<SkillReward>> rewardsByLevel = new HashMap<>();
    
    static {
    // Set up XP requirements for each level
        for (int level = 1; level <= 100; level++) {
            // New formula: More gradual scaling, especially for early levels
            // We'll use a quadratic formula instead of exponential to prevent too rapid early gains
            XP_REQUIREMENTS.put(level, 100.0 + (50.0 * level) + (10.0 * Math.pow(level, 1.5)));
        }
    }
    
    private final Skill parentSkill;
    
    public OreExtractionSubskill(Skill parentSkill) {
        super(SubskillType.ORE_EXTRACTION.getId(), 
            SubskillType.ORE_EXTRACTION.getDisplayName(), 
            SubskillType.ORE_EXTRACTION.getDescription(), 
            100); // Max level of 100
        
        this.parentSkill = parentSkill;
        
        // Initialize rewards
        initializeRewards();
    }
    
    private void initializeRewards() {
        // Level 5: Mining Fortune +0.1
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.1));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Mining Fortune +0.2 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.2));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Mining Fortune +0.3 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.3));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Mining Fortune +0.4 (Milestone level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.4));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Mining Fortune +0.5 (Milestone level)
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.5));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Mining Fortune +1.0 (Milestone level, max level)
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 1.0));
        rewardsByLevel.put(100, level100Rewards);
    }
    
    @Override
    public boolean isMainSkill() {
        return false; // This is a subskill
    }
    
    @Override
    public Skill getParentSkill() {
        return parentSkill;
    }
    
    @Override
    public List<Skill> getSubskills() {
        return new ArrayList<>(); // Subskills don't have their own subskills
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
     * Check if a material is affected by this skill
     */
    public boolean affectsMaterial(Material material) {
        return AFFECTED_MATERIALS.contains(material);
    }
    
    /**
     * Get the mining speed multiplier based on skill level
     * @param level The current level of this skill
     * @return A multiplier for mining speed (1.0 = normal speed)
     */
    public double getMiningSpeedMultiplier(int level) {
        // Formula: 1.0 (normal speed) + 0.01 per level (up to +100% at level 100)
        return 1.0 + (level * 0.01);
    }
    
    /**
     * Get the bonus drop multiplier based on skill level
     * @param level The current level of this skill
     * @return A multiplier for additional drops (0.0 = no bonus)
     */
    public double getBonusDropChance(int level) {
        // Formula: 0.005 per level (0.5% per level, up to 50% at level 100)
        return level * 0.005;
    }

    /**
     * Calculate chance of triggering a cave-in effect
     * @param level The current level of this skill
     * @return A percentage chance (0.0 to 1.0) of triggering cave-in
     */
    public double getCaveInChance(int level) {
        // Formula: Starts at 25% at level 1, decreases by 0.24% per level, down to 1% at level 100
        return Math.max(0.01, 0.25 - (level * 0.0024));
    }
}