package com.server.crafting.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.items.CustomItems;

/**
 * Manager for handling custom crafting recipes and vanilla recipe integration
 */
public class CustomCraftingManager {
    
    private static CustomCraftingManager instance;
    private final Map<String, ItemStack> customRecipes;
    private final List<Recipe> vanillaRecipes;
    
    private CustomCraftingManager() {
        this.customRecipes = new HashMap<>();
        this.vanillaRecipes = new ArrayList<>();
        loadVanillaRecipes();
        initializeCustomRecipes(); // Add this line
    }
    
    public static CustomCraftingManager getInstance() {
        if (instance == null) {
            instance = new CustomCraftingManager();
        }
        return instance;
    }

    /**
     * Initialize custom recipes
     */
    private void initializeCustomRecipes() {
        // Add Copperhead Pickaxe recipe
        addCopperheadPickaxeRecipe();
        addForgedCopperPickaxeRecipe();
        
        // Add more custom recipes here in the future
    }

    /**
     * Add the Copperhead Pickaxe custom recipe
     * Recipe pattern:
     * [16 Copper] [16 Copper] [16 Copper]
     * [   Air   ] [ 2 Stick ] [   Air   ]
     * [   Air   ] [ 2 Stick ] [   Air   ]
     */
    private void addCopperheadPickaxeRecipe() {
        ItemStack[] recipe = new ItemStack[9];
        
        // Top row: 16 copper ingots each
        recipe[0] = new ItemStack(Material.COPPER_INGOT, 16);
        recipe[1] = new ItemStack(Material.COPPER_INGOT, 16);
        recipe[2] = new ItemStack(Material.COPPER_INGOT, 16);
        
        // Middle row: empty, 1 stick, empty
        recipe[3] = new ItemStack(Material.AIR);
        recipe[4] = new ItemStack(Material.STICK, 1);
        recipe[5] = new ItemStack(Material.AIR);
        
        // Bottom row: empty, 1 stick, empty
        recipe[6] = new ItemStack(Material.AIR);
        recipe[7] = new ItemStack(Material.STICK, 1);
        recipe[8] = new ItemStack(Material.AIR);
        
        // Result: Copperhead Pickaxe
        ItemStack result = CustomItems.createCopperheadPickaxe();
        
        addCustomRecipe(recipe, result);
        
        Main.getInstance().getLogger().info("Added custom recipe: Copperhead Pickaxe");
    }

    /**
     * Add the Forged Copper Pickaxe custom recipe
     * This recipe can be positioned in different rows due to its 2-row pattern
     * Pattern 1 (top/middle):        Pattern 2 (middle/bottom):
     * [64 Copper] [64 Copper] [64 Copper]    [   Air   ] [   Air   ] [   Air   ]
     * [   Air   ] [Copperhead] [   Air   ]    [64 Copper] [64 Copper] [64 Copper]
     * [   Air   ] [   Air   ] [   Air   ]     [   Air   ] [Copperhead] [   Air   ]
     */
    private void addForgedCopperPickaxeRecipe() {
        // Pattern 1: Top row copper, middle row copperhead pickaxe
        ItemStack[] recipe1 = new ItemStack[9];
        recipe1[0] = new ItemStack(Material.COPPER_INGOT, 64);  // Top left
        recipe1[1] = new ItemStack(Material.COPPER_INGOT, 64);  // Top middle
        recipe1[2] = new ItemStack(Material.COPPER_INGOT, 64);  // Top right
        recipe1[3] = new ItemStack(Material.AIR);               // Middle left
        recipe1[4] = CustomItems.createCopperheadPickaxe();     // Middle center
        recipe1[5] = new ItemStack(Material.AIR);               // Middle right
        recipe1[6] = new ItemStack(Material.AIR);               // Bottom left
        recipe1[7] = new ItemStack(Material.AIR);               // Bottom middle
        recipe1[8] = new ItemStack(Material.AIR);               // Bottom right
        
        // Pattern 2: Middle row copper, bottom row copperhead pickaxe
        ItemStack[] recipe2 = new ItemStack[9];
        recipe2[0] = new ItemStack(Material.AIR);               // Top left
        recipe2[1] = new ItemStack(Material.AIR);               // Top middle
        recipe2[2] = new ItemStack(Material.AIR);               // Top right
        recipe2[3] = new ItemStack(Material.COPPER_INGOT, 64);  // Middle left
        recipe2[4] = new ItemStack(Material.COPPER_INGOT, 64);  // Middle middle
        recipe2[5] = new ItemStack(Material.COPPER_INGOT, 64);  // Middle right
        recipe2[6] = new ItemStack(Material.AIR);               // Bottom left
        recipe2[7] = CustomItems.createCopperheadPickaxe();     // Bottom middle
        recipe2[8] = new ItemStack(Material.AIR);               // Bottom right
        
        // Result: Forged Copper Pickaxe
        ItemStack result = CustomItems.createForgedCopperPickaxe();
        
        // Add both patterns
        addCustomRecipe(recipe1, result);
        addCustomRecipe(recipe2, result);
        
        Main.getInstance().getLogger().info("Added custom recipe: Forged Copper Pickaxe (2 patterns)");
    }
    
