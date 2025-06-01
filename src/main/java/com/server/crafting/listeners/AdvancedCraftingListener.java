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
import com.server.crafting.gui.CustomCraftingGUI;
import com.server.crafting.manager.CustomCraftingManager;

/**
 * Listener for advanced 4x4 crafting table interactions
 */
public class AdvancedCraftingListener implements Listener {
    
    private final Main plugin;
    
    public AdvancedCraftingListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is our advanced crafting GUI
        if (!AdvancedCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            // Handle shift-clicks from player inventory
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                
                ItemStack clickedItem = event.getCurrentItem();
                ItemStack remainingItem = tryMoveToAdvancedCraftingGrid(inventory, clickedItem);
                
                // Update the original stack
                if (remainingItem == null || remainingItem.getAmount() == 0) {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                } else {
                    event.getClickedInventory().setItem(event.getSlot(), remainingItem);
                }
                
                // Schedule update for crafting result
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    AdvancedCraftingGUI.updateCraftingResult(inventory, player);
                }, 1L);
            }
            return;
        }
        
        // Check if clicking on a crafting slot
        if (AdvancedCraftingGUI.isCraftingSlot(slot)) {
            // Allow normal item placement/removal in crafting slots
            if (event.getClick().isShiftClick()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    AdvancedCraftingGUI.updateCraftingResult(inventory, player);
                }, 2L);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    AdvancedCraftingGUI.updateCraftingResult(inventory, player);
                });
            }
            return;
        }
        
        // Check if clicking on an output slot
        if (AdvancedCraftingGUI.isOutputSlot(slot)) {
            event.setCancelled(true);
            
            ItemStack outputItem = inventory.getItem(slot);
            if (outputItem == null || outputItem.getType() == Material.BARRIER) {
                return; // No valid recipe result
            }
            
            // Handle shift-click for bulk crafting
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                handleAdvancedBulkCrafting(player, inventory, outputItem, slot);
            } else {
                // Handle single item crafting
                handleAdvancedSingleCrafting(player, inventory, outputItem, slot);
            }
            return;
        }
        
        // Check if clicking on the navigation arrow
        if (AdvancedCraftingGUI.isNavigationSlot(slot)) {
            event.setCancelled(true);
            
            // Clear the advanced crafting grid and return items
            AdvancedCraftingGUI.clearCraftingGrid(inventory, player);
            
            // Remove the advanced GUI
            AdvancedCraftingGUI.removeActiveAdvancedCraftingGUI(player);
            
            // Open the 3x3 crafting GUI
            CustomCraftingGUI.openCraftingTable(player);
            return;
        }
        
        // For all other slots (borders, decorations), cancel the event
        event.setCancelled(true);
    }

    /**
     * Helper method to manually move items to the 4x4 crafting grid only
     */
    private ItemStack tryMoveToAdvancedCraftingGrid(Inventory craftingInventory, ItemStack itemToMove) {
        ItemStack remaining = itemToMove.clone();
        
        // Try to add to existing stacks in the crafting grid first
        for (int craftingSlot : AdvancedCraftingGUI.CRAFTING_SLOTS) {
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
        for (int craftingSlot : AdvancedCraftingGUI.CRAFTING_SLOTS) {
            ItemStack existing = craftingInventory.getItem(craftingSlot);
            
            if (existing == null || existing.getType() == Material.AIR) {
                craftingInventory.setItem(craftingSlot, remaining.clone());
                return null; // All moved
            }
        }
        
        // Return remaining items if crafting grid is full
        return remaining;
    }

    /**
     * Handle single item crafting from advanced output slot
     */
    private void handleAdvancedSingleCrafting(Player player, Inventory inventory, ItemStack outputItem, int outputSlot) {
        // Check cursor compatibility
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!cursor.isSimilar(outputItem)) {
                player.sendMessage("§cCannot pick up different item type!");
                return;
            }
            
            if (cursor.getAmount() + outputItem.getAmount() > cursor.getMaxStackSize()) {
                player.sendMessage("§cNot enough space on cursor!");
                return;
            }
        }
        
        // Get all current outputs
        ItemStack[] currentOutputs = new ItemStack[AdvancedCraftingGUI.OUTPUT_SLOTS.length];
        for (int i = 0; i < AdvancedCraftingGUI.OUTPUT_SLOTS.length; i++) {
            currentOutputs[i] = inventory.getItem(AdvancedCraftingGUI.OUTPUT_SLOTS[i]);
        }
        
        // Consume ingredients from 4x4 crafting grid
        ItemStack[] craftingGrid = AdvancedCraftingGUI.getCraftingGrid(inventory);
        // FIXED: Pass player parameter to avoid null pointer exception
        CustomCraftingManager.getInstance().consumeAdvancedIngredients(craftingGrid, currentOutputs, player);
        
        // Update the crafting grid in the GUI
        for (int i = 0; i < AdvancedCraftingGUI.CRAFTING_SLOTS.length; i++) {
            inventory.setItem(AdvancedCraftingGUI.CRAFTING_SLOTS[i], craftingGrid[i]);
        }
        
        // Give item to player's cursor
        if (cursor == null || cursor.getType() == Material.AIR) {
            player.setItemOnCursor(outputItem.clone());
        } else {
            cursor.setAmount(cursor.getAmount() + outputItem.getAmount());
            player.setItemOnCursor(cursor);
        }
        
        // Update the result
        AdvancedCraftingGUI.updateCraftingResult(inventory, player);
    }

    /**
     * Handle bulk crafting from advanced output slot
     */
    private void handleAdvancedBulkCrafting(Player player, Inventory inventory, ItemStack outputItem, int outputSlot) {
        ItemStack[] craftingGrid = AdvancedCraftingGUI.getCraftingGrid(inventory);
        
        // Calculate maximum craftable amount
        // FIXED: Pass player parameter to avoid null pointer exception
        int maxCraftable = CustomCraftingManager.getInstance().getMaxAdvancedCraftableAmount(craftingGrid, outputItem, player);
        
        if (maxCraftable <= 0) {
            player.sendMessage("§cNot enough materials!");
            return;
        }
        
        // Calculate how many we can actually give to the player by checking inventory space
        int totalToCraft = 0;
        int craftsToPerform = maxCraftable / outputItem.getAmount();
        
        // Create a simple inventory to test space
        org.bukkit.inventory.Inventory testInventory = org.bukkit.Bukkit.createInventory(null, 36);
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                testInventory.setItem(i, item.clone());
            }
        }
        
        // Test how many items we can actually fit
        int actualCraftsToPerform = 0;
        for (int i = 0; i < craftsToPerform; i++) {
            HashMap<Integer, ItemStack> leftover = testInventory.addItem(outputItem.clone());
            if (leftover.isEmpty()) {
                actualCraftsToPerform++;
            } else {
                break; // Can't fit any more
            }
        }
        
        if (actualCraftsToPerform <= 0) {
            player.sendMessage("§cNot enough inventory space!");
            return;
        }
        
        // Get all current outputs for each craft
        ItemStack[] currentOutputs = new ItemStack[AdvancedCraftingGUI.OUTPUT_SLOTS.length];
        for (int i = 0; i < AdvancedCraftingGUI.OUTPUT_SLOTS.length; i++) {
            currentOutputs[i] = inventory.getItem(AdvancedCraftingGUI.OUTPUT_SLOTS[i]);
        }
        
        // Actually give items and consume ingredients
        for (int i = 0; i < actualCraftsToPerform; i++) {
            // Give the item to player
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(outputItem.clone());
            
            // Drop any items that don't fit (shouldn't happen due to our check above)
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            
            // Consume ingredients
            // FIXED: Pass player parameter to avoid null pointer exception
            CustomCraftingManager.getInstance().consumeAdvancedIngredients(craftingGrid, currentOutputs, player);
        }
        
        // Update the crafting grid in the GUI
        for (int i = 0; i < AdvancedCraftingGUI.CRAFTING_SLOTS.length; i++) {
            inventory.setItem(AdvancedCraftingGUI.CRAFTING_SLOTS[i], craftingGrid[i]);
        }
        
        // Update the result
        AdvancedCraftingGUI.updateCraftingResult(inventory, player);
        
        // Send success message
        if (actualCraftsToPerform > 1) {
            player.sendMessage("§aCrafted " + (actualCraftsToPerform * outputItem.getAmount()) + "x " + outputItem.getType().name() + "!");
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory inventory = event.getInventory();
        
        // Check if this is our advanced crafting GUI
        if (!AdvancedCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Check if any dragged slots are outside the allowed crafting area
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize() && !AdvancedCraftingGUI.isCraftingSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Update the result after drag is processed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            AdvancedCraftingGUI.updateCraftingResult(inventory, (Player) event.getWhoClicked());
        }, 2L);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is our advanced crafting GUI
        if (!AdvancedCraftingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Return all items from the crafting grid to the player
        AdvancedCraftingGUI.clearCraftingGrid(inventory, player);
        
        // Remove the player's active advanced crafting GUI
        AdvancedCraftingGUI.removeActiveAdvancedCraftingGUI(player);
    }
}