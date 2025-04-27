package com.server.profiles.skills.core;

/**
 * Enum of all parent skills in the system
 */
public enum SkillType {
    MINING("Mining", "Break blocks and mine ores to level up"),
    EXCAVATING("Excavating", "Dig soil and gravel to find treasures"),
    FISHING("Fishing", "Catch fish and aquatic treasures"),
    FARMING("Farming", "Grow crops and raise animals"),
    COMBAT("Combat", "Defeat enemies and monsters");
    
    private final String displayName;
    private final String description;
    
    SkillType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getId() {
        return name().toLowerCase();
    }
}