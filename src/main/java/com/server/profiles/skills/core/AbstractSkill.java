package com.server.profiles.skills.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.PlayerSkillData;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.events.SkillExpGainEvent;
import com.server.profiles.skills.events.SkillLevelUpEvent;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;

/**
 * Abstract base implementation of the Skill interface with common functionality
 */
public abstract class AbstractSkill implements Skill {
    protected final String id;
    protected final String displayName;
    protected final String description;
    protected final int maxLevel;
    protected final boolean isMainSkill;
    protected final Skill parentSkill;
    protected final List<Skill> subskills;
    protected final List<Integer> milestones;
    protected final Map<Integer, Double> xpRequirements;
    protected final Map<Integer, List<SkillReward>> rewards;
    
    /**
     * Constructor for a main skill
     */
    public AbstractSkill(String id, String displayName, String description, int maxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
        this.isMainSkill = true;
        this.parentSkill = null;
        this.subskills = new ArrayList<>();
        this.milestones = new ArrayList<>();
        this.xpRequirements = initializeXpRequirements();
        this.rewards = new HashMap<>();
    }
    
    /**
     * Constructor for a subskill
     */
    public AbstractSkill(String id, String displayName, String description, int maxLevel, Skill parentSkill) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
        this.isMainSkill = false;
        this.parentSkill = parentSkill;
        this.subskills = null;
        this.milestones = initializeMilestones();
        this.xpRequirements = initializeXpRequirements();
        this.rewards = new HashMap<>();
    }
    
    /**
     * Initialize XP requirements for each level
     * Default implementation uses a power curve formula: 100 * level^1.5
     */
    protected Map<Integer, Double> initializeXpRequirements() {
        Map<Integer, Double> requirements = new HashMap<>();
        for (int level = 1; level <= maxLevel; level++) {
            requirements.put(level, 100 * Math.pow(level, 1.5));
        }
        return requirements;
    }
    
    /**
     * Initialize milestone levels
     * Default implementation creates milestones at levels 5, 10, 15, 20, etc.
     */
    protected List<Integer> initializeMilestones() {
        List<Integer> milestoneList = new ArrayList<>();
        for (int level = 5; level <= maxLevel; level += 5) {
            milestoneList.add(level);
        }
        return milestoneList;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public int getMaxLevel() {
        return maxLevel;
    }
    
    @Override
    public boolean isMainSkill() {
        return isMainSkill;
    }
    
    @Override
    public Skill getParentSkill() {
        return parentSkill;
    }
    
    @Override
    public List<Skill> getSubskills() {
        return isMainSkill ? new ArrayList<>(subskills) : new ArrayList<>();
    }
    
    @Override
    public List<SkillReward> getRewardsForLevel(int level) {
        return rewards.getOrDefault(level, new ArrayList<>());
    }
    
    @Override
    public SkillLevel getSkillLevel(Player player) {
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return new SkillLevel(1, 0, 0); // Default level
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return new SkillLevel(1, 0, 0);
        
        PlayerSkillData skillData = SkillRegistry.getInstance().getPlayerSkillData(profile);
        return skillData.getSkillLevel(this);
    }
    
    @Override
    public boolean addExperience(Player player, double amount) {
        SkillExpGainEvent expEvent = new SkillExpGainEvent(player, this, amount);
        Main.getInstance().getServer().getPluginManager().callEvent(expEvent);
        
        if (expEvent.isCancelled()) {
            return false;
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Get skill data
        PlayerSkillData skillData = profile.getSkillData();
        SkillLevel currentLevel = skillData.getSkillLevel(this);
        
        // Stop if already max level
        if (currentLevel.getLevel() >= maxLevel) {
            return false;
        }
        
        // Get required XP for next level
        double requiredXp = getXpForLevel(currentLevel.getLevel() + 1);
        
        // Add XP
        double newTotalXp = currentLevel.getTotalXp() + expEvent.getAmount();
        double newCurrentXp = currentLevel.getCurrentXp() + expEvent.getAmount();
        int newLevel = currentLevel.getLevel();
        boolean leveledUp = false;
        
        // Check for level up
        if (newCurrentXp >= requiredXp) {
            newLevel++;
            newCurrentXp -= requiredXp;
            leveledUp = true;
            
            // Call level up event with the NEW level
            SkillLevelUpEvent levelUpEvent = new SkillLevelUpEvent(player, this, newLevel);
            Main.getInstance().getServer().getPluginManager().callEvent(levelUpEvent);
            
            // Apply rewards
            List<SkillReward> levelRewards = getRewardsForLevel(newLevel);
            for (SkillReward reward : levelRewards) {
                reward.grantTo(player);
            }
            
            // Handle milestone
            if (hasMilestoneAt(newLevel) && parentSkill != null) {
                // This is a subskill reaching a milestone level
                // Give bonus XP to parent skill
                double bonusXp = newLevel * 100.0; // Simple formula: milestone level * 100
                
                // Add XP to parent skill
                parentSkill.addExperience(player, bonusXp);
                
                // Notify the player
                player.sendMessage(
                    ChatColor.GREEN + "✦ " + ChatColor.GOLD + "MILESTONE REACHED" + 
                    ChatColor.GREEN + " ✦ " + ChatColor.YELLOW + getDisplayName() + 
                    ChatColor.GREEN + " has reached milestone level " + ChatColor.YELLOW + newLevel + 
                    ChatColor.GREEN + " and granted " + ChatColor.YELLOW + bonusXp + 
                    ChatColor.GREEN + " bonus XP to " + ChatColor.YELLOW + parentSkill.getDisplayName()
                );
            }
        }
        
        // Update skill data
        double progressToNext = 0;
        if (newLevel < maxLevel) {
            requiredXp = getXpForLevel(newLevel + 1);
            progressToNext = newCurrentXp / requiredXp;
        }
        
        // Create new SkillLevel with correct parameter order
        SkillLevel newSkillLevel = new SkillLevel(newLevel, newCurrentXp, newTotalXp);
        skillData.setSkillLevel(this, newSkillLevel);
        
        return leveledUp;
    }
    
    @Override
    public boolean hasMilestoneAt(int level) {
        return milestones.contains(level);
    }
    
    @Override
    public List<Integer> getMilestones() {
        return milestones;
    }
    
    @Override
    public Map<Integer, Double> getXpRequirements() {
        return xpRequirements;
    }
    
    @Override
    public double getXpForLevel(int level) {
        if (level <= 0 || level > maxLevel) return 0;
        return xpRequirements.getOrDefault(level, 100.0 * Math.pow(level, 1.5));
    }

    private void initializeSubskills() {
        // Create the subskills
        OreExtractionSubskill oreExtraction = new OreExtractionSubskill(this);
        GemCarvingSubskill gemCarving = new GemCarvingSubskill(this);
        
        // Add them to the AbstractSkill's subskills list using the addSubskill method
        addSubskill(oreExtraction);
        addSubskill(gemCarving);
        
        // CRITICAL FIX: Explicitly register subskills with the SkillRegistry
        SkillRegistry.getInstance().registerSkill(oreExtraction);
        SkillRegistry.getInstance().registerSkill(gemCarving);
        
        // Future subskills
        // addSubskill(new OreEfficiencySubskill(this));
        // addSubskill(new RareFindsSubskill(this));
        // addSubskill(new StoneBreakerSubskill(this));
    }
        
    /**
     * Add a subskill to this skill
     * Only works if this is a main skill
     */
    public void addSubskill(Skill subskill) {
        if (isMainSkill && subskill != null) {
            // Don't add if it's already in the list
            if (!subskills.contains(subskill)) {
                subskills.add(subskill);
            }
        }
    }
    
    /**
     * Add a reward for reaching a specific level
     */
    public void addReward(int level, SkillReward reward) {
        if (level <= 0 || level > maxLevel || reward == null) return;
        
        if (!rewards.containsKey(level)) {
            rewards.put(level, new ArrayList<>());
        }
        
        rewards.get(level).add(reward);
    }
}