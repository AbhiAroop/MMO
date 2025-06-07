package com.server.crafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.crafting.data.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Listener for custom furnace interactions
 */
public class CustomFurnaceListener implements Listener {
    
    private final Main plugin;
    
    public CustomFurnaceListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.FURNACE) {
            return;
        }
        
        // Cancel the vanilla furnace opening
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        Location furnaceLocation = clickedBlock.getLocation();
        
        // Open our custom furnace GUI instead
        CustomFurnaceGUI.openFurnaceGUI(player, furnaceLocation);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom furnace GUI
        if (!CustomFurnaceGUI.isCustomFurnaceGUI(inventory)) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // CRITICAL FIX: Handle shift-clicks from player inventory to furnace slots
        if (slot >= inventory.getSize()) {
            // This is a click in the player's inventory
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                
                // Handle shift-click from player inventory
                handleShiftClickFromPlayerInventory(event, player, inventory);
            }
            return; // Allow normal interaction with player inventory
        }
        
        // Only allow interaction with functional slots
        if (!CustomFurnaceGUI.isFunctionalSlot(slot)) {
            event.setCancelled(true);
            return;
        }
        
        // Get furnace data
        Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
        if (furnaceLocation == null) {
            event.setCancelled(true);
            return;
        }
        
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
        
        // Handle different slot types with immediate validation
        if (CustomFurnaceGUI.isInputSlot(slot)) {
            if (!handleInputSlotClick(event, furnaceData, player)) {
                return; // Event was cancelled
            }
        } else if (CustomFurnaceGUI.isFuelSlot(slot)) {
            if (!handleFuelSlotClick(event, furnaceData, player)) {
                return; // Event was cancelled
            }
        } else if (CustomFurnaceGUI.isOutputSlot(slot)) {
            handleOutputSlotClick(event, furnaceData, player);
            // Save immediately after output click
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                CustomFurnaceGUI.saveFurnaceContents(inventory, furnaceData);
            });
            return;
        }
        
        // Save furnace contents after other clicks
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                CustomFurnaceGUI.saveFurnaceContents(inventory, furnaceData);
                CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Error updating furnace after click: " + e.getMessage());
            }
        });
    }

    /**
     * Handle shift-click from player inventory to furnace slots - NEW METHOD
     */
    private void handleShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, Inventory furnaceInventory) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        // Check what type of item this is and where it should go
        boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
        boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
        
        if (!isSmeltable && !isFuel) {
            // Item can't be used in furnace
            return;
        }
        
        // Cancel the default shift-click behavior
        event.setCancelled(true);
        
        int targetSlot = -1;
        
        if (isSmeltable) {
            // Try to add to input slot
            targetSlot = CustomFurnaceGUI.INPUT_SLOT;
        } else if (isFuel) {
            // Try to add to fuel slot
            targetSlot = CustomFurnaceGUI.FUEL_SLOT;
        }
        
        if (targetSlot == -1) return;
        
        // Try to add the item to the target slot
        ItemStack targetSlotItem = furnaceInventory.getItem(targetSlot);
        
        if (targetSlotItem == null || targetSlotItem.getType() == Material.AIR) {
            // Slot is empty, move entire stack
            furnaceInventory.setItem(targetSlot, item.clone());
            event.getClickedInventory().setItem(event.getSlot(), null);
        } else if (targetSlotItem.isSimilar(item)) {
            // Items are similar, try to stack
            int spaceAvailable = targetSlotItem.getMaxStackSize() - targetSlotItem.getAmount();
            int toMove = Math.min(spaceAvailable, item.getAmount());
            
            if (toMove > 0) {
                targetSlotItem.setAmount(targetSlotItem.getAmount() + toMove);
                furnaceInventory.setItem(targetSlot, targetSlotItem);
                
                if (toMove >= item.getAmount()) {
                    event.getClickedInventory().setItem(event.getSlot(), null);
                } else {
                    item.setAmount(item.getAmount() - toMove);
                    event.getClickedInventory().setItem(event.getSlot(), item);
                }
            }
        }
        
        // Save the changes
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
            if (furnaceLocation != null) {
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
                CustomFurnaceGUI.saveFurnaceContents(furnaceInventory, furnaceData);
                CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
            }
        });
    }

    /**
     * Handle input slot clicks with proper validation
     */
    private boolean handleInputSlotClick(InventoryClickEvent event, FurnaceData furnaceData, Player player) {
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        
        // If placing an item, check if it can be smelted
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!CustomFurnaceManager.getInstance().canSmelt(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item cannot be smelted!");
                return false;
            }
        }
        
        // Log the action for debugging
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            String action = cursor != null && cursor.getType() != Material.AIR ? "placing " + cursor.getType() : "taking item";
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Input slot - " + action);
        }
        
        return true; // Allow the click
    }

    /**
     * Handle fuel slot clicks with proper validation
     */
    private boolean handleFuelSlotClick(InventoryClickEvent event, FurnaceData furnaceData, Player player) {
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        
        // If placing an item, check if it's fuel
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!CustomFurnaceManager.getInstance().isFuel(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                return false;
            }
        }
        
        // Log the action for debugging
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            String action = cursor != null && cursor.getType() != Material.AIR ? "placing " + cursor.getType() : "taking item";
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Fuel slot - " + action);
        }
        
        return true; // Allow the click
    }

    /**
     * Handle output slot clicks - FIXED: Properly remove items from furnace data
     */
    private void handleOutputSlotClick(InventoryClickEvent event, FurnaceData furnaceData, Player player) {
        ItemStack cursor = event.getCursor();
        
        // Don't allow placing items in output slot
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }
        
        // If taking items from output slot, update the furnace data
        ItemStack outputItem = event.getCurrentItem();
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Taking " + outputItem.getAmount() + "x " + outputItem.getType() + " from output");
            }
            
            // CRITICAL: Update furnace data to reflect the removed output
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Check if the output slot is now empty after the click
                ItemStack remainingOutput = event.getInventory().getItem(CustomFurnaceGUI.OUTPUT_SLOT);
                if (remainingOutput == null || remainingOutput.getType() == Material.AIR) {
                    furnaceData.setOutputItem(null);
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Cleared output slot in furnace data");
                    }
                } else {
                    furnaceData.setOutputItem(remainingOutput.clone());
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Updated output slot to " + remainingOutput.getAmount() + "x " + remainingOutput.getType());
                    }
                }
            });
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom furnace GUI
        if (!CustomFurnaceGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Get furnace location and save final state
        Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
        if (furnaceLocation != null) {
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            CustomFurnaceGUI.saveFurnaceContents(inventory, furnaceData);
        }
        
        // Remove the player's active furnace GUI
        CustomFurnaceGUI.removeActiveFurnaceGUI(player);
    }

    @EventHandler(priority = EventPriority.MONITOR) // Use MONITOR to run after other plugins
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Block block = event.getBlock();
        if (block.getType() != Material.FURNACE) return;
        
        Location furnaceLocation = block.getLocation();
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
        
        // Drop any items that were in the furnace
        if (furnaceData.getInputItem() != null) {
            block.getWorld().dropItemNaturally(furnaceLocation.add(0.5, 0.5, 0.5), furnaceData.getInputItem());
        }
        if (furnaceData.getFuelItem() != null) {
            block.getWorld().dropItemNaturally(furnaceLocation.add(0.5, 0.5, 0.5), furnaceData.getFuelItem());
        }
        if (furnaceData.getOutputItem() != null) {
            block.getWorld().dropItemNaturally(furnaceLocation.add(0.5, 0.5, 0.5), furnaceData.getOutputItem());
        }
        
        // Clean up the furnace data
        CustomFurnaceManager.getInstance().handleFurnaceBreak(furnaceLocation);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Furnace broken at " + furnaceLocation.getBlockX() + 
                "," + furnaceLocation.getBlockY() + "," + furnaceLocation.getBlockZ() + " - items dropped");
        }
    }
}