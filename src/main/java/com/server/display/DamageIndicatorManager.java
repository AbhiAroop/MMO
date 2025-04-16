package com.server.display;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class DamageIndicatorManager implements Listener {
    private final Main plugin;

    public DamageIndicatorManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Track both original and final damage to show reduction
     * Use LOWEST priority to capture the original damage before any reductions
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageEarly(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        // Store the original damage value before any reductions
        Player player = (Player) event.getEntity();
        double originalDamage = event.getDamage();
        
        // Store the original damage in a temporary metadata key
        player.setMetadata("originalDamage", new org.bukkit.metadata.FixedMetadataValue(plugin, originalDamage));
    }
    
    /**
     * Main damage handler that shows the indicators after all reductions
     * Use MONITOR priority to capture the final damage after all reductions
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        double damage = event.getFinalDamage();
        
        // Get damage type symbol and color
        String symbol;
        String color;
        
        // For now, most damage is physical
        boolean isMagical = false;
        switch (event.getCause()) {
            case MAGIC:
            case DRAGON_BREATH:
            case WITHER:
            case POISON:
            case LIGHTNING:
                symbol = "✦"; // Magic damage symbol
                color = "§b"; // Aqua color
                isMagical = true;
                break;
            default:
                symbol = "⚔"; // Physical damage symbol
                color = "§c"; // Red color
                break;
        }
        
        // Standard damage indicator for all entities
        spawnDamageIndicator(entity.getLocation(), damage, symbol, color);
        
        // Special handling for players to show damage reduction
        if (entity instanceof Player) {
            Player player = (Player) entity;
            
            // Check if we have the original damage stored
            if (player.hasMetadata("originalDamage")) {
                double originalDamage = player.getMetadata("originalDamage").get(0).asDouble();
                double reducedAmount = originalDamage - damage;
                
                // Only show reduction indicator if significant damage was reduced
                if (reducedAmount > 2.0) {
                    // Get player profile for stat information
                    Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            // Get appropriate stat for the damage type
                            int defenseValue = isMagical ? 
                                profile.getStats().getMagicResist() : 
                                profile.getStats().getArmor();
                            
                            // Calculate percentage reduction
                            double percentReduction = (reducedAmount / originalDamage) * 100;
                            
                            // Only show for significant reductions (10%+)
                            if (percentReduction >= 10) {
                                String defenseType = isMagical ? "Magic Resist" : "Armor";
                                String defenseColor = isMagical ? "§b" : "§a"; // Aqua for magic, green for physical
                                
                                // Spawn the reduction indicator
                                spawnReductionIndicator(
                                    player.getLocation(),
                                    reducedAmount,
                                    percentReduction,
                                    defenseValue,
                                    defenseType,
                                    defenseColor
                                );
                            }
                        }
                    }
                }
                
                // Clean up metadata to prevent memory leaks
                player.removeMetadata("originalDamage", plugin);
            }
        }
    }

    private void spawnDamageIndicator(Location loc, double damage, String symbol, String color) {
        // Offset location slightly to avoid stacking
        loc = loc.add(
            Math.random() * 0.5 - 0.25,
            0.5 + Math.random() * 0.5,
            Math.random() * 0.5 - 0.25
        );
        
        // Create armor stand
        ArmorStand indicator = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        
        // Set armor stand properties
        indicator.setVisible(false);
        indicator.setGravity(false);
        indicator.setMarker(true);
        indicator.setCustomNameVisible(true);
        
        // Format damage text
        String displayText = String.format("%s%s %.1f", color, symbol, damage);
        indicator.setCustomName(displayText);
        
        // Animation and removal
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location startLoc = indicator.getLocation();
            
            @Override
            public void run() {
                if (ticks >= 20) { // Remove after 1 second
                    indicator.remove();
                    this.cancel();
                    return;
                }
                
                // Move upward slowly and fade out
                Location newLoc = startLoc.clone().add(0, ticks * 0.05, 0);
                indicator.teleport(newLoc);
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    /**
     * Spawns a special indicator showing how much damage was reduced by armor/magic resist
     */
    private void spawnReductionIndicator(Location loc, double reducedAmount, double percentage, 
                                        int defenseValue, String defenseType, String color) {
        // Offset to the right of the damage indicator
        loc = loc.add(
            Math.random() * 0.3 + 0.5, // Always spawn to the right side
            0.7 + Math.random() * 0.3,  // Slightly higher than damage indicator
            Math.random() * 0.2 - 0.1
        );
        
        // Create armor stand
        ArmorStand indicator = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        
        // Set armor stand properties
        indicator.setVisible(false);
        indicator.setGravity(false);
        indicator.setMarker(true);
        indicator.setCustomNameVisible(true);
        
        // Format reduction text
        String displayText = String.format("%s-%s: §f%.1f (%.0f%%)", 
                                        color, defenseType, reducedAmount, percentage);
        indicator.setCustomName(displayText);
        
        // Animation and removal
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location startLoc = indicator.getLocation();
            
            @Override
            public void run() {
                if (ticks >= 30) { // Display slightly longer (1.5 seconds)
                    indicator.remove();
                    this.cancel();
                    return;
                }
                
                // Move upward slowly and fade out
                Location newLoc = startLoc.clone().add(0, ticks * 0.03, 0);
                indicator.teleport(newLoc);
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}