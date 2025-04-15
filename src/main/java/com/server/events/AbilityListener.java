package com.server.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.server.abilities.AbilityManager;

public class AbilityListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // Check if the item has an ability and trigger it
        if (AbilityManager.getInstance().activateAbility(event.getPlayer(), item)) {
            // Cancel the event to prevent normal item use
            event.setCancelled(true);
        }
    }
}