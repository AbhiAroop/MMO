package com.server.profiles.skills.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;

/**
 * Listener for skill GUI interactions
 */
public class SkillGUIListener implements Listener {

    private final Main plugin;
    
    public SkillGUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is any of our skills-related GUI titles
        // Extended to cover all variations including Rewards GUI
        if (title.equals("✦ Skills Menu ✦") || 
            title.startsWith("Skill Details: ") || 
            title.startsWith("Subskills: ") ||
            title.startsWith("Skill Tree: ") ||
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ")) {
            
            // Cancel all interactions to prevent taking items
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
            
            // Handle clicks for specific GUI types
            if (title.equals("✦ Skills Menu ✦")) {
                handleSkillsMenuClick(player, clickedItem);
            } else if (title.startsWith("Skill Details: ")) {
                handleSkillDetailsClick(player, clickedItem, event);
            } else if (title.startsWith("Subskills: ")) {
                handleSubskillsMenuClick(player, clickedItem, event);
            } else if (title.startsWith("Skill Tree: ")) {
                handleSkillTreeClick(player, clickedItem);
            } else if (title.startsWith("Rewards: ")) {
                handleRewardsClick(player, clickedItem);
            } else if (title.startsWith("Milestones: ")) {
                handleMilestonesClick(player, clickedItem, event);
            }
        }
    }
    
    // Add drag event cancellation for all skill-related GUIs
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        
        // Updated to include all skill-related GUIs
        if (title.equals("✦ Skills Menu ✦") || 
            title.startsWith("Skill Details: ") || 
            title.startsWith("Subskills: ") ||
            title.startsWith("Skill Tree: ") ||
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ")) {
            
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle clicks in the main skills menu
     */
    private void handleSkillsMenuClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Back button
        if (displayName.equals(ChatColor.RED + "« Back to Menu")) {
            player.closeInventory();
            
            // Use scheduler to prevent glitches
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ProfileGUI.openMainMenu(player);
            }, 1L);
            
            return;
        }
        
        // Help button
        if (displayName.equals(ChatColor.YELLOW + "How Skills Work")) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Skills Guide" + ChatColor.GREEN + " ===");
            player.sendMessage(ChatColor.YELLOW + "• Skills level up as you perform related activities");
            player.sendMessage(ChatColor.YELLOW + "• Higher skill levels unlock bonuses and abilities");
            player.sendMessage(ChatColor.YELLOW + "• Each main skill has multiple subskills to master");
            player.sendMessage(ChatColor.YELLOW + "• Unlock and upgrade skill tree nodes using tokens");
            player.sendMessage(ChatColor.YELLOW + "• Configure active and passive abilities in the skill menu");
            return;
        }
        
        // Check if this is a skill item
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (displayName.contains(skill.getDisplayName())) {
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                }, 1L);
                
                return;
            }
        }
    }
    
    /**
     * Handle clicks in the skill details menu
     */
    private void handleSkillDetailsClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String title = event.getView().getTitle();
        
        // Back button
        if (displayName.equals(ChatColor.RED + "« Back to Skills")) {
            player.closeInventory();
            
            // Use scheduler to prevent glitches
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SkillsGUI.openSkillsMenu(player);
            }, 1L);
            
            return;
        }
        
        // Subskills button - check for both standard and fancy format
        if (displayName.equals(ChatColor.AQUA + "View Subskills") || 
            displayName.equals(ChatColor.AQUA + "✦ View Subskills")) {
            
            String skillName = title.substring("Skill Details: ".length());
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SubskillsGUI.openSubskillsMenu(player, skill);
                }, 1L);
            }
            
            return;
        }
        
        // Abilities button - handle all variants
        if (displayName.equals(ChatColor.LIGHT_PURPLE + "Skill Abilities") || 
            displayName.equals(ChatColor.LIGHT_PURPLE + "✦ Skill Abilities") ||
            displayName.equals(ChatColor.GOLD + "View Abilities")) {
            
            String skillName = title.substring("Skill Details: ".length());
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                
                // Open the abilities GUI using scheduler
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    com.server.profiles.skills.abilities.gui.AbilitiesGUI.openAbilitiesMenu(player, skill);
                }, 1L);
            }
            
            return;
        }
        
        // Skill Tree button
        if (displayName.contains("Skill Tree")) {
            String skillName = title.substring("Skill Details: ".length());
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SkillTreeGUI.openSkillTreeGUI(player, skill);
                }, 1L);
            }
            
            return;
        }
        
        // Rewards button - handle both variants
        if (displayName.contains("Rewards") || displayName.contains("✦ Skill Rewards")) {
            String skillName = title.substring("Skill Details: ".length());
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    RewardsGUI.openRewardsMenu(player, skill);
                }, 1L);
            }
            
            return;
        }
                
        // Help button
        if (displayName.equals(ChatColor.YELLOW + "Help")) {
            // Just display a tooltip, no navigation
            return;
        }
    }
    
    /**
     * Handle clicks in the subskills menu
     */
    private void handleSubskillsMenuClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Back button
        if (itemName.equals(ChatColor.RED + "Back to Skill Details")) {
            String title = event.getView().getTitle();
            if (title.startsWith("Subskills: ")) {
                String mainSkillName = title.substring("Subskills: ".length());
                Skill mainSkill = findSkillByName(mainSkillName);
                if (mainSkill != null) {
                    player.closeInventory();
                    
                    // Use scheduler to prevent glitches
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, mainSkill);
                    }, 1L);
                }
            }
            return;
        }
        
        // Handle subskill clicks (open subskill details)
        String title = event.getView().getTitle();
        if (title.startsWith("Subskills: ")) {
            for (Skill subskill : SkillRegistry.getInstance().getAllSkills()) {
                if (itemName.contains(subskill.getDisplayName())) {
                    player.closeInventory();
                    
                    // Use scheduler to prevent glitches
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, subskill);
                    }, 1L);
                    
                    return;
                }
            }
        }
    }
    
    /**
     * Handle clicks in the skill tree GUI
     */
    private void handleSkillTreeClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Back button
        if (displayName.equals(ChatColor.RED + "Back to Skills")) {
            player.closeInventory();
            
            // Use scheduler to prevent glitches
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SkillDetailsGUI.openSkillDetailsMenu(player, findSkillByName(
                    player.getOpenInventory().getTitle().substring("Skill Tree: ".length())
                ));
            }, 1L);
            return;
        }
        
        // Node clicks
        if (clickedItem.getItemMeta().getLore() != null) {
            for (String lore : clickedItem.getItemMeta().getLore()) {
                if (lore.startsWith(ChatColor.BLACK + "ID:")) {
                    SkillTreeGUI.handleNodeClick(player, clickedItem);
                    return;
                }
                if (lore.startsWith(ChatColor.BLACK + "DIRECTION:")) {
                    SkillTreeGUI.handleNavigationClick(player, clickedItem);
                    return;
                }
                if (lore.contains(ChatColor.BLACK + "RESET_BUTTON")) {
                    SkillTreeGUI.handleResetClick(player);
                    return;
                }
            }
        }
    }
    
    /**
     * Handle clicks in the rewards GUI
     */
    private void handleRewardsClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String title = player.getOpenInventory().getTitle();
        
        // Only process the back button in the rewards GUI
        if (displayName.equals(ChatColor.RED + "Back to Skill Details")) {
            if (title.startsWith("Rewards: ")) {
                // Extract skill name from GUI title
                String skillName = title.substring("Rewards: ".length());
                Skill skill = findSkillByName(skillName);
                
                if (skill != null) {
                    player.closeInventory();
                    
                    // Use scheduler to prevent glitches
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }, 1L);
                }
            }
        }
    }
    
    /**
     * Handle clicks in the milestones GUI
     */
    private void handleMilestonesClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String title = event.getView().getTitle();
        
        // Back button
        if (displayName.equals(ChatColor.RED + "Back to Skill Details")) {
            if (title.startsWith("Milestones: ")) {
                String skillName = title.substring("Milestones: ".length());
                Skill skill = findSkillByName(skillName);
                
                if (skill != null) {
                    player.closeInventory();
                    
                    // Use scheduler to prevent glitches
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }, 1L);
                }
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