    /**
     * Load all vanilla recipes (excluding leather armor dyeing)
     */
    private void loadVanillaRecipes() {
        Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
        
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            
            // Skip leather armor dyeing recipes and other complex recipes we don't want
            if (recipe.getResult().getType().name().contains("LEATHER_")) {
                continue;
            }
            
            // Skip banner pattern recipes, firework recipes, etc.
            if (recipe.getResult().getType() == Material.FIREWORK_ROCKET ||
                recipe.getResult().getType() == Material.FIREWORK_STAR ||
                recipe.getResult().getType().name().contains("BANNER")) {
                continue;
            }
            
            // Only include shaped and shapeless recipes
            if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
                vanillaRecipes.add(recipe);
            }
        }
        
        Main.getInstance().getLogger().info("Loaded " + vanillaRecipes.size() + " vanilla recipes for custom crafting");
    }
        
    /**
     * Check if the crafting grid matches a shaped recipe
     */
    private boolean matchesShapedRecipe(ItemStack[] grid, ShapedRecipe recipe) {
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredients = recipe.getIngredientMap();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            // Debug the recipe structure
            if (recipe.getResult().getType() == Material.BUCKET) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] === BUCKET RECIPE DEBUG ===");
                Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Recipe shape array length: " + shape.length);
                for (int i = 0; i < shape.length; i++) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Recipe Debug] Shape[" + i + "]: '" + shape[i] + "' (length: " + shape[i].length() + ")");
                }
                
                Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Ingredient mapping:");
                for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
                    ItemStack item = entry.getValue();
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Recipe Debug] '" + entry.getKey() + "' -> " + (item == null ? "NULL" : item.getType()));
                }
            }
        }
        
        // FIXED: Calculate max width properly
        int maxWidth = 0;
        for (String row : shape) {
            maxWidth = Math.max(maxWidth, row.length());
        }
        
        // Try all possible positions in the 3x3 grid
        for (int startRow = 0; startRow <= 3 - shape.length; startRow++) {
            for (int startCol = 0; startCol <= 3 - maxWidth; startCol++) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && recipe.getResult().getType() == Material.BUCKET) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Recipe Debug] Trying bucket recipe at position (" + startRow + "," + startCol + ")");
                }
                
                if (matchesShapedRecipeAt(grid, shape, ingredients, startRow, startCol)) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && recipe.getResult().getType() == Material.BUCKET) {
                        Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] BUCKET RECIPE MATCHED!");
                    }
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if the shaped recipe matches at a specific position
     */
    private boolean matchesShapedRecipeAt(ItemStack[] grid, String[] shape, 
                                        Map<Character, ItemStack> ingredients, int startRow, int startCol) {
        // Create a boolean array to track which slots are covered by the recipe pattern
        boolean[] patternCovered = new boolean[9];
        
        // Check the recipe pattern and mark covered slots
        for (int row = 0; row < shape.length; row++) {
            String currentRow = shape[row];
            for (int col = 0; col < currentRow.length(); col++) {
                int gridIndex = (startRow + row) * 3 + (startCol + col);
                
                // SAFETY CHECK: Ensure we don't go out of bounds
                if (gridIndex >= 9) {
                    return false;
                }
                
                // Mark this slot as covered by the pattern
                patternCovered[gridIndex] = true;
                
                char recipeChar = currentRow.charAt(col);
                ItemStack requiredItem = ingredients.get(recipeChar);
                ItemStack gridItem = grid[gridIndex];
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Recipe Debug] Checking slot " + gridIndex + 
                        " - Recipe char: '" + recipeChar + "'" +
                        " - Required: " + (requiredItem == null ? "NULL" : requiredItem.getType()) +
                        " - Found: " + (gridItem == null ? "NULL" : gridItem.getType()));
                }
                
                if (recipeChar == ' ' || requiredItem == null) {
                    // Empty space in recipe (either explicit space or unmapped character) - this slot MUST be empty
                    if (gridItem != null && gridItem.getType() != Material.AIR) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Recipe Debug] FAILED - Empty slot " + gridIndex + " should be empty but contains: " + gridItem.getType());
                        }
                        return false;
                    }
                } else {
                    // Required ingredient
                    if (!itemsMatch(gridItem, requiredItem)) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Recipe Debug] FAILED - Ingredient mismatch at slot " + gridIndex + 
                                " - Expected: " + requiredItem.getType() + ", Found: " + (gridItem == null ? "NULL" : gridItem.getType()));
                        }
                        return false;
                    }
                }
            }
        }
        
        // CRITICAL FIX: Check that all slots NOT covered by the pattern are empty (null or AIR)
        for (int i = 0; i < 9; i++) {
            if (!patternCovered[i]) {
                ItemStack slotItem = grid[i];
                // FIXED: Check for both null AND AIR
                if (slotItem != null && slotItem.getType() != Material.AIR) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Recipe Debug] FAILED - Non-pattern slot " + i + " should be empty but contains: " + slotItem.getType());
                    }
                    return false; // Found item in slot not covered by recipe pattern
                }
            }
        }

        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            StringBuilder patternDebug = new StringBuilder("[Recipe Debug] Pattern coverage: ");
            for (int i = 0; i < 9; i++) {
                patternDebug.append(i).append(":").append(patternCovered[i] ? "✓" : "✗").append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, patternDebug.toString());
            
            // Add debug for actual slot contents
            StringBuilder slotDebug = new StringBuilder("[Recipe Debug] Slot contents: ");
            for (int i = 0; i < 9; i++) {
                ItemStack slotItem = grid[i];
                String content = slotItem == null ? "NULL" : 
                            slotItem.getType() == Material.AIR ? "AIR" : slotItem.getType().name();
                slotDebug.append(i).append(":").append(content).append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, slotDebug.toString());
            
            // Add debug for recipe shape analysis with more detail
            StringBuilder shapeDebug = new StringBuilder("[Recipe Debug] Shape analysis: ");
            for (int row = 0; row < shape.length; row++) {
                for (int col = 0; col < shape[row].length(); col++) {
                    int gridIndex = (startRow + row) * 3 + (startCol + col);
                    char recipeChar = shape[row].charAt(col);
                    shapeDebug.append("(").append(gridIndex).append(":").append(recipeChar).append(") ");
                }
            }
            Main.getInstance().debugLog(DebugSystem.GUI, shapeDebug.toString());
            
            // Add debug for ingredient mapping
            StringBuilder ingredientDebug = new StringBuilder("[Recipe Debug] Ingredient mapping: ");
            for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
                ItemStack item = entry.getValue();
                ingredientDebug.append(entry.getKey()).append("=")
                            .append(item == null ? "NULL" : item.getType()).append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, ingredientDebug.toString());
        }
        
        return true;
    }
    
    /**
     * Check if the crafting grid matches a shapeless recipe
     */
    private boolean matchesShapelessRecipe(ItemStack[] grid, ShapelessRecipe recipe) {
        List<ItemStack> required = new ArrayList<>(recipe.getIngredientList());
        List<ItemStack> available = new ArrayList<>();
        
        // Collect all non-air items from the grid
        for (ItemStack item : grid) {
            if (item.getType() != Material.AIR) {
                available.add(item.clone());
            }
        }
        
        // Must have the same number of ingredients
        if (required.size() != available.size()) {
            return false;
        }
        
        // Try to match each required ingredient with an available one
        for (ItemStack requiredItem : required) {
            boolean found = false;
            for (int i = 0; i < available.size(); i++) {
                if (itemsMatch(available.get(i), requiredItem)) {
                    available.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return available.isEmpty();
    }
    
    /**
     * Check if two items match for recipe purposes
     */
    private boolean itemsMatch(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        return item1.getType() == item2.getType();
    }
    
    /**
     * Create a recipe key from the crafting grid for custom recipes
     */
    private String createRecipeKey(ItemStack[] grid) {
        StringBuilder key = new StringBuilder();
        for (ItemStack item : grid) {
            if (item == null || item.getType() == Material.AIR) {
                key.append("AIR:0,");
            } else {
                key.append(item.getType().name()).append(":").append(item.getAmount()).append(",");
            }
        }
        return key.toString();
    }

    /**
     * Check if the crafting grid matches a custom recipe pattern
     */
    private boolean matchesCustomRecipe(ItemStack[] grid, ItemStack[] recipePattern) {
        if (grid.length != recipePattern.length) return false;
        
        for (int i = 0; i < grid.length; i++) {
            ItemStack gridItem = grid[i] == null ? new ItemStack(Material.AIR) : grid[i];
            ItemStack recipeItem = recipePattern[i] == null ? new ItemStack(Material.AIR) : recipePattern[i];
            
            // Check material type
            if (gridItem.getType() != recipeItem.getType()) {
                return false;
            }
            
            // Skip amount and custom data checks for AIR
            if (recipeItem.getType() == Material.AIR) {
                continue;
            }
            
            // Check amount (grid must have at least the required amount)
            if (gridItem.getAmount() < recipeItem.getAmount()) {
                return false;
            }
            
            // Special handling for custom items (check display name and custom model data)
            if (isCustomItem(recipeItem)) {
                if (!isCustomItem(gridItem)) {
                    return false; // Recipe requires custom item but grid has vanilla item
                }
                
                // Check if they're the same custom item by comparing display names and model data
                if (!areCustomItemsSame(gridItem, recipeItem)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Check if an item is a custom item (has custom model data)
     */
    private boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData();
    }

    /**
     * Check if two custom items are the same type
     */
    private boolean areCustomItemsSame(ItemStack item1, ItemStack item2) {
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) return false;
        
        // Compare custom model data
        if (item1.getItemMeta().hasCustomModelData() && item2.getItemMeta().hasCustomModelData()) {
            if (item1.getItemMeta().getCustomModelData() != item2.getItemMeta().getCustomModelData()) {
                return false;
            }
        }
        
        // Compare display names
        if (item1.getItemMeta().hasDisplayName() && item2.getItemMeta().hasDisplayName()) {
            return item1.getItemMeta().getDisplayName().equals(item2.getItemMeta().getDisplayName());
        }
        
        return true;
    }

    /**
     * Get the result of a recipe from a 3x3 crafting grid
     */
    public ItemStack getRecipeResult(ItemStack[] craftingGrid) {
        // Clean the grid (convert null to air)
        ItemStack[] cleanGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // First check custom recipes with pattern matching
        for (Map.Entry<String, CustomRecipeData> entry : customRecipePatterns.entrySet()) {
            if (matchesCustomRecipe(cleanGrid, entry.getValue().pattern)) {
                return entry.getValue().result.clone();
            }
        }
        
        // Then check vanilla recipes
        for (Recipe recipe : vanillaRecipes) {
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                if (matchesShapedRecipe(cleanGrid, shapedRecipe)) {
                    ItemStack result = recipe.getResult().clone();
                    // Apply rarity to vanilla crafted items
                    return com.server.items.ItemManager.applyRarity(result);
                }
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                if (matchesShapelessRecipe(cleanGrid, shapelessRecipe)) {
                    ItemStack result = recipe.getResult().clone();
                    // Apply rarity to vanilla crafted items
                    return com.server.items.ItemManager.applyRarity(result);
                }
            }
        }
        
        return null; // No matching recipe
    }
    
    /**
     * Consume ingredients from the crafting grid after successful crafting
     */
    public void consumeIngredients(ItemStack[] craftingGrid, ItemStack result) {
        // Find which recipe was used
        ItemStack[] cleanGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Check if it's a custom recipe
        for (Map.Entry<String, CustomRecipeData> entry : customRecipePatterns.entrySet()) {
            if (matchesCustomRecipe(cleanGrid, entry.getValue().pattern) && 
                entry.getValue().result.isSimilar(result)) {
                // Consume ingredients according to custom recipe pattern
                ItemStack[] pattern = entry.getValue().pattern;
                for (int i = 0; i < craftingGrid.length; i++) {
                    if (pattern[i] != null && pattern[i].getType() != Material.AIR && 
                        craftingGrid[i] != null && craftingGrid[i].getType() != Material.AIR) {
                        
                        int consumeAmount = pattern[i].getAmount();
                        
                        // For custom items, consume the entire item regardless of amount
                        if (isCustomItem(pattern[i])) {
                            craftingGrid[i] = new ItemStack(Material.AIR);
                        } else {
                            // For regular items, consume the specified amount
                            craftingGrid[i].setAmount(craftingGrid[i].getAmount() - consumeAmount);
                            if (craftingGrid[i].getAmount() <= 0) {
                                craftingGrid[i] = new ItemStack(Material.AIR);
                            }
                        }
                    }
                }
                return;
            }
        }
        
        // If not a custom recipe, consume 1 of each ingredient (vanilla behavior)
        for (int i = 0; i < craftingGrid.length; i++) {
            if (craftingGrid[i] != null && craftingGrid[i].getType() != Material.AIR) {
                craftingGrid[i].setAmount(craftingGrid[i].getAmount() - 1);
                if (craftingGrid[i].getAmount() <= 0) {
                    craftingGrid[i] = new ItemStack(Material.AIR);
                }
            }
        }
    }

    /**
     * Get the maximum number of items that can be crafted with the current ingredients
     */
    public int getMaxCraftableAmount(ItemStack[] craftingGrid, ItemStack result) {
        if (result == null) return 0;
        
        // Clean the grid
        ItemStack[] cleanGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Check if it's a custom recipe
        for (Map.Entry<String, CustomRecipeData> entry : customRecipePatterns.entrySet()) {
            if (matchesCustomRecipe(cleanGrid, entry.getValue().pattern) && 
                entry.getValue().result.isSimilar(result)) {
                
                // Calculate max crafts based on custom recipe requirements
                int maxCrafts = Integer.MAX_VALUE;
                ItemStack[] pattern = entry.getValue().pattern;
                
                for (int i = 0; i < craftingGrid.length; i++) {
                    if (pattern[i] != null && pattern[i].getType() != Material.AIR) {
                        if (craftingGrid[i] == null || craftingGrid[i].getType() != pattern[i].getType()) {
                            return 0; // Missing required ingredient
                        }
                        
                        int possibleCrafts;
                        
                        // For custom items, can only craft once (they're consumed entirely)
                        if (isCustomItem(pattern[i])) {
                            possibleCrafts = areCustomItemsSame(craftingGrid[i], pattern[i]) ? 1 : 0;
                        } else {
                            // For regular items, calculate based on amounts
                            int availableAmount = craftingGrid[i].getAmount();
                            int requiredAmount = pattern[i].getAmount();
                            possibleCrafts = availableAmount / requiredAmount;
                        }
                        
                        maxCrafts = Math.min(maxCrafts, possibleCrafts);
                    }
                }
                
                if (maxCrafts == Integer.MAX_VALUE) maxCrafts = 0;
                
                // Limit by result stack size
                int maxStackSize = result.getMaxStackSize();
                maxCrafts = Math.min(maxCrafts, maxStackSize / result.getAmount());
                
                return maxCrafts * result.getAmount();
            }
        }
        
        // If not a custom recipe, use vanilla logic
        int minStackSize = Integer.MAX_VALUE;
        boolean hasIngredients = false;
        
        for (ItemStack item : craftingGrid) {
            if (item != null && item.getType() != Material.AIR) {
                hasIngredients = true;
                minStackSize = Math.min(minStackSize, item.getAmount());
            }
        }
        
        if (!hasIngredients) return 0;
        
        int maxCrafts = minStackSize;
        int maxStackSize = result.getMaxStackSize();
        maxCrafts = Math.min(maxCrafts, maxStackSize / result.getAmount());
        
        return maxCrafts * result.getAmount();
    }
    
    /**
     * Remove a custom recipe
     */
    public void removeCustomRecipe(ItemStack[] pattern) {
        String key = createRecipeKey(pattern);
        customRecipes.remove(key);
    }
    
    /**
     * Get all custom recipes
     */
    public Map<String, ItemStack> getCustomRecipes() {
        return new HashMap<>(customRecipes);
    }

    /**
     * Data structure to hold custom recipe information
     */
    private static class CustomRecipeData {
        public final ItemStack[] pattern;
        public final ItemStack result;
        
        public CustomRecipeData(ItemStack[] pattern, ItemStack result) {
            this.pattern = pattern;
            this.result = result;
        }
    }

    private final Map<String, CustomRecipeData> customRecipePatterns = new HashMap<>();

    /**
     * Add a custom recipe with pattern matching
     */
    public void addCustomRecipe(ItemStack[] pattern, ItemStack result) {
        String key = createRecipeKey(pattern);
        customRecipePatterns.put(key, new CustomRecipeData(pattern.clone(), result.clone()));
        customRecipes.put(key, result.clone()); // Keep for backwards compatibility
    }
}