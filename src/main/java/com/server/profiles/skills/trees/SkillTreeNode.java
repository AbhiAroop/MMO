package com.server.profiles.skills.trees;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Represents a node in a skill tree
 */
public class SkillTreeNode {
    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final ChatColor color;
    private final TreeGridPosition gridPosition;
    private final int tokenCost;
    
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.gridPosition = new TreeGridPosition(gridX, gridY);
        this.tokenCost = tokenCost;
    }
    
    /**
     * Get the node ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the node name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the node description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the icon for this node
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Get the color for this node
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Get the token cost to unlock this node
     */
    public int getTokenCost() {
        return tokenCost;
    }
    
    /**
     * Get the position in the grid
     */
    public TreeGridPosition getGridPosition() {
        return gridPosition;
    }
}