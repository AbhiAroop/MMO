package com.server.crafting.furnace;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.server.crafting.temperature.TemperatureSystem;

/**
 * Data structure for individual furnace instances
 * Step 1: Furnace state management
 */
public class FurnaceData {
    
    private final Location location;
    private final FurnaceType furnaceType;
    
    // Temperature system
    private int currentTemperature;
    private int targetTemperature;
    private boolean isHeating;
    private boolean isCooling;
    
    // Fuel system
    private int fuelTime;           // Remaining fuel time in ticks
    private int maxFuelTime;        // Maximum fuel time for current fuel
    private boolean hasFuel;
    
    // Cooking system
    private int cookTime;           // Current cooking progress in ticks
    private int maxCookTime;        // Required cooking time for current recipe
    private boolean isActive;       // Whether furnace is currently processing
    private boolean isPaused;       // Whether cooking is paused due to temperature
    
    // Inventory slots (dynamic based on furnace type)
    private ItemStack[] inputSlots;
    private ItemStack[] fuelSlots;
    private ItemStack[] outputSlots;
    
    // Safety system
    private int overheatingTime;    // How long furnace has been overheating
    private int explosionCountdown; // Countdown to explosion
    private boolean isEmergencyShutdown;
    
    public FurnaceData(Location location, FurnaceType furnaceType) {
        this.location = location.clone();
        this.furnaceType = furnaceType;
        
        // Initialize temperature
        this.currentTemperature = TemperatureSystem.ROOM_TEMPERATURE;
        this.targetTemperature = TemperatureSystem.ROOM_TEMPERATURE;
        this.isHeating = false;
        this.isCooling = false;
        
        // Initialize fuel system
        this.fuelTime = 0;
        this.maxFuelTime = 0;
        this.hasFuel = false;
        
        // Initialize cooking system
        this.cookTime = 0;
        this.maxCookTime = 0;
        this.isActive = false;
        this.isPaused = false;
        
        // Initialize slots based on furnace type
        this.inputSlots = new ItemStack[furnaceType.getInputSlots()];
        this.fuelSlots = new ItemStack[furnaceType.getFuelSlots()];
        this.outputSlots = new ItemStack[furnaceType.getOutputSlots()];
        
        // Initialize safety system
        this.overheatingTime = 0;
        this.explosionCountdown = 0;
        this.isEmergencyShutdown = false;
    }
    
    // Basic getters
    public Location getLocation() { return location.clone(); }
    public FurnaceType getFurnaceType() { return furnaceType; }
    
    // Temperature system
    public int getCurrentTemperature() { return currentTemperature; }
    public int getTargetTemperature() { return targetTemperature; }
    public boolean isHeating() { return isHeating; }
    public boolean isCooling() { return isCooling; }
    
    public void setCurrentTemperature(int temperature) { this.currentTemperature = temperature; }
    public void setTargetTemperature(int temperature) { this.targetTemperature = temperature; }
    public void setHeating(boolean heating) { this.isHeating = heating; }
    public void setCooling(boolean cooling) { this.isCooling = cooling; }
    
    // Fuel system
    public int getFuelTime() { return fuelTime; }
    public int getMaxFuelTime() { return maxFuelTime; }
    public boolean hasFuel() { return hasFuel; }
    
    public void setFuelTime(int fuelTime) { this.fuelTime = fuelTime; }
    public void setMaxFuelTime(int maxFuelTime) { this.maxFuelTime = maxFuelTime; }
    public void setHasFuel(boolean hasFuel) { this.hasFuel = hasFuel; }
    
