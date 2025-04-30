package com.server.profiles.skills.abilities.active;

import org.bukkit.entity.Player;

import com.server.profiles.skills.abilities.SkillAbility;

/**
 * Represents an ability that can be actively triggered by the player
 */
public interface ActiveAbility extends SkillAbility {
    /**
     * Get the cooldown of this ability in seconds
     */
    int getCooldownSeconds();
    
    /**
     * Get the current cooldown remaining for a player in milliseconds
     */
    long getCooldownRemaining(Player player);
    
    /**
     * Set the cooldown for a player
     */
    void setCooldown(Player player);
    
    /**
     * Check if the ability is on cooldown for a player
     */
    boolean isOnCooldown(Player player);
    
    /**
     * Activate the ability for a player
     * @return true if the ability was successfully activated, false otherwise
     */
    boolean activate(Player player);
    
    /**
     * Get the activation method description (how to use the ability)
     */
    String getActivationMethod();
}