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
import org.bukkit.util.Vector;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.AffinityModifier;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Deepcurrent - Water Offensive Enchantment
 * 
 * Unleashes a powerful tide wave on hit that knocks back enemies in a cone.
 * The knockback strength is affected by the player's water affinity.
 * 
 * Equipment: Greatswords, Hammers, Gauntlets (heavy weapons)
 * Rarity: RARE
 * Element: WATER
 */
public class Deepcurrent extends CustomEnchantment {
    
    private static final Map<UUID, Long> tideCooldowns = new HashMap<>();
    private static final long TIDE_COOLDOWN = 8000; // 8 seconds between waves
    
    public Deepcurrent() {
        super(
            "deepcurrent",
            "Deepcurrent",
            "Unleashes a tide wave that sweeps enemies away",
            EnchantmentRarity.RARE,
            ElementType.WATER
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to heavy weapons: swords (greatswords), axes (hammers), tridents
        return type == Material.NETHERITE_SWORD || type == Material.DIAMOND_SWORD ||
               type == Material.IRON_SWORD ||
               type == Material.NETHERITE_AXE || type == Material.DIAMOND_AXE ||
               type == Material.IRON_AXE ||
               type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [knockback_strength, cone_range_blocks]
        return new double[]{2.5, 5.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) damageEvent.getEntity();
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (tideCooldowns.containsKey(playerId)) {
            long lastTide = tideCooldowns.get(playerId);
            if (currentTime - lastTide < TIDE_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Update cooldown
        tideCooldowns.put(playerId, currentTime);
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double baseKnockback = stats[0];
        double coneRange = stats[1];
        
        // Apply affinity modifier to knockback strength
        double affinityBonus = getAffinityKnockbackBonus(player);
        double finalKnockback = baseKnockback * affinityBonus;
        
        // Create tide wave effect
        createTideWave(player, target, finalKnockback, coneRange);
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.5f, 0.8f);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 1.2f, 0.9f);
        }
        
        player.sendMessage("§6⚔ §bDeepcurrent §7surges forth!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Creates a tide wave that knocks back enemies in a cone
     */
    private void createTideWave(Player player, LivingEntity initialTarget, double knockback, double range) {
        Location playerLoc = player.getLocation();
        if (playerLoc == null || playerLoc.getWorld() == null) return;
        
        Vector playerDirection = playerLoc.getDirection().normalize();
        
        // Spawn water particles in a cone shape
        for (int i = 0; i < 15; i++) {
            double distance = (i / 15.0) * range;
            double spread = (i / 15.0) * 2.0; // Cone widens as it goes
            
            for (int j = -2; j <= 2; j++) {
                Vector offset = playerDirection.clone().multiply(distance);
                offset.add(new Vector(j * spread * 0.3, 0, j * spread * 0.3));
                
                Location particleLoc = playerLoc.clone().add(offset).add(0, 1, 0);
                
                playerLoc.getWorld().spawnParticle(
                    Particle.SPLASH,
                    particleLoc,
                    3,
                    0.2, 0.2, 0.2,
                    0.1
                );
                
                if (i % 3 == 0) {
                    playerLoc.getWorld().spawnParticle(
                        Particle.BUBBLE_POP,
                        particleLoc,
                        2,
                        0.1, 0.1, 0.1,
                        0.05
                    );
                }
            }
        }
        
        // Find and knockback entities in the cone
        for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;
            
            LivingEntity livingEntity = (LivingEntity) entity;
            Vector toEntity = livingEntity.getLocation().toVector().subtract(playerLoc.toVector());
            
            // Check if entity is in front of player (cone check)
            double angle = playerDirection.angle(toEntity);
            if (angle > Math.PI / 3) continue; // 60-degree cone
            
            // Calculate knockback direction (away from player)
            Vector knockbackVec = toEntity.normalize().multiply(knockback);
            knockbackVec.setY(0.4); // Add upward component
            
            // Apply knockback
            livingEntity.setVelocity(knockbackVec);
            
            // Apply damage with affinity modifier for PVP
            if (entity instanceof Player) {
                Player targetPlayer = (Player) entity;
                double modifier = AffinityModifier.calculateDamageModifier(player, targetPlayer, ElementType.WATER);
                double damage = 3.0 * modifier; // Base 3 damage
                
                double newHealth = Math.max(0, targetPlayer.getHealth() - damage);
                targetPlayer.setHealth(newHealth);
                
                // Send feedback
                String feedback = AffinityModifier.getAffinityFeedback(modifier);
                targetPlayer.sendMessage("§b⚔ Swept by Deepcurrent! " + feedback);
            } else {
                // PVE damage
                double damage = 3.0;
                double newHealth = Math.max(0, livingEntity.getHealth() - damage);
                livingEntity.setHealth(newHealth);
            }
            
            // Particle effect on hit entity
            Location entityLoc = livingEntity.getLocation();
            if (entityLoc != null && entityLoc.getWorld() != null) {
                entityLoc.getWorld().spawnParticle(
                    Particle.SPLASH,
                    entityLoc.add(0, 1, 0),
                    15,
                    0.5, 0.5, 0.5,
                    0.2
                );
            }
        }
    }
    
    /**
     * Calculate knockback bonus from player's water affinity
     * Returns a multiplier (1.0 baseline, up to 1.3 with max affinity)
     */
    private double getAffinityKnockbackBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double waterAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.WATER);
        
        // Knockback scaling: 1.0 + (affinity / 200) capped at 1.3x
        double bonus = Math.min(0.30, waterAffinity / 200.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
}
