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
    private final Map<String, Integer> minLevelRequirements; // Minimum level required to unlock connected nodes
    
    public SkillTree(Skill skill) {
        this.skill = skill;
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
        this.minLevelRequirements = new HashMap<>();
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
     * By default, any level of the source node will unlock the connection
     */
    public void addConnection(String fromNodeId, String toNodeId) {
        addConnection(fromNodeId, toNodeId, 1);
    }
    
    /**
     * Add a connection between two nodes with a minimum level requirement
     * The source node must be at least the specified level to unlock the connection
     */
    public void addConnection(String fromNodeId, String toNodeId, int minLevel) {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            throw new IllegalArgumentException("Both nodes must exist in the tree");
        }
        
        if (!connections.containsKey(fromNodeId)) {
            connections.put(fromNodeId, new HashSet<>());
        }
        
        connections.get(fromNodeId).add(toNodeId);
        
        // Store the minimum level requirement for this connection
        String connectionKey = fromNodeId + ":" + toNodeId;
        minLevelRequirements.put(connectionKey, minLevel);
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
     * Takes into account minimum level requirements
     */
    public boolean isNodeAvailable(String nodeId, Set<String> unlockedNodes, Map<String, Integer> nodeLevels) {
        // Root node is always available, even if not yet unlocked
        if (nodeId.equals("root")) {
            return true;
        }
        
        // Check if any of the nodes that connect to this one are unlocked
        for (Map.Entry<String, Set<String>> entry : connections.entrySet()) {
            String fromNodeId = entry.getKey();
            Set<String> toNodeIds = entry.getValue();
            
            if (toNodeIds.contains(nodeId) && unlockedNodes.contains(fromNodeId)) {
                // Check if the source node is at the required level
                String connectionKey = fromNodeId + ":" + nodeId;
                int requiredLevel = minLevelRequirements.getOrDefault(connectionKey, 1);
                int currentLevel = nodeLevels.getOrDefault(fromNodeId, 0);
                
                if (currentLevel >= requiredLevel) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a node is connected to at least one unlocked node
     * Simplified version that uses default level 1 requirement
     */
    public boolean isNodeAvailable(String nodeId, Set<String> unlockedNodes) {
        Map<String, Integer> defaultLevels = new HashMap<>();
        for (String node : unlockedNodes) {
            defaultLevels.put(node, 1);
        }
        return isNodeAvailable(nodeId, unlockedNodes, defaultLevels);
    }

    /**
     * Get all connections in the tree
     */
    public Map<String, Set<String>> getAllConnections() {
        return new HashMap<>(connections);
    }
    
    /**
     * Get the minimum level requirement for a connection
     */
    public int getMinLevelRequirement(String fromNodeId, String toNodeId) {
        String connectionKey = fromNodeId + ":" + toNodeId;
        return minLevelRequirements.getOrDefault(connectionKey, 1);
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