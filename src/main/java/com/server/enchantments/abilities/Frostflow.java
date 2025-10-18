package com.server.enchantments.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;

/**
 * Frostflow - Water Enchantment (Uncommon, Crowd Control)
 * Apply stacking slowness debuff on hit
 * 
 * Base Stats: [slow_duration_seconds, slow_amplifier, max_stacks]
 * Base: 5.0s duration, 1.5 amplifier per stack, 3 max stacks
 * 
 * LEVEL SCALING (Power - Duration & Stacks):
 *   Level I:   5.0s, 1.5 amp, 3 stacks (max Slow III)
 *   Level II:  6.5s, 1.5 amp, 3 stacks (max Slow III)
 *   Level III: 8.0s, 1.5 amp, 3 stacks (max Slow III)
 *   Level IV:  10.0s, 1.5 amp, 3 stacks (max Slow III)
 *   Level V:   12.5s, 1.5 amp, 3 stacks (max Slow III)
 * 
 * QUALITY SCALING (Effectiveness):
 *   Poor: 0.5x = 2.5s to 6.25s, 2 stacks
 *   Common: 0.7x = 3.5s to 8.75s, 2 stacks
 *   Uncommon: 0.9x = 4.5s to 11.25s, 2 stacks
 *   Rare: 1.1x = 5.5s to 13.75s, 3 stacks
 *   Epic: 1.4x = 7.0s to 17.5s, 4 stacks
 *   Legendary: 1.7x = 8.5s to 21.25s, 5 stacks
 *   Godly: 2.0x = 10s to 25s, 6 stacks
 * 
 * Example: Frostflow V [Legendary] = 12.5s × 1.7 = 21.25s duration, 5 stacks max
 * At max stacks: Slowness VII (almost immobile!)
 */
public class Frostflow extends CustomEnchantment {
    
    // Track slowness stacks per entity
    private static final Map<UUID, Integer> slowStacks = new HashMap<>();
    
    public Frostflow() {
        super("frostflow",
              "Frostflow",
              "Apply stacking slowness debuff",
              EnchantmentRarity.UNCOMMON,
              ElementType.WATER);
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to weapons
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE ||
               type == Material.BOW || type == Material.CROSSBOW || type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [slow_duration_seconds, slow_amplifier, max_stacks]
        return new double[]{5.0, 1.5, 3.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        // Backwards compatibility - default to Level I
        trigger(player, quality, com.server.enchantments.data.EnchantmentLevel.I, event);
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, com.server.enchantments.data.EnchantmentLevel level, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        // Check if player is the attacker
        if (!damageEvent.getDamager().equals(player)) return;
        
        // Get the target
        Entity targetEntity = damageEvent.getEntity();
        if (!(targetEntity instanceof LivingEntity)) return;
        
        LivingEntity target = (LivingEntity) targetEntity;
        UUID targetId = target.getUniqueId();
        
        // Get scaled stats with both quality AND level
        double[] stats = getScaledStats(quality, level);
        double duration = stats[0];
        double amplifier = stats[1];
        int maxStacks = (int) Math.round(stats[2]);
        
        // Get or initialize stacks
        int currentStacks = slowStacks.getOrDefault(targetId, 0);
        
        // Increment stacks (cap at max)
        int newStacks = Math.min(currentStacks + 1, maxStacks);
        slowStacks.put(targetId, newStacks);
        
        // Calculate slowness level (0-based, so subtract 1)
        int slowLevel = (int) Math.round(amplifier * newStacks) - 1;
        slowLevel = Math.max(0, Math.min(slowLevel, 9)); // Cap at Slow X
        
        // Apply slowness effect
        int durationTicks = (int) (duration * 20);
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            durationTicks,
            slowLevel,
            false,
            true,
            true
        ));
        
        // Visual/audio feedback
        if (newStacks == 1) {
            player.sendMessage(getColoredName() + " §7applied §bFrostflow §7(" + 
                              newStacks + "/" + maxStacks + ")");
        } else {
            player.sendMessage(getColoredName() + " §7stacked to §b" + 
                              newStacks + "/" + maxStacks);
        }
        
        // Schedule stack decay
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.server.Main.getInstance(),
            () -> {
                int stacks = slowStacks.getOrDefault(targetId, 0);
                if (stacks > 0) {
                    slowStacks.put(targetId, stacks - 1);
                    if (stacks - 1 <= 0) {
                        slowStacks.remove(targetId);
                    }
                }
            },
            durationTicks
        );
    }
    
    @Override
    public TriggerType getTriggerType() {
        return TriggerType.ON_HIT;
    }
    
    /**
     * Get current slowness stacks on an entity
     */
    public static int getStacks(UUID entityId) {
        return slowStacks.getOrDefault(entityId, 0);
    }
    
    /**
     * Clear all stacks (useful for cleanup)
     */
    public static void clearAllStacks() {
        slowStacks.clear();
    }
}
