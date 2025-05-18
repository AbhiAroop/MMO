package com.server.entities.npc.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a node in a dialogue tree
 */
public class DialogueNode {
    private final String id;
    private final String text;
    private final List<DialogueResponse> responses;
    
    /**
     * Create a dialogue node with a generated ID
     * 
     * @param text The text displayed for this node
     */
    public DialogueNode(String text) {
        this(UUID.randomUUID().toString(), text, new ArrayList<>());
    }
    
    /**
     * Create a dialogue node
     * 
     * @param id The unique ID of this node
     * @param text The text displayed for this node
     * @param responses The available responses
     */
    public DialogueNode(String id, String text, List<DialogueResponse> responses) {
        this.id = id;
        this.text = text;
        this.responses = responses != null ? new ArrayList<>(responses) : new ArrayList<>();
    }
    
    /**
     * Get the unique ID of this node
     * 
     * @return The node ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the text displayed for this node
     * 
     * @return The text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Get the available responses for this node
     * 
     * @return List of responses
     */
    public List<DialogueResponse> getResponses() {
        return new ArrayList<>(responses);
    }
    
    /**
     * Add a response to this node
     * 
     * @param response The response to add
     */
    public void addResponse(DialogueResponse response) {
        responses.add(response);
    }
    
    /**
     * Check if this node has responses
     * 
     * @return True if there are responses
     */
    public boolean hasResponses() {
        return !responses.isEmpty();
    }
}