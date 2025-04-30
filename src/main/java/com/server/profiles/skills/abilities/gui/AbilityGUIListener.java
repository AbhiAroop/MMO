package com.server.profiles.skills.abilities.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.SkillAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.gui.SkillDetailsGUI;
import com.server.profiles.skills.gui.SkillsGUI;

/**
 * Listener for ability GUI interactions
 */
public class AbilityGUIListener implements Listener {
    
    private final Main plugin;
    
    public AbilityGUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is one of our ability GUIs
        if (title.startsWith(AbilitiesGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleAbilitiesMenuClick(player, event.getCurrentItem(), event.getClick());
        }
        else if (title.startsWith("All Passive Abilities: ") || 
                title.startsWith("All Active Abilities: ") ||
                title.startsWith("Unlocked Passive Abilities: ") || 
                title.startsWith("Unlocked Active Abilities: ")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleAbilityListClick(player, event.getCurrentItem(), title);
        }
    }
    
    /**
     * Handle clicks in the abilities menu
     */
    private void handleAbilitiesMenuClick(Player player, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String title = player.getOpenInventory().getTitle();
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle passive abilities button
        if (clickedItem.getType() == Material.REDSTONE_TORCH && 
            displayName.equals(ChatColor.GOLD + "Passive Abilities")) {
            
            // Extract skill ID from lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            if (skillId == null) {
                // Try to extract from title if lore is not available
                String skillName = "";
                if (title != null && title.startsWith("Abilities: ")) {
                    skillName = title.substring("Abilities: ".length());
                    Skill skill = findSkillByName(skillName);
                    if (skill != null) {
                        skillId = skill.getId();
                    }
                }
            }
            
            if (skillId != null) {
                player.closeInventory();
                
                // Open the passive abilities menu based on click type
                boolean showAll = clickType.isRightClick();
                final String skillIdString = skillId;
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilityListGUI.openAbilityList(player, skillIdString, "PASSIVE", showAll);
                }, 1L);
            }
            
            return;
        }
        
        // Handle active abilities button
        if (clickedItem.getType() == Material.BLAZE_POWDER && 
            displayName.equals(ChatColor.GOLD + "Active Abilities")) {
            
            // Extract skill ID from lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            if (skillId == null) {
                // Try to extract from title if lore is not available
                String skillName = "";
                if (title != null && title.startsWith("Abilities: ")) {
                    skillName = title.substring("Abilities: ".length());
                    Skill skill = findSkillByName(skillName);
                    if (skill != null) {
                        skillId = skill.getId();
                    }
                }
            }
            
            if (skillId != null) {
                player.closeInventory();
                
                // Open the active abilities menu based on click type
                boolean showAll = clickType.isRightClick();
                final String skillIdString = skillId;
                
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilityListGUI.openAbilityList(player, skillIdString, "ACTIVE", showAll);
                }, 1L);
            }
            
            return;
        }
        
        // Handle back button
        if (clickedItem.getType() == Material.ARROW && 
            displayName.equals(ChatColor.RED + "Back to Skill Details")) {
            player.closeInventory();
            
            // Get skill name from title
            if (title != null && title.startsWith("Abilities: ") && title.length() > "Abilities: ".length()) {
                String skillName = title.substring("Abilities: ".length());
                Skill skill = findSkillByName(skillName);
                
                if (skill != null) {
                    // Short delay to prevent inventory conflicts
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }, 1L);
                } else {
                    // Fallback - return to main skills menu if we can't determine the skill
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillsGUI.openSkillsMenu(player);
                    }, 1L);
                }
            } else {
                // Fallback - return to main skills menu
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SkillsGUI.openSkillsMenu(player);
                }, 1L);
            }
            
            return;
        }
    }

    /**
     * Find a skill by its display name
     */
    private Skill findSkillByName(String name) {
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (skill.getDisplayName().equals(name)) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * Handle clicks in the ability list GUI
     */
    private void handleAbilityListClick(Player player, ItemStack clickedItem, String title) {
        // Handle toggle button click
        String toggleShowAll = extractValueFromLore(clickedItem, "TOGGLE_SHOW_ALL:");
        if (toggleShowAll != null) {
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
            
            if (skillId != null && abilityType != null) {
                player.closeInventory();
                
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean showAll = Boolean.parseBoolean(toggleShowAll);
                    AbilityListGUI.openAbilityList(player, skillId, abilityType, showAll);
                }, 1L);
            }
            return;
        }
        
        // Handle back button click
        if (clickedItem.getType() == org.bukkit.Material.ARROW && 
            clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Back to Abilities")) {
            player.closeInventory();
            
            // Extract skill name from title
            String skillName = extractSkillNameFromTitle(title);
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilitiesGUI.openAbilitiesMenu(player, skill);
                }, 1L);
            }
            return;
        }
        
        // Handle ability item click
        String abilityId = extractValueFromLore(clickedItem, "ABILITY:");
        String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
        
        if (abilityId != null && "PASSIVE".equals(abilityType)) {
            // Toggle passive ability
            AbilityRegistry registry = AbilityRegistry.getInstance();
            SkillAbility ability = registry.getAbility(abilityId);
            
            if (ability instanceof PassiveAbility && ability.isUnlocked(player)) {
                PassiveAbility passiveAbility = (PassiveAbility) ability;
                boolean newState = passiveAbility.toggleEnabled(player);
                
                // Play sound
                player.playSound(player.getLocation(), 
                                newState ? Sound.BLOCK_WOODEN_BUTTON_CLICK_ON : Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 
                                0.5f, newState ? 1.2f : 0.8f);
                
                // Update the GUI
                boolean showAll = title.startsWith("All ");
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                
                if (skillId != null) {
                    player.closeInventory();
                    
                    // Short delay to prevent inventory conflicts
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        AbilityListGUI.openAbilityList(player, skillId, "PASSIVE", showAll);
                    }, 1L);
                }
            }
        }
    }
    
    /**
     * Extract a skill name from a GUI title
     */
    private String extractSkillNameFromTitle(String title) {
        // Title format: "Unlocked/All Passive/Active Abilities: [skill name]"
        int colonIndex = title.indexOf(":");
        if (colonIndex != -1 && colonIndex < title.length() - 2) {
            return title.substring(colonIndex + 2);
        }
        return null;
    }
    
    /**
     * Extract a value from an item's lore with a specific prefix
     */
    private String extractValueFromLore(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.contains(prefix)) {
                int startIndex = line.indexOf(prefix) + prefix.length();
                if (startIndex < line.length()) {
                    return line.substring(startIndex);
                }
            }
        }
        
        return null;
    }

}