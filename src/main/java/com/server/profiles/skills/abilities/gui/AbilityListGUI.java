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

import com.server.Main;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.SkillAbility;
import com.server.profiles.skills.abilities.active.ActiveAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;

/**
 * GUI for listing passive or active abilities
 */
public class AbilityListGUI {
    
    public static final String GUI_TITLE_PREFIX_UNLOCKED = "Unlocked ";
    public static final String GUI_TITLE_PREFIX_ALL = "All ";
    
    /**
     * Open the list of abilities for a skill
     * 
     * @param player The player to show the GUI to
     * @param skillId The skill ID to show abilities for
     * @param abilityType The type of abilities to show ("PASSIVE" or "ACTIVE")
     * @param showAll Whether to show all abilities or just unlocked ones
     */
    public static void openAbilityList(Player player, String skillId, String abilityType, boolean showAll) {
            // Get the skill
            Skill skill = SkillRegistry.getInstance().getSkill(skillId);
            if (skill == null) {
                // Log error
                Main.getInstance().getLogger().warning("Attempt to open ability list for unknown skill ID: " + skillId);
                player.sendMessage(ChatColor.RED + "Error: Skill not found.");
                return;
            }
            
            // Debug logging
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Opening ability list for " + player.getName() + 
                                                ", skill: " + skillId + 
                                                ", type: " + abilityType + 
                                                ", showAll: " + showAll);
            }
            
            // Dump abilities in debug mode
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("=== Available Abilities Debug Dump ===");
                Main.getInstance().getLogger().info("Checking abilities for skill: " + skillId);
                
                // Check parent skill ID if this is a subskill
                String parentSkillId = null;
                if (!skill.isMainSkill() && skill.getParentSkill() != null) {
                    parentSkillId = skill.getParentSkill().getId();
                    Main.getInstance().getLogger().info("This is a subskill. Parent skill: " + parentSkillId);
                }
                
                // Log all registered abilities
                AbilityRegistry registry = AbilityRegistry.getInstance();
                
                // Check passive abilities
                Main.getInstance().getLogger().info("Passive abilities registered for " + skillId + ":");
                List<PassiveAbility> passives = registry.getPassiveAbilities(skillId);
                for (PassiveAbility ability : passives) {
                    Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                    Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
                }
                
                // Check active abilities
                Main.getInstance().getLogger().info("Active abilities registered for " + skillId + ":");
                List<ActiveAbility> actives = registry.getActiveAbilities(skillId);
                for (ActiveAbility ability : actives) {
                    Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                    Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
                }
                
                // Check parent skills if this is a subskill
                if (parentSkillId != null) {
                    Main.getInstance().getLogger().info("Checking parent skill abilities for " + parentSkillId + ":");
                    
                    // Check passive abilities for parent
                    Main.getInstance().getLogger().info("Parent passive abilities:");
                    List<PassiveAbility> parentPassives = registry.getPassiveAbilities(parentSkillId);
                    for (PassiveAbility ability : parentPassives) {
                        Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                        Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
                    }
                    
                    // Check active abilities for parent
                    Main.getInstance().getLogger().info("Parent active abilities:");
                    List<ActiveAbility> parentActives = registry.getActiveAbilities(parentSkillId);
                    for (ActiveAbility ability : parentActives) {
                        Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                        Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
                    }
                }
                
                Main.getInstance().getLogger().info("=== End of Abilities Debug Dump ===");
            }
            
            // Create title based on type and mode
            String title = (showAll ? GUI_TITLE_PREFIX_ALL : GUI_TITLE_PREFIX_UNLOCKED) + 
                        abilityType.charAt(0) + abilityType.substring(1).toLowerCase() + 
                        " Abilities: " + skill.getDisplayName();
            
            // Create inventory
            Inventory gui = Bukkit.createInventory(null, 54, title);
            
            // Get the abilities
            AbilityRegistry registry = AbilityRegistry.getInstance();
            List<? extends SkillAbility> abilities;
            
            if ("PASSIVE".equals(abilityType)) {
                if (showAll) {
                    abilities = registry.getPassiveAbilities(skillId);
                } else {
                    abilities = registry.getUnlockedPassiveAbilities(player, skillId);
                }
            } else if ("ACTIVE".equals(abilityType)) {
                if (showAll) {
                    abilities = registry.getActiveAbilities(skillId);
                } else {
                    abilities = registry.getUnlockedActiveAbilities(player, skillId);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Error: Invalid ability type.");
                return;
            }
        
        // Debug log abilities found
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Found " + abilities.size() + " abilities for skill " + skillId + " of type " + abilityType);
            for (SkillAbility ability : abilities) {
                Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            }
        }
        
        // Add abilities to GUI
        if (abilities.isEmpty()) {
            // Create empty message
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = emptyItem.getItemMeta();
            emptyMeta.setDisplayName(ChatColor.RED + "No Abilities Found");
            
            List<String> emptyLore = new ArrayList<>();
            if (showAll) {
                emptyLore.add(ChatColor.GRAY + "There are no " + abilityType.toLowerCase() + " abilities");
                emptyLore.add(ChatColor.GRAY + "available for this skill yet.");
            } else {
                emptyLore.add(ChatColor.GRAY + "You haven't unlocked any " + abilityType.toLowerCase());
                emptyLore.add(ChatColor.GRAY + "abilities for this skill yet.");
                emptyLore.add("");
                emptyLore.add(ChatColor.YELLOW + "Right-click to view all available abilities");
                emptyLore.add(ChatColor.YELLOW + "and how to unlock them!");
            }
            
            emptyMeta.setLore(emptyLore);
            emptyItem.setItemMeta(emptyMeta);
            gui.setItem(22, emptyItem);
        } else {
            // Add each ability item
            int slot = 10;
            for (SkillAbility ability : abilities) {
                ItemStack abilityItem = ability.createDisplayItem(player);
                gui.setItem(slot, abilityItem);
                
                // Increment slot, skipping the border
                slot++;
                if (slot % 9 == 8) slot += 2;
                if (slot >= 36) break; // Maximum of 21 abilities (3 rows)
            }
        }
        
        // Add info item
        ItemStack infoItem = createInfoItem(skillId, abilityType, showAll, abilities.size());
        gui.setItem(4, infoItem);
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Abilities");
        
        // Add skill ID to lore for back navigation
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.BLACK + "SKILL:" + skillId);
        backMeta.setLore(backLore);
        
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);
        
        // Add toggle button
        ItemStack toggleButton = new ItemStack(Material.MAP);
        ItemMeta toggleMeta = toggleButton.getItemMeta();
        toggleMeta.setDisplayName(showAll ? 
            ChatColor.AQUA + "Show Unlocked Abilities" : 
            ChatColor.AQUA + "Show All Abilities");
        
        List<String> toggleLore = new ArrayList<>();
        toggleLore.add(ChatColor.GRAY + "Click to " + (showAll ? 
            "show only abilities you've unlocked" : 
            "show all abilities, including locked ones"));
        
        // Add skill ID and ability type for identification in GUI handler
        toggleLore.add(ChatColor.BLACK + "SKILL:" + skillId);
        toggleLore.add(ChatColor.BLACK + "ABILITY_TYPE:" + abilityType);
        toggleLore.add(ChatColor.BLACK + "TOGGLE_SHOW_ALL:" + !showAll);
        
        toggleMeta.setLore(toggleLore);
        toggleButton.setItemMeta(toggleMeta);
        gui.setItem(49, toggleButton);
        
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
     * Create info item for the GUI
     */
    private static ItemStack createInfoItem(String skillId, String abilityType, boolean showAll, int abilityCount) {
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) return new ItemStack(Material.BARRIER);
        
        Material icon = Material.BOOK;
        if (skill.getDisplayName().equalsIgnoreCase("Mining")) {
            icon = Material.DIAMOND_PICKAXE;
        } else if (skill.getDisplayName().equalsIgnoreCase("Ore Extraction")) {
            icon = Material.IRON_ORE;
        } else if (skill.getDisplayName().equalsIgnoreCase("Gem Carving")) {
            icon = Material.EMERALD;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName() + " " + abilityType + " Abilities");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Viewing " + (showAll ? "all" : "unlocked") + " " + 
                abilityType.toLowerCase() + " abilities");
        lore.add(ChatColor.GRAY + "for " + skill.getDisplayName() + ".");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Total abilities: " + ChatColor.WHITE + abilityCount);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * Create an item representation of an ability for the GUI
     */
    private static ItemStack createAbilityItem(SkillAbility ability, Player player) {
        ItemStack item = ability.createDisplayItem(player);
        ItemMeta meta = item.getItemMeta();
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Add the ability ID for tracking
        lore.add(ChatColor.BLACK + "ABILITY:" + ability.getId());
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + (ability instanceof PassiveAbility ? "PASSIVE" : "ACTIVE"));
        lore.add(ChatColor.BLACK + "SKILL:" + ability.getSkillId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
}