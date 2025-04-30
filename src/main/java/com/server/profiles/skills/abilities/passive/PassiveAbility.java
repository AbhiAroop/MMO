package com.server.profiles.skills.abilities.passive;

import org.bukkit.entity.Player;

import com.server.profiles.skills.abilities.SkillAbility;

/**
 * Represents a passive ability that provides a constant effect
 */
public interface PassiveAbility extends SkillAbility {
    /**
     * Check if this passive ability is enabled for a player
     */
    boolean isEnabled(Player player);
    
    /**
     * Toggle the enabled state of this passive ability for a player
     * @return The new enabled state
     */
    boolean toggleEnabled(Player player);
    
    /**
     * Set the enabled state of this passive ability for a player
     */
    void setEnabled(Player player, boolean enabled);
    
    /**
     * Called when the passive ability is enabled
     */
    void onEnable(Player player);
    
    /**
     * Called when the passive ability is disabled
     */
    void onDisable(Player player);
}