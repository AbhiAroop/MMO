package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
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
 * Ashen Veil - Fire Utility Enchantment
 * 
 * Grants invisibility when killing an enemy, wrapping the player in ash and smoke.
 * The invisibility duration is affected by the player's fire affinity.
 * 
 * Equipment: Light armor, cloaks, rings
 * Rarity: UNCOMMON
 * Element: FIRE
 */
public class AshenVeil extends CustomEnchantment {
    
    private static final Map<UUID, Long> veilCooldowns = new HashMap<>();
    private static final long VEIL_COOLDOWN = 15000; // 15 seconds between procs
    
    public AshenVeil() {
        super(
            "ashen_veil",
            "Ashen Veil",
            "Shrouds you in ash when you slay an enemy, granting brief invisibility",
            EnchantmentRarity.UNCOMMON,
            ElementType.FIRE
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to light armor (leather, chainmail)
        return type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE ||
               type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS ||
               type == Material.CHAINMAIL_HELMET || type == Material.CHAINMAIL_CHESTPLATE ||
               type == Material.CHAINMAIL_LEGGINGS || type == Material.CHAINMAIL_BOOTS;
    }
    
    @Override
    public double[] getBaseStats() {
        // [invisibility_duration_ticks]
        return new double[]{60.0}; // 3 seconds base
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDeathEvent)) return;
        EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        
        // Check if player is the killer
        Player killer = deathEvent.getEntity().getKiller();
        if (killer == null || !killer.equals(player)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (veilCooldowns.containsKey(playerId)) {
            long lastVeil = veilCooldowns.get(playerId);
            if (currentTime - lastVeil < VEIL_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Update cooldown
        veilCooldowns.put(playerId, currentTime);
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        int baseDuration = (int) Math.round(stats[0]);
        
        // Apply affinity modifier to duration (for single-player/self-buff)
        // Higher fire affinity = longer invisibility
        double affinityBonus = getAffinityDurationBonus(player);
        int finalDuration = (int) Math.round(baseDuration * affinityBonus);
        
        // Apply invisibility
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY,
            finalDuration,
            0, // Level 0 (I)
            false, // Not ambient
            false  // No particles (for true stealth)
        ));
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Ash cloud particles
            loc.getWorld().spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                loc.add(0, 1, 0),
                20,
                0.5, 0.8, 0.5,
                0.02
            );
            
            // Flame particles for the fire element
            loc.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                loc,
                15,
                0.3, 0.5, 0.3,
                0.01
            );
            
            // Subtle whoosh sound
            loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.5f, 0.7f);
        }
        
        // Feedback message with duration
        int durationSeconds = finalDuration / 20;
        player.sendMessage("§6⚔ §7Ashen Veil §fenvelops you! §7(" + durationSeconds + "s)");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version since level doesn't affect this enchantment
        trigger(player, quality, event);
    }
    
    /**
     * Calculate duration bonus from player's fire affinity
     * Returns a multiplier (1.0 baseline, up to 1.3 with max affinity)
     */
    private double getAffinityDurationBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double fireAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.FIRE);
        
        // For self-buffs, use a simple scaling: 1.0 + (affinity / 200) capped at 1.3x
        // This gives +30% duration at 60 affinity (max reasonable value)
        double bonus = Math.min(0.30, fireAffinity / 200.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_KILL;
    }
}
