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
 * Inferno Strike - Fire Enchantment (Rare, Offensive)
 * Chance to deal bonus fire damage on hit
 * 
 * Base Stats: [proc_chance, bonus_damage]
 * Base: 10% chance, 4.0 damage
 * 
 * LEVEL SCALING (Power - Damage & Proc Chance):
 *   Level I:   10% proc, 4.0 dmg
 *   Level II:  13% proc, 5.2 dmg
 *   Level III: 16% proc, 6.4 dmg
 *   Level IV:  20% proc, 8.0 dmg
 *   Level V:   25% proc, 10.0 dmg
 * 
 * QUALITY SCALING (Effectiveness):
 *   Poor: 0.5x = 5% proc, 2.0 to 5.0 dmg
 *   Common: 0.7x = 7% proc, 2.8 to 7.0 dmg
 *   Uncommon: 0.9x = 9% proc, 3.6 to 9.0 dmg
 *   Rare: 1.1x = 11% proc, 4.4 to 11.0 dmg
 *   Epic: 1.4x = 14% proc, 5.6 to 14.0 dmg
 *   Legendary: 1.7x = 17% proc, 6.8 to 17.0 dmg
 *   Godly: 2.0x = 20% proc, 8.0 to 20.0 dmg
 * 
 * Example: Inferno Strike V [Legendary] = 25% × 1.7 = 42.5% proc, 10 × 1.7 = 17 damage!
 */
public class InfernoStrike extends CustomEnchantment {
    
    public InfernoStrike() {
        super("inferno_strike",
              "Inferno Strike",
              "Chance to deal bonus fire damage",
              EnchantmentRarity.RARE,
              ElementType.FIRE);
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
               type == Material.TRIDENT;
    }
    
    @Override
    public double[] getBaseStats() {
        // [proc_chance (0.0-1.0), bonus_damage]
        return new double[]{0.10, 4.0};
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
        
        // Get scaled stats with both quality AND level
        double[] stats = getScaledStats(quality, level);
        double procChance = stats[0];
        double bonusDamage = stats[1];
        
        // Check if proc occurs
        if (Math.random() > procChance) return;
        
        // Apply bonus fire damage
        // For player targets, CombatListener will automatically apply armor reduction
        // For non-player targets, vanilla damage system handles it
        com.server.enchantments.utils.EnchantmentDamageUtil.addBonusDamageToEvent(
            damageEvent, bonusDamage, getElement()
        );
        
        // Set target on fire briefly for visual effect
        target.setFireTicks(40); // 2 seconds
        
        // Visual/audio feedback
        String damageType = (target instanceof Player) ? "physical fire" : "fire";
        player.sendMessage(getColoredName() + " §7dealt §c+" + 
                          String.format("%.1f", bonusDamage) + " §7" + damageType + " damage!");
        
        if (target instanceof Player) {
            player.sendMessage("§8(Will be reduced by target's armor)");
        }
    }
    
    @Override
    public TriggerType getTriggerType() {
        return TriggerType.ON_HIT;
    }
}
