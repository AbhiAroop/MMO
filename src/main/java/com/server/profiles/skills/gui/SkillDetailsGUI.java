package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * GUI for displaying detailed information about a skill
 */
public class SkillDetailsGUI {

    private static final String GUI_TITLE_PREFIX = "Skill: ";
    
    /**
     * Open the skill details menu for a player
     */
    public static void openSkillDetailsMenu(Player player, Skill skill) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill level
        SkillLevel level = skill.getSkillLevel(player);
        
        // Add skill info
        ItemStack infoItem = createSkillInfoItem(skill, level);
        gui.setItem(4, infoItem);
        
        // Add progress bar
        ItemStack progressItem = createProgressItem(skill, level);
        gui.setItem(13, progressItem);
        
        // Add rewards button
        ItemStack rewardsItem = createRewardsItem(skill, level);
        gui.setItem(11, rewardsItem);
        
        // Add skill tree button
        ItemStack skillTreeItem = createSkillTreeItem(skill, profile);
        gui.setItem(15, skillTreeItem);
        
        // Add abilities button
        ItemStack abilitiesItem = createAbilitiesItem(skill, player);
        gui.setItem(22, abilitiesItem);
        
        // If this is a main skill with subskills, add subskills button
        if (skill.isMainSkill() && !skill.getSubskills().isEmpty()) {
            ItemStack subskillsItem = new ItemStack(Material.BOOK);
            ItemMeta subskillsMeta = subskillsItem.getItemMeta();
            subskillsMeta.setDisplayName(ChatColor.AQUA + "View Subskills");
            
            List<String> subskillsLore = new ArrayList<>();
            subskillsLore.add(ChatColor.GRAY + "Click to view subskills for");
            subskillsLore.add(ChatColor.GRAY + "this skill.");
            subskillsLore.add("");
            subskillsLore.add(ChatColor.YELLOW + "Subskills: " + skill.getSubskills().size());
            
            subskillsMeta.setLore(subskillsLore);
            subskillsItem.setItemMeta(subskillsMeta);
            
            gui.setItem(22 - 9, subskillsItem); // Place above abilities button
        }
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Skills");
        backButton.setItemMeta(backMeta);
        gui.setItem(18, backButton);
        
