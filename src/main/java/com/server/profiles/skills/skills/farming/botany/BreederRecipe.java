package com.server.profiles.skills.skills.farming.botany;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents a breeding recipe for the crop breeder
 */
public class BreederRecipe {
    
    private final ItemStack seed1;
    private final ItemStack seed2;
    private final ItemStack catalyst;
    private final Material fluidType;
    private final ItemStack output;
    private final int duration; // in seconds
    
    public BreederRecipe(ItemStack seed1, ItemStack seed2, ItemStack catalyst, 
                         Material fluidType, ItemStack output, int duration) {
        this.seed1 = seed1.clone();
        this.seed2 = seed2.clone();
        this.catalyst = catalyst.clone();
        this.fluidType = fluidType;
        this.output = output.clone();
        this.duration = duration;
    }
    
    /**
     * Check if the provided items match this recipe
     */
    public boolean matches(ItemStack testSeed1, ItemStack testSeed2, 
                          ItemStack testCatalyst, ItemStack testFluid) {
        if (!matchesItem(seed1, testSeed1)) return false;
        if (!matchesItem(seed2, testSeed2)) return false;
        if (!matchesItem(catalyst, testCatalyst)) return false;
        if (testFluid == null || testFluid.getType() != fluidType) return false;
        
        return true;
    }
    
    /**
     * Check if two items match (considering custom model data)
     */
    public static boolean matchesItem(ItemStack recipe, ItemStack test) {
        if (recipe == null || test == null) return false;
        if (recipe.getType() != test.getType()) return false;
        
        // Check custom model data if present
        ItemMeta recipeMeta = recipe.getItemMeta();
        ItemMeta testMeta = test.getItemMeta();
        
        if (recipeMeta != null && testMeta != null) {
            if (recipeMeta.hasCustomModelData() && testMeta.hasCustomModelData()) {
                return recipeMeta.getCustomModelData() == testMeta.getCustomModelData();
            }
        }
        
        // If no custom model data, just check material
        return true;
    }
    
    // Getters
    public ItemStack getSeed1() {
        return seed1.clone();
    }
    
    public ItemStack getSeed2() {
        return seed2.clone();
    }
    
    public ItemStack getCatalyst() {
        return catalyst.clone();
    }
    
    public Material getFluidType() {
        return fluidType;
    }
    
    public ItemStack getOutput() {
        return output.clone();
    }
    
    public int getDuration() {
        return duration;
    }
}
