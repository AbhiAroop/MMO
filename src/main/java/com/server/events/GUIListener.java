package com.server.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;

import com.server.Main;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.gui.StatsGUI;
import com.server.profiles.skills.gui.SkillsGUI;

/**
 * Listener for GUI interactions
 */
public class GUIListener implements Listener {
    
    // Reference to main plugin for debugging
    private final Main plugin;
    
    public GUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        
        // Debug the click to help troubleshoot
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("GUI Click: " + player.getName() + 
                                   ", Title: " + title + 
                                   ", Slot: " + event.getSlot() + 
                                   ", Item: " + (event.getCurrentItem() != null ? 
                                               event.getCurrentItem().getType().name() : "null"));
        }
        
        // Cancel clicks in all menu GUIs (with robust checks for different title formats)
        if (title.equals(ProfileGUI.PLAYER_MENU_TITLE) || 
            title.equals(ProfileGUI.PROFILE_SELECTION_TITLE) || 
            title.contains("Stats") && title.startsWith(ChatColor.GOLD + "✦") || 
            title.equals(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Skills Menu" + ChatColor.GOLD + " ✦") ||
            title.contains("Cosmetic Equipment") ||
            title.contains("Rewards:") ||
            title.contains("Skill Details:") ||
            title.contains("Abilities:") ||
            title.contains("VeinMiner Config") ||
            title.contains("OreConduit Config")) {
            
            // Always cancel the click first to prevent item removal
            event.setCancelled(true);
            
            // Process click based on GUI type
            if (title.equals(ProfileGUI.PLAYER_MENU_TITLE)) {
                handleMainMenuClick(event);
            }
            else if (title.equals(ProfileGUI.PROFILE_SELECTION_TITLE)) {
                handleProfileSelection(event);
            }
        }
        
        // Additional protection - cancel any slot clicks in hotbar or outside for all GUIs
        if ((title.equals(ProfileGUI.PLAYER_MENU_TITLE) || 
             title.equals(ProfileGUI.PROFILE_SELECTION_TITLE)) &&
            (event.getSlotType() == SlotType.OUTSIDE || 
             event.getSlot() >= event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle clicks in the main menu
     */
    private void handleMainMenuClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check item name and handle accordingly
        if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
            String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            
            if (itemName.contains("Profile Selection")) {
                player.closeInventory();
                ProfileGUI.openProfileSelector(player);
            }
            else if (itemName.contains("Player Stats")) {
                player.closeInventory();
                StatsGUI.openStatsMenu(player);
            }
            else if (itemName.contains("Skills Menu")) {
                player.closeInventory();
                SkillsGUI.openSkillsMenu(player);
            }
            else if (itemName.contains("Currency Balances")) {
                player.sendMessage(ChatColor.YELLOW + "Use /balance to check your balances");
                player.closeInventory();
            }
        }
    }

    /**
     * Handle clicks in the profile selection menu
     */
    private void handleProfileSelection(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Debug slot information
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Profile selection click: Player=" + player.getName() + 
                                   ", Slot=" + slot + 
                                   ", Item=" + event.getCurrentItem().getType().name());
        }
        
        // Handle back button
        if (slot == 36 && event.getCurrentItem().getType() == Material.ARROW) {
            player.closeInventory();
            ProfileGUI.openMainMenu(player);
            return;
        }
        
        // Handle profile slots (now vertically arranged at slots 13, 22, 31)
        if (slot == 13 || slot == 22 || slot == 31) {
            // Convert GUI slot to profile slot (13 -> 0, 22 -> 1, 31 -> 2)
            int profileSlot = (slot - 13) / 9;
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Profile slot calculation: " + slot + " -> Profile #" + profileSlot);
            }
            
            if (profileSlot < 0 || profileSlot >= 3) return;
            
            ProfileManager profileManager = ProfileManager.getInstance();
            
            if (event.isShiftClick() && event.isRightClick()) {
                // Delete profile
                profileManager.deleteProfile(player, profileSlot);
                player.closeInventory();
                ProfileGUI.openProfileSelector(player);
            } else {
                // Logic to create or select profile
                if (profileManager.getProfiles(player.getUniqueId())[profileSlot] == null) {
                    // Create new profile
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Creating new profile for " + player.getName() + " in slot " + profileSlot);
                    }
                    
                    boolean success = profileManager.createProfile(player, profileSlot, "Profile " + (profileSlot + 1));
                    
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "Created new profile in slot #" + (profileSlot + 1));
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to create profile. Please try again.");
                    }
                    
                    // Close inventory to trigger the onInventoryClose event in PlayerListener
                    player.closeInventory();
                } else {
                    // Select existing profile
                    profileManager.selectProfile(player, profileSlot);
                    player.closeInventory();
                }
            }
        }
    }
}