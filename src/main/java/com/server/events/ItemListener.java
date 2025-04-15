package com.server.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import com.server.items.ItemManager;

public class ItemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack result = event.getCurrentItem();
        if (result != null) {
            event.setCurrentItem(ItemManager.applyRarity(result));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (!ItemManager.hasRarity(item)) {
            event.getItem().setItemStack(ItemManager.applyRarity(item));
        }
    }
}