package com.server.crafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.crafting.fuel.FuelRegistry;
import com.server.crafting.furnace.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Handles interactions with custom furnace GUIs
 * Step 3: GUI interaction system
 */
public class CustomFurnaceGUIListener implements Listener {
    
    private final Main plugin;
    
    public CustomFurnaceGUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory clicks in furnace GUIs
     * Step 3: Click handling
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a custom furnace GUI
        if (!CustomFurnaceGUI.isFurnaceGUI(inventory)) {
            return;
        }
        
        FurnaceData furnaceData = CustomFurnaceGUI.getPlayerFurnaceData(player);
        if (furnaceData == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Error: Furnace data not found!");
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            // Allow normal interaction with player inventory
            return;
        }
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI,
                "[Furnace GUI] Player " + player.getName() + " clicked slot " + slot + 
                " in " + furnaceData.getFurnaceType().getDisplayName());
        }
        
        // Handle functional slot interactions
        if (CustomFurnaceGUI.isInputSlot(slot, furnaceData)) {
            handleInputSlotClick(event, player, furnaceData, slot);
        } else if (CustomFurnaceGUI.isFuelSlot(slot, furnaceData)) {
            handleFuelSlotClick(event, player, furnaceData, slot);
        } else if (CustomFurnaceGUI.isOutputSlot(slot, furnaceData)) {
            handleOutputSlotClick(event, player, furnaceData, slot);
        } else {
            // All other slots are decorative or informational - cancel interaction
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle input slot interactions
     * Step 3: Input slot management
     */
    private void handleInputSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        // For input slots, allow most interactions but validate items
        // (Recipe validation will be handled in Step 4)
        
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            // Shift-click from player inventory - only allow if item can be smelted
            if (slotItem != null && slotItem.getType() != Material.AIR) {
                // TODO: Add recipe validation in Step 4
                // For now, allow all items
            }
        }
        
        // Schedule GUI update after click is processed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
        }, 1L);
    }
    
    /**
     * Handle fuel slot interactions
     * Step 3: Fuel slot management
     */
    private void handleFuelSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        // Validate fuel items
        if (cursor != null && cursor.getType() != Material.AIR) {
            // Check if cursor item is valid fuel
            if (!FuelRegistry.getInstance().isFuel(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                return;
            }
        }
        
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            // Shift-click from player inventory - validate fuel
            if (slotItem != null && slotItem.getType() != Material.AIR) {
                if (!FuelRegistry.getInstance().isFuel(slotItem)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                    return;
                }
            }
        }
        
        // Schedule GUI update after click is processed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
        }, 1L);
    }
    
    /**
     * Handle output slot interactions
     * Step 3: Output slot management
     */
    private void handleOutputSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        // Output slots should only allow taking items, not placing
        if (cursor != null && cursor.getType() != Material.AIR) {
            // Don't allow placing items in output slots
            event.setCancelled(true);
            return;
        }
        
        // Allow taking items from output slots
        if (slotItem != null && slotItem.getType() != Material.AIR) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                // Shift-click to player inventory - allow
            } else {
                // Regular click to cursor - allow
            }
        }
        
        // Schedule GUI update after click is processed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
        }, 1L);
    }
    
    /**
     * Handle inventory drag events
     * Step 3: Drag handling
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a custom furnace GUI
        if (!CustomFurnaceGUI.isFurnaceGUI(inventory)) {
            return;
        }
        
        FurnaceData furnaceData = CustomFurnaceGUI.getPlayerFurnaceData(player);
        if (furnaceData == null) {
            event.setCancelled(true);
            return;
        }
        
        // Check if any dragged slots are in decorative areas
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) { // Only check GUI slots
                // Allow dragging only to functional slots
                if (!CustomFurnaceGUI.isInputSlot(slot, furnaceData) &&
                    !CustomFurnaceGUI.isFuelSlot(slot, furnaceData) &&
                    !CustomFurnaceGUI.isOutputSlot(slot, furnaceData)) {
                    event.setCancelled(true);
                    return;
                }
                
                // For fuel slots, validate that dragged item is fuel
                if (CustomFurnaceGUI.isFuelSlot(slot, furnaceData)) {
                    ItemStack draggedItem = event.getOldCursor();
                    if (draggedItem != null && !FuelRegistry.getInstance().isFuel(draggedItem)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                        return;
                    }
                }
                
                // Don't allow dragging to output slots
                if (CustomFurnaceGUI.isOutputSlot(slot, furnaceData)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // Schedule GUI update after drag is processed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
        }, 2L);
    }
    
    /**
     * Handle inventory close events
     * Step 3: Cleanup and synchronization
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is a custom furnace GUI
        if (!CustomFurnaceGUI.isFurnaceGUI(inventory)) {
            return;
        }
        
        FurnaceData furnaceData = CustomFurnaceGUI.getPlayerFurnaceData(player);
        if (furnaceData == null) {
            CustomFurnaceGUI.removeActiveFurnaceGUI(player);
            return;
        }
        
        // Sync GUI contents to furnace data
        CustomFurnaceGUI.syncGUIToFurnaceData(player);
        
        // Remove player access
        CustomFurnaceManager.getInstance().removeFurnaceAccess(player);
        CustomFurnaceGUI.removeActiveFurnaceGUI(player);
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI,
                "[Furnace GUI] Player " + player.getName() + " closed " + 
                furnaceData.getFurnaceType().getDisplayName() + " GUI");
        }
    }
    
    /**
     * Synchronize GUI contents with furnace data and update displays
     * Step 3: Synchronization helper
     */
    private void syncAndUpdateGUI(Player player, FurnaceData furnaceData) {
        try {
            // Sync GUI contents to furnace data
            CustomFurnaceGUI.syncGUIToFurnaceData(player);
            
            // Update real-time displays
            CustomFurnaceGUI.updateFurnaceGUI(furnaceData);
            
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI,
                    "[Furnace GUI] Synced and updated GUI for " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Furnace GUI] Error syncing GUI for " + player.getName() + ": " + e.getMessage());
        }
    }
}