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
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.data.SkillLevel;

/**
 * GUI for displaying skills and their levels
 */
public class SkillsGUI {

    private static final String GUI_TITLE = "Skills";
    
    /**
     * Open the skills menu for a player
     */
    public static void openSkillsMenu(Player player) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Add mining skill
        Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
        if (miningSkill != null) {
            SkillLevel miningLevel = profile.getSkillData().getSkillLevel(miningSkill);
            ItemStack miningItem = createSkillItem(player, profile, miningSkill, miningLevel, Material.IRON_PICKAXE);
            gui.setItem(10, miningItem);
        }
        
        // Add excavating skill
        Skill excavatingSkill = SkillRegistry.getInstance().getSkill(SkillType.EXCAVATING);
        if (excavatingSkill != null) {
            SkillLevel excavatingLevel = profile.getSkillData().getSkillLevel(excavatingSkill);
            ItemStack excavatingItem = createSkillItem(player, profile, excavatingSkill, excavatingLevel, Material.IRON_SHOVEL);
            gui.setItem(12, excavatingItem);
        }
        
        // Add fishing skill
        Skill fishingSkill = SkillRegistry.getInstance().getSkill(SkillType.FISHING);
        if (fishingSkill != null) {
            SkillLevel fishingLevel = profile.getSkillData().getSkillLevel(fishingSkill);
            ItemStack fishingItem = createSkillItem(player, profile, fishingSkill, fishingLevel, Material.FISHING_ROD);
            gui.setItem(14, fishingItem);
        }
        
        // Add farming skill
        Skill farmingSkill = SkillRegistry.getInstance().getSkill(SkillType.FARMING);
        if (farmingSkill != null) {
            SkillLevel farmingLevel = profile.getSkillData().getSkillLevel(farmingSkill);
            ItemStack farmingItem = createSkillItem(player, profile, farmingSkill, farmingLevel, Material.IRON_HOE);
            gui.setItem(16, farmingItem);
        }
        
        // Add combat skill
        Skill combatSkill = SkillRegistry.getInstance().getSkill(SkillType.COMBAT);
        if (combatSkill != null) {
            SkillLevel combatLevel = profile.getSkillData().getSkillLevel(combatSkill);
            ItemStack combatItem = createSkillItem(player, profile, combatSkill, combatLevel, Material.IRON_SWORD);
            gui.setItem(22, combatItem);
        }
        
        // Add info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Skill Information");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Skills improve as you perform actions");
        infoLore.add(ChatColor.GRAY + "Each skill grants different bonuses");
        infoLore.add(ChatColor.GRAY + "and has unique subskills to master!");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Click on a skill to view more details");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(31, infoItem);
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Menu");
        backButton.setItemMeta(backMeta);
        gui.setItem(27, backButton);
        
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
     * Create an item representing a skill
     */
    private static ItemStack createSkillItem(Player player, PlayerProfile profile, Skill skill, SkillLevel level, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with level indicator (0 for beginners)
        meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName() + ChatColor.GRAY + " [Lvl " + level.getLevel() + "]");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + skill.getDescription());
        lore.add("");
        
        // For level 0, show progress to level 1
        // For higher levels, show progress to next level
        double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
        double progress = level.getProgressPercentage(xpForNextLevel);
        
        if (level.getLevel() < skill.getMaxLevel()) {
            lore.add(ChatColor.YELLOW + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(ChatColor.GRAY + createProgressBar(progress));
            lore.add(ChatColor.GRAY + "XP: " + String.format("%.1f", level.getCurrentXp()) + 
                    " / " + String.format("%.1f", xpForNextLevel) + 
                    " (" + String.format("%.1f", progress * 100) + "%)");
        } else {
            lore.add(ChatColor.GREEN + "MAXIMUM LEVEL REACHED!");
            lore.add(ChatColor.GRAY + "Total XP: " + String.format("%.1f", level.getTotalXp()));
        }
        
        lore.add("");
        
        // Add number of subskills if this is a main skill
        if (skill.isMainSkill()) {
            List<Skill> subskills = skill.getSubskills();
            lore.add(ChatColor.AQUA + "Subskills: " + subskills.size());
            
            // Show the subskills directly in the main menu
            if (!subskills.isEmpty()) {
                for (Skill subskill : subskills) {
                    SkillLevel subskillLevel = profile.getSkillData().getSkillLevel(subskill);
                    lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + subskill.getDisplayName() + 
                            ChatColor.GRAY + " [Lvl " + subskillLevel.getLevel() + "]");
                }
            }
        }
        
        meta.setLore(lore);
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
}