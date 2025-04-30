package com.server.profiles.skills.skills.mining.subskills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

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
        // Level 5: Mining Fortune +0.10
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 1.00));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Mining Fortune +0.20 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 2.00));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Mining Fortune +0.30 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 3.00));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Mining Fortune +0.40 (Milestone level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 4.00));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Mining Fortune +0.50 (Milestone level)
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 5.00));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Mining Fortune +1.00 (Milestone level, max level)
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 10.00));
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
     * Legacy method for backward compatibility
     * @param level The level to calculate speed for
     * @return A multiplier for mining speed (1.0 = normal speed)
     */
    public double getMiningSpeedMultiplier(int level) {
        // Base speed multiplier from skill level
        double baseMultiplier = 1.0 + (level * 0.01); // 1% per level
        
        // We don't have player context, so we can't add skill tree bonuses
        return baseMultiplier;
    }

    /**
     * Get the mining speed multiplier based on skill level and material
     * @param level The level to calculate speed for
     * @param material The material being mined
     * @return A multiplier for mining speed (1.0 = normal speed)
     */
    public double getMiningSpeedMultiplier(int level, Material material) {
        // Base speed multiplier from skill level
        double baseMultiplier = 1.0 + (level * 0.01); // 1% per level
        
        // Add material-specific bonuses
        if (material != null) {
            if (material.name().contains("DEEPSLATE")) {
                // Deepslate is harder to mine, so the bonus is less effective
                return baseMultiplier * 0.8;
            } else if (material.name().contains("NETHER")) {
                // Nether materials get a slightly better bonus
                return baseMultiplier * 1.1;
            }
        }
        
        return baseMultiplier;
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

    /**
     * Calculate mining fortune bonus based on skill level
     * Legacy method for backward compatibility
     * @param level The level to calculate fortune for
     * @return The mining fortune bonus
     */
    public double getMiningFortuneBonus(int level) {
        // Base fortune bonus from skill level
        double baseBonus = level * 0.5; // 0.5 per level
        
        // We don't have player context, so we can't add skill tree bonuses
        return baseBonus;
    }

    /**
     * Get the benefits from unlocked skill tree nodes
     * @param player The player to get benefits for
     * @return A map of benefit types to their values
     */
    public Map<String, Double> getSkillTreeBenefits(Player player) {
        Map<String, Double> benefits = new HashMap<>();
        
        // Default benefit values
        benefits.put("mining_fortune", 0.0);
        benefits.put("mining_speed", 0.0);
        benefits.put("vein_miner_size", 0.0);
        benefits.put("smelting_chance", 0.0);
        benefits.put("ore_radar_range", 0.0);
        benefits.put("xp_boost", 0.0);
        benefits.put("token_yield", 0.0);
        benefits.put("hunger_reduction", 0.0);
        benefits.put("health_regen", 0.0);
        
        // Get player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return benefits;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return benefits;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        Set<String> unlockedNodes = treeData.getUnlockedNodes(this.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(this.getId());
        
        // Apply benefits from nodes
        
        // Mining Fortune nodes
        if (nodeLevels.containsKey("mining_fortune")) {
            int level = nodeLevels.get("mining_fortune");
            double fortune = 0.0;
            
            switch (level) {
                case 1: fortune = 0.5; break;
                case 2: fortune = 1.0; break;
                case 3: fortune = 1.5; break;
                case 4: fortune = 2.0; break;
                case 5: fortune = 3.0; break;
            }
            
            benefits.put("mining_fortune", fortune);
        }
        
        // Master Fortune node (single level)
        if (unlockedNodes.contains("master_fortune")) {
            benefits.put("mining_fortune", benefits.get("mining_fortune") + 5.0);
        }
        
        // Mining Speed nodes
        if (nodeLevels.containsKey("mining_speed")) {
            int level = nodeLevels.get("mining_speed");
            double speedBoost = 0.0;
            
            switch (level) {
                case 1: speedBoost = 0.05; break; // 5%
                case 2: speedBoost = 0.10; break; // 10%
                case 3: speedBoost = 0.15; break; // 15%
                case 4: speedBoost = 0.20; break; // 20%
                case 5: speedBoost = 0.30; break; // 30%
            }
            
            benefits.put("mining_speed", speedBoost);
        }
        
        // Deepslate Efficiency nodes
        if (nodeLevels.containsKey("deepslate_efficiency")) {
            int level = nodeLevels.get("deepslate_efficiency");
            double deepslateBoost = 0.0;
            
            switch (level) {
                case 1: deepslateBoost = 0.10; break; // 10%
                case 2: deepslateBoost = 0.20; break; // 20%
                case 3: deepslateBoost = 0.30; break; // 30%
            }
            
            benefits.put("deepslate_speed", deepslateBoost);
        }
        
        // Vein Miner nodes
        if (nodeLevels.containsKey("vein_miner")) {
            int level = nodeLevels.get("vein_miner");
            int size = 0;
            
            switch (level) {
                case 1: size = 3; break;
                case 2: size = 5; break;
                case 3: size = 8; break;
            }
            
            benefits.put("vein_miner_size", (double) size);
        }
        
        // Smelting Touch nodes
        if (nodeLevels.containsKey("smelting_touch")) {
            int level = nodeLevels.get("smelting_touch");
            double chance = 0.0;
            
            switch (level) {
                case 1: chance = 0.20; break; // 20%
                case 2: chance = 0.40; break; // 40%
                case 3: chance = 0.60; break; // 60%
                case 4: chance = 0.80; break; // 80%
                case 5: chance = 1.00; break; // 100%
            }
            
            benefits.put("smelting_chance", chance);
        }
        
        // Ore Radar nodes
        if (nodeLevels.containsKey("ore_radar")) {
            int level = nodeLevels.get("ore_radar");
            int range = 0;
            
            switch (level) {
                case 1: range = 5; break;
                case 2: range = 8; break;
                case 3: range = 12; break;
            }
            
            benefits.put("ore_radar_range", (double) range);
        }
        
        // XP Boost nodes
        if (nodeLevels.containsKey("xp_boost")) {
            int level = nodeLevels.get("xp_boost");
            double boost = 0.0;
            
            switch (level) {
                case 1: boost = 0.10; break; // 10%
                case 2: boost = 0.20; break; // 20%
                case 3: boost = 0.30; break; // 30%
                case 4: boost = 0.50; break; // 50%
            }
            
            benefits.put("xp_boost", boost);
        }
        
        // Token Yield nodes
        if (nodeLevels.containsKey("token_yield")) {
            int level = nodeLevels.get("token_yield");
            double chance = 0.0;
            
            switch (level) {
                case 1: chance = 0.10; break; // 10%
                case 2: chance = 0.20; break; // 20%
                case 3: chance = 0.30; break; // 30%
                case 4: chance = 0.50; break; // 50%
            }
            
            benefits.put("token_yield", chance);
        }
        
        // Mining Stamina nodes
        if (nodeLevels.containsKey("mining_stamina")) {
            int level = nodeLevels.get("mining_stamina");
            double reduction = 0.0;
            
            switch (level) {
                case 1: reduction = 0.10; break; // 10%
                case 2: reduction = 0.20; break; // 20%
                case 3: reduction = 0.30; break; // 30%
            }
            
            benefits.put("hunger_reduction", reduction);
        }
        
        // Mining Regeneration nodes
        if (nodeLevels.containsKey("mining_regeneration")) {
            int level = nodeLevels.get("mining_regeneration");
            double regen = 0.0;
            
            switch (level) {
                case 1: regen = 0.2; break; // 0.2 health/sec
                case 2: regen = 0.4; break; // 0.4 health/sec
                case 3: regen = 0.6; break; // 0.6 health/sec
            }
            
            benefits.put("health_regen", regen);
        }
        
        return benefits;
    }

    /**
     * Apply skill tree benefits to a player
     * This should be called when mining-related events occur
     */
    public void applySkillTreeBenefits(Player player, Block block) {
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        
        // Apply vein miner if applicable
        int veinSize = benefits.get("vein_miner_size").intValue();
        if (veinSize > 0 && affectsMaterial(block.getType())) {
            // Implement vein miner logic
            mineOreVein(player, block, veinSize);
        }
        
        // Apply smelting touch if applicable
        double smeltChance = benefits.get("smelting_chance");
        if (smeltChance > 0 && affectsMaterial(block.getType())) {
            // Implement smelting touch logic (in block drop event)
            if (Math.random() < smeltChance) {
                // Flag this block to have drops auto-smelted
                // This would be handled in your block break event
                block.setMetadata("auto_smelt", new FixedMetadataValue(Main.getInstance(), true));
            }
        }
        
        // Apply ore radar if applicable
        int radarRange = benefits.get("ore_radar_range").intValue();
        if (radarRange > 0) {
            // Implement ore radar logic
            highlightNearbyOres(player, radarRange);
        }
    }

    /**
     * Get the mining fortune bonus from skill tree nodes
     */
    public double getMiningFortuneFromSkillTree(Player player) {
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        return benefits.get("mining_fortune");
    }

    /**
     * Get the mining speed multiplier from skill tree nodes
     */
    public double getMiningSpeedFromSkillTree(Player player, Material material) {
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        
        double speedBoost = benefits.get("mining_speed");
        
        // Apply deepslate-specific boost if applicable
        if (material.name().contains("DEEPSLATE")) {
            speedBoost += benefits.get("deepslate_speed");
        }
        
        // Apply nether-specific boost if applicable
        if (material.name().contains("NETHER") || material == Material.ANCIENT_DEBRIS || 
            material == Material.BLACKSTONE || material == Material.BASALT) {
            
            // Get player's skill tree data and check nether_mining level
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile != null) {
                    int level = profile.getSkillTreeData().getNodeLevel(this.getId(), "nether_mining");
                    
                    switch (level) {
                        case 1: speedBoost += 0.10; break; // 10%
                        case 2: speedBoost += 0.20; break; // 20%
                        case 3: speedBoost += 0.30; break; // 30%
                        case 4: speedBoost += 0.50; break; // 50%
                    }
                }
            }
        }
        
        return 1.0 + speedBoost; // Convert to multiplier (e.g., 0.2 becomes 1.2)
    }

    /**
     * Method to mine an ore vein (Vein Miner ability)
     */
    private void mineOreVein(Player player, Block startBlock, int maxBlocks) {
        // Implementation depends on your server setup
        // This is a placeholder for the vein mining logic
        // You would typically use a breadth-first search to find connected blocks
    }

    /**
     * Method to highlight nearby ores (Ore Radar ability)
     */
    private void highlightNearbyOres(Player player, int range) {
        // Implementation depends on your server setup
        // This is a placeholder for the ore radar logic
        // You would typically use a particle effect to highlight ores
    }
}