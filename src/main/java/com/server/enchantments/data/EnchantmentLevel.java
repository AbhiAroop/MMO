package com.server.enchantments.data;

import org.bukkit.ChatColor;

/**
 * Represents the level of an enchantment (I-V).
 * Level determines the base power/magnitude of the enchantment ability.
 * Higher levels = more powerful base effect.
 */
public enum EnchantmentLevel {
    I(1, "I", 1.0, ChatColor.GRAY),
    II(2, "II", 1.3, ChatColor.GRAY),
    III(3, "III", 1.6, ChatColor.WHITE),
    IV(4, "IV", 2.0, ChatColor.YELLOW),
    V(5, "V", 2.5, ChatColor.GOLD);
    
    private final int numericLevel;
    private final String roman;
    private final double powerMultiplier;
    private final ChatColor color;
    
    EnchantmentLevel(int numericLevel, String roman, double powerMultiplier, ChatColor color) {
        this.numericLevel = numericLevel;
        this.roman = roman;
        this.powerMultiplier = powerMultiplier;
        this.color = color;
    }
    
    public int getNumericLevel() {
        return numericLevel;
    }
    
    public String getRoman() {
        return roman;
    }
    
    public double getPowerMultiplier() {
        return powerMultiplier;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public String getDisplayName() {
        return color + roman;
    }
    
    /**
     * Get enchantment level based on fragment count.
     * More fragments = higher chance of higher level.
     * Maximum of 192 fragments per slot.
     */
    public static EnchantmentLevel calculateLevel(int fragmentCount) {
        if (fragmentCount <= 0) return I;
        
        // Cap at 192 fragments
        fragmentCount = Math.min(fragmentCount, 192);
        
        // Calculate probability distribution
        // 1-16 fragments: Mostly Level I, small chance of II
        // 17-48 fragments: Level II dominant, chance of I or III
        // 49-96 fragments: Level III dominant, chance of II or IV
        // 97-144 fragments: Level IV dominant, chance of III or V
        // 145-192 fragments: Level V dominant, high chance, some IV
        
        double random = Math.random();
        
        if (fragmentCount <= 16) {
            // Level I zone: 70% I, 25% II, 5% III
            if (random < 0.70) return I;
            if (random < 0.95) return II;
            return III;
        } else if (fragmentCount <= 48) {
            // Level II zone: 15% I, 60% II, 20% III, 5% IV
            if (random < 0.15) return I;
            if (random < 0.75) return II;
            if (random < 0.95) return III;
            return IV;
        } else if (fragmentCount <= 96) {
            // Level III zone: 10% II, 65% III, 20% IV, 5% V
            if (random < 0.10) return II;
            if (random < 0.75) return III;
            if (random < 0.95) return IV;
            return V;
        } else if (fragmentCount <= 144) {
            // Level IV zone: 10% III, 60% IV, 30% V
            if (random < 0.10) return III;
            if (random < 0.70) return IV;
            return V;
        } else {
            // Level V zone: 20% IV, 80% V
            if (random < 0.20) return IV;
            return V;
        }
    }
    
    /**
     * Get level from numeric value
     */
    public static EnchantmentLevel fromNumeric(int level) {
        for (EnchantmentLevel el : values()) {
            if (el.numericLevel == level) return el;
        }
        return I; // Default
    }
}
