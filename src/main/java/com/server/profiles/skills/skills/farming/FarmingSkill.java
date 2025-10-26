package com.server.profiles.skills.skills.farming;

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
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.CurrencyReward;
import com.server.profiles.skills.rewards.rewards.ItemReward;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.skills.farming.subskills.BotanySubskill;
import com.server.profiles.skills.skills.farming.subskills.CultivatingSubskill;
import com.server.profiles.skills.skills.farming.subskills.HarvestingSubskill;
import com.server.profiles.stats.PlayerStats;

/**
 * The Farming skill - focused on harvesting and cultivating crops
 */
public class FarmingSkill extends AbstractSkill {
    
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
    
    public FarmingSkill() {
        super(SkillType.FARMING.getId(), 
              SkillType.FARMING.getDisplayName(), 
              SkillType.FARMING.getDescription(), 
              50); // Max level of 50 for main skills
        
        // Initialize subskills
        initializeSubskills();
        
        // Initialize rewards
        initializeRewards();
    }
    
    private void initializeSubskills() {
        // Add subskills
        this.subskills.add(new HarvestingSubskill(this));
        this.subskills.add(new CultivatingSubskill(this));
        this.subskills.add(new BotanySubskill(this));
    }
    
    /**
     * Initialize the rewards for each level
     */
    private void initializeRewards() {
        // Level 5: Basic stat boost
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 0.1));
        level5Rewards.add(new CurrencyReward("coins", 100));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Item reward
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new ItemReward("iron_hoe", 1));
        level10Rewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 0.2));
        rewardsByLevel.put(10, level10Rewards);
        
        // Add more rewards for other milestone levels
        for (int level : MILESTONE_LEVELS) {
            if (level > 10) {
                List<SkillReward> levelRewards = new ArrayList<>();
                levelRewards.add(new StatReward(SkillRewardType.FARMING_FORTUNE, 0.1 * (level / 5)));
                levelRewards.add(new CurrencyReward("coins", 50 * level));
                rewardsByLevel.put(level, levelRewards);
            }
        }
    }
    
    /**
     * Handle skill tree node upgrades for the Farming skill
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Farming] Applying node upgrade: " + nodeId + " from " + oldLevel + " to " + newLevel);
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, "[Farming] No active profile for " + player.getName());
            }
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, "[Farming] No profile found for " + player.getName());
            }
            return;
        }
        
        PlayerStats stats = profile.getStats();
        
        switch (nodeId) {
            case "farming_fortune":
                // Apply farming fortune bonus (0.5 per level)
                double fortuneIncrease = (newLevel - oldLevel) * 0.5;
                double oldFortune = stats.getFarmingFortune();
                stats.increaseDefaultFarmingFortune(fortuneIncrease);
                double newFortune = stats.getFarmingFortune();
                
                player.sendMessage(ChatColor.GREEN + "Farming Fortune increased by " + 
                                ChatColor.GOLD + "+" + fortuneIncrease + 
                                ChatColor.GREEN + " (Total: " + ChatColor.GOLD + 
                                String.format("%.1f", newLevel * 0.5) + ChatColor.GREEN + ")");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Farming] Applied fortune increase: " + fortuneIncrease + 
                        " to " + player.getName() + " (old: " + oldFortune + ", new: " + newFortune + ")");
                }
                break;
                
            case "farming_xp_boost":
                // Apply farming XP boost (2% per level)
                double xpBoostIncrease = (newLevel - oldLevel) * 0.02;
                double totalXpBoost = newLevel * 0.02;
                double percentageBonus = totalXpBoost * 100;
                
                player.sendMessage(ChatColor.GREEN + "Farming XP Boost increased by " + 
                                ChatColor.LIGHT_PURPLE + "+" + String.format("%.0f", xpBoostIncrease * 100) + "%" +
                                ChatColor.GREEN + " (Total: " + ChatColor.LIGHT_PURPLE + 
                                String.format("%.0f", percentageBonus) + "%" + ChatColor.GREEN + ")");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Farming] Applied XP boost increase: " + xpBoostIncrease + 
                        " to " + player.getName());
                }
                break;

            case "unlock_cultivating":
                // Unlock cultivating subskill (requires Harvesting level 10)
                player.sendMessage(ChatColor.GREEN + "You have unlocked the " + 
                                ChatColor.GOLD + "Cultivating" + ChatColor.GREEN + " subskill!");
                player.sendMessage(ChatColor.YELLOW + "You can now gain XP from planting and growing crops.");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Farming] Unlocked cultivating for " + player.getName());
                }
                break;
            
            // Harvesting Crop Unlocks
            case "unlock_carrots":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Carrots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest carrots to gain Harvesting XP.");
                break;
                
            case "unlock_potatoes":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Potatoes" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest potatoes to gain Harvesting XP.");
                break;
                
            case "unlock_beetroots":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Beetroots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest beetroots to gain Harvesting XP.");
                break;
                
            case "unlock_sweet_berries":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Sweet Berries" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest sweet berries to gain Harvesting XP.");
                break;
                
            case "unlock_cocoa":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Cocoa Beans" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest cocoa beans to gain Harvesting XP.");
                break;
                
            case "unlock_nether_wart":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Nether Wart" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest nether wart to gain Harvesting XP.");
                break;
                
            case "unlock_melons":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Melons" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest melons to gain Harvesting XP.");
                break;
                
            case "unlock_pumpkins":
                player.sendMessage(ChatColor.GREEN + "You can now harvest " + ChatColor.GOLD + "Pumpkins" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Harvest pumpkins to gain Harvesting XP.");
                break;
            
            // Cultivating Planting Unlocks
            case "unlock_plant_carrots":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Carrots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant carrots to gain Cultivating XP.");
                break;
                
            case "unlock_plant_potatoes":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Potatoes" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant potatoes to gain Cultivating XP.");
                break;
                
            case "unlock_plant_beetroots":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Beetroots" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant beetroots to gain Cultivating XP.");
                break;
                
            case "unlock_plant_sweet_berries":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Sweet Berries" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant sweet berries to gain Cultivating XP.");
                break;
                
            case "unlock_plant_cocoa":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Cocoa Beans" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant cocoa beans to gain Cultivating XP.");
                break;
                
            case "unlock_plant_nether_wart":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Nether Wart" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant nether wart to gain Cultivating XP.");
                break;
                
            case "unlock_plant_melons":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Melons" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant melon seeds to gain Cultivating XP.");
                break;
                
            case "unlock_plant_pumpkins":
                player.sendMessage(ChatColor.GREEN + "You can now plant " + ChatColor.GOLD + "Pumpkins" + ChatColor.GREEN + "!");
                player.sendMessage(ChatColor.YELLOW + "Plant pumpkin seeds to gain Cultivating XP.");
                break;
                
            default:
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Farming] Unknown node ID: " + nodeId);
                }
                break;
        }
    }
    
    /**
     * Handle skill tree reset for the Farming skill
     */
    public void handleSkillTreeReset(Player player, Map<String, Integer> oldNodeLevels) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Farming] Handling skill tree reset for " + player.getName());
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        PlayerStats stats = profile.getStats();
        
        // Remove farming fortune bonuses
        if (oldNodeLevels.containsKey("farming_fortune")) {
            int fortuneLevel = oldNodeLevels.get("farming_fortune");
            double fortuneToRemove = fortuneLevel * 0.5;
            
            stats.increaseDefaultFarmingFortune(-fortuneToRemove);
            
            player.sendMessage(ChatColor.GRAY + "Farming Fortune bonus of " + 
                             ChatColor.GOLD + fortuneToRemove + 
                             ChatColor.GRAY + " has been removed");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Farming] Removed fortune bonus: " + fortuneToRemove + 
                    " from " + player.getName());
            }
        }
        
        // Remove farming XP boost bonuses
        if (oldNodeLevels.containsKey("farming_xp_boost")) {
            int xpBoostLevel = oldNodeLevels.get("farming_xp_boost");
            double xpBoostToRemove = xpBoostLevel * 0.02;
            
            double percentageRemoved = xpBoostToRemove * 100;
            player.sendMessage(ChatColor.GRAY + "Farming XP Boost bonus of " + 
                             ChatColor.LIGHT_PURPLE + String.format("%.0f", percentageRemoved) + "%" +
                             ChatColor.GRAY + " has been removed");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Farming] Removed XP boost bonus: " + xpBoostToRemove + 
                    " from " + player.getName());
            }
        }

        // Handle cultivating unlock reset
        if (oldNodeLevels.containsKey("unlock_cultivating")) {
            player.sendMessage(ChatColor.GRAY + "Cultivating subskill access has been " + 
                            ChatColor.RED + "LOCKED" + ChatColor.GRAY + 
                            ". You can no longer gain XP from planting crops.");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Farming] Locked cultivating for " + player.getName());
            }
        }
    }

    /**
     * Check if a player can harvest a specific crop
     * This delegates to the Harvesting subskill
     */
    public boolean canHarvestCrop(Player player, Material material) {
        for (Skill subskill : getSubskills()) {
            if (subskill instanceof HarvestingSubskill) {
                HarvestingSubskill harvesting = (HarvestingSubskill) subskill;
                if (harvesting.affectsCrop(material)) {
                    return harvesting.canHarvestCrop(player, material);
                }
            }
        }
        
        // Default to allowing non-crop materials
        return true;
    }
    
    /**
     * Check if a player can plant a specific crop
     * This delegates to the Cultivating subskill
     */
    public boolean canPlantCrop(Player player, Material material) {
        for (Skill subskill : getSubskills()) {
            if (subskill instanceof CultivatingSubskill) {
                CultivatingSubskill cultivating = (CultivatingSubskill) subskill;
                if (cultivating.affectsPlantable(material)) {
                    return cultivating.canPlantCrop(player, material);
                }
            }
        }
        
        // Default to allowing non-plantable materials
        return true;
    }
    
    @Override
    public boolean isMainSkill() {
        return true;
    }
    
    @Override
    public Skill getParentSkill() {
        return null; // This is a main skill
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
        return XP_REQUIREMENTS.getOrDefault(level, 0.0);
    }
    
    /**
     * Get the total farming fortune bonus from this skill (parent + subskills)
     * @param level The level of this main skill
     * @return A multiplier for farming fortune (1.0 = normal fortune)
     */
    public double getFarmingFortuneBonus(int level) {
        // Base bonus from main skill level
        double bonus = 1.0 + (level * 0.01); // 1% per level
        
        // Add subskill bonuses if needed
        // This can be extended later
        
        return bonus;
    }
}