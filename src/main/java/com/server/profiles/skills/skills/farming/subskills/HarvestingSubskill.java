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
 * Harvesting Subskill - Focused on breaking and collecting crops
 * Players gain XP when breaking crops, but can only break crops they've unlocked
 */
public class HarvestingSubskill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(
        5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    );
    
    // Materials that this skill affects
    private static final List<Material> CROP_MATERIALS = Arrays.asList(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.SWEET_BERRY_BUSH,
        Material.COCOA,
        Material.NETHER_WART,
        Material.MELON,
        Material.PUMPKIN
    );
    
    // Cache for rewards by level
    private final Map<Integer, List<SkillReward>> rewardsByLevel = new HashMap<>();
    
    static {
        // Set up XP requirements for each level
        for (int level = 1; level <= 100; level++) {
            // Gradual scaling formula similar to ore extraction
            XP_REQUIREMENTS.put(level, 100.0 + (50.0 * level) + (10.0 * Math.pow(level, 1.5)));
        }
    }
    
    public HarvestingSubskill(Skill parentSkill) {
        super(SubskillType.HARVESTING.getId(), 
            SubskillType.HARVESTING.getDisplayName(), 
            SubskillType.HARVESTING.getDescription(), 
            100, // Max level of 100
            parentSkill); // Pass parent skill to AbstractSkill constructor
        
        // Initialize rewards
        initializeRewards();
    }
    
    private void initializeRewards() {
        // Level 5: Farming Fortune +1.00
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 1.00));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Farming Fortune +2.00 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 2.00));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Farming Fortune +3.00 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 3.00));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Farming Fortune +4.00 (Milestone level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 4.00));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Farming Fortune +5.00 (Milestone level)
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 5.00));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Farming Fortune +10.00 (Milestone level, max level)
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 10.00));
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
     * Check if a material is a crop that this skill affects
     */
    public boolean affectsCrop(Material material) {
        return CROP_MATERIALS.contains(material);
    }
    
    /**
     * Check if a player can harvest a specific crop
     * @param player The player to check
     * @param material The crop material to check
     * @return True if the player can harvest this crop, false otherwise
     */
    public boolean canHarvestCrop(Player player, Material material) {
        // Wheat is always unlocked by default
        if (material == Material.WHEAT) {
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
            case CARROTS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_carrots");
            case POTATOES:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_potatoes");
            case BEETROOTS:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_beetroots");
            case SWEET_BERRY_BUSH:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_sweet_berries");
            case COCOA:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_cocoa");
            case NETHER_WART:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_nether_wart");
            case MELON:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_melons");
            case PUMPKIN:
                return treeData.isNodeUnlocked(farmingSkillId, "unlock_pumpkins");
            default:
                return false;
        }
    }
    
    /**
     * Check if a player can gain XP from a specific crop
     */
    public boolean canGainXpFromCrop(Player player, Material material) {
        // First check if we can harvest this crop at all
        if (!canHarvestCrop(player, material)) {
            return false;
        }
        
        // Then check if this subskill affects this material
        return affectsCrop(material);
    }
    
    /**
     * Get the bonus drop chance based on skill level
     * @param level The current level of this skill
     * @return A multiplier for additional drops (0.0 = no bonus)
     */
    public double getBonusDropChance(int level) {
        // Formula: 0.005 per level (0.5% per level, up to 50% at level 100)
        return level * 0.005;
    }
    
    /**
     * Get the farming fortune bonus from skill tree nodes
     */
    public double getFarmingFortuneFromSkillTree(Player player) {
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        return benefits.get("farming_fortune");
    }
    
    /**
     * Get the benefits from unlocked skill tree nodes
     * @param player The player to get benefits for
     * @return A map of benefit types to their values
     */
    public Map<String, Double> getSkillTreeBenefits(Player player) {
        Map<String, Double> benefits = new HashMap<>();
        
        // Default benefit values
        benefits.put("farming_fortune", 0.0);
        benefits.put("harvest_speed", 0.0);
        benefits.put("xp_boost", 0.0);
        
        // Get player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return benefits;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return benefits;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(this.getId());
        
        // Apply benefits from nodes
        
        // Farming Fortune node - Add 0.5 farming fortune per level
        if (nodeLevels.containsKey("farming_fortune")) {
            int level = nodeLevels.get("farming_fortune");
            double fortune = level * 0.5;
            benefits.put("farming_fortune", fortune);
        }
        
        // Harvest Speed node - Add speed bonus
        if (nodeLevels.containsKey("harvest_speed")) {
            int level = nodeLevels.get("harvest_speed");
            double speed = level * 0.05; // 5% per level
            benefits.put("harvest_speed", speed);
        }
        
        // XP Boost node - Add 0.5% XP boost per level
        if (nodeLevels.containsKey("harvesting_xp")) {
            int level = nodeLevels.get("harvesting_xp");
            double xpBoost = level * 0.5 / 100.0; // Convert percentage to decimal
            benefits.put("xp_boost", xpBoost);
        }
        
        return benefits;
    }
    
    /**
     * Apply node upgrade benefits
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Harvesting] Applying node upgrade: " + nodeId + " from " + oldLevel + " to " + newLevel);
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        switch (nodeId) {
            case "unlock_carrots":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.GOLD + "Carrots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting carrots.");
                break;
            case "unlock_potatoes":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.GOLD + "Potatoes" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting potatoes.");
                break;
            case "unlock_beetroots":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.DARK_RED + "Beetroots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting beetroots.");
                break;
            case "unlock_sweet_berries":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.RED + "Sweet Berries" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting sweet berries.");
                break;
            case "unlock_cocoa":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.DARK_RED + "Cocoa Beans" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting cocoa.");
                break;
            case "unlock_nether_wart":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.RED + "Nether Wart" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting nether wart.");
                break;
            case "unlock_melons":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.GREEN + "Melons" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting melons.");
                break;
            case "unlock_pumpkins":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + 
                                ChatColor.GOLD + "Pumpkins" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "You will now gain XP from harvesting pumpkins.");
                break;
            default:
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Harvesting] Unknown node ID: " + nodeId);
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
                "[Harvesting] Handling skill tree reset for " + player.getName());
        }
        
        // Handle crop unlock resets
        if (oldNodeLevels.containsKey("unlock_carrots")) {
            player.sendMessage(ChatColor.GRAY + "Carrot harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from carrots.");
        }
        
        if (oldNodeLevels.containsKey("unlock_potatoes")) {
            player.sendMessage(ChatColor.GRAY + "Potato harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from potatoes.");
        }
        
        if (oldNodeLevels.containsKey("unlock_beetroots")) {
            player.sendMessage(ChatColor.GRAY + "Beetroot harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from beetroots.");
        }
        
        if (oldNodeLevels.containsKey("unlock_sweet_berries")) {
            player.sendMessage(ChatColor.GRAY + "Sweet Berry harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from sweet berries.");
        }
        
        if (oldNodeLevels.containsKey("unlock_cocoa")) {
            player.sendMessage(ChatColor.GRAY + "Cocoa harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from cocoa.");
        }
        
        if (oldNodeLevels.containsKey("unlock_nether_wart")) {
            player.sendMessage(ChatColor.GRAY + "Nether Wart harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from nether wart.");
        }
        
        if (oldNodeLevels.containsKey("unlock_melons")) {
            player.sendMessage(ChatColor.GRAY + "Melon harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from melons.");
        }
        
        if (oldNodeLevels.containsKey("unlock_pumpkins")) {
            player.sendMessage(ChatColor.GRAY + "Pumpkin harvesting access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from pumpkins.");
        }
    }
}
