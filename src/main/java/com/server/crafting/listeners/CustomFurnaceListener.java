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
        
        // CRITICAL FIX: Handle shift-clicks from player inventory to prevent item loss
        if (slot >= inventory.getSize()) {
            // This is a click in the player's inventory
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                
                // STRICT VALIDATION: Only allow items that can actually be used
                ItemStack item = event.getCurrentItem();
                boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
                boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
                
                if (!isSmeltable && !isFuel) {
                    // PREVENT shift-click of invalid items - this prevents item loss
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
        }
        
        // Save furnace contents after any valid interaction
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
     * Handle VALID shift-click from player inventory to furnace slots - IMPROVED
     */
    private void handleValidShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, Inventory furnaceInventory) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        // We already validated this item can be used, now determine where it goes
        boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
        boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
        
        int targetSlot = -1;
        String slotType = "";
        
        if (isSmeltable) {
            targetSlot = CustomFurnaceGUI.INPUT_SLOT;
            slotType = "input";
        } else if (isFuel) {
            targetSlot = CustomFurnaceGUI.FUEL_SLOT;
            slotType = "fuel";
        }
        
        if (targetSlot == -1) {
            // This should never happen due to our validation above
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
            // Cancel the default shift-click behavior since we handled it
            event.setCancelled(true);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Moved " + item.getType() + " to " + slotType + " slot via shift-click");
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
        } else {
            // Couldn't move item, but don't lose it
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "The " + slotType + " slot is full!");
        }
    }

    /**
     * Handle input slot clicks with validation - ENHANCED
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
        
        // ADDITIONAL CHECK: Prevent dropping invalid items via other means
        if (event.getAction().toString().contains("DROP") && currentItem != null && currentItem.getType() != Material.AIR) {
            if (!CustomFurnaceManager.getInstance().canSmelt(currentItem)) {
                event.setCancelled(true);
                return false;
            }
        }
        
        return true; // Allow the click
    }

    /**
     * Handle fuel slot clicks with validation - ENHANCED
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
        
        // ADDITIONAL CHECK: Prevent dropping invalid items via other means
        if (event.getAction().toString().contains("DROP") && currentItem != null && currentItem.getType() != Material.AIR) {
            if (!CustomFurnaceManager.getInstance().isFuel(currentItem)) {
                event.setCancelled(true);
                return false;
            }
        }
        
        return true; // Allow the click
    }

    /**
     * Handle output slot clicks - STRICT: Only allow taking items
     */
    private void handleOutputSlotClick(InventoryClickEvent event, FurnaceData furnaceData, Player player) {
        ItemStack cursor = event.getCursor();
        
        // STRICT: Don't allow placing ANY items in output slot
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place items in the output slot!");
            return;
        }
        
        // Only allow taking items - this is handled normally by Minecraft
        ItemStack outputItem = event.getCurrentItem();
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Taking " + outputItem.getAmount() + "x " + outputItem.getType() + " from output");
            }
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