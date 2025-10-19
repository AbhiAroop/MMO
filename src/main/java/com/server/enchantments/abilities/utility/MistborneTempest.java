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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
 * Mistborne Tempest - Water/Air Hybrid Utility Enchantment
 * 
 * Transform into mist and dash forward with incredible speed
 * Provides brief water breathing and fall damage immunity
 * Water primary (60%), Air secondary (40%)
 * 
 * Equipment: All armor pieces (boots preferred)
 * Rarity: EPIC
 */
public class MistborneTempest extends CustomEnchantment {
    
    private static class DashData {
        long lastDashTime;
        BukkitRunnable particleTask;
        
        DashData(long time) {
            this.lastDashTime = time;
        }
    }
    
    private static final Map<UUID, DashData> playerDashes = new HashMap<>();
    private static final long DASH_COOLDOWN = 12000; // 12 seconds
    
    public MistborneTempest() {
        super(
            "mistborne_tempest",
            "Mistborne Tempest",
            "Become one with the mist, dashing through air and water alike",
            EnchantmentRarity.EPIC,
            HybridElement.MIST
        );
    }
    
    @Override
    public int getMaxLevel() {
        return 7; // Epic hybrid utility
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
        // [dash_power, buff_duration_ticks]
        return new double[]{2.5, 60.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof PlayerToggleSneakEvent)) return;
        PlayerToggleSneakEvent sneakEvent = (PlayerToggleSneakEvent) event;
        
        // Only trigger on sneak down
        if (!sneakEvent.isSneaking()) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        DashData dashData = playerDashes.get(playerId);
        if (dashData != null) {
            long timeSinceLastDash = currentTime - dashData.lastDashTime;
            if (timeSinceLastDash < DASH_COOLDOWN) {
                long remainingSeconds = (DASH_COOLDOWN - timeSinceLastDash) / 1000;
                player.sendMessage("§b🌫 Mistborne Tempest §7on cooldown: §e" + remainingSeconds + "s");
                return;
            }
        } else {
            dashData = new DashData(currentTime);
            playerDashes.put(playerId, dashData);
        }
        
        // Update cooldown
        dashData.lastDashTime = currentTime;
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseDashPower = stats[0];
        int baseBuffDuration = (int) stats[1];
        
        // Apply hybrid affinity bonuses (60% water, 40% air)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double waterBonus = affinityBonuses[0]; // Primary (60%)
        double airBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Water affects buff duration, Air affects dash power
        double finalDashPower = baseDashPower * (1.0 + airBonus * 0.6); // +60% max from air
        int finalBuffDuration = (int) (baseBuffDuration * (1.0 + waterBonus * 0.5)); // +50% max from water
        
        // Apply dash velocity
        Vector direction = player.getLocation().getDirection();
        direction.normalize();
        direction.multiply(finalDashPower);
        direction.setY(direction.getY() + 0.5); // Slight upward boost
        player.setVelocity(direction);
        
        // Apply buffs
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, finalBuffDuration, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, finalBuffDuration, 1, false, false, false)); // Fall damage immunity
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Mist burst
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.SPLASH, loc, 20, 0.4, 0.6, 0.4, 0.2);
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.3, 0.5, 0.3, 0);
            
            // Wind sound
            loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
            loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 1.2f);
        }
        
        // Start mist trail
        startMistTrail(player, finalBuffDuration);
        
        // Feedback
        int durationSeconds = finalBuffDuration / 20;
        player.sendMessage("§b🌫 Mistborne Tempest §7activated! §7(§b" + durationSeconds + "s§7)");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Create mist trail while dash is active
     */
    private void startMistTrail(Player player, int duration) {
        UUID playerId = player.getUniqueId();
        DashData dashData = playerDashes.get(playerId);
        
        // Cancel existing particle task
        if (dashData != null && dashData.particleTask != null) {
            dashData.particleTask.cancel();
        }
        
        // Create new particle task
        BukkitRunnable particleTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksElapsed >= duration) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                if (loc != null && loc.getWorld() != null) {
                    // Mist trail
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);
                    loc.getWorld().spawnParticle(Particle.SPLASH, loc, 2, 0.2, 0.3, 0.2, 0.05);
                    
                    // Occasional sweep
                    if (ticksElapsed % 5 == 0) {
                        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                    }
                }
                
                ticksElapsed += 2;
            }
        };
        
        particleTask.runTaskTimer(Main.getInstance(), 0L, 2L);
        
        if (dashData != null) {
            dashData.particleTask = particleTask;
        }
    }
    
    /**
     * Get hybrid affinity bonuses [water, air]
     * Water is primary (60%), Air is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double waterAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.WATER);
        double airAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.AIR);
        
        // 60% weight for primary (water), 40% weight for secondary (air)
        double waterBonus = Math.min(0.60, waterAffinity / 100.0 * 0.6);
        double airBonus = Math.min(0.40, airAffinity / 100.0 * 0.4);
        
        return new double[]{waterBonus, airBonus};
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.PASSIVE;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{7}; // Movement Abilities
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"GaleStep"};
    }
}
