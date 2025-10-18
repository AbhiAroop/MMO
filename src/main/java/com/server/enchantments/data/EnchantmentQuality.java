package com.server.enchantments.data;

import org.bukkit.ChatColor;

/**
 * Quality rating for enchantments affecting effectiveness and affinity
 * Poor → Common → Uncommon → Rare → Epic → Legendary → Godly
 */
public enum EnchantmentQuality {
    POOR("Poor", ChatColor.DARK_GRAY, 0.5, 10),
    COMMON("Common", ChatColor.WHITE, 0.7, 20),
    UNCOMMON("Uncommon", ChatColor.GREEN, 0.9, 35),
    RARE("Rare", ChatColor.BLUE, 1.1, 50),
    EPIC("Epic", ChatColor.DARK_PURPLE, 1.4, 70),
    LEGENDARY("Legendary", ChatColor.GOLD, 1.7, 90),
    GODLY("Godly", ChatColor.LIGHT_PURPLE, 2.0, 100);
    
    private final String displayName;
    private final ChatColor color;
    private final double effectivenessMultiplier;
    private final int affinityValue;
    
    EnchantmentQuality(String displayName, ChatColor color, double effectivenessMultiplier, int affinityValue) {
        this.displayName = displayName;
        this.color = color;
        this.effectivenessMultiplier = effectivenessMultiplier;
        this.affinityValue = affinityValue;
    }
    
    public String getDisplayName() {
        return color + displayName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Get the effectiveness multiplier (0.5x to 2.0x)
     * Scales duration, damage, proc chance, etc.
     */
    public double getEffectivenessMultiplier() {
        return effectivenessMultiplier;
    }
    
    /**
     * Get the affinity value (10 to 100)
     * Contributes to player's elemental affinity
     */
    public int getAffinityValue() {
        return affinityValue;
    }
    
    /**
     * Roll a random quality based on weighted chances
     * @param weights Array of weights for each quality tier
     */
    public static EnchantmentQuality rollQuality(int[] weights) {
        if (weights == null || weights.length != values().length) {
            weights = new int[]{40, 30, 20, 7, 2, 1, 0}; // Default to basic weights
        }
        
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        
        int roll = (int) (Math.random() * totalWeight);
        int currentWeight = 0;
        
        for (int i = 0; i < weights.length; i++) {
            currentWeight += weights[i];
            if (roll < currentWeight) {
                return values()[i];
            }
        }
        
        return COMMON; // Fallback
    }
}
