package com.server.crafting.listeners;

import java.util.HashMap;

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
     * Enhanced inventory click handler - FIXED: Better shift-click logic with item loss prevention
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is a custom furnace GUI
        FurnaceData furnaceData = CustomFurnaceGUI.getPlayerFurnaceData(player);
        if (furnaceData == null) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle player inventory clicks (shift-clicking items into furnace)
        if (slot >= inventory.getSize()) {
            // CRITICAL FIX: Enhanced shift-click validation
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null) {
                handleShiftClickFromPlayerInventory(event, player, furnaceData, event.getCurrentItem());
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
     * Handle shift-clicks from player inventory with smart slot targeting - ENHANCED: Item loss prevention
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
        
        boolean success = false;
        String failureReason = "";
        
        if (isFuel) {
            // Try to place entire stack in fuel slots
            if (tryPlaceInFuelSlots(event.getInventory(), furnaceData, item)) {
                // All items were placed successfully
                event.setCurrentItem(null);
                success = true;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Shift Click] Successfully placed entire fuel stack of " + item.getAmount());
                }
            } else {
                // Could only place some items - calculate how many were actually placed
                ItemStack originalItem = event.getCurrentItem();
                int originalAmount = originalItem.getAmount();
                
                // Count how many items are actually in the fuel slots now
                int currentInSlots = countItemsInFuelSlots(event.getInventory(), furnaceData, item);
                
                // Try to find how many were placed by comparing before/after
                // This is a more reliable method than the previous approach
                int actuallyPlaced = Math.min(originalAmount, calculatePlacedAmount(furnaceData, item, true));
                
                if (actuallyPlaced > 0) {
                    originalItem.setAmount(originalAmount - actuallyPlaced);
                    if (originalItem.getAmount() <= 0) {
                        event.setCurrentItem(null);
                    }
                    success = true;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Shift Click] Partially placed fuel stack: " + actuallyPlaced + "/" + originalAmount);
                    }
                } else {
                    failureReason = "Fuel slots are full!";
                }
            }
        } else {
            // CRITICAL FIX: Only try input slots for non-fuel items, NEVER output slots
            if (tryPlaceInInputSlots(event.getInventory(), furnaceData, item)) {
                // All items were placed successfully
                event.setCurrentItem(null);
                success = true;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Shift Click] Successfully placed entire input stack of " + item.getAmount());
                }
            } else {
                // Could only place some items - calculate how many were actually placed
                ItemStack originalItem = event.getCurrentItem();
                int originalAmount = originalItem.getAmount();
                
                int actuallyPlaced = Math.min(originalAmount, calculatePlacedAmount(furnaceData, item, false));
                
                if (actuallyPlaced > 0) {
                    originalItem.setAmount(originalAmount - actuallyPlaced);
                    if (originalItem.getAmount() <= 0) {
                        event.setCurrentItem(null);
                    }
                    success = true;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Shift Click] Partially placed input stack: " + actuallyPlaced + "/" + originalAmount);
                    }
                } else {
                    failureReason = "Input slots are full!";
                }
            }
        }
        
        // CRITICAL FIX: Only show failure message if nothing was placed
        if (!success && !failureReason.isEmpty()) {
            player.sendMessage(ChatColor.RED + failureReason);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Shift Click] Failed to place any items: " + failureReason);
            }
        }
        
        // Update GUI regardless of success/failure
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
        }, 1L);
    }

    /**
     * Calculate how many items were actually placed by checking slot capacity
     * This is more reliable than counting existing items
     */
    private int calculatePlacedAmount(FurnaceData furnaceData, ItemStack item, boolean isFuel) {
        int totalCapacity = 0;
        int maxStackSize = item.getMaxStackSize();
        
        if (isFuel) {
            FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
            
            for (int i = 0; i < layout.fuelSlots.length; i++) {
                ItemStack slotItem = furnaceData.getFuelSlot(i);
                
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    // Empty slot can hold full stack
                    totalCapacity += maxStackSize;
                } else if (slotItem.isSimilar(item)) {
                    // Partially filled slot
                    int spaceAvailable = maxStackSize - slotItem.getAmount();
                    totalCapacity += Math.max(0, spaceAvailable);
                }
                // Different item type = no capacity
            }
        } else {
            FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
            
            for (int i = 0; i < layout.inputSlots.length; i++) {
                ItemStack slotItem = furnaceData.getInputSlot(i);
                
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    // Empty slot can hold full stack
                    totalCapacity += maxStackSize;
                } else if (slotItem.isSimilar(item)) {
                    // Partially filled slot
                    int spaceAvailable = maxStackSize - slotItem.getAmount();
                    totalCapacity += Math.max(0, spaceAvailable);
                }
                // Different item type = no capacity
            }
        }
        
        return totalCapacity;
    }

    /**
     * Try to place an item in input slots - ENHANCED: Never try output slots
     */
    private boolean tryPlaceInInputSlots(Inventory gui, FurnaceData furnaceData, ItemStack item) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        ItemStack remainingItems = item.clone();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Input Slots] Attempting to place " + item.getType().name() + " x" + item.getAmount());
        }
        
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
                    
                    // CRITICAL FIX: Update furnace data immediately
                    furnaceData.setInputSlot(i, slotItem);
                    
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
                
                // CRITICAL FIX: Update furnace data immediately
                furnaceData.setInputSlot(i, newStack);
                
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
     * Try to place an item in fuel slots - ENHANCED: Better feedback and data sync
     */
    private boolean tryPlaceInFuelSlots(Inventory gui, FurnaceData furnaceData, ItemStack item) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        ItemStack remainingItems = item.clone();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Fuel Slots] Attempting to place " + item.getType().name() + " x" + item.getAmount());
        }
        
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
                    
                    // CRITICAL FIX: Update furnace data immediately
                    furnaceData.setFuelSlot(i, slotItem);
                    
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
                
                // CRITICAL FIX: Update furnace data immediately
                furnaceData.setFuelSlot(i, newStack);
                
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
     * Handle input slot interactions - ENHANCED: Immediate sync with delay
     * Step 3: Input slot management
     */
    private void handleInputSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        // Allow the click to proceed normally for input slots
        // The sync will happen after the click is processed
        
        // CRITICAL FIX: Schedule both immediate and delayed sync
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Immediate sync
            syncAndUpdateGUI(player, furnaceData);
        });
        
        // ADDITIONAL SAFETY: Schedule another sync after a small delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            syncAndUpdateGUI(player, furnaceData);
            
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI,
                    "[Furnace GUI] Delayed sync completed for input slot interaction by " + player.getName());
            }
        }, 2L);
    }

    /**
     * Handle fuel slot interactions - ENHANCED: Immediate sync after changes
     * Step 3: Fuel slot management
     */
    private void handleFuelSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        // Allow normal interaction with fuel slots
        // The sync will happen on the next tick to reflect the changes
        
        // Store the current state before the click
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        int fuelSlotIndex = -1;
        for (int i = 0; i < layout.fuelSlots.length; i++) {
            if (layout.fuelSlots[i] == slot) {
                fuelSlotIndex = i;
                break;
            }
        }
        
        final int finalFuelSlotIndex = fuelSlotIndex;
        
        // Schedule immediate synchronization after the click is processed
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Sync the modified slot immediately
            ItemStack currentSlotItem = event.getInventory().getItem(slot);
            furnaceData.setFuelSlot(finalFuelSlotIndex, currentSlotItem);
            
            // Force immediate synchronization for all viewers
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                String itemName = currentSlotItem != null ? 
                    currentSlotItem.getType().name() + " x" + currentSlotItem.getAmount() : "AIR";
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Fuel Slot] Player " + player.getName() + " modified fuel slot " + finalFuelSlotIndex + 
                    " to " + itemName + " - immediate sync performed");
            }
        });
    }
    
    /**
     * Handle output slot interactions - ENHANCED: Multi-player synchronization and duplication prevention
     */
    private void handleOutputSlotClick(InventoryClickEvent event, Player player, FurnaceData furnaceData, int slot) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        // CRITICAL FIX: Comprehensive protection against item placement
        
        // 1. Never allow placing items with cursor
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place items in the output slots!");
            return;
        }
        
        // 2. Block ALL shift-click attempts from player inventory
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            if (event.getRawSlot() >= event.getInventory().getSize()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place items in the output slots!");
                return;
            }
        }
        
        // 3. Block number key swaps
        if (clickType == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place items in the output slots!");
            return;
        }
        
        // 4. Block drop actions that might place items
        if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }
        
        // 5. Allow taking items from output slots only
        if (slotItem != null && slotItem.getType() != Material.AIR) {
            // Determine which output slot this is
            FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
            int outputSlotIndex = -1;
            for (int i = 0; i < layout.outputSlots.length; i++) {
                if (layout.outputSlots[i] == slot) {
                    outputSlotIndex = i;
                    break;
                }
            }
            
            if (outputSlotIndex == -1) {
                event.setCancelled(true);
                return;
            }
            
            // CRITICAL FIX: Double-check item still exists in furnace data (prevent race conditions)
            ItemStack furnaceOutputItem = furnaceData.getOutputSlot(outputSlotIndex);
            if (furnaceOutputItem == null || furnaceOutputItem.getType() == Material.AIR) {
                // Item was already taken by another player
                event.setCancelled(true);
                event.getInventory().setItem(slot, null);
                return;
            }
            
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                // Shift-click to take output - move to player inventory
                if (hasSpaceForItem(player, furnaceOutputItem)) {
                    // CRITICAL FIX: Immediately remove from furnace data BEFORE adding to inventory
                    ItemStack takenItem = furnaceOutputItem.clone();
                    furnaceData.setOutputSlot(outputSlotIndex, null);
                    
                    // Add to player inventory
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(takenItem);
                    
                    // Handle any leftover items
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    
                    // CRITICAL FIX: Immediately update the GUI slot
                    event.getInventory().setItem(slot, null);
                    event.setCancelled(true);
                    
                    // CRITICAL FIX: Force immediate sync for ALL viewing players
                    final FurnaceData finalFurnaceData = furnaceData;
                    final int finalOutputSlotIndex = outputSlotIndex;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        updateAllViewersOutputSlot(finalFurnaceData, finalOutputSlotIndex, null);
                    });
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Output Slot] " + player.getName() + " shift-clicked " + takenItem.getType().name() + 
                            " x" + takenItem.getAmount() + " from output slot " + outputSlotIndex);
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Your inventory is full!");
                }
            } else {
                // Normal click to take output
                if (cursor == null || cursor.getType() == Material.AIR) {
                    // Taking item with empty cursor
                    ItemStack takenItem = furnaceOutputItem.clone();
                    
                    // CRITICAL FIX: Immediately remove from furnace data
                    furnaceData.setOutputSlot(outputSlotIndex, null);
                    
                    // Set player cursor
                    player.setItemOnCursor(takenItem);
                    
                    // CRITICAL FIX: Immediately update the GUI slot
                    event.getInventory().setItem(slot, null);
                    event.setCancelled(true);
                    
                    // CRITICAL FIX: Force immediate sync for ALL viewing players
                    final FurnaceData finalFurnaceData = furnaceData;
                    final int finalOutputSlotIndex = outputSlotIndex;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        updateAllViewersOutputSlot(finalFurnaceData, finalOutputSlotIndex, null);
                    });
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Output Slot] " + player.getName() + " took " + takenItem.getType().name() + 
                            " x" + takenItem.getAmount() + " from output slot " + outputSlotIndex);
                    }
                } else if (cursor.isSimilar(furnaceOutputItem)) {
                    // Stacking with similar item on cursor
                    int maxStack = cursor.getMaxStackSize();
                    int currentAmount = cursor.getAmount();
                    int spaceAvailable = maxStack - currentAmount;
                    
                    if (spaceAvailable > 0) {
                        int toTake = Math.min(spaceAvailable, furnaceOutputItem.getAmount());
                        
                        // Update cursor
                        cursor.setAmount(currentAmount + toTake);
                        player.setItemOnCursor(cursor);
                        
                        // Update slot
                        if (furnaceOutputItem.getAmount() <= toTake) {
                            // CRITICAL FIX: Remove entire stack from furnace data
                            furnaceData.setOutputSlot(outputSlotIndex, null);
                            event.getInventory().setItem(slot, null);
                            
                            // CRITICAL FIX: Force immediate sync for ALL viewing players
                            final FurnaceData finalFurnaceData = furnaceData;
                            final int finalOutputSlotIndex = outputSlotIndex;
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                updateAllViewersOutputSlot(finalFurnaceData, finalOutputSlotIndex, null);
                            });
                        } else {
                            // Reduce stack size
                            ItemStack remaining = furnaceOutputItem.clone();
                            remaining.setAmount(furnaceOutputItem.getAmount() - toTake);
                            furnaceData.setOutputSlot(outputSlotIndex, remaining);
                            // CRITICAL FIX: Force immediate sync for ALL viewing players
                            final FurnaceData finalFurnaceData = furnaceData;
                            final int finalOutputSlotIndex = outputSlotIndex;
                            final ItemStack finalRemaining = remaining;
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                updateAllViewersOutputSlot(finalFurnaceData, finalOutputSlotIndex, finalRemaining);
                            });
                        }
                        
                        event.setCancelled(true);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI,
                                "[Output Slot] " + player.getName() + " stacked " + toTake + " items from output slot " + outputSlotIndex);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                } else {
                    // Different item on cursor - can't take
                    event.setCancelled(true);
                }
            }
        } else {
            // No item in slot - just cancel any interaction
            event.setCancelled(true);
        }
    }

    /**
     * Update a specific output slot for ALL players viewing this furnace
     * CRITICAL FIX: Multi-player synchronization
     */
    private void updateAllViewersOutputSlot(FurnaceData furnaceData, int outputSlotIndex, ItemStack newItem) {
        FurnaceGUILayout layout = CustomFurnaceGUI.getFurnaceLayout(furnaceData.getFurnaceType());
        if (outputSlotIndex < 0 || outputSlotIndex >= layout.outputSlots.length) {
            return;
        }
        
        int guiSlot = layout.outputSlots[outputSlotIndex];
        
        // Update ALL players viewing this furnace
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (CustomFurnaceGUI.isPlayerViewingFurnace(onlinePlayer, furnaceData)) {
                Inventory viewerGui = CustomFurnaceGUI.getActiveFurnaceGUI(onlinePlayer);
                if (viewerGui != null) {
                    viewerGui.setItem(guiSlot, newItem != null ? newItem.clone() : null);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        String itemName = newItem != null ? 
                            newItem.getType().name() + " x" + newItem.getAmount() : "AIR";
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Multi-Player Sync] Updated output slot " + outputSlotIndex + 
                            " to " + itemName + " for viewer " + onlinePlayer.getName());
                    }
                }
            }
        }
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
     * Synchronize GUI contents with furnace data and update displays - ENHANCED: Immediate mode with forced sync
     * Step 3: Synchronization helper
     */
    private void syncAndUpdateGUI(Player player, FurnaceData furnaceData) {
        try {
            Inventory gui = CustomFurnaceGUI.getActiveFurnaceGUI(player);
            
            if (gui != null && CustomFurnaceGUI.getPlayerFurnaceData(player) == furnaceData) {
                // CRITICAL FIX: Force immediate bidirectional sync
                CustomFurnaceGUI.syncGUIToFurnaceData(player);
                
                // CRITICAL FIX: Force immediate furnace-to-GUI sync to show consumed items
                CustomFurnaceGUI.forceUpdateAllSlots(player, furnaceData);
                
                // Then update the visual displays
                CustomFurnaceGUI.updateFurnaceGUI(furnaceData);
                
                if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                    plugin.debugLog(DebugSystem.GUI,
                        "[Custom Furnace GUI] Complete bidirectional sync for " + player.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Furnace GUI] Error syncing GUI for " + player.getName() + ": " + e.getMessage());
        }
    }
}