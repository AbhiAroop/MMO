package com.server.profiles.skills.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import com.server.profiles.skills.trees.SkillTreeRegistry;

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
        // Extended to cover all variations including SubskillDetails GUI
        if (title.equals("✦ Skills Menu ✦") || 
            title.startsWith("Skill Details: ") || 
            title.startsWith("Subskills: ") ||
            title.startsWith("Skill Tree: ") ||
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ") ||
            (title.contains("Details") && title.startsWith(ChatColor.GOLD + "✦"))) { // Catch SubskillDetails GUI
            
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
            } else if (title.contains("Details") && title.startsWith(ChatColor.GOLD + "✦")) {
                // Handle SubskillDetails GUI clicks - specifically for the back button
                handleSubskillDetailGUIClick(player, clickedItem);
            }
        }
    }
    
    // Add drag event cancellation for all skill-related GUIs
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        
        // Updated to include all skill-related GUIs including SubskillDetails
        if (title.equals("✦ Skills Menu ✦") || 
            title.startsWith("Skill Details: ") || 
            title.startsWith("Subskills: ") ||
            title.startsWith("Skill Tree: ") ||
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ") ||
            (title.contains("Details") && title.startsWith(ChatColor.GOLD + "✦"))) {
            
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
        
        // Check for detailed subskill info button
        if (displayName.startsWith(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Detailed ") && 
            displayName.endsWith(" Info")) {
            
            // Extract subskill ID from the lore
            String subskillId = null;
            
            if (clickedItem.getItemMeta().hasLore()) {
                List<String> lore = clickedItem.getItemMeta().getLore();
                for (String line : lore) {
                    if (line.startsWith(ChatColor.BLACK + "VIEW_DETAILS:")) {
                        subskillId = line.substring((ChatColor.BLACK + "VIEW_DETAILS:").length());
                        break;
                    }
                }
            }
            
            if (subskillId != null) {
                final String finalSubskillId = subskillId;
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Skill subskill = SkillRegistry.getInstance().getSkill(finalSubskillId);
                    if (subskill != null) {
                        // Store this as the recent subskill for this category
                        if (subskill.getParentSkill() != null) {
                            String metadataKey = "recent_subskill_" + subskill.getParentSkill().getId();
                            player.setMetadata(metadataKey, new org.bukkit.metadata.FixedMetadataValue(plugin, subskill.getId()));
                        }
                        
                        SubskillDetailsGUI.openSubskillDetailsGUI(player, subskill);
                    } else {
                        player.sendMessage(ChatColor.RED + "Error: Could not find subskill with ID " + finalSubskillId);
                    }
                }, 1L);
                
                return;
            }
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
        
        // Check for detailed info button click
        if (itemName.startsWith(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Detailed ") && 
            itemName.endsWith(" Info")) {
            
            // Extract subskill ID from the lore
            String subskillId = getDetailViewSkillId(clickedItem);
            if (subskillId != null) {
                final String finalSubskillId = subskillId;
                player.closeInventory();
                
                // Use scheduler to prevent glitches
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Skill subskill = SkillRegistry.getInstance().getSkill(finalSubskillId);
                    if (subskill != null) {
                        // Store this as the recent subskill for this category
                        // Use player metadata instead of profile metadata
                        if (subskill.getParentSkill() != null) {
                            String metadataKey = "recent_subskill_" + subskill.getParentSkill().getId();
                            player.setMetadata(metadataKey, new org.bukkit.metadata.FixedMetadataValue(plugin, subskill.getId()));
                            
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("Stored recent subskill preference for " + player.getName() + 
                                            ": " + metadataKey + "=" + subskill.getId());
                            }
                        }
                        
                        SubskillDetailsGUI.openSubskillDetailsGUI(player, subskill);
                    } else {
                        player.sendMessage(ChatColor.RED + "Error: Could not find subskill with ID " + finalSubskillId);
                    }
                }, 1L);
                return;
            }
        }
        
        // Extract subskill ID from lore if present
        String subskillId = getSkillIdFromLore(clickedItem);
        if (subskillId == null) return;
        
        Skill subskill = SkillRegistry.getInstance().getSkill(subskillId);
        if (subskill == null) return;
        
        // Handle subskill clicks based on click type
        player.closeInventory();
        
        if (event.isShiftClick()) {
            // SHIFT+CLICK: Open detailed info GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Store this as the recent subskill for this category
                if (subskill.getParentSkill() != null) {
                    String metadataKey = "recent_subskill_" + subskill.getParentSkill().getId();
                    player.setMetadata(metadataKey, new org.bukkit.metadata.FixedMetadataValue(plugin, subskill.getId()));
                }
                SubskillDetailsGUI.openSubskillDetailsGUI(player, subskill);
            }, 1L);
        } else if (event.isRightClick() && 
                SkillTreeRegistry.getInstance().getSkillTree(subskill) != null) {
            // RIGHT-CLICK: Open skill tree
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SkillTreeGUI.openSkillTreeGUI(player, subskill);
            }, 1L);
        } else {
            // LEFT-CLICK or default: View regular skill details
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SkillDetailsGUI.openSkillDetailsMenu(player, subskill);
            }, 1L);
        }
    }

    /**
     * Extract the VIEW_DETAILS skill ID from item lore
     */
    private String getDetailViewSkillId(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (line.startsWith(ChatColor.BLACK + "VIEW_DETAILS:")) {
                    return line.substring((ChatColor.BLACK + "VIEW_DETAILS:").length());
                }
            }
        }
        return null;
    }
    
    /**
     * Extract skill ID from item lore
     */
    private String getSkillIdFromLore(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (line.startsWith(ChatColor.BLACK + "SKILL_ID:")) {
                    return line.substring((ChatColor.BLACK + "SKILL_ID:").length());
                }
            }
        }
        return null;
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
        // Check for both exact match and contains to be more resilient
        if (displayName.equals(ChatColor.RED + "« Back to Skill Details") || 
            (displayName.contains("Back to Skill Details") && displayName.startsWith(ChatColor.RED.toString()))) {
            
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
     * Handle clicks in the SubskillDetails GUI
     */
    private void handleSubskillDetailGUIClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle back button click
        if (clickedItem.getType() == Material.ARROW && 
            (displayName.equals(ChatColor.RED + "« Back to " + ChatColor.RESET) || 
            displayName.contains("Back to") && displayName.startsWith(ChatColor.RED.toString()))) {
            
            // Extract parent skill ID from lore
            String parentSkillId = null;
            
            if (clickedItem.getItemMeta().hasLore()) {
                for (String line : clickedItem.getItemMeta().getLore()) {
                    if (line.startsWith(ChatColor.BLACK + "SKILL_ID:")) {
                        parentSkillId = line.substring((ChatColor.BLACK + "SKILL_ID:").length());
                        break;
                    }
                }
            }
            
            if (parentSkillId != null) {
                // Find parent skill by ID
                Skill parentSkill = SkillRegistry.getInstance().getSkill(parentSkillId);
                if (parentSkill != null) {
                    player.closeInventory();
                    
                    // Use scheduler to prevent glitches
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, parentSkill);
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