package com.server.enchantments.abilities;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;

/**
 * Ember Veil - Fire Enchantment (Common, Reactive)
 * When damaged, ignite the attacker
 * 
 * Base Stats: [burn_duration_seconds]
 * Base: 4.0 seconds
 * 
 * LEVEL SCALING (Power - Duration):
 *   Level I:   4.0s × 1.0 = 4.0s
 *   Level II:  4.0s × 1.3 = 5.2s
 *   Level III: 4.0s × 1.6 = 6.4s
 *   Level IV:  4.0s × 2.0 = 8.0s
 *   Level V:   4.0s × 2.5 = 10.0s
 * 
 * QUALITY SCALING (Effectiveness):
 *   Poor: 0.5x = 2.0s to 5.0s
 *   Common: 0.7x = 2.8s to 7.0s
 *   Uncommon: 0.9x = 3.6s to 9.0s
 *   Rare: 1.1x = 4.4s to 11.0s
 *   Epic: 1.4x = 5.6s to 14.0s
 *   Legendary: 1.7x = 6.8s to 17.0s
 *   Godly: 2.0x = 8.0s to 20.0s
 * 
 * Example: Ember Veil V [Legendary] = 4.0 × 2.5 × 1.7 = 17 seconds of fire!
 */
public class EmberVeil extends CustomEnchantment {
    
    public EmberVeil() {
        super("ember_veil", 
              "Ember Veil", 
              "Ignites attackers when you take damage",
              EnchantmentRarity.COMMON,
              ElementType.FIRE);
    }
    
    @Override
    public int getMaxLevel() {
        return 2; // Simple reactive - very low cap
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Check for vanilla armor pieces
        boolean isVanillaArmor = 
               type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE ||
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
        
        // Check for custom armor (23XXXX pattern)
        boolean isCustomArmor = false;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModelData = item.getItemMeta().getCustomModelData();
            // Custom armor uses 23XXXX format: 2 = functional, 3 = armor
            isCustomArmor = (customModelData >= 230000 && customModelData < 240000);
        }
        
        return isVanillaArmor || isCustomArmor;
    }
    
    @Override
    public double[] getBaseStats() {
        // [burn_duration_seconds]
        // Level and quality multipliers are applied automatically in EnchantmentData
        return new double[]{4.0};
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
        
        // Check if player is the one being damaged
        if (!damageEvent.getEntity().equals(player)) return;
        
        // Get the attacker
        Entity damagerEntity = damageEvent.getDamager();
        if (!(damagerEntity instanceof LivingEntity)) return;
        
        LivingEntity attacker = (LivingEntity) damagerEntity;
        
        // Get scaled stats with both quality AND level
        double[] stats = getScaledStats(quality, level);
        double burnDuration = stats[0];
        
        // Apply fire ticks (20 ticks = 1 second)
        int fireTicks = (int) (burnDuration * 20);
        attacker.setFireTicks(Math.max(attacker.getFireTicks(), fireTicks));
        
        // Visual/audio feedback
        player.sendMessage(getColoredName() + " §7ignited your attacker!");
    }
    
    @Override
    public TriggerType getTriggerType() {
        return TriggerType.ON_DAMAGED;
    }
}
