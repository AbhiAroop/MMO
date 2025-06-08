package com.server.crafting.data;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;
import com.server.items.ItemManager;

/**
 * Data class to store furnace state and processing information
 */
public class FurnaceData {
    
    private final Location location;
    private ItemStack inputItem;
    private ItemStack fuelItem;
    private ItemStack outputItem;
    
    // Smelting progress
    private boolean isActive;
    private int smeltTime;
    private int maxSmeltTime;
    private ItemStack smeltingResult;
    
    // Fuel data
    private boolean hasFuel;
    private int fuelTime;
    private int maxFuelTime;
    
    public FurnaceData(Location location) {
        this.location = location.clone();
        this.inputItem = null;
        this.fuelItem = null;
        this.outputItem = null;
        this.isActive = false;
        this.smeltTime = 0;
        this.maxSmeltTime = 200; // 10 seconds (200 ticks)
        this.smeltingResult = null;
        this.hasFuel = false;
        this.fuelTime = 0;
        this.maxFuelTime = 0;
    }
    
    // Location methods
    public Location getLocation() {
        return location.clone();
    }
    
    // Item getters and setters
    public ItemStack getInputItem() {
        return inputItem != null ? inputItem.clone() : null;
    }
    
    /**
     * Set input item and update cook time based on item - ENHANCED
     */
    public void setInputItem(ItemStack inputItem) {
        this.inputItem = inputItem != null ? inputItem.clone() : null;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Set input item: " + getItemDebugName(inputItem));
        }
        
