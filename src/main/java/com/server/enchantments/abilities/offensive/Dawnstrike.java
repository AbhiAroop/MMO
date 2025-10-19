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

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Dawnstrike - Light Offensive Enchantment
 * 
 * Strikes enemies with radiant light, applying blindness
 * Chance to blind increases with light affinity
 * Creates brilliant light burst on successful blind
 * 
 * Equipment: Swords, Maces (axes)
 * Rarity: RARE
 */
public class Dawnstrike extends CustomEnchantment {
    
    private static final Map<UUID, Long> blindCooldowns = new HashMap<>();
    private static final long BLIND_COOLDOWN = 3000; // 3 seconds between blind procs
    
    public Dawnstrike() {
        super(
            "dawnstrike",
            "Dawnstrike",
            "Strike with radiant light, blinding enemies with divine brilliance",
            EnchantmentRarity.RARE,
            ElementType.LIGHT
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Swords and maces (axes)
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [blind_chance, blind_duration_ticks]
        return new double[]{0.25, 60.0};
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
        if (blindCooldowns.containsKey(playerId)) {
            long lastBlind = blindCooldowns.get(playerId);
            if (currentTime - lastBlind < BLIND_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseBlindChance = stats[0];
        int baseDuration = (int) stats[1];
        
        // Apply light affinity bonus to blind chance
        double finalBlindChance = baseBlindChance + getAffinityBlindBonus(player);
        
        // Roll for blind proc
        if (Math.random() > finalBlindChance) {
            return; // No blind this time
        }
        
        // Apply light affinity bonus to duration
        double affinityDurationBonus = getAffinityDurationBonus(player);
        int finalDuration = (int) (baseDuration * affinityDurationBonus);
        
        // Apply blindness effect
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS,
            finalDuration,
            0, // Blindness I
            false,
            true,
            true
        ));
        
        // Visual and sound effects
        createLightBurst(target.getLocation());
        Location loc = target.getEyeLocation();
        if (loc != null && loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.3f, 1.8f);
        }
        
        // Set cooldown
        blindCooldowns.put(playerId, currentTime);
        
        // Feedback
        int durationSeconds = finalDuration / 20;
        player.sendMessage("§e☀ Dawnstrike §7blinded §f" + target.getName() + " §7for §e" + durationSeconds + "s§7!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Create radiant light burst effect
     */
    private void createLightBurst(Location center) {
        if (center == null || center.getWorld() == null) return;
        
        // Central explosion of light
        center.getWorld().spawnParticle(Particle.GLOW, center.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        center.getWorld().spawnParticle(Particle.END_ROD, center, 30, 0.3, 0.5, 0.3, 0.15);
        center.getWorld().spawnParticle(Particle.FIREWORK, center, 20, 0.4, 0.4, 0.4, 0.1);
        
        // Radial burst
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI * i) / 12;
            double x = center.getX() + Math.cos(angle) * 1.5;
            double z = center.getZ() + Math.sin(angle) * 1.5;
            double y = center.getY() + 0.5;
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Upward spiral
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            double radius = 0.8 - (i * 0.1);
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + (i * 0.3);
            
            Location spiralLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.GLOW, spiralLoc, 2, 0.05, 0.05, 0.05, 0);
        }
    }
    
    /**
     * Calculate blind chance bonus from player's light affinity
     * Returns additional chance (0 to 0.30, i.e., up to +30%)
     */
    private double getAffinityBlindBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        
        // +0.3% blind chance per light affinity, max +30% at 100 affinity
        return Math.min(0.30, lightAffinity / 333.33);
    }
    
    /**
     * Calculate duration bonus from player's light affinity
     * Returns multiplier (1.0 baseline, up to 1.5x with max affinity)
     */
    private double getAffinityDurationBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        
        // +0.5% duration per light affinity, max +50% at 100 affinity
        double bonus = Math.min(0.50, lightAffinity / 200.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
}
