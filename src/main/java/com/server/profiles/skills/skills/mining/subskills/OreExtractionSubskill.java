package com.server.profiles.skills.skills.mining.subskills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.SkillTreeRegistry;
import com.server.profiles.stats.PlayerStats;

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
        benefits.put("deepslate_speed", 0.0);
        benefits.put("nether_speed", 0.0);
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
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(this.getId());
        
        // Apply benefits from nodes
        
        // Mining Fortune node - Add 0.5 mining fortune per level
        if (nodeLevels.containsKey("mining_fortune")) {
            int level = nodeLevels.get("mining_fortune");
            double fortune = level * 0.5;
            benefits.put("mining_fortune", fortune);
            
            // Apply the mining fortune directly to the player's stats
            // Use the increaseDefaultMiningFortune method which properly updates both default and current values
            PlayerStats stats = profile.getStats();
            
            // Store the current mining fortune to calculate the difference
            double oldFortune = stats.getMiningFortune();
            
            // Use the dedicated method to increase the mining fortune value
            stats.increaseDefaultMiningFortune(fortune);
            
            // Log the change for debugging
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Updated mining fortune for " + player.getName() + 
                    " from skill tree node: " + oldFortune + " -> " + stats.getMiningFortune());
            }
        }
        
        // XP Boost node - Add 0.5% XP boost per level
        if (nodeLevels.containsKey("ore_extraction_xp")) {
            int level = nodeLevels.get("ore_extraction_xp");
            double xpBoost = level * 0.5 / 100.0; // Convert percentage to decimal
            benefits.put("xp_boost", xpBoost);
        }
        
        // Note: The item_unlocker and ability_unlocker nodes don't provide any direct stats
        
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
     * Apply skill tree benefits to a player when a node is upgraded
     * This should be called when a node is unlocked or upgraded
     * 
     * @param player The player to apply benefits to
     * @param nodeId The ID of the node being upgraded
     * @param oldLevel The previous level of the node
     * @param newLevel The new level of the node
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        PlayerStats stats = profile.getStats();
        
        // Calculate the incremental benefit
        int levelDifference = newLevel - oldLevel;
        
        if (nodeId.equals("mining_fortune")) {
            // Add 0.5 mining fortune per level
            double fortuneBonus = levelDifference * 0.5;
            stats.increaseDefaultMiningFortune(fortuneBonus);
            
            // Log for debugging
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Added " + fortuneBonus + " mining fortune to " + 
                    player.getName() + " (now " + stats.getMiningFortune() + ") from skill tree node upgrade");
            }
        } else if (nodeId.equals("mining_xp_boost")) {
            // Calculate XP to award - 100 XP per level
            int xpAmount = levelDifference * 100;
            
            // Get the Mining parent skill
            Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
            
            if (miningSkill != null) {
                // Log before giving XP (for debugging)
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Awarding " + xpAmount + " Mining XP from mining_xp_boost to " + 
                        player.getName() + " (level diff: " + levelDifference + ")");
                }
            
                // Add XP to the Mining skill directly
                boolean leveledUp = miningSkill.addExperience(player, xpAmount);
                
                // Notify the player
                player.sendMessage(ChatColor.GREEN + "You gained " + xpAmount + " Mining XP from your Mining Knowledge!");
                
                // Play sound effect
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                
                // Log for debugging
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Added " + xpAmount + " Mining XP to " + 
                        player.getName() + " from mining_xp_boost node upgrade");
                }
                
                // Also ensure this node is marked as special for preservation during resets
                PlayerSkillTreeData treeData = profile.getSkillTreeData();
                SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(getId());
                if (tree != null) {
                    SkillTreeNode node = tree.getNode("mining_xp_boost");
                    if (node != null) {
                        // Always mark it as special, just to be sure
                        if (!node.isSpecialNode()) {
                            node.setSpecialNode(true);
                            
                            if (Main.getInstance().isDebugMode()) {
                                Main.getInstance().getLogger().info("Marked mining_xp_boost node as special for " + player.getName());
                            }
                        }
                    }
                }
            }
        }
        // Add other node types here as needed
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

    /**
     * Check if a player can mine a specific ore type
     * @param player The player to check
     * @param material The material to check
     * @return True if the player can mine this material, false otherwise
     */
    public boolean canMineOre(Player player, Material material) {
        // Only regular coal ore is available by default, not deepslate variant
        if (material == Material.COAL_ORE) {
            return true;
        }
        
        // Get the player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Check specific ore types that need unlocks
        if (material == Material.DEEPSLATE_COAL_ORE) {
            return treeData.isNodeUnlocked(this.getId(), "unlock_deepslate_mining");
        }
        
        if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
            return treeData.isNodeUnlocked(this.getId(), "unlock_iron_ore");
        }
        
        // For now, all other ores are locked until we add their specific unlock nodes
        if (affectsMaterial(material) && material != Material.COAL_ORE) {
            return false;
        }
        
        // Non-ore materials are always mineable
        return true;
    }
}