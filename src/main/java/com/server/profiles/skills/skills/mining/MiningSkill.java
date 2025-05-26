package com.server.profiles.skills.skills.mining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
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
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.stats.PlayerStats;

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
        // Add subskills
        this.subskills.add(new OreExtractionSubskill(this));
        this.subskills.add(new GemCarvingSubskill(this));
    }
    
    /**
     * Initialize the rewards for each level
     */
    private void initializeRewards() {
        // Level 5: Basic stat boost
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.1));
        level5Rewards.add(new CurrencyReward("coins", 100));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Item reward
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new ItemReward("iron_pickaxe", 1));
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.2));
        rewardsByLevel.put(10, level10Rewards);
        
        // Add more rewards for other milestone levels
        for (int level : MILESTONE_LEVELS) {
            if (level > 10) {
                List<SkillReward> rewards = new ArrayList<>();
                rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.1 * (level / 5)));
                rewards.add(new CurrencyReward("coins", 50 * level));
                rewardsByLevel.put(level, rewards);
            }
        }
    }
    
    /**
     * Handle skill tree node upgrades for the Mining skill
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Mining] Applying node upgrade: " + nodeId + " from " + oldLevel + " to " + newLevel);
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, "[Mining] No active profile for " + player.getName());
            }
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, "[Mining] No profile found for " + player.getName());
            }
            return;
        }
        
        PlayerStats stats = profile.getStats();
        
        switch (nodeId) {
            case "mining_fortune":
                // Apply mining fortune bonus (0.5 per level)
                double fortuneIncrease = (newLevel - oldLevel) * 0.5;
                double oldFortune = stats.getMiningFortune();
                stats.increaseDefaultMiningFortune(fortuneIncrease);
                double newFortune = stats.getMiningFortune();
                
                player.sendMessage(ChatColor.GREEN + "Mining Fortune increased by " + 
                                ChatColor.GOLD + "+" + fortuneIncrease + 
                                ChatColor.GREEN + " (Total: " + ChatColor.GOLD + 
                                String.format("%.1f", newLevel * 0.5) + ChatColor.GREEN + ")");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Mining] Applied fortune increase: " + fortuneIncrease + 
                        " to " + player.getName() + " (old: " + oldFortune + ", new: " + newFortune + ")");
                }
                break;
                
            case "mining_speed":
                // Apply mining speed bonus (0.01 per level)
                double speedIncrease = (newLevel - oldLevel) * 0.01;
                double oldSpeed = stats.getMiningSpeed();
                stats.increaseDefaultMiningSpeed(speedIncrease);
                double newSpeed = stats.getMiningSpeed();
                
                double totalSpeedBonus = newLevel * 0.01;
                double percentageBonus = totalSpeedBonus * 100;
                
                player.sendMessage(ChatColor.GREEN + "Mining Speed increased by " + 
                                ChatColor.AQUA + "+" + String.format("%.0f", speedIncrease * 100) + "%" +
                                ChatColor.GREEN + " (Total: " + ChatColor.AQUA + 
                                String.format("%.0f", percentageBonus) + "%" + ChatColor.GREEN + ")");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Mining] Applied speed increase: " + speedIncrease + 
                        " to " + player.getName() + " (old: " + oldSpeed + ", new: " + newSpeed + ")");
                }
                break;

            case "mining_armor":
                // Apply mining armor bonus (+3 default armor)
                int armorIncrease = (newLevel - oldLevel) * 3;
                int oldArmor = stats.getArmor();
                stats.increaseDefaultArmor(armorIncrease);
                int newArmor = stats.getArmor();
                
                player.sendMessage(ChatColor.GREEN + "Mining Armor increased by " + 
                                ChatColor.GRAY + "+" + armorIncrease + 
                                ChatColor.GREEN + " (Total: " + ChatColor.GRAY + 
                                newLevel * 3 + ChatColor.GREEN + ")");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Mining] Applied armor increase: " + armorIncrease + 
                        " to " + player.getName() + " (old: " + oldArmor + ", new: " + newArmor + ")");
                }
                break;
                
            default:
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    Main.getInstance().debugLog(DebugSystem.SKILLS, 
                        "[Mining] Unknown node ID: " + nodeId);
                }
                break;
        }
    }
    
    /**
     * Handle skill tree reset for the Mining skill
     */
    public void handleSkillTreeReset(Player player, Map<String, Integer> oldNodeLevels) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[Mining] Handling skill tree reset for " + player.getName());
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        PlayerStats stats = profile.getStats();
        
        // Remove mining fortune bonuses
        if (oldNodeLevels.containsKey("mining_fortune")) {
            int fortuneLevel = oldNodeLevels.get("mining_fortune");
            double fortuneToRemove = fortuneLevel * 0.5;
            
            stats.increaseDefaultMiningFortune(-fortuneToRemove);
            
            player.sendMessage(ChatColor.GRAY + "Mining Fortune bonus of " + 
                             ChatColor.GOLD + fortuneToRemove + 
                             ChatColor.GRAY + " has been removed");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Mining] Removed fortune bonus: " + fortuneToRemove + 
                    " from " + player.getName());
            }
        }
        
        // Remove mining speed bonuses
        if (oldNodeLevels.containsKey("mining_speed")) {
            int speedLevel = oldNodeLevels.get("mining_speed");
            double speedToRemove = speedLevel * 0.01;
            
            stats.increaseDefaultMiningSpeed(-speedToRemove);
            
            double percentageRemoved = speedToRemove * 100;
            player.sendMessage(ChatColor.GRAY + "Mining Speed bonus of " + 
                             ChatColor.AQUA + String.format("%.0f", percentageRemoved) + "%" +
                             ChatColor.GRAY + " has been removed");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Mining] Removed speed bonus: " + speedToRemove + 
                    " from " + player.getName());
            }
        }

        // Remove mining armor bonuses
        if (oldNodeLevels.containsKey("mining_armor")) {
            int armorLevel = oldNodeLevels.get("mining_armor");
            int armorToRemove = armorLevel * 3;
            
            stats.increaseDefaultArmor(-armorToRemove);
            
            player.sendMessage(ChatColor.GRAY + "Mining Armor bonus of " + 
                            ChatColor.GRAY + armorToRemove + 
                            ChatColor.GRAY + " has been removed");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                Main.getInstance().debugLog(DebugSystem.SKILLS, 
                    "[Mining] Removed armor bonus: " + armorToRemove + 
                    " from " + player.getName());
            }
        }
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
     * Get the total mining fortune bonus from this skill (parent + subskills)
     * @param level The level of this main skill
     * @return A multiplier for mining fortune (1.0 = normal fortune)
     */
    public double getMiningFortuneBonus(int level) {
        // Base bonus from main skill level
        double bonus = 1.0 + (level * 0.01); // 1% per level
        
        // Add subskill bonuses if needed
        // This can be extended later
        
        return bonus;
    }
}