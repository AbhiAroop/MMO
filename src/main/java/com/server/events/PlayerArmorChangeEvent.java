package com.server.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Event that is called when a player's armor is changed
 */
public class PlayerArmorChangeEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final EquipmentSlot slot;
    private final ItemStack oldItem;
    private final ItemStack newItem;
    
    public PlayerArmorChangeEvent(Player player, EquipmentSlot slot, 
                                 ItemStack oldItem, ItemStack newItem) {
        super(player);
        this.slot = slot;
        this.oldItem = oldItem;
        this.newItem = newItem;
    }
    
    public EquipmentSlot getSlot() {
        return slot;
    }
    
    public ItemStack getOldItem() {
        return oldItem;
    }
    
    public ItemStack getNewItem() {
        return newItem;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}