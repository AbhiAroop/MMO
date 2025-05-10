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

    private static final String GUI_TITLE = "✦ Skills Menu ✦";
    
    /**
     * Open the skills menu for a player
     */
    public static void openSkillsMenu(Player player) {
        // Create inventory with improved size (45 slots for better layout)
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Create fancy border for the top
        createBorder(gui);
        
        // Display skills in a circular pattern around the center
        
        // Mining skill (top left)
        Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
        if (miningSkill != null) {
            SkillLevel miningLevel = profile.getSkillData().getSkillLevel(miningSkill);
            ItemStack miningItem = createSkillItem(player, profile, miningSkill, miningLevel, Material.DIAMOND_PICKAXE);
            gui.setItem(11, miningItem);
        }
        
        // Excavating skill (top right)
        Skill excavatingSkill = SkillRegistry.getInstance().getSkill(SkillType.EXCAVATING);
        if (excavatingSkill != null) {
            SkillLevel excavatingLevel = profile.getSkillData().getSkillLevel(excavatingSkill);
            ItemStack excavatingItem = createSkillItem(player, profile, excavatingSkill, excavatingLevel, Material.DIAMOND_SHOVEL);
            gui.setItem(15, excavatingItem);
        }
        
        // Fishing skill (middle left)
        Skill fishingSkill = SkillRegistry.getInstance().getSkill(SkillType.FISHING);
        if (fishingSkill != null) {
            SkillLevel fishingLevel = profile.getSkillData().getSkillLevel(fishingSkill);
            ItemStack fishingItem = createSkillItem(player, profile, fishingSkill, fishingLevel, Material.FISHING_ROD);
            gui.setItem(20, fishingItem);
        }
        
        // Farming skill (middle right)
        Skill farmingSkill = SkillRegistry.getInstance().getSkill(SkillType.FARMING);
        if (farmingSkill != null) {
            SkillLevel farmingLevel = profile.getSkillData().getSkillLevel(farmingSkill);
            ItemStack farmingItem = createSkillItem(player, profile, farmingSkill, farmingLevel, Material.DIAMOND_HOE);
            gui.setItem(24, farmingItem);
        }
        
        // Combat skill (bottom center)
        Skill combatSkill = SkillRegistry.getInstance().getSkill(SkillType.COMBAT);
        if (combatSkill != null) {
            SkillLevel combatLevel = profile.getSkillData().getSkillLevel(combatSkill);
            ItemStack combatItem = createSkillItem(player, profile, combatSkill, combatLevel, Material.DIAMOND_SWORD);
            gui.setItem(31, combatItem);
        }
        
        // Add center info item with player stats
        ItemStack infoItem = createInfoItem(player, profile);
        gui.setItem(22, infoItem);
        
        // Add back button (bottom left)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to Menu");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to the main menu");
        backMeta.setLore(backLore);
        backButton.setItemMeta(backMeta);
        gui.setItem(36, backButton);
        
        // Add help button (bottom right)
        ItemStack helpButton = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta helpMeta = helpButton.getItemMeta();
        helpMeta.setDisplayName(ChatColor.YELLOW + "How Skills Work");
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.GRAY + "Click for a quick tutorial");
        helpLore.add(ChatColor.GRAY + "on how the skill system works");
        helpMeta.setLore(helpLore);
        helpButton.setItemMeta(helpMeta);
        gui.setItem(44, helpButton);
        
        // Fill remaining slots with glass panes
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }
    
    /**
     * Create an item representing a skill
     */
    private static ItemStack createSkillItem(Player player, PlayerProfile profile, Skill skill, SkillLevel level, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Add enchant glow for max level skills
        if (level.getLevel() >= skill.getMaxLevel()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        // Set display name with level indicator
        meta.setDisplayName(ChatColor.GOLD + "✦ " + skill.getDisplayName() + " " + 
                ChatColor.YELLOW + "[Lvl " + level.getLevel() + "]");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        
        // Add divider
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Description with improved formatting
        for (String line : skill.getDescription().split("\\.")) {
            if (!line.trim().isEmpty()) {
                lore.add(ChatColor.GRAY + line.trim() + ".");
            }
        }
        
        lore.add("");
        
        // Level progress section with fancy formatting
        if (level.getLevel() < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add(ChatColor.YELLOW + "» Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(createProgressBar(progress));
            lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + String.format("%.1f", level.getCurrentXp()) + 
                    ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%.1f", xpForNextLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.GREEN + String.format("%.1f", progress * 100) + "%" + 
                    ChatColor.GRAY + ")");
        } else {
            lore.add(ChatColor.GREEN + "» MAXIMUM LEVEL REACHED!" + ChatColor.GOLD + " ✦");
            lore.add(ChatColor.WHITE + "Total XP Earned: " + ChatColor.AQUA + String.format("%.1f", level.getTotalXp()));
        }
        
        lore.add("");
        
        // Add bonuses section
        lore.add(ChatColor.YELLOW + "» Current Bonuses:");
        // This would be skill-specific and can be expanded
        lore.add(ChatColor.GRAY + "• +5% " + skill.getDisplayName() + " XP Gain");
        if (level.getLevel() >= 10) {
            lore.add(ChatColor.GRAY + "• +10% Drop Rate");
        }
        if (level.getLevel() >= 25) {
            lore.add(ChatColor.GRAY + "• +15% Resource Efficiency");
        }
        
        lore.add("");
        
        // Add subskills with improved formatting
        if (skill.isMainSkill() && !skill.getSubskills().isEmpty()) {
            List<Skill> subskills = skill.getSubskills();
            lore.add(ChatColor.YELLOW + "» Subskills " + ChatColor.GRAY + "(" + subskills.size() + "):");
            
            for (Skill subskill : subskills) {
                SkillLevel subskillLevel = profile.getSkillData().getSkillLevel(subskill);
                
                // Color code based on level
                ChatColor levelColor = ChatColor.RED;
                if (subskillLevel.getLevel() > 20) levelColor = ChatColor.GREEN;
                else if (subskillLevel.getLevel() > 10) levelColor = ChatColor.YELLOW;
                else if (subskillLevel.getLevel() > 5) levelColor = ChatColor.GOLD;
                
                lore.add(ChatColor.AQUA + "• " + ChatColor.WHITE + subskill.getDisplayName() + " " + 
                        levelColor + "[Lvl " + subskillLevel.getLevel() + "]");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view skill details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create info item with player stats
     */
    private static ItemStack createInfoItem(Player player, PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ " + player.getName() + "'s Skills ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Skills improve as you perform actions");
        lore.add(ChatColor.GRAY + "related to each skill type.");
        lore.add("");
        
        // Calculate total skill level
        int totalLevel = 0;
        int totalMaxLevel = 0;
        
        for (SkillType type : SkillType.values()) {
            Skill skill = SkillRegistry.getInstance().getSkill(type);
            if (skill != null && skill.isMainSkill()) {
                SkillLevel level = profile.getSkillData().getSkillLevel(skill);
                totalLevel += level.getLevel();
                totalMaxLevel += skill.getMaxLevel();
            }
        }
        
        double completionPercent = (double) totalLevel / totalMaxLevel * 100;
        
        lore.add(ChatColor.YELLOW + "» Total Level: " + ChatColor.GREEN + totalLevel + "/" + totalMaxLevel);
        lore.add(ChatColor.YELLOW + "» Completion: " + ChatColor.GREEN + String.format("%.1f", completionPercent) + "%");
        lore.add(createProgressBar(completionPercent / 100));
        lore.add("");
        lore.add(ChatColor.GRAY + "Each skill grants different bonuses");
        lore.add(ChatColor.GRAY + "and has unique subskills to master!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a visual progress bar
     */
    private static String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 24;
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
     * Create decorative border
     */
    private static void createBorder(Inventory gui) {
        // Top border with alternating colors
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack cyan = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack corner = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Set corners
        gui.setItem(0, corner);
        gui.setItem(8, corner);
        gui.setItem(36, corner);
        gui.setItem(44, corner);
        
        // Top and bottom borders
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : cyan);
            gui.setItem(36 + i, i % 2 == 0 ? blue : cyan);
        }
        
        // Side borders
        for (int i = 1; i <= 3; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? blue : cyan);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? blue : cyan);
        }
    }
    
    /**
     * Create a glass pane with empty name
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