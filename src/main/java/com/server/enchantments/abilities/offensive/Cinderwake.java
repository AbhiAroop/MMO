package com.server.enchantments.abilities.offensive;

import java.util.HashMap;
import java.util.Map;
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
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.AffinityModifier;

/**
 * Fragment of Cinderwake
 * Equipment: Sword, Dagger, Spear
 * Effect: Attacks leave brief burning trails (2s) that ignite enemies crossing them.
 * Role: Aggressive AoE fire damage.
 */
public class Cinderwake extends CustomEnchantment {
    
    private static final Map<UUID, Long> trailCooldowns = new HashMap<>();
    private static final long TRAIL_COOLDOWN = 5000; // 5 seconds between trail spawns
    
    public Cinderwake() {
        super(
            "cinderwake",
            "Cinderwake",
            "Leaves burning trails that ignite enemies",
            EnchantmentRarity.RARE,
            ElementType.FIRE
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to swords, axes, and tridents (spear-like)
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE ||
               type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [duration_ticks, damage_per_tick]
        return new double[]{40.0, 2.0};
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
        
        if (trailCooldowns.containsKey(playerId)) {
            long lastTrail = trailCooldowns.get(playerId);
            if (currentTime - lastTrail < TRAIL_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        int trailDuration = (int) stats[0];
        double damagePerTick = stats[1];
        
        // Update cooldown
        trailCooldowns.put(playerId, currentTime);
        
        // Create burning trail
        Location targetLoc = target.getLocation();
        createBurningTrail(player, targetLoc, trailDuration, damagePerTick);
        
        // Visual and sound effects
        Location playerLoc = player.getLocation();
        if (playerLoc != null && playerLoc.getWorld() != null) {
            playerLoc.getWorld().playSound(playerLoc, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
        }
        player.sendMessage("§6⚔ §cCinderwake §7trail ignited!");
    }
    
    /**
     * Creates a burning trail at the target location
     */
    private void createBurningTrail(Player player, Location startLoc, int duration, 
                                    double damagePerTick) {
        // Create trail in the direction the player is facing
        Location playerLoc = player.getLocation();
        if (playerLoc == null || startLoc == null) return;
        
        org.bukkit.util.Vector direction = startLoc.toVector().subtract(playerLoc.toVector()).normalize();
        double trailLength = 3.0; // Fixed 3 blocks
        
        // Create multiple burning zones along the trail
        int segments = (int) Math.max(2, trailLength);
        
        new BukkitRunnable() {
            int ticksRemaining = duration;
            
            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }
                
                // Spawn particles and check for enemies in each segment
                for (int i = 0; i < segments; i++) {
                    Location segmentLoc = startLoc.clone().add(direction.clone().multiply(i));
                    
                    // Spawn fire particles
                    segmentLoc.getWorld().spawnParticle(
                        Particle.FLAME,
                        segmentLoc.add(0, 0.1, 0),
                        5,
                        0.3, 0.1, 0.3,
                        0.01
                    );
                    
                    if (ticksRemaining % 10 == 0) { // Check every 0.5 seconds
                        segmentLoc.getWorld().spawnParticle(
                            Particle.LAVA,
                            segmentLoc,
                            2,
                            0.2, 0.1, 0.2,
                            0
                        );
                    }
                    
                    // Check for entities in burning zone
                    for (Entity entity : segmentLoc.getWorld().getNearbyEntities(segmentLoc, 1.0, 2.0, 1.0)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity livingEntity = (LivingEntity) entity;
                            
                            // Apply burn damage with affinity modifier
                            if (entity instanceof Player) {
                                Player target = (Player) entity;
                                double modifier = AffinityModifier.calculateDamageModifier(
                                    player, target, ElementType.FIRE);
                                double modifiedDamage = damagePerTick * modifier;
                                
                                // Apply damage
                                double newHealth = Math.max(0, target.getHealth() - modifiedDamage);
                                target.setHealth(newHealth);
                                
                                // Visual feedback
                                Location targetLoc = target.getLocation();
                                if (targetLoc != null && targetLoc.getWorld() != null) {
                                    targetLoc.getWorld().spawnParticle(
                                        Particle.FLAME,
                                        targetLoc.add(0, 1, 0),
                                        10,
                                        0.3, 0.5, 0.3,
                                        0.05
                                    );
                                }
                            } else {
                                // PVE - no affinity modifier
                                double newHealth = Math.max(0, livingEntity.getHealth() - damagePerTick);
                                livingEntity.setHealth(newHealth);
                            }
                            
                            // Set entity on fire briefly
                            livingEntity.setFireTicks(20); // 1 second
                        }
                    }
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L); // Run every 10 ticks (0.5s)
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version since level doesn't affect this enchantment
        trigger(player, quality, event);
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{1, 2}; // Fire Damage, AOE/Chain
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Stormfire", "Embershade", "Voltbrand", "Deepcurrent", "CelestialSurge"};
    }
}
