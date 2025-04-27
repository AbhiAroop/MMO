package com.server.profiles.skills.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.server.profiles.skills.core.Skill;

/**
 * Event fired when a player levels up a skill
 */
public class SkillLevelUpEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Skill skill;
    private final int newLevel;
    
    public SkillLevelUpEvent(Player player, Skill skill, int newLevel) {
        this.player = player;
        this.skill = skill;
        this.newLevel = newLevel;
    }
    
    /**
     * Get the player who leveled up
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the skill that was leveled up
     */
    public Skill getSkill() {
        return skill;
    }
    
    /**
     * Get the new level of the skill
     */
    public int getNewLevel() {
        return newLevel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}