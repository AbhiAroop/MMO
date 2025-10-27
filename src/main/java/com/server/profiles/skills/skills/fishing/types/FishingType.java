package com.server.profiles.skills.skills.fishing.types;

import org.bukkit.Material;

/**
 * Types of fishing environments with unique mechanics and loot tables
 */
public enum FishingType {
    NORMAL_WATER(
        "Normal Water",
        "§9",
        "Standard fishing in water bodies",
        Material.WATER,
        1.0,
        false
    ),
    
    ICE(
        "Ice Fishing",
        "§b",
        "Fish through frozen lakes with freeze line mechanics",
        Material.ICE,
        1.5,
        true
    ),
    
    LAVA(
        "Lava Fishing",
        "§6",
        "Fish in lava for magma fish and fire-resistant treasures",
        Material.LAVA,
        2.0,
        true
    ),
    
    VOID(
        "Void Fishing",
        "§5",
        "Fish in the void for eldritch entities and dimensional artifacts",
        Material.AIR,
        3.0,
        true
    );
    
    private final String displayName;
    private final String colorCode;
    private final String description;
    private final Material associatedMaterial;
    private final double difficultyMultiplier;
    private final boolean requiresUnlock;
    
    FishingType(String displayName, String colorCode, String description, 
                Material associatedMaterial, double difficultyMultiplier, boolean requiresUnlock) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
        this.associatedMaterial = associatedMaterial;
        this.difficultyMultiplier = difficultyMultiplier;
        this.requiresUnlock = requiresUnlock;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public String getColoredName() {
        return colorCode + displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getAssociatedMaterial() {
        return associatedMaterial;
    }
    
    public double getDifficultyMultiplier() {
        return difficultyMultiplier;
    }
    
    public boolean requiresUnlock() {
        return requiresUnlock;
    }
    
    /**
     * Get the skill tree node ID required to unlock this fishing type
     */
    public String getUnlockNodeId() {
        switch (this) {
            case ICE:
                return "unlock_ice_fishing";
            case LAVA:
                return "unlock_lava_fishing";
            case VOID:
                return "unlock_void_fishing";
            default:
                return null; // Normal water doesn't require unlock
        }
    }
}
