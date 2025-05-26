package com.server.profiles.skills.gui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
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
        
        // Debug output
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            ItemStack clickedItem = event.getCurrentItem();
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[DEBUG:GUI] GUI Click: " + player.getName() + 
                ", Title: " + title + 
                ", Slot: " + event.getRawSlot() + 
                ", Item: " + (clickedItem != null ? clickedItem.getType() : "NULL"));
        }
        
        // FIXED: Exclude Skill Tree GUI from this listener - let SkillTreeGUIListener handle it exclusively
        if (title.startsWith(SkillTreeGUI.GUI_TITLE_PREFIX)) {
            return;
        }
        
        // Handle all other skill-related GUIs
        if (title.equals("✦ Skills Menu ✦") || 
            title.startsWith("Skill Details: ") || 
            title.startsWith("Subskills: ") ||
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ") ||
            (title.contains("Details") && (title.contains("Ore Extraction") || title.contains("Gem Carving"))) ||
            title.startsWith(ChatColor.GOLD + "✦")) {
            
            event.setCancelled(true);
            
            // Handle clicks on items
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }
            
            ItemStack clickedItem = event.getCurrentItem();
            
            // Route to appropriate handler based on GUI title
            if (title.equals("✦ Skills Menu ✦")) {
                handleSkillsMenuClick(player, clickedItem);
            } else if (title.startsWith("Skill Details: ")) {
                handleSkillDetailsClick(player, clickedItem, event);
            } else if (title.startsWith("Subskills: ")) {
                handleSubskillsMenuClick(player, clickedItem, event);
            } else if (title.startsWith(ChatColor.GOLD + "✦") && title.contains("Details")) {
                handleSubskillDetailGUIClick(player, clickedItem);
            } else if (title.startsWith("Rewards: ") || title.equals("Rewards")) {
                handleRewardsClick(player, clickedItem);
            } else if (title.startsWith("Milestones: ")) {
                handleMilestonesClick(player, clickedItem, event);
            } else if (title.equals("Reset Confirmation")) {
                ConfirmationGUI.handleConfirmAction(player, clickedItem);
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
            title.startsWith("Abilities: ") ||
            title.startsWith("Rewards: ") ||
            title.equals("Rewards") ||
            title.equals("Reset Confirmation") ||
            title.startsWith("Milestones: ") ||
            title.startsWith(SkillTreeGUI.GUI_TITLE_PREFIX) ||
            // FIXED: Better detection for SubskillDetails GUI  
            (title.contains("Details") && (title.contains("Ore Extraction") || title.contains("Gem Carving"))) ||
            title.startsWith(ChatColor.GOLD + "✦")) {
            
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
        
        // Back button for main skills - navigates to main skills menu
        if (displayName.equals(ChatColor.RED + "« Back to Skills") ||
            (displayName.contains("Back to Skills") && displayName.startsWith(ChatColor.RED.toString()))) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                plugin, 
                () -> SkillsGUI.openSkillsMenu(player), 
                1L);
            return;
        }
        
        // FIXED: Back button for subskills - navigates to parent skill's subskills menu
        if (displayName.startsWith(ChatColor.RED + "« Back to") && displayName.contains("Subskills")) {
            List<String> lore = clickedItem.getItemMeta().getLore();
            String parentSkillId = null;
            
            // Extract parent skill ID from lore
            for (String line : lore) {
                if (line.startsWith(ChatColor.BLACK + "PARENT_SKILL:")) {
                    parentSkillId = line.substring((ChatColor.BLACK + "PARENT_SKILL:").length());
                    break;
                }
            }
            
            if (parentSkillId != null) {
                Skill parentSkill = SkillRegistry.getInstance().getSkill(parentSkillId);
                if (parentSkill != null) {
                    player.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(
                        plugin, 
                        () -> SubskillsGUI.openSubskillsMenu(player, parentSkill), 
                        1L);
                    return;
                }
            }
            
            // Fallback to main skills menu
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                plugin, 
                () -> SkillsGUI.openSkillsMenu(player), 
                1L);
            return;
        }
        
        // Subskills button - check for both standard and fancy format
        if (displayName.equals(ChatColor.AQUA + "View Subskills") || 
            (displayName.contains("Subskills") && displayName.contains("✦"))) {
            
            // Extract skill name from title
            String skillName = extractSkillFromTitle(title);
            Skill skill = findSkillByName(skillName);
            
            if (skill != null && skill.isMainSkill()) {
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    plugin, 
                    () -> SubskillsGUI.openSubskillsMenu(player, skill), 
                    1L);
            }
            return;
        }
        
        // Detailed info button for subskills
        if (displayName.contains("Detailed") && displayName.contains("Info")) {
            String subskillName = extractSubskillNameFromDetailButton(displayName);
            Skill subskill = findSkillByName(subskillName);
            
            if (subskill != null && !subskill.isMainSkill()) {
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    plugin, 
                    () -> SubskillDetailsGUI.openSubskillDetailsGUI(player, subskill), 
                    1L);
            }
            return;
        }
        
        // Skill tree button
        if (displayName.contains("Skill Tree") || displayName.contains("✦ Skill Tree")) {
            String skillName = extractSkillFromTitle(title);
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    plugin, 
                    () -> SkillTreeGUI.openSkillTreeGUI(player, skill), 
                    1L);
            }
            return;
        }
        
        // Abilities button
        if (displayName.contains("Abilities")) {
            String skillName = extractSkillFromTitle(title);
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                // Open abilities GUI when implemented
                player.sendMessage(ChatColor.YELLOW + "Abilities GUI coming soon!");
            }
            return;
        }
        
        // Rewards button
        if (displayName.contains("Rewards")) {
            String skillName = extractSkillFromTitle(title);
            Skill skill = findSkillByName(skillName);
            
            if (skill != null) {
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    plugin, 
                    () -> RewardsGUI.openRewardsMenu(player, skill), 
                    1L);
            }
            return;
        }
        
        // Help button
        if (displayName.contains("Help") || displayName.contains("Information")) {
            player.sendMessage(ChatColor.GOLD + "=== Skill Details Help ===");
            player.sendMessage(ChatColor.YELLOW + "• View detailed information about skills");
            player.sendMessage(ChatColor.YELLOW + "• Check progress, rewards, and abilities");
            player.sendMessage(ChatColor.YELLOW + "• Access skill trees and subskills");
            return;
        }
    }

    /**
     * Handle clicks in the subskills menu
     */
    private void handleSubskillsMenuClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle back button
        if (itemName.equals(ChatColor.RED + "« Back to Skill Details")) {
            List<String> lore = clickedItem.getItemMeta().getLore();
            String parentSkillId = null;
            
            // Extract parent skill ID from lore
            for (String line : lore) {
                if (line.startsWith(ChatColor.BLACK + "PARENT_SKILL:")) {
                    parentSkillId = line.substring((ChatColor.BLACK + "PARENT_SKILL:").length());
                    break;
                }
            }
            
            // Get parent skill
            Skill parentSkill = null;
            if (parentSkillId != null) {
                parentSkill = SkillRegistry.getInstance().getSkill(parentSkillId);
            }
            
            // If we found the parent skill, navigate to its details
            if (parentSkill != null) {
                player.closeInventory();
                
                final Skill finalParentSkill = parentSkill;
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SkillDetailsGUI.openSkillDetailsMenu(player, finalParentSkill);
                }, 1L);
            } else {
                // Fallback to main skills menu if parent skill not found
                player.closeInventory();
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    SkillsGUI.openSkillsMenu(player);
                }, 1L);
            }
            return;
        }
        
        // Handle help button
        if (itemName.contains("Help") || itemName.contains("Information")) {
            player.sendMessage(ChatColor.GOLD + "=== Subskills Help ===");
            player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.GREEN + "Left-click" + ChatColor.YELLOW + " a subskill to view skill details");
            player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.GREEN + "Right-click" + ChatColor.YELLOW + " for specialized subskill information");
            player.sendMessage(ChatColor.YELLOW + "• Each subskill levels independently but contributes to the main skill");
            player.sendMessage(ChatColor.YELLOW + "• Tokens from subskills go to the parent skill tree");
            return;
        }
        
        // FIXED: Handle subskill clicks with proper left/right click detection
        String subskillId = getSkillIdFromLore(clickedItem);
        if (subskillId == null) return;
        
        Skill subskill = SkillRegistry.getInstance().getSkill(subskillId);
        if (subskill == null) return;
        
        // Handle subskill clicks based on click type
        player.closeInventory();
        
        // Use scheduler to prevent glitches
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Store this as the recent subskill for this category
            if (subskill.getParentSkill() != null) {
                String metadataKey = "recent_subskill_" + subskill.getParentSkill().getId();
                player.setMetadata(metadataKey, new org.bukkit.metadata.FixedMetadataValue(plugin, subskill.getId()));
                
                if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                    plugin.debugLog(DebugSystem.GUI, "Stored recent subskill preference for " + player.getName() + 
                                ": " + metadataKey + "=" + subskill.getId());
                }
            }
            
            // FIXED: Check click type and navigate accordingly
            if (event.getClick().isRightClick()) {
                // Right-click: Open specialized subskill details GUI
                SubskillDetailsGUI.openSubskillDetailsGUI(player, subskill);
            } else {
                // Left-click: Open general skill details GUI for the subskill
                SkillDetailsGUI.openSkillDetailsMenu(player, subskill);
            }
        }, 1L);
    }

    /**
     * Handle clicks in the SubskillDetails GUI
    */
    private void handleSubskillDetailGUIClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Add debug logging
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI, "SubskillDetails GUI Click - Item: " + itemName);
        }
        
        // FIXED: Handle back button - should go back to subskill's detail page
        if (itemName.startsWith(ChatColor.RED + "« Back to") && itemName.contains("Details")) {
            List<String> lore = clickedItem.getItemMeta().getLore();
            String subskillId = null;
            
            // Extract subskill ID from lore
            for (String line : lore) {
                if (line.startsWith(ChatColor.BLACK + "SUBSKILL_ID:")) {
                    subskillId = line.substring((ChatColor.BLACK + "SUBSKILL_ID:").length());
                    break;
                }
            }
            
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, "SubskillDetails back button - extracted ID: " + subskillId);
            }
            
            if (subskillId != null) {
                Skill subskill = SkillRegistry.getInstance().getSkill(subskillId);
                if (subskill != null) {
                    if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                        plugin.debugLog(DebugSystem.GUI, "Navigating back to Skill Details for: " + subskill.getDisplayName());
                    }
                    
                    player.closeInventory();
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Navigate back to the subskill's Skill Details page
                        SkillDetailsGUI.openSkillDetailsMenu(player, subskill);
                    }, 1L);
                    return;
                }
            }
            
            // Fallback to main skills menu
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, "SubskillDetails back button - falling back to main skills menu");
            }
            
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SkillsGUI.openSkillsMenu(player);
            }, 1L);
            return;
        }
        
        // Handle help button
        if (itemName.contains("Help & Information")) {
            player.sendMessage(ChatColor.GOLD + "=== Subskill Details Help ===");
            player.sendMessage(ChatColor.YELLOW + "Green checkmarks indicate unlocked content");
            player.sendMessage(ChatColor.YELLOW + "Red X marks indicate locked content");
            player.sendMessage(ChatColor.YELLOW + "Unlock requirements are shown in the descriptions");
            return;
        }
        
        // Handle ore/gem item clicks (could show more detailed information)
        if (itemName.contains("✓") || itemName.contains("✗")) {
            // Could potentially open detailed information about the specific ore/gem
            // For now, just a simple message
            if (itemName.contains("✓")) {
                player.sendMessage(ChatColor.GREEN + "This item is unlocked and ready to use!");
            } else {
                player.sendMessage(ChatColor.RED + "This item is locked. Check the requirements to unlock it.");
            }
            return;
        }
    }

    /**
     * Extract skill ID from item lore
     */
    private String getSkillIdFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            // Check for subskill ID format
            if (line.startsWith(ChatColor.BLACK + "SUBSKILL_ID:")) {
                return line.substring((ChatColor.BLACK + "SUBSKILL_ID:").length());
            }
            // Check for regular skill ID format
            if (line.startsWith(ChatColor.BLACK + "SKILL_ID:")) {
                return line.substring((ChatColor.BLACK + "SKILL_ID:").length());
            }
        }
        
        return null;
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
                String title = player.getOpenInventory().getTitle();
                
                // The player's inventory is already closed at this point, so we need to get the title from elsewhere
                // Use a safer approach that checks if the title exists and has the proper format
                if (title != null && title.startsWith("Skill Tree: ") && title.length() > "Skill Tree: ".length()) {
                    String skillName = title.substring("Skill Tree: ".length());
                    Skill skill = findSkillByName(skillName);
                    
                    if (skill != null) {
                        SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                        return;
                    }
                }
                
                // Fallback to main skills menu if we can't determine the skill
                SkillsGUI.openSkillsMenu(player);
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
                    // Extract the skill name from the inventory title
                    String title = player.getOpenInventory().getTitle();
                    
                    // Assuming the title format is "Skill Tree: SkillName"
                    if (title != null && title.startsWith("Skill Tree: ")) {
                        String skillName = title.substring("Skill Tree: ".length());
                        SkillTreeGUI.handleResetClick(player, skillName);
                    }
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
     * Extract parent skill name from back button display name
     * Format: "« Back to ParentSkillName Subskills"
     */
    private String extractParentSkillNameFromBackButton(String displayName) {
        // Remove color codes and extract the skill name
        String cleanName = ChatColor.stripColor(displayName);
        
        // Format: "« Back to ParentSkillName Subskills"
        if (cleanName.startsWith("« Back to") && cleanName.endsWith("Subskills")) {
            String withoutPrefix = cleanName.substring("« Back to ".length());
            String skillName = withoutPrefix.substring(0, withoutPrefix.length() - " Subskills".length());
            return skillName.trim();
        }
        
        return null;
    }

    /**
     * Extract subskill name from a SubskillDetails GUI title
     * Format is "✦ SubskillName Details ✦"
     */
    private String extractSubskillNameFromTitle(String title) {
        if (title != null && title.contains("Details") && title.startsWith(ChatColor.GOLD + "✦")) {
            // Remove color codes and extract the name
            String withoutPrefix = title.substring((ChatColor.GOLD + "✦ " + ChatColor.AQUA).length());
            return withoutPrefix.substring(0, withoutPrefix.indexOf(" Details"));
        }
        return null;
    }
    
    /**
     * Extract parent skill name from title with format "Subskills: ParentSkillName"
     */
    private String extractParentSkillFromTitle(String title) {
        if (title != null && title.startsWith("Subskills: ")) {
            return title.substring("Subskills: ".length());
        }
        return null;
    }
    
    /**
     * Extract skill name from title with format "Skill Details: SkillName"
     */
    private String extractSkillFromTitle(String title) {
        if (title != null && title.startsWith("Skill Details: ")) {
            return title.substring("Skill Details: ".length());
        }
        return null;
    }
    
    /**
     * Extract subskill name from a detail button display name
     * Format is "✦ Detailed SubskillName Info"
     */
    private String extractSubskillNameFromDetailButton(String displayName) {
        if (displayName != null && 
            displayName.startsWith(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Detailed ") &&
            displayName.endsWith(" Info")) {
            
            String prefix = ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Detailed ";
            String suffix = " Info";
            
            return displayName.substring(prefix.length(), displayName.length() - suffix.length());
        }
        return null;
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