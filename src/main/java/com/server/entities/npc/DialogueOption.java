package com.server.entities.npc;

/**
 * Represents an option in a dialogue node
 */
public class DialogueOption {
    
    private final String text;
    private final String nextNodeId;
    
    /**
     * Create a new dialogue option
     * 
     * @param text The text for this option
     * @param nextNodeId The ID of the node to go to if this option is selected
     */
    public DialogueOption(String text, String nextNodeId) {
        this.text = text;
        this.nextNodeId = nextNodeId;
    }
    
    /**
     * Get the text for this option
     * 
     * @return The text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the ID of the node to go to if this option is selected
     * 
     * @return The next node ID
     */
    public String getNextNodeId() {
        return nextNodeId;
    }
}