package com.server.crafting.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * Listener for custom furnace interactions - ENHANCED with aggressive spam protection
 */
public class CustomFurnaceListener implements Listener {
    
    private final Main plugin;
    
    // ENHANCED: Aggressive rate limiting
    private static final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final Map<UUID, Integer> clickCount = new HashMap<>();
    private static final Map<UUID, Integer> violationCount = new HashMap<>(); // NEW: Track violations
    private static final long MIN_CLICK_INTERVAL = 250L; // INCREASED from 200ms to 250ms
    private static final long RATE_LIMIT_WINDOW = 1000L; // 1 second window
    private static final int MAX_CLICKS_PER_WINDOW = 2; // REDUCED from 3 to 2 clicks per second
    private static final int MAX_VIOLATIONS_BEFORE_CURSOR_CLEAR = 5; // NEW: Clear cursor after 5 violations
    
    public CustomFurnaceListener(Main plugin) {
        this.plugin = plugin;
        startViolationCleanupTask();
    }
    
    /**
     * Handle inventory clicks - ENHANCED: Aggressive spam protection with cursor clearing
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
        
        // CRITICAL: Enhanced rate limiting with violation tracking
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Layer 1: Minimum interval check
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && (currentTime - lastClick) < MIN_CLICK_INTERVAL) {
            incrementViolation(player, "too fast: " + (currentTime - lastClick) + "ms");
            event.setCancelled(true);
            return;
        }
        
        // Layer 2: Click count limiting within time window
        clickCount.putIfAbsent(playerId, 0);
        if (lastClick == null || (currentTime - lastClick) > RATE_LIMIT_WINDOW) {
            // Reset click count if outside time window
            clickCount.put(playerId, 0);
        }
        
        int currentClickCount = clickCount.get(playerId);
        if (currentClickCount >= MAX_CLICKS_PER_WINDOW) {
            incrementViolation(player, "too many clicks: " + currentClickCount + "/" + MAX_CLICKS_PER_WINDOW);
            event.setCancelled(true);
            return;
        }
        
        // Update rate limiting data
        lastClickTime.put(playerId, currentTime);
        clickCount.put(playerId, currentClickCount + 1);
        
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
        
        // CRITICAL: Only save if the click is actually changing something
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Processing valid click on slot " + slot);
        }
        
        // If we get here, the click is valid - let it proceed and save after a delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                // ENHANCED: Only save if the GUI is still open and valid
                if (!player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                    return; // Player closed GUI, don't save
                }
                
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
                
                // ENHANCED: Only update if something actually changed
                boolean changed = false;
                ItemStack currentInput = furnaceData.getInputItem();
                ItemStack currentFuel = furnaceData.getFuelItem();
                ItemStack currentOutput = furnaceData.getOutputItem();
                
                if (!itemsEqual(currentInput, inputItem)) {
                    furnaceData.setInputItem(inputItem != null ? inputItem.clone() : null);
                    changed = true;
                }
                if (!itemsEqual(currentFuel, fuelItem)) {
                    furnaceData.setFuelItem(fuelItem != null ? fuelItem.clone() : null);
                    changed = true;
                }
                if (!itemsEqual(currentOutput, outputItem)) {
                    furnaceData.setOutputItem(outputItem != null ? outputItem.clone() : null);
                    changed = true;
                }
                
                if (changed) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Saved contents after click - Input: " + getItemDebugName(inputItem) + 
                            ", Fuel: " + getItemDebugName(fuelItem) + 
                            ", Output: " + getItemDebugName(outputItem));
                    }
                    
                    // Try to start smelting after saving items
                    CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                } else {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] No changes detected, skipping save");
                    }
                }
                
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("[Custom Furnace] Error saving after click: " + e.getMessage());
            }
        }, 3L); // INCREASED delay to 3 ticks for extra safety
    }

    /**
     * Increment violation count and handle punishment - NEW METHOD
     */
    private void incrementViolation(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        int violations = violationCount.getOrDefault(playerId, 0) + 1;
        violationCount.put(playerId, violations);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] BLOCKED spam click - " + reason + " (violation " + violations + ")");
        }
        
        // Clear cursor after too many violations
        if (violations >= MAX_VIOLATIONS_BEFORE_CURSOR_CLEAR) {
            clearPlayerCursorSafely(player);
            violationCount.put(playerId, 0); // Reset violation count after punishment
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] CLEARED CURSOR for " + player.getName() + " due to excessive spam clicking");
            }
            
            player.sendMessage(ChatColor.RED + "⚠ Cursor cleared due to excessive clicking!");
        }
    }

    /**
     * Safely clear a player's cursor and return items to inventory - NEW METHOD
     */
    private void clearPlayerCursorSafely(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            // Try to add the item back to player's inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(cursor);
            
            // If inventory is full, drop items at player's location
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                player.sendMessage(ChatColor.YELLOW + "Some items were dropped because your inventory is full!");
            }
            
            // Clear the cursor
            player.setItemOnCursor(null);
        }
    }

    /**
     * Clean up old violation data periodically - NEW TASK
     */
    private void startViolationCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // Remove old violation data (older than 30 seconds)
            violationCount.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                Long lastClick = lastClickTime.get(playerId);
                return lastClick == null || (currentTime - lastClick) > 30000L;
            });
            
        }, 600L, 600L); // Run every 30 seconds
    }

    /**
     * Check if a player has recent violations - PUBLIC METHOD for manager access
     */
    public static boolean hasRecentViolations(UUID playerId) {
        int violations = violationCount.getOrDefault(playerId, 0);
        return violations > 2; // Consider recent if more than 2 violations
    }

    /**
     * Reset violation count for a player - PUBLIC METHOD
     */
    public static void resetViolations(UUID playerId) {
        violationCount.remove(playerId);
    }

    /**
     * Get the last click time map for interaction detection - PUBLIC ACCESS
     */
    public static Map<UUID, Long> getLastClickTimeMap() {
        return lastClickTime;
    }

    /**
     * Check if a player is currently interacting with a furnace GUI - PUBLIC METHOD
     */
    public static boolean isPlayerCurrentlyInteracting(UUID playerId) {
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null) {
            long timeSinceLastClick = System.currentTimeMillis() - lastClick;
            return timeSinceLastClick < 500L; // 500ms interaction window
        }
        return false;
    }

    /**
     * Check if two items are equal for comparison purposes
     */
    private boolean itemsEqual(ItemStack item1, ItemStack item2) {
        // Both null
        if (item1 == null && item2 == null) return true;
        
        // One null, one not
        if (item1 == null || item2 == null) return false;
        
        // Both air
        if (item1.getType() == Material.AIR && item2.getType() == Material.AIR) return true;
        
        // One air, one not
        if (item1.getType() == Material.AIR || item2.getType() == Material.AIR) return false;
        
        // Compare type and amount
        if (item1.getType() != item2.getType()) return false;
        if (item1.getAmount() != item2.getAmount()) return false;
        
        return true;
    }

    /**
     * Handle VALID shift-click from player inventory to furnace slots
     */
    private void handleValidShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, Inventory furnaceInventory) {
        // Existing implementation remains the same...
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
            
            // Save changes after a delay
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
            }, 2L);
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
     * Clean up violation tracking when player quits - ENHANCED
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastClickTime.remove(playerId);
        clickCount.remove(playerId);
        violationCount.remove(playerId); // NEW: Clean up violation tracking
        CustomFurnaceGUI.removeActiveFurnaceGUI(event.getPlayer());
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Existing implementation remains the same...
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (GUI_TITLE.equals(title)) {
            Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
            if (furnaceLocation != null) {
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
                
                // Save contents properly before closing
                Inventory inventory = event.getInventory();
                
                // Get items from GUI slots BEFORE clearing associations
                ItemStack inputItem = inventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
                ItemStack fuelItem = inventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
                ItemStack outputItem = inventory.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
                
                // Save to furnace data with proper null checking
                if (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0) {
                    furnaceData.setInputItem(inputItem.clone());
                } else {
                    furnaceData.setInputItem(null);
                }
                
                if (fuelItem != null && fuelItem.getType() != Material.AIR && fuelItem.getAmount() > 0) {
                    furnaceData.setFuelItem(fuelItem.clone());
                } else {
                    furnaceData.setFuelItem(null);
                }
                
                if (outputItem != null && outputItem.getType() != Material.AIR && outputItem.getAmount() > 0) {
                    furnaceData.setOutputItem(outputItem.clone());
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