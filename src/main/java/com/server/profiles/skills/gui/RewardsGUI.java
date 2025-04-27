package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;

/**
 * GUI for displaying all rewards for a skill
 */
public class RewardsGUI {

    private static final String GUI_TITLE_PREFIX = "Rewards: ";
    private static final Map<SkillRewardType, Material> REWARD_TYPE_ICONS = new HashMap<>();
    
    static {
        REWARD_TYPE_ICONS.put(SkillRewardType.STAT_BOOST, Material.DIAMOND);
        REWARD_TYPE_ICONS.put(SkillRewardType.ITEM, Material.CHEST);
        REWARD_TYPE_ICONS.put(SkillRewardType.CURRENCY, Material.GOLD_INGOT);
        REWARD_TYPE_ICONS.put(SkillRewardType.UNLOCK, Material.NETHER_STAR);
        REWARD_TYPE_ICONS.put(SkillRewardType.PERK, Material.ENCHANTED_BOOK);
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
        
        // Create skill info item
        ItemStack skillInfoItem = createSkillInfoItem(skill, level);
        gui.setItem(4, skillInfoItem);
        
        // Map of levels with rewards
        Map<Integer, List<SkillReward>> rewardsMap = new HashMap<>();
        
        // Find all levels with rewards
        for (int i = 1; i <= skill.getMaxLevel(); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                rewardsMap.put(i, rewards);
            }
        }
        
        // Add reward items
        int slot = 9;
        for (Map.Entry<Integer, List<SkillReward>> entry : rewardsMap.entrySet()) {
            int rewardLevel = entry.getKey();
            List<SkillReward> rewards = entry.getValue();
            
            // Create reward item
            ItemStack rewardItem = createRewardItem(rewardLevel, rewards, level.getLevel() >= rewardLevel);
            gui.setItem(slot, rewardItem);
            
            // Move to next slot
            slot++;
            
            // Skip to next row if we've reached the end of the current row
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            // Stop if we run out of space
            if (slot >= gui.getSize() - 9) {
                break;
            }
        }
        
        // Add explanation item
        ItemStack explanationItem = createExplanationItem();
        gui.setItem(49, explanationItem);
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Skill Details");
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
        
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
                icon = Material.IRON_SHOVEL;
                break;
            case "fishing":
                icon = Material.FISHING_ROD;
                break;
            case "farming":
                icon = Material.IRON_HOE;
                break;
            case "combat":
                icon = Material.IRON_SWORD;
                break;
            default:
                icon = Material.BOOK;
                break;
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
        lore.add("");
        lore.add(ChatColor.GOLD + "Skill rewards are unlocked");
        lore.add(ChatColor.GOLD + "as you level up this skill.");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create an item for a level reward
     */
    private static ItemStack createRewardItem(int level, List<SkillReward> rewards, boolean unlocked) {
        Material icon = Material.PAPER;
        
        // If there's only one reward, use its icon
        if (rewards.size() == 1) {
            icon = REWARD_TYPE_ICONS.getOrDefault(rewards.get(0).getType(), Material.PAPER);
        } else {
            // Use a chest for multiple rewards
            icon = Material.CHEST;
        }
        
        ItemStack item = unlocked ? new ItemStack(icon) : new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + "Level " + level + " Rewards");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        
        if (unlocked) {
            lore.add(ChatColor.GREEN + "UNLOCKED");
        } else {
            lore.add(ChatColor.RED + "LOCKED");
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Rewards:");
        
        for (SkillReward reward : rewards) {
            lore.add(ChatColor.GRAY + "- " + reward.getDescription());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create an explanation item for the rewards menu
     */
    private static ItemStack createExplanationItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + "About Skill Rewards");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Skill rewards are permanent bonuses");
        lore.add(ChatColor.GRAY + "you earn as you level up skills.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Types of rewards include:");
        lore.add(ChatColor.YELLOW + "• Stat Boosts: " + ChatColor.GRAY + "Increase your player stats");
        lore.add(ChatColor.YELLOW + "• Items: " + ChatColor.GRAY + "Receive special items");
        lore.add(ChatColor.YELLOW + "• Currency: " + ChatColor.GRAY + "Earn various currencies");
        lore.add(ChatColor.YELLOW + "• Unlocks: " + ChatColor.GRAY + "Gain access to new features");
        lore.add(ChatColor.YELLOW + "• Perks: " + ChatColor.GRAY + "Receive passive benefits");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
}