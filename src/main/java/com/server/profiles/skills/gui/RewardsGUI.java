package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.skills.tokens.SkillToken;

/**
 * GUI for displaying all rewards for a skill
 */
public class RewardsGUI {

    private static final String GUI_TITLE_PREFIX = "Rewards: ";
    private static final Map<SkillRewardType, Material> REWARD_TYPE_ICONS = new HashMap<>();
    private static final Map<SkillRewardType, ChatColor> REWARD_TYPE_COLORS = new HashMap<>();
    
    static {
        // Material icons for reward types
        REWARD_TYPE_ICONS.put(SkillRewardType.STAT_BOOST, Material.DIAMOND);
        REWARD_TYPE_ICONS.put(SkillRewardType.ITEM, Material.CHEST);
        REWARD_TYPE_ICONS.put(SkillRewardType.CURRENCY, Material.GOLD_INGOT);
        REWARD_TYPE_ICONS.put(SkillRewardType.UNLOCK, Material.NETHER_STAR);
        REWARD_TYPE_ICONS.put(SkillRewardType.PERK, Material.ENCHANTED_BOOK);
        
        // Colors for each reward type
        REWARD_TYPE_COLORS.put(SkillRewardType.STAT_BOOST, ChatColor.AQUA);
        REWARD_TYPE_COLORS.put(SkillRewardType.ITEM, ChatColor.GOLD);
        REWARD_TYPE_COLORS.put(SkillRewardType.CURRENCY, ChatColor.YELLOW);
        REWARD_TYPE_COLORS.put(SkillRewardType.UNLOCK, ChatColor.LIGHT_PURPLE);
        REWARD_TYPE_COLORS.put(SkillRewardType.PERK, ChatColor.GREEN);
    }
    
    /**
     * Open the rewards menu for a player
     */
    public static void openRewardsMenu(Player player, Skill skill) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill level
        SkillLevel level = profile.getSkillData().getSkillLevel(skill);
        
        // Create decorative border
        createBorder(gui);
        
        // Create skill info item in the center top
        ItemStack skillInfoItem = createSkillInfoItem(skill, level);
        gui.setItem(4, skillInfoItem);
        
        // Map of levels with rewards - use TreeMap to sort by level
        Map<Integer, List<SkillReward>> rewardsMap = new TreeMap<>();
        Map<Integer, List<SkillReward>> milestoneMap = new TreeMap<>();
        
        // Keep track of stats to show summary
        Map<String, Double> totalStatBoosts = new HashMap<>();
        int totalUnlocks = 0;
        int totalItems = 0;
        int totalCurrency = 0;
        int totalPerks = 0;
        
        // Determine the milestone cap based on whether this is a main skill or subskill
        int milestoneCap = skill.isMainSkill() ? 50 : 100;
        
        // Find all levels with rewards and create placeholders for milestones
        for (int i = 1; i <= skill.getMaxLevel(); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            boolean isRealReward = !rewards.isEmpty();
            
            // Check if this is a milestone level (every 5 levels from 5 to milestone cap)
            boolean isMilestone = i % 5 == 0 && i >= 5 && i <= milestoneCap;
            
            if (isMilestone) {
                // Even if no rewards are defined yet, we still want to display the milestone
                // If no rewards exist, use an empty list
                if (!isRealReward) {
                    rewards = new ArrayList<>();
                }
                milestoneMap.put(i, rewards);
                
                // If there are actual rewards, count them for the summary
                if (isRealReward) {
                    countRewards(rewards, totalStatBoosts, totalUnlocks, totalItems, totalCurrency, totalPerks);
                }
            } else if (isRealReward) {
                // Only add non-milestone levels that have actual rewards
                rewardsMap.put(i, rewards);
                countRewards(rewards, totalStatBoosts, totalUnlocks, totalItems, totalCurrency, totalPerks);
            }
        }
        
        // Define slot layouts with enhanced distribution
        int[] milestoneSlots;
        
