package com.server.profiles.skills.gui;

import static java.lang.String.format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.server.profiles.skills.tokens.SkillToken;

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
        
        // Create inventory with a larger size (45 slots) for better layout
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE_PREFIX + mainSkill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Create border for better visual appearance
        createBorder(gui);
        
        // Create main skill info item with enhanced visuals
        SkillLevel mainSkillLevel = profile.getSkillData().getSkillLevel(mainSkill);
        ItemStack mainSkillItem = createMainSkillItem(mainSkill, mainSkillLevel);
        gui.setItem(4, mainSkillItem);
        
        // REMOVED: Category header below main skill
        // Instead, moved subskills one row up
        
        // Add subskill items in a more visually appealing layout
        // 3x3 grid in the center but moved up one row (slots 10-12, 19-21, 28-30)
        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        
        for (int i = 0; i < Math.min(subskills.size(), slots.length); i++) {
            Skill subskill = subskills.get(i);
            SkillLevel subskillLevel = profile.getSkillData().getSkillLevel(subskill);
            
            Material icon = SUBSKILL_ICONS.getOrDefault(subskill.getId(), Material.PAPER);
            ItemStack subskillItem = createSubskillItem(subskill, subskillLevel, icon);
            gui.setItem(slots[i], subskillItem);
        }
        
        // Add help button in the bottom right
        ItemStack helpButton = createHelpButton();
        gui.setItem(44, helpButton);
        
        // Add back button in the bottom left - FIXED to navigate to parent skill
        ItemStack backButton = createBackButton(mainSkill);
        gui.setItem(36, backButton);
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Create back button - FIXED to link to parent skill details
     */
    private static ItemStack createBackButton(Skill parentSkill) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to Skill Details");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to " + parentSkill.getDisplayName() + " details");
        
        // Store parent skill ID for click handler in a fixed format that matches what the handler expects
        lore.add(ChatColor.BLACK + "PARENT_SKILL:" + parentSkill.getId());
        
        backMeta.setLore(lore);
        backButton.setItemMeta(backMeta);
        return backButton;
    }
    
    /**
     * Create a decorative border for the GUI
     */
    private static void createBorder(Inventory gui) {
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack lightBlue = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack cyan = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        
        // Set corners with special glass color
        gui.setItem(0, cyan);
        gui.setItem(8, cyan);
        gui.setItem(36, cyan); // Will be replaced with back button
        gui.setItem(44, cyan); // Will be replaced with help button
        
        // Top and bottom borders
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : lightBlue);
            gui.setItem(36 + i, i % 2 == 0 ? blue : lightBlue);
        }
        
        // Side borders
        for (int i = 1; i < 4; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? lightBlue : blue);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? lightBlue : blue);
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
     * Create an item for the main skill with enhanced visuals
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
        
        // Set display name with enhanced formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ " + skill.getDisplayName() + " ✦");
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Create lore with decorative elements
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + skill.getDescription());
        lore.add("");
        
        // Add level information with color coding
        ChatColor levelColor = ChatColor.RED;
        if (level.getLevel() >= skill.getMaxLevel()) levelColor = ChatColor.AQUA;
        else if (level.getLevel() > 50) levelColor = ChatColor.GREEN;
        else if (level.getLevel() > 25) levelColor = ChatColor.YELLOW;
        else if (level.getLevel() > 10) levelColor = ChatColor.GOLD;
        
        lore.add(ChatColor.YELLOW + "Skill Level: " + levelColor + level.getLevel() + ChatColor.GRAY + "/" + skill.getMaxLevel());
        
        if (level.getLevel() < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(createProgressBar(progress));
            lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + String.format("%,.1f", level.getCurrentXp()) + 
                    ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,.1f", xpForNextLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.GREEN + String.format("%.1f%%", progress * 100) + ChatColor.GRAY + ")");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "✦ MAXIMUM LEVEL REACHED! ✦");
            lore.add(ChatColor.GRAY + "Total XP Earned: " + ChatColor.AQUA + String.format("%,.1f", level.getTotalXp()));
        }
        
        // Add subskill count with styling
        lore.add("");
        lore.add(ChatColor.AQUA + "» Subskills: " + ChatColor.YELLOW + skill.getSubskills().size());
        lore.add(ChatColor.GRAY + "Choose a subskill below to view details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a category header item
     */
    private static ItemStack createCategoryHeader(String title, String description) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "» " + title + " «");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an item for a subskill with enhanced visuals
     */
    private static ItemStack createSubskillItem(Skill subskill, SkillLevel level, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Add glow effect for max level subskills
        if (level.getLevel() >= subskill.getMaxLevel()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        // Set display name with level
        meta.setDisplayName(ChatColor.AQUA + "✦ " + subskill.getDisplayName() + " " + 
                        ChatColor.YELLOW + "[Lvl " + level.getLevel() + "]");
        
        List<String> lore = new ArrayList<>();
        
        // Add divider
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Description
        for (String line : subskill.getDescription().split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        
        // Progress information
        if (level.getLevel() < subskill.getMaxLevel()) {
            double xpCurrent = level.getCurrentXp();
            double xpNeeded = subskill.getXpForLevel(level.getLevel() + 1);
            double progress = xpCurrent / xpNeeded;
            
            lore.add(ChatColor.YELLOW + "» Level: " + ChatColor.WHITE + level.getLevel() + "/" + subskill.getMaxLevel());
            lore.add(ChatColor.YELLOW + "» Progress: " + createProgressBar(progress));
            lore.add(ChatColor.YELLOW + "» XP: " + ChatColor.WHITE + 
                    format("%.0f", xpCurrent) + "/" + format("%.0f", xpNeeded));
        } else {
            lore.add(ChatColor.GOLD + "» MAX LEVEL REACHED!");
            lore.add(ChatColor.GOLD + "» All abilities unlocked");
        }
        
        lore.add("");
        
        // Milestones
        String nextMilestone = getNextMilestone(subskill, level.getLevel());
        if (nextMilestone != null) {
            lore.add(ChatColor.AQUA + "» Next Milestone: " + ChatColor.YELLOW + nextMilestone);
            
            // Show what the next milestone will award
            int nextMilestoneLevel = Integer.parseInt(nextMilestone.replaceAll("[^0-9]", ""));
            int tokenReward = calculateTokensForLevel(nextMilestoneLevel);
            if (tokenReward > 0) {
                SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(subskill.getParentSkill());
                lore.add(ChatColor.GRAY + "  Rewards: " + ChatColor.GOLD + tokenReward + " " + 
                        tokenInfo.color + tokenInfo.displayName + " Token" + 
                        (tokenReward > 1 ? "s" : ""));
            }
        } else {
            lore.add(ChatColor.AQUA + "» " + ChatColor.GREEN + "All Milestones Completed!");
        }
        
        // UPDATED: Add interaction instructions
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GREEN + "Left-click" + ChatColor.GRAY + " for skill details");
        lore.add(ChatColor.GREEN + "Right-click" + ChatColor.GRAY + " for specialized info");
        lore.add("");
        lore.add(ChatColor.GRAY + "Tokens from this subskill contribute to");
        lore.add(ChatColor.GRAY + "your " + ChatColor.YELLOW + subskill.getParentSkill().getDisplayName() + 
                ChatColor.GRAY + " skill tree progression.");
        
        // Add subskill ID for click handler
        lore.add(ChatColor.BLACK + "SUBSKILL_ID:" + subskill.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Calculate tokens for a specific level (helper method)
     */
    private static int calculateTokensForLevel(int level) {
        // Special milestones get more tokens
        if (level % 25 == 0) {
            return 5; // Every 25 levels: 25, 50, 75, 100
        } else if (level % 10 == 0) {
            return 3; // Every 10 levels: 10, 20, 30, etc. (except those already covered)
        } else if (level % 5 == 0) {
            return 2; // Every 5 levels: 5, 15, etc. (except those already covered)
        } else {
            return 1; // All other levels
        }
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
     * Creates a progress bar visualization with color gradients
     */
    private static String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 20;
        int filledBars = (int) Math.round(progress * barLength);
        
        // Start with bracket
        bar.append(ChatColor.GRAY + "[");
        
        // Create gradient of colors based on fill
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
     * Create help button
     */
    private static ItemStack createHelpButton() {
        ItemStack helpButton = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta helpMeta = helpButton.getItemMeta();
        helpMeta.setDisplayName(ChatColor.YELLOW + "Information");
        
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.GRAY + "This screen shows all subskills");
        helpLore.add(ChatColor.GRAY + "available for this skill category.");
        helpLore.add("");
        helpLore.add(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Each subskill has unique bonuses");
        helpLore.add(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Level them up separately");
        helpLore.add(ChatColor.YELLOW + "• " + ChatColor.WHITE + "Upgrades are now in the main skill tree");
        helpLore.add("");
        helpLore.add(ChatColor.GREEN + "Left-click" + ChatColor.GRAY + " any subskill to view details");
        
        helpMeta.setLore(helpLore);
        helpButton.setItemMeta(helpMeta);
        return helpButton;
    }
    
    /**
     * Fill empty slots with glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}