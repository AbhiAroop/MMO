package com.server.enchantments.abilities.defensive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Mistveil - Water Defensive Enchantment
 * 
 * Shrouds the player in a protective mist that has a chance to deflect projectiles.
 * The deflection chance is increased by the player's water affinity.
 * 
 * Equipment: Armor, Cloaks, Rings
 * Rarity: UNCOMMON
 * Element: WATER
 */
public class Mistveil extends CustomEnchantment {
    
    private static final Map<UUID, Integer> deflectionCounters = new HashMap<>();
    
    public Mistveil() {
        super(
            "mistveil",
            "Mistveil",
            "Shrouds you in mist that deflects incoming projectiles",
            EnchantmentRarity.UNCOMMON,
            ElementType.WATER
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to all armor types
        return type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE ||
               type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS ||
               type == Material.CHAINMAIL_HELMET || type == Material.CHAINMAIL_CHESTPLATE ||
               type == Material.CHAINMAIL_LEGGINGS || type == Material.CHAINMAIL_BOOTS ||
               type == Material.IRON_HELMET || type == Material.IRON_CHESTPLATE ||
               type == Material.IRON_LEGGINGS || type == Material.IRON_BOOTS ||
               type == Material.GOLDEN_HELMET || type == Material.GOLDEN_CHESTPLATE ||
               type == Material.GOLDEN_LEGGINGS || type == Material.GOLDEN_BOOTS ||
               type == Material.DIAMOND_HELMET || type == Material.DIAMOND_CHESTPLATE ||
               type == Material.DIAMOND_LEGGINGS || type == Material.DIAMOND_BOOTS ||
               type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
               type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS;
    }
    
    @Override
    public double[] getBaseStats() {
        // [deflection_chance_percentage]
        return new double[]{25.0}; // 25% base chance
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        // Check if damage is from a projectile
        if (!(damageEvent.getDamager() instanceof Projectile)) return;
        Projectile projectile = (Projectile) damageEvent.getDamager();
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double baseChance = stats[0];
        
        // Apply affinity modifier to deflection chance
        double affinityBonus = getAffinityDeflectionBonus(player);
        double finalChance = Math.min(75.0, baseChance + affinityBonus); // Cap at 75%
        
        // Roll for deflection
        if (Math.random() * 100 > finalChance) {
            return; // Deflection failed
        }
        
        // Cancel the damage
        damageEvent.setCancelled(true);
        
        // Deflect the projectile
        deflectProjectile(player, projectile);
        
        // Track deflections
        UUID playerId = player.getUniqueId();
        int count = deflectionCounters.getOrDefault(playerId, 0) + 1;
        deflectionCounters.put(playerId, count);
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Mist particles
            loc.getWorld().spawnParticle(
                Particle.CLOUD,
                loc.add(0, 1, 0),
                15,
                0.5, 0.8, 0.5,
                0.05
            );
            
            // Water droplets
            loc.getWorld().spawnParticle(
                Particle.DRIPPING_WATER,
                loc,
                10,
                0.4, 0.6, 0.4,
                0.0
            );
            
            // Deflection sound
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.2f);
        }
        
        // Feedback message
        player.sendMessage("§6⚔ §bMistveil §fdeflected the projectile! §7(" + count + " total)");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Deflects a projectile back towards its source
     */
    private void deflectProjectile(Player player, Projectile projectile) {
        Location playerLoc = player.getLocation();
        if (playerLoc == null) return;
        
        // Get the shooter
        if (projectile.getShooter() == null) {
            // No shooter, just remove the projectile
            projectile.remove();
            return;
        }
        
        // Calculate deflection vector
        Vector deflectionVector;
        
        if (projectile.getShooter() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity shooter = (org.bukkit.entity.LivingEntity) projectile.getShooter();
            Location shooterLoc = shooter.getLocation();
            
            // Deflect back towards shooter
            deflectionVector = shooterLoc.toVector().subtract(playerLoc.toVector()).normalize();
        } else {
            // Deflect in opposite direction
            deflectionVector = projectile.getVelocity().normalize().multiply(-1);
        }
        
        // Add some randomness (mist scatters)
        deflectionVector.add(new Vector(
            (Math.random() - 0.5) * 0.3,
            (Math.random() - 0.5) * 0.3,
            (Math.random() - 0.5) * 0.3
        ));
        
        // Set new velocity (reduced speed)
        projectile.setVelocity(deflectionVector.normalize().multiply(1.2));
        
        // If it's an arrow, make it unable to be picked up
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;
            arrow.setPickupStatus(Arrow.PickupStatus.CREATIVE_ONLY);
        }
        
        // Spawn deflection particles at projectile location
        Location projLoc = projectile.getLocation();
        if (projLoc != null && projLoc.getWorld() != null) {
            projLoc.getWorld().spawnParticle(
                Particle.SPLASH,
                projLoc,
                20,
                0.2, 0.2, 0.2,
                0.3
            );
        }
    }
    
    /**
     * Calculate deflection chance bonus from player's water affinity
     * Returns a flat bonus percentage (0 to +25%)
     */
    private double getAffinityDeflectionBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        double waterAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.WATER);
        
        // Flat bonus: up to +25% at 60 affinity
        return Math.min(25.0, waterAffinity * 0.416); // 60 * 0.416 ≈ 25
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_DAMAGED;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{5}; // Defensive Response
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Whispers", "RadiantGrace"};
    }
}
