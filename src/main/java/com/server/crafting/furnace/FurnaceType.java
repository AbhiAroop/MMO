package com.server.crafting.furnace;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Defines all custom furnace types with their characteristics
 * Step 1: Furnace type system
 */
public enum FurnaceType {
    // Basic tier furnaces
    STONE_FURNACE(
        "Stone Furnace", 
        ChatColor.GRAY, 
        Material.FURNACE,
        50, 400, 450,  // min, max, explosion temp
        1, 1, 1,       // input, fuel, output slots
        "A basic stone furnace for simple smelting"
    ),
    
    CLAY_KILN(
        "Clay Kiln", 
        ChatColor.GOLD, 
        Material.FURNACE,
        100, 600, 700,
        2, 1, 2,
        "An improved kiln with better heat retention"
    ),
    
    // Intermediate tier furnaces
    IRON_FORGE(
        "Iron Forge", 
        ChatColor.WHITE, 
        Material.FURNACE,
        200, 1000, 1200,
        2, 2, 2,
        "A reinforced forge capable of higher temperatures"
    ),
    
    STEEL_FURNACE(
        "Steel Furnace", 
        ChatColor.DARK_GRAY, 
        Material.FURNACE,
        300, 1400, 1600,
        3, 2, 3,
        "Advanced steel construction for demanding smelting"
    ),
    
    // Advanced tier furnaces
    MAGMATIC_FORGE(
        "Magmatic Forge", 
        ChatColor.RED, 
        Material.FURNACE,
        500, 2000, 2300,
        3, 3, 3,
        "Harnesses magmatic energy for extreme temperatures"
    ),
    
    ARCANE_CRUCIBLE(
        "Arcane Crucible", 
        ChatColor.DARK_PURPLE, 
        Material.FURNACE,
        800, 2500, 2800,
        4, 3, 4,
        "Mystical crucible infused with arcane energies"
    ),
    
    // Specialized furnaces
    VOID_EXTRACTOR(
        "Void Extractor", 
        ChatColor.DARK_AQUA, 
        Material.FURNACE,
        -50, 100, 150,  // Operates at low temperatures
        2, 2, 1,
        "Extracts essences using sub-zero processing"
    );
    
    private final String displayName;
    private final ChatColor nameColor;
    private final Material blockMaterial;
    private final int minTemperature;
    private final int maxTemperature;
    private final int explosionTemperature;
    private final int inputSlots;
    private final int fuelSlots;
    private final int outputSlots;
    private final String description;
    
    FurnaceType(String displayName, ChatColor nameColor, Material blockMaterial,
                int minTemperature, int maxTemperature, int explosionTemperature,
                int inputSlots, int fuelSlots, int outputSlots, String description) {
        this.displayName = displayName;
        this.nameColor = nameColor;
        this.blockMaterial = blockMaterial;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.explosionTemperature = explosionTemperature;
        this.inputSlots = inputSlots;
        this.fuelSlots = fuelSlots;
        this.outputSlots = outputSlots;
        this.description = description;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public ChatColor getNameColor() { return nameColor; }
    public String getColoredName() { return nameColor + displayName; }
    public Material getBlockMaterial() { return blockMaterial; }
    public int getMinTemperature() { return minTemperature; }
    public int getMaxTemperature() { return maxTemperature; }
    public int getExplosionTemperature() { return explosionTemperature; }
    public int getInputSlots() { return inputSlots; }
    public int getFuelSlots() { return fuelSlots; }
    public int getOutputSlots() { return outputSlots; }
    public String getDescription() { return description; }
    
    /**
     * Get operating temperature range as formatted string
     */
    public String getTemperatureRange() {
        return ChatColor.GRAY + "Operating Range: " + 
               ChatColor.AQUA + minTemperature + "°T" + ChatColor.GRAY + " - " + 
               ChatColor.GOLD + maxTemperature + "°T";
    }
    
    /**
     * Get total slot count for GUI sizing
     */
    public int getTotalSlots() {
        return inputSlots + fuelSlots + outputSlots;
    }
    
    /**
     * Check if temperature is within safe operating range
     */
    public boolean isTemperatureSafe(int temperature) {
        return temperature >= minTemperature && temperature <= maxTemperature;
    }
    
    /**
     * Check if temperature will cause explosion
     */
    public boolean willExplode(int temperature) {
        return temperature >= explosionTemperature;
    }
    
    /**
     * Get furnace type by display name (case insensitive)
     */
    public static FurnaceType getByName(String name) {
        for (FurnaceType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}