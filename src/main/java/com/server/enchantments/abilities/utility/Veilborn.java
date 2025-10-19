package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Veilborn - Shadow Utility Enchantment
 * 
 * Double-tap sneak to become invisible with shadow particles
 * Duration scales with shadow affinity
 * Provides brief speed boost on activation
 * 
 * Equipment: All armor pieces (representing cloaks/rings)
 * Rarity: RARE
 */
public class Veilborn extends CustomEnchantment {
    
    private static final String VEILBORN_MODIFIER = "mmo.veilborn.movement_speed";
    
    // Double-tap sneak detection
    private static class SneakData {
        long lastSneakTime;
        BukkitRunnable particleTask;
        BukkitRunnable speedTask;
        
        SneakData(long time) {
            this.lastSneakTime = time;
        }
    }
    
    private static final Map<UUID, SneakData> playerSneaks = new HashMap<>();
    private static final Map<UUID, Long> invisCooldowns = new HashMap<>();
    private static final long DOUBLE_TAP_WINDOW = 300; // 300ms for double-tap
    private static final long INVIS_COOLDOWN = 15000; // 15 seconds
    
    public Veilborn() {
        super(
            "veilborn",
            "Veilborn",
            "Embrace the shadows, becoming invisible with a cloak of darkness",
            EnchantmentRarity.RARE,
            ElementType.SHADOW
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // All armor pieces (representing cloaks/rings)
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
        // [invisibility_duration_ticks, movement_speed_bonus]
        return new double[]{200.0, 0.05};
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
        if (invisCooldowns.containsKey(playerId)) {
            long lastInvis = invisCooldowns.get(playerId);
            if (currentTime - lastInvis < INVIS_COOLDOWN) {
                long remainingSeconds = (INVIS_COOLDOWN - (currentTime - lastInvis)) / 1000;
                player.sendMessage("§5☠ Veilborn §7on cooldown: §e" + remainingSeconds + "s");
                return;
            }
        }
        
        // Check for double-tap
        SneakData sneakData = playerSneaks.get(playerId);
        
        if (sneakData == null) {
            // First tap
            sneakData = new SneakData(currentTime);
            playerSneaks.put(playerId, sneakData);
            return;
        }
        
        long timeSinceLastSneak = currentTime - sneakData.lastSneakTime;
        
        if (timeSinceLastSneak > DOUBLE_TAP_WINDOW) {
            // Too slow, reset
            sneakData.lastSneakTime = currentTime;
            return;
        }
        
        // Double-tap detected! Activate invisibility
        sneakData.lastSneakTime = currentTime;
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        int baseInvisDuration = (int) stats[0];
        double movementSpeedBonus = stats[1];
        
        // Apply shadow affinity bonus
        double affinityBonus = getAffinityDurationBonus(player);
        int finalInvisDuration = (int) (baseInvisDuration * affinityBonus);
        
        // Apply invisibility
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY,
            finalInvisDuration,
            0,
            false,
            false,
            false // No icon
        ));
        
        // Apply movement speed boost using attribute
        applyMovementSpeedAttribute(player, movementSpeedBonus, finalInvisDuration);
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Shadow burst
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.4, 0.6, 0.4, 0.05);
            loc.getWorld().spawnParticle(Particle.SOUL, loc, 15, 0.3, 0.5, 0.3, 0.02);
            
            // Shadow whisper sound
            loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.7f, 0.6f);
            loc.getWorld().playSound(loc, Sound.BLOCK_SOUL_SAND_BREAK, 0.5f, 0.5f);
        }
        
        // Set cooldown
        invisCooldowns.put(playerId, currentTime);
        
        // Start shadow particle effect
        startShadowParticles(player, finalInvisDuration);
        
        // Feedback
        int durationSeconds = finalInvisDuration / 20;
        player.sendMessage("§5☠ Veilborn §7activated! §7(§5" + durationSeconds + "s§7)");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Apply movement speed attribute bonus
     */
    private void applyMovementSpeedAttribute(Player player, double speedBonus, int duration) {
        try {
            AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed == null) return;
            
            // Remove existing Veilborn modifier
            for (AttributeModifier mod : movementSpeed.getModifiers()) {
                if (mod.getName().equals(VEILBORN_MODIFIER)) {
                    movementSpeed.removeModifier(mod);
                }
            }
            
            // Add new modifier
            AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                VEILBORN_MODIFIER,
                speedBonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            movementSpeed.addModifier(modifier);
            
            // Schedule removal after duration
            UUID playerId = player.getUniqueId();
            SneakData sneakData = playerSneaks.get(playerId);
            
            // Cancel existing speed task
            if (sneakData != null && sneakData.speedTask != null) {
                sneakData.speedTask.cancel();
            }
            
            // Create new speed removal task
            BukkitRunnable speedTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        removeMovementSpeedAttribute(player);
                    }
                }
            };
            
            speedTask.runTaskLater(Main.getInstance(), duration);
            
            if (sneakData != null) {
                sneakData.speedTask = speedTask;
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Remove movement speed attribute modifier
     */
    private void removeMovementSpeedAttribute(Player player) {
        try {
            AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed == null) return;
            
            for (AttributeModifier mod : movementSpeed.getModifiers()) {
                if (mod.getName().equals(VEILBORN_MODIFIER)) {
                    movementSpeed.removeModifier(mod);
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Create continuous shadow particle effect while invisible
     */
    private void startShadowParticles(Player player, int duration) {
        UUID playerId = player.getUniqueId();
        SneakData sneakData = playerSneaks.get(playerId);
        
        // Cancel existing particle task
        if (sneakData != null && sneakData.particleTask != null) {
            sneakData.particleTask.cancel();
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
                    // Shadow trail particles
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.01);
                    
                    // Occasional soul particles
                    if (ticksElapsed % 10 == 0) {
                        loc.getWorld().spawnParticle(Particle.SOUL, loc, 1, 0.2, 0.3, 0.2, 0.01);
                    }
                }
                
                ticksElapsed += 2;
            }
        };
        
        particleTask.runTaskTimer(Main.getInstance(), 0L, 2L); // Every 2 ticks
        
        if (sneakData != null) {
            sneakData.particleTask = particleTask;
        }
    }
    
    /**
     * Calculate duration bonus from player's shadow affinity
     * Returns multiplier (1.0 baseline, up to 1.4x with max affinity)
     */
    private double getAffinityDurationBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double shadowAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.SHADOW);
        
        // +0.4% duration per shadow affinity, max +40% at 100 affinity
        double bonus = Math.min(0.40, shadowAffinity / 250.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.PASSIVE;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{4}; // Invisibility Effects
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"AshenVeil"};
    }
}
