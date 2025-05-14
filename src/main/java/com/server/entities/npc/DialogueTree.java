package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a tree of dialogue options for an NPC conversation
 */
public class DialogueTree {
    
    private final Map<String, DialogueNode> nodes;
    private final String rootNodeId;
    
    /**
     * Create a new DialogueTree
     * 
     * @param rootNodeId The ID of the root node
     */
    public DialogueTree(String rootNodeId) {
        this.nodes = new HashMap<>();
        this.rootNodeId = rootNodeId;
    }
    
    /**
     * Add a dialogue node to the tree
     * 
     * @param id The unique ID for this node
     * @param text The text to display for this node
     * @param responses The possible player responses to this node
     */
    public void addNode(String id, String text, List<DialogueResponse> responses) {
        DialogueNode node = new DialogueNode(id, text, responses);
        nodes.put(id, node);
    }
    
    /**
     * Get a dialogue node by ID
     * 
     * @param id The ID of the node to get
     * @return The dialogue node, or null if not found
     */
    public DialogueNode getNode(String id) {
        return nodes.get(id);
    }
    
    /**
     * Get the root node of the dialogue tree
     * 
     * @return The root dialogue node
     */
    public DialogueNode getRootNode() {
        return nodes.get(rootNodeId);
    }
    
    /**
     * Helper builder for creating dialogue trees
     */
    public static class Builder {
        private final DialogueTree tree;
        private final String rootNodeId;
        
        /**
         * Create a new dialogue tree builder
         * 
         * @param rootNodeId The ID of the root node
         */
        public Builder(String rootNodeId) {
            this.rootNodeId = rootNodeId;
            this.tree = new DialogueTree(rootNodeId);
        }
        
        /**
         * Add a node to the dialogue tree
         * 
         * @param id The node ID
         * @param text The node text
         * @param responses The possible responses
         * @return This builder, for chaining
         */
        public Builder addNode(String id, String text, List<DialogueResponse> responses) {
            tree.addNode(id, text, responses);
            return this;
        }
        
        /**
         * Add a simple node with no responses
         * 
         * @param id The node ID
         * @param text The node text
         * @return This builder, for chaining
         */
        public Builder addSimpleNode(String id, String text) {
            tree.addNode(id, text, new ArrayList<>());
            return this;
        }
        
        /**
         * Build the dialogue tree
         * 
         * @return The completed dialogue tree
         */
        public DialogueTree build() {
            if (!tree.nodes.containsKey(rootNodeId)) {
                throw new IllegalStateException("Root node has not been added to the dialogue tree");
            }
            return tree;
        }
    }
}