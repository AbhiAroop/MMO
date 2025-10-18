package com.server.enchantments.abilities.offensive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.EnchantmentDamageUtil;

/**
 * TEMPLATE: Simple Offensive Enchantment (Proc-based)
 * Copy this file and modify for basic damage enchantments
 * 
 * Example: Simple bonus damage with proc chance
 */
public class OffensiveSimpleTemplate extends CustomEnchantment {
    
    // Optional: Cooldown tracking
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 3000; // 3 seconds
    
    public OffensiveSimpleTemplate() {
        super(
            "template_offensive_simple",
            "Template Offensive Simple",
            "Chance to deal bonus damage",
            EnchantmentRarity.UNCOMMON,
            ElementType.FIRE
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Weapons only
        return type == Material.DIAMOND_SWORD ||
               type == Material.NETHERITE_SWORD ||
               type == Material.DIAMOND_AXE ||
               type == Material.NETHERITE_AXE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [proc_chance, bonus_damage]
        return new double[]{0.25, 5.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        // Optional: Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if (currentTime - lastUse < COOLDOWN_MS) {
                return;
            }
        }
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double procChance = stats[0];
        double bonusDamage = stats[1];
        
        // Check proc
        if (Math.random() > procChance) return;
        
        // Update cooldown (if using)
        cooldowns.put(playerId, currentTime);
        
        // Apply bonus damage (affinity automatically applied in PVP)
        EnchantmentDamageUtil.addBonusDamageToEvent(
            damageEvent,
            bonusDamage,
            getElement()
        );
        
        // Feedback
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        player.sendMessage("§6⚔ Bonus damage! §c+" + String.format("%.1f", bonusDamage));
    }
}
