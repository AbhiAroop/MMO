package com.server.crafting.temperature;

import org.bukkit.ChatColor;

/**
 * Core temperature system for furnaces
 * Step 1: Temperature infrastructure
 */
public class TemperatureSystem {
    
    // Temperature constants
    public static final int ROOM_TEMPERATURE = 20;
    public static final int MIN_TEMPERATURE = -100;
    public static final int MAX_SAFE_TEMPERATURE = 3000;
    public static final int EXPLOSION_THRESHOLD = 3500;
    
    // Temperature zones for display
    public enum TemperatureZone {
        FROZEN("Frozen", ChatColor.AQUA, -100, 0),
        COLD("Cold", ChatColor.BLUE, 0, 100),
        COOL("Cool", ChatColor.GRAY, 100, 200),
        WARM("Warm", ChatColor.YELLOW, 200, 400),
        HOT("Hot", ChatColor.GOLD, 400, 800),
        VERY_HOT("Very Hot", ChatColor.RED, 800, 1500),
        EXTREME("Extreme", ChatColor.DARK_RED, 1500, 2500),
        MOLTEN("Molten", ChatColor.DARK_PURPLE, 2500, 3000),
        CRITICAL("Critical", ChatColor.DARK_RED, 3000, Integer.MAX_VALUE);
        
        private final String displayName;
        private final ChatColor color;
        private final int minTemp;
        private final int maxTemp;
        
        TemperatureZone(String displayName, ChatColor color, int minTemp, int maxTemp) {
            this.displayName = displayName;
            this.color = color;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
        }
        
        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public int getMinTemp() { return minTemp; }
        public int getMaxTemp() { return maxTemp; }
        
        public static TemperatureZone getZone(int temperature) {
            for (TemperatureZone zone : values()) {
                if (temperature >= zone.minTemp && temperature < zone.maxTemp) {
                    return zone;
                }
            }
            return CRITICAL; // Fallback for extreme temperatures
        }
    }
    
    /**
     * Format temperature for display
     * 
     * @param temperature The temperature value
     * @return Formatted temperature string with color and zone
     */
    public static String formatTemperature(int temperature) {
        TemperatureZone zone = TemperatureZone.getZone(temperature);
        return zone.getColor().toString() + temperature + "°T " + ChatColor.GRAY + "(" + zone.getDisplayName() + ")";
    }
    
    /**
     * Get temperature efficiency multiplier for cooking
     * Higher temperatures = faster cooking (up to optimal point)
     * 
     * @param actualTemp Current furnace temperature
     * @param requiredTemp Required temperature for recipe
     * @return Efficiency multiplier (1.0 = normal speed, 2.0 = double speed, etc.)
     */
    public static double getTemperatureEfficiency(int actualTemp, int requiredTemp) {
        if (actualTemp < requiredTemp) {
            return 0.0; // Cannot cook below required temperature
        }
        
        // Calculate efficiency boost
        int tempDifference = actualTemp - requiredTemp;
        
        // Linear efficiency increase up to 200% efficiency
        // Every 100 degrees above required gives 10% boost, max 100% boost
        double efficiency = 1.0 + Math.min(tempDifference / 100.0 * 0.1, 1.0);
        
        return efficiency;
    }
    
    /**
     * Check if temperature is in safe operating range for furnace
     * 
     * @param temperature Current temperature
     * @param maxSafe Maximum safe temperature for furnace type
     * @return true if safe, false if dangerous
     */
    public static boolean isSafeTemperature(int temperature, int maxSafe) {
        return temperature <= maxSafe;
    }
    
    /**
     * Calculate temperature decay rate when no fuel is burning
     * 
     * @param currentTemp Current temperature
     * @return Temperature reduction per tick
     */
    public static int getTemperatureDecay(int currentTemp) {
        if (currentTemp <= ROOM_TEMPERATURE) {
            return 0; // No decay at room temperature
        }
        
        // Higher temperatures cool down faster
        if (currentTemp > 1000) {
            return 3; // Fast cooling for very hot furnaces
        } else if (currentTemp > 500) {
            return 2; // Medium cooling
        } else {
            return 1; // Slow cooling for lower temperatures
        }
    }
}