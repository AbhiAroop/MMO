package com.server.crafting.data;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;

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
    
    public void setInputItem(ItemStack inputItem) {
        this.inputItem = inputItem != null ? inputItem.clone() : null;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            if (inputItem != null) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Set input item: " + inputItem.getAmount() + "x " + inputItem.getType());
            } else {
                Main.getInstance().debugLog(DebugSystem.GUI, "[FurnaceData] Cleared input item");
            }
        }
        
        // Reset smelting if input changes
        if (inputItem == null) {
            setActive(false);
            setSmeltingResult(null);
        }
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
    
    // Result handling
    public ItemStack getSmeltingResult() {
        return smeltingResult != null ? smeltingResult.clone() : null;
    }
    
    public void setSmeltingResult(ItemStack result) {
        this.smeltingResult = result != null ? result.clone() : null;
    }
    
    /**
     * Complete a smelting cycle - ENHANCED: Better item consumption tracking
     */
    private void completeSmeltingCycle() {
        try {
            ItemStack result = getSmeltingResult();
            if (result == null) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Cannot complete smelting - no result available");
                }
                return;
            }
            
            // ENHANCED: Consume input with detailed logging
            if (inputItem != null && inputItem.getAmount() > 0) {
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
            }
            
            // Add result to output
            addToOutput(result);
            
            // Reset smelting time for next cycle
            smeltTime = 0;
            
            // Update smelting result for next cycle (in case input changed)
            updateSmeltingResult();
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Completed smelting cycle, produced " + 
                    result.getAmount() + "x " + result.getType());
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("[FurnaceData] Error completing smelting cycle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Progress tick method - ENHANCED: Better fuel consumption tracking
     */
    public void tick() {
        // Only process if we have input to smelt
        if (inputItem == null || inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0) {
            if (isActive) {
                setActive(false);
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Stopped smelting - no input item");
                }
            }
            return;
        }
        
        // Check if we have fuel to continue
        if (fuelTime <= 0) {
            if (isActive) {
                setActive(false);
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[FurnaceData] Stopped smelting - no fuel");
                }
            }
            return;
        }
        
        // Check if output slot can accept the result
        ItemStack result = getSmeltingResult();
        if (result == null || !canAddToOutput(result)) {
            // Pause smelting but don't consume fuel
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Smelting paused - output full or no result");
            }
            return;
        }
        
        // We can proceed with smelting
        if (!isActive) {
            setActive(true);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[FurnaceData] Started smelting");
            }
        }
        
        // ENHANCED: Consume fuel with detailed logging
        int previousFuelTime = fuelTime;
        fuelTime--;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && fuelTime % 100 == 0) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Fuel burning: " + fuelTime + " ticks remaining");
        }
        
        // Progress smelting
        smeltTime++;
        
        // Check if smelting is complete
        if (smeltTime >= maxSmeltTime) {
            completeSmeltingCycle();
        }
    }

    /**
     * Update the smelting result based on current input - NEW METHOD
     */
    private void updateSmeltingResult() {
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            // Get smelting recipes from CustomFurnaceManager
            Map<Material, ItemStack> smeltingRecipes = getSmeltingRecipes();
            ItemStack result = smeltingRecipes.get(inputItem.getType());
            setSmeltingResult(result);
        } else {
            setSmeltingResult(null);
        }
    }

    /**
     * Get smelting recipes - HELPER METHOD
     */
    private Map<Material, ItemStack> getSmeltingRecipes() {
        // This should ideally be injected or accessed through a proper reference
        // For now, we'll create basic recipes inline
        Map<Material, ItemStack> recipes = new HashMap<>();
        recipes.put(Material.RAW_IRON, new ItemStack(Material.IRON_INGOT));
        recipes.put(Material.RAW_GOLD, new ItemStack(Material.GOLD_INGOT));
        recipes.put(Material.RAW_COPPER, new ItemStack(Material.COPPER_INGOT));
        recipes.put(Material.COBBLESTONE, new ItemStack(Material.STONE));
        recipes.put(Material.SAND, new ItemStack(Material.GLASS));
        recipes.put(Material.CLAY_BALL, new ItemStack(Material.BRICK));
        recipes.put(Material.BEEF, new ItemStack(Material.COOKED_BEEF));
        recipes.put(Material.PORKCHOP, new ItemStack(Material.COOKED_PORKCHOP));
        recipes.put(Material.MUTTON, new ItemStack(Material.COOKED_MUTTON));
        recipes.put(Material.CHICKEN, new ItemStack(Material.COOKED_CHICKEN));
        recipes.put(Material.COD, new ItemStack(Material.COOKED_COD));
        recipes.put(Material.SALMON, new ItemStack(Material.COOKED_SALMON));
        recipes.put(Material.POTATO, new ItemStack(Material.BAKED_POTATO));
        recipes.put(Material.KELP, new ItemStack(Material.DRIED_KELP));
        return recipes;
    }

    /**
     * Check if an item can be added to the output slot - IMPROVED: Better capacity checking
     */
    private boolean canAddToOutput(ItemStack result) {
        if (outputItem == null) return true;
        
        if (outputItem.isSimilar(result)) {
            int totalAmount = outputItem.getAmount() + result.getAmount();
            int maxStackSize = Math.min(outputItem.getMaxStackSize(), 64); // Ensure we don't exceed GUI slot limit
            return totalAmount <= maxStackSize;
        }
        
        return false;
    }

    /**
     * Get cooking status for animation purposes - FIXED: Only animate when fuel is actively burning
     */
    public boolean shouldShowCookingAnimation() {
        // Show animation only if we have fuel that's actively burning
        // This means we have fuel time remaining AND we're not paused due to full output
        if (!hasFuel || fuelTime <= 0) {
            return false;
        }
        
        // If we're trying to smelt but output is full, don't show animation
        if (isActive && inputItem != null && smeltingResult != null && !canAddToOutput(smeltingResult)) {
            return false;
        }
        
        // Show animation if we have active fuel (even if not actively smelting yet)
        return true;
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
     * Add an item to the output slot - IMPROVED with debugging
     */
    private void addToOutput(ItemStack result) {
        if (outputItem == null) {
            outputItem = result.clone();
        } else if (outputItem.isSimilar(result)) {
            outputItem.setAmount(outputItem.getAmount() + result.getAmount());
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[FurnaceData] Added to output: " + outputItem.getAmount() + "x " + outputItem.getType());
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
    
    public static int getFuelValue(Material material) {
        return FUEL_VALUES.getOrDefault(material, 0);
    }
    
    public static boolean isFuel(Material material) {
        return FUEL_VALUES.containsKey(material);
    }
}