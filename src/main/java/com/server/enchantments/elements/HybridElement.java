package com.server.enchantments.elements;

import org.bukkit.ChatColor;

/**
 * Hybrid elemental types formed from combining two base elements
 */
public enum HybridElement {
    ICE("Ice", ChatColor.DARK_AQUA, "❄", ElementType.AIR, ElementType.WATER, 
        "Provides freezing and immobilization effects"),
    STORM("Storm", ChatColor.YELLOW, "⚡", ElementType.FIRE, ElementType.LIGHTNING,
        "Combines fire and lightning into devastating attacks"),
    MIST("Mist", ChatColor.AQUA, "🌫", ElementType.WATER, ElementType.AIR,
        "Provides evasion and mobility through watery vapors"),
    DECAY("Decay", ChatColor.DARK_GRAY, "☠", ElementType.EARTH, ElementType.SHADOW,
        "Corrupts and weakens through shadow-infused earth"),
    RADIANCE("Radiance", ChatColor.GOLD, "✦", ElementType.LIGHT, ElementType.LIGHTNING,
        "Channels divine energy with shocking speed"),
    ASH("Ash", ChatColor.GRAY, "💀", ElementType.FIRE, ElementType.SHADOW,
        "Burns with dark flames that consume and weaken"),
    PURITY("Purity", ChatColor.WHITE, "✧", ElementType.WATER, ElementType.LIGHT,
        "Cleanses and protects with sacred waters");
    
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
    
    /**
     * Get the primary element (60% affinity contribution)
     */
    public ElementType getPrimary() {
        return element1;
    }
    
    /**
     * Get the secondary element (40% affinity contribution)
     */
    public ElementType getSecondary() {
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