        // Reset smelting if input changes
        if (inputItem == null) {
            smeltTime = 0;
            smeltingResult = null;
            setActive(false);
        } else {
            // Update max smelt time based on the input item
            int customCookTime = CustomFurnaceManager.getInstance().getCookTime(inputItem);
            setMaxSmeltTime(customCookTime);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Updated cook time to " + customCookTime + " ticks for item: " + 
                    getItemDebugName(inputItem));
            }
            
            updateSmeltingResult();
        }
    }

    /**
     * Get fuel value for an item - ENHANCED: Support custom items
     */
    public static int getFuelValue(ItemStack item) {
        if (item == null) return 0;
        
        // Use the manager's enhanced fuel value method
        return CustomFurnaceManager.getFuelValue(item);
    }

    /**
     * Check if an item is fuel - ENHANCED: Support custom items
     */
    public static boolean isFuel(ItemStack item) {
        if (item == null) return false;
        
        // Use the manager's enhanced fuel check method
        return CustomFurnaceManager.getInstance().isFuel(item);
    }

    /**
     * Get fuel value for a material - LEGACY METHOD for backward compatibility
     */
    public static int getFuelValue(Material material) {
        return FUEL_VALUES.getOrDefault(material, 0);
    }

    /**
     * Check if a material is fuel - LEGACY METHOD for backward compatibility
     */
    public static boolean isFuel(Material material) {
        return FUEL_VALUES.containsKey(material);
    }

    public void setFuelItem(ItemStack fuelItem) {
        this.fuelItem = fuelItem != null ? fuelItem.clone() : null;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            if (fuelItem != null) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Set fuel item: " + fuelItem.getAmount() + "x " + fuelItem.getType());
            } else {
                Main.getInstance().debugLog(DebugSystem.GUI, "[FurnaceData] Cleared fuel item");
            }
        }
    }

    public void setOutputItem(ItemStack outputItem) {
        this.outputItem = outputItem != null ? outputItem.clone() : null;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            if (outputItem != null) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Set output item: " + outputItem.getAmount() + "x " + outputItem.getType());
            } else {
                Main.getInstance().debugLog(DebugSystem.GUI, "[FurnaceData] Cleared output item");
            }
        }
    }
    
    public ItemStack getFuelItem() {
        return fuelItem != null ? fuelItem.clone() : null;
    }
    
    public ItemStack getOutputItem() {
        return outputItem != null ? outputItem.clone() : null;
    }
        
    // Smelting state
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            this.smeltTime = 0;
        }
    }
    
    public int getSmeltTime() {
        return smeltTime;
    }
    
    public void setSmeltTime(int smeltTime) {
        this.smeltTime = smeltTime;
    }
    
    public int getMaxSmeltTime() {
        return maxSmeltTime;
    }
    
    public void setMaxSmeltTime(int maxSmeltTime) {
        this.maxSmeltTime = maxSmeltTime;
    }
    
    public double getSmeltingProgress() {
        if (maxSmeltTime <= 0) return 0.0;
        return Math.min(1.0, (double) smeltTime / maxSmeltTime);
    }
    
    public int getRemainingSmeltTime() {
        return Math.max(0, maxSmeltTime - smeltTime);
    }
    
    // Fuel state
    public boolean hasFuel() {
        return hasFuel;
    }
    
    public void setHasFuel(boolean hasFuel) {
        this.hasFuel = hasFuel;
    }
    
    public int getFuelTime() {
        return fuelTime;
    }
    
    public void setFuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
    }
    
    public int getMaxFuelTime() {
        return maxFuelTime;
    }
    
    public void setMaxFuelTime(int maxFuelTime) {
        this.maxFuelTime = maxFuelTime;
    }
    
    public double getFuelProgress() {
        if (maxFuelTime <= 0) return 0.0;
        return Math.min(1.0, (double) fuelTime / maxFuelTime);
    }
    
    public int getRemainingFuelTime() {
        return Math.max(0, fuelTime);
    }
    
    /**
     * Get the current smelting result - FIXED: Use CustomFurnaceManager
     */
    public ItemStack getSmeltingResult() {
        if (inputItem == null || inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0) {
            return null;
        }
        
        // CRITICAL FIX: Use CustomFurnaceManager instead of hardcoded recipes
        return CustomFurnaceManager.getInstance().getSmeltingResult(inputItem);
    }
    
    public void setSmeltingResult(ItemStack result) {
        this.smeltingResult = result != null ? result.clone() : null;
    }
    
    /**
     * Complete a smelting cycle - ENHANCED: Better input validation
     */
    private void completeSmeltingCycle() {
        try {
            // CRITICAL: Validate input still exists before completing cycle
            if (inputItem == null || inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0) {
                // Input disappeared - can't complete smelting
                setActive(false);
                smeltingResult = null;
                smeltTime = 0;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Cannot complete smelting - input disappeared");
                }
                return;
            }
            
            ItemStack result = getSmeltingResult();
            if (result == null) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Cannot complete smelting - no result available");
                }
                return;
            }
            
            // Double-check that we can still add to output
            if (!canAddToOutput(result)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Cannot complete smelting - output slot full");
                }
                return;
            }
            
            // Ensure the result has rarity before adding to output
            if (!ItemManager.hasRarity(result)) {
                result = ItemManager.applyRarity(result);
            }
            
            // Track input consumption for logging
            ItemStack originalInput = inputItem.clone();
            
            // ALWAYS consume input regardless of GUI state
            if (inputItem.getAmount() > 1) {
                inputItem.setAmount(inputItem.getAmount() - 1);
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Consumed 1 input item, " + inputItem.getAmount() + " remaining");
                }
            } else {
                inputItem = null;
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Consumed last input item");
                }
            }
            
            // Add result to output
            addToOutput(result);
            
            // Reset smelting time for next cycle
            smeltTime = 0;
            
            // Update smelting result for next cycle (in case input changed or was consumed)
            updateSmeltingResult();
            
            // Update input slot for all viewers if input was consumed
            updateGUIViewersForInputConsumption(originalInput, inputItem);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Completed smelting cycle, produced " + 
                    result.getAmount() + "x " + result.getType() + " with rarity");
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("[FurnaceData] Error completing smelting cycle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update GUI viewers when input is consumed - ENHANCED METHOD
     */
    private void updateGUIViewersForInputConsumption(ItemStack originalInput, ItemStack newInput) {
        try {
            // Find all players viewing this furnace's GUI and update them
            for (Map.Entry<Player, Location> entry : CustomFurnaceGUI.getActiveFurnaceGUIs().entrySet()) {
                Player viewer = entry.getKey();
                Location viewerLocation = entry.getValue();
                
                if (viewerLocation.equals(this.location)) {
                    // This player is viewing our furnace - update their input slot
                    Inventory gui = viewer.getOpenInventory().getTopInventory();
                    if (gui != null && CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                        gui.setItem(CustomFurnaceGUI.INPUT_SLOT, newInput != null ? newInput.clone() : null);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Updated input slot for viewer " + viewer.getName() + 
                                " - consumed from: " + getItemDebugName(originalInput) + 
                                " to: " + getItemDebugName(newInput));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[FurnaceData] Error updating GUI viewers for input consumption: " + e.getMessage());
        }
    }

    /**
     * Trigger GUI update when input is consumed - NEW METHOD
     */
    private void triggerGUIUpdateForInputConsumption(ItemStack originalInput, ItemStack newInput) {
        try {
            // Find all players viewing this furnace's GUI
            for (Map.Entry<Player, Location> entry : CustomFurnaceGUI.getActiveFurnaceGUIs().entrySet()) {
                Player viewer = entry.getKey();
                Location viewerLocation = entry.getValue();
                
                if (viewerLocation.equals(this.location)) {
                    // This player is viewing our furnace - update their GUI
                    Inventory gui = viewer.getOpenInventory().getTopInventory();
                    if (gui != null && CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                        // Update the input slot immediately
                        gui.setItem(CustomFurnaceGUI.INPUT_SLOT, newInput != null ? newInput.clone() : null);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Updated input slot for viewer " + viewer.getName() + 
                                " - consumed from: " + getItemDebugName(originalInput) + 
                                " to: " + getItemDebugName(newInput));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[FurnaceData] Error triggering GUI update: " + e.getMessage());
        }
    }

    /**
     * Helper method to compare items for equality - NEW METHOD
     */
    private boolean itemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;
        
        return item1.getType() == item2.getType() && 
            item1.getAmount() == item2.getAmount() &&
            java.util.Objects.equals(item1.getItemMeta(), item2.getItemMeta());
    }

    /**
     * Get debug name for an item - HELPER METHOD
     */
    private String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        
        StringBuilder sb = new StringBuilder();
        sb.append(item.getAmount()).append("x ");
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            sb.append(item.getItemMeta().getDisplayName());
        } else {
            sb.append(item.getType().name());
        }
        
        return sb.toString();
    }

    /**
     * Progress tick method - ENHANCED: Always burn fuel, only consume new fuel when needed
     */
    public void tick() {
        try {
            // CRITICAL: Check if we have valid input and can smelt BEFORE processing new fuel consumption
            boolean hasValidInput = (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0);
            boolean canSmeltInput = hasValidInput && CustomFurnaceManager.getInstance().canSmelt(inputItem);
            ItemStack expectedResult = canSmeltInput ? CustomFurnaceManager.getInstance().getSmeltingResult(inputItem) : null;
            boolean canAddOutput = (expectedResult != null) && canAddToOutput(expectedResult);
            
            // Can we productively use fuel for smelting?
            boolean canUseProductively = canSmeltInput && canAddOutput;
            
            // ENHANCED: Fuel ALWAYS burns down (realistic behavior)
            if (fuelTime > 0) {
                fuelTime--;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Burning fuel - " + fuelTime + " ticks remaining" + 
                        (canUseProductively ? " (productive)" : " (wasted)"));
                }
                
                if (fuelTime <= 0) {
                    // Fuel ran out
                    setHasFuel(false);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Fuel depleted. Can use productively: " + canUseProductively);
                    }
                    
                    // CRITICAL: Only consume new fuel if we can use it productively
                    if (canUseProductively && tryConsumeNewFuel()) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Successfully consumed new fuel for productive smelting, fuel time: " + fuelTime);
                        }
                    } else {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            String reason = !canUseProductively ? "cannot smelt productively" : "no fuel available";
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Not consuming new fuel - " + reason);
                        }
                    }
                }
            }
            
            // Smelting logic (only runs when conditions are met AND fuel is available)
            if (isActive && hasFuel && smeltingResult != null) {
                // ENHANCED: Validate that we still have input to smelt
                if (!hasValidInput) {
                    // Input was removed - stop smelting immediately
                    setActive(false);
                    smeltingResult = null;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Input removed mid-smelting - stopping process");
                    }
                    return;
                }
                
                if (!canSmeltInput || expectedResult == null) {
                    // Input changed to something that can't be smelted - stop process
                    setActive(false);
                    smeltingResult = null;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Input cannot be smelted - stopping process. CanSmelt: " + 
                            canSmeltInput + ", ExpectedResult: " + (expectedResult != null ? expectedResult.getType() : "null"));
                    }
                    return;
                }
                
                // FIXED: Check if the expected result is compatible with our current smelting target
                if (!expectedResult.isSimilar(smeltingResult)) {
                    // The result changed (different recipe) - restart with new result
                    setSmeltingResult(expectedResult);
                    smeltTime = 0; // Reset progress
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Recipe result changed - restarting with new result: " + expectedResult.getType());
                    }
                }
                
                // Check if output slot can accept the result
                if (canAddToOutput(smeltingResult)) {
                    smeltTime++;
                    
                    // Complete smelting cycle when time is reached
                    if (smeltTime >= maxSmeltTime) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Smelting cycle completed at " + smeltTime + "/" + maxSmeltTime + " ticks");
                        }
                        
                        completeSmeltingCycle();
                    }
                } else {
                    // Output slot is full - pause smelting but keep fuel burning
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Smelting paused - output slot full, but fuel continues burning");
                    }
                }
            } else if (isActive && !hasFuel) {
                // No fuel - stop smelting but preserve progress
                setActive(false);
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Smelting stopped - no fuel");
                }
            }
            
            // Try to restart smelting if we have input, fuel, and can output
            if (!isActive && hasValidInput && canSmeltInput && hasFuel && canAddOutput) {
                updateSmeltingResult();
                if (smeltingResult != null) {
                    setActive(true);
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[FurnaceData] Restarted smelting - all conditions met");
                    }
                }
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("[FurnaceData] Error in tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the smelting result based on current input - FIXED: Use CustomFurnaceManager
     */
    private void updateSmeltingResult() {
        if (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0) {
            // CRITICAL FIX: Use CustomFurnaceManager for consistent results
            ItemStack result = CustomFurnaceManager.getInstance().getSmeltingResult(inputItem);
            setSmeltingResult(result);
        } else {
            setSmeltingResult(null);
        }
    }

    /**
     * Check if an item can be added to the output slot - FIXED: Better capacity checking
     */
    private boolean canAddToOutput(ItemStack result) {
        if (result == null) return false;
        
        if (outputItem == null || outputItem.getType() == Material.AIR || outputItem.getAmount() <= 0) {
            return true; // Empty output slot can accept anything
        }
        
        // Check if items are similar and can stack
        if (outputItem.isSimilar(result)) {
            int totalAmount = outputItem.getAmount() + result.getAmount();
            int maxStackSize = Math.min(outputItem.getMaxStackSize(), result.getMaxStackSize());
            
            boolean canAdd = totalAmount <= maxStackSize;
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Can add to output check: current=" + outputItem.getAmount() + 
                    ", adding=" + result.getAmount() + ", total=" + totalAmount + 
                    ", maxStack=" + maxStackSize + ", canAdd=" + canAdd);
            }
            
            return canAdd;
        }
        
        // Different items can't be added to existing output
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Can't add to output - different items: existing=" + 
                outputItem.getType() + ", new=" + result.getType());
        }
        
        return false;
    }

    /**
     * Add an item to the output slot - ENHANCED: Prevent overwriting player items
     */
    private void addToOutput(ItemStack result) {
        if (result == null) return;
        
        // CRITICAL: Safety check - don't overwrite items that shouldn't be there
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            // Check if the existing output is a valid smelting result
            ItemStack expectedResult = getSmeltingResult();
            if (expectedResult == null || !outputItem.isSimilar(expectedResult)) {
                // The output contains an item that's not a smelting result (player placed item)
                // Log this as a potential issue but don't overwrite it
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] WARNING: Output contains unexpected item: " + getItemDebugName(outputItem) + 
                        ", expected: " + getItemDebugName(expectedResult) + ". Not overwriting.");
                }
                return;
            }
        }
        
        // Get current state before modifying
        ItemStack previousOutput = outputItem != null ? outputItem.clone() : null;
        
        if (outputItem == null) {
            outputItem = result.clone();
        } else if (outputItem.isSimilar(result)) {
            int newAmount = outputItem.getAmount() + result.getAmount();
            int maxStack = outputItem.getMaxStackSize();
            
            if (newAmount <= maxStack) {
                outputItem.setAmount(newAmount);
            } else {
                // If it would exceed max stack size, only add what we can
                outputItem.setAmount(maxStack);
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            String rarityInfo = ItemManager.hasRarity(outputItem) ? " (with rarity)" : " (no rarity)";
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Added to output: " + getItemDebugName(outputItem) + rarityInfo + 
                " (was: " + getItemDebugName(previousOutput) + ")");
        }
        
        // CRITICAL: Only update GUI viewers if this is actually a new item production
        if (!itemsEqual(previousOutput, outputItem)) {
            updateGUIViewersForOutputCreation();
        }
    }

    /**
     * Get cooking status for animation purposes - FIXED: Show burning when fuel is active
     */
    public boolean shouldShowCookingAnimation() {
        // Show cooking animation when fuel is burning (regardless of smelting status)
        return hasFuel && fuelTime > 0;
    }

    /**
     * Check if fuel is paused due to full output - NEW METHOD
     */
    public boolean isFuelPaused() {
        return hasFuel && fuelTime > 0 && isActive && inputItem != null && 
            smeltingResult != null && !canAddToOutput(smeltingResult);
    }

    /**
     * Check if cooking is paused due to full output
     */
    public boolean isCookingPaused() {
        return isActive && hasFuel && inputItem != null && smeltingResult != null && !canAddToOutput(smeltingResult);
    }

    /**
     * Update GUI viewers when output is created - NEW METHOD
     */
    private void updateGUIViewersForOutputCreation() {
        try {
            // Find all players viewing this furnace's GUI and update them immediately
            for (Map.Entry<Player, Location> entry : CustomFurnaceGUI.getActiveFurnaceGUIs().entrySet()) {
                Player viewer = entry.getKey();
                Location viewerLocation = entry.getValue();
                
                if (viewerLocation.equals(this.location)) {
                    // This player is viewing our furnace - update their output slot immediately
                    Inventory gui = viewer.getOpenInventory().getTopInventory();
                    if (gui != null && CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                        gui.setItem(CustomFurnaceGUI.OUTPUT_SLOT, outputItem != null ? outputItem.clone() : null);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Updated output slot for viewer " + viewer.getName() + 
                                " - created: " + getItemDebugName(outputItem));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[FurnaceData] Error updating GUI viewers for output: " + e.getMessage());
        }
    }

    /**
     * Check if fuel is currently burning wastefully (not being used productively) - NEW METHOD
     */
    public boolean isFuelBeingWasted() {
        if (!hasFuel || fuelTime <= 0) {
            return false; // No fuel burning
        }
        
        // Check if we have valid input and can smelt
        boolean hasValidInput = (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0);
        boolean canSmeltInput = hasValidInput && CustomFurnaceManager.getInstance().canSmelt(inputItem);
        ItemStack expectedResult = canSmeltInput ? CustomFurnaceManager.getInstance().getSmeltingResult(inputItem) : null;
        boolean canAddOutput = (expectedResult != null) && canAddToOutput(expectedResult);
        
        // Fuel is wasted if we can't use it productively
        return !(canSmeltInput && canAddOutput);
    }

    /**
     * Get a status message for fuel usage - NEW METHOD
     */
    public String getFuelStatusMessage() {
        if (!hasFuel || fuelTime <= 0) {
            return "No fuel";
        }
        
        if (isFuelBeingWasted()) {
            if (inputItem == null || inputItem.getType() == Material.AIR) {
                return "Fuel burning wastefully - No input";
            } else if (!CustomFurnaceManager.getInstance().canSmelt(inputItem)) {
                return "Fuel burning wastefully - Cannot smelt input";
            } else {
                return "Fuel burning wastefully - Output full";
            }
        }
        
        return "Fuel burning productively";
    }

    /**
     * Try to consume new fuel when current fuel runs out - ENHANCED: Only consume when productive
     */
    private boolean tryConsumeNewFuel() {
        // CRITICAL: Only consume fuel if we actually have something to smelt
        if (inputItem == null || inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Not consuming new fuel - no input to smelt");
            }
            return false;
        }
        
        // Check if the input can actually be smelted
        if (!CustomFurnaceManager.getInstance().canSmelt(inputItem)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Not consuming new fuel - input cannot be smelted: " + getItemDebugName(inputItem));
            }
            return false;
        }
        
        // Check if we can output the result
        ItemStack expectedResult = CustomFurnaceManager.getInstance().getSmeltingResult(inputItem);
        if (expectedResult == null || !canAddToOutput(expectedResult)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Not consuming new fuel - cannot output result (output full or no recipe)");
            }
            return false;
        }
        
        if (fuelItem == null || fuelItem.getType() == Material.AIR || fuelItem.getAmount() <= 0) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] No new fuel available to consume");
            }
            return false;
        }
        
        // Check if the fuel item is valid
        if (!FurnaceData.isFuel(fuelItem)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Fuel item is not valid fuel: " + getItemDebugName(fuelItem));
            }
            return false;
        }
        
        // Get fuel value for this item
        int fuelValue = FurnaceData.getFuelValue(fuelItem);
        if (fuelValue <= 0) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Fuel item has no fuel value: " + getItemDebugName(fuelItem));
            }
            return false;
        }
        
        // Track original fuel for logging
        ItemStack originalFuel = fuelItem.clone();
        
        // Consume one fuel item
        if (fuelItem.getAmount() > 1) {
            fuelItem.setAmount(fuelItem.getAmount() - 1);
        } else {
            fuelItem = null;
        }
        
        // Set new fuel time
        setFuelTime(fuelValue);
        setMaxFuelTime(fuelValue);
        setHasFuel(true);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Auto-consumed fuel productively: " + originalFuel.getType() + 
                ", new fuel time: " + fuelValue + " ticks, remaining fuel: " + 
                (fuelItem != null ? fuelItem.getAmount() + "x " + fuelItem.getType() : "none"));
        }
        
        // CRITICAL: Update all GUIs to show fuel consumption
        updateGUIViewersForFuelConsumption(originalFuel, fuelItem);
        
        return true;
    }

    /**
     * Update GUI viewers when fuel is consumed - NEW METHOD
     */
    private void updateGUIViewersForFuelConsumption(ItemStack originalFuel, ItemStack newFuel) {
        try {
            // Get all players who have this furnace open
            Map<Player, Location> activeGUIs = CustomFurnaceGUI.getActiveFurnaceGUIs();
            
            for (Map.Entry<Player, Location> entry : activeGUIs.entrySet()) {
                Player player = entry.getKey();
                Location guiLocation = entry.getValue();
                
                // Check if this player has the same furnace open
                if (guiLocation.equals(this.location)) {
                    // Update this player's GUI immediately
                    Inventory gui = player.getOpenInventory().getTopInventory();
                    if (CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                        // Update fuel slot
                        gui.setItem(CustomFurnaceGUI.FUEL_SLOT, newFuel != null ? newFuel.clone() : null);
                        
                        // Update fuel timer
                        CustomFurnaceGUI.updateFuelTimer(gui, this);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[FurnaceData] Updated fuel GUI for " + player.getName() + 
                                " - was: " + getItemDebugName(originalFuel) + 
                                ", now: " + getItemDebugName(newFuel));
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[FurnaceData] Error updating fuel GUI: " + e.getMessage());
        }
    }
    
    // Fuel value mapping
    private static final Map<Material, Integer> FUEL_VALUES = new HashMap<>();
    
    static {
        // Initialize fuel values (in ticks, 20 ticks = 1 second)
        FUEL_VALUES.put(Material.COAL, 1600);
        FUEL_VALUES.put(Material.CHARCOAL, 1600);
        FUEL_VALUES.put(Material.COAL_BLOCK, 16000);
        FUEL_VALUES.put(Material.BLAZE_ROD, 2400);
        FUEL_VALUES.put(Material.LAVA_BUCKET, 20000);
        
        // Wood items
        FUEL_VALUES.put(Material.OAK_LOG, 300);
        FUEL_VALUES.put(Material.BIRCH_LOG, 300);
        FUEL_VALUES.put(Material.SPRUCE_LOG, 300);
        FUEL_VALUES.put(Material.JUNGLE_LOG, 300);
        FUEL_VALUES.put(Material.ACACIA_LOG, 300);
        FUEL_VALUES.put(Material.DARK_OAK_LOG, 300);
        FUEL_VALUES.put(Material.CRIMSON_STEM, 300);
        FUEL_VALUES.put(Material.WARPED_STEM, 300);
        
        FUEL_VALUES.put(Material.OAK_PLANKS, 300);
        FUEL_VALUES.put(Material.BIRCH_PLANKS, 300);
        FUEL_VALUES.put(Material.SPRUCE_PLANKS, 300);
        FUEL_VALUES.put(Material.JUNGLE_PLANKS, 300);
        FUEL_VALUES.put(Material.ACACIA_PLANKS, 300);
        FUEL_VALUES.put(Material.DARK_OAK_PLANKS, 300);
        FUEL_VALUES.put(Material.CRIMSON_PLANKS, 300);
        FUEL_VALUES.put(Material.WARPED_PLANKS, 300);
        
        FUEL_VALUES.put(Material.STICK, 100);
        FUEL_VALUES.put(Material.BAMBOO, 50);
    }
}