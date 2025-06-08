package com.server.crafting.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
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
     * Handle inventory clicks - ENHANCED: Immediate input/fuel handling
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

        // CRITICAL: Validate that player still has access to this furnace
        Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
        if (furnaceLocation == null || 
            !CustomFurnaceManager.getInstance().hasAccessToFurnace(furnaceLocation, player)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You no longer have access to this furnace!");
            return;
        }
        
        // Rate limiting logic
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && (currentTime - lastClick) < 50L) {
            event.setCancelled(true);
            incrementViolation(player, "too fast clicking");
            return;
        }
        
        clickCount.putIfAbsent(playerId, 0);
        
        if (lastClick == null || (currentTime - lastClick) > RATE_LIMIT_WINDOW) {
            clickCount.put(playerId, 1);
        } else {
            int currentCount = clickCount.get(playerId) + 1;
            clickCount.put(playerId, currentCount);
            
            if (currentCount > 6) {
                event.setCancelled(true);
                incrementViolation(player, "too many clicks");
                return;
            }
        }
        
        lastClickTime.put(playerId, currentTime);
        
        if (violationCount.containsKey(playerId)) {
            int violations = violationCount.get(playerId);
            if (violations > 0) {
                violationCount.put(playerId, Math.max(0, violations - 2));
            }
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            if (event.isShiftClick() && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                
                ItemStack item = event.getCurrentItem();
                boolean isSmeltable = CustomFurnaceManager.getInstance().canSmelt(item);
                boolean isFuel = CustomFurnaceManager.getInstance().isFuel(item);
                
                if (!isSmeltable && !isFuel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be used in the furnace!");
                    return;
                }
                
                handleValidShiftClickFromPlayerInventory(event, player, inventory);
            }
            return;
        }
        
        // Block interactions with decorative items
        if (isDecorativeSlot(slot)) {
            event.setCancelled(true);
            return;
        }
        
        ItemStack cursor = player.getItemOnCursor();
        
        // CRITICAL: Handle output slot clicks immediately to prevent duplication
        if (CustomFurnaceGUI.isOutputSlot(slot)) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place items in the output slot!");
                return;
            }
            
            // Handle output slot immediately
            handleOutputSlotClick(player, inventory, furnaceLocation, event);
            return;
        }
        
        // ENHANCED: Handle input slot clicks immediately
        if (CustomFurnaceGUI.isInputSlot(slot)) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!CustomFurnaceManager.getInstance().canSmelt(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be smelted!");
                    return;
                }
            }
            
            // Handle input slot immediately
            handleInputSlotClick(player, inventory, furnaceLocation, event);
            return;
        }
        
        // ENHANCED: Handle fuel slot clicks immediately  
        if (CustomFurnaceGUI.isFuelSlot(slot)) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!CustomFurnaceManager.getInstance().isFuel(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This item cannot be used as fuel!");
                    return;
                }
            }
            
            // Handle fuel slot immediately
            handleFuelSlotClick(player, inventory, furnaceLocation, event);
            return;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Processing valid click on slot " + slot);
        }
    }

    /**
     * Handle fuel slot clicks immediately to prevent fuel issues - ENHANCED: Prevent double consumption
     */
    private void handleFuelSlotClick(Player player, Inventory inventory, Location furnaceLocation, InventoryClickEvent event) {
        try {
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            if (furnaceData == null) return;
            
            // Get the current state immediately
            ItemStack currentFuelInGUI = inventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
            ItemStack playerCursor = player.getItemOnCursor();
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Fuel click - GUI has: " + getItemDebugName(currentFuelInGUI) + 
                    ", Cursor has: " + getItemDebugName(playerCursor));
            }
            
            // Let the click happen naturally (don't cancel it)
            // But immediately update the furnace data after the click
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    // Get the new state after the click
                    ItemStack newFuelInGUI = inventory.getItem(CustomFurnaceGUI.FUEL_SLOT);
                    
                    // Clean null/air items
                    if (newFuelInGUI != null && (newFuelInGUI.getType() == Material.AIR || newFuelInGUI.getAmount() <= 0)) {
                        newFuelInGUI = null;
                    }
                    
                    // CRITICAL: Update the furnace data immediately
                    ItemStack oldFuel = furnaceData.getFuelItem();
                    furnaceData.setFuelItem(newFuelInGUI != null ? newFuelInGUI.clone() : null);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Updated fuel immediately - was: " + getItemDebugName(oldFuel) + 
                            ", now: " + getItemDebugName(newFuelInGUI));
                    }
                    
                    // CRITICAL: Only try to start smelting if fuel was ADDED, not removed
                    if (newFuelInGUI != null && !furnaceData.isActive() && furnaceData.getInputItem() != null) {
                        // Check if this is actually new fuel being added (not just fuel already being consumed)
                        boolean shouldTryStart = true;
                        
                        // Don't start if fuel is currently burning and we're just updating the slot
                        if (furnaceData.getFuelTime() > 0 && furnaceData.hasFuel()) {
                            shouldTryStart = false;
                            
                            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                                Main.getInstance().debugLog(DebugSystem.GUI, 
                                    "[Custom Furnace] Fuel already burning, not starting new smelting process");
                            }
                        }
                        
                        if (shouldTryStart) {
                            CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                            
                            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                                Main.getInstance().debugLog(DebugSystem.GUI, 
                                    "[Custom Furnace] New fuel added, attempting to restart smelting");
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("[Custom Furnace] Error handling fuel click: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in fuel slot handler: " + e.getMessage());
        }
    }

    /**
     * Handle input slot clicks immediately to prevent smelting continuation - NEW METHOD
     */
    private void handleInputSlotClick(Player player, Inventory inventory, Location furnaceLocation, InventoryClickEvent event) {
        try {
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            if (furnaceData == null) return;
            
            // Get the current state immediately
            ItemStack currentInputInGUI = inventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
            ItemStack playerCursor = player.getItemOnCursor();
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Input click - GUI has: " + getItemDebugName(currentInputInGUI) + 
                    ", Cursor has: " + getItemDebugName(playerCursor) + 
                    ", Furnace currently active: " + furnaceData.isActive());
            }
            
            // Let the click happen naturally (don't cancel it)
            // But immediately update the furnace data after the click
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    // Get the new state after the click
                    ItemStack newInputInGUI = inventory.getItem(CustomFurnaceGUI.INPUT_SLOT);
                    
                    // Clean null/air items
                    if (newInputInGUI != null && (newInputInGUI.getType() == Material.AIR || newInputInGUI.getAmount() <= 0)) {
                        newInputInGUI = null;
                    }
                    
                    // CRITICAL: Update the furnace data immediately
                    ItemStack oldInput = furnaceData.getInputItem();
                    furnaceData.setInputItem(newInputInGUI != null ? newInputInGUI.clone() : null);
                    
                    // CRITICAL: If input was removed during active smelting, stop the process immediately
                    if (furnaceData.isActive() && (oldInput != null && oldInput.getAmount() > 0) && 
                        (newInputInGUI == null || newInputInGUI.getAmount() == 0)) {
                        
                        furnaceData.setActive(false);
                        furnaceData.setSmeltingResult(null);
                        furnaceData.setSmeltTime(0);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Custom Furnace] Input removed during smelting - STOPPED process immediately");
                        }
                    }
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Updated input immediately - was: " + getItemDebugName(oldInput) + 
                            ", now: " + getItemDebugName(newInputInGUI) + 
                            ", active: " + furnaceData.isActive());
                    }
                    
                    // Try to restart smelting if new input was added
                    if (newInputInGUI != null && !furnaceData.isActive()) {
                        CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Custom Furnace] New input added, attempting to restart smelting");
                        }
                    }
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("[Custom Furnace] Error handling input click: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in input slot handler: " + e.getMessage());
        }
    }

    /**
     * Handle output slot clicks immediately to prevent duplication - NEW METHOD
     */
    private void handleOutputSlotClick(Player player, Inventory inventory, Location furnaceLocation, InventoryClickEvent event) {
        try {
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            if (furnaceData == null) return;
            
            // Get the current state immediately
            ItemStack currentOutputInGUI = inventory.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
            ItemStack playerCursor = player.getItemOnCursor();
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Output click - GUI has: " + getItemDebugName(currentOutputInGUI) + 
                    ", Cursor has: " + getItemDebugName(playerCursor));
            }
            
            // Let the click happen naturally (don't cancel it)
            // But immediately update the furnace data after the click
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    // Get the new state after the click
                    ItemStack newOutputInGUI = inventory.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
                    
                    // Clean null/air items
                    if (newOutputInGUI != null && (newOutputInGUI.getType() == Material.AIR || newOutputInGUI.getAmount() <= 0)) {
                        newOutputInGUI = null;
                    }
                    
                    // CRITICAL: Update the furnace data immediately
                    ItemStack oldOutput = furnaceData.getOutputItem();
                    furnaceData.setOutputItem(newOutputInGUI != null ? newOutputInGUI.clone() : null);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Updated output immediately - was: " + getItemDebugName(oldOutput) + 
                            ", now: " + getItemDebugName(newOutputInGUI));
                    }
                    
                    // Try to restart smelting if output was cleared
                    if (newOutputInGUI == null && oldOutput != null) {
                        CustomFurnaceManager.getInstance().tryStartSmelting(furnaceData);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Custom Furnace] Output cleared, attempting to restart smelting");
                        }
                    }
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("[Custom Furnace] Error handling output click: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in output slot handler: " + e.getMessage());
        }
    }

    /**
     * Increment violation count with better recovery - ENHANCED: More forgiving
     */
    private void incrementViolation(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        int violations = violationCount.getOrDefault(playerId, 0) + 1;
        violationCount.put(playerId, violations);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] BLOCKED spam click - " + reason + " (violation " + violations + ")");
        }
        
        // IMPROVED: Only clear cursor after significantly more violations
        if (violations >= 12) { // Increased from 8 to 12
            clearPlayerCursorSafely(player);
            violationCount.put(playerId, 0); // Reset after clearing
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] CLEARED CURSOR for " + player.getName() + " due to excessive spam clicking");
            }
        }
    }

    /**
     * Check if a slot contains decorative/filler items that shouldn't be interacted with - NEW METHOD
     */
    private boolean isDecorativeSlot(int slot) {
        // Check if slot is a functional slot (input, fuel, output, timers)
        if (CustomFurnaceGUI.isFunctionalSlot(slot)) {
            return false;
        }
        
        // All other slots are decorative (borders, indicators, arrows, etc.)
        return true;
    }

    /**
     * Save inventory state safely - ENHANCED: Only for non-critical slots now
     */
    private void saveInventoryState(Player player, Inventory inventory, Location furnaceLocation) {
        // NOTE: This method is now only called for non-critical slots
        // Input, Fuel, and Output slots are handled immediately in their respective handlers
        
        try {
            if (!player.isOnline()) return;
            
            Location currentLocation = CustomFurnaceGUI.getFurnaceLocation(player);
            if (currentLocation == null || !currentLocation.equals(furnaceLocation)) return;
            
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            if (furnaceData == null) return;
            
            // This method now only handles general state synchronization
            // Critical slot changes (input/fuel/output) are handled immediately
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] General state synchronization (non-critical slots)");
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in general state sync: " + e.getMessage());
        }
    }

    /**
     * Safely clear a player's cursor and return items to inventory - ENHANCED: Better item preservation
     */
    private void clearPlayerCursorSafely(Player player) {
        try {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                return; // Nothing to clear
            }
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Safely clearing cursor for " + player.getName() + 
                    ": " + getItemDebugName(cursor));
            }
            
            // ENHANCED: Try to restore item to appropriate furnace slot first
            Location furnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
            if (furnaceLocation != null) {
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
                if (furnaceData != null) {
                    boolean restoredToFurnace = false;
                    
                    // Try to restore to appropriate furnace slot
                    if (CustomFurnaceManager.getInstance().canSmelt(cursor)) {
                        ItemStack currentInput = furnaceData.getInputItem();
                        if (currentInput == null || 
                            (currentInput.isSimilar(cursor) && 
                            (currentInput.getAmount() + cursor.getAmount()) <= cursor.getMaxStackSize())) {
                            
                            // Restore to input slot
                            if (currentInput == null) {
                                furnaceData.setInputItem(cursor.clone());
                            } else {
                                currentInput.setAmount(currentInput.getAmount() + cursor.getAmount());
                                furnaceData.setInputItem(currentInput);
                            }
                            restoredToFurnace = true;
                            
                            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                                Main.getInstance().debugLog(DebugSystem.GUI, 
                                    "[Custom Furnace] Restored " + getItemDebugName(cursor) + " to input slot");
                            }
                        }
                    } else if (CustomFurnaceManager.getInstance().isFuel(cursor)) {
                        ItemStack currentFuel = furnaceData.getFuelItem();
                        if (currentFuel == null || 
                            (currentFuel.isSimilar(cursor) && 
                            (currentFuel.getAmount() + cursor.getAmount()) <= cursor.getMaxStackSize())) {
                            
                            // Restore to fuel slot
                            if (currentFuel == null) {
                                furnaceData.setFuelItem(cursor.clone());
                            } else {
                                currentFuel.setAmount(currentFuel.getAmount() + cursor.getAmount());
                                furnaceData.setFuelItem(currentFuel);
                            }
                            restoredToFurnace = true;
                            
                            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                                Main.getInstance().debugLog(DebugSystem.GUI, 
                                    "[Custom Furnace] Restored " + getItemDebugName(cursor) + " to fuel slot");
                            }
                        }
                    }
                    
                    if (restoredToFurnace) {
                        // Clear cursor and update GUI
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                        
                        // Update the GUI immediately
                        Inventory gui = player.getOpenInventory().getTopInventory();
                        if (gui != null && CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                            CustomFurnaceGUI.loadFurnaceContents(gui, furnaceData);
                        }
                        return;
                    }
                }
            }
            
            // If we couldn't restore to furnace, add to player inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(cursor);
            
            // If inventory is full, drop items near player
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
                player.sendMessage(ChatColor.YELLOW + "Some items were dropped near you due to full inventory!");
            }
            
            // Clear cursor
            player.setItemOnCursor(new ItemStack(Material.AIR));
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Safely returned cursor item to " + player.getName());
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error clearing cursor safely: " + e.getMessage());
        }
    }

    /**
     * Emergency item recovery - NEW METHOD
     */
    public static void recoverLostItems(Player player, Location furnaceLocation) {
        try {
            FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
            if (furnaceData == null) return;
            
            // Check if player has items on cursor that might be lost
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                // Try to return to appropriate furnace slot
                boolean returned = false;
                
                if (CustomFurnaceManager.getInstance().canSmelt(cursor)) {
                    ItemStack currentInput = furnaceData.getInputItem();
                    if (currentInput == null) {
                        furnaceData.setInputItem(cursor.clone());
                        returned = true;
                    }
                } else if (CustomFurnaceManager.getInstance().isFuel(cursor)) {
                    ItemStack currentFuel = furnaceData.getFuelItem();
                    if (currentFuel == null) {
                        furnaceData.setFuelItem(cursor.clone());
                        returned = true;
                    }
                }
                
                if (returned) {
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    player.sendMessage(ChatColor.GREEN + "Recovered lost item to furnace!");
                }
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in item recovery: " + e.getMessage());
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
     * Clean up access tracking when player quits - ENHANCED
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastClickTime.remove(playerId);
        clickCount.remove(playerId);
        violationCount.remove(playerId);
        
        // CRITICAL: Release any furnace access when player quits
        CustomFurnaceManager.getInstance().releasePlayerAccess(event.getPlayer());
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

    /**
     * Handle explosion damage to furnaces - UPDATED: Use common explosion handler
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        
        // Check if any furnaces are in the explosion
        for (Block block : event.blockList()) {
            if (block.getType() == Material.FURNACE) {
                Location location = block.getLocation();
                handleFurnaceExplosion(location);
            }
        }
    }

    /**
     * Handle entity explosion damage to furnaces - NEW: Handle EntityExplodeEvent too
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        
        // Check if any furnaces are in the explosion
        for (Block block : event.blockList()) {
            if (block.getType() == Material.FURNACE) {
                Location location = block.getLocation();
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Furnace found in entity explosion at " + 
                        location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                }
                
                // Use the same logic as block explosion
                handleFurnaceExplosion(location);
            }
        }
    }

    /**
     * Common method to handle furnace destruction by explosion - NEW METHOD
     */
    private void handleFurnaceExplosion(Location location) {
        try {
            // Notify users BEFORE dropping items
            Player currentUser = CustomFurnaceManager.getInstance().getFurnaceUser(location);
            if (currentUser != null) {
                currentUser.sendMessage(ChatColor.RED + "Your furnace was destroyed by an explosion!");
                currentUser.closeInventory();
            }
            
            // Test if furnace data exists before attempting drop
            FurnaceData testData = CustomFurnaceManager.getInstance().getFurnaceData(location);
            if (testData == null) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] No furnace data found for explosion at " + 
                        location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                }
            } else {
                // Log what we're about to drop
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Furnace data found - Input: " + getItemDebugName(testData.getInputItem()) + 
                        ", Fuel: " + getItemDebugName(testData.getFuelItem()) + 
                        ", Output: " + getItemDebugName(testData.getOutputItem()));
                }
                
                // Drop contents using enhanced method
                CustomFurnaceManager.getInstance().dropFurnaceContentsEnhanced(location, null);
            }
            
            // Clean up access and data
            CustomFurnaceManager.getInstance().releaseFurnaceAccess(location);
            CustomFurnaceManager.getInstance().cleanupFurnaceDataOnly(location);
            
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("[Custom Furnace] Error handling furnace explosion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle furnace block breaking - ENHANCED: Use enhanced drop method
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        if (event.getBlock().getType() == Material.FURNACE) {
            Location location = event.getBlock().getLocation();
            Player breaker = event.getPlayer();
            
            // Check if someone is currently using this furnace
            Player currentUser = CustomFurnaceManager.getInstance().getFurnaceUser(location);
            if (currentUser != null && currentUser != breaker) {
                // Notify the user that their furnace was broken
                currentUser.sendMessage(ChatColor.RED + "The furnace you were using has been broken!");
                currentUser.closeInventory();
            }
            
            // UPDATED: Use the enhanced drop method instead of the simple one
            CustomFurnaceManager.getInstance().dropFurnaceContentsEnhanced(location, breaker);
            
            // Release furnace access and clean up data (without dropping items again)
            CustomFurnaceManager.getInstance().releaseFurnaceAccess(location);
            CustomFurnaceManager.getInstance().cleanupFurnaceDataOnly(location);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Furnace broken at " + location.getBlockX() + "," + 
                    location.getBlockY() + "," + location.getBlockZ() + " - dropped contents and cleaned up access");
            }
        }
    }
}