        if (skill.isMainSkill()) {
            // Main skills have fewer milestones (up to 50), use 2 rows of milestones
            milestoneSlots = new int[] {
                10, 11, 12, 13, 14, 15, 16,     // First row (5, 10, 15, 20, 25, 30, 35)
                19, 20, 21, 22, 23, 24, 25      // Second row (40, 45, 50)
            };
        } else {
            // Subskills have more milestones (up to 100), use all 4 rows
            milestoneSlots = new int[] {
                10, 11, 12, 13, 14, 15, 16,     // First row (5, 10, 15, 20, 25, 30, 35)
                19, 20, 21, 22, 23, 24, 25,     // Second row (40, 45, 50, 55, 60, 65, 70)
                28, 29, 30, 31, 32, 33, 34,     // Third row (75, 80, 85, 90, 95, 100)
                37, 38, 39, 40, 41, 42, 43      // Fourth row for regular rewards
            };
        }
        
        // Create an array of milestone levels we expect
        List<Integer> milestoneExpectedLevels = new ArrayList<>();
        for (int i = 5; i <= milestoneCap; i += 5) {
            milestoneExpectedLevels.add(i);
        }
        
        // Add milestone items in order
        int milestoneIndex = 0;
        for (int milestoneLevel : milestoneExpectedLevels) {
            if (milestoneIndex >= milestoneSlots.length) break;
            
            // Get rewards if they exist, otherwise use empty list
            List<SkillReward> rewards = milestoneMap.getOrDefault(milestoneLevel, new ArrayList<>());
            
            // Create milestone reward item (even for empty milestones)
            ItemStack rewardItem = createRewardItem(milestoneLevel, rewards, level.getLevel() >= milestoneLevel);
            gui.setItem(milestoneSlots[milestoneIndex], rewardItem);
            
            milestoneIndex++;
        }
        
        // Add regular reward items - put them in remaining milestone slots or bottom row
        int regularIndex = milestoneIndex;  // Start where milestones ended
        for (Map.Entry<Integer, List<SkillReward>> entry : rewardsMap.entrySet()) {
            if (regularIndex >= milestoneSlots.length) break;
            
            int rewardLevel = entry.getKey();
            List<SkillReward> rewards = entry.getValue();
            
            // Create reward item
            ItemStack rewardItem = createRewardItem(rewardLevel, rewards, level.getLevel() >= rewardLevel);
            gui.setItem(milestoneSlots[regularIndex], rewardItem);
            
            regularIndex++;
        }
        
        // Add milestone info item with updated text for subskills
        ItemStack milestoneInfoItem = createMilestoneInfoItem(skill.isMainSkill());
        gui.setItem(3, milestoneInfoItem);
        
        // Add explanation item
        ItemStack explanationItem = createExplanationItem();
        gui.setItem(5, explanationItem);
        
