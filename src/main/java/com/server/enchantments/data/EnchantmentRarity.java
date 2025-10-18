package com.server.enchantments.data;

import org.bukkit.ChatColor;

/**
 * Rarity of enchantments - determines availability and power
 */
public enum EnchantmentRarity {
    COMMON("Common", ChatColor.WHITE, 1),
    UNCOMMON("Uncommon", ChatColor.GREEN, 2),
    RARE("Rare", ChatColor.BLUE, 3),
    EPIC("Epic", ChatColor.DARK_PURPLE, 4),
    LEGENDARY("Legendary", ChatColor.GOLD, 5);
    
    private final String displayName;
    private final ChatColor color;
    private final int tierLevel;
    
    EnchantmentRarity(String displayName, ChatColor color, int tierLevel) {
        this.displayName = displayName;
        this.color = color;
        this.tierLevel = tierLevel;
    }
    
    public String getDisplayName() {
        return color + displayName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public int getTierLevel() {
        return tierLevel;
    }
    
    /**
     * Get minimum XP cost for this rarity
     */
    public int getMinXPCost() {
        switch (this) {
            case COMMON: return 5;
            case UNCOMMON: return 10;
            case RARE: return 20;
            case EPIC: return 35;
            case LEGENDARY: return 50;
            default: return 5;
        }
    }
    
    /**
     * Get maximum XP cost for this rarity
     */
    public int getMaxXPCost() {
        switch (this) {
            case COMMON: return 10;
            case UNCOMMON: return 20;
            case RARE: return 35;
            case EPIC: return 50;
            case LEGENDARY: return 75;
            default: return 10;
        }
    }
}