    // Cooking system
    public int getCookTime() { return cookTime; }
    public int getMaxCookTime() { return maxCookTime; }
    public boolean isActive() { return isActive; }
    public boolean isPaused() { return isPaused; }
    
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }
    public void setMaxCookTime(int maxCookTime) { this.maxCookTime = maxCookTime; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    
    // Inventory management
    public ItemStack[] getInputSlots() { return Arrays.copyOf(inputSlots, inputSlots.length); }
    public ItemStack[] getFuelSlots() { return Arrays.copyOf(fuelSlots, fuelSlots.length); }
    public ItemStack[] getOutputSlots() { return Arrays.copyOf(outputSlots, outputSlots.length); }
    
    public ItemStack getInputSlot(int index) {
        return index >= 0 && index < inputSlots.length ? inputSlots[index] : null;
    }
    
    public ItemStack getFuelSlot(int index) {
        return index >= 0 && index < fuelSlots.length ? fuelSlots[index] : null;
    }
    
    public ItemStack getOutputSlot(int index) {
        return index >= 0 && index < outputSlots.length ? outputSlots[index] : null;
    }
    
    public void setInputSlot(int index, ItemStack item) {
        if (index >= 0 && index < inputSlots.length) {
            inputSlots[index] = item;
        }
    }
    
    public void setFuelSlot(int index, ItemStack item) {
        if (index >= 0 && index < fuelSlots.length) {
            fuelSlots[index] = item;
        }
    }
    
    public void setOutputSlot(int index, ItemStack item) {
        if (index >= 0 && index < outputSlots.length) {
            outputSlots[index] = item;
        }
    }
    
    // Safety system
    public int getOverheatingTime() { return overheatingTime; }
    public int getExplosionCountdown() { return explosionCountdown; }
    public boolean isEmergencyShutdown() { return isEmergencyShutdown; }
    
    public void setOverheatingTime(int time) { this.overheatingTime = time; }
    public void setExplosionCountdown(int countdown) { this.explosionCountdown = countdown; }
    public void setEmergencyShutdown(boolean shutdown) { this.isEmergencyShutdown = shutdown; }
    
    // Utility methods
    public boolean isOverheating() {
        return currentTemperature > furnaceType.getMaxTemperature();
    }
    
    public boolean isWithinOperatingRange() {
        return currentTemperature >= furnaceType.getMinTemperature() && 
               currentTemperature <= furnaceType.getMaxTemperature();
    }
    
    public boolean willExplode() {
        return currentTemperature >= furnaceType.getExplosionTemperature();
    }
    
    public double getTemperatureEfficiency(int requiredTemp) {
        return TemperatureSystem.getTemperatureEfficiency(currentTemperature, requiredTemp);
    }
    
    public String getFormattedTemperature() {
        return TemperatureSystem.formatTemperature(currentTemperature);
    }
    
    public double getFuelProgress() {
        return maxFuelTime > 0 ? (double) fuelTime / maxFuelTime : 0.0;
    }
    
    public double getCookProgress() {
        return maxCookTime > 0 ? (double) cookTime / maxCookTime : 0.0;
    }
    
    /**
     * Check if furnace has any input items
     */
    public boolean hasInput() {
        for (ItemStack item : inputSlots) {
            if (item != null && item.getAmount() > 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if furnace has space in output slots
     */
    public boolean hasOutputSpace() {
        for (ItemStack item : outputSlots) {
            if (item == null || item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Reset furnace to safe state
     */
    public void emergencyShutdown() {
        setEmergencyShutdown(true);
        setActive(false);
        setHasFuel(false);
        setTargetTemperature(TemperatureSystem.ROOM_TEMPERATURE);
        setCooling(true);
        setOverheatingTime(0);
        setExplosionCountdown(0);
    }

    /**
     * Check if furnace has space in output slots for specific item
     * Step 4: Enhanced output checking
     */
    public boolean hasOutputSpaceFor(org.bukkit.inventory.ItemStack item) {
        if (item == null) return true;
        
        int requiredSpace = item.getAmount();
        
        // Check existing stacks that can be added to
        for (int i = 0; i < outputSlots.length; i++) {
            org.bukkit.inventory.ItemStack outputItem = outputSlots[i];
            
            if (outputItem != null && outputItem.isSimilar(item)) {
                int maxStack = outputItem.getMaxStackSize();
                int currentAmount = outputItem.getAmount();
                int spaceAvailable = maxStack - currentAmount;
                requiredSpace -= spaceAvailable;
                
                if (requiredSpace <= 0) {
                    return true;
                }
            }
        }
        
        // Check empty slots
        for (int i = 0; i < outputSlots.length; i++) {
            if (outputSlots[i] == null || outputSlots[i].getType() == org.bukkit.Material.AIR) {
                requiredSpace -= item.getMaxStackSize();
                
                if (requiredSpace <= 0) {
                    return true;
                }
            }
        }
        
        return requiredSpace <= 0;
    }

    /**
     * Check if furnace can process a specific recipe
     * Step 4: Recipe validation
     */
    public boolean canProcessRecipe(com.server.crafting.recipes.FurnaceRecipe recipe) {
        if (recipe == null) return false;
        
        // Check temperature capability
        if (furnaceType.getMaxTemperature() < recipe.getRequiredTemperature()) {
            return false;
        }
        
        // Check if we have space for all outputs
        for (org.bukkit.inventory.ItemStack output : recipe.getOutputs()) {
            if (!hasOutputSpaceFor(output)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get current recipe being processed (if any)
     * Step 4: Recipe tracking
     */
    public com.server.crafting.recipes.FurnaceRecipe getCurrentRecipe() {
        if (!isActive) return null;
        
        // Get current inputs and find matching recipe
        java.util.List<org.bukkit.inventory.ItemStack> currentInputs = new java.util.ArrayList<>();
        for (int i = 0; i < inputSlots.length; i++) {
            if (inputSlots[i] != null && inputSlots[i].getType() != org.bukkit.Material.AIR) {
                currentInputs.add(inputSlots[i]);
            }
        }
        
        return com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().findRecipe(currentInputs);
    }

    /**
     * Get estimated time remaining for current recipe
     * Step 4: Time estimation
     */
    public int getEstimatedTimeRemaining() {
        if (!isActive || maxCookTime <= 0) return 0;
        
        int remainingTicks = maxCookTime - cookTime;
        
        // Apply temperature efficiency
        com.server.crafting.recipes.FurnaceRecipe currentRecipe = getCurrentRecipe();
        if (currentRecipe != null) {
            double efficiency = com.server.crafting.temperature.TemperatureSystem
                .getTemperatureEfficiency(currentTemperature, currentRecipe.getRequiredTemperature());
            
            if (efficiency > 0) {
                remainingTicks = (int) (remainingTicks / efficiency);
            }
        }
        
        return remainingTicks;
    }
}