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
     * Get quality based on fishing accuracy with randomness
     * Harsh penalty for misses, making high quality very difficult to achieve
     */
    public static FishQuality fromAccuracy(double accuracy) {
        // Accuracy is percentage (0-100)
        // Lower accuracy = much higher chance of poor quality
        
        // Generate random roll (0-100)
        double roll = Math.random() * 100.0;
        
        // Quality thresholds based on accuracy
        // With perfect accuracy (100%), still only 25% chance for EXCELLENT
        // With any misses, quality drops dramatically
        
        if (accuracy >= 100.0) {
            // Perfect accuracy (no misses)
            // 5% PERFECT, 25% EXCELLENT, 40% GOOD, 25% NORMAL, 5% POOR
            if (roll < 5) return PERFECT;
            else if (roll < 30) return EXCELLENT;
            else if (roll < 70) return GOOD;
            else if (roll < 95) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 90.0) {
            // 1 miss in 5 rounds (90% accuracy)
            // 1% PERFECT, 15% EXCELLENT, 34% GOOD, 40% NORMAL, 10% POOR
            if (roll < 1) return PERFECT;
            else if (roll < 16) return EXCELLENT;
            else if (roll < 50) return GOOD;
            else if (roll < 90) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 80.0) {
            // 1 miss in 4-5 rounds (80-90% accuracy)
            // 10% EXCELLENT, 25% GOOD, 45% NORMAL, 20% POOR
            if (roll < 10) return EXCELLENT;
            else if (roll < 35) return GOOD;
            else if (roll < 80) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 70.0) {
            // 70-80% accuracy
            // 5% EXCELLENT, 20% GOOD, 40% NORMAL, 35% POOR
            if (roll < 5) return EXCELLENT;
            else if (roll < 25) return GOOD;
            else if (roll < 65) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 60.0) {
            // 60-70% accuracy (2 misses in 5 rounds = 60%)
            // 15% GOOD, 35% NORMAL, 50% POOR
            if (roll < 15) return GOOD;
            else if (roll < 50) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 50.0) {
            // 50-60% accuracy
            // 5% GOOD, 25% NORMAL, 70% POOR
            if (roll < 5) return GOOD;
            else if (roll < 30) return NORMAL;
            else return POOR;
            
        } else if (accuracy >= 40.0) {
            // 40-50% accuracy (2 misses in 4 rounds = 50%)
            // 20% NORMAL, 80% POOR
            if (roll < 20) return NORMAL;
            else return POOR;
            
        } else {
            // Below 40% accuracy
            // 10% NORMAL, 90% POOR
            if (roll < 10) return NORMAL;
            else return POOR;
        }
    }
}
