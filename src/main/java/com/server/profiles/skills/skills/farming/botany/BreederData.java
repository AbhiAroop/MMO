package com.server.profiles.skills.skills.farming.botany;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Stores persistent data for a breeder block
 */
public class BreederData {
    
    // Slot constants
    public static final int SEED_SLOT_1 = 10;
    public static final int SEED_SLOT_2 = 12;
    public static final int CATALYST_SLOT = 14;
    public static final int FLUID_SLOT = 16;
    public static final int OUTPUT_SLOT = 24;
    public static final int START_BUTTON_SLOT = 22;
    public static final int TIMER_DISPLAY_SLOT = 13; // NEW: Shows breeding progress
    
    private final UUID armorStandId;
    private final Location location;
    private final Map<Integer, ItemStack> inventory;
    
    private boolean isBreeding;
    private long breedingStartTime;
    private int breedingDuration; // in seconds
    private BreederRecipe activeRecipe;
    
    public BreederData(UUID armorStandId, Location location) {
        this.armorStandId = armorStandId;
        this.location = location.clone();
        this.inventory = new HashMap<>();
        this.isBreeding = false;
        this.breedingStartTime = 0;
        this.breedingDuration = 0;
    }
    
    /**
     * Clean up invalid slots from inventory
     * Called after construction or loading from persistence
     */
    public void cleanInventory() {
        // Remove any items in invalid slots
        inventory.keySet().removeIf(slot -> 
            slot != SEED_SLOT_1 && slot != SEED_SLOT_2 && 
            slot != CATALYST_SLOT && slot != FLUID_SLOT && slot != OUTPUT_SLOT
        );
    }
    
    /**
     * Set an item in a specific slot
     */
    public void setItem(int slot, ItemStack item) {
        // Only allow valid slots (input/output slots only)
        if (slot != SEED_SLOT_1 && slot != SEED_SLOT_2 && 
            slot != CATALYST_SLOT && slot != FLUID_SLOT && slot != OUTPUT_SLOT) {
            return; // Ignore invalid slots
        }
        
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            inventory.remove(slot);
        } else {
            inventory.put(slot, item.clone());
        }
    }
    
    /**
     * Get an item from a specific slot
     */
    public ItemStack getItem(int slot) {
        ItemStack item = inventory.get(slot);
        return item != null ? item.clone() : null;
    }
    
    /**
     * Check if a slot has an item
     */
    public boolean hasItem(int slot) {
        ItemStack item = inventory.get(slot);
        return item != null && !item.getType().isAir();
    }
    
    /**
     * Start the breeding process
     */
    public boolean startBreeding(BreederRecipe recipe) {
        if (isBreeding) {
            return false;
        }
        
        this.activeRecipe = recipe;
        this.isBreeding = true;
        this.breedingStartTime = System.currentTimeMillis();
        this.breedingDuration = recipe.getDuration();
        
        // Consume catalyst immediately
        ItemStack catalyst = getItem(CATALYST_SLOT);
        if (catalyst != null) {
            catalyst.setAmount(catalyst.getAmount() - 1);
            setItem(CATALYST_SLOT, catalyst);
        }
        
        return true;
    }
    
    /**
     * Cancel the breeding process
     */
    public void cancelBreeding() {
        this.isBreeding = false;
        this.activeRecipe = null;
        this.breedingStartTime = 0;
        this.breedingDuration = 0;
    }
    
    /**
     * Check if breeding is complete
     */
    public boolean isBreedingComplete() {
        if (!isBreeding) {
            return false;
        }
        
        long elapsed = (System.currentTimeMillis() - breedingStartTime) / 1000;
        return elapsed >= breedingDuration;
    }
    
    /**
     * Get remaining breeding time in seconds
     */
    public int getRemainingTime() {
        if (!isBreeding) {
            return 0;
        }
        
        long elapsed = (System.currentTimeMillis() - breedingStartTime) / 1000;
        return Math.max(0, breedingDuration - (int) elapsed);
    }
    
    /**
     * Complete the breeding process
     */
    public void completeBreeding() {
        if (!isBreeding || activeRecipe == null) {
            return;
        }
        
        // Consume input seeds
        ItemStack seed1 = getItem(SEED_SLOT_1);
        if (seed1 != null) {
            if (seed1.getAmount() > 1) {
                seed1.setAmount(seed1.getAmount() - 1);
                setItem(SEED_SLOT_1, seed1);
            } else {
                setItem(SEED_SLOT_1, null); // Remove completely if only 1 left
            }
        }
        
        ItemStack seed2 = getItem(SEED_SLOT_2);
        if (seed2 != null) {
            if (seed2.getAmount() > 1) {
                seed2.setAmount(seed2.getAmount() - 1);
                setItem(SEED_SLOT_2, seed2);
            } else {
                setItem(SEED_SLOT_2, null); // Remove completely if only 1 left
            }
        }
        
        // Consume fluid
        ItemStack fluid = getItem(FLUID_SLOT);
        if (fluid != null) {
            // If it's a bucket, replace with empty bucket
            if (fluid.getType().toString().contains("BUCKET") && !fluid.getType().toString().equals("BUCKET")) {
                setItem(FLUID_SLOT, new ItemStack(org.bukkit.Material.BUCKET));
            } else {
                if (fluid.getAmount() > 1) {
                    fluid.setAmount(fluid.getAmount() - 1);
                    setItem(FLUID_SLOT, fluid);
                } else {
                    setItem(FLUID_SLOT, null); // Remove completely if only 1 left
                }
            }
        }
        
        // Create output
        ItemStack output = activeRecipe.getOutput().clone();
        ItemStack existing = getItem(OUTPUT_SLOT);
        if (existing != null && existing.isSimilar(output)) {
            output.setAmount(existing.getAmount() + output.getAmount());
        }
        setItem(OUTPUT_SLOT, output);
        
        // Reset breeding state
        this.isBreeding = false;
        this.breedingStartTime = 0;
        this.activeRecipe = null;
    }
    
    /**
     * Clear all contents
     */
    public void clear() {
        inventory.clear();
        cancelBreeding();
    }
    
    // Getters
    public UUID getArmorStandId() {
        return armorStandId;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public boolean isBreeding() {
        return isBreeding;
    }
    
    public int getBreedingDuration() {
        return breedingDuration;
    }
    
    public BreederRecipe getActiveRecipe() {
        return activeRecipe;
    }
    
    public Map<Integer, ItemStack> getInventory() {
        return new HashMap<>(inventory);
    }
}
