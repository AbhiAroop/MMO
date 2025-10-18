package com.server.enchantments.elements;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Elemental types with counter cycle system
 * Counter Cycle: Fire > Nature > Earth > Lightning > Water > Air > Shadow > Light > Fire
 */
public enum ElementType {
    FIRE("Fire", ChatColor.RED, Material.BLAZE_POWDER, "🔥"),
    NATURE("Nature", ChatColor.GREEN, Material.OAK_LEAVES, "🌿"),
    EARTH("Earth", ChatColor.GOLD, Material.BROWN_TERRACOTTA, "⛰"),
    LIGHTNING("Lightning", ChatColor.YELLOW, Material.LIGHTNING_ROD, "⚡"),
    WATER("Water", ChatColor.AQUA, Material.PRISMARINE_SHARD, "💧"),
    AIR("Air", ChatColor.WHITE, Material.FEATHER, "💨"),
    SHADOW("Shadow", ChatColor.DARK_GRAY, Material.ECHO_SHARD, "🌑"),
    LIGHT("Light", ChatColor.LIGHT_PURPLE, Material.GLOWSTONE_DUST, "✨");
    
    private final String displayName;
    private final ChatColor color;
    private final Material fragmentMaterial;
    private final String icon;
    
    ElementType(String displayName, ChatColor color, Material fragmentMaterial, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.fragmentMaterial = fragmentMaterial;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return color + displayName;
    }
    
    public String getColoredIcon() {
        return color + icon;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public Material getFragmentMaterial() {
        return fragmentMaterial;
    }
    
    public String getIcon() {
        return icon;
    }
    
    /**
     * Get the element this element is strong against (counter cycle)
     * Fire > Nature > Earth > Lightning > Water > Air > Shadow > Light > Fire
     */
    public ElementType getStrongAgainst() {
        switch (this) {
            case FIRE: return NATURE;
            case NATURE: return EARTH;
            case EARTH: return LIGHTNING;
            case LIGHTNING: return WATER;
            case WATER: return AIR;
            case AIR: return SHADOW;
            case SHADOW: return LIGHT;
            case LIGHT: return FIRE;
            default: return null;
        }
    }
    
    /**
     * Get the element this element is weak against (counter cycle)
     */
    public ElementType getWeakAgainst() {
        switch (this) {
            case FIRE: return LIGHT;
            case NATURE: return FIRE;
            case EARTH: return NATURE;
            case LIGHTNING: return EARTH;
            case WATER: return LIGHTNING;
            case AIR: return WATER;
            case SHADOW: return AIR;
            case LIGHT: return SHADOW;
            default: return null;
        }
    }
    
    /**
     * Calculate damage multiplier based on elemental matchup
     * @param defender The element being attacked
     * @return Multiplier (0.6x weak, 1.0x neutral, 1.4x strong)
     */
    public double getDamageMultiplier(ElementType defender) {
        if (defender == null) return 1.0;
        if (this.getStrongAgainst() == defender) return 1.4;
        if (this.getWeakAgainst() == defender) return 0.6;
        return 1.0;
    }
    
    /**
     * Check if this element can form a hybrid with another
     * Only specific combinations allowed
     */
    public boolean canFormHybrid(ElementType other) {
        if (other == null || other == this) return false;
        
        // Ice: Air + Water
        if ((this == AIR && other == WATER) || (this == WATER && other == AIR)) return true;
        
        // Future hybrids can be added here
        // Storm: Lightning + Air
        // Magma: Fire + Earth
        // etc.
        
        return false;
    }
    
    /**
     * Get the hybrid element type if combination is valid
     */
    public HybridElement getHybridWith(ElementType other) {
        if (!canFormHybrid(other)) return null;
        
        // Ice: Air + Water
        if ((this == AIR && other == WATER) || (this == WATER && other == AIR)) {
            return HybridElement.ICE;
        }
        
        return null;
    }
}
