package com.server.crafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.server.Main;
import com.server.crafting.gui.AutoCraftingGUI;
import com.server.crafting.gui.AutoCraftingGUI.CraftableItem;
import com.server.crafting.gui.CustomCraftingGUI;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Listener for auto-crafting GUI interactions
 */
public class AutoCraftingListener implements Listener {
    
    private final Main plugin;
    
    public AutoCraftingListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is our auto-crafting GUI
        String title = event.getView().getTitle();
        if (!AutoCraftingGUI.GUI_TITLE.equals(title) && 
            !AutoCraftingGUI.OVERFLOW_GUI_TITLE.equals(title)) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            return; // Allow normal interaction with player inventory
        }
        
        // Cancel all clicks in the GUI
        event.setCancelled(true);
        
        boolean isOverflow = AutoCraftingGUI.OVERFLOW_GUI_TITLE.equals(title);
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Player " + player.getName() + " clicked slot " + slot + 
                " in " + (isOverflow ? "overflow" : "main") + " GUI");
        }
        
        // Handle back arrow
        if (AutoCraftingGUI.isBackArrowSlot(slot)) {
            handleBackNavigation(player, isOverflow);
            return;
        }
        
        // Handle next page / previous page
        if (AutoCraftingGUI.isNextPageSlot(slot)) {
            handlePageNavigation(player, isOverflow);
            return;
        }
        
        // Handle refresh
        if (AutoCraftingGUI.isRefreshSlot(slot)) {
            handleRefresh(player);
            return;
        }
        
        // Handle craftable item clicks
        if (AutoCraftingGUI.isCraftableItemSlot(slot)) {
            handleCraftableItemClick(player, slot, event.getClick(), isOverflow);
            return;
        }
        
        // All other clicks are ignored (decorative elements)
    }
    
    /**
     * Handle back navigation - FIXED: Use clearPlayerData instead of removeActiveAutoCraftingGUI
     */
    private void handleBackNavigation(Player player, boolean isOverflow) {
        if (isOverflow) {
            // Go back to main auto-crafting GUI
            AutoCraftingGUI.openAutoCraftingGUI(player);
        } else {
            // Go back to 3x3 crafting GUI - clear all auto-crafting data
            AutoCraftingGUI.clearPlayerData(player);
            CustomCraftingGUI.openCraftingTable(player);
        }
    }
    
    /**
     * Handle page navigation
     */
    private void handlePageNavigation(Player player, boolean isOverflow) {
        if (isOverflow) {
            // Go back to main page
            AutoCraftingGUI.openAutoCraftingGUI(player);
        } else {
            // Go to overflow page
            AutoCraftingGUI.openOverflowGUI(player);
        }
    }
    
    /**
     * Handle refresh button
     */
    private void handleRefresh(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Refreshing craftable items...");
        AutoCraftingGUI.openAutoCraftingGUI(player);
    }
    
    /**
     * Handle craftable item clicks - FIXED: Better error handling and refresh logic
     */
    private void handleCraftableItemClick(Player player, int slot, ClickType clickType, boolean isOverflow) {
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Attempting to get craftable item from slot " + slot + " (overflow: " + isOverflow + ")");
        }
        
        // CRITICAL FIX: Don't use the method that might return null, 
        // instead regenerate if needed within the method
        CraftableItem craftable = AutoCraftingGUI.getCraftableItemFromSlot(player, slot, isOverflow);
        
        if (craftable == null) {
            // If we still can't find it, the slot might be empty or invalid
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] No craftable item found for slot " + slot + " even after regeneration attempt");
            }
            
            // Try refreshing the entire GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                AutoCraftingGUI.openAutoCraftingGUI(player);
            });
            return;
        }
        
        boolean shiftClick = clickType.isShiftClick();
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            String itemName = craftable.result.hasItemMeta() && craftable.result.getItemMeta().hasDisplayName() ?
                            ChatColor.stripColor(craftable.result.getItemMeta().getDisplayName()) :
                            craftable.result.getType().name();
            plugin.debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Player " + player.getName() + " attempting to craft " + itemName + 
                " (shift: " + shiftClick + ", max: " + craftable.maxCraftable + ")");
        }
        
        // Check if the item can actually be crafted
        if (craftable.maxCraftable <= 0) {
            player.sendMessage(ChatColor.RED + "You don't have enough materials to craft this item!");
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Craft rejected - maxCraftable is " + craftable.maxCraftable);
            }
            return;
        }
        
        // Perform the auto-crafting
        boolean success = AutoCraftingGUI.performAutoCraft(player, craftable, shiftClick);
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Craft " + (success ? "successful" : "failed") + ", refreshing GUI");
        }
        
        // ALWAYS refresh the GUI after any craft attempt to ensure data consistency
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (isOverflow) {
                    AutoCraftingGUI.openOverflowGUI(player);
                } else {
                    AutoCraftingGUI.openAutoCraftingGUI(player);
                }
            } catch (Exception e) {
                plugin.debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Error refreshing GUI: " + e.getMessage());
                e.printStackTrace();
            }
        }, 2L); // Increased delay to ensure inventory changes are processed
    }
        
        @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        // Check if this is our auto-crafting GUI
        if (AutoCraftingGUI.GUI_TITLE.equals(title) || 
            AutoCraftingGUI.OVERFLOW_GUI_TITLE.equals(title)) {
            
            // Clean up GUI references but keep craftable items data for potential reopening
            AutoCraftingGUI.removeActiveAutoCraftingGUI(player);
            
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Player " + player.getName() + " closed auto-crafting GUI");
            }
        }
    }
}