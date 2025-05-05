package com.server.profiles.skills.abilities.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.core.Skill;

/**
 * GUI for browsing passive and active abilities for a skill
 */
public class AbilitiesGUI {
    
    public static final String GUI_TITLE_PREFIX = "Abilities: ";
    
    /**
     * Open the abilities selector menu for a skill
     * 
     * @param player The player to show the GUI to
     * @param skill The skill to show abilities for
     */
    public static void openAbilitiesMenu(Player player, Skill skill) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Create skill info item
        ItemStack skillInfoItem = createSkillInfoItem(skill, player);
        gui.setItem(4, skillInfoItem);
        
        // Create passive abilities item
        ItemStack passiveItem = createTypeItem(
            Material.REDSTONE_TORCH,
            "Passive Abilities",
            "Abilities that are always active or can be toggled on/off",
            skill.getId(),
            player,
            "PASSIVE"
        );
        gui.setItem(11, passiveItem);
        
        // Create active abilities item
        ItemStack activeItem = createTypeItem(
            Material.BLAZE_POWDER,
            "Active Abilities",
            "Abilities that you can trigger on demand",
            skill.getId(),
            player,
            "ACTIVE"
        );
        gui.setItem(15, activeItem);
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Skill Details");
        
        // Add hidden data to identify which skill to go back to
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.BLACK + "SKILL:" + skill.getId());
        backMeta.setLore(backLore);
        
        backButton.setItemMeta(backMeta);
        gui.setItem(18, backButton);
        
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
        
        // Open the inventory
        player.openInventory(gui);
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
            default:
                icon = Material.NETHER_STAR;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName() + " Abilities");
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Browse and manage abilities for");
        lore.add(ChatColor.GRAY + "your " + ChatColor.YELLOW + skill.getDisplayName() + ChatColor.GRAY + " skill.");
        lore.add("");
        
        // Get counts of unlocked abilities
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int unlockedPassive = registry.getUnlockedPassiveAbilities(player, skill.getId()).size();
        int totalPassive = registry.getPassiveAbilities(skill.getId()).size();
        int unlockedActive = registry.getUnlockedActiveAbilities(player, skill.getId()).size();
        int totalActive = registry.getActiveAbilities(skill.getId()).size();
        
        lore.add(ChatColor.YELLOW + "Passive Abilities: " + 
                ChatColor.GREEN + unlockedPassive + "/" + totalPassive);
        lore.add(ChatColor.YELLOW + "Active Abilities: " + 
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
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + name);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        
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
        
        lore.add(ChatColor.YELLOW + "Unlocked: " + 
                ChatColor.GREEN + unlocked + "/" + total);
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "LEFT-CLICK: " + 
                ChatColor.YELLOW + "View Unlocked Abilities");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.WHITE + "RIGHT-CLICK: " + 
                ChatColor.YELLOW + "View All Abilities");
        
        // Add skill ID and ability type for identification in GUI handler
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + abilityType);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    
}