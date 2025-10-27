package com.server.profiles.skills.skills.fishing.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.server.profiles.skills.skills.fishing.types.FishingType;

/**
 * Different types of fish that can be caught
 */
public enum FishType {
    // Normal Water Fish
    COD("Cod", Material.COD, 30.0, 10, FishingType.NORMAL_WATER),
    SALMON("Salmon", Material.SALMON, 50.0, 15, FishingType.NORMAL_WATER),
    TROPICAL_FISH("Tropical Fish", Material.TROPICAL_FISH, 10.0, 20, FishingType.NORMAL_WATER),
    PUFFERFISH("Pufferfish", Material.PUFFERFISH, 15.0, 25, FishingType.NORMAL_WATER),
    
    // Ice Fishing
    FROZEN_COD("Frozen Cod", Material.COD, 40.0, 30, FishingType.ICE),
    FROZEN_SALMON("Frozen Salmon", Material.SALMON, 60.0, 40, FishingType.ICE),
    ICE_PIKE("Ice Pike", Material.COD, 80.0, 60, FishingType.ICE),
    FROST_TROUT("Frost Trout", Material.SALMON, 70.0, 50, FishingType.ICE),
    
    // Lava Fishing
    MAGMA_FISH("Magma Fish", Material.TROPICAL_FISH, 100.0, 100, FishingType.LAVA),
    FIRE_BASS("Fire Bass", Material.SALMON, 120.0, 150, FishingType.LAVA),
    MOLTEN_GROUPER("Molten Grouper", Material.COD, 150.0, 200, FishingType.LAVA),
    BLAZEFIN("Blazefin", Material.PUFFERFISH, 200.0, 300, FishingType.LAVA),
    
    // Void Fishing
    VOID_ANGLER("Void Angler", Material.COD, 250.0, 500, FishingType.VOID),
    ELDRITCH_EEL("Eldritch Eel", Material.SALMON, 300.0, 750, FishingType.VOID),
    ABYSS_DWELLER("Abyss Dweller", Material.TROPICAL_FISH, 400.0, 1000, FishingType.VOID),
    DIMENSION_LEVIATHAN("Dimension Leviathan", Material.PUFFERFISH, 500.0, 2000, FishingType.VOID);
    
    private final String displayName;
    private final Material material;
    private final double baseSize; // In centimeters
    private final int baseValue; // Base coin value
    private final FishingType fishingType;
    
    FishType(String displayName, Material material, double baseSize, int baseValue, FishingType fishingType) {
        this.displayName = displayName;
        this.material = material;
        this.baseSize = baseSize;
        this.baseValue = baseValue;
        this.fishingType = fishingType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public double getBaseSize() {
        return baseSize;
    }
    
    public int getBaseValue() {
        return baseValue;
    }
    
    public FishingType getFishingType() {
        return fishingType;
    }
    
    /**
     * Get a random fish type for the given fishing environment
     */
    public static FishType getRandomFish(FishingType fishingType) {
        List<FishType> validFish = new ArrayList<>();
        
        for (FishType fish : values()) {
            if (fish.getFishingType() == fishingType) {
                validFish.add(fish);
            }
        }
        
        if (validFish.isEmpty()) {
            return COD; // Fallback
        }
        
        return validFish.get((int) (Math.random() * validFish.size()));
    }
    
    /**
     * Get all fish for a specific fishing type
     */
    public static List<FishType> getFishForType(FishingType fishingType) {
        List<FishType> result = new ArrayList<>();
        
        for (FishType fish : values()) {
            if (fish.getFishingType() == fishingType) {
                result.add(fish);
            }
        }
        
        return result;
    }
}
