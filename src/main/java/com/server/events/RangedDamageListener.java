package com.server.events;

import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

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
        Entity damager = event.getDamager();
        
        // Only handle projectile damage
        if (!(damager instanceof Projectile)) {
            return;
        }
        
        Projectile projectile = (Projectile) damager;
        ProjectileSource source = projectile.getShooter();
        
        // Only handle player-shot projectiles
        if (!(source instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) source;
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(shooter.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(shooter.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get ranged damage stat
        int rangedDamage = profile.getStats().getRangedDamage();
        
        // Get projectile force/charge level
        float force = 1.0f;  // Default to full charge if metadata not present
        if (projectile.hasMetadata("bow_force")) {
            force = projectile.getMetadata("bow_force").get(0).asFloat();
        }
        
        // UPDATED DAMAGE CALCULATION:
        // At 0% charge: Damage is 0.5
        // At 1% charge: Damage is max(0.5, 1% of full damage)
        // At X% charge: Damage is max(0.5, X% of full damage)
        // At 100% charge: Full damage
        double baseDamage;
        if (force <= 0.01) {
            // At 0-1% charge, minimum damage is 0.5
            baseDamage = 0.5;
        } else {
            // For charges above 1%, scale linearly but ensure minimum 0.5 damage
            baseDamage = Math.max(0.5, rangedDamage * (force/3));
        }
        
        // Apply custom damage calculations
        boolean isCritical = false;
        
        // Check for critical hit based on critical chance stat
        // MODIFIED: Critical hits can happen at any charge level
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
        }
        
        // Check for special effects
        double finalDamage = baseDamage;
        
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;
            
            // Handle special arrow effects (e.g., fire, knockback)
            if (arrow.getFireTicks() > 0) {
                // Apply additional fire damage or effects
                finalDamage *= 1.25; // 25% more damage for fire arrows
            }
        }
        else if (projectile instanceof Trident) {
            // Apply trident-specific damage modifiers
            finalDamage *= 1.5; // 50% more damage for tridents (they're more powerful)
        }
        
        // Handle burst chance (after critical calculation)
        if (Math.random() < profile.getStats().getBurstChance()) {
            finalDamage *= profile.getStats().getBurstDamage();
            
            // Display burst effect
            event.getEntity().getWorld().playSound(
                event.getEntity().getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE, 
                0.5f, 1.5f
            );
        }
        
        // Set the actual damage
        event.setDamage(finalDamage);
        
        // Apply lifesteal
        if (event.getEntity() instanceof LivingEntity && finalDamage > 0) {
            double lifeStealPercent = profile.getStats().getLifeSteal();
            if (lifeStealPercent > 0) {
                double healAmount = finalDamage * (lifeStealPercent / 100.0);
                double maxHealth = shooter.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                
                if (shooter.getHealth() < maxHealth && healAmount > 0) {
                    double newHealth = Math.min(shooter.getHealth() + healAmount, maxHealth);
                    shooter.setHealth(newHealth);
                    
                    // Visual effect for significant lifesteal
                    if (healAmount >= 1.0) {
                        // Play a subtle healing sound
                        shooter.playSound(shooter.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.5f, 1.2f);
                        
                        // Display healing message if significant
                        if (healAmount >= 3.0) {
                            shooter.sendMessage("§a⚕ §7Lifesteal healed you for §a" + String.format("%.1f", healAmount) + " §7health");
                        }
                    }
                }
            }
        }
        
        // Debug information
        if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
            plugin.debugLog(DebugSystem.COMBAT, shooter.getName() + "'s ranged attack: " +
                        "Force=" + String.format("%.2f", force) + 
                        ", Ranged Damage=" + rangedDamage + 
                        ", Base Damage=" + String.format("%.2f", baseDamage) +
                        ", Final Damage=" + String.format("%.2f", finalDamage) +
                        ", Critical=" + isCritical);
        }
        
        // Add visual effects for critical hits
        if (isCritical && event.getEntity() instanceof LivingEntity) {
            event.getEntity().getWorld().spawnParticle(
                org.bukkit.Particle.CRIT,
                event.getEntity().getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
}