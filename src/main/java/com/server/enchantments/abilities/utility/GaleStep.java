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
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
 * Gale Step - Air Utility Enchantment
 * 
 * Double-tap sneak to dash forward with the power of wind.
 * Dash distance and cooldown are affected by air affinity.
 * 
 * Equipment: Boots, Spears, Daggers
 * Rarity: UNCOMMON
 * Element: AIR
 */
public class GaleStep extends CustomEnchantment {
    
    private static final Map<UUID, Long> lastSneakTime = new HashMap<>();
    private static final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private static final long DOUBLE_TAP_WINDOW = 300; // 300ms to double-tap
    private static final long BASE_COOLDOWN = 5000; // 5 seconds base cooldown
    
    public GaleStep() {
        super(
            "gale_step",
            "Gale Step",
            "Double-tap sneak to dash forward on the wind",
            EnchantmentRarity.UNCOMMON,
            ElementType.AIR
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to boots and light weapons
        return type == Material.LEATHER_BOOTS || type == Material.CHAINMAIL_BOOTS ||
               type == Material.IRON_BOOTS || type == Material.GOLDEN_BOOTS ||
               type == Material.DIAMOND_BOOTS || type == Material.NETHERITE_BOOTS ||
               type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [dash_power]
        return new double[]{2.0}; // Base dash multiplier
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof PlayerToggleSneakEvent)) return;
        PlayerToggleSneakEvent sneakEvent = (PlayerToggleSneakEvent) event;
        
        // Only trigger on sneak press, not release
        if (!sneakEvent.isSneaking()) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check for double-tap
        Long lastSneak = lastSneakTime.get(playerId);
        if (lastSneak == null || currentTime - lastSneak > DOUBLE_TAP_WINDOW) {
            // First tap or too slow
            lastSneakTime.put(playerId, currentTime);
            return;
        }
        
        // Double-tap detected! Clear the timer
        lastSneakTime.remove(playerId);
        
        // Check cooldown
        Long lastDash = dashCooldowns.get(playerId);
        if (lastDash != null) {
            long cooldownRemaining = BASE_COOLDOWN - (currentTime - lastDash);
            if (cooldownRemaining > 0) {
                player.sendMessage("§6⚔ §7Gale Step §con cooldown! §7(" + (cooldownRemaining / 1000.0) + "s)");
                return;
            }
        }
        
        // Update cooldown
        dashCooldowns.put(playerId, currentTime);
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double dashPower = stats[0];
        
        // Apply affinity modifier to dash distance
        double affinityBonus = getAffinityDashBonus(player);
        double finalDashPower = dashPower * affinityBonus;
        
        // Perform dash
        performDash(player, finalDashPower);
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Wind particle trail
            loc.getWorld().spawnParticle(
                Particle.CLOUD,
                loc.add(0, 1, 0),
                20,
                0.3, 0.5, 0.3,
                0.15
            );
            
            loc.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                loc,
                3,
                0.5, 0.3, 0.5,
                0.0
            );
            
            // Whoosh sound
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        }
        
        player.sendMessage("§6⚔ §7Gale Step!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Perform the dash movement
     */
    private void performDash(Player player, double power) {
        // Get player's look direction
        Vector direction = player.getLocation().getDirection().normalize();
        
        // Keep horizontal movement, add slight upward component
        direction.setY(0.3);
        direction = direction.normalize().multiply(power);
        
        // Apply velocity
        player.setVelocity(direction);
        
        // Create particle trail behind player
        Location startLoc = player.getLocation();
        for (int i = 0; i < 5; i++) {
            Location particleLoc = startLoc.clone().subtract(direction.clone().multiply(i * 0.5));
            if (particleLoc.getWorld() != null) {
                particleLoc.getWorld().spawnParticle(
                    Particle.CLOUD,
                    particleLoc.add(0, 1, 0),
                    3,
                    0.2, 0.2, 0.2,
                    0.05
                );
            }
        }
    }
    
    /**
     * Calculate dash bonus from player's air affinity
     * Returns a multiplier (1.0 baseline, up to 1.4 with max affinity)
     */
    private double getAffinityDashBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double airAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.AIR);
        
        // Dash distance scaling: 1.0 + (affinity / 150) capped at 1.4x
        double bonus = Math.min(0.40, airAffinity / 150.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.PASSIVE; // Uses custom sneak listener
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{7}; // Movement Abilities
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"MistborneTempest"};
    }
}
