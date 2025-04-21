package com.server.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;

/**
 * Handles combat-related events and fixes for attribute issues
 */
public class RangedCombatManager implements Listener {
    private final Main plugin;
    
    public RangedCombatManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Fix attribute stacking issues when entities take damage
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageForAttributeFix(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Schedule size attribute correction after damage processing
        // Use multiple timings to ensure it catches any delayed modifications
        for (int delay : new int[] {1, 3, 5}) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    
                    try {
                        AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
                        if (scaleAttribute != null) {
                            // Check if scale is abnormally high (over 1.8)
                            double currentScale = scaleAttribute.getValue();
                            
                            if (currentScale > 1.8) {
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().warning("Detected abnormal scale value: " + currentScale + 
                                                    " for player: " + player.getName() + ", correcting...");
                                }
                                
                                // Force fix by directly resetting the scale attribute
                                Set<AttributeModifier> allModifiers = new HashSet<>(scaleAttribute.getModifiers());
                                for (AttributeModifier modifier : allModifiers) {
                                    scaleAttribute.removeModifier(modifier);
                                }
                                scaleAttribute.setBaseValue(1.0);
                                
                                // Add a capped modifier if needed (for Crown of Magnus)
                                // Use a maximum of 0.8 to ensure total scale stays at 1.8 or less
                                AttributeModifier cappedMod = new AttributeModifier(
                                    UUID.randomUUID(),
                                    "mmo.size.fixed",
                                    0.8,  // Maximum 1.8 total size
                                    AttributeModifier.Operation.ADD_NUMBER
                                );
                                scaleAttribute.addModifier(cappedMod);
                                
                                // Request a rescan by stopping and starting scanning
                                // This is a public API that forces a full rescan
                                plugin.getStatScanManager().stopScanning(player);
                                plugin.getStatScanManager().startScanning(player);
                                
                                if (plugin.isDebugMode()) {
                                    // Check if the fix worked
                                    double newScale = player.getAttribute(Attribute.GENERIC_SCALE).getValue();
                                    plugin.getLogger().info("Corrected scale value from " + currentScale + 
                                                " to " + newScale + " for player: " + player.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().warning("Error fixing scale attribute: " + e.getMessage());
                        }
                    }
                }
            }.runTaskLater(plugin, delay);
        }
    }
    
    /**
     * Reset a player's attributes to vanilla defaults
     * Used when resetting a player's state
     */
    public void resetAttributes(Player player) {
        try {
            // Reset attack range attribute
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    attackRangeAttribute.removeModifier(modifier);
                }
                attackRangeAttribute.setBaseValue(3.0); // Vanilla default
            }
            
            // Reset scale attribute
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(scaleAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    scaleAttribute.removeModifier(modifier);
                }
                scaleAttribute.setBaseValue(1.0); // Vanilla default
            }
            
            // Reset attack speed attribute
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                Set<AttributeModifier> speedModifiers = new HashSet<>(attackSpeedAttribute.getModifiers());
                for (AttributeModifier modifier : speedModifiers) {
                    attackSpeedAttribute.removeModifier(modifier);
                }
                attackSpeedAttribute.setBaseValue(4.0); // Vanilla default
            }
            
            // Reset movement speed attribute
            AttributeInstance movementSpeedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeedAttribute != null) {
                Set<AttributeModifier> speedModifiers = new HashSet<>(movementSpeedAttribute.getModifiers());
                for (AttributeModifier modifier : speedModifiers) {
                    movementSpeedAttribute.removeModifier(modifier);
                }
                movementSpeedAttribute.setBaseValue(0.1); // Vanilla default
            }
            
            // Reset attack damage attribute
            AttributeInstance attackDamageAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackDamageAttribute != null) {
                Set<AttributeModifier> damageModifiers = new HashSet<>(attackDamageAttribute.getModifiers());
                for (AttributeModifier modifier : damageModifiers) {
                    attackDamageAttribute.removeModifier(modifier);
                }
                attackDamageAttribute.setBaseValue(1.0); // Vanilla default
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting attributes: " + e.getMessage());
        }
    }
}