        // Open inventory
        player.openInventory(gui);
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
            case "ore_extraction":
                icon = Material.IRON_ORE;
                break;
            case "gem_carving":
                icon = Material.DIAMOND;
                break;
            default:
                icon = Material.NETHER_STAR;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName() + ChatColor.GRAY + " [Level " + level.getLevel() + "]");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + skill.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Current Level: " + level.getLevel() + "/" + skill.getMaxLevel());
        lore.add(ChatColor.YELLOW + "Total XP: " + String.format("%.1f", level.getTotalXp()));
        
        if (skill.isMainSkill()) {
            lore.add("");
            lore.add(ChatColor.AQUA + "Subskills: " + skill.getSubskills().size());
            
            // Add info about available subskills
            if (!skill.getSubskills().isEmpty()) {
                for (Skill subskill : skill.getSubskills()) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + subskill.getDisplayName());
                }
            }
        } else {
            lore.add("");
            lore.add(ChatColor.AQUA + "Parent Skill: " + skill.getParentSkill().getDisplayName());
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Milestones: " + String.join(", ", convertListToStrings(skill.getMilestones())));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item showing skill progress
     */
    private static ItemStack createProgressItem(Skill skill, SkillLevel level) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Skill Progress");
        
        List<String> lore = new ArrayList<>();
        
        if (level.getLevel() < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add(ChatColor.GRAY + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(ChatColor.GRAY + createProgressBar(progress));
            lore.add(ChatColor.GRAY + "XP: " + String.format("%.1f", level.getCurrentXp()) + 
                    " / " + String.format("%.1f", xpForNextLevel) + 
                    " (" + String.format("%.1f", progress * 100) + "%)");
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Total XP Needed to Max Level:");
            
            // Calculate total XP needed to reach max level
            double totalXpToMax = 0;
            for (int i = level.getLevel() + 1; i <= skill.getMaxLevel(); i++) {
                totalXpToMax += skill.getXpForLevel(i);
            }
            
            // Subtract current level progress
            totalXpToMax -= level.getCurrentXp();
            
            lore.add(ChatColor.GRAY + "Remaining XP: " + String.format("%.1f", totalXpToMax));
        } else {
            lore.add(ChatColor.GREEN + "MAXIMUM LEVEL REACHED!");
            lore.add(ChatColor.GRAY + "Total XP Earned: " + String.format("%.1f", level.getTotalXp()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Congratulations on maxing this skill!");
        }
        
        // Add special details for mining subskills
        if (skill instanceof OreExtractionSubskill) {
            OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
            lore.add("");
            lore.add(ChatColor.AQUA + "Ore Extraction Bonuses:");
            lore.add(ChatColor.GRAY + "• Mining Speed: " + 
                    ChatColor.YELLOW + String.format("%.2fx", oreSkill.getMiningSpeedMultiplier(level.getLevel())));
            lore.add(ChatColor.GRAY + "• Mining Fortune: " + 
                    ChatColor.YELLOW + String.format("+%.1f", oreSkill.getMiningFortuneBonus(level.getLevel())));
            lore.add(ChatColor.GRAY + "• Bonus Drops: " + 
                    ChatColor.YELLOW + String.format("%.1f%%", oreSkill.getBonusDropChance(level.getLevel()) * 100));
            lore.add(ChatColor.GRAY + "• Cave-in Risk: " + 
                    ChatColor.YELLOW + String.format("%.1f%%", oreSkill.getCaveInChance(level.getLevel()) * 100));
        }
        else if (skill instanceof GemCarvingSubskill) {
            GemCarvingSubskill gemSkill = (GemCarvingSubskill) skill;
            lore.add("");
            lore.add(ChatColor.AQUA + "Gem Carving Bonuses:");
            lore.add(ChatColor.GRAY + "• Gem Find Rate: " + 
                    ChatColor.YELLOW + String.format("%.1f%%", gemSkill.getGemFindChance(level.getLevel()) * 100));
            lore.add(ChatColor.GRAY + "• Extraction Success: " + 
                    ChatColor.YELLOW + String.format("%.1f%%", gemSkill.getExtractionSuccessChance(level.getLevel()) * 100));
            lore.add(ChatColor.GRAY + "• Gem Quality: " + 
                    ChatColor.YELLOW + String.format("%.2fx", gemSkill.getGemQualityMultiplier(level.getLevel())));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item showing skill rewards
     */
    private static ItemStack createRewardsItem(Skill skill, SkillLevel level) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Skill Rewards");
        
        List<String> lore = new ArrayList<>();
        
        // Count how many levels have rewards
        int levelsWithRewards = 0;
        int rewardsEarned = 0;
        
        for (int i = 1; i <= skill.getMaxLevel(); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                levelsWithRewards++;
                if (i <= level.getLevel()) {
                    rewardsEarned++;
                }
            }
        }
        
        lore.add(ChatColor.GRAY + "Rewards Earned: " + 
                ChatColor.YELLOW + rewardsEarned + "/" + levelsWithRewards);
        lore.add("");
        
        // Show a few rewards that have been earned
        lore.add(ChatColor.YELLOW + "Earned Rewards:");
        boolean hasEarnedRewards = false;
        
        for (int i = 1; i <= level.getLevel() && i <= 3; i++) { // Show only first 3 earned levels
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                hasEarnedRewards = true;
                lore.add(ChatColor.GRAY + "Level " + i + ":");
                for (SkillReward reward : rewards) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + reward.getDescription());
                }
            }
        }
        
        if (!hasEarnedRewards) {
            lore.add(ChatColor.GRAY + "None yet");
        }
        
        lore.add("");
        
        // Show next rewards to earn
        lore.add(ChatColor.YELLOW + "Next Rewards:");
        boolean hasNextRewards = false;
        
        for (int i = level.getLevel() + 1; i <= skill.getMaxLevel() && i <= level.getLevel() + 3; i++) { // Show only next 3 levels
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                hasNextRewards = true;
                lore.add(ChatColor.GRAY + "Level " + i + ":");
                for (SkillReward reward : rewards) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.RED + reward.getDescription());
                }
            }
        }
        
        if (!hasNextRewards) {
            if (level.getLevel() == skill.getMaxLevel()) {
                lore.add(ChatColor.GREEN + "All rewards earned!");
            } else {
                lore.add(ChatColor.GRAY + "None in the next few levels");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view all rewards");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Convert a list of integers to a list of strings
     */
    private static List<String> convertListToStrings(List<Integer> list) {
        List<String> strings = new ArrayList<>();
        for (Integer item : list) {
            strings.add(item.toString());
        }
        return strings;
    }
    
    /**
     * Create a visual progress bar
     * @param progress The progress percentage (0-100)
     * @return A string representing the progress bar
     */
    private static String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 20;
        int filledBars = (int) Math.round(progress * barLength);
        
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filledBars; i++) {
            bar.append("■");
        }
        
        bar.append(ChatColor.GRAY);
        for (int i = filledBars; i < barLength; i++) {
            bar.append("■");
        }
        
        return bar.toString();
    }

    /**
     * Create a button for accessing the skill tree
     */
    private static ItemStack createSkillTreeItem(Skill skill, PlayerProfile profile) {
        // Get skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int tokenCount = treeData.getTokenCount(skill.getId());
        
        // Get token display information
        SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
        
        // Create item
        ItemStack item = new ItemStack(tokenInfo.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Skill Tree");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Unlock special abilities and");
        lore.add(ChatColor.GRAY + "bonuses for " + ChatColor.YELLOW + skill.getDisplayName());
        lore.add("");
        lore.add(ChatColor.YELLOW + "You have " + tokenInfo.color + tokenCount + " " + 
            tokenInfo.displayName + " Token" + (tokenCount != 1 ? "s" : ""));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view Skill Tree");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a button for accessing abilities
     */
    private static ItemStack createAbilitiesItem(Skill skill, Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Skill Abilities");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "View and manage abilities for");
        lore.add(ChatColor.GRAY + "this skill.");
        lore.add("");
        
        // Add ability counts
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int unlockedPassive = registry.getUnlockedPassiveAbilities(player, skill.getId()).size();
        int totalPassive = registry.getPassiveAbilities(skill.getId()).size();
        int unlockedActive = registry.getUnlockedActiveAbilities(player, skill.getId()).size();
        int totalActive = registry.getActiveAbilities(skill.getId()).size();
        
        lore.add(ChatColor.YELLOW + "Passive Abilities: " + 
                ChatColor.GREEN + unlockedPassive + "/" + totalPassive);
        lore.add(ChatColor.YELLOW + "Active Abilities: " + 
                ChatColor.GREEN + unlockedActive + "/" + totalActive);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view abilities");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
}