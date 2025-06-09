package com.server.crafting.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Registry for all furnace recipes with temperature-based processing
 * Step 4: Recipe management system
 */
public class FurnaceRecipeRegistry {
    
    private static FurnaceRecipeRegistry instance;
    private final Map<String, FurnaceRecipe> recipes;
    private final Map<String, List<FurnaceRecipe>> inputLookup; // Fast lookup by input key
    
    private FurnaceRecipeRegistry() {
        this.recipes = new ConcurrentHashMap<>();
        this.inputLookup = new ConcurrentHashMap<>();
        initializeVanillaRecipes();
        initializeCustomRecipes();
    }
    
    public static FurnaceRecipeRegistry getInstance() {
        if (instance == null) {
            instance = new FurnaceRecipeRegistry();
        }
        return instance;
    }
    
    /**
     * Initialize vanilla smelting recipes with temperature requirements
     * Step 4: Vanilla recipe integration
     */
    private void initializeVanillaRecipes() {
        // Basic smelting (low temperature)
        registerVanillaSmeltingRecipe("iron_ingot", Material.RAW_IRON, Material.IRON_INGOT, 300, 200);
        registerVanillaSmeltingRecipe("gold_ingot", Material.RAW_GOLD, Material.GOLD_INGOT, 400, 200);
        registerVanillaSmeltingRecipe("copper_ingot", Material.RAW_COPPER, Material.COPPER_INGOT, 250, 200);
        
        // Stone processing
        registerVanillaSmeltingRecipe("stone", Material.COBBLESTONE, Material.STONE, 150, 200);
        registerVanillaSmeltingRecipe("smooth_stone", Material.STONE, Material.SMOOTH_STONE, 200, 400);
        registerVanillaSmeltingRecipe("glass", Material.SAND, Material.GLASS, 300, 200);
        
        // Food processing
        registerVanillaSmeltingRecipe("cooked_beef", Material.BEEF, Material.COOKED_BEEF, 100, 200);
        registerVanillaSmeltingRecipe("cooked_pork", Material.PORKCHOP, Material.COOKED_PORKCHOP, 100, 200);
        registerVanillaSmeltingRecipe("baked_potato", Material.POTATO, Material.BAKED_POTATO, 80, 200);
        
        // High temperature processing
        registerVanillaSmeltingRecipe("netherite_scrap", Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP, 1500, 800);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Recipe Registry] Loaded " + recipes.size() + " vanilla furnace recipes");
        }
    }
    
    /**
     * Initialize custom recipes with advanced processing
     * Step 4: Custom recipe system
     */
    private void initializeCustomRecipes() {
        // Copper processing chain
        registerCopperProcessingRecipes();
        
        // Advanced alloy recipes
        registerAlloyRecipes();
        
        // Crystal processing recipes
        registerCrystalRecipes();
        
        // Magical processing recipes
        registerMagicalRecipes();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Recipe Registry] Loaded " + (recipes.size()) + " total furnace recipes");
        }
    }
    
    /**
     * Register copper processing recipes
     * Step 4: Copper alloy system
     */
    private void registerCopperProcessingRecipes() {
        // Refined Copper (higher purity)
        List<ItemStack> refinedCopperInputs = Arrays.asList(
            new ItemStack(Material.COPPER_INGOT, 3),
            new ItemStack(Material.COAL, 1)
        );
        List<ItemStack> refinedCopperOutputs = Arrays.asList(
            new ItemStack(Material.COPPER_INGOT, 2), // Refined copper (same material, better quality)
            new ItemStack(Material.CHARCOAL, 1) // Byproduct
        );
        
        FurnaceRecipe refinedCopper = new FurnaceRecipe(
            "refined_copper",
            refinedCopperInputs,
            refinedCopperOutputs,
            600, // Required temperature
            600, // Cook time (30 seconds)
            FurnaceRecipe.RecipeType.REFINING,
            "Refined Copper",
            "Purify copper ingots for higher quality alloys"
        );
        registerRecipe(refinedCopper);
        
        // Bronze Alloy
        List<ItemStack> bronzeInputs = Arrays.asList(
            new ItemStack(Material.COPPER_INGOT, 8),
            new ItemStack(Material.IRON_INGOT, 2)
        );
        List<ItemStack> bronzeOutputs = Arrays.asList(
            new ItemStack(Material.GOLD_INGOT, 5) // Using gold as bronze placeholder
        );
        
        FurnaceRecipe bronzeAlloy = new FurnaceRecipe(
            "bronze_alloy",
            bronzeInputs,
            bronzeOutputs,
            800, // Higher temperature for alloying
            800, // 40 seconds
            FurnaceRecipe.RecipeType.ALLOYING,
            "Bronze Alloy",
            "Combine copper and iron to create bronze"
        );
        registerRecipe(bronzeAlloy);
    }
    
    /**
     * Register advanced alloy recipes
     * Step 4: Advanced metallurgy
     */
    private void registerAlloyRecipes() {
        // Steel Production
        List<ItemStack> steelInputs = Arrays.asList(
            new ItemStack(Material.IRON_INGOT, 4),
            new ItemStack(Material.COAL_BLOCK, 1)
        );
        List<ItemStack> steelOutputs = Arrays.asList(
            new ItemStack(Material.NETHERITE_INGOT, 2) // Using netherite as steel placeholder
        );
        
        FurnaceRecipe steelProduction = new FurnaceRecipe(
            "steel_production",
            steelInputs,
            steelOutputs,
            1200, // Very high temperature
            1200, // 60 seconds
            FurnaceRecipe.RecipeType.ALLOYING,
            "Steel Production",
            "Forge superior steel from iron and carbon"
        );
        registerRecipe(steelProduction);
        
        // Electrum (Gold-Silver alloy)
        List<ItemStack> electrumInputs = Arrays.asList(
            new ItemStack(Material.GOLD_INGOT, 3),
            new ItemStack(Material.IRON_INGOT, 2) // Using iron as silver placeholder
        );
        List<ItemStack> electrumOutputs = Arrays.asList(
            new ItemStack(Material.GOLD_INGOT, 4) // Enhanced gold
        );
        
        FurnaceRecipe electrumAlloy = new FurnaceRecipe(
            "electrum_alloy",
            electrumInputs,
            electrumOutputs,
            900,
            600,
            FurnaceRecipe.RecipeType.ALLOYING,
            "Electrum Alloy",
            "Blend gold with silver for magical conductivity"
        );
        registerRecipe(electrumAlloy);
    }
    
    /**
     * Register crystal processing recipes
     * Step 4: Crystal system
     */
    private void registerCrystalRecipes() {
        // Quartz Purification
        List<ItemStack> quartzInputs = Arrays.asList(
            new ItemStack(Material.QUARTZ, 4),
            new ItemStack(Material.REDSTONE, 2)
        );
        List<ItemStack> quartzOutputs = Arrays.asList(
            new ItemStack(Material.QUARTZ, 6), // More quartz output
            new ItemStack(Material.GLOWSTONE_DUST, 1) // Magical byproduct
        );
        
        FurnaceRecipe quartzPurification = new FurnaceRecipe(
            "quartz_purification",
            quartzInputs,
            quartzOutputs,
            400,
            400,
            FurnaceRecipe.RecipeType.CRYSTALLIZATION,
            "Quartz Purification",
            "Enhance quartz crystals with redstone energy"
        );
        registerRecipe(quartzPurification);
        
        // Diamond Synthesis
        List<ItemStack> diamondInputs = Arrays.asList(
            new ItemStack(Material.COAL_BLOCK, 8),
            new ItemStack(Material.LAPIS_LAZULI, 4)
        );
        List<ItemStack> diamondOutputs = Arrays.asList(
            new ItemStack(Material.DIAMOND, 1)
        );
        
        FurnaceRecipe diamondSynthesis = new FurnaceRecipe(
            "diamond_synthesis",
            diamondInputs,
            diamondOutputs,
            2000, // Extreme temperature
            1600, // 80 seconds
            FurnaceRecipe.RecipeType.CRYSTALLIZATION,
            "Diamond Synthesis",
            "Crystallize carbon under extreme heat and pressure"
        );
        registerRecipe(diamondSynthesis);
    }
    
    /**
     * Register magical processing recipes
     * Step 4: Magical furnace recipes
     */
    private void registerMagicalRecipes() {
        // Essence Extraction
        List<ItemStack> essenceInputs = Arrays.asList(
            new ItemStack(Material.ENDER_PEARL, 2),
            new ItemStack(Material.BLAZE_POWDER, 3)
        );
        List<ItemStack> essenceOutputs = Arrays.asList(
            new ItemStack(Material.EXPERIENCE_BOTTLE, 4)
        );
        
        FurnaceRecipe essenceExtraction = new FurnaceRecipe(
            "essence_extraction",
            essenceInputs,
            essenceOutputs,
            1000,
            800,
            FurnaceRecipe.RecipeType.EXTRACTION,
            "Essence Extraction",
            "Extract magical essence from enchanted materials"
        );
        registerRecipe(essenceExtraction);
        
        // Void Processing (low temperature, special furnace only)
        List<ItemStack> voidInputs = Arrays.asList(
            new ItemStack(Material.OBSIDIAN, 1),
            new ItemStack(Material.WATER_BUCKET, 1)
        );
        List<ItemStack> voidOutputs = Arrays.asList(
            new ItemStack(Material.END_STONE, 2),
            new ItemStack(Material.BUCKET, 1)
        );
        
        FurnaceRecipe voidProcessing = new FurnaceRecipe(
            "void_processing",
            voidInputs,
            voidOutputs,
            50, // Very low temperature
            1000,
            FurnaceRecipe.RecipeType.TRANSMUTATION,
            "Void Processing",
            "Transform matter using void energies"
        );
        registerRecipe(voidProcessing);
    }
    
    /**
     * Register a vanilla smelting recipe with temperature
     */
    private void registerVanillaSmeltingRecipe(String id, Material input, Material output, 
                                             int temperature, int cookTime) {
        List<ItemStack> inputs = Arrays.asList(new ItemStack(input, 1));
        List<ItemStack> outputs = Arrays.asList(new ItemStack(output, 1));
        
        FurnaceRecipe recipe = new FurnaceRecipe(
            id,
            inputs,
            outputs,
            temperature,
            cookTime,
            FurnaceRecipe.RecipeType.SMELTING,
            output.name().replace("_", " "),
            "Smelt " + input.name().replace("_", " ") + " into " + output.name().replace("_", " ")
        );
        
        registerRecipe(recipe);
    }
    
    /**
     * Register a recipe with the registry
     * Step 4: Recipe registration
     */
    public void registerRecipe(FurnaceRecipe recipe) {
        recipes.put(recipe.getRecipeId(), recipe);
        
        // Add to input lookup for fast searching
        String inputKey = recipe.createInputKey();
        inputLookup.computeIfAbsent(inputKey, k -> new ArrayList<>()).add(recipe);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Recipe Registry] Registered recipe: " + recipe.getDisplayName() + 
                " (" + recipe.getFormattedTemperature() + ", " + recipe.getFormattedCookTime() + ")");
        }
    }
    
    /**
     * Find a recipe that matches the given inputs
     * Step 4: Recipe matching
     */
    public FurnaceRecipe findRecipe(List<ItemStack> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        
        // Try all registered recipes
        for (FurnaceRecipe recipe : recipes.values()) {
            if (recipe.matches(inputs)) {
                return recipe;
            }
        }
        
        return null;
    }
    
    /**
     * Get recipe by ID
     */
    public FurnaceRecipe getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }
    
    /**
     * Get all recipes
     */
    public Collection<FurnaceRecipe> getAllRecipes() {
        return recipes.values();
    }
    
    /**
     * Get recipes by type
     */
    public List<FurnaceRecipe> getRecipesByType(FurnaceRecipe.RecipeType type) {
        return recipes.values().stream()
            .filter(recipe -> recipe.getRecipeType() == type)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get recipes that can be processed at a given temperature
     */
    public List<FurnaceRecipe> getRecipesForTemperature(int temperature) {
        return recipes.values().stream()
            .filter(recipe -> recipe.getRequiredTemperature() <= temperature)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Remove a recipe (for admin commands)
     */
    public boolean removeRecipe(String recipeId) {
        FurnaceRecipe removed = recipes.remove(recipeId);
        if (removed != null) {
            // Clean up input lookup
            String inputKey = removed.createInputKey();
            List<FurnaceRecipe> lookup = inputLookup.get(inputKey);
            if (lookup != null) {
                lookup.remove(removed);
                if (lookup.isEmpty()) {
                    inputLookup.remove(inputKey);
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Check if inputs can make any recipe
     */
    public boolean hasValidRecipe(List<ItemStack> inputs) {
        return findRecipe(inputs) != null;
    }
}