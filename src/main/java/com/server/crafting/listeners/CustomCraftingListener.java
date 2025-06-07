package com.server.crafting.listeners;

import java.util.HashMap;

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
import com.server.crafting.gui.AdvancedCraftingGUI;
import com.server.crafting.gui.AutoCraftingGUI;
import com.server.crafting.gui.CustomCraftingGUI;
import com.server.crafting.manager.CustomCraftingManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Listener for custom crafting table interactions
 */
public class CustomCraftingListener implements Listener {
    
    private final Main plugin;
    
    public CustomCraftingListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom crafting GUI
        if (!CustomCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            // Check if this is a shift-click that might move items to crafting slots
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                // CRITICAL FIX: Handle shift-click from player inventory manually to prevent items going to output slot
                event.setCancelled(true);
                
                ItemStack clickedItem = event.getCurrentItem();
                ItemStack remainingItem = tryMoveToCustomCraftingGrid(inventory, clickedItem);
                
                // Update the original stack
                if (remainingItem == null || remainingItem.getAmount() == 0) {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                } else {
                    event.getClickedInventory().setItem(event.getSlot(), remainingItem);
                }
                
                // Schedule update for crafting result
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    CustomCraftingGUI.updateCraftingResult(inventory, player);
                }, 1L);
            }
            // Allow normal interaction with player inventory for non-shift clicks
            return;
        }
        
        // Rest of the method remains the same...
        // Check if clicking on a crafting slot
        if (CustomCraftingGUI.isCraftingSlot(slot)) {
            // Allow normal item placement/removal in crafting slots
            // For shift-clicks, we need a longer delay to ensure the item is fully placed
            if (event.getClick().isShiftClick()) {
                // Shift-click needs extra delay to ensure item placement is complete
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    CustomCraftingGUI.updateCraftingResult(inventory, player);
                }, 2L); // 2 ticks instead of 1 for shift-clicks
            } else {
                // Regular clicks can use immediate scheduling
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    CustomCraftingGUI.updateCraftingResult(inventory, player);
                });
            }
            return;
        }
        
        // Check if clicking on the output slot
        if (CustomCraftingGUI.isOutputSlot(slot)) {
            event.setCancelled(true);
            
            ItemStack outputItem = inventory.getItem(slot);
            if (outputItem == null || outputItem.getType() == Material.BARRIER) {
                return; // No valid recipe result
            }
            
            // Handle shift-click for bulk crafting
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                handleBulkCrafting(player, inventory, outputItem);
            } else {
                // Handle single item crafting
                handleSingleCrafting(player, inventory, outputItem);
            }
            return;
        }

        if (CustomCraftingGUI.isAdvancedNavigationSlot(slot)) {
            event.setCancelled(true);
            
            // Clear the 3x3 crafting grid and return items
            CustomCraftingGUI.clearCraftingGrid(inventory, player);
            
            // Remove the 3x3 GUI
            CustomCraftingGUI.removeActiveCraftingGUI(player);
            
            // Open the 4x4 advanced crafting GUI
            AdvancedCraftingGUI.openAdvancedCraftingTable(player);
            return;
        }

        // Check if clicking on the auto-crafting navigation slot
        if (CustomCraftingGUI.isAutoCraftingNavigationSlot(slot)) {
            event.setCancelled(true);
            
            if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                plugin.debugLog(DebugSystem.GUI, 
                    "[Crafting] " + player.getName() + " navigating to auto-crafting GUI");
            }
            
            // Remove the 3x3 GUI
            CustomCraftingGUI.removeActiveCraftingGUI(player);
            
            // Clear any existing auto-crafting data to ensure fresh analysis
            AutoCraftingGUI.clearPlayerData(player);
            
            // Open the auto-crafting GUI
            AutoCraftingGUI.openAutoCraftingGUI(player);
            return;
        }
        
        // For all other slots (borders, decorations), cancel the event
        event.setCancelled(true);
    }

    /**
     * Helper method to manually move items to the crafting grid only
     */
    private ItemStack tryMoveToCustomCraftingGrid(Inventory craftingInventory, ItemStack itemToMove) {
        ItemStack remaining = itemToMove.clone();
        
        // Try to add to existing stacks in the crafting grid first
        for (int craftingSlot : CustomCraftingGUI.CRAFTING_SLOTS) {
            ItemStack existing = craftingInventory.getItem(craftingSlot);
            
            if (existing != null && existing.isSimilar(remaining)) {
                int canAdd = existing.getMaxStackSize() - existing.getAmount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining.getAmount());
                    existing.setAmount(existing.getAmount() + toAdd);
                    remaining.setAmount(remaining.getAmount() - toAdd);
                    
                    if (remaining.getAmount() <= 0) {
                        return null; // All moved
                    }
                }
            }
        }
        
        // Try to place in empty crafting slots
        for (int craftingSlot : CustomCraftingGUI.CRAFTING_SLOTS) {
            ItemStack existing = craftingInventory.getItem(craftingSlot);
            
            if (existing == null || existing.getType() == Material.AIR) {
                craftingInventory.setItem(craftingSlot, remaining.clone());
                return null; // All moved
            }
        }
        
        // Return remaining items if crafting grid is full
        return remaining;
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom crafting GUI
        if (!CustomCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Check if any dragged slots are outside the allowed crafting area
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize() && !CustomCraftingGUI.isCraftingSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Update the result after drag is processed - use longer delay for reliability
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            CustomCraftingGUI.updateCraftingResult(inventory, (Player) event.getWhoClicked());
        }, 2L); // Changed from runTask to runTaskLater(2L)
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom crafting GUI
        if (!CustomCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Return all items from the crafting grid to the player
        CustomCraftingGUI.clearCraftingGrid(inventory, player);
        
        // Remove the player's active crafting GUI
        CustomCraftingGUI.removeActiveCraftingGUI(player);
    }
    
    /**
     * Handle single item crafting (normal click)
     */
    private void handleSingleCrafting(Player player, Inventory inventory, ItemStack outputItem) {
        // Check cursor - if player has item on cursor, try to add to it or reject
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            // If cursor has different item type, can't pick up
            if (!cursor.isSimilar(outputItem)) {
                return; // Can't pick up different item type
            }
            
            // If cursor would overflow, can't pick up
            if (cursor.getAmount() + outputItem.getAmount() > cursor.getMaxStackSize()) {
                return; // Would exceed stack size
            }
        }
        
        // Consume ingredients from crafting grid
        ItemStack[] craftingGrid = CustomCraftingGUI.getCraftingGrid(inventory);
        CustomCraftingManager.getInstance().consumeIngredients(craftingGrid, outputItem); // Pass result
        
        // Update the crafting grid in the GUI
        for (int i = 0; i < CustomCraftingGUI.CRAFTING_SLOTS.length; i++) {
            inventory.setItem(CustomCraftingGUI.CRAFTING_SLOTS[i], craftingGrid[i]);
        }
        
        // Give item to player's cursor
        if (cursor == null || cursor.getType() == Material.AIR) {
            // Empty cursor - place item on cursor
            player.setItemOnCursor(outputItem.clone());
        } else {
            // Add to existing stack on cursor
            cursor.setAmount(cursor.getAmount() + outputItem.getAmount());
            player.setItemOnCursor(cursor);
        }
        
        // Update the result
        CustomCraftingGUI.updateCraftingResult(inventory, player);
    }

    /**
     * Handle bulk crafting (shift-click)
     */
    private void handleBulkCrafting(Player player, Inventory inventory, ItemStack outputItem) {
        ItemStack[] craftingGrid = CustomCraftingGUI.getCraftingGrid(inventory);
        
        // Calculate maximum craftable amount
        int maxCraftable = CustomCraftingManager.getInstance().getMaxCraftableAmount(craftingGrid, outputItem);
        
        if (maxCraftable <= 0) {
            return;
        }
        
        // Calculate how many we can actually give to the player by checking inventory space
        int totalToCraft = 0;
        int craftsToPerform = maxCraftable / outputItem.getAmount();
        
        // FIXED: Create a simple inventory to test space without casting to PlayerInventory
        Inventory testInventory = org.bukkit.Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                testInventory.setItem(i, item.clone());
            }
        }

        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI, 
                "[Bulk Crafting] Player: " + player.getName() + 
                ", Item: " + outputItem.getType() + 
                ", Max Craftable: " + maxCraftable + 
                ", Crafts to Perform: " + craftsToPerform);
        }
        
        // Test how many items we can actually fit
        int actualCraftsToPerform = 0;
        for (int i = 0; i < craftsToPerform; i++) {
            ItemStack testStack = outputItem.clone();
            java.util.HashMap<Integer, ItemStack> leftover = testInventory.addItem(testStack);
            
            if (!leftover.isEmpty()) {
                break; // Inventory full
            }
            
            actualCraftsToPerform++;
            totalToCraft += outputItem.getAmount();
        }
        
        if (actualCraftsToPerform <= 0) {
            player.sendMessage("§cYour inventory is full!");
            return;
        }
        
        // Actually give items and consume ingredients
        for (int i = 0; i < actualCraftsToPerform; i++) {
            // Give the item to player's inventory (shift-click behavior)
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(outputItem.clone());
            
            // Drop any items that don't fit (safety net)
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            
            // Consume ingredients
            CustomCraftingManager.getInstance().consumeIngredients(craftingGrid, outputItem);
        }
        
        // Update the crafting grid in the GUI
        for (int i = 0; i < CustomCraftingGUI.CRAFTING_SLOTS.length; i++) {
            inventory.setItem(CustomCraftingGUI.CRAFTING_SLOTS[i], craftingGrid[i]);
        }
        
        // Update the result
        CustomCraftingGUI.updateCraftingResult(inventory, player);
        
        // Send success message
        if (actualCraftsToPerform > 1) {
            player.sendMessage("§aCrafted " + totalToCraft + " " + outputItem.getType().name().toLowerCase().replace('_', ' ') + "!");
        }
    }
    
}