        // Add back button with enhanced style
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to Skill Details");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to skill details screen");
        backMeta.setLore(backLore);
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);
        
        // Create summary item and place it in the bottom right corner (slot 53)
        ItemStack summaryItem = createSummaryItem(totalStatBoosts, totalUnlocks, totalItems, totalCurrency, totalPerks);
        gui.setItem(53, summaryItem);
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Count reward types for the summary display
     */
    private static void countRewards(List<SkillReward> rewards, 
                                Map<String, Double> totalStatBoosts,
                                int totalUnlocks, int totalItems,
                                int totalCurrency, int totalPerks) {
        for (SkillReward reward : rewards) {
            switch (reward.getType()) {
                case STAT_BOOST:
                    String statName = reward.getDescription();
                    if (statName.contains("+")) {
                        String[] parts = statName.split("\\+");
                        if (parts.length > 1) {
                            try {
                                String valuePart = parts[1].trim().split(" ")[0];
                                double value = Double.parseDouble(valuePart);
                                String stat = parts[1].substring(valuePart.length()).trim();
                                totalStatBoosts.put(stat, totalStatBoosts.getOrDefault(stat, 0.0) + value);
                            } catch (Exception e) {
                                // Just skip if parsing fails
                            }
                        }
                    }
                    break;
                case UNLOCK:
                    totalUnlocks++;
                    break;
                case ITEM:
                    totalItems++;
                    break;
                case CURRENCY:
                    totalCurrency++;
                    break;
                case PERK:
                    totalPerks++;
                    break;
            }
        }
    }

    /**
     * Create milestone rewards display item
     */
    private static ItemStack createMilestoneRewardsItem(Skill skill, int level, boolean earned, 
                                                    PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        
        if (earned) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        meta.setDisplayName((earned ? ChatColor.GREEN : ChatColor.YELLOW) + 
                        "✦ Level " + level + " Milestone Rewards");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (earned) {
            lore.add(ChatColor.GREEN + "✓ Milestone Completed!");
        } else {
            lore.add(ChatColor.YELLOW + "Milestone not yet reached");
        }
        
        lore.add("");
        lore.add(ChatColor.GOLD + "» Milestone Rewards:");
        
        // Show direct rewards from the skill
        List<SkillReward> rewards = skill.getRewardsForLevel(level);
        if (rewards != null && !rewards.isEmpty()) {
            for (SkillReward reward : rewards) {
                lore.add(ChatColor.WHITE + "• " + reward.getDescription());
            }
        } else {
            lore.add(ChatColor.GRAY + "• No direct stat rewards");
        }
        
        // Show token rewards - always reference the appropriate skill
        Map<SkillToken.TokenTier, Integer> tokenRewards = calculateTokensForLevel(level);
        int totalTokens = 0;
        for (Integer count : tokenRewards.values()) {
            totalTokens += count;
        }
        
        if (totalTokens > 0) {
            // Determine which skill receives the tokens
            Skill tokenRecipient = skill.isMainSkill() ? skill : skill.getParentSkill();
            
            lore.add(ChatColor.WHITE + "• " + ChatColor.YELLOW + "Skill Tokens:");
            
            // Add entry for each token tier
            for (Map.Entry<SkillToken.TokenTier, Integer> entry : tokenRewards.entrySet()) {
                SkillToken.TokenTier tier = entry.getKey();
                int count = entry.getValue();
                if (count > 0) {
                    lore.add("  " + tier.getColor() + tier.getSymbol() + " " + count + " " + 
                            tier.getDisplayName() + ChatColor.WHITE + " Token" + (count > 1 ? "s" : ""));
                }
            }
            
            if (!skill.isMainSkill()) {
                // For subskills, clarify that tokens go to parent
                lore.add(ChatColor.GRAY + "  (for " + tokenRecipient.getDisplayName() + " tree)");
            }
        }
        
        // Add current token count if milestone is earned
        if (earned) {
            Skill tokenRecipient = skill.isMainSkill() ? skill : skill.getParentSkill();
            Map<SkillToken.TokenTier, Integer> currentTokens = profile.getSkillTreeData().getAllTokenCounts(tokenRecipient.getId());
            SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(tokenRecipient);
            
            lore.add("");
            lore.add(ChatColor.AQUA + "Current " + tokenInfo.displayName + " Tokens:");
            
            boolean hasTokens = false;
            for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
                int count = currentTokens.getOrDefault(tier, 0);
                if (count > 0) {
                    hasTokens = true;
                    lore.add("  " + tier.getColor() + tier.getSymbol() + " " + count + " " + 
                            tier.getDisplayName() + ChatColor.WHITE + " Token" + (count > 1 ? "s" : ""));
                }
            }
            
            if (!hasTokens) {
                lore.add(ChatColor.GRAY + "  No tokens available");
                lore.add(ChatColor.YELLOW + "  Reach milestones to earn tokens!");
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item with skill information
     */
    private static ItemStack createSkillInfoItem(Skill skill, SkillLevel level) {
        Material icon;
        
        // Choose appropriate icon based on skill type
        switch (skill.getId()) {
            case "mining":
                icon = Material.DIAMOND_PICKAXE;
                break;
            case "excavating":
                icon = Material.DIAMOND_SHOVEL;
                break;
            case "fishing":
                icon = Material.FISHING_ROD;
                break;
            case "farming":
                icon = Material.DIAMOND_HOE;
                break;
            case "combat":
                icon = Material.DIAMOND_SWORD;
                break;
            default:
                icon = Material.NETHER_STAR;
                break;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Add enchant glow
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Set display name with fancy formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ " + skill.getDisplayName() + " " + 
                ChatColor.YELLOW + "[Level " + level.getLevel() + "/" + skill.getMaxLevel() + "]");
        
        // Create lore with divider for better readability
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Format description with improved line breaks
        for (String line : skill.getDescription().split("\\.")) {
            if (!line.trim().isEmpty()) {
                lore.add(ChatColor.GRAY + line.trim() + ".");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + level.getLevel() + "/" + skill.getMaxLevel());
        
        // Add progress information
        if (level.getLevel() < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add(ChatColor.YELLOW + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(createProgressBar(progress));
            lore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + 
                    String.format("%,.1f", level.getCurrentXp()) + "/" + 
                    String.format("%,.1f", xpForNextLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.GREEN + 
                    String.format("%.1f", progress * 100) + "%" + ChatColor.GRAY + ")");
        } else {
            lore.add(ChatColor.GREEN + "✦ MAXIMUM LEVEL REACHED! ✦");
        }
        
        lore.add("");
        lore.add(ChatColor.GOLD + "✧ Skill rewards are unlocked");
        lore.add(ChatColor.GOLD + "✧ as you level up this skill.");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a summary item showing all reward totals
     */
    private static ItemStack createSummaryItem(Map<String, Double> totalStatBoosts, 
                                             int totalUnlocks, int totalItems, 
                                             int totalCurrency, int totalPerks) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        
        // Add enchant glow
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ Rewards Summary");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Total rewards from this skill:");
        lore.add("");
        
        // Add stat boosts summary
        if (!totalStatBoosts.isEmpty()) {
            lore.add(ChatColor.AQUA + "» Stat Boosts:");
            for (Map.Entry<String, Double> entry : totalStatBoosts.entrySet()) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "+" + 
                        String.format("%.1f", entry.getValue()) + " " + entry.getKey());
            }
            lore.add("");
        }
        
        // Add other reward type counts
        lore.add(ChatColor.YELLOW + "» Reward Counts:");
        if (totalItems > 0) {
            lore.add(ChatColor.GRAY + "• " + ChatColor.GOLD + totalItems + " Items");
        }
        if (totalCurrency > 0) {
            lore.add(ChatColor.GRAY + "• " + ChatColor.YELLOW + totalCurrency + " Currency Rewards");
        }
        if (totalUnlocks > 0) {
            lore.add(ChatColor.GRAY + "• " + ChatColor.LIGHT_PURPLE + totalUnlocks + " Feature Unlocks");
        }
        if (totalPerks > 0) {
            lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + totalPerks + " Passive Perks");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
 * Calculate tokens for a specific level with tier distribution
 */
private static Map<SkillToken.TokenTier, Integer> calculateTokensForLevel(int level) {
    Map<SkillToken.TokenTier, Integer> tokens = new HashMap<>();
    
    // Different milestone ranges give different token tiers
    if (level >= 90) {
        // Very high levels: Mix of all tiers, emphasis on Master
        tokens.put(SkillToken.TokenTier.BASIC, 1);
        tokens.put(SkillToken.TokenTier.ADVANCED, 2);
        tokens.put(SkillToken.TokenTier.MASTER, 3);
    } else if (level >= 70) {
        // High levels: Advanced and Master tokens
        tokens.put(SkillToken.TokenTier.ADVANCED, 2);
        tokens.put(SkillToken.TokenTier.MASTER, 1);
    } else if (level >= 50) {
        // Mid-high levels: Mix of Advanced and some Master
        tokens.put(SkillToken.TokenTier.BASIC, 1);
        tokens.put(SkillToken.TokenTier.ADVANCED, 2);
        if (level % 20 == 0) { // Every 20 levels gets a Master token
            tokens.put(SkillToken.TokenTier.MASTER, 1);
        }
    } else if (level >= 25) {
        // Mid levels: Basic and Advanced tokens
        tokens.put(SkillToken.TokenTier.BASIC, 1);
        tokens.put(SkillToken.TokenTier.ADVANCED, 1);
    } else if (level >= 10) {
        // Early-mid levels: Mostly Basic, some Advanced
        tokens.put(SkillToken.TokenTier.BASIC, 2);
        if (level % 15 == 0) { // Every 15 levels gets an Advanced token
            tokens.put(SkillToken.TokenTier.ADVANCED, 1);
        }
    } else {
        // Early levels: Only Basic tokens
        tokens.put(SkillToken.TokenTier.BASIC, 1);
    }
    
    return tokens;
}

/**
 * Update reward item creation to show tiered token information
 */
private static ItemStack createRewardItem(int level, List<SkillReward> rewards, boolean unlocked) {
    boolean isMilestone = level % 5 == 0;
    boolean isEmpty = rewards == null || rewards.isEmpty();
    
    // Choose icon based on content
    Material icon = Material.PAPER;
    if (isEmpty && isMilestone) {
        // Milestone with no rewards - use token icon
        icon = Material.GOLD_NUGGET;
    } else if (isEmpty) {
        // No rewards - use placeholder
        icon = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    } else if (rewards.size() == 1) {
        // Single reward - use specific icon
        icon = REWARD_TYPE_ICONS.getOrDefault(rewards.get(0).getType(), Material.PAPER);
    } else if (rewards.size() > 1) {
        // Multiple rewards
        icon = Material.CHEST;
    }
    
    // For locked items, use different appearance
    ItemStack item = unlocked ? new ItemStack(icon) : 
                (isMilestone ? new ItemStack(Material.GILDED_BLACKSTONE) : new ItemStack(Material.BARRIER));
    ItemMeta meta = item.getItemMeta();
    
    // Add enchant glow for unlocked items (unless it's a barrier)
    if (unlocked && !isEmpty) {
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    
    // Set display name with level - special formatting for milestones
    if (isMilestone) {
        meta.setDisplayName((unlocked ? ChatColor.GOLD : ChatColor.RED) + "✦ " + 
            "Level " + level + " Milestone" + (unlocked ? ChatColor.GOLD + " ✦" : ""));
    } else {
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + "Level " + level + " Rewards");
    }
    
    // Create lore with better formatting
    List<String> lore = new ArrayList<>();
    lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    
    // Add status badge with milestone-specific styling
    if (unlocked) {
        lore.add(isMilestone ? ChatColor.GOLD + "✓ MILESTONE UNLOCKED" : ChatColor.GREEN + "✓ UNLOCKED");
        
        // Add skill tokens info for milestones with tier breakdown
        if (isMilestone) {
            Map<SkillToken.TokenTier, Integer> tokenRewards = calculateTokensForLevel(level);
            lore.add(ChatColor.YELLOW + "Token Rewards Received:");
            
            for (Map.Entry<SkillToken.TokenTier, Integer> entry : tokenRewards.entrySet()) {
                SkillToken.TokenTier tier = entry.getKey();
                int count = entry.getValue();
                
                lore.add("  " + tier.getColor() + tier.getSymbol() + " " + count + " " + 
                         tier.getDisplayName() + ChatColor.WHITE + " Token" + (count > 1 ? "s" : ""));
            }
        }
    } else {
        lore.add(isMilestone ? ChatColor.RED + "✗ MILESTONE LOCKED" : ChatColor.RED + "✗ LOCKED");
        
        // Show token info for locked milestones too
        if (isMilestone) {
            Map<SkillToken.TokenTier, Integer> tokenRewards = calculateTokensForLevel(level);
            lore.add(ChatColor.GRAY + "Token Rewards:");
            
            for (Map.Entry<SkillToken.TokenTier, Integer> entry : tokenRewards.entrySet()) {
                SkillToken.TokenTier tier = entry.getKey();
                int count = entry.getValue();
                
                lore.add("  " + tier.getColor() + tier.getSymbol() + " " + count + " " + 
                         tier.getDisplayName() + ChatColor.GRAY + " Token" + (count > 1 ? "s" : ""));
            }
        }
    }
    
    lore.add("");
    
    // Add specific rewards if any
    if (!isEmpty) {
        lore.add(ChatColor.AQUA + "Rewards:");
        for (SkillReward reward : rewards) {
            lore.add(ChatColor.WHITE + "• " + reward.getDescription());
        }
        lore.add("");
    }
    
    // Add requirement information for locked rewards
    if (!unlocked) {
        lore.add(ChatColor.RED + "Requirement: Reach level " + level);
    }
    
    meta.setLore(lore);
    item.setItemMeta(meta);
    return item;
}

/**
 * Create a milestone info item explaining the NEW tiered milestone reward system
 */
private static ItemStack createMilestoneInfoItem(boolean isMainSkill) {
    ItemStack infoItem = new ItemStack(Material.KNOWLEDGE_BOOK);
    ItemMeta meta = infoItem.getItemMeta();
    meta.setDisplayName(ChatColor.GOLD + "✦ Milestone System ✦");
    
    List<String> lore = new ArrayList<>();
    lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    lore.add(ChatColor.YELLOW + "Every 5 levels grants milestone rewards!");
    lore.add("");
    
    lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Token Rewards by Level:");
    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "Level 5-9: " + 
             SkillToken.TokenTier.BASIC.getColor() + "Basic Tokens");
    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "Level 10-24: " + 
             SkillToken.TokenTier.BASIC.getColor() + "Basic + " + 
             SkillToken.TokenTier.ADVANCED.getColor() + "Advanced");
    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "Level 25-49: " + 
             SkillToken.TokenTier.BASIC.getColor() + "Basic + " + 
             SkillToken.TokenTier.ADVANCED.getColor() + "Advanced");
    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "Level 50-69: " + 
             SkillToken.TokenTier.ADVANCED.getColor() + "Advanced + " + 
             SkillToken.TokenTier.MASTER.getColor() + "Master");
    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "Level 70+: " + 
             SkillToken.TokenTier.ADVANCED.getColor() + "Advanced + " + 
             SkillToken.TokenTier.MASTER.getColor() + "Master");
    lore.add("");
    
    lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "How to Use Tokens:");
    if (isMainSkill) {
        lore.add(ChatColor.GRAY + "Spend tokens in this skill's tree");
    } else {
        lore.add(ChatColor.GRAY + "Tokens contribute to parent skill tree");
    }
    lore.add(ChatColor.GRAY + "Higher tier tokens can unlock lower tier nodes");
    lore.add(ChatColor.GRAY + "Each node requires specific token tiers");
    lore.add("");
    
    lore.add(ChatColor.AQUA + "Token Tiers:");
    lore.add("  " + SkillToken.TokenTier.BASIC.getColor() + SkillToken.TokenTier.BASIC.getSymbol() + 
             " Basic: Foundation abilities");
    lore.add("  " + SkillToken.TokenTier.ADVANCED.getColor() + SkillToken.TokenTier.ADVANCED.getSymbol() + 
             " Advanced: Specialized skills");
    lore.add("  " + SkillToken.TokenTier.MASTER.getColor() + SkillToken.TokenTier.MASTER.getSymbol() + 
             " Master: Elite abilities");
    
    meta.setLore(lore);
    infoItem.setItemMeta(meta);
    return infoItem;
}
    
    /**
     * Create an explanation item for the rewards menu
     */
    private static ItemStack createExplanationItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with improved formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ About Skill Rewards");
        
        // Create lore with divider and color-coding
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Skill rewards are permanent bonuses");
        lore.add(ChatColor.GRAY + "you earn as you level up skills.");
        lore.add("");
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Types of rewards:");
        lore.add(REWARD_TYPE_COLORS.get(SkillRewardType.STAT_BOOST) + "• Stat Boosts: " + 
                ChatColor.WHITE + "Increase your player stats");
        lore.add(REWARD_TYPE_COLORS.get(SkillRewardType.ITEM) + "• Items: " + 
                ChatColor.WHITE + "Receive special items");
        lore.add(REWARD_TYPE_COLORS.get(SkillRewardType.CURRENCY) + "• Currency: " + 
                ChatColor.WHITE + "Earn various currencies");
        lore.add(REWARD_TYPE_COLORS.get(SkillRewardType.UNLOCK) + "• Unlocks: " + 
                ChatColor.WHITE + "Gain access to new features");
        lore.add(REWARD_TYPE_COLORS.get(SkillRewardType.PERK) + "• Perks: " + 
                ChatColor.WHITE + "Receive passive benefits");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a visual progress bar
     */
    private static String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 20;
        int filledBars = (int) Math.round(progress * barLength);
        
        // Start with bracket
        bar.append(ChatColor.GRAY + "[");
        
        // Add graduated color based on fill percentage
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if (progress < 0.25) {
                    bar.append(ChatColor.RED);
                } else if (progress < 0.5) {
                    bar.append(ChatColor.GOLD);
                } else if (progress < 0.75) {
                    bar.append(ChatColor.YELLOW);
                } else {
                    bar.append(ChatColor.GREEN);
                }
                bar.append("■");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("■");
            }
        }
        
        // Close bracket
        bar.append(ChatColor.GRAY + "]");
        
        return bar.toString();
    }
    
    /**
     * Create decorative border for GUI
     */
    private static void createBorder(Inventory gui) {
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack cyan = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack corner = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Set corners
        gui.setItem(0, corner);
        gui.setItem(8, corner);
        gui.setItem(45, corner); // Don't override back button
        gui.setItem(53, corner);
        
        // Top and bottom borders with alternating colors
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : cyan);
            gui.setItem(45 + i, i % 2 == 0 ? blue : cyan);
        }
        
        // Side borders
        for (int i = 1; i <= 4; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? blue : cyan);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? blue : cyan);
        }
    }
    
    /**
     * Create glass pane with empty name
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Fill empty slots with black glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}