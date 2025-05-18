package com.server.entities.npc.dialogue;

import org.bukkit.entity.Player;

/**
 * Represents a response option in a dialogue
 */
public class DialogueResponse {
    private final String text;
    private final String nextNodeId;
    private DialogueAction action;
    
    /**
     * Create a dialogue response
     * 
     * @param text The response text
     * @param nextNodeId The ID of the next dialogue node
     */
    public DialogueResponse(String text, String nextNodeId) {
        this.text = text;
        this.nextNodeId = nextNodeId;
    }
    
    /**
     * Create a dialogue response with an action
     * 
     * @param text The response text
     * @param nextNodeId The ID of the next dialogue node
     * @param action The action to execute when this response is chosen
     */
    public DialogueResponse(String text, String nextNodeId, DialogueAction action) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
    }
    
    /**
     * Get the response text
     * 
     * @return The text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the ID of the next node
     * 
     * @return The next node ID
     */
    public String getNextNodeId() {
        return nextNodeId;
    }
    
    /**
     * Check if this response has an action
     * 
     * @return True if there is an action
     */
    public boolean hasAction() {
        return action != null;
    }
    
    /**
     * Execute the action
     * 
     * @param player The player who selected this response
     */
    public void executeAction(Player player) {
        if (action != null) {
            action.execute(player);
        }
    }
}