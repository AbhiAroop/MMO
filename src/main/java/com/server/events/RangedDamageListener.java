package com.server.events;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Handles ranged damage from bows, crossbows, and other projectiles
 */
public class RangedDamageListener implements Listener {
    
    private final Main plugin;
    
    public RangedDamageListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Store force/charge info when a player shoots an arrow
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBowShoot(EntityShootBowEvent event) {
        // Only handle player shots
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Entity projectile = event.getProjectile();
        float force = event.getForce(); // This is the charge level (0.0 to 1.0)
        
        // Store charge data on the projectile
        projectile.setMetadata("bow_force", new FixedMetadataValue(plugin, force));
        projectile.setMetadata("shooter_uuid", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        
        if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
            plugin.debugLog(DebugSystem.COMBAT, "Player " + player.getName() + " shot a projectile with force: " + force);
        }
        
        // Handle special bow properties
        ItemStack bow = event.getBow();
        if (bow != null && bow.hasItemMeta() && bow.getItemMeta().hasLore()) {
            // Check for custom bow properties and apply them to the projectile
            if (bow.getItemMeta().hasCustomModelData()) {
                int modelData = bow.getItemMeta().getCustomModelData();
                projectile.setMetadata("custom_bow_id", new FixedMetadataValue(plugin, modelData));
                
                if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
                    plugin.debugLog(DebugSystem.COMBAT, "Shot with custom bow ID: " + modelData);
                }
            }
        }
    }
    
    /**
     * Handle damage from arrows and other projectiles
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        
        Projectile projectile = (Projectile) event.getDamager();
        if (!(projectile.getShooter() instanceof Player)) return;
        
        Player shooter = (Player) projectile.getShooter();
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(shooter.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(shooter.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Use ranged damage instead of physical damage for projectiles
        double baseDamage = profile.getStats().getRangedDamage();
        
        // CRITICAL HIT SYSTEM for ranged attacks
        boolean isCritical = false;
        double critChance = profile.getStats().getCriticalChance();
        if (Math.random() < critChance) {
            double critDamage = profile.getStats().getCriticalDamage();
            baseDamage *= critDamage;
            isCritical = true;
            
            // Play critical hit sound
            shooter.getWorld().playSound(
                event.getEntity().getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                1.0f, 1.0f
            );
            
            // Critical hit effects for ranged
            applyRangedCriticalEffects(shooter, event.getEntity(), baseDamage);
        }
        
        // Check for special effects
        double finalDamage = baseDamage;
        
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;
            
            // Handle special arrow effects (e.g., fire, knockback)
            if (arrow.getFireTicks() > 0) {
                event.getEntity().setFireTicks(100); // 5 seconds of fire
            }
            
            // Apply knockback based on arrow's knockback strength
            if (arrow.getKnockbackStrength() > 0) {
                Vector knockback = arrow.getVelocity().normalize().multiply(arrow.getKnockbackStrength() * 0.5);
                if (event.getEntity() instanceof LivingEntity) {
                    event.getEntity().setVelocity(event.getEntity().getVelocity().add(knockback));
                }
            }
        }
        
        // Set the final damage
        event.setDamage(finalDamage);
        
        // Debug logging
        if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
            plugin.debugLog(DebugSystem.COMBAT, "Ranged attack from " + shooter.getName() + 
                ": " + finalDamage + " damage" + (isCritical ? " (CRITICAL)" : ""));
        }
        
        // Apply lifesteal for ranged attacks
        applyRangedLifesteal(shooter, profile, finalDamage);
    }

    /**
     * Apply critical hit effects for ranged attacks
     */
    private void applyRangedCriticalEffects(Player shooter, Entity target, double damage) {
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        
        // Different particles for ranged crits (more arrow-like)
        target.getWorld().spawnParticle(
            Particle.CRIT, 
            targetLoc, 
            6, 
            0.4, 0.4, 0.4, 
            0.2
        );
        
        // Arrow-specific crit particles
        target.getWorld().spawnParticle(
            Particle.ENCHANTED_HIT, 
            targetLoc, 
            4, 
            0.3, 0.3, 0.3, 
            0.1
        );
        
        // Send message to shooter
        shooter.sendMessage("§a§l🏹 CRITICAL SHOT! §r§a" + String.format("%.1f", damage) + " damage!");
    }

    /**
     * Apply lifesteal for ranged attacks
     */
    private void applyRangedLifesteal(Player shooter, PlayerProfile profile, double damage) {
        double lifeStealPercent = profile.getStats().getLifeSteal();
        if (lifeStealPercent <= 0) return;
        
        double healAmount = damage * (lifeStealPercent / 100.0);
        double currentHealth = shooter.getHealth();
        double maxHealth = shooter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        
        if (currentHealth < maxHealth && healAmount > 0) {
            double newHealth = Math.min(currentHealth + healAmount, maxHealth);
            shooter.setHealth(newHealth);
            profile.getStats().setCurrentHealth(newHealth);
            
            if (healAmount >= 1.0) {
                shooter.playSound(shooter.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.4f, 1.3f);
                
                if (healAmount >= 2.0) {
                    shooter.sendMessage("§a⚕ §7Ranged lifesteal: §a+" + String.format("%.1f", healAmount) + " §7health");
                }
            }
        }
    }
}