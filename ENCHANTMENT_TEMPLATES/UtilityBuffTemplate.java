package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;

/**
 * TEMPLATE: Utility Enchantment (Buff/Stealth)
 * Copy this file and modify for invisibility, speed, or other utility effects
 * 
 * Example: Invisibility on kill
 */
public class UtilityBuffTemplate extends CustomEnchantment {
    
    public UtilityBuffTemplate() {
        super(
            "template_utility_buff",
            "Template Utility Buff",
            "Grants invisibility on kill",
            EnchantmentRarity.RARE,
            ElementType.SHADOW
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Armor and weapons
        return type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type == Material.DIAMOND_SWORD ||
               type == Material.NETHERITE_SWORD;
    }
    
    @Override
    public double[] getBaseStats() {
        // [invisibility_duration_ticks, speed_bonus]
        return new double[]{40.0, 0.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        // Check if this is a kill event
        if (!(event instanceof EntityDeathEvent)) return;
        EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        
        // Verify player got the kill
        if (!(deathEvent.getEntity().getKiller() instanceof Player)) return;
        if (!deathEvent.getEntity().getKiller().equals(player)) return;
        
        // Get stats
        double[] stats = getScaledStats(quality);
        int duration = (int) stats[0];
        
        // Apply invisibility
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY,
            duration,
            0,
            false,
            false
        ));
        
        // Visual effect
        player.getWorld().spawnParticle(
            Particle.SMOKE_LARGE,
            player.getLocation().add(0, 1, 0),
            20,
            0.5, 0.5, 0.5,
            0.05
        );
        
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
        player.sendMessage("§8✦ Vanished into shadow!");
    }
}
