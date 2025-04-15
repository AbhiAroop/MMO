package com.server.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.gui.StatsGUI;

public class GUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        
        // Cancel clicks in all menu GUIs
        if (title.equals("Player Menu") || title.equals("Profile Selection") || title.equals("Profile Stats")) {
            event.setCancelled(true);
            
            // Handle main menu clicks
            if (title.equals("Player Menu")) {
                handleMainMenuClick(event);
            }
            // Handle profile selection clicks
            else if (title.equals("Profile Selection")) {
                handleProfileSelection(event);
            }
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
        
        if (itemName.equals("§6§lProfile Selection")) {
            player.closeInventory();
            ProfileGUI.openProfileSelector(player);
        }
        else if (itemName.equals("§b§lPlayer Stats")) {
            player.closeInventory();
            StatsGUI.openStatsMenu(player);
        }
    }

    private void handleProfileSelection(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        // Convert GUI slot to profile slot (11, 13, 15 -> 0, 1, 2)
        int profileSlot = (slot - 11) / 2;
        if (profileSlot < 0 || profileSlot >= 3) return;
        
        ProfileManager profileManager = ProfileManager.getInstance();
        
        if (event.isShiftClick() && event.isRightClick()) {
            profileManager.deleteProfile(player, profileSlot);
            player.closeInventory();
            ProfileGUI.openProfileSelector(player);
        } else {
            if (profileManager.getProfiles(player.getUniqueId())[profileSlot] == null) {
                profileManager.createProfile(player, profileSlot, "Profile " + (profileSlot + 1));
            } else {
                profileManager.selectProfile(player, profileSlot);
            }
            player.closeInventory();
        }
    }
}