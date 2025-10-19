package com.server.enchantments.data;

import org.bukkit.ChatColor;

/**
 * Represents the level of an enchantment (I-VIII).
 * Level determines the base power/magnitude of the enchantment ability.
 * Higher levels = more powerful base effect.
 * Levels now range from 1-8 for variety (previously capped at 5).
 */
public enum EnchantmentLevel {
    I(1, "I", 1.0, ChatColor.GRAY),
    II(2, "II", 1.3, ChatColor.GRAY),
    III(3, "III", 1.6, ChatColor.WHITE),
    IV(4, "IV", 2.0, ChatColor.YELLOW),
    V(5, "V", 2.5, ChatColor.GOLD),
    VI(6, "VI", 3.0, ChatColor.GOLD),
    VII(7, "VII", 3.5, ChatColor.RED),
    VIII(8, "VIII", 4.0, ChatColor.DARK_RED);
    
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
     * Maximum of 256 fragments per slot (increased from 192).
     * Levels now range 1-8 for variety.
     */
    public static EnchantmentLevel calculateLevel(int fragmentCount) {
        if (fragmentCount <= 0) return I;
        
        // Cap at 256 fragments (increased to support level 8)
        fragmentCount = Math.min(fragmentCount, 256);
        
        // Calculate probability distribution across 8 levels
        // 1-16 fragments: Mostly Level I, small chance of II
        // 17-32 fragments: Level II dominant
        // 33-64 fragments: Level III dominant
        // 65-96 fragments: Level IV dominant
        // 97-128 fragments: Level V dominant
        // 129-176 fragments: Level VI dominant
        // 177-216 fragments: Level VII dominant
        // 217-256 fragments: Level VIII dominant
        
        double random = Math.random();
        
        if (fragmentCount <= 16) {
            // Level I zone: 70% I, 25% II, 5% III
            if (random < 0.70) return I;
            if (random < 0.95) return II;
            return III;
        } else if (fragmentCount <= 32) {
            // Level II zone: 15% I, 60% II, 20% III, 5% IV
            if (random < 0.15) return I;
            if (random < 0.75) return II;
            if (random < 0.95) return III;
            return IV;
        } else if (fragmentCount <= 64) {
            // Level III zone: 10% II, 60% III, 25% IV, 5% V
            if (random < 0.10) return II;
            if (random < 0.70) return III;
            if (random < 0.95) return IV;
            return V;
        } else if (fragmentCount <= 96) {
            // Level IV zone: 10% III, 60% IV, 25% V, 5% VI
            if (random < 0.10) return III;
            if (random < 0.70) return IV;
            if (random < 0.95) return V;
            return VI;
        } else if (fragmentCount <= 128) {
            // Level V zone: 10% IV, 60% V, 25% VI, 5% VII
            if (random < 0.10) return IV;
            if (random < 0.70) return V;
            if (random < 0.95) return VI;
            return VII;
        } else if (fragmentCount <= 176) {
            // Level VI zone: 10% V, 60% VI, 25% VII, 5% VIII
            if (random < 0.10) return V;
            if (random < 0.70) return VI;
            if (random < 0.95) return VII;
            return VIII;
        } else if (fragmentCount <= 216) {
            // Level VII zone: 10% VI, 65% VII, 25% VIII
            if (random < 0.10) return VI;
            if (random < 0.75) return VII;
            return VIII;
        } else {
            // Level VIII zone: 20% VII, 80% VIII
            if (random < 0.20) return VII;
            return VIII;
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
