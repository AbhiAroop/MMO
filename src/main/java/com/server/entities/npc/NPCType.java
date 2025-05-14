package com.server.entities.npc;

import org.bukkit.ChatColor;

/**
 * Different types of NPCs with varying difficulty and rewards
 */
public enum NPCType {
    NORMAL(ChatColor.GREEN, "●", 1.0),
    ELITE(ChatColor.GOLD, "❈", 2.0),
    MINIBOSS(ChatColor.RED, "✦", 3.0),
    BOSS(ChatColor.DARK_PURPLE, "☠", 5.0);
    
    private final ChatColor color;
    private final String symbol;
    private final double rewardMultiplier;
    
    NPCType(ChatColor color, String symbol, double rewardMultiplier) {
        this.color = color;
        this.symbol = symbol;
        this.rewardMultiplier = rewardMultiplier;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public double getRewardMultiplier() {
        return rewardMultiplier;
    }
}