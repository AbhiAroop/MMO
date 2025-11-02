package com.server.profiles.skills.skills.farming.botany;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.server.profiles.skills.skills.farming.botany.CustomCrop.BreedingRecipe;
import com.server.profiles.skills.skills.farming.botany.CustomCrop.CropRarity;

/**
 * Registry for all custom crops
 * Manages custom crop definitions and provides lookup functionality
 */
public class CustomCropRegistry {
    
    private static CustomCropRegistry instance;
    
    private final Map<String, CustomCrop> cropsById = new HashMap<>();
    private final Map<Integer, CustomCrop> cropsByCustomModelData = new HashMap<>();
    
    private CustomCropRegistry() {
        registerDefaultCrops();
    }
    
    public static CustomCropRegistry getInstance() {
        if (instance == null) {
            instance = new CustomCropRegistry();
        }
        return instance;
    }
    
    /**
     * Register a custom crop
     */
    public void registerCrop(CustomCrop crop) {
        cropsById.put(crop.getId(), crop);
        
        // Register all growth stages for quick lookup
        for (int stage = 0; stage < crop.getMaxGrowthStages(); stage++) {
            int customModelData = crop.getCustomModelData(stage);
            cropsByCustomModelData.put(customModelData, crop);
        }
    }
    
    /**
     * Get a custom crop by its ID
     */
    public CustomCrop getCrop(String id) {
        return cropsById.get(id);
    }
    
    /**
     * Get a custom crop by its seed's custom model data
     */
    public CustomCrop getCropByCustomModelData(int customModelData) {
        return cropsByCustomModelData.get(customModelData);
    }
    
    /**
     * Get a custom crop from an ItemStack seed
     */
    public CustomCrop getCropFromSeed(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return null;
        }
        
