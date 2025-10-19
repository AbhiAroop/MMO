package com.server.enchantments.abilities.offensive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import com.server.enchantments.elements.HybridElement;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Stormfire - Fire/Lightning Hybrid Offensive Enchantment
 * 
 * Strikes enemies with explosive lightning-infused flames
 * Creates AOE burning explosion with chain lightning
 * Fire primary (60%), Lightning secondary (40%)
 * 
 * Equipment: Swords, Axes, Maces
 * Rarity: EPIC
 */
public class Stormfire extends CustomEnchantment {
    
    private static final Map<UUID, Long> strikeCooldowns = new HashMap<>();
    private static final long STRIKE_COOLDOWN = 8000; // 8 seconds
    
    public Stormfire() {
        super(
            "stormfire",
            "Stormfire",
            "Unleash explosive lightning-infused flames upon your foes",
            EnchantmentRarity.EPIC,
            HybridElement.STORM
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Swords, axes (maces)
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [explosion_damage, burn_duration_ticks, explosion_radius]
        return new double[]{6.0, 100.0, 3.0};
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
        if (strikeCooldowns.containsKey(playerId)) {
            long lastStrike = strikeCooldowns.get(playerId);
            if (currentTime - lastStrike < STRIKE_COOLDOWN) {
                return;
            }
        }
        
        // Update cooldown
        strikeCooldowns.put(playerId, currentTime);
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseExplosionDamage = stats[0];
        int baseBurnDuration = (int) stats[1];
        double baseRadius = stats[2];
        
        // Apply hybrid affinity bonuses (60% fire, 40% lightning)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double fireBonus = affinityBonuses[0]; // Primary (60%)
        double lightningBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Fire affects damage and radius, Lightning affects burn duration
        double finalDamage = baseExplosionDamage * (1.0 + fireBonus * 0.5); // +50% max from fire
        int finalBurnDuration = (int) (baseBurnDuration * (1.0 + lightningBonus * 0.4)); // +40% max from lightning
        double finalRadius = baseRadius * (1.0 + fireBonus * 0.3); // +30% max radius from fire
        
        Location targetLoc = target.getLocation();
        
        // Create explosive lightning strike
        createStormfireExplosion(targetLoc, player, finalDamage, finalRadius, finalBurnDuration);
        
        // Visual and sound effects
        if (targetLoc != null && targetLoc.getWorld() != null) {
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
        }
        
        // Feedback
        player.sendMessage("§e⚡ §c🔥 Stormfire §7erupts with §e" + 
                          String.format("%.1f", finalDamage) + " §7explosive damage!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Create the stormfire explosion effect with burning
     */
    private void createStormfireExplosion(Location center, Player caster, 
                                         double damage, double radius, int burnDuration) {
        if (center == null || center.getWorld() == null) return;
        
        // Damage and burn nearby enemies
        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.equals(caster)) continue;
            if (entity instanceof Player) continue; // Skip players for now
            
            LivingEntity living = (LivingEntity) entity;
            
            // Apply damage
            double distance = living.getLocation().distance(center);
            double damageMultiplier = 1.0 - (distance / radius) * 0.5; // 50-100% damage based on distance
            living.damage(damage * damageMultiplier, caster);
            
            // Apply fire
            living.setFireTicks(burnDuration);
        }
        
        // Create explosion particles
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 10) { // 0.5 second animation
                    this.cancel();
                    return;
                }
                
                // Expanding fire ring
                double currentRadius = (radius * ticks) / 10.0;
                int points = (int) (currentRadius * 16);
                
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = center.getX() + Math.cos(angle) * currentRadius;
                    double z = center.getZ() + Math.sin(angle) * currentRadius;
                    double y = center.getY() + 0.5 + (Math.random() * 0.5);
                    
                    Location particleLoc = new Location(center.getWorld(), x, y, z);
                    
                    // Fire particles
                    center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    
                    // Lightning particles
                    if (Math.random() < 0.3) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                
                // Center explosion
                center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 1, 0), 
                                               5, 0.3, 0.3, 0.3, 0.1);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 
                                               10, 0.5, 0.5, 0.5, 0.05);
                
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
    
    /**
     * Get hybrid affinity bonuses [fire, lightning]
     * Fire is primary (60%), Lightning is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double fireAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.FIRE);
        double lightningAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHTNING);
        
        // 60% weight for primary (fire), 40% weight for secondary (lightning)
        // Max bonus: +0.6 from fire at 100 affinity, +0.4 from lightning at 100 affinity
        double fireBonus = Math.min(0.60, fireAffinity / 100.0 * 0.6);
        double lightningBonus = Math.min(0.40, lightningAffinity / 100.0 * 0.4);
        
        return new double[]{fireBonus, lightningBonus};
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{1, 2}; // Fire Damage, AOE/Chain Damage
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Cinderwake", "Embershade", "Voltbrand", "Deepcurrent", "CelestialSurge"};
    }
}
