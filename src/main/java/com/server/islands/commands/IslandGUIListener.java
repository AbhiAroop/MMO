package com.server.islands.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.islands.data.IslandType;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles all island GUI click events.
 */
public class IslandGUIListener implements Listener {
    
    private final IslandManager islandManager;
    
    public IslandGUIListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        
        // Check if it's an island GUI
        if (title.contains("Create Island")) {
            event.setCancelled(true);
            handleCreateGUIClick(player, event.getSlot(), event.getCurrentItem());
        } else if (title.contains("Island Upgrades")) {
            event.setCancelled(true);
            handleUpgradeGUIClick(player, event.getSlot());
        } else if (title.contains("Island Info")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) { // Close button
                player.closeInventory();
            }
        }
    }
    
    /**
     * Handles clicks in the Create Island GUI
     */
    private void handleCreateGUIClick(Player player, int slot, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        Component displayName = item.getItemMeta().displayName();
        if (displayName == null) {
            return;
        }
        
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        // Determine which island type was clicked
        IslandType type = null;
        if (itemName.contains("Sky Island")) {
            type = IslandType.SKY;
        } else if (itemName.contains("Ocean Island")) {
            type = IslandType.OCEAN;
        } else if (itemName.contains("Forest Island")) {
            type = IslandType.FOREST;
        }
        
        if (type != null) {
            IslandCreateGUI.handleClick(player, type, islandManager);
        }
    }
    
    /**
     * Handles clicks in the Upgrade GUI
     */
    private void handleUpgradeGUIClick(Player player, int slot) {
        // Get the player's island
        islandManager.loadIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                return;
            }
            
            // Handle upgrade clicks (slots: 11, 13, 15, 29, 31)
            if (slot == 11 || slot == 13 || slot == 15 || slot == 29 || slot == 31) {
                IslandUpgradeGUI.handleUpgradeClick(player, island, slot, islandManager);
            } else if (slot == 49) {
                // Currency info - just close
                player.closeInventory();
            }
        });
    }
}
