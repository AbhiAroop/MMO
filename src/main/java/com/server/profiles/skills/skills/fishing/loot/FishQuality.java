package com.server.profiles.skills.skills.fishing.loot;

/**
 * Quality grades for fish based on minigame performance
 */
public enum FishQuality {
    POOR("§7", "Poor", 0.5, 0, 50),
    NORMAL("§f", "Normal", 1.0, 50, 70),
    GOOD("§a", "Good", 1.5, 70, 85),
    EXCELLENT("§b", "Excellent", 2.0, 85, 95),
    PERFECT("§6", "Perfect", 3.0, 95, 100);
    
    private final String colorCode;
    private final String displayName;
    private final double valueMultiplier;
    private final int minAccuracy; // Minimum accuracy percentage required
    private final int maxAccuracy; // Maximum accuracy percentage for this tier
    
    FishQuality(String colorCode, String displayName, double valueMultiplier, 
                int minAccuracy, int maxAccuracy) {
        this.colorCode = colorCode;
        this.displayName = displayName;
        this.valueMultiplier = valueMultiplier;
        this.minAccuracy = minAccuracy;
        this.maxAccuracy = maxAccuracy;
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
    
    public int getMinAccuracy() {
        return minAccuracy;
    }
    
    public int getMaxAccuracy() {
        return maxAccuracy;
    }
    
    /**
     * Get quality based on fishing accuracy
     */
    public static FishQuality fromAccuracy(double accuracy) {
        for (FishQuality quality : values()) {
            if (accuracy >= quality.getMinAccuracy() && accuracy <= quality.getMaxAccuracy()) {
                return quality;
            }
        }
        return NORMAL; // Fallback
    }
}
