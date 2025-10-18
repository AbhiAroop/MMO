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
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Burdened Stone - Earth Offensive Enchantment
 * 
 * Each hit stacks a slowing debuff on the target, weighing them down like stone.
 * The slow intensity increases with earth affinity.
 * 
 * Equipment: Hammers, Maces, Shields (heavy weapons)
 * Rarity: UNCOMMON
 * Element: EARTH
 */
public class BurdenedStone extends CustomEnchantment {
    
    private static final Map<UUID, Integer> slowStacks = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final int MAX_STACKS = 5;
    private static final long STACK_DECAY_TIME = 4000; // 4 seconds before stacks start decaying
    
    public BurdenedStone() {
        super(
            "burdened_stone",
            "Burdened Stone",
            "Each strike weighs enemies down, stacking slows",
            EnchantmentRarity.UNCOMMON,
            ElementType.EARTH
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to heavy weapons and shields
        return type == Material.NETHERITE_AXE || type == Material.DIAMOND_AXE ||
               type == Material.IRON_AXE || type == Material.STONE_AXE ||
               type == Material.MACE || // Mace if available
               type == Material.SHIELD;
    }
    
    @Override
    public double[] getBaseStats() {
        // [slow_duration_ticks, slow_amplifier_per_stack]
        return new double[]{80.0, 1.0}; // 4 seconds duration, Slowness I per stack
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) damageEvent.getEntity();
        
        UUID targetId = target.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if stacks should decay
        if (lastHitTime.containsKey(targetId)) {
            long timeSinceLastHit = currentTime - lastHitTime.get(targetId);
            if (timeSinceLastHit > STACK_DECAY_TIME) {
                // Decay one stack
                int currentStacks = slowStacks.getOrDefault(targetId, 0);
                if (currentStacks > 0) {
                    slowStacks.put(targetId, currentStacks - 1);
                }
            }
        }
        
        // Add new stack
        int newStacks = Math.min(MAX_STACKS, slowStacks.getOrDefault(targetId, 0) + 1);
        slowStacks.put(targetId, newStacks);
        lastHitTime.put(targetId, currentTime);
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        int duration = (int) Math.round(stats[0]);
        
        // Apply affinity modifier to slow strength
        double affinityBonus = getAffinitySlowBonus(player);
        int slowAmplifier = (int) Math.round((newStacks - 1) * affinityBonus); // Slowness starts at 0 (I)
        
        // Apply slowness effect
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            duration,
            slowAmplifier,
            false,
            true // Show particles
        ));
        
        // Apply mining fatigue at max stacks for extra weight
        if (newStacks >= MAX_STACKS) {
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                duration,
                0,
                false,
                true
            ));
        }
        
        // Visual and sound effects
        Location loc = target.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Stone/dirt particles
            loc.getWorld().spawnParticle(
                Particle.BLOCK,
                loc.add(0, 1, 0),
                10 + (newStacks * 3),
                0.3, 0.5, 0.3,
                0.1,
                Material.STONE.createBlockData()
            );
            
            // Heavier sound with more stacks
            float pitch = 1.0f - (newStacks * 0.1f);
            loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 0.8f, pitch);
            
            if (newStacks >= MAX_STACKS) {
                loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.8f);
            }
        }
        
        // Feedback message
        String stackBar = "▮".repeat(newStacks) + "▯".repeat(MAX_STACKS - newStacks);
        player.sendMessage("§6⚔ §7Burdened Stone §8[" + stackBar + "§8]");
        
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            targetPlayer.sendMessage("§8⚔ Stone weighs you down! §7(" + newStacks + "/" + MAX_STACKS + ")");
        }
        
        // Schedule stack cleanup after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                long timeSinceLastHit = System.currentTimeMillis() - lastHitTime.getOrDefault(targetId, 0L);
                if (timeSinceLastHit > duration * 50) { // Convert ticks to ms
                    slowStacks.remove(targetId);
                    lastHitTime.remove(targetId);
                }
            }
        }.runTaskLater(Main.getInstance(), duration + 20L);
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Calculate slow bonus from player's earth affinity
     * Returns a multiplier (1.0 baseline, up to 1.5 with max affinity)
     */
    private double getAffinitySlowBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double earthAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.EARTH);
        
        // Slow intensity scaling: 1.0 + (affinity / 120) capped at 1.5x
        double bonus = Math.min(0.50, earthAffinity / 120.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
}
