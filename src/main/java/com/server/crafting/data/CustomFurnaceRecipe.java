package com.server.crafting.data;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a custom furnace recipe with exact item matching and custom timing
 */
public class CustomFurnaceRecipe {
    
    private final ItemStack input;
    private final ItemStack result;
    private final int cookTime; // In ticks (20 ticks = 1 second)
    private final String recipeId;
    
    public CustomFurnaceRecipe(String recipeId, ItemStack input, ItemStack result, int cookTime) {
        this.recipeId = recipeId;
        this.input = input.clone();
        this.result = result.clone();
        this.cookTime = cookTime;
    }
    
    public CustomFurnaceRecipe(String recipeId, ItemStack input, ItemStack result) {
        this(recipeId, input, result, 200); // Default 10 seconds
    }
    
    public String getRecipeId() {
        return recipeId;
    }
    
    public ItemStack getInput() {
        return input.clone();
    }
    
    public ItemStack getResult() {
        return result.clone();
    }
    
    public int getCookTime() {
        return cookTime;
    }
    
    /**
     * Check if the given item matches this recipe's input exactly
     * Compares type, custom model data, display name, and lore
     */
    public boolean matchesInput(ItemStack item) {
        if (item == null || input == null) {
            return false;
        }
        
        // Check material type
        if (item.getType() != input.getType()) {
            return false;
        }
        
        // Both items must have meta or both must not have meta
        boolean itemHasMeta = item.hasItemMeta();
        boolean inputHasMeta = input.hasItemMeta();
        
        if (itemHasMeta != inputHasMeta) {
            return false;
        }
        
        // If neither has meta, they match
        if (!itemHasMeta) {
            return true;
        }
        
        // Compare custom model data
        Integer itemModelData = item.getItemMeta().hasCustomModelData() ? 
            item.getItemMeta().getCustomModelData() : null;
        Integer inputModelData = input.getItemMeta().hasCustomModelData() ? 
            input.getItemMeta().getCustomModelData() : null;
        
        if (!java.util.Objects.equals(itemModelData, inputModelData)) {
            return false;
        }
        
        // Compare display names
        String itemDisplayName = item.getItemMeta().hasDisplayName() ? 
            item.getItemMeta().getDisplayName() : null;
        String inputDisplayName = input.getItemMeta().hasDisplayName() ? 
            input.getItemMeta().getDisplayName() : null;
        
        if (!java.util.Objects.equals(itemDisplayName, inputDisplayName)) {
            return false;
        }
        
        // Compare lore
        java.util.List<String> itemLore = item.getItemMeta().hasLore() ? 
            item.getItemMeta().getLore() : null;
        java.util.List<String> inputLore = input.getItemMeta().hasLore() ? 
            input.getItemMeta().getLore() : null;
        
        return java.util.Objects.equals(itemLore, inputLore);
    }
    
    @Override
    public String toString() {
        return "CustomFurnaceRecipe{" +
                "recipeId='" + recipeId + '\'' +
                ", input=" + getItemDebugName(input) +
                ", result=" + getItemDebugName(result) +
                ", cookTime=" + cookTime +
                '}';
    }
    
    private String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
}