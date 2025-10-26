package com.server.profiles.skills.skills.farming.subskills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
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
 * Cultivating Subskill - Focused on planting and growing crops
 * Players gain XP when planting crops and when crops grow
 * Requires Harvesting level 10 to unlock
 */
public class CultivatingSubskill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(
        5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    );
    
    // Materials that this skill affects (plantable items)
    private static final List<Material> PLANTABLE_MATERIALS = Arrays.asList(
        Material.WHEAT_SEEDS,
        Material.CARROT,
        Material.POTATO,
        Material.BEETROOT_SEEDS,
        Material.SWEET_BERRIES,
        Material.COCOA_BEANS,
        Material.NETHER_WART,
        Material.MELON_SEEDS,
        Material.PUMPKIN_SEEDS
    );
    
    // Cache for rewards by level
    private final Map<Integer, List<SkillReward>> rewardsByLevel = new HashMap<>();
    
    static {
        // Set up XP requirements for each level
        for (int level = 1; level <= 100; level++) {
            // Gradual scaling formula similar to harvesting
            XP_REQUIREMENTS.put(level, 100.0 + (50.0 * level) + (10.0 * Math.pow(level, 1.5)));
        }
    }
    
    public CultivatingSubskill(Skill parentSkill) {
        super(SubskillType.CULTIVATING.getId(), 
            SubskillType.CULTIVATING.getDisplayName(), 
            SubskillType.CULTIVATING.getDescription(), 
            100, // Max level of 100
            parentSkill); // Pass parent skill to AbstractSkill constructor
        
        // Initialize rewards
        initializeRewards();
    }
    
    private void initializeRewards() {
        // Level 5: Growth Speed +1%
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.01));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Growth Speed +2%
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.02));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Growth Speed +3%
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.03));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Growth Speed +5%
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.05));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Growth Speed +7%
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.07));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Growth Speed +10%
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.CROP_GROWTH_SPEED, 0.10));
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
     * Check if a material is a plantable item that this skill affects
     */
    public boolean affectsPlantable(Material material) {
        return PLANTABLE_MATERIALS.contains(material);
    }
    
    /**
     * Check if a player can plant a specific crop
     * @param player The player to check
     * @param material The plantable material to check
     * @return True if the player can plant this crop, false otherwise
     */
    public boolean canPlantCrop(Player player, Material material) {
        // First check if Cultivating is unlocked via skill tree
        if (!isCultivatingUnlocked(player)) {
            return false;
        }
        
        // Wheat seeds are always plantable once Cultivating is unlocked
        if (material == Material.WHEAT_SEEDS) {
            return true;
        }
        
        // Get player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Check for each crop type unlock in the PARENT skill's tree (farming)
        String farmingSkillId = parentSkill.getId();
        switch (material) {
            case CARROT:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_carrots");
            case POTATO:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_potatoes");
            case BEETROOT_SEEDS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_beetroots");
            case SWEET_BERRIES:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_sweet_berries");
            case COCOA_BEANS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_cocoa");
            case NETHER_WART:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_nether_wart");
            case MELON_SEEDS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_melons");
            case PUMPKIN_SEEDS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_plant_pumpkins");
            default:
                return false;
        }
    }
    
    /**
     * Check if a player has unlocked the Cultivating subskill
     * Checks for the unlock_cultivating node in the farming skill tree
     */
    public boolean isCultivatingUnlocked(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Check if unlock_cultivating node is unlocked in the farming skill tree
        String farmingSkillId = parentSkill.getId();
        return treeData.isNodeUnlocked(farmingSkillId, "unlock_cultivating");
    }
    
    /**
     * Check if a player can gain XP from planting a specific crop
     */
    public boolean canGainXpFromPlanting(Player player, Material material) {
        // First check if we can plant this crop at all
        if (!canPlantCrop(player, material)) {
            return false;
        }
        
        // Then check if this subskill affects this material
        return affectsPlantable(material);
    }
    
    /**
     * Get the crop growth speed multiplier based on skill level
     * @param level The current level of this skill
     * @return A multiplier for crop growth speed (1.0 = normal speed)
     */
    public double getCropGrowthSpeedMultiplier(int level) {
        // Formula: 0.01 per level (1% per level, up to 100% faster at level 100)
        return 1.0 + (level * 0.01);
    }
    
    /**
     * Get the benefits from unlocked skill tree nodes
     * @param player The player to get benefits for
     * @return A map of benefit types to their values
     */
    public Map<String, Double> getSkillTreeBenefits(Player player) {
        Map<String, Double> benefits = new HashMap<>();
        
        // Default benefit values
        benefits.put("growth_speed", 0.0);
        benefits.put("xp_boost", 0.0);
        benefits.put("replant_chance", 0.0);
        
        // Get player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return benefits;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return benefits;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(this.getId());
        
        // Apply benefits from nodes
        
        // Growth Speed node - Add 2% growth speed per level
        if (nodeLevels.containsKey("growth_speed")) {
            int level = nodeLevels.get("growth_speed");
            double speed = level * 0.02; // 2% per level
            benefits.put("growth_speed", speed);
        }
        
        // XP Boost node - Add 0.5% XP boost per level
        if (nodeLevels.containsKey("cultivating_xp")) {
            int level = nodeLevels.get("cultivating_xp");
            double xpBoost = level * 0.5 / 100.0; // Convert percentage to decimal
            benefits.put("xp_boost", xpBoost);
        }
        
        // Auto-replant chance
        if (nodeLevels.containsKey("auto_replant")) {
            int level = nodeLevels.get("auto_replant");
            double chance = level * 0.10; // 10% per level
            benefits.put("replant_chance", chance);
        }
        
        return benefits;
    }
    
    /**
     * Apply node upgrade benefits
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Cultivating] Applying node upgrade: " + nodeId + " from " + oldLevel + " to " + newLevel);
        }
        
        switch (nodeId) {
            case "unlock_plant_carrots":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.GOLD + "Carrots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing carrots.");
                break;
            case "unlock_plant_potatoes":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.GOLD + "Potatoes" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing potatoes.");
                break;
            case "unlock_plant_beetroots":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.DARK_RED + "Beetroots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing beetroots.");
                break;
            case "unlock_plant_sweet_berries":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.RED + "Sweet Berries" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing sweet berries.");
                break;
            case "unlock_plant_cocoa":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.DARK_RED + "Cocoa Beans" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing cocoa.");
                break;
            case "unlock_plant_nether_wart":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.RED + "Nether Wart" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing nether wart.");
                break;
            case "unlock_plant_melons":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.GREEN + "Melons" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing melons.");
                break;
            case "unlock_plant_pumpkins":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + 
                                ChatColor.GOLD + "Pumpkins" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from planting and growing pumpkins.");
                break;
            default:
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Cultivating] Unknown node ID: " + nodeId);
                }
                break;
        }
    }
    
    /**
     * Handle skill tree reset for a player
     */
    public void handleSkillTreeReset(Player player, Map<String, Integer> oldNodeLevels) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Cultivating] Handling skill tree reset for " + player.getName());
        }
        
        // Handle crop planting unlock resets
        if (oldNodeLevels.containsKey("unlock_plant_carrots")) {
            player.sendMessage(ChatColor.GRAY + "Carrot planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from carrots.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_potatoes")) {
            player.sendMessage(ChatColor.GRAY + "Potato planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from potatoes.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_beetroots")) {
            player.sendMessage(ChatColor.GRAY + "Beetroot planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from beetroots.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_sweet_berries")) {
            player.sendMessage(ChatColor.GRAY + "Sweet Berry planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from sweet berries.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_cocoa")) {
            player.sendMessage(ChatColor.GRAY + "Cocoa planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from cocoa.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_nether_wart")) {
            player.sendMessage(ChatColor.GRAY + "Nether Wart planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from nether wart.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_melons")) {
            player.sendMessage(ChatColor.GRAY + "Melon planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from melons.");
        }
        
        if (oldNodeLevels.containsKey("unlock_plant_pumpkins")) {
            player.sendMessage(ChatColor.GRAY + "Pumpkin planting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer plant or gain XP from pumpkins.");
        }
    }
}
