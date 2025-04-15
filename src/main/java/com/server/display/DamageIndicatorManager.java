package com.server.display;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;

public class DamageIndicatorManager implements Listener {
    private final Main plugin;

    public DamageIndicatorManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        double damage = event.getFinalDamage();
        
        // Get damage type symbol and color
        String symbol;
        String color;
        
        // For now, most damage is physical
        switch (event.getCause()) {
            case MAGIC:
            case DRAGON_BREATH:
            case POISON:
                symbol = "✦"; // Magic damage symbol
                color = "§b"; // Aqua color
                break;
            default:
                symbol = "⚔"; // Physical damage symbol
                color = "§c"; // Red color
                break;
        }
        
        spawnDamageIndicator(entity.getLocation(), damage, symbol, color);
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
}