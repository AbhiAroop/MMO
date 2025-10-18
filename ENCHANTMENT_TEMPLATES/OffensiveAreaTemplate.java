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
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.AffinityModifier;

/**
 * TEMPLATE: Offensive Enchantment with Area Effect
 * Copy this file and modify for new offensive enchantments
 * 
 * Example: Cinderwake - Leaves burning trails
 */
public class OffensiveAreaTemplate extends CustomEnchantment {
    
    // Cooldown tracking (optional)
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000; // 5 seconds
    
    public OffensiveAreaTemplate() {
        super(
            "template_offensive_area",
            "Template Offensive Area",
            "Example area damage enchantment",
            EnchantmentRarity.RARE,
            ElementType.FIRE
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // List valid materials
        return type == Material.DIAMOND_SWORD ||
               type == Material.NETHERITE_SWORD ||
               type == Material.DIAMOND_AXE ||
               type == Material.NETHERITE_AXE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [duration_ticks, damage_per_tick, area_radius]
        return new double[]{40.0, 2.0, 3.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) damageEvent.getEntity();
        
        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if (currentTime - lastUse < COOLDOWN_MS) {
                return; // Still on cooldown
            }
        }
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        int duration = (int) stats[0];
        double damagePerTick = stats[1];
        double radius = stats[2];
        
        // Update cooldown
        cooldowns.put(playerId, currentTime);
        
        // Create area effect
        createAreaEffect(player, target.getLocation(), duration, damagePerTick, radius);
        
        // Feedback
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
        player.sendMessage("§6⚔ §cArea effect triggered!");
    }
    
    /**
     * Creates a persistent area effect that damages enemies
     */
    private void createAreaEffect(Player player, Location center, int duration, 
                                  double damagePerTick, double radius) {
        new BukkitRunnable() {
            int ticksRemaining = duration;
            
            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }
                
                // Spawn particles
                center.getWorld().spawnParticle(
                    Particle.FLAME,
                    center.clone().add(0, 0.1, 0),
                    10,
                    radius * 0.3, 0.2, radius * 0.3,
                    0.01
                );
                
                // Damage nearby entities every 10 ticks (0.5s)
                if (ticksRemaining % 10 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 2.0, radius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;
                            
                            // Apply damage with affinity in PVP
                            if (target instanceof Player) {
                                double modifier = AffinityModifier.calculateDamageModifier(
                                    player, (Player) target, getElement());
                                double modifiedDamage = damagePerTick * modifier;
                                target.damage(modifiedDamage, player);
                            } else {
                                // PVE - no affinity
                                target.damage(damagePerTick, player);
                            }
                            
                            // Visual feedback
                            target.getWorld().spawnParticle(
                                Particle.FLAME,
                                target.getLocation().add(0, 1, 0),
                                5,
                                0.3, 0.5, 0.3,
                                0.05
                            );
                        }
                    }
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L); // Run every tick
    }
}
