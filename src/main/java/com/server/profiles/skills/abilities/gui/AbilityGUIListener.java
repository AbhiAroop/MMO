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
import com.server.profiles.skills.abilities.active.ActiveAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;

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
        else if (clickedItem.getType() == Material.BLAZE_POWDER && 
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
        else if (clickedItem.getType() == Material.ARROW && 
                displayName.equals(ChatColor.RED + "Back to Skill Details")) {
            player.closeInventory();
            
            // Get skill ID from button lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            
            // If no direct ID, try from title
            if (skillId == null && title != null && title.startsWith("Abilities: ")) {
                String skillName = title.substring("Abilities: ".length());
                Skill skill = findSkillByName(skillName);
                if (skill != null) {
                    skillId = skill.getId();
                }
            }
            
            if (skillId != null) {
                final Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                if (skill != null) {
                    // Short delay to prevent inventory conflicts
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        com.server.profiles.skills.gui.SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }, 1L);
                    return;
                }
            }
            
            // Fallback if we couldn't determine the skill
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                com.server.profiles.skills.gui.SkillsGUI.openSkillsMenu(player);
            }, 1L);
        }
    }

    /**
     * Find a skill by its display name
     */
    private Skill findSkillByName(String name) {
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (skill.getDisplayName().equalsIgnoreCase(name)) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * Handle clicks in the ability list GUI
     */
    private void handleAbilityListClick(Player player, ItemStack clickedItem, String title) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        // Check if this is an ability item
        String abilityId = extractValueFromLore(clickedItem, "ABILITY:");
        if (abilityId == null) {
            // Handle other types of clicks
            String toggleShowAll = extractValueFromLore(clickedItem, "TOGGLE_SHOW_ALL:");
            if (toggleShowAll != null) {
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
                
                if (skillId != null && abilityType != null) {
                    boolean showAll = Boolean.parseBoolean(toggleShowAll);
                    AbilityListGUI.openAbilityList(player, skillId, abilityType, showAll);
                }
                return;
            }
            
            // Handle back button
            if (clickedItem.getType() == Material.ARROW && 
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Back to Abilities")) {
                player.closeInventory();
                
                // Extract skill ID from lore
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                
                // If no ID in lore, try to extract from title
                if (skillId == null) {
                    // Extract skill name from title
                    String skillName = extractSkillNameFromTitle(title);
                    if (skillName != null) {
                        Skill skill = findSkillByName(skillName);
                        if (skill != null) {
                            skillId = skill.getId();
                        }
                    }
                }
                
                if (skillId != null) {
                    final Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                    if (skill != null) {
                        // Short delay to prevent inventory conflicts
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            AbilitiesGUI.openAbilitiesMenu(player, skill);
                        }, 1L);
                        return;
                    }
                }
                
                // Fallback if we couldn't determine the skill
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    com.server.profiles.skills.gui.SkillsGUI.openSkillsMenu(player);
                }, 1L);
                return;
            }
            return;
        }
        
        // Get the ability
        SkillAbility ability = AbilityRegistry.getInstance().getAbility(abilityId);
        if (ability == null) {
            return;
        }
        
        // If it's a passive ability, toggle it
        if (ability instanceof PassiveAbility) {
            PassiveAbility passiveAbility = (PassiveAbility) ability;
            
            // Check if the ability is unlocked
            if (passiveAbility.isUnlocked(player)) {
                boolean newState = passiveAbility.toggleEnabled(player);
                
                // Play sound based on the new state
                player.playSound(player.getLocation(), 
                    newState ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS, 
                    0.5f, newState ? 1.5f : 0.8f);
                
                // Update the GUI to show the new state
                String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                if (skillId != null && abilityType != null) {
                    // Reopen the ability list with the updated state
                    boolean showAll = title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_ALL);
                    AbilityListGUI.openAbilityList(player, skillId, abilityType, showAll);
                }
            } else {
                // Tell the player how to unlock this ability
                player.sendMessage(ChatColor.RED + "You haven't unlocked this ability yet!");
                player.sendMessage(ChatColor.YELLOW + "To unlock: " + ChatColor.WHITE + passiveAbility.getUnlockRequirement());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        } else if (ability instanceof ActiveAbility) {
            // Handle active abilities info display
            ActiveAbility activeAbility = (ActiveAbility) ability;
            player.sendMessage(ChatColor.AQUA + "===== " + ChatColor.GOLD + activeAbility.getDisplayName() + ChatColor.AQUA + " =====");
            player.sendMessage(ChatColor.GREEN + "This is an active ability!");
            player.sendMessage(ChatColor.YELLOW + "Activation: " + ChatColor.WHITE + activeAbility.getActivationMethod());
            player.sendMessage(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + activeAbility.getCooldownSeconds() + " seconds");
            
            // Show cooldown if on cooldown
            if (activeAbility.isOnCooldown(player)) {
                long remaining = activeAbility.getCooldownRemaining(player) / 1000; // Convert to seconds
                player.sendMessage(ChatColor.RED + "On cooldown: " + remaining + " seconds remaining");
            }
            
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        }
    }
    
    /**
     * Extract a skill name from a GUI title
     */
    private String extractSkillNameFromTitle(String title) {
        if (title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_UNLOCKED)) {
            // Format: "Unlocked [Type] Abilities: [Skill]"
            int colonIndex = title.indexOf(": ");
            if (colonIndex >= 0 && colonIndex + 2 < title.length()) {
                return title.substring(colonIndex + 2);
            }
        }
        else if (title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_ALL)) {
            // Format: "All [Type] Abilities: [Skill]"
            int colonIndex = title.indexOf(": ");
            if (colonIndex >= 0 && colonIndex + 2 < title.length()) {
                return title.substring(colonIndex + 2);
            }
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