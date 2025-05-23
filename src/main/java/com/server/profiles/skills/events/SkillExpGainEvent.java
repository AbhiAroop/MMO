package com.server.profiles.skills.events;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.server.profiles.skills.core.Skill;

/**
 * Event fired when a player gains skill XP
 */
public class SkillExpGainEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Skill skill;
    private double amount;
    private boolean cancelled;
    private final Map<String, Object> metadata = new HashMap<>();
    
    public SkillExpGainEvent(Player player, Skill skill, double amount) {
        this.player = player;
        this.skill = skill;
        this.amount = amount;
        this.cancelled = false;
    }
    
    /**
     * Get the player gaining XP
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the skill gaining XP
     */
    public Skill getSkill() {
        return skill;
    }
    
    /**
     * Get the amount of XP being gained
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Set the amount of XP being gained
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    /**
     * Add metadata to this event
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get metadata from this event
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Check if this event has specific metadata
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}