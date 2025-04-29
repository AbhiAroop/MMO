package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.server.profiles.skills.core.Skill;

/**
 * Represents a skill tree for a particular skill
 */
public class SkillTree {
    private final Skill skill;
    private final Map<String, SkillTreeNode> nodes;
    private final Map<String, Set<String>> connections;
    
    public SkillTree(Skill skill) {
        this.skill = skill;
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
    }
    
    /**
     * Get the skill this tree belongs to
     */
    public Skill getSkill() {
        return skill;
    }
    
    /**
     * Add a node to the tree
     */
    public void addNode(SkillTreeNode node) {
        nodes.put(node.getId(), node);
        if (!connections.containsKey(node.getId())) {
            connections.put(node.getId(), new HashSet<>());
        }
    }
    
    /**
     * Add a connection between two nodes
     */
    public void addConnection(String fromNodeId, String toNodeId) {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            throw new IllegalArgumentException("Both nodes must exist in the tree");
        }
        
        if (!connections.containsKey(fromNodeId)) {
            connections.put(fromNodeId, new HashSet<>());
        }
        
        connections.get(fromNodeId).add(toNodeId);
    }
    
    /**
     * Get a node by ID
     */
    public SkillTreeNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * Get all connections from a node
     */
    public Set<String> getConnections(String nodeId) {
        return connections.getOrDefault(nodeId, new HashSet<>());
    }
    
    /**
     * Get all nodes in the tree
     */
    public Map<String, SkillTreeNode> getAllNodes() {
        return new HashMap<>(nodes);
    }
    
    /**
     * Check if a node is connected to at least one unlocked node
     */
    public boolean isNodeAvailable(String nodeId, Set<String> unlockedNodes) {
        // Root node is always available
        if (nodeId.equals("root")) {
            return true;
        }
        
        // Check if any of the nodes that connect to this one are unlocked
        for (Map.Entry<String, Set<String>> entry : connections.entrySet()) {
            if (entry.getValue().contains(nodeId) && unlockedNodes.contains(entry.getKey())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get all connections in the tree
     */
    public Map<String, Set<String>> getAllConnections() {
        return new HashMap<>(connections);
    }
    
    /**
     * Calculate node position in a grid (for GUI display)
     */
    public TreeGridPosition getNodeGridPosition(String nodeId) {
        SkillTreeNode node = nodes.get(nodeId);
        if (node == null) {
            return new TreeGridPosition(0, 0);
        }
        
        return node.getGridPosition();
    }
    
    /**
     * Get a node by its position in the grid
     */
    public SkillTreeNode getNodeAtPosition(int x, int y) {
        for (SkillTreeNode node : nodes.values()) {
            TreeGridPosition pos = node.getGridPosition();
            if (pos.getX() == x && pos.getY() == y) {
                return node;
            }
        }
        return null;
    }
}