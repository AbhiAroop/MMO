package com.server.profiles.skills.abilities.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.core.Skill;

/**
 * GUI for browsing passive and active abilities for a skill
 */
public class AbilitiesGUI {
    
    // Keep title prefix the same to maintain compatibility with GUI listeners
    public static final String GUI_TITLE_PREFIX = "Abilities: ";
    
    /**
     * Open the abilities selector menu for a skill
     * 
     * @param player The player to show the GUI to
     * @param skill The skill to show abilities for
     */
    public static void openAbilitiesMenu(Player player, Skill skill) {
        // Create inventory with 36 slots for a better layout
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Create decorative border
        createBorder(gui);
        
        // Create skill info item for the top center
        ItemStack skillInfoItem = createSkillInfoItem(skill, player);
        gui.setItem(4, skillInfoItem);
        
        // Create passive abilities item with enhanced design
        ItemStack passiveItem = createTypeItem(
            Material.REDSTONE_TORCH,
            "✦ Passive Abilities",
            "Abilities that are always active or\ncan be toggled on/off",
            skill.getId(),
            player,
            "PASSIVE"
        );
        gui.setItem(11, passiveItem);
        
        // Create active abilities item with enhanced design
        ItemStack activeItem = createTypeItem(
            Material.BLAZE_POWDER,
            "✦ Active Abilities",
            "Abilities that you can trigger\non demand with special effects",
            skill.getId(),
            player,
            "ACTIVE"
        );
        gui.setItem(15, activeItem);
        
        // Add help/tips item in the center
        ItemStack tipsItem = createTipsItem();
        gui.setItem(22, tipsItem);
        
        // Add back button with improved design
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to Skill Details");
        
        // Add hidden data to identify which skill to go back to
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to skill details screen");
        backLore.add("");
        backLore.add(ChatColor.BLACK + "SKILL:" + skill.getId());
        backMeta.setLore(backLore);
        
        backButton.setItemMeta(backMeta);
        gui.setItem(27, backButton);
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui);
        
        // Open the inventory
        player.openInventory(gui);
    }
    
    /**
     * Create tips item to help players understand abilities
     */
    private static ItemStack createTipsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ About Abilities ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Abilities are special powers that");
        lore.add(ChatColor.GRAY + "enhance your skill performance.");
        lore.add("");
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Types of Abilities:");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "Passive: " + 
                ChatColor.GRAY + "Always active or toggleable");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "Active: " + 
                ChatColor.GRAY + "Trigger manually for effects");
        lore.add("");
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "How to Unlock:");
        lore.add(ChatColor.GRAY + "Abilities are unlocked through the");
        lore.add(ChatColor.GRAY + "skill tree or reaching specific levels");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create item showing information about a skill
     */
    private static ItemStack createSkillInfoItem(Skill skill, Player player) {
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
                icon = Material.EMERALD;
                break;
            default:
                icon = Material.NETHER_STAR;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with enhanced formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ " + skill.getDisplayName() + " Abilities ✦");
        
        // Add enchanted glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Create lore with better formatting
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Browse and manage special abilities");
        lore.add(ChatColor.GRAY + "for your " + ChatColor.YELLOW + skill.getDisplayName() + ChatColor.GRAY + " skill.");
        lore.add("");
        
        // Get counts of unlocked abilities with visual indicators
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int unlockedPassive = registry.getUnlockedPassiveAbilities(player, skill.getId()).size();
        int totalPassive = registry.getPassiveAbilities(skill.getId()).size();
        int unlockedActive = registry.getUnlockedActiveAbilities(player, skill.getId()).size();
        int totalActive = registry.getActiveAbilities(skill.getId()).size();
        
        // Add progress bars for better visual representation
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Passive Abilities:");
        lore.add(createProgressBar(unlockedPassive, totalPassive) + " " + 
                ChatColor.GREEN + unlockedPassive + "/" + totalPassive);
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Active Abilities:");
        lore.add(createProgressBar(unlockedActive, totalActive) + " " + 
                ChatColor.GREEN + unlockedActive + "/" + totalActive);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create an item for accessing a specific type of abilities
     */
    private static ItemStack createTypeItem(Material icon, String name, String description, 
                                          String skillId, Player player, String abilityType) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with improved formatting
        meta.setDisplayName(ChatColor.GOLD + name);
        
        // Get counts of unlocked abilities
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int unlocked = 0;
        int total = 0;
        
        if ("PASSIVE".equals(abilityType)) {
            unlocked = registry.getUnlockedPassiveAbilities(player, skillId).size();
            total = registry.getPassiveAbilities(skillId).size();
        } else if ("ACTIVE".equals(abilityType)) {
            unlocked = registry.getUnlockedActiveAbilities(player, skillId).size();
            total = registry.getActiveAbilities(skillId).size();
        }
        
        // Add glow effect if abilities are unlocked
        if (unlocked > 0) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Create lore with better formatting
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Format description with line breaks
        for (String line : description.split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        
        // Show progress with visual indicator
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Unlocked Abilities:");
        lore.add(createProgressBar(unlocked, total) + " " + 
                ChatColor.GREEN + unlocked + "/" + total);
        
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "LEFT-CLICK: " + 
                ChatColor.YELLOW + "View Unlocked Abilities");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "RIGHT-CLICK: " + 
                ChatColor.YELLOW + "View All Abilities");
        
        // Add skill ID and ability type for identification in GUI handler
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + abilityType);
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a visual progress bar
     */
    private static String createProgressBar(int value, int max) {
        StringBuilder bar = new StringBuilder();
        int barLength = 10;
        int filledBars = max > 0 ? (int) Math.round((double) value / max * barLength) : 0;
        
        bar.append(ChatColor.GRAY + "[");
        
        // Create gradient color based on progress
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if ((double) value / max < 0.33) {
                    bar.append(ChatColor.RED);
                } else if ((double) value / max < 0.66) {
                    bar.append(ChatColor.GOLD);
                } else {
                    bar.append(ChatColor.GREEN);
                }
                bar.append("■");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("■");
            }
        }
        
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
        gui.setItem(gui.getSize() - 9, corner);
        gui.setItem(gui.getSize() - 1, corner);
        
        // Top and bottom rows
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : cyan);
            gui.setItem(gui.getSize() - 9 + i, i % 2 == 0 ? blue : cyan);
        }
        
        // Side borders
        for (int i = 1; i <= 2; i++) {
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