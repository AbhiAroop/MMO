package com.server.entities.npc;

import java.util.function.Consumer;

import org.bukkit.entity.Player;

/**
 * Represents a dialogue response option for players
 */
public class DialogueResponse {
    
    private final String text;
    private final String nextNodeId;
    private final Consumer<Player> action;
    
    /**
     * Create a dialogue response that leads to another node
     * 
     * @param text The text to display for this response
     * @param nextNodeId The ID of the next dialogue node
     */
    public DialogueResponse(String text, String nextNodeId) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = null;
    }
    
    /**
     * Create a dialogue response that performs an action
     * 
     * @param text The text to display for this response
     * @param action The action to perform when this response is chosen
     */
    public DialogueResponse(String text, Consumer<Player> action) {
        this.text = text;
        this.nextNodeId = null;
        this.action = action;
    }
    
    /**
     * Create a dialogue response that both leads to another node and performs an action
     * 
     * @param text The text to display for this response
     * @param nextNodeId The ID of the next dialogue node
     * @param action The action to perform when this response is chosen
     */
    public DialogueResponse(String text, String nextNodeId, Consumer<Player> action) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
    }
    
    /**
     * Get the text for this response
     * 
     * @return The response text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the ID of the next dialogue node
     * 
     * @return The next node ID, or null if there is no next node
     */
    public String getNextNodeId() {
        return nextNodeId;
    }
    
    /**
     * Check if this response has an action
     * 
     * @return True if this response has an action, false otherwise
     */
    public boolean hasAction() {
        return action != null;
    }
    
    /**
     * Execute the action for this response
     * 
     * @param player The player who chose this response
     */
    public void executeAction(Player player) {
        if (action != null) {
            action.accept(player);
        }
    }
}