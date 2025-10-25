package com.server.islands.commands;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.islands.managers.IslandManager;

/**
 * Listener for Island Info GUI interactions
 */
public class IslandInfoGUIListener implements Listener {
    
    private final IslandManager islandManager;
    
    public IslandInfoGUIListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        // Check if it's the Island Information GUI
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Island Information")) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= clickedInventory.getSize()) {
            return;
        }
        
        ItemStack clickedItem = clickedInventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Handle back button
        if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            IslandMenuGUI.open(player, islandManager);
        }
    }
}
