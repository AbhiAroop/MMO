package com.server.crafting.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.items.CustomItems;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.skills.mining.MiningSkill;

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
        // Determine grid size based on array length
        int gridSize = grid.length == 9 ? 3 : 4; // 3x3 or 4x4
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Recipe Debug] Checking shaped recipe at position (" + startRow + "," + startCol + ") in " + gridSize + "x" + gridSize + " grid");
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Recipe Debug] Recipe shape: " + String.join(" | ", shape));
        }
        
        // Create a boolean array to track which slots are covered by the recipe pattern
        boolean[] patternCovered = new boolean[grid.length];
        
        // Check the recipe pattern and mark covered slots
        for (int row = 0; row < shape.length; row++) {
            String currentRow = shape[row];
            for (int col = 0; col < currentRow.length(); col++) {
                int gridIndex = (startRow + row) * gridSize + (startCol + col);
                
                // SAFETY CHECK: Ensure we don't go out of bounds
                if (gridIndex >= grid.length || (startRow + row) >= gridSize || (startCol + col) >= gridSize) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Recipe Debug] FAILED - Recipe extends beyond grid bounds at (" + (startRow + row) + "," + (startCol + col) + ")");
                    }
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
                        " (row=" + (startRow + row) + ",col=" + (startCol + col) + ")" +
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
        for (int i = 0; i < grid.length; i++) {
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
            for (int i = 0; i < grid.length; i++) {
                patternDebug.append(i).append(":").append(patternCovered[i] ? "✓" : "✗").append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, patternDebug.toString());
            
            // Add debug for actual slot contents
            StringBuilder slotDebug = new StringBuilder("[Recipe Debug] Slot contents: ");
            for (int i = 0; i < grid.length; i++) {
                ItemStack slotItem = grid[i];
                String content = slotItem == null ? "NULL" : 
                            slotItem.getType() == Material.AIR ? "AIR" : slotItem.getType().name();
                slotDebug.append(i).append(":").append(content).append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, slotDebug.toString());
            
            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] ✅ RECIPE MATCH SUCCESS!");
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
    public ItemStack getRecipeResult(ItemStack[] craftingGrid, Player player) {
        if (craftingGrid.length != 9) return null;
        
        // Clean the grid first
        ItemStack[] cleanGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Count non-air items to help with prioritization
        int nonAirCount = 0;
        for (ItemStack item : cleanGrid) {
            if (item != null && item.getType() != Material.AIR) {
                nonAirCount++;
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Looking for recipes with " + nonAirCount + " ingredients");
        }
        
        // PRIORITY 1: Check custom recipes first (they have explicit patterns)
        String recipeKey = createRecipeKey(cleanGrid);
        if (customRecipes.containsKey(recipeKey)) {
            ItemStack result = customRecipes.get(recipeKey);
            if (isRecipeUnlocked(player, result)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Found custom recipe: " + result.getType());
                }
                return result;
            }
        }
        
        // Check custom recipe patterns
        for (Map.Entry<String, CustomRecipeData> entry : customRecipePatterns.entrySet()) {
            if (matchesCustomRecipe(cleanGrid, entry.getValue().pattern)) {
                ItemStack result = entry.getValue().result;
                if (isRecipeUnlocked(player, result)) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Found custom pattern recipe: " + result.getType());
                    }
                    return result;
                }
            }
        }
        
        // PRIORITY 2: Check vanilla recipes, but sort them by ingredient count (largest first)
        List<Recipe> sortedRecipes = new ArrayList<>(vanillaRecipes);
        
        // Sort recipes by the number of ingredients they require (descending)
        sortedRecipes.sort((r1, r2) -> {
            int count1 = getRecipeIngredientCount(r1);
            int count2 = getRecipeIngredientCount(r2);
            return Integer.compare(count2, count1); // Descending order (most ingredients first)
        });
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Checking " + sortedRecipes.size() + " vanilla recipes (sorted by ingredient count)");
        }
        
        for (Recipe recipe : sortedRecipes) {
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                
                // Skip recipes that require more ingredients than we have
                int requiredIngredients = getRecipeIngredientCount(shapedRecipe);
                if (requiredIngredients > nonAirCount) {
                    continue;
                }
                
                if (matchesShapedRecipe(cleanGrid, shapedRecipe)) {
                    ItemStack result = shapedRecipe.getResult();
                    if (isRecipeUnlocked(player, result)) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Found vanilla shaped recipe: " + result.getType() + " (requires " + requiredIngredients + " ingredients)");
                        }
                        return result;
                    }
                }
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                
                // Skip recipes that require more ingredients than we have
                int requiredIngredients = shapelessRecipe.getIngredientList().size();
                if (requiredIngredients > nonAirCount) {
                    continue;
                }
                
                if (matchesShapelessRecipe(cleanGrid, shapelessRecipe)) {
                    ItemStack result = shapelessRecipe.getResult();
                    if (isRecipeUnlocked(player, result)) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] Found vanilla shapeless recipe: " + result.getType() + " (requires " + requiredIngredients + " ingredients)");
                        }
                        return result;
                    }
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Recipe Debug] No matching recipes found");
        }
        
        return null;
    }

    /**
     * Helper method to count the number of ingredients a recipe requires
     */
    private int getRecipeIngredientCount(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shaped = (ShapedRecipe) recipe;
            return (int) shaped.getIngredientMap().values().stream()
                    .filter(item -> item != null && item.getType() != Material.AIR)
                    .count();
        } else if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
            return shapeless.getIngredientList().size();
        }
        return 0;
    }

    /**
     * Get the result of a recipe from a 3x3 crafting grid (legacy method for backward compatibility)
     */
    public ItemStack getRecipeResult(ItemStack[] craftingGrid) {
        // For backward compatibility, we need to handle cases where no player is provided
        // In this case, we'll assume all recipes are unlocked
        return getRecipeResultWithoutPlayerCheck(craftingGrid);
    }

    /**
     * Get recipe result without player unlock checks (for backward compatibility)
     */
    private ItemStack getRecipeResultWithoutPlayerCheck(ItemStack[] craftingGrid) {
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
     * Check if a recipe result is unlocked for the player
     */
    private boolean isRecipeUnlocked(Player player, ItemStack result) {
        MiningSkill miningSkill = (MiningSkill) SkillRegistry.getInstance().getSkill(SkillType.MINING);
        
        // Check for copperhead pickaxe
        if (CustomItems.createCopperheadPickaxe().isSimilar(result)) {
            return miningSkill.isCopperheadCraftingUnlocked(player);
        }
        
        // Check for forged copper pickaxe
        if (CustomItems.createForgedCopperPickaxe().isSimilar(result)) {
            return miningSkill.isForgedCopperCraftingUnlocked(player);
        }
        
        // All other recipes are unlocked by default
        return true;
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

    private final Map<String, AdvancedRecipeData> advancedRecipePatterns = new HashMap<>();

    /**
     * Data structure to hold advanced recipe information with multiple outputs
     */
    private static class AdvancedRecipeData {
        public final ItemStack[] pattern; // 16-slot array for 4x4 grid
        public final ItemStack[] results; // Multiple output items
        
        public AdvancedRecipeData(ItemStack[] pattern, ItemStack[] results) {
            this.pattern = pattern.clone();
            this.results = results.clone();
        }
    }

    /**
     * Add a 4x4 custom recipe with multiple outputs
     */
    public void addAdvancedRecipe(ItemStack[] pattern, ItemStack[] results) {
        if (pattern.length != 16) {
            throw new IllegalArgumentException("Advanced recipe pattern must be 16 items (4x4 grid)");
        }
        
        String key = createAdvancedRecipeKey(pattern);
        advancedRecipePatterns.put(key, new AdvancedRecipeData(pattern, results));
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Advanced Crafting] Added 4x4 recipe with " + results.length + " outputs");
        }
    }

    /**
     * Get the result of a 4x4 recipe from a 4x4 crafting grid
     */
    public ItemStack[] getAdvancedRecipeResult(ItemStack[] craftingGrid, Player player) {
        if (craftingGrid.length != 16) return null;
        
        // Clean the grid (convert null to air)
        ItemStack[] cleanGrid = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Check 4x4 custom recipes first
        for (Map.Entry<String, AdvancedRecipeData> entry : advancedRecipePatterns.entrySet()) {
            if (matchesAdvancedRecipe(cleanGrid, entry.getValue().pattern)) {
                ItemStack[] results = entry.getValue().results;
                
                // Check if player has unlocked all recipes in the result
                boolean allUnlocked = true;
                for (ItemStack result : results) {
                    if (result != null && result.getType() != Material.AIR) {
                        if (!isRecipeUnlocked(player, result)) {
                            allUnlocked = false;
                            break;
                        }
                    }
                }
                
                if (allUnlocked) {
                    return cloneResults(results);
                }
            }
        }
        
        // Try to fit 3x3 recipes (including custom 3x3 recipes) into the 4x4 grid
        return tryFit3x3RecipesIn4x4Grid(cleanGrid, player);
    }

    /**
     * Extract a 3x3 grid from a specific position in the 4x4 grid
     */
    private ItemStack[] extract3x3FromPosition(ItemStack[] grid4x4, int startRow, int startCol) {
        ItemStack[] grid3x3 = new ItemStack[9];
        int index3x3 = 0;
        
        // Add debug for extraction
        debugGridExtraction(grid4x4, startRow, startCol);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index4x4 = (startRow + row) * 4 + (startCol + col);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    ItemStack item = index4x4 < 16 ? grid4x4[index4x4] : null;
                    String content = item == null ? "NULL" : 
                                item.getType() == Material.AIR ? "AIR" : item.getType().name();
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[4x4 Recipe] 4x4[" + index4x4 + "] -> 3x3[" + index3x3 + "] = " + content);
                }
                
                // FIXED: Add bounds checking to prevent array out of bounds
                if (index4x4 < 16) {
                    grid3x3[index3x3] = grid4x4[index4x4];
                } else {
                    grid3x3[index3x3] = new ItemStack(Material.AIR);
                }
                index3x3++;
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            StringBuilder extractDebug = new StringBuilder("[4x4 Recipe] Final extracted 3x3 grid: ");
            for (int i = 0; i < 9; i++) {
                ItemStack item = grid3x3[i];
                String content = item == null ? "NULL" : 
                            item.getType() == Material.AIR ? "AIR" : item.getType().name();
                extractDebug.append("[").append(i).append(":").append(content).append("] ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, extractDebug.toString());
        }
        
        return grid3x3;
    }

    /**
     * Try to fit 3x3 recipes into different positions of the 4x4 grid
     * IMPORTANT: All slots outside the 3x3 area must be empty for the recipe to work
     */
    private ItemStack[] tryFit3x3RecipesIn4x4Grid(ItemStack[] grid4x4, Player player) {
        debugPositionExtraction(grid4x4);
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[4x4 Recipe] === Trying to fit 3x3 recipes in 4x4 grid ===");
            
            // Debug the 4x4 grid contents with explicit grid layout
            StringBuilder gridDebug = new StringBuilder("[4x4 Recipe] 4x4 Grid contents (16 slots):\n");
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int index = row * 4 + col;
                    ItemStack item = grid4x4[index];
                    String content = item == null ? "NULL" : 
                                item.getType() == Material.AIR ? "AIR" : item.getType().name();
                    gridDebug.append(String.format("[%2d:%8s] ", index, content));
                }
                gridDebug.append("\n");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, gridDebug.toString());
            
            // Count non-air items
            int nonAirCount = 0;
            for (int i = 0; i < 16; i++) {
                if (grid4x4[i] != null && grid4x4[i].getType() != Material.AIR) {
                    nonAirCount++;
                }
            }
            Main.getInstance().debugLog(DebugSystem.GUI, "[4x4 Recipe] Total non-air items in 4x4 grid: " + nonAirCount);
        }
        
        // Try all possible 3x3 positions within the 4x4 grid
        for (int startRow = 0; startRow <= 1; startRow++) { // Can start at row 0 or 1
            for (int startCol = 0; startCol <= 1; startCol++) { // Can start at col 0 or 1
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[4x4 Recipe] === Trying 3x3 position starting at (" + startRow + "," + startCol + ") ===");
                }
                
                // CRITICAL FIX: Check if all slots OUTSIDE the 3x3 area are empty first
                if (!areNon3x3SlotsEmpty(grid4x4, startRow, startCol)) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[4x4 Recipe] ❌ Position (" + startRow + "," + startCol + ") invalid - items found outside 3x3 area");
                    }
                    continue; // Skip this position if there are items outside the 3x3 area
                }
                
                ItemStack[] extracted3x3 = extract3x3FromPosition(grid4x4, startRow, startCol);
                
                // Count non-air items in extracted 3x3
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    int nonAir3x3 = 0;
                    StringBuilder extractDebug = new StringBuilder("[4x4 Recipe] Extracted 3x3 grid from position (" + startRow + "," + startCol + "):\n");
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int index = row * 3 + col;
                            ItemStack item = extracted3x3[index];
                            String content = item == null ? "NULL" : 
                                        item.getType() == Material.AIR ? "AIR" : item.getType().name();
                            extractDebug.append(String.format("[%d:%8s] ", index, content));
                            if (item != null && item.getType() != Material.AIR) {
                                nonAir3x3++;
                            }
                        }
                        extractDebug.append("\n");
                    }
                    extractDebug.append("Non-air items in 3x3: ").append(nonAir3x3);
                    Main.getInstance().debugLog(DebugSystem.GUI, extractDebug.toString());
                }
                
                // Use the player-aware 3x3 recipe method to support custom recipes and unlocks
                ItemStack result = getRecipeResult(extracted3x3, player);
                
                if (result != null) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[4x4 Recipe] ✅ Found matching 3x3 recipe at position (" + startRow + "," + startCol + "): " + result.getType());
                    }
                    return new ItemStack[]{result};
                } else {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[4x4 Recipe] ❌ No recipe match at position (" + startRow + "," + startCol + ")");
                    }
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[4x4 Recipe] ❌ No matching 3x3 recipes found in any position of 4x4 grid");
        }
        
        return null;
    }

    /**
     * Check if all slots outside a 3x3 area within the 4x4 grid are empty
     * This ensures that 3x3 recipes only work when there are no extra items
     */
    private boolean areNon3x3SlotsEmpty(ItemStack[] grid4x4, int startRow, int startCol) {
        // Create a boolean array to mark which slots are part of the 3x3 area
        boolean[] in3x3Area = new boolean[16];
        
        // Mark all slots that are part of the 3x3 area
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index4x4 = (startRow + row) * 4 + (startCol + col);
                if (index4x4 < 16) {
                    in3x3Area[index4x4] = true;
                }
            }
        }
        
        // Check that all slots NOT in the 3x3 area are empty
        for (int i = 0; i < 16; i++) {
            if (!in3x3Area[i]) {
                ItemStack item = grid4x4[i];
                if (item != null && item.getType() != Material.AIR) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[4x4 Recipe] Non-3x3 slot " + i + " contains: " + item.getType() + " (should be empty)");
                    }
                    return false; // Found an item outside the 3x3 area
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            StringBuilder coverage = new StringBuilder("[4x4 Recipe] 3x3 area coverage for position (" + startRow + "," + startCol + "): ");
            for (int i = 0; i < 16; i++) {
                coverage.append(i).append(":").append(in3x3Area[i] ? "✓" : "✗").append(" ");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, coverage.toString());
            Main.getInstance().debugLog(DebugSystem.GUI, "[4x4 Recipe] ✅ All non-3x3 slots are empty for position (" + startRow + "," + startCol + ")");
        }
        
        return true;
    }

    /**
     * Consume ingredients from a 4x4 crafting grid with player context
     */
    public void consumeAdvancedIngredients(ItemStack[] craftingGrid, ItemStack[] results, Player player) {
        if (craftingGrid.length != 16) return;
        
        // Find which 4x4 recipe was used
        ItemStack[] cleanGrid = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Check if it's an advanced recipe
        for (Map.Entry<String, AdvancedRecipeData> entry : advancedRecipePatterns.entrySet()) {
            if (matchesAdvancedRecipe(cleanGrid, entry.getValue().pattern)) {
                // Consume ingredients according to advanced recipe pattern
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
        
        // If not an advanced recipe, try 3x3 consumption
        // Find which 3x3 position was used and consume only those ingredients
        for (int startRow = 0; startRow <= 1; startRow++) {
            for (int startCol = 0; startCol <= 1; startCol++) {
                ItemStack[] extracted3x3 = extract3x3FromPosition(cleanGrid, startRow, startCol);
                // Use player parameter when available, otherwise use without player check
                ItemStack result = player != null ? getRecipeResult(extracted3x3, player) : getRecipeResultWithoutPlayerCheck(extracted3x3);
                
                if (result != null && results.length > 0 && result.isSimilar(results[0])) {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[4x4 Recipe] Consuming ingredients from 3x3 position (" + startRow + "," + startCol + ")");
                    }
                    
                    // Consume ingredients from the 3x3 area
                    consumeIngredients(extracted3x3, result);
                    
                    // Put the consumed 3x3 back into the 4x4 grid
                    int index3x3 = 0;
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int index4x4 = (startRow + row) * 4 + (startCol + col);
                            if (index4x4 < 16) { // Bounds check
                                craftingGrid[index4x4] = extracted3x3[index3x3];
                            }
                            index3x3++;
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Get the maximum number of items that can be crafted with the current ingredients for 4x4 recipes (with player context)
     */
    public int getMaxAdvancedCraftableAmount(ItemStack[] craftingGrid, ItemStack result, Player player) {
        if (craftingGrid.length != 16) return 0;
        
        // Clean the grid
        ItemStack[] cleanGrid = new ItemStack[16];
        for (int i = 0; i < 16; i++) {
            cleanGrid[i] = craftingGrid[i] == null ? new ItemStack(Material.AIR) : craftingGrid[i];
        }
        
        // Check if it's an advanced recipe
        for (Map.Entry<String, AdvancedRecipeData> entry : advancedRecipePatterns.entrySet()) {
            if (matchesAdvancedRecipe(cleanGrid, entry.getValue().pattern)) {
                // Calculate max crafts based on ingredient amounts
                ItemStack[] pattern = entry.getValue().pattern;
                int maxCrafts = Integer.MAX_VALUE;
                
                for (int i = 0; i < pattern.length; i++) {
                    if (pattern[i] != null && pattern[i].getType() != Material.AIR) {
                        ItemStack gridItem = cleanGrid[i];
                        if (gridItem == null || gridItem.getType() == Material.AIR) {
                            return 0; // Missing required ingredient
                        }
                        
                        int requiredAmount = pattern[i].getAmount();
                        int availableAmount = gridItem.getAmount();
                        int possibleCrafts = availableAmount / requiredAmount;
                        maxCrafts = Math.min(maxCrafts, possibleCrafts);
                    }
                }
                
                return maxCrafts == Integer.MAX_VALUE ? 0 : maxCrafts * result.getAmount();
            }
        }
        
        // If not an advanced recipe, try 3x3 recipes
        for (int startRow = 0; startRow <= 1; startRow++) {
            for (int startCol = 0; startCol <= 1; startCol++) {
                ItemStack[] extracted3x3 = extract3x3FromPosition(cleanGrid, startRow, startCol);
                // Use player parameter when available, otherwise use without player check
                ItemStack recipe3x3Result = player != null ? getRecipeResult(extracted3x3, player) : getRecipeResultWithoutPlayerCheck(extracted3x3);
                
                if (recipe3x3Result != null && recipe3x3Result.isSimilar(result)) {
                    return getMaxCraftableAmount(extracted3x3, result);
                }
            }
        }
        
        return 0;
    }

    /**
     * Check if a 4x4 grid matches an advanced recipe pattern
     */
    private boolean matchesAdvancedRecipe(ItemStack[] grid, ItemStack[] recipePattern) {
        if (grid.length != 16 || recipePattern.length != 16) return false;
        
        for (int i = 0; i < 16; i++) {
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
            
            // Special handling for custom items
            if (isCustomItem(recipeItem)) {
                if (!isCustomItem(gridItem)) {
                    return false;
                }
                
                if (!areCustomItemsSame(gridItem, recipeItem)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Create a recipe key from a 4x4 crafting grid
     */
    private String createAdvancedRecipeKey(ItemStack[] grid) {
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
     * Clone the results array to prevent modification
     */
    private ItemStack[] cloneResults(ItemStack[] results) {
        ItemStack[] cloned = new ItemStack[results.length];
        for (int i = 0; i < results.length; i++) {
            if (results[i] != null) {
                cloned[i] = results[i].clone();
            }
        }
        return cloned;
    }

    /**
     * Debug method to verify 4x4 grid extraction
     */
    private void debugGridExtraction(ItemStack[] grid4x4, int startRow, int startCol) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[4x4 Debug] Extracting 3x3 from 4x4 grid at position (" + startRow + "," + startCol + ")");
            
            // Show the 4x4 grid layout
            StringBuilder grid4x4Debug = new StringBuilder("[4x4 Debug] Full 4x4 grid:\n");
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int index = row * 4 + col;
                    ItemStack item = grid4x4[index];
                    String content = item == null ? "NULL" : 
                                item.getType() == Material.AIR ? "AIR" : item.getType().name();
                    grid4x4Debug.append(String.format("[%2d:%8s] ", index, content));
                }
                grid4x4Debug.append("\n");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, grid4x4Debug.toString());
            
            // Show which slots will be extracted
            StringBuilder extractionMap = new StringBuilder("[4x4 Debug] Extraction mapping:\n");
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int index4x4 = (startRow + row) * 4 + (startCol + col);
                    int index3x3 = row * 3 + col;
                    extractionMap.append(String.format("3x3[%d] <- 4x4[%2d] ", index3x3, index4x4));
                }
                extractionMap.append("\n");
            }
            Main.getInstance().debugLog(DebugSystem.GUI, extractionMap.toString());
        }
    }

    /**
     * Debug method to test 4x4 to 3x3 position extraction
     */
    private void debugPositionExtraction(ItemStack[] grid4x4) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Position Debug] Testing all 3x3 extractions from 4x4 grid");
            
            for (int startRow = 0; startRow <= 1; startRow++) {
                for (int startCol = 0; startCol <= 1; startCol++) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Position Debug] === Extracting from position (" + startRow + "," + startCol + ") ===");
                    
                    // Show which 4x4 slots will be extracted
                    StringBuilder mapping = new StringBuilder("[Position Debug] 4x4 slots that will be extracted:\n");
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int index4x4 = (startRow + row) * 4 + (startCol + col);
                            int index3x3 = row * 3 + col;
                            ItemStack item = index4x4 < 16 ? grid4x4[index4x4] : null;
                            String content = item == null ? "NULL" : 
                                        item.getType() == Material.AIR ? "AIR" : item.getType().name();
                            mapping.append(String.format("4x4[%2d] -> 3x3[%d] = %s\n", index4x4, index3x3, content));
                        }
                    }
                    Main.getInstance().debugLog(DebugSystem.GUI, mapping.toString());
                    
                    // Extract and show result
                    ItemStack[] extracted = extract3x3FromPosition(grid4x4, startRow, startCol);
                    StringBuilder result = new StringBuilder("[Position Debug] Extracted 3x3 result: ");
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = extracted[i];
                        String content = item == null ? "NULL" : 
                                    item.getType() == Material.AIR ? "AIR" : item.getType().name();
                        result.append("[").append(i).append(":").append(content).append("] ");
                    }
                    Main.getInstance().debugLog(DebugSystem.GUI, result.toString());
                }
            }
        }
    }

}