        int customModelData = item.getItemMeta().getCustomModelData();
        return getCropByCustomModelData(customModelData);
    }
    
    /**
     * Get all registered custom crops
     */
    public List<CustomCrop> getAllCrops() {
        return new ArrayList<>(cropsById.values());
    }
    
    /**
     * Get crops by rarity
     */
    public List<CustomCrop> getCropsByRarity(CropRarity rarity) {
        List<CustomCrop> result = new ArrayList<>();
        for (CustomCrop crop : cropsById.values()) {
            if (crop.getRarity() == rarity) {
                result.add(crop);
            }
        }
        return result;
    }
    
    /**
     * Find possible breeding results for two parent crops
     */
    public List<CustomCrop> findBreedingResults(String parentCrop1Id, String parentCrop2Id) {
        List<CustomCrop> results = new ArrayList<>();
        
        for (CustomCrop crop : cropsById.values()) {
            for (BreedingRecipe recipe : crop.getRecipes()) {
                if (recipe.matches(parentCrop1Id, parentCrop2Id)) {
                    results.add(crop);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Register default custom crops
     * This creates the initial set of custom crops available in the game
     */
    private void registerDefaultCrops() {
        // COMMON CROPS - Easy to breed, low requirements
        
        // Golden Wheat - First custom crop (Wheat + Wheat with gold)
        CustomCrop goldenWheat = new CustomCrop(
            "golden_wheat",
            "Golden Wheat",
            CropRarity.COMMON,
            4, // 4 growth stages
            600L, // 30 seconds per stage (2 minutes total)
            new int[]{100001, 100002, 100003, 100004}, // Custom model data
            "crops/golden_wheat",
            1, // Requires Botany level 1
            new ItemStack(Material.WHEAT, 1),
            2, 4, // 2-4 drops
            0.15, // 15% seed drop chance
            10.0, // Plant XP
            15.0, // Harvest XP
            25.0  // Breed XP
        );
        registerCrop(goldenWheat);
        
        // UNCOMMON CROPS
        
        // Crimson Carrot - Nether-infused carrot
        CustomCrop crimsonCarrot = new CustomCrop(
            "crimson_carrot",
            "Crimson Carrot",
            CropRarity.UNCOMMON,
            4,
            800L, // 40 seconds per stage
            new int[]{100011, 100012, 100013, 100014},
            "crops/crimson_carrot",
            5, // Requires Botany level 5
            new ItemStack(Material.CARROT, 1),
            3, 5,
            0.12,
            20.0,
            30.0,
            50.0
        );
        crimsonCarrot.addBreedingRecipe(new BreedingRecipe(
            "golden_wheat", "golden_wheat", 0.60, 5
        ));
        registerCrop(crimsonCarrot);
        
        // Moonpetal - Mystical flower that blooms under moonlight
        // Create custom moonpetal drop item
        ItemStack moonpetalDrop = new ItemStack(Material.ALLIUM, 1);
        org.bukkit.inventory.meta.ItemMeta moonpetalMeta = moonpetalDrop.getItemMeta();
        if (moonpetalMeta != null) {
            moonpetalMeta.setDisplayName("§aMoonpetal");
            moonpetalMeta.setCustomModelData(100065); // moonpetal.png
            moonpetalDrop.setItemMeta(moonpetalMeta);
        }
        
        CustomCrop moonpetal = new CustomCrop(
            "moonpetal",
            "Moonpetal",
            CropRarity.UNCOMMON,
            4,
            800L, // 40 seconds per stage
            // Index 0 = seed (100060), indices 1-4 = growth stages (100061-100064)
            new int[]{100060, 100061, 100062, 100063, 100064},
            "crops/moonpetal",
            5, // Requires Botany level 5
            moonpetalDrop, // Custom drop item with CMD 100065
            3, 5,
            0.12,
            20.0,
            30.0,
            50.0
        );
        moonpetal.addBreedingRecipe(new BreedingRecipe(
            "golden_wheat", "golden_wheat", 0.60, 5
        ));
        registerCrop(moonpetal);
        
        // RARE CROPS
        
        // Ender Berry - Mysterious berry with ender properties
        CustomCrop enderBerry = new CustomCrop(
            "ender_berry",
            "Ender Berry",
            CropRarity.RARE,
            5,
            1000L, // 50 seconds per stage
            new int[]{100021, 100022, 100023, 100024, 100025},
            "crops/ender_berry",
            15, // Requires Botany level 15
            new ItemStack(Material.SWEET_BERRIES, 1),
            2, 4,
            0.10,
            40.0,
            60.0,
            100.0
        );
        enderBerry.addBreedingRecipe(new BreedingRecipe(
            "crimson_carrot", "crimson_carrot", 0.40, 15
        ));
        registerCrop(enderBerry);
        
        // EPIC CROPS
        
        // Crystal Melon - Glowing melon with crystal properties
        CustomCrop crystalMelon = new CustomCrop(
            "crystal_melon",
            "Crystal Melon",
            CropRarity.EPIC,
            6,
            1200L, // 60 seconds per stage (6 minutes total)
            new int[]{100031, 100032, 100033, 100034, 100035, 100036},
            "crops/crystal_melon",
            30, // Requires Botany level 30
            new ItemStack(Material.MELON_SLICE, 1),
            4, 6,
            0.08,
            80.0,
            120.0,
            200.0
        );
        crystalMelon.addBreedingRecipe(new BreedingRecipe(
            "ender_berry", "crimson_carrot", 0.30, 30
        ));
        registerCrop(crystalMelon);
        
        // LEGENDARY CROPS
        
        // Celestial Potato - Divine potato from the heavens
        CustomCrop celestialPotato = new CustomCrop(
            "celestial_potato",
            "Celestial Potato",
            CropRarity.LEGENDARY,
            7,
            1600L, // 80 seconds per stage (~9 minutes total)
            new int[]{100041, 100042, 100043, 100044, 100045, 100046, 100047},
            "crops/celestial_potato",
            50, // Requires Botany level 50
            new ItemStack(Material.POTATO, 1),
            3, 5,
            0.05,
            150.0,
            250.0,
            500.0
        );
        celestialPotato.addBreedingRecipe(new BreedingRecipe(
            "crystal_melon", "ender_berry", 0.20, 50
        ));
        registerCrop(celestialPotato);
        
        // MYTHIC CROPS
        
        // Void Pumpkin - Ultimate crop, absorbs void energy
        CustomCrop voidPumpkin = new CustomCrop(
            "void_pumpkin",
            "Void Pumpkin",
            CropRarity.MYTHIC,
            8,
            2000L, // 100 seconds per stage (~13 minutes total)
            new int[]{100051, 100052, 100053, 100054, 100055, 100056, 100057, 100058},
            "crops/void_pumpkin",
            75, // Requires Botany level 75
            new ItemStack(Material.PUMPKIN, 1),
            2, 4,
            0.02, // Very rare seed drop
            300.0,
            500.0,
            1000.0
        );
        voidPumpkin.addBreedingRecipe(new BreedingRecipe(
            "celestial_potato", "crystal_melon", 0.10, 75
        ));
        registerCrop(voidPumpkin);
    }
}
