package com.server.entities.npc;

import java.util.List;

/**
 * Represents a single node in a dialogue tree
 */
public class DialogueNode {
    
    private final String id;
    private final String text;
    private final List<DialogueResponse> responses;
    
    /**
     * Create a new dialogue node
     * 
     * @param id The unique ID for this node
     * @param text The text to display for this node
     * @param responses The possible player responses to this node
     */
    public DialogueNode(String id, String text, List<DialogueResponse> responses) {
        this.id = id;
        this.text = text;
        this.responses = responses;
    }
    
    /**
     * Get the ID of this node
     * 
     * @return The node ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the text of this node
     * 
     * @return The node text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the possible responses to this node
     * 
     * @return The list of possible responses
     */
    public List<DialogueResponse> getResponses() {
        return responses;
    }
    
    /**
     * Check if this node has any responses
     * 
     * @return True if this node has responses, false otherwise
     */
    public boolean hasResponses() {
        return responses != null && !responses.isEmpty();
    }
}