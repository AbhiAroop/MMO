package com.server.profiles.skills.skills.fishing.loot;

/**
 * Rarity tiers for fish loot
 */
public enum FishRarity {
    COMMON("§f", "Common", 1.0, 50.0),
    UNCOMMON("§a", "Uncommon", 1.5, 30.0),
    RARE("§9", "Rare", 2.0, 15.0),
    EPIC("§5", "Epic", 3.0, 4.0),
    LEGENDARY("§6", "Legendary", 5.0, 0.9),
    MYTHIC("§c", "Mythic", 10.0, 0.1);
    
    private final String colorCode;
    private final String displayName;
    private final double valueMultiplier;
    private final double dropChance; // Base drop chance percentage
    
    FishRarity(String colorCode, String displayName, double valueMultiplier, double dropChance) {
        this.colorCode = colorCode;
        this.displayName = displayName;
        this.valueMultiplier = valueMultiplier;
        this.dropChance = dropChance;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColoredName() {
        return colorCode + displayName;
    }
    
    public double getValueMultiplier() {
        return valueMultiplier;
    }
    
    public double getDropChance() {
        return dropChance;
    }
    
    /**
     * Get a random rarity based on drop chances and treasure bonus
     */
    public static FishRarity getRandomRarity(double treasureBonus) {
        double roll = Math.random() * 100.0;
        double cumulativeChance = 0.0;
        
        // Start from highest rarity (treasure bonus improves chances)
        for (FishRarity rarity : values()) {
            double adjustedChance = rarity.getDropChance() * (1.0 + (treasureBonus / 100.0));
            cumulativeChance += adjustedChance;
            
            if (roll <= cumulativeChance) {
                return rarity;
            }
        }
        
        return COMMON; // Fallback
    }
}
