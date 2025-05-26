package com.server.profiles.skills.skills.mining.subskills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.rewards.rewards.CurrencyReward;
import com.server.profiles.skills.rewards.rewards.StatReward;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.stats.PlayerStats;

/**
 * Gem Carving Subskill - Carefully extract fragile crystals and gems embedded in rocks
 * Mini-precision game "trace" the outline without shattering the gem; slow, methodical.
 */
public class GemCarvingSubskill extends AbstractSkill {
    
    private static final Map<Integer, Double> XP_REQUIREMENTS = new HashMap<>();
    private static final List<Integer> MILESTONE_LEVELS = Arrays.asList(
        5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    );
    
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

    // Store the bonus XP amounts for players from skill tree
    private final Map<UUID, Integer> bonusXpMap = new HashMap<>();
    
    // Store the mining fortune bonus from skill tree
    private final Map<UUID, Double> miningFortuneMap = new HashMap<>();
    
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
        // Level 5: Mining Fortune +0.10
        List<SkillReward> level5Rewards = new ArrayList<>();
        level5Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.10));
        rewardsByLevel.put(5, level5Rewards);
        
        // Level 10: Mining Fortune +0.10, Luck +1 (Milestone level)
        List<SkillReward> level10Rewards = new ArrayList<>();
        level10Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.10));
        level10Rewards.add(new StatReward(SkillRewardType.LUCK, 1));
        rewardsByLevel.put(10, level10Rewards);
        
        // Level 25: Currency Reward 500 (Milestone level)
        List<SkillReward> level25Rewards = new ArrayList<>();
        level25Rewards.add(new CurrencyReward(500));
        rewardsByLevel.put(25, level25Rewards);
        
        // Level 50: Mining Fortune +0.30, Luck +2 (Milestone level)
        List<SkillReward> level50Rewards = new ArrayList<>();
        level50Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.30));
        level50Rewards.add(new StatReward(SkillRewardType.LUCK, 2));
        rewardsByLevel.put(50, level50Rewards);
        
        // Level 75: Currency Reward 1500 (Milestone level)
        List<SkillReward> level75Rewards = new ArrayList<>();
        level75Rewards.add(new CurrencyReward(1500));
        rewardsByLevel.put(75, level75Rewards);
        
        // Level 100: Mining Fortune +0.50, Luck +5 (Milestone level, max level)
        List<SkillReward> level100Rewards = new ArrayList<>();
        level100Rewards.add(new StatReward(SkillRewardType.MINING_FORTUNE, 0.50));
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

    /**
     * Apply node upgrade benefits from the main Mining skill tree
     */
    public void applyNodeUpgrade(Player player, String nodeId, int oldLevel, int newLevel) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
            Main.getInstance().debugLog(DebugSystem.SKILLS, 
                "[GemCarving] Applying node upgrade: " + nodeId + " from " + oldLevel + " to " + newLevel);
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        PlayerStats stats = profile.getStats();
        
        switch (nodeId) {
            case "gem_mining_fortune":
                // Apply gem-specific mining fortune
                double fortuneIncrease = 0.5; // 0.5 per level
                stats.addMiningFortune(fortuneIncrease);
                
                // Track this for reset purposes
                UUID playerId = player.getUniqueId();
                miningFortuneMap.put(playerId, miningFortuneMap.getOrDefault(playerId, 0.0) + fortuneIncrease);
                
                player.sendMessage(ChatColor.GREEN + "Gem Mining Fortune increased by " + 
                                ChatColor.AQUA + "+" + fortuneIncrease);
                break;
                
            case "advanced_gem_cutting":
                player.sendMessage(ChatColor.GREEN + "Advanced gem cutting techniques unlocked!");
                // Additional gem carving bonuses can be implemented here
                break;
                
            case "gem_carving_mastery":
                player.sendMessage(ChatColor.GREEN + "Gem carving mastery unlocked! " +
                                "You can now access advanced gem carving features.");
                break;
        }
    }

    
    /**
     * Get the bonus XP for a player from the skill tree
     * @param player The player to check
     * @return The bonus XP amount (default 0)
     */
    public int getBonusXp(Player player) {
        return bonusXpMap.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Award XP for a successful gem extraction
     * @param player The player to award XP to
     * @param baseXp The base XP amount for the extraction
     * @param quality The quality of the extracted gem (0-100)
     */
    public void awardExtractionXp(Player player, double baseXp, int quality) {
        // Get player's benefits from skill tree - use the exact node level
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        int bonusXp = (int)Math.round(benefits.getOrDefault("gem_carving_xp", 0.0));
        
        // Calculate final XP with bonus and quality factor
        double qualityFactor = 0.5 + (quality / 200.0); // 0.5 to 1.0 based on quality
        double finalXp = baseXp + (bonusXp * qualityFactor);  // Make sure the full bonus is applied
        
        // Award the XP
        SkillProgressionManager.getInstance().addExperience(player, this, finalXp);
        
        // Show info message about the bonus XP if it's significant
        if (bonusXp > 0) {
            player.sendMessage(ChatColor.GRAY + "+" + String.format("%.1f", bonusXp * qualityFactor) + 
                            " bonus XP from Carver's Expertise");
        }
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Awarded " + finalXp + " GemCarving XP to " + player.getName() + 
                " (Base: " + baseXp + ", Bonus: " + bonusXp + ", Quality factor: " + qualityFactor + ")");
        }
    }
    
    /**
     * Load saved player data
     * This method is called when a player's profile is loaded
     * Used to restore bonusXp and mining fortune values from the skill tree
     */
    public void loadPlayerData(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the player's skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) return;
        
        // Load the bonus XP from the skill tree
        int gemcarvingXpBoostLevel = treeData.getNodeLevel(getId(), "gemcarving_xp_boost");
        
        if (gemcarvingXpBoostLevel > 0) {
            bonusXpMap.put(player.getUniqueId(), gemcarvingXpBoostLevel);
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Loaded GemCarving XP bonus of " + gemcarvingXpBoostLevel + " for " + player.getName());
            }
        }
        
        // Load mining fortune from skill tree - calling getSkillTreeBenefits will apply it
        Map<String, Double> benefits = getSkillTreeBenefits(player);
        double fortune = benefits.getOrDefault("mining_fortune", 0.0);
        
        if (fortune > 0 && Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Loaded and applied " + fortune + " mining fortune for " + 
                player.getName() + " from GemCarving skill tree");
        }
    }

    /**
     * Check if a player has access to a specific crystal type
     * @param player The player to check
     * @param crystalType The crystal type to check access for
     * @return true if the player can carve this crystal, false otherwise
     */
    public boolean hasCrystalAccess(Player player, String crystalType) {
        // Mooncrystal is always accessible
        if (crystalType.equalsIgnoreCase("mooncrystal")) {
            return true;
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Get the player's skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) return false;
        
        // Check for specific crystal types based on unlocked nodes
        if (crystalType.equalsIgnoreCase("azuralite")) {
            return treeData.getNodeLevel(getId(), "basic_crystals") > 0;
        }
        
        // Future crystal types can be added here with their corresponding nodes
        
        // Unknown crystal type - default to false
        return false;
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
        benefits.put("gem_carving_xp", 0.0);
        benefits.put("basic_crystals_unlocked", 0.0);
        
        // Get player's skill tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return benefits;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return benefits;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(this.getId());
        PlayerStats stats = profile.getStats();
        
        // Apply benefits from nodes
        
        // GemCarving XP Boost node - Each level gives +1 XP (FULL AMOUNT)
        if (nodeLevels.containsKey("gemcarving_xp_boost")) {
            int level = nodeLevels.get("gemcarving_xp_boost");
            
            // IMPORTANT: Store the exact node level as a double with no scaling
            benefits.put("gem_carving_xp", (double)level); // +1 XP per level (exact value)
            
            // Update the bonusXpMap for tracking
            bonusXpMap.put(player.getUniqueId(), level);
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Applied GemCarving XP boost of " + level + " XP for " + player.getName());
            }
        }
        
        // Mining Fortune node - Add 0.5 mining fortune per level
        if (nodeLevels.containsKey("gem_mining_fortune")) {
            int level = nodeLevels.get("gem_mining_fortune");
            double fortune = level * 0.5;
            benefits.put("mining_fortune", fortune);
            
            // Store the current mining fortune to calculate the difference
            double oldFortune = stats.getMiningFortune();
            
            // Get previously applied mining fortune for this node to avoid double-applying
            double previouslyApplied = miningFortuneMap.getOrDefault(player.getUniqueId(), 0.0);
            
            // If there's a change in the mining fortune value, update it
            if (fortune != previouslyApplied) {
                // First remove the old value if it exists
                if (previouslyApplied > 0) {
                    stats.addMiningFortune(-previouslyApplied);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("Removed previous " + previouslyApplied + " mining fortune from " + 
                            player.getName());
                    }
                }
                
                // Now add the new value and update our tracking
                if (fortune > 0) {
                    stats.addMiningFortune(fortune);
                    miningFortuneMap.put(player.getUniqueId(), fortune);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("Added " + fortune + " mining fortune to " + player.getName() + 
                            " (now " + stats.getMiningFortune() + ")");
                    }
                }
            }
        }
        
        // Basic Crystals node - Unlocks Azuralite crystals
        if (nodeLevels.containsKey("basic_crystals")) {
            int level = nodeLevels.get("basic_crystals");
            if (level > 0) {
                benefits.put("basic_crystals_unlocked", 1.0);
                
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Applied basic_crystals unlock for " + player.getName());
                }
            }
        }
        
        return benefits;
    }

    /**
     * Update cached skill tree benefits for a player
     */
    private void updateCachedSkillTreeBenefits(Player player) {
        // Calling getSkillTreeBenefits will apply all the benefits
        getSkillTreeBenefits(player);
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Updated cached skill tree benefits for " + player.getName());
        }
    }
        
    /**
     * Clean up player data when they leave
     * This method is called when a player logs out
     */
    public void cleanupPlayerData(Player player) {
        // Before removing the tracking data, make sure we don't leave any
        // permanent stats applied
        PlayerProfile profile = null;
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        
        if (activeSlot != null) {
            profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        }
        
        // Clean up mining fortune - it will be re-applied when they log back in
        if (profile != null) {
            double appliedFortune = miningFortuneMap.getOrDefault(player.getUniqueId(), 0.0);
            if (appliedFortune > 0) {
                profile.getStats().addMiningFortune(-appliedFortune);
                
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Removed " + appliedFortune + 
                        " mining fortune from " + player.getName() + " on logout");
                }
            }
        }
        
        // Remove from tracking maps
        bonusXpMap.remove(player.getUniqueId());
        miningFortuneMap.remove(player.getUniqueId());
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Cleaned up GemCarving player data for " + player.getName());
        }
    }

    /**
     * Handle skill tree reset for a player
     * This should be called when a player resets their skill tree
     * 
     * @param player The player who reset their skill tree
     * @param oldNodeLevels The node levels before the reset
     */
    public void handleSkillTreeReset(Player player, Map<String, Integer> oldNodeLevels) {
        Main.getInstance().getLogger().info("[GemCarvingReset] Starting reset process for player: " + player.getName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            Main.getInstance().getLogger().warning("[GemCarvingReset] Failed: activeSlot is null for " + player.getName());
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) {
            Main.getInstance().getLogger().warning("[GemCarvingReset] Failed: profile is null for " + player.getName());
            return;
        }
        
        PlayerStats stats = profile.getStats();
        
        // Log all node levels for debugging
        Main.getInstance().getLogger().info("[GemCarvingReset] Node levels before reset: " + oldNodeLevels);
        
        // Check if gem_mining_fortune node was previously unlocked
        if (oldNodeLevels.containsKey("gem_mining_fortune")) {
            int previousLevel = oldNodeLevels.get("gem_mining_fortune");
            Main.getInstance().getLogger().info("[GemCarvingReset] Found gem_mining_fortune level: " + previousLevel);
            
            if (previousLevel <= 0) {
                Main.getInstance().getLogger().info("[GemCarvingReset] Mining fortune level is 0 or negative, nothing to remove");
                return;
            }
            
            // Each level gives +0.5 mining fortune
            double fortuneToRemove = previousLevel * 0.5;
            
            // Get previously applied mining fortune from our tracking map
            double previouslyApplied = miningFortuneMap.getOrDefault(player.getUniqueId(), 0.0);
            
            Main.getInstance().getLogger().info("[GemCarvingReset] Mining fortune to remove: " + fortuneToRemove + 
                ", previously applied: " + previouslyApplied);
            
            // Reset tracking map first - very important
            miningFortuneMap.put(player.getUniqueId(), 0.0);
            
            try {
                // Use reflection to directly access and modify the field values
                java.lang.reflect.Field defaultField = PlayerStats.class.getDeclaredField("defaultMiningFortune");
                defaultField.setAccessible(true);
                double currentDefault = (double) defaultField.get(stats);
                defaultField.set(stats, currentDefault - fortuneToRemove);
                
                java.lang.reflect.Field currentField = PlayerStats.class.getDeclaredField("miningFortune");
                currentField.setAccessible(true);
                double current = (double) currentField.get(stats);
                currentField.set(stats, current - fortuneToRemove);
                
                // Verify the change
                double newDefaultFortune = stats.getDefaultMiningFortune();
                double newCurrentFortune = stats.getMiningFortune();
                
                Main.getInstance().getLogger().info("[GemCarvingReset] After modification: default=" + 
                    newDefaultFortune + ", current=" + newCurrentFortune);
                    
                // Inform the player
                player.sendMessage(ChatColor.GRAY + "Mining Fortune bonus of " + 
                    ChatColor.RED + String.format("%.1f", fortuneToRemove) + 
                    ChatColor.GRAY + " has been removed due to skill tree reset.");
                
                Main.getInstance().getLogger().info("[GemCarvingReset] Final mining fortune values: default=" + 
                    stats.getDefaultMiningFortune() + ", current=" + stats.getMiningFortune());
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("[GemCarvingReset] Error using reflection: " + e.getMessage());
                e.printStackTrace();
                
                // Fallback to removeExactMiningFortune method
                stats.addMiningFortune(-fortuneToRemove);
                Main.getInstance().getLogger().info("[GemCarvingReset] Used fallback method, final values: default=" + 
                    stats.getDefaultMiningFortune() + ", current=" + stats.getMiningFortune());
            }
        } else {
            Main.getInstance().getLogger().info("[GemCarvingReset] No gem_mining_fortune node found in oldNodeLevels for " + player.getName());
        }
    }


}