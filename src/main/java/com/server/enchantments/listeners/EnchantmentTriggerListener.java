package com.server.enchantments.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.EnchantmentRegistry;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;

/**
 * Routes game events to enchantment triggers.
 * Scans equipment for enchantments and activates them based on trigger type.
 */
public class EnchantmentTriggerListener implements Listener {
    
    /**
     * Handles damage events for ON_HIT and ON_DAMAGED triggers.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if attacker is a player (ON_HIT triggers)
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            handleOnHitTriggers(attacker, event);
        }
        
        // Check if victim is a player (ON_DAMAGED triggers)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            handleOnDamagedTriggers(victim, event);
        }
    }
    
    /**
     * Handles death events for ON_KILL triggers.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            handleOnKillTriggers(killer, event);
        }
    }
    
    /**
     * Handles sneak events for PASSIVE triggers (like GaleStep).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        handlePassiveTriggers(player, event);
    }
    
    /**
     * Handles ON_HIT triggers from attacker's equipment.
     */
    private void handleOnHitTriggers(Player player, EntityDamageByEntityEvent event) {
        // Scan held item and armor
        ItemStack[] equipment = getPlayerEquipment(player);
        
        for (ItemStack item : equipment) {
            // Skip null or air items
            if (item == null || item.getType() == Material.AIR) continue;
            
            List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
            
            for (EnchantmentData data : enchantments) {
                CustomEnchantment enchantment = EnchantmentRegistry.getInstance().getEnchantment(data.getEnchantmentId());
                if (enchantment == null) continue;
                
                EnchantmentQuality quality = data.getQuality();
                EnchantmentLevel level = data.getLevel();
                
                if (enchantment.getTriggerType() == CustomEnchantment.TriggerType.ON_HIT) {
                    enchantment.trigger(player, quality, level, event);
                }
            }
        }
    }
    
    /**
     * Handles ON_DAMAGED triggers from victim's equipment.
     */
    private void handleOnDamagedTriggers(Player player, EntityDamageByEntityEvent event) {
        // Scan armor pieces (primarily)
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return;
        
        for (ItemStack item : armor) {
            // Skip null or air items
            if (item == null || item.getType() == Material.AIR) continue;
            
            List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
            
            for (EnchantmentData data : enchantments) {
                CustomEnchantment enchantment = EnchantmentRegistry.getInstance().getEnchantment(data.getEnchantmentId());
                if (enchantment == null) continue;
                
                EnchantmentQuality quality = data.getQuality();
                EnchantmentLevel level = data.getLevel();
                
                if (enchantment.getTriggerType() == CustomEnchantment.TriggerType.ON_DAMAGED) {
                    enchantment.trigger(player, quality, level, event);
                }
            }
        }
    }
    
    /**
     * Handles ON_KILL triggers from killer's equipment.
     */
    private void handleOnKillTriggers(Player player, EntityDeathEvent event) {
        ItemStack[] equipment = getPlayerEquipment(player);
        
        for (ItemStack item : equipment) {
            // Skip null or air items
            if (item == null || item.getType() == Material.AIR) continue;
            
            List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
            
            for (EnchantmentData data : enchantments) {
                CustomEnchantment enchantment = EnchantmentRegistry.getInstance().getEnchantment(data.getEnchantmentId());
                if (enchantment == null) continue;
                
                EnchantmentQuality quality = data.getQuality();
                EnchantmentLevel level = data.getLevel();
                
                if (enchantment.getTriggerType() == CustomEnchantment.TriggerType.ON_KILL) {
                    enchantment.trigger(player, quality, level, event);
                }
            }
        }
    }
    
    /**
     * Handles PASSIVE triggers (for special events like sneak).
     */
    private void handlePassiveTriggers(Player player, PlayerToggleSneakEvent event) {
        ItemStack[] equipment = getPlayerEquipment(player);
        
        for (ItemStack item : equipment) {
            // Skip null or air items
            if (item == null || item.getType() == Material.AIR) continue;
            
            List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
            
            for (EnchantmentData data : enchantments) {
                CustomEnchantment enchantment = EnchantmentRegistry.getInstance().getEnchantment(data.getEnchantmentId());
                if (enchantment == null) continue;
                
                EnchantmentQuality quality = data.getQuality();
                EnchantmentLevel level = data.getLevel();
                
                // Check for PASSIVE trigger type (used by GaleStep and similar)
                if (enchantment.getTriggerType() == CustomEnchantment.TriggerType.PASSIVE) {
                    enchantment.trigger(player, quality, level, event);
                }
            }
        }
    }
    
    /**
     * Gets all equipment from a player (armor + held item).
     */
    private ItemStack[] getPlayerEquipment(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return new ItemStack[0];
        
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        ItemStack[] equipment = new ItemStack[armor.length + 1];
        System.arraycopy(armor, 0, equipment, 0, armor.length);
        equipment[armor.length] = heldItem;
        
        return equipment;
    }
}
