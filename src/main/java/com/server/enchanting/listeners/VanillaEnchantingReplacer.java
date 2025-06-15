package com.server.enchanting.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.server.enchanting.gui.CustomEnchantingGUI;

/**
 * Replaces vanilla enchanting table interactions with our custom system
 */
public class VanillaEnchantingReplacer implements Listener {
    
    /**
     * Replace vanilla enchanting table interaction
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Cancel vanilla enchanting table opening
        event.setCancelled(true);
        
        // Open our custom enchanting GUI
        CustomEnchantingGUI.openEnchantingGUI(player, block.getLocation());
    }
    
    /**
     * Disable vanilla enchanting completely
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        // Cancel all vanilla enchanting
        event.setCancelled(true);
    }
}