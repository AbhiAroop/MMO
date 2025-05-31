package com.server.crafting.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

import com.server.crafting.gui.CustomCraftingGUI;

/**
 * Replaces vanilla crafting table interactions with our custom system
 */
public class VanillaCraftingReplacer implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CRAFTING_TABLE) {
            return;
        }
        
        // Cancel the vanilla crafting table opening
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Open our custom crafting table instead
        CustomCraftingGUI.openCraftingTable(player);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        // Disable vanilla workbench crafting completely
        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            event.setCancelled(true);
        }
    }
}