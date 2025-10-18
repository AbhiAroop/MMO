package com.server.enchantments.abilities.offensive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.AffinityModifier;

/**
 * TEMPLATE: Chain/Bounce Offensive Enchantment
 * Copy this file and modify for chain lightning or bouncing effects
 * 
 * Example: Lightning that chains between enemies
 */
public class OffensiveChainTemplate extends CustomEnchantment {
    
    // Charge tracking (every Nth hit)
    private static final Map<UUID, Integer> hitCounts = new HashMap<>();
    private static final int HITS_TO_TRIGGER = 5;
    
    public OffensiveChainTemplate() {
        super(
            "template_offensive_chain",
            "Template Offensive Chain",
            "Every 5th hit chains to nearby enemies",
            EnchantmentRarity.EPIC,
            ElementType.LIGHTNING
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        return type == Material.DIAMOND_SWORD ||
               type == Material.NETHERITE_SWORD ||
               type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [chain_damage, max_chains, chain_range]
        return new double[]{4.0, 3.0, 5.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity initialTarget = (LivingEntity) damageEvent.getEntity();
        
        // Track hits
        UUID playerId = player.getUniqueId();
        int hits = hitCounts.getOrDefault(playerId, 0) + 1;
        
        if (hits < HITS_TO_TRIGGER) {
            hitCounts.put(playerId, hits);
            return;
        }
        
        // Reset counter
        hitCounts.put(playerId, 0);
        
        // Get stats
        double[] stats = getScaledStats(quality);
        double chainDamage = stats[0];
        int maxChains = (int) stats[1];
        double chainRange = stats[2];
        
        // Perform chain effect
        chainEffect(player, initialTarget, chainDamage, maxChains, chainRange);
        
        // Feedback
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);
        player.sendMessage("§e⚡ Chain lightning!");
    }
    
    /**
     * Chains damage between nearby enemies
     */
    private void chainEffect(Player player, LivingEntity initialTarget, 
                            double damage, int maxChains, double range) {
        Set<UUID> hitEntities = new HashSet<>();
        hitEntities.add(initialTarget.getUniqueId());
        
        LivingEntity currentTarget = initialTarget;
        
        for (int chain = 0; chain < maxChains; chain++) {
            // Find next closest target
            LivingEntity nextTarget = null;
            double closestDistance = Double.MAX_VALUE;
            
            Location currentLoc = currentTarget.getLocation();
            for (Entity entity : currentTarget.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity && 
                    !hitEntities.contains(entity.getUniqueId()) &&
                    entity != player) {
                    
                    LivingEntity living = (LivingEntity) entity;
                    double distance = living.getLocation().distance(currentLoc);
                    
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        nextTarget = living;
                    }
                }
            }
            
            if (nextTarget == null) break; // No more targets
            
            // Visual effect
            drawChain(currentLoc, nextTarget.getLocation());
            
            // Apply damage (reduced per chain)
            double chainMultiplier = Math.pow(0.75, chain); // 75% per bounce
            double finalDamage = damage * chainMultiplier;
            
            if (nextTarget instanceof Player) {
                double affinityModifier = AffinityModifier.calculateDamageModifier(
                    player, (Player) nextTarget, getElement());
                finalDamage *= affinityModifier;
            }
            
            nextTarget.damage(finalDamage, player);
            
            // Mark as hit and continue
            hitEntities.add(nextTarget.getUniqueId());
            currentTarget = nextTarget;
        }
    }
    
    /**
     * Draws visual chain between two locations
     */
    private void drawChain(Location from, Location to) {
        double distance = from.distance(to);
        int particles = (int) (distance * 3);
        
        for (int i = 0; i < particles; i++) {
            double ratio = (double) i / particles;
            Location particleLoc = from.clone().add(
                to.clone().subtract(from).toVector().multiply(ratio)
            );
            
            particleLoc.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                particleLoc,
                1,
                0.1, 0.1, 0.1,
                0
            );
        }
    }
}
