package com.server.profiles.skills.abilities;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Base interface for all skill abilities
 */
public interface SkillAbility {
    /**
     * Get the unique identifier for this ability
     */
    String getId();
    
    /**
     * Get the display name of this ability
     */
    String getDisplayName();
    
    /**
     * Get the description of this ability
     */
    String getDescription();
    
    /**
     * Get the skill ID that this ability belongs to
     */
    String getSkillId();
    
    /**
     * Get the icon for this ability
     */
    Material getIcon();
    
    /**
     * Get the requirement description for unlocking this ability
     */
    String getUnlockRequirement();
    
    /**
     * Check if a player has unlocked this ability
     */
    boolean isUnlocked(Player player);
    
    /**
     * Create an item representation of this ability for GUI display
     */
    ItemStack createDisplayItem(Player player);
}