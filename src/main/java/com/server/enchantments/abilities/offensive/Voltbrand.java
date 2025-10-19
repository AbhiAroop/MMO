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
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.EnchantmentDamageUtil;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Voltbrand - Lightning Offensive Enchantment
 * 
 * Chain lightning that bounces between nearby enemies
 * Each bounce deals reduced damage
 * Affected by lightning affinity for increased chain count
 * 
 * Equipment: Swords, Spears, Crossbows
 * Rarity: RARE
 */
public class Voltbrand extends CustomEnchantment {
    
    private static final Map<UUID, Long> chainCooldowns = new HashMap<>();
    private static final long CHAIN_COOLDOWN = 6000; // 6 seconds
    
    public Voltbrand() {
        super(
            "voltbrand",
            "Voltbrand",
            "Strikes enemies with chain lightning that bounces between foes",
            EnchantmentRarity.RARE,
            ElementType.LIGHTNING
        );
    }
    
    @Override
    public int getMaxLevel() {
        return 7; // Powerful chain mechanic
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to swords, tridents (spears), and crossbows
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.TRIDENT || type == Material.CROSSBOW;
    }
    
    @Override
    public double[] getBaseStats() {
        // [chain_count, damage_multiplier, chain_range]
        return new double[]{3.0, 0.7, 8.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity initialTarget = (LivingEntity) damageEvent.getEntity();
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (chainCooldowns.containsKey(playerId)) {
            long lastChain = chainCooldowns.get(playerId);
            if (currentTime - lastChain < CHAIN_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Update cooldown
        chainCooldowns.put(playerId, currentTime);
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        int baseChainCount = (int) Math.round(stats[0]);
        double damageMultiplier = stats[1];
        double chainRange = stats[2];
        
        // Apply affinity modifier for chain count
        int finalChainCount = Math.min(baseChainCount + getAffinityChainBonus(player), 5);
        
        // Get initial damage
        double initialDamage = damageEvent.getFinalDamage();
        
        // Start chain lightning
        executeChainLightning(player, initialTarget, initialDamage, finalChainCount, damageMultiplier, chainRange);
        
        // Feedback
        player.sendMessage("§e⚡ Voltbrand §7chains through §e" + finalChainCount + " §7enemies!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Execute the chain lightning effect
     */
    private void executeChainLightning(Player caster, LivingEntity initialTarget, 
                                      double initialDamage, int chainCount, 
                                      double damageMultiplier, double range) {
        
        Set<UUID> hitEntities = new HashSet<>();
        hitEntities.add(initialTarget.getUniqueId());
        
        // Start the chain
        new BukkitRunnable() {
            private int currentChain = 0;
            private LivingEntity currentTarget = initialTarget;
            private double currentDamage = initialDamage * 0.5; // Chain starts at 50% of initial hit
            
            @Override
            public void run() {
                if (currentChain >= chainCount || currentTarget == null || currentTarget.isDead()) {
                    this.cancel();
                    return;
                }
                
                // Find next target
                LivingEntity nextTarget = findNearestTarget(currentTarget.getLocation(), 
                                                            range, hitEntities, caster);
                
                if (nextTarget == null) {
                    this.cancel();
                    return;
                }
                
                // Apply damage through unified system for proper affinity integration
                EntityDamageByEntityEvent chainEvent = new EntityDamageByEntityEvent(
                    caster, nextTarget, org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC, 0.0);
                EnchantmentDamageUtil.addBonusDamageToEvent(chainEvent, currentDamage, ElementType.LIGHTNING);
                
                // Apply the damage
                double finalDamage = chainEvent.getFinalDamage();
                double newHealth = Math.max(0, nextTarget.getHealth() - finalDamage);
                nextTarget.setHealth(newHealth);
                
                // Visual and sound effects
                createLightningArc(currentTarget.getLocation().add(0, 1, 0), 
                                  nextTarget.getLocation().add(0, 1, 0));
                currentTarget.getWorld().playSound(currentTarget.getLocation(), 
                                                  Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 
                                                  0.5f, 1.5f);
                
                // Mark as hit
                hitEntities.add(nextTarget.getUniqueId());
                
                // Prepare for next bounce
                currentTarget = nextTarget;
                currentDamage *= damageMultiplier;
                currentChain++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L); // 5 tick delay between chains
    }
    
    /**
     * Find the nearest valid target for chaining
     */
    private LivingEntity findNearestTarget(Location from, double range, 
                                          Set<UUID> excludeEntities, Player caster) {
        LivingEntity nearest = null;
        double nearestDistance = range;
        
        for (Entity entity : from.getWorld().getNearbyEntities(from, range, range, range)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            
            LivingEntity living = (LivingEntity) entity;
            
            // Skip if already hit, is the caster, or is a player
            if (excludeEntities.contains(living.getUniqueId()) || 
                living.equals(caster) ||
                living instanceof Player) {
                continue;
            }
            
            double distance = from.distance(living.getLocation());
            if (distance < nearestDistance) {
                nearest = living;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Create visual lightning arc between two points
     */
    private void createLightningArc(Location start, Location end) {
        double distance = start.distance(end);
        int points = (int) (distance * 3); // 3 particles per block
        
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            
            // Linear interpolation with slight random offset for lightning effect
            double x = start.getX() + (end.getX() - start.getX()) * ratio;
            double y = start.getY() + (end.getY() - start.getY()) * ratio;
            double z = start.getZ() + (end.getZ() - start.getZ()) * ratio;
            
            // Add random jitter for lightning effect
            x += (Math.random() - 0.5) * 0.3;
            y += (Math.random() - 0.5) * 0.3;
            z += (Math.random() - 0.5) * 0.3;
            
            Location particleLoc = new Location(start.getWorld(), x, y, z);
            start.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
            start.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Calculate bonus chains from player's lightning affinity
     * Returns number of additional chains (0-2)
     */
    private int getAffinityChainBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0;
        
        double lightningAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHTNING);
        
        // +1 chain every 25 affinity, max +2 chains
        return Math.min((int) (lightningAffinity / 25.0), 2);
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{2}; // AOE/Chain Damage
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Deepcurrent", "Cinderwake", "Stormfire", "CelestialSurge"};
    }
}
