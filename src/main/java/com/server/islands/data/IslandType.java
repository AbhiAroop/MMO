package com.server.islands.data;

/**
 * Enum representing the different types of islands available.
 */
public enum IslandType {
    
    SKY("Sky Island", 10000, "A floating island high in the sky with endless views"),
    OCEAN("Ocean Island", 15000, "An island surrounded by crystal clear ocean waters"),
    FOREST("Forest Island", 12000, "A lush island covered in dense vegetation");
    
    private final String displayName;
    private final int cost;
    private final String description;
    
    IslandType(String displayName, int cost, String description) {
        this.displayName = displayName;
        this.cost = cost;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getCost() {
        return cost;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get island type from string name.
     */
    public static IslandType fromString(String name) {
        for (IslandType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
