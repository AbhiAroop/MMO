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

/**
 * GUI for displaying subskills for a main skill
 */
public class SubskillsGUI {

    private static final String GUI_TITLE_PREFIX = "Subskills: ";
    private static final Map<String, Material> SUBSKILL_ICONS = new HashMap<>();
    
    static {
        // Mining subskills
        SUBSKILL_ICONS.put("ore_extraction", Material.IRON_ORE);
        SUBSKILL_ICONS.put("gem_carving", Material.DIAMOND);
        SUBSKILL_ICONS.put("ore_efficiency", Material.REDSTONE);
        SUBSKILL_ICONS.put("rare_finds", Material.GOLD_NUGGET);
        SUBSKILL_ICONS.put("stone_breaker", Material.COBBLESTONE);
        
        // Excavating subskills
        SUBSKILL_ICONS.put("treasure_hunter", Material.CHEST);
        SUBSKILL_ICONS.put("soil_master", Material.DIRT);
        SUBSKILL_ICONS.put("archaeologist", Material.BONE);
        
        // Fishing subskills
        SUBSKILL_ICONS.put("fisherman", Material.COD);
        SUBSKILL_ICONS.put("aquatic_treasures", Material.PRISMARINE_CRYSTALS);
        SUBSKILL_ICONS.put("master_angler", Material.TROPICAL_FISH);
        
        // Farming subskills
        SUBSKILL_ICONS.put("crop_growth", Material.WHEAT);
        SUBSKILL_ICONS.put("animal_breeder", Material.EGG);
        SUBSKILL_ICONS.put("harvester", Material.HAY_BLOCK);
        
        // Combat subskills
        SUBSKILL_ICONS.put("swordsmanship", Material.DIAMOND_SWORD);
        SUBSKILL_ICONS.put("archery", Material.BOW);
        SUBSKILL_ICONS.put("defense", Material.SHIELD);
    }
    
    /**
     * Open the subskills menu for a player
     */
    public static void openSubskillsMenu(Player player, Skill mainSkill) {
        if (!mainSkill.isMainSkill()) {
            player.sendMessage(ChatColor.RED + "Only main skills have subskills!");
            return;
        }
        
        List<Skill> subskills = mainSkill.getSubskills();
        if (subskills.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This skill doesn't have any subskills yet!");
            return;
        }
        
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE_PREFIX + mainSkill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Create main skill info item
        SkillLevel mainSkillLevel = profile.getSkillData().getSkillLevel(mainSkill);
        ItemStack mainSkillItem = createMainSkillItem(mainSkill, mainSkillLevel);
        gui.setItem(4, mainSkillItem);
        
        // Add subskill items
        int[] slots = {11, 13, 15, 20, 22, 24}; // Slots for subskills in a symmetrical layout
        for (int i = 0; i < Math.min(subskills.size(), slots.length); i++) {
            Skill subskill = subskills.get(i);
            SkillLevel subskillLevel = profile.getSkillData().getSkillLevel(subskill);
            
            Material icon = SUBSKILL_ICONS.getOrDefault(subskill.getId(), Material.PAPER);
            ItemStack subskillItem = createSubskillItem(subskill, subskillLevel, icon);
            gui.setItem(slots[i], subskillItem);
        }
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Skill Details");
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
     * Create an item for the main skill
     */
    private static ItemStack createMainSkillItem(Skill skill, SkillLevel level) {
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
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName() + ChatColor.GRAY + " [Level " + level.getLevel() + "]");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + skill.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Skill Level: " + level.getLevel() + "/" + skill.getMaxLevel());
        
        if (level.getLevel() < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add(ChatColor.YELLOW + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(ChatColor.GRAY + createProgressBar(progress));
            lore.add(ChatColor.GRAY + "XP: " + String.format("%.1f", level.getCurrentXp()) + 
                    " / " + String.format("%.1f", xpForNextLevel) + 
                    " (" + String.format("%.1f", progress * 100) + "%)");
        } else {
            lore.add(ChatColor.GREEN + "MAXIMUM LEVEL REACHED!");
        }
        
        lore.add("");
        lore.add(ChatColor.AQUA + "Subskills Available: " + skill.getSubskills().size());
        lore.add(ChatColor.YELLOW + "Click on a subskill to view details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item for a subskill
     */
    private static ItemStack createSubskillItem(Skill subskill, SkillLevel level, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.AQUA + subskill.getDisplayName() + ChatColor.GRAY + " [Level " + level.getLevel() + "]");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + subskill.getDescription());
        lore.add("");
        
        double xpForNextLevel = subskill.getXpForLevel(level.getLevel() + 1);
        double progress = level.getProgressPercentage(xpForNextLevel);
        
        if (level.getLevel() < subskill.getMaxLevel()) {
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
        
        // Show next milestone level
        String nextMilestone = getNextMilestone(subskill, level.getLevel());
        if (nextMilestone != null) {
            lore.add(ChatColor.YELLOW + "Next Milestone: " + nextMilestone);
        } else {
            lore.add(ChatColor.GREEN + "All Milestones Achieved!");
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get the next milestone level for a skill
     */
    private static String getNextMilestone(Skill skill, int currentLevel) {
        for (int milestoneLevel : skill.getMilestones()) {
            if (milestoneLevel > currentLevel) {
                return "Level " + milestoneLevel;
            }
        }
        return null; // No more milestones
    }
    
    /**
     * Creates a progress bar visualization
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