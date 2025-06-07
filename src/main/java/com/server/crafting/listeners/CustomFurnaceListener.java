package com.server.crafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.crafting.data.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
import static com.server.crafting.gui.CustomFurnaceGUI.GUI_TITLE;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Listener for custom furnace interactions - SIMPLIFIED VERSION
 */
public class CustomFurnaceListener implements Listener {
    
    private final Main plugin;
    
    public CustomFurnaceListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory clicks - FIXED: Ensure smelting starts after item placement
     */
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
        
        // Handle shift-clicks from player inventory to furnace
        if (slot >= inventory.getSize()) {
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                
                ItemStack item = event.getCurrentItem();
                boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
                boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
                
                if (!isSmeltable && !isFuel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be used in the furnace!");
                    return;
                }
                
                // Handle valid shift-click
                handleValidShiftClickFromPlayerInventory(event, player, inventory);
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
        
        // Validate what the player is trying to do
        ItemStack cursor = event.getCursor();
        
        if (CustomFurnaceGUI.isInputSlot(slot)) {
            // Input slot validation
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!CustomFurnaceManager.getInstance().canSmelt(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be smelted!");
                    return;
                }
            }
        } else if (CustomFurnaceGUI.isFuelSlot(slot)) {
            // Fuel slot validation
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!CustomFurnaceManager.getInstance().isFuel(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                    return;
                }
            }
        } else if (CustomFurnaceGUI.isOutputSlot(slot)) {
            // Output slot validation - STRICT: no placing items
            if (cursor != null && cursor.getType() != Material.AIR) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place items in the output slot!");
                return;
            }
        }
        
        // If we get here, the click is valid - let it proceed and save after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                // Get the current state of slots after the click
                ItemStack inputItem = inventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
                ItemStack fuelItem = inventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
                ItemStack outputItem = inventory.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
                
                // Clean null/air items
                if (inputItem != null && (inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0)) {
                    inputItem = null;
                }
                if (fuelItem != null && (fuelItem.getType() == Material.AIR || fuelItem.getAmount() <= 0)) {
                    fuelItem = null;
                }
                if (outputItem != null && (outputItem.getType() == Material.AIR || outputItem.getAmount() <= 0)) {
                    outputItem = null;
                }
                
                // Save to furnace data
                furnaceData.setInputItem(inputItem != null ? inputItem.clone() : null);
                furnaceData.setFuelItem(fuelItem != null ? fuelItem.clone() : null);
                furnaceData.setOutputItem(outputItem != null ? outputItem.clone() : null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Saved contents after click - Input: " + getItemDebugName(inputItem) + 
                        ", Fuel: " + getItemDebugName(fuelItem) + 
                        ", Output: " + getItemDebugName(outputItem));
                }
                
                // CRITICAL: Try to start smelting after saving items
                CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("[Custom Furnace] Error saving after click: " + e.getMessage());
            }
        }, 1L);
    }

    /**
     * Handle VALID shift-click from player inventory to furnace slots
     */
    private void handleValidShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, Inventory furnaceInventory) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        // Determine target slot
        boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
        boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
        
        int targetSlot = -1;
        if (isSmeltable) {
            targetSlot = CustomFurnaceGUI.INPUT_SLOT;
        } else if (isFuel) {
            targetSlot = CustomFurnaceGUI.FUEL_SLOT;
        }
        
        if (targetSlot == -1) {
            event.setCancelled(true);
            return;
        }
        
        // Try to add the item to the target slot
        ItemStack targetSlotItem = furnaceInventory.getItem(targetSlot);
        boolean moved = false;
        
        if (targetSlotItem == null || targetSlotItem.getType() == Material.AIR) {
            // Slot is empty, move entire stack
            furnaceInventory.setItem(targetSlot, item.clone());
            event.getClickedInventory().setItem(event.getSlot(), null);
            moved = true;
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
                moved = true;
            }
        }
        
        if (moved) {
            event.setCancelled(true);
            
            // Save changes after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
                if (furnaceLocation != null) {
                    FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
                    
                    // Update furnace data
                    ItemStack inputItem = furnaceInventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
                    ItemStack fuelItem = furnaceInventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
                    
                    furnaceData.setInputItem(inputItem != null && inputItem.getType() != Material.AIR ? inputItem.clone() : null);
                    furnaceData.setFuelItem(fuelItem != null && fuelItem.getType() != Material.AIR ? fuelItem.clone() : null);
                    
                    CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                }
            }, 1L);
        } else {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "The target slot is full!");
        }
    }

    /**
     * Get debug name for an item
     */
    private String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        return item.getAmount() + "x " + item.getType().name();
    }

    /**
     * Clean up when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CustomFurnaceGUI.removeActiveFurnaceGUI(event.getPlayer());
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (GUI_TITLE.equals(title)) {
            Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
            if (furnaceLocation != null) {
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
                
                // CRITICAL FIX: Save contents properly before closing
                Inventory inventory = event.getInventory();
                
                // Get items from GUI slots BEFORE clearing associations
                ItemStack inputItem = inventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
                ItemStack fuelItem = inventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
                ItemStack outputItem = inventory.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
                
                // Save to furnace data with proper null checking
                if (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0) {
                    furnaceData.setInputItem(inputItem.clone());
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Saved input on close: " + inputItem.getAmount() + "x " + inputItem.getType());
                    }
                } else {
                    furnaceData.setInputItem(null);
                }
                
                if (fuelItem != null && fuelItem.getType() != Material.AIR && fuelItem.getAmount() > 0) {
                    furnaceData.setFuelItem(fuelItem.clone());
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Saved fuel on close: " + fuelItem.getAmount() + "x " + fuelItem.getType());
                    }
                } else {
                    furnaceData.setFuelItem(null);
                }
                
                if (outputItem != null && outputItem.getType() != Material.AIR && outputItem.getAmount() > 0) {
                    furnaceData.setOutputItem(outputItem.clone());
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Saved output on close: " + outputItem.getAmount() + "x " + outputItem.getType());
                    }
                } else {
                    furnaceData.setOutputItem(null);
                }
                
                // Try to start smelting after saving items
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                }, 1L);
            }
            
            // Remove GUI association AFTER saving
            CustomFurnaceGUI.removeActiveFurnaceGUI(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.FURNACE) {
            CustomFurnaceManager.getInstance().handleFurnaceBreak(event.getBlock().getLocation());
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Furnace broken at " + event.getBlock().getLocation());
            }
        }
    }
}