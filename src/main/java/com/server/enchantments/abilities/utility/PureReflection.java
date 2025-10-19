package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Pure Reflection - Water/Light Hybrid Utility Enchantment
 * 
 * Creates a sacred water barrier that absorbs damage and purifies debuffs
 * Provides damage absorption and cleansing to nearby allies
 * Water primary (60%), Light secondary (40%)
 * 
 * Equipment: All armor pieces (chestplate preferred)
 * Rarity: EPIC
 */
public class PureReflection extends CustomEnchantment {
    
    private static final Map<UUID, Long> barrierCooldowns = new HashMap<>();
    private static final Map<UUID, Double> damageAbsorbed = new HashMap<>();
    private static final long BARRIER_COOLDOWN = 15000; // 15 seconds
    
    public PureReflection() {
        super(
            "pure_reflection",
            "Pure Reflection",
            "Summon sacred waters that shield and purify you and your allies",
            EnchantmentRarity.EPIC,
            HybridElement.PURITY
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // All armor pieces
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
        // [damage_absorption, barrier_duration_ticks, cleanse_radius]
        return new double[]{10.0, 100.0, 5.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageEvent)) return;
        EntityDamageEvent damageEvent = (EntityDamageEvent) event;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (barrierCooldowns.containsKey(playerId)) {
            long lastBarrier = barrierCooldowns.get(playerId);
            if (currentTime - lastBarrier < BARRIER_COOLDOWN) {
                // Still within barrier duration - absorb damage
                if (damageAbsorbed.containsKey(playerId)) {
                    double absorbed = damageAbsorbed.get(playerId);
                    double incomingDamage = damageEvent.getDamage();
                    
                    if (absorbed > 0) {
                        double damageReduction = Math.min(absorbed, incomingDamage);
                        damageEvent.setDamage(incomingDamage - damageReduction);
                        damageAbsorbed.put(playerId, absorbed - damageReduction);
                        
                        // Visual feedback
                        Location loc = player.getLocation();
                        if (loc != null && loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.SPLASH, loc.add(0, 1, 0), 
                                                        10, 0.5, 0.5, 0.5, 0.1);
                            loc.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.5f);
                        }
                        
                        player.sendMessage("§b✧ Pure Reflection §7absorbed §c" + 
                                         String.format("%.1f", damageReduction) + " §7damage!");
                    }
                }
                return;
            }
        }
        
        // Activate barrier
        barrierCooldowns.put(playerId, currentTime);
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseDamageAbsorption = stats[0];
        int baseBarrierDuration = (int) stats[1];
        double baseCleanseRadius = stats[2];
        
        // Apply hybrid affinity bonuses (60% water, 40% light)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double waterBonus = affinityBonuses[0]; // Primary (60%)
        double lightBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Water affects absorption and duration, Light affects cleansing and radius
        double finalAbsorption = baseDamageAbsorption * (1.0 + waterBonus * 0.6); // +60% max from water
        int finalDuration = (int) (baseBarrierDuration * (1.0 + waterBonus * 0.4)); // +40% max from water
        double finalRadius = baseCleanseRadius * (1.0 + lightBonus * 0.4); // +40% max radius from light
        
        // Set absorption amount
        damageAbsorbed.put(playerId, finalAbsorption);
        
        // Apply absorption and regeneration effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, finalDuration, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, finalDuration, 0, false, true, true));
        
        // Cleanse debuffs from player
        cleanseDebuffs(player);
        
        // Cleanse nearby allies
        int alliesCleansed = 0;
        for (Entity entity : player.getNearbyEntities(finalRadius, finalRadius, finalRadius)) {
            if (!(entity instanceof Player)) continue;
            Player ally = (Player) entity;
            
            cleanseDebuffs(ally);
            
            // Give allies smaller absorption
            ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, finalDuration / 2, 0, false, true, true));
            
            // Visual effect on ally
            Location allyLoc = ally.getLocation();
            if (allyLoc != null && allyLoc.getWorld() != null) {
                allyLoc.getWorld().spawnParticle(Particle.GLOW, allyLoc.add(0, 1, 0), 15, 0.5, 0.8, 0.5, 0.05);
            }
            
            ally.sendMessage("§b✧ Pure Reflection §7cleansed and shielded you!");
            alliesCleansed++;
        }
        
        // Create barrier effect
        createPureBarrier(player, finalDuration, finalRadius);
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ITEM_BUCKET_FILL, 0.8f, 1.3f);
        }
        
        // Feedback
        player.sendMessage("§b✧ Pure Reflection §7activated! §7(§b" + (finalDuration / 20) + "s§7)" +
                          (alliesCleansed > 0 ? " §7Protected §b" + alliesCleansed + " §7allies!" : ""));
        
        // Clear absorption after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                damageAbsorbed.remove(playerId);
            }
        }.runTaskLater(Main.getInstance(), finalDuration);
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Remove negative effects from player
     */
    private void cleanseDebuffs(Player player) {
        // Remove common debuffs
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        
        // Extinguish fire
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }
    
    /**
     * Create sacred water barrier visual
     */
    private void createPureBarrier(Player player, int duration, double radius) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    this.cancel();
                    return;
                }
                
                // Rotating water/light sphere
                int points = 16;
                double angleOffset = (ticks * 0.15) % (2 * Math.PI);
                
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i / points) + angleOffset;
                    double x = loc.getX() + Math.cos(angle) * radius;
                    double z = loc.getZ() + Math.sin(angle) * radius;
                    double y = loc.getY() + 1.0 + (Math.sin(angle * 2 + ticks * 0.2) * 0.5);
                    
                    Location barrierLoc = new Location(loc.getWorld(), x, y, z);
                    
                    // Water particles
                    loc.getWorld().spawnParticle(Particle.SPLASH, barrierLoc, 1, 0, 0, 0, 0);
                    
                    // Light particles
                    if (Math.random() < 0.3) {
                        loc.getWorld().spawnParticle(Particle.GLOW, barrierLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
                
                // Central pillar every second
                if (ticks % 20 == 0) {
                    for (int i = 0; i < 5; i++) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, i * 0.5, 0), 
                                                    2, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                
                ticks += 3;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 3L);
    }
    
    /**
     * Get hybrid affinity bonuses [water, light]
     * Water is primary (60%), Light is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double waterAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.WATER);
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        
        // 60% weight for primary (water), 40% weight for secondary (light)
        double waterBonus = Math.min(0.60, waterAffinity / 100.0 * 0.6);
        double lightBonus = Math.min(0.40, lightAffinity / 100.0 * 0.4);
        
        return new double[]{waterBonus, lightBonus};
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_DAMAGED;
    }
}
