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
import com.server.crafting.gui.CustomFurnaceGUI.FurnaceGUILayout;
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
     * Enhanced inventory click handler - FIXED: Better shift-click logic
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
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
        
        int slot = event.getRawSlot();
        
        // CRITICAL FIX: Handle shift-clicks from player inventory more carefully
        if (slot >= inventory.getSize()) {
            // This is a click in the player's inventory
            ClickType clickType = event.getClick();
            
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    // Try to place in appropriate slots with strict validation
                    handleShiftClickFromPlayerInventory(event, player, furnaceData, clickedItem);
                    return;
                }
            }
            return; // Allow normal player inventory interactions
        }
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
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
            // Cancel clicks on decorative elements
            event.setCancelled(true);
        }
    }

    /**
     * Try to place an item in fuel slots - FIXED: Handle entire stacks
     */
    private boolean tryPlaceInFuelSlots(Inventory gui, FurnaceData furnaceData, ItemStack item) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        ItemStack remainingItems = item.clone();
        
        // Try to add to existing stacks first
        for (int i = 0; i < layout.fuelSlots.length && remainingItems.getAmount() > 0; i++) {
            int guiSlot = layout.fuelSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem != null && slotItem.isSimilar(remainingItems)) {
                int maxStack = slotItem.getMaxStackSize();
                int currentAmount = slotItem.getAmount();
                int spaceAvailable = maxStack - currentAmount;
                
                if (spaceAvailable > 0) {
                    int toAdd = Math.min(spaceAvailable, remainingItems.getAmount());
                    slotItem.setAmount(currentAmount + toAdd);
                    gui.setItem(guiSlot, slotItem);
                    remainingItems.setAmount(remainingItems.getAmount() - toAdd);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Fuel Slots] Added " + toAdd + " items to existing stack in slot " + i + 
                            ", remaining: " + remainingItems.getAmount());
                    }
                }
            }
        }
        
        // Try to place remaining items in empty slots
        for (int i = 0; i < layout.fuelSlots.length && remainingItems.getAmount() > 0; i++) {
            int guiSlot = layout.fuelSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                int maxStack = remainingItems.getMaxStackSize();
                int toPlace = Math.min(maxStack, remainingItems.getAmount());
                
                ItemStack newStack = remainingItems.clone();
                newStack.setAmount(toPlace);
                gui.setItem(guiSlot, newStack);
                remainingItems.setAmount(remainingItems.getAmount() - toPlace);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Fuel Slots] Placed " + toPlace + " items in empty slot " + i + 
                        ", remaining: " + remainingItems.getAmount());
                }
            }
        }
        
        // Return true if we placed all items, false if some items couldn't be placed
        boolean allPlaced = remainingItems.getAmount() == 0;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Fuel Slots] Stack placement result: " + (allPlaced ? "SUCCESS" : "PARTIAL") + 
                ", original: " + item.getAmount() + ", remaining: " + remainingItems.getAmount());
        }
        
        return allPlaced;
    }

    /**
     * Try to place an item in input slots - FIXED: Handle entire stacks
     */
    private boolean tryPlaceInInputSlots(Inventory gui, FurnaceData furnaceData, ItemStack item) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        ItemStack remainingItems = item.clone();
        
        // Try to add to existing stacks first
        for (int i = 0; i < layout.inputSlots.length && remainingItems.getAmount() > 0; i++) {
            int guiSlot = layout.inputSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem != null && slotItem.isSimilar(remainingItems)) {
                int maxStack = slotItem.getMaxStackSize();
                int currentAmount = slotItem.getAmount();
                int spaceAvailable = maxStack - currentAmount;
                
                if (spaceAvailable > 0) {
                    int toAdd = Math.min(spaceAvailable, remainingItems.getAmount());
                    slotItem.setAmount(currentAmount + toAdd);
                    gui.setItem(guiSlot, slotItem);
                    remainingItems.setAmount(remainingItems.getAmount() - toAdd);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Input Slots] Added " + toAdd + " items to existing stack in slot " + i + 
                            ", remaining: " + remainingItems.getAmount());
                    }
                }
            }
        }
        
        // Try to place remaining items in empty slots
        for (int i = 0; i < layout.inputSlots.length && remainingItems.getAmount() > 0; i++) {
            int guiSlot = layout.inputSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                int maxStack = remainingItems.getMaxStackSize();
                int toPlace = Math.min(maxStack, remainingItems.getAmount());
                
                ItemStack newStack = remainingItems.clone();
                newStack.setAmount(toPlace);
                gui.setItem(guiSlot, newStack);
                remainingItems.setAmount(remainingItems.getAmount() - toPlace);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Input Slots] Placed " + toPlace + " items in empty slot " + i + 
                        ", remaining: " + remainingItems.getAmount());
                }
            }
        }
        
        // Return true if we placed all items, false if some items couldn't be placed
        boolean allPlaced = remainingItems.getAmount() == 0;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Input Slots] Stack placement result: " + (allPlaced ? "SUCCESS" : "PARTIAL") + 
                ", original: " + item.getAmount() + ", remaining: " + remainingItems.getAmount());
        }
        
        return allPlaced;
    }

    /**
     * Handle shift-clicks from player inventory with smart slot targeting - ENHANCED: Full stack handling
     */
    private void handleShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, 
                                                FurnaceData furnaceData, ItemStack item) {
        event.setCancelled(true); // Cancel the default shift-click behavior
        
        // Determine where this item should go
        boolean isFuel = FuelRegistry.getInstance().isFuel(item);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Shift Click] Player " + player.getName() + " shift-clicking " + 
                item.getType().name() + " x" + item.getAmount() + 
                " (isFuel: " + isFuel + ")");
        }
        
        if (isFuel) {
            // Try to place entire stack in fuel slots
            if (tryPlaceInFuelSlots(event.getInventory(), furnaceData, item)) {
                // All items were placed successfully
                event.setCurrentItem(null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Shift Click] Successfully placed entire fuel stack of " + item.getAmount());
                }
            } else {
                // Could only place some items - calculate how many were actually placed
                ItemStack originalItem = event.getCurrentItem();
                int originalAmount = originalItem.getAmount();
                
                // Count how many items are actually in the fuel slots now
                int placedAmount = countItemsInFuelSlots(event.getInventory(), furnaceData, item);
                int actuallyPlaced = Math.min(originalAmount, placedAmount);
                
                if (actuallyPlaced > 0) {
                    originalItem.setAmount(originalAmount - actuallyPlaced);
                    if (originalItem.getAmount() <= 0) {
                        event.setCurrentItem(null);
                    }
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Shift Click] Partially placed fuel stack: " + actuallyPlaced + "/" + originalAmount);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Fuel slots are full!");
                }
            }
            
            // Update GUI
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                syncAndUpdateGUI(player, furnaceData);
            }, 1L);
            
        } else {
            // Try to place entire stack in input slots (not output slots!)
            if (tryPlaceInInputSlots(event.getInventory(), furnaceData, item)) {
                // All items were placed successfully
                event.setCurrentItem(null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Shift Click] Successfully placed entire input stack of " + item.getAmount());
                }
            } else {
                // Could only place some items - calculate how many were actually placed
                ItemStack originalItem = event.getCurrentItem();
                int originalAmount = originalItem.getAmount();
                
                // Count how many items are actually in the input slots now
                int placedAmount = countItemsInInputSlots(event.getInventory(), furnaceData, item);
                int actuallyPlaced = Math.min(originalAmount, placedAmount);
                
                if (actuallyPlaced > 0) {
                    originalItem.setAmount(originalAmount - actuallyPlaced);
                    if (originalItem.getAmount() <= 0) {
                        event.setCurrentItem(null);
                    }
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Shift Click] Partially placed input stack: " + actuallyPlaced + "/" + originalAmount);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Input slots are full!");
                }
            }
            
            // Update GUI
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                syncAndUpdateGUI(player, furnaceData);
            }, 1L);
        }
    }

    /**
     * Count how many of a specific item type are in fuel slots (for partial placement calculation)
     */
    private int countItemsInFuelSlots(Inventory gui, FurnaceData furnaceData, ItemStack targetItem) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        int count = 0;
        
        for (int i = 0; i < layout.fuelSlots.length; i++) {
            int guiSlot = layout.fuelSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem != null && slotItem.isSimilar(targetItem)) {
                count += slotItem.getAmount();
            }
        }
        
        return count;
    }

    /**
     * Count how many of a specific item type are in input slots (for partial placement calculation)
     */
    private int countItemsInInputSlots(Inventory gui, FurnaceData furnaceData, ItemStack targetItem) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        int count = 0;
        
        for (int i = 0; i < layout.inputSlots.length; i++) {
            int guiSlot = layout.inputSlots[i];
            ItemStack slotItem = gui.getItem(guiSlot);
            
            if (slotItem != null && slotItem.isSimilar(targetItem)) {
                count += slotItem.getAmount();
            }
        }
        
        return count;
    }

    /**
     * Check if player has space for an item
     */
    private boolean hasSpaceForItem(Player player, ItemStack item) {
        // Check for empty slots
        if (player.getInventory().firstEmpty() != -1) {
            return true;
        }
        
        // Check for stackable slots
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                if (inventoryItem.getAmount() < inventoryItem.getMaxStackSize()) {
                    return true;
                }
            }
        }
        
        return false;
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
     * Step 3: Output slot management - ENHANCED: Better protection against invalid placements
     */
    private void handleOutputSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        // CRITICAL FIX: Never allow placing items in output slots
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place items in the output slots!");
            return;
        }
        
        // CRITICAL FIX: Block all shift-clicks that try to place items in output slots
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            // Check if this is a shift-click from player inventory trying to place an item
            if (event.getRawSlot() >= event.getInventory().getSize()) {
                // This is a shift-click from player inventory - block it completely for output slots
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place items in the output slots!");
                return;
            }
        }
        
        // Allow taking items from output slots only
        if (slotItem != null && slotItem.getType() != Material.AIR) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                // Shift-click to take output - allow this but ensure it goes to player inventory
                if (cursor == null || cursor.getType() == Material.AIR) {
                    // Try to move to player inventory
                    if (player.getInventory().firstEmpty() != -1 || 
                        hasSpaceForItem(player, slotItem)) {
                        // Allow the shift-click to take the item
                        // The item will automatically go to player inventory
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Your inventory is full!");
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    return;
                }
            } else {
                // Normal click to take output - allow this
                if (cursor == null || cursor.getType() == Material.AIR || 
                    (cursor.isSimilar(slotItem) && cursor.getAmount() + slotItem.getAmount() <= cursor.getMaxStackSize())) {
                    // Allow taking the item
                } else {
                    event.setCancelled(true);
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
     * Step 3: Cleanup and synchronization - ENHANCED: Handle explosion cleanup
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is a custom furnace GUI
        if (!CustomFurnaceGUI.isFurnaceGUI(inventory)) {
            return;
        }
        
        FurnaceData furnaceData = CustomFurnaceGUI.getPlayerFurnaceData(player);
        
        if (furnaceData != null) {
            // Only sync if furnace still exists (not exploded)
            if (CustomFurnaceManager.getInstance().hasCustomFurnace(furnaceData.getLocation())) {
                // Synchronize GUI contents with furnace data
                CustomFurnaceGUI.syncGUIToFurnaceData(player);
                
                if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                    plugin.debugLog(DebugSystem.GUI,
                        "[Furnace GUI] " + player.getName() + " closed " + 
                        furnaceData.getFurnaceType().getDisplayName() + " GUI - data synchronized");
                }
            } else {
                if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                    plugin.debugLog(DebugSystem.GUI,
                        "[Furnace GUI] " + player.getName() + " closed GUI - furnace no longer exists (likely exploded)");
                }
            }
            
            // Remove player access regardless
            CustomFurnaceManager.getInstance().removeFurnaceAccess(player);
        }
        
        // Clean up GUI tracking
        CustomFurnaceGUI.removeActiveFurnaceGUI(player);
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