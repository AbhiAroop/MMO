package com.server.enchantments.elements;

import org.bukkit.ChatColor;

/**
 * Hybrid elemental types formed from combining two base elements
 */
public enum HybridElement {
    ICE("Ice", ChatColor.DARK_AQUA, "❄", ElementType.AIR, ElementType.WATER, 
        "Provides freezing and immobilization effects");
    
    // Future hybrids:
    // STORM("Storm", ChatColor.BLUE, "⛈", ElementType.LIGHTNING, ElementType.AIR),
    // MAGMA("Magma", ChatColor.DARK_RED, "🌋", ElementType.FIRE, ElementType.EARTH),
    // POISON("Poison", ChatColor.DARK_GREEN, "☠", ElementType.NATURE, ElementType.SHADOW),
    // CRYSTAL("Crystal", ChatColor.LIGHT_PURPLE, "💎", ElementType.EARTH, ElementType.LIGHT),
    
    private final String displayName;
    private final ChatColor color;
    private final String icon;
    private final ElementType element1;
    private final ElementType element2;
    private final String description;
    
    HybridElement(String displayName, ChatColor color, String icon, 
                  ElementType element1, ElementType element2, String description) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.element1 = element1;
        this.element2 = element2;
        this.description = description;
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
    
    public String getIcon() {
        return icon;
    }
    
    public ElementType getElement1() {
        return element1;
    }
    
    public ElementType getElement2() {
        return element2;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this hybrid is formed from the two given elements
     */
    public boolean isFormedFrom(ElementType e1, ElementType e2) {
        return (element1 == e1 && element2 == e2) || (element1 == e2 && element2 == e1);
    }
    
    /**
     * Get the hybrid formed from two elements, or null if invalid combination
     */
    public static HybridElement getHybrid(ElementType e1, ElementType e2) {
        if (e1 == null || e2 == null) return null;
        
        for (HybridElement hybrid : values()) {
            if (hybrid.isFormedFrom(e1, e2)) {
                return hybrid;
            }
        }
        return null;
    }
}
