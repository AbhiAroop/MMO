package com.server.crafting.recipes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a custom furnace recipe with temperature requirements
 * Step 4: Recipe foundation system
 */
public class FurnaceRecipe {
    
    private final String recipeId;
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;
    private final int requiredTemperature;
    private final int cookTime; // In ticks
    private final RecipeType recipeType;
    private final String displayName;
    private final String description;
    
    public enum RecipeType {
        SMELTING("Smelting", "Basic material processing"),
        ALLOYING("Alloying", "Combining metals to create alloys"), 
        REFINING("Refining", "Purifying materials to higher grades"),
        CRYSTALLIZATION("Crystallization", "Growing crystals from raw materials"),
        EXTRACTION("Extraction", "Extracting essences from materials"),
        TRANSMUTATION("Transmutation", "Magical transformation of matter");
        
        private final String displayName;
        private final String description;
        
        RecipeType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public FurnaceRecipe(String recipeId, List<ItemStack> inputs, List<ItemStack> outputs, 
                        int requiredTemperature, int cookTime, RecipeType recipeType,
                        String displayName, String description) {
        this.recipeId = recipeId;
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
        this.requiredTemperature = requiredTemperature;
        this.cookTime = cookTime;
        this.recipeType = recipeType;
        this.displayName = displayName;
        this.description = description;
    }
    
    // Getters
    public String getRecipeId() { return recipeId; }
    public List<ItemStack> getInputs() { return new ArrayList<>(inputs); }
    public List<ItemStack> getOutputs() { return new ArrayList<>(outputs); }
    public int getRequiredTemperature() { return requiredTemperature; }
    public int getCookTime() { return cookTime; }
    public RecipeType getRecipeType() { return recipeType; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    /**
     * Check if the provided inputs match this recipe
     */
    public boolean matches(List<ItemStack> providedInputs) {
        if (providedInputs.size() != inputs.size()) {
            return false;
        }
        
        // Create working copies to avoid modifying original lists
        List<ItemStack> requiredCopy = new ArrayList<>();
        for (ItemStack item : inputs) {
            if (item != null) {
                requiredCopy.add(item.clone());
            }
        }
        
        List<ItemStack> providedCopy = new ArrayList<>();
        for (ItemStack item : providedInputs) {
            if (item != null) {
                providedCopy.add(item.clone());
            }
        }
        
        // Try to match each required item with provided items
        for (ItemStack required : requiredCopy) {
            boolean found = false;
            
            for (int i = 0; i < providedCopy.size(); i++) {
                ItemStack provided = providedCopy.get(i);
                
                if (provided != null && itemsMatch(required, provided)) {
                    // Check if we have enough quantity
                    if (provided.getAmount() >= required.getAmount()) {
                        // Consume the required amount
                        provided.setAmount(provided.getAmount() - required.getAmount());
                        if (provided.getAmount() <= 0) {
                            providedCopy.set(i, null);
                        }
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if two items match for recipe purposes
     */
    private boolean itemsMatch(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        // Check basic material match
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        // For custom items, check custom model data
        if (item1.hasItemMeta() && item1.getItemMeta().hasCustomModelData() &&
            item2.hasItemMeta() && item2.getItemMeta().hasCustomModelData()) {
            
            return item1.getItemMeta().getCustomModelData() == 
                   item2.getItemMeta().getCustomModelData();
        }
        
        // For vanilla items, just material match is sufficient
        return !item1.hasItemMeta() || !item1.getItemMeta().hasCustomModelData();
    }
    
    /**
     * Get formatted cook time string
     */
    public String getFormattedCookTime() {
        int seconds = cookTime / 20;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
    }
    
    /**
     * Get formatted temperature string
     */
    public String getFormattedTemperature() {
        return com.server.crafting.temperature.TemperatureSystem.formatTemperature(requiredTemperature);
    }
    
    /**
     * Create a simple recipe key for lookup
     */
    public String createInputKey() {
        StringBuilder key = new StringBuilder();
        for (ItemStack input : inputs) {
            if (input != null) {
                key.append(input.getType().name());
                if (input.hasItemMeta() && input.getItemMeta().hasCustomModelData()) {
                    key.append(":").append(input.getItemMeta().getCustomModelData());
                }
                key.append(":").append(input.getAmount()).append(",");
            }
        }
        return key.toString();
    }
}