package com.server.enchantments.elements;

import org.bukkit.ChatColor;

/**
 * Represents the three categories of elemental affinity.
 * Affinities are now split into three distinct categories:
 * - OFFENSE: Affects damage dealt, debuff potency, and offensive proc chances
 * - DEFENSE: Affects damage received, defensive effects, and resistance
 * - UTILITY: Affects non-combat benefits (movement, regen, resource management)
 * 
 * This system allows for counter-mechanics where matching offense/defense
 * affinities result in reduced effectiveness (e.g., Fire offense vs Fire defense).
 */
public enum AffinityCategory {
    /**
     * Offensive affinity - affects outgoing damage and offensive effects.
     * Used when dealing damage, applying debuffs, or triggering offensive procs.
     */
    OFFENSE("Offensive", ChatColor.RED, "⚔"),
    
    /**
     * Defensive affinity - affects incoming damage and defensive effects.
     * Used when taking damage, blocking effects, or triggering defensive procs.
     * Counter-mechanic: If attacker's offensive element matches defender's defensive element,
     * attacks are less effective (e.g., Fire offense vs Fire defense = -20% effectiveness).
     */
    DEFENSE("Defensive", ChatColor.BLUE, "🛡"),
    
    /**
     * Utility affinity - affects non-combat benefits.
     * NOT used in combat calculations. Affects movement speed, regeneration,
     * resource management, and other quality-of-life benefits.
     */
    UTILITY("Utility", ChatColor.GREEN, "✦");
    
    private final String displayName;
    private final ChatColor color;
    private final String icon;
    
    AffinityCategory(String displayName, ChatColor color, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
    }
    
    /**
     * Get the display name for this category.
     * @return Display name (e.g., "Offensive", "Defensive", "Utility")
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the color associated with this category.
     * @return ChatColor for this category
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Get the icon/symbol for this category.
     * @return Unicode icon character
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * Check if this category affects combat calculations.
     * @return true for OFFENSE and DEFENSE, false for UTILITY
     */
    public boolean affectsCombat() {
        return this == OFFENSE || this == DEFENSE;
    }
    
    /**
     * Get formatted display text with color and icon.
     * @return Colored text with icon (e.g., "§c⚔ Offensive")
     */
    public String getFormattedName() {
        return color + icon + " " + displayName;
    }
}
