package com.server.profiles.skills.gui;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;

/**
 * Listener for skill tree GUI interactions
 */
public class SkillTreeGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Cancel all clicks in skill tree GUI
        if (title.startsWith(SkillTreeGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            
            // Handle clicks on items
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta() || 
                !event.getCurrentItem().getItemMeta().hasDisplayName()) {
                return;
            }
            
            ItemStack clickedItem = event.getCurrentItem();
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            // Handle the corrected back button that goes to skill details
            if (itemName.contains("« Back to") && itemName.contains("Details")) {
                // Extract skill ID from lore
                String skillId = getSkillIdFromBackButton(clickedItem);
                if (skillId != null) {
                    Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                    if (skill != null) {
                        player.closeInventory();
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                        return;
                    }
                }
                
                // Fallback: go to skills menu if skill ID not found
                player.closeInventory();
                SkillsGUI.openSkillsMenu(player);
                return;
            }
            
            // Handle old format back button for backward compatibility
            if (itemName.equals(ChatColor.RED + "« Back to Skills")) {
                player.closeInventory();
                SkillsGUI.openSkillsMenu(player);
                return;
            }
            
            // Check if this is a navigation button
            if (isNavigationButton(clickedItem)) {
                SkillTreeGUI.handleNavigationClick(player, clickedItem);
                return;
            }
            
            // Check if this is the reset button
            if (isResetButton(clickedItem)) {
                // Extract skill name from title
                String skillName = title.substring(SkillTreeGUI.GUI_TITLE_PREFIX.length());
                SkillTreeGUI.handleResetClick(player, skillName);
                return;
            }
            
            // Check if this is a node in the skill tree
            if (isSkillTreeNode(clickedItem)) {
                SkillTreeGUI.handleNodeClick(player, clickedItem);
                return;
            }
        }
    }

    /**
     * Extract skill ID from back button lore
     */
    private String getSkillIdFromBackButton(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "SKILL_ID:")) {
                return line.substring((ChatColor.BLACK + "SKILL_ID:").length());
            }
        }
        
        return null;
    }

    /**
     * Check if an item is the reset button
     */
    private boolean isResetButton(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            if (line.equals(ChatColor.BLACK + "RESET_BUTTON")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if an item is a navigation button
     */
    private boolean isNavigationButton(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "NAVIGATION:")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if an item is a skill tree node
     */
    private boolean isSkillTreeNode(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "ID:")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Handle clicks on the confirmation GUI
     */
    @EventHandler
    public void onConfirmationInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is a confirmation GUI
        if (title.startsWith(ConfirmationGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            
            // Handle clicks on items
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta() || 
                !event.getCurrentItem().getItemMeta().hasDisplayName()) {
                return;
            }
            
            ItemStack clickedItem = event.getCurrentItem();
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            if (itemName.equals(ChatColor.GREEN + "Confirm Reset")) {
                // Handle confirm button click
                ConfirmationGUI.handleConfirmAction(player, clickedItem);
                player.closeInventory();
            } else if (itemName.equals(ChatColor.RED + "Cancel")) {
                // Handle cancel button click - just close the inventory
                player.closeInventory();
                
                // Extract skill name from title
                String skillName = title.substring(ConfirmationGUI.GUI_TITLE_PREFIX.length() + "Reset ".length());
                skillName = skillName.replace(" Tree", ""); // Remove " Tree" suffix
                
                // Find the skill
                Skill skill = findSkillByDisplayName(skillName);
                if (skill != null) {
                    // Reopen the skill tree GUI
                    SkillTreeGUI.openSkillTreeGUI(player, skill);
                }
            }
        }
    }

    /**
     * Find a skill by its display name
     */
    private Skill findSkillByDisplayName(String displayName) {
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (skill.getDisplayName().equals(displayName)) {
                return skill;
            }
        }
        return null;
    }
}