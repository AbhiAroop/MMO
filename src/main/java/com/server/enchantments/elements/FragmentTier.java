package com.server.enchantments.elements;

import org.bukkit.ChatColor;

/**
 * Fragment quality tiers that affect enchantment outcomes
 */
public enum FragmentTier {
    BASIC("Basic", ChatColor.GRAY, 1, 0.30, 10),      // 30% hybrid chance, 10 affinity
    REFINED("Refined", ChatColor.BLUE, 2, 0.40, 30),  // 40% hybrid chance, 30 affinity
    PRISTINE("Pristine", ChatColor.LIGHT_PURPLE, 3, 0.50, 50); // 50% hybrid chance, 50 affinity
    
    private final String displayName;
    private final ChatColor color;
    private final int tierLevel;
    private final double hybridChanceBoost;
    private final int affinityBase;
    
    FragmentTier(String displayName, ChatColor color, int tierLevel, 
                 double hybridChanceBoost, int affinityBase) {
        this.displayName = displayName;
        this.color = color;
        this.tierLevel = tierLevel;
        this.hybridChanceBoost = hybridChanceBoost;
        this.affinityBase = affinityBase;
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
    
    public double getHybridChanceBoost() {
        return hybridChanceBoost;
    }
    
    public int getAffinityBase() {
        return affinityBase;
    }
    
    /**
     * Get quality rating weights for this tier
     * Higher tier = better chance for higher quality
     */
    public int[] getQualityWeights() {
        switch (this) {
            case BASIC:
                // Poor(40%), Common(30%), Uncommon(20%), Rare(7%), Epic(2%), Legendary(1%), Godly(0%)
                return new int[]{40, 30, 20, 7, 2, 1, 0};
            case REFINED:
                // Poor(20%), Common(25%), Uncommon(30%), Rare(15%), Epic(7%), Legendary(2%), Godly(1%)
                return new int[]{20, 25, 30, 15, 7, 2, 1};
            case PRISTINE:
                // Poor(5%), Common(15%), Uncommon(25%), Rare(25%), Epic(15%), Legendary(10%), Godly(5%)
                return new int[]{5, 15, 25, 25, 15, 10, 5};
            default:
                return new int[]{40, 30, 20, 7, 2, 1, 0};
        }
    }
    
    /**
     * Get rarity weights for enchantment selection
     * Higher tier = better chance for rarer enchantments
     */
    public int[] getRarityWeights() {
        switch (this) {
            case BASIC:
                // Common(70%), Uncommon(20%), Rare(8%), Epic(2%), Legendary(0%)
                return new int[]{70, 20, 8, 2, 0};
            case REFINED:
                // Common(40%), Uncommon(30%), Rare(20%), Epic(8%), Legendary(2%)
                return new int[]{40, 30, 20, 8, 2};
            case PRISTINE:
                // Common(20%), Uncommon(30%), Rare(30%), Epic(15%), Legendary(5%)
                return new int[]{20, 30, 30, 15, 5};
            default:
                return new int[]{70, 20, 8, 2, 0};
        }
    }
}
