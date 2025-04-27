package com.server.profiles.skills.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.trees.SkillTreeRegistry;

/**
 * Listener for skill GUI interactions
 */
public class SkillGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Debug output to help diagnose issues
        if (Main.getInstance().isDebugMode() && event.getCurrentItem() != null && 
            event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
            Main.getInstance().getLogger().info("Click in " + title + " on " + 
                event.getCurrentItem().getItemMeta().getDisplayName() + 
                ", right click: " + event.isRightClick() + 
                ", click type: " + event.getClick().name());
        }
        
        // Cancel all clicks in any skill GUI
        if (title.equals("Skills") || 
            title.startsWith("Skill: ") || 
            title.startsWith("Subskills: ") || 
            title.startsWith("Rewards: ") ||
            title.startsWith("Skill Tree: ")) {
            event.setCancelled(true);
            
            // Handle clicks
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta() || 
                !event.getCurrentItem().getItemMeta().hasDisplayName()) return;
            
            ItemStack clickedItem = event.getCurrentItem();
            String itemName = clickedItem.getItemMeta().getDisplayName();
            
            // Back button is common to several GUIs
            if (itemName.equals(ChatColor.RED + "Back to Menu") ||
                itemName.equals(ChatColor.RED + "Back to Skills") ||
                itemName.equals(ChatColor.RED + "Back to Skill Details")) {
                handleBackButton(player, title, itemName);
                return;
            }
            
            // Handle clicks based on the GUI
            if (title.equals("Skills")) {
                handleSkillsMenuClick(player, clickedItem);
            } else if (title.startsWith("Skill: ")) {
                String skillName = title.substring("Skill: ".length());
                handleSkillDetailsMenuClick(player, clickedItem, skillName);
            } else if (title.startsWith("Subskills: ")) {
                handleSubskillsMenuClick(player, clickedItem, event);
            } else if (title.startsWith("Rewards: ")) {
                handleRewardsMenuClick(player, clickedItem);
            }
        }
        
        // Handle profile menu click to open skills menu
        if (title.equals("Player Menu") && 
            event.getCurrentItem() != null && 
            event.getCurrentItem().hasItemMeta() && 
            event.getCurrentItem().getItemMeta().hasDisplayName() &&
            event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "§a§lSkills")) {
            player.closeInventory();
            SkillsGUI.openSkillsMenu(player);
            event.setCancelled(true);
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
        
        // View skill tree button
        if (itemName.equals(ChatColor.AQUA + "Skill Tree")) {
            player.closeInventory();
            SkillTreeGUI.openSkillTreeGUI(player, skill);
            return;
        }
    }
    
    /**
     * Handle clicks in the subskills menu
     */
    private void handleSubskillsMenuClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        // Debug the click event in detail to help diagnose the issue
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("SubskillGUI click: " + player.getName() + " clicked " + 
                                            (clickedItem != null ? clickedItem.getType() : "null") + 
                                            ", right-click: " + event.isRightClick() + 
                                            ", click type: " + event.getClick());
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the clicked item name
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
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                        SkillDetailsGUI.openSkillDetailsMenu(player, mainSkill);
                    }, 1L);
                }
            }
            return;
        }
        
        // Try to get the skill ID from the lore - this is critical
        String skillId = extractSkillIdFromItem(clickedItem);
        
        // Debug the extracted skill ID
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Extracted skill ID: " + skillId + " from item " + itemName);
        }
        
        Skill subskill = null;
        
        // First try to get the skill by ID (most reliable)
        if (skillId != null) {
            subskill = SkillRegistry.getInstance().getSkill(skillId);
            
            // Debug the skill lookup result
            if (Main.getInstance().isDebugMode()) {
                if (subskill != null) {
                    Main.getInstance().getLogger().info("Found skill by ID: " + subskill.getDisplayName());
                } else {
                    Main.getInstance().getLogger().warning("Could not find skill with ID: " + skillId);
                }
            }
        }
        
        // Fallback to checking by display name if no ID was found or lookup failed
        if (subskill == null) {
            // Strip color codes and level info from item name for comparison
            String cleanItemName = ChatColor.stripColor(itemName);
            if (cleanItemName.contains("[")) {
                cleanItemName = cleanItemName.substring(0, cleanItemName.indexOf("[")).trim();
            }
            
            // Try each skill to find a matching display name
            for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
                if (!skill.isMainSkill() && skill.getDisplayName().equalsIgnoreCase(cleanItemName)) {
                    subskill = skill;
                    break;
                }
            }
            
            // Debug the fallback lookup
            if (Main.getInstance().isDebugMode()) {
                if (subskill != null) {
                    Main.getInstance().getLogger().info("Found skill by display name: " + subskill.getDisplayName());
                } else {
                    Main.getInstance().getLogger().warning("Could not find skill with display name: " + cleanItemName);
                }
            }
        }
        
        if (subskill != null) {
            // Debug message to confirm clicks
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Found subskill: " + subskill.getId() + " - " + subskill.getDisplayName());
                Main.getInstance().getLogger().info(player.getName() + " clicked on " + subskill.getDisplayName() + 
                    " subskill (Right: " + event.isRightClick() + ", Click: " + event.getClick() + ")");
            }
            
            // Before handling the click, force the skill object to be the correct type
            // This ensures we're using the actual Skill object from the registry
            final Skill finalSubskill = subskill;
            final String skillTreeId = finalSubskill.getId();
            
            // Check if skill tree exists for this subskill
            boolean treeExists = SkillTreeRegistry.getInstance().getSkillTree(skillTreeId) != null;
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Skill tree exists for " + skillTreeId + ": " + treeExists);
            }
            
            // Right-click to open skill tree (check multiple right-click types for reliability)
            if (event.isRightClick() || event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                // Close inventory first
                player.closeInventory();
                
                // Force a short delay to ensure inventory is fully closed before opening new one
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    try {
                        // Check again if skill tree exists
                        if (SkillTreeRegistry.getInstance().getSkillTree(finalSubskill) == null) {
                            player.sendMessage(ChatColor.RED + "Skill tree for " + finalSubskill.getDisplayName() + " is not available yet.");
                            if (Main.getInstance().isDebugMode()) {
                                Main.getInstance().getLogger().severe("Missing skill tree for " + finalSubskill.getId());
                            }
                            return;
                        }
                        
                        // Open the skill tree GUI with the final subskill reference
                        SkillTreeGUI.openSkillTreeGUI(player, finalSubskill);
                        
                        // Additional debug message
                        if (Main.getInstance().isDebugMode()) {
                            Main.getInstance().getLogger().info("Opening skill tree for " + finalSubskill.getDisplayName());
                        }
                    } catch (Exception e) {
                        // Log any errors that occur
                        if (Main.getInstance().isDebugMode()) {
                            Main.getInstance().getLogger().severe("Error opening skill tree: " + e.getMessage());
                            e.printStackTrace();
                        }
                        player.sendMessage(ChatColor.RED + "An error occurred opening the skill tree. Please try again later.");
                    }
                }, 2L); // Increased to 2 ticks for more reliable GUI switching
            } else {
                // Left-click to view details
                player.closeInventory();
                
                // Force a short delay
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    SkillDetailsGUI.openSkillDetailsMenu(player, finalSubskill);
                }, 2L); // Increased to 2 ticks
            }
        } else {
            // If we couldn't find the skill, log detailed error info
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().warning("Could not find subskill for item: " + itemName);
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                    Main.getInstance().getLogger().warning("Lore: " + clickedItem.getItemMeta().getLore());
                }
                
                // List all available skills for debugging
                Main.getInstance().getLogger().warning("Available skills:");
                for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
                    Main.getInstance().getLogger().warning(" - " + skill.getId() + ": " + skill.getDisplayName() + 
                                                        " (Main: " + skill.isMainSkill() + ")");
                }
            }
        }
    }

    /**
     * Extract the skill ID from an item's lore
     */
    private String extractSkillIdFromItem(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
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
     * Handle clicks in the rewards menu
     */
    private void handleRewardsMenuClick(Player player, ItemStack clickedItem) {
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Implementation for handling reward menu item clicks
        // Add your specific reward handling logic here
        
        // Example: if there are specific reward items to click
        // if (itemName.startsWith(ChatColor.YELLOW + "Reward:")) {
        //     // Handle reward selection
        // }
    }

    /**
     * Handle back button clicks
     */
    private void handleBackButton(Player player, String title, String buttonName) {
        player.closeInventory();
        
        if (buttonName.equals(ChatColor.RED + "Back to Menu")) {
            // From Skills menu back to main menu
            ProfileGUI.openMainMenu(player);
        } else if (buttonName.equals(ChatColor.RED + "Back to Skills")) {
            // From Skill Details back to Skills menu
            SkillsGUI.openSkillsMenu(player);
        } else if (buttonName.equals(ChatColor.RED + "Back to Skill Details")) {
            // From Subskills/Rewards menu back to Skill Details
            if (title.startsWith("Subskills: ")) {
                String mainSkillName = title.substring("Subskills: ".length());
                Skill mainSkill = findSkillByName(mainSkillName);
                if (mainSkill != null) {
                    SkillDetailsGUI.openSkillDetailsMenu(player, mainSkill);
                } else {
                    SkillsGUI.openSkillsMenu(player);
                }
            } else if (title.startsWith("Rewards: ")) {
                String skillName = title.substring("Rewards: ".length());
                Skill skill = findSkillByName(skillName);
                if (skill != null) {
                    SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                } else {
                    SkillsGUI.openSkillsMenu(player);
                }
            } else {
                SkillsGUI.openSkillsMenu(player);
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