package com.server.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.items.ItemManager;

/**
 * Automatically enhances items with fuel lore and other properties when obtained
 */
public class AutoItemEnhancementListener implements Listener {
    
    private final Main plugin;
    
    public AutoItemEnhancementListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle item pickup - enhance with fuel lore
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        
        if (ItemManager.needsFuelLore(item)) {
            ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(item);
            event.getItem().setItemStack(enhanced);
        }
    }
    
    /**
     * Handle crafting - enhance crafted items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        
        if (result != null && ItemManager.needsFuelLore(result)) {
            ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(result);
            event.getInventory().setResult(enhanced);
        }
    }
    
    /**
     * Handle entity deaths - enhance dropped items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        for (int i = 0; i < event.getDrops().size(); i++) {
            ItemStack drop = event.getDrops().get(i);
            
            if (ItemManager.needsFuelLore(drop)) {
                ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(drop);
                event.getDrops().set(i, enhanced);
            }
        }
    }
    
    /**
     * Handle fishing - enhance caught items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getCaught() != null && event.getCaught() instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item caughtItem = (org.bukkit.entity.Item) event.getCaught();
            ItemStack item = caughtItem.getItemStack();
            
            if (ItemManager.needsFuelLore(item)) {
                ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(item);
                caughtItem.setItemStack(enhanced);
            }
        }
    }
    
    /**
     * Handle inventory interactions - enhance items moved into inventories
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        // Delay the enhancement to ensure the item has been moved
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            
            // Check current item
            if (currentItem != null && ItemManager.needsFuelLore(currentItem)) {
                ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(currentItem);
                event.setCurrentItem(enhanced);
            }
            
            // Check cursor item
            if (cursor != null && ItemManager.needsFuelLore(cursor)) {
                ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(cursor);
                event.setCursor(enhanced);
            }
        });
    }
}