package com.server.profiles.skills.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;

/**
 * Listener for skill GUI interactions
 */
public class SkillGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Cancel all clicks in any skill GUI
        if (title.equals("Skills") || 
            title.startsWith("Skill: ") || 
            title.startsWith("Subskills: ") || 
            title.startsWith("Rewards: ")) {
            event.setCancelled(true);
            
            // Handle clicks
            if (event.getCurrentItem() == null) return;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
            
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            // Handle back buttons
            if (itemName.equals(ChatColor.RED + "Back to Menu")) {
                player.closeInventory();
                ProfileGUI.openMainMenu(player);
                return;
            }
            
            if (itemName.equals(ChatColor.RED + "Back to Skills")) {
                player.closeInventory();
                SkillsGUI.openSkillsMenu(player);
                return;
            }
            
            if (itemName.equals(ChatColor.RED + "Back to Skill Details")) {
                String skillName = title.substring(title.indexOf(": ") + 2);
                Skill skill = findSkillByName(skillName);
                if (skill != null) {
                    player.closeInventory();
                    SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                }
                return;
            }
            
            // Main Skills menu
            if (title.equals("Skills")) {
                handleSkillsMenuClick(player, clickedItem);
            }
            else if (title.startsWith("Skill: ")) {
                // Extract skill name from title
                String skillName = title.substring(7); // "Skill: " is 7 characters
                handleSkillDetailsMenuClick(player, clickedItem, skillName);
            }
            else if (title.startsWith("Subskills: ")) {
                handleSubskillsMenuClick(player, clickedItem);
            }
            else if (title.startsWith("Rewards: ")) {
                // Handle rewards menu clicks if needed
                String skillName = title.substring(9); // "Rewards: " is 9 characters
                if (itemName.equals(ChatColor.RED + "Back to Skill Details")) {
                    Skill skill = findSkillByName(skillName);
                    if (skill != null) {
                        player.closeInventory();
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }
                }
            }
        }
        
        // Handle profile menu click to open skills menu
        if (title.equals("Player Menu") && 
            event.getCurrentItem() != null && 
            event.getCurrentItem().hasItemMeta() && 
            event.getCurrentItem().getItemMeta().hasDisplayName() &&
            event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "§a§lSkills")) {
            
            event.setCancelled(true);
            player.closeInventory();
            SkillsGUI.openSkillsMenu(player);
        }
    }
    
    /**
     * Handle clicks in the main skills menu
     */
    private void handleSkillsMenuClick(Player player, ItemStack clickedItem) {
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Check if clicked on a skill
        for (SkillType type : SkillType.values()) {
            if (itemName.startsWith(ChatColor.GOLD + type.getDisplayName())) {
                Skill skill = SkillRegistry.getInstance().getSkill(type);
                if (skill != null) {
                    player.closeInventory();
                    SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                }
                return;
            }
        }
    }
    
    /**
     * Handle clicks in the skill details menu
     */
    private void handleSkillDetailsMenuClick(Player player, ItemStack clickedItem, String skillName) {
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        Skill skill = findSkillByName(skillName);
        if (skill == null) return;
        
        // View subskills button
        if (itemName.equals(ChatColor.AQUA + "View Subskills")) {
            player.closeInventory();
            SubskillsGUI.openSubskillsMenu(player, skill);
            return;
        }
        
        // View rewards button
        if (itemName.equals(ChatColor.GOLD + "Skill Rewards")) {
            player.closeInventory();
            RewardsGUI.openRewardsMenu(player, skill);
            return;
        }
    }
    
    /**
     * Handle clicks in the subskills menu
     */
    private void handleSubskillsMenuClick(Player player, ItemStack clickedItem) {
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Check if clicked on a subskill
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (!skill.isMainSkill() && itemName.startsWith(ChatColor.AQUA + skill.getDisplayName())) {
                player.closeInventory();
                SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                return;
            }
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
}