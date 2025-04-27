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
import com.server.profiles.skills.rewards.rewards.CurrencyReward;
import com.server.profiles.skills.rewards.rewards.StatReward;

/**
 * Gem Carving Subskill - Carefully extract fragile crystals and gems embedded in rocks
 * Mini-precision game "trace" the outline without shattering the gem; slow, methodical.
 */
public class GemCarvingSubskill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(10, 25, 50, 75, 100);
    
    // Materials that can potentially contain gems
    private static final List<Material> GEM_HOST_MATERIALS = Arrays.asList(
        Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
        Material.DEEPSLATE, Material.TUFF, Material.BASALT, Material.BLACKSTONE,
        Material.AMETHYST_CLUSTER, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE
    );
    
    // Potential gem materials that can be found
    private static final List<Material> GEM_MATERIALS = Arrays.asList(
        Material.DIAMOND, Material.EMERALD, Material.AMETHYST_SHARD, 
        Material.QUARTZ, Material.LAPIS_LAZULI
    );
    
    // Cache for rewards by level
    private final Map<Integer, List<SkillReward>> rewardsByLevel = new HashMap<>();
    
    static {
        // Set up XP requirements for each level
        for (int level = 1; level <= 100; level++) {
            // Formula: Base 100 XP, increasing by 12% each level (slightly easier than ore extraction)
            XP_REQUIREMENTS.put(level, 100.0 * Math.pow(1.12, level - 1));
        }
    }
    
    private final Skill parentSkill;
    
    public GemCarvingSubskill(Skill parentSkill) {
        super(SubskillType.GEM_CARVING.getId(), 
              SubskillType.GEM_CARVING.getDisplayName(), 
              SubskillType.GEM_CARVING.getDescription(), 
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
        
        // Level 10: Mining Fortune +0.1, Luck +1 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.1));
        level10Rewards.add(new StatReward(SkillRewardType.LUCK, 1));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Currency Reward 500 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new CurrencyReward(500));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Mining Fortune +0.3, Luck +2 (Milestone level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.3));
        level50Rewards.add(new StatReward(SkillRewardType.LUCK, 2));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Currency Reward 1500 (Milestone level)
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new CurrencyReward(1500));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Mining Fortune +0.5, Luck +5 (Milestone level, max level)
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.5));
        level100Rewards.add(new StatReward(SkillRewardType.LUCK, 5));
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
     * Check if a material could contain gems
     */
    public boolean isGemHostMaterial(Material material) {
        return GEM_HOST_MATERIALS.contains(material);
    }
    
    /**
     * Get chance of finding a gem based on skill level
     * @param level The current level of this skill
     * @return A percentage chance (0.0 to 1.0) of finding a gem
     */
    public double getGemFindChance(int level) {
        // Formula: Base 0.5% + 0.05% per level (up to 5.5% at level 100)
        return 0.005 + (level * 0.0005);
    }
    
    /**
     * Get chance of successfully extracting a gem without breaking it
     * @param level The current level of this skill
     * @return A percentage chance (0.0 to 1.0) of successful extraction
     */
    public double getExtractionSuccessChance(int level) {
        // Formula: Base 30% + 0.5% per level (up to 80% at level 100)
        return 0.3 + (level * 0.005);
    }
    
    /**
     * Get a random gem material based on skill level
     * Higher levels have better chances for rarer gems
     * @param level The current level of this skill
     * @return A Material representing a gem
     */
    public Material getRandomGemMaterial(int level) {
        // This would be implemented with weighted probabilities based on level
        // For now, just return a random gem material
        int index = (int)(Math.random() * GEM_MATERIALS.size());
        return GEM_MATERIALS.get(index);
    }
    
    /**
     * Get bonus gem quality (affects value) based on skill level
     * @param level The current level of this skill
     * @return A multiplier for gem value (1.0 = normal value)
     */
    public double getGemQualityMultiplier(int level) {
        // Formula: 1.0 (normal quality) + 0.01 per level (up to +100% at level 100)
        return 1.0 + (level * 0.01);
    }

}