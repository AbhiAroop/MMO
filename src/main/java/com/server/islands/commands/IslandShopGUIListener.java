package com.server.islands.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.islands.gui.IslandShopGUI;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Listener for Island Shop GUI interactions
 */
public class IslandShopGUIListener implements Listener {
    
    private final IslandManager islandManager;
    
    // Shop item definitions: slot -> (material, cost, requiredLevel, quantity)
    private static final Map<Integer, ShopItem> SHOP_ITEMS = new HashMap<>();
    
    static {
        // Mycelium - Test item
        SHOP_ITEMS.put(20, new ShopItem(Material.MYCELIUM, 50, 1, 64));
        
        // Add more items here as they're implemented
    }
    
    public IslandShopGUIListener(IslandManager islandManager) {
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
        
        // Check if it's the Island Shop GUI
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Island Shop")) {
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
            IslandMenuGUI.open(player, islandManager);
            return;
        }
        
        // Handle shop item purchase
        if (SHOP_ITEMS.containsKey(slot)) {
            handlePurchase(player, slot, SHOP_ITEMS.get(slot));
        }
    }
    
    /**
     * Handle a shop item purchase
     */
    private void handlePurchase(Player player, int slot, ShopItem item) {
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("You don't have an island!", NamedTextColor.RED));
                return;
            }
            
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                int currentLevel = island.getIslandLevel();
                int currentTokens = island.getIslandTokens();
                
                // Check level requirement
                if (currentLevel < item.requiredLevel) {
                    player.sendMessage(Component.text("❌ Your island level is too low! Required: " + item.requiredLevel, NamedTextColor.RED));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                
                // Check if player has enough tokens
                if (currentTokens < item.cost) {
                    player.sendMessage(Component.text("❌ You don't have enough tokens! Required: " + item.cost, NamedTextColor.RED));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                
                // Check if player has inventory space
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(Component.text("❌ Your inventory is full!", NamedTextColor.RED));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                
                // Deduct tokens
                island.addIslandTokens(-item.cost);
                
                // Give item to player
                ItemStack purchasedItem = new ItemStack(item.material, item.quantity);
                player.getInventory().addItem(purchasedItem);
                
                // Success message
                player.sendMessage(Component.text("✔ Purchased ", NamedTextColor.GREEN)
                    .append(Component.text(item.quantity + "x " + item.material.name(), NamedTextColor.YELLOW))
                    .append(Component.text(" for ", NamedTextColor.GREEN))
                    .append(Component.text(item.cost + " tokens", NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.GREEN)));
                
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                
                // Refresh the GUI
                IslandShopGUI.open(player, islandManager);
            });
        });
    }
    
    /**
     * Shop item data class
     */
    private static class ShopItem {
        final Material material;
        final int cost;
        final int requiredLevel;
        final int quantity;
        
        ShopItem(Material material, int cost, int requiredLevel, int quantity) {
            this.material = material;
            this.cost = cost;
            this.requiredLevel = requiredLevel;
            this.quantity = quantity;
        }
    }
}
