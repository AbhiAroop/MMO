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
 * Celestial Surge - Light/Lightning Hybrid Offensive Enchantment
 * 
 * Calls down divine lightning that purges and stuns enemies
 * Provides brief regeneration to the caster after successful strikes
 * Light primary (60%), Lightning secondary (40%)
 * 
 * Equipment: Swords, Tridents, Crossbows
 * Rarity: EPIC
 */
public class CelestialSurge extends CustomEnchantment {
    
    private static final Map<UUID, Long> surgeCooldowns = new HashMap<>();
    private static final long SURGE_COOLDOWN = 10000; // 10 seconds
    
    public CelestialSurge() {
        super(
            "celestial_surge",
            "Celestial Surge",
            "Summon divine lightning to smite your enemies with righteous fury",
            EnchantmentRarity.EPIC,
            HybridElement.RADIANCE
        );
    }
    
    @Override
    public int getMaxLevel() {
        return 8; // Epic hybrid - divine power
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Swords, tridents, crossbows
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.TRIDENT || type == Material.CROSSBOW;
    }
    
    @Override
    public double[] getBaseStats() {
        // [lightning_damage, stun_duration_ticks, regen_duration_ticks]
        return new double[]{8.0, 40.0, 60.0};
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
        if (surgeCooldowns.containsKey(playerId)) {
            long lastSurge = surgeCooldowns.get(playerId);
            if (currentTime - lastSurge < SURGE_COOLDOWN) {
                return;
            }
        }
        
        // Update cooldown
        surgeCooldowns.put(playerId, currentTime);
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseLightningDamage = stats[0];
        int baseStunDuration = (int) stats[1];
        int baseRegenDuration = (int) stats[2];
        
        // Apply hybrid affinity bonuses (60% light, 40% lightning)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double lightBonus = affinityBonuses[0]; // Primary (60%)
        double lightningBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Light affects regen, Lightning affects damage and stun
        double finalLightningDamage = baseLightningDamage * (1.0 + lightningBonus * 0.6); // +60% max from lightning
        int finalStunDuration = (int) (baseStunDuration * (1.0 + lightningBonus * 0.4)); // +40% max from lightning
        int finalRegenDuration = (int) (baseRegenDuration * (1.0 + lightBonus * 0.5)); // +50% max from light
        int regenAmplifier = (int) (lightBonus * 3); // 0-3 amplifier based on light affinity
        
        // Apply lightning damage
        target.damage(finalLightningDamage, player);
        
        // Apply stun (slowness + jump fatigue)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, finalStunDuration, 4, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, finalStunDuration, 250, false, true, true)); // Negative jump = can't jump
        
        // Give caster regeneration
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, finalRegenDuration, 
                                                regenAmplifier, false, true, true));
        
        // Create celestial strike effect
        createCelestialStrike(target.getLocation());
        
        // Visual and sound effects
        Location targetLoc = target.getLocation();
        if (targetLoc != null && targetLoc.getWorld() != null) {
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f);
            targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.8f);
        }
        
        // Feedback
        player.sendMessage("§e✦ §6Celestial Surge §7struck §f" + target.getName() + 
                          " §7for §e" + String.format("%.1f", finalLightningDamage) + " §7divine damage!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Create divine lightning strike visual
     */
    private void createCelestialStrike(Location center) {
        if (center == null || center.getWorld() == null) return;
        
        // Lightning beam from sky
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 8) {
                    this.cancel();
                    return;
                }
                
                // Descending beam
                double startY = center.getY() + 10.0 - (ticks * 1.5);
                double endY = center.getY();
                
                int points = 8;
                for (int i = 0; i <= points; i++) {
                    double ratio = (double) i / points;
                    double y = startY + (endY - startY) * ratio;
                    
                    Location beamLoc = center.clone();
                    beamLoc.setY(y);
                    
                    // Golden light particles
                    center.getWorld().spawnParticle(Particle.GLOW, beamLoc, 2, 0.1, 0, 0.1, 0);
                    center.getWorld().spawnParticle(Particle.END_ROD, beamLoc, 1, 0.05, 0, 0.05, 0.02);
                    
                    // Lightning particles
                    if (Math.random() < 0.5) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamLoc, 1, 0.15, 0, 0.15, 0.05);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
        
        // Ground impact burst
        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.15);
        center.getWorld().spawnParticle(Particle.GLOW, center.clone().add(0, 0.5, 0), 30, 0.8, 0.8, 0.8, 0.1);
        
        // Radial burst
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI * i) / 16;
            double x = center.getX() + Math.cos(angle) * 2.0;
            double z = center.getZ() + Math.sin(angle) * 2.0;
            double y = center.getY() + 0.5;
            
            Location burstLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.END_ROD, burstLoc, 1, 0, 0, 0, 0.1);
        }
    }
    
    /**
     * Get hybrid affinity bonuses [light, lightning]
     * Light is primary (60%), Lightning is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        double lightningAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHTNING);
        
        // 60% weight for primary (light), 40% weight for secondary (lightning)
        double lightBonus = Math.min(0.60, lightAffinity / 100.0 * 0.6);
        double lightningBonus = Math.min(0.40, lightningAffinity / 100.0 * 0.4);
        
        return new double[]{lightBonus, lightningBonus};
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
        return new String[]{"Voltbrand", "Deepcurrent", "Cinderwake", "Stormfire"};
    }
}
