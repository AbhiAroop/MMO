package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.Map;

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
    private final int maxLevel; // Maximum upgrade level for this node
    private final Map<Integer, String> levelDescriptions; // Descriptions for each level
    private final Map<Integer, Integer> levelCosts; // Token costs for each level
    
    /**
     * Create a simple non-upgradable node
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost) {
        this(id, name, description, icon, color, gridX, gridY, tokenCost, 1);
    }
    
    /**
     * Create an upgradable node with the same description for all levels
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.gridPosition = new TreeGridPosition(gridX, gridY);
        this.tokenCost = tokenCost;
        this.maxLevel = Math.max(1, maxLevel);
        this.levelDescriptions = new HashMap<>();
        this.levelCosts = new HashMap<>();
        
        // Set default descriptions and costs
        for (int i = 1; i <= maxLevel; i++) {
            this.levelDescriptions.put(i, description + (maxLevel > 1 ? " (Level " + i + ")" : ""));
            this.levelCosts.put(i, tokenCost * i); // Default cost scales linearly
        }
    }
    
    /**
     * Create a fully customized upgradable node
     */
    public SkillTreeNode(String id, String name, Material icon, ChatColor color, 
                        int gridX, int gridY, Map<Integer, String> levelDescriptions,
                        Map<Integer, Integer> levelCosts) {
        this.id = id;
        this.name = name;
        this.description = levelDescriptions.getOrDefault(1, "");
        this.icon = icon;
        this.color = color;
        this.gridPosition = new TreeGridPosition(gridX, gridY);
        this.tokenCost = levelCosts.getOrDefault(1, 1);
        this.levelDescriptions = new HashMap<>(levelDescriptions);
        this.levelCosts = new HashMap<>(levelCosts);
        this.maxLevel = Math.max(1, levelCosts.size());
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
     * Get the description for a specific level
     */
    public String getDescription(int level) {
        return levelDescriptions.getOrDefault(Math.min(level, maxLevel), description);
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
     * Get the token cost for a specific level
     */
    public int getTokenCost(int level) {
        return levelCosts.getOrDefault(Math.min(level, maxLevel), tokenCost);
    }
    
    /**
     * Get the maximum upgrade level
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Get the position in the grid
     */
    public TreeGridPosition getGridPosition() {
        return gridPosition;
    }
    
    /**
     * Set a custom description for a specific level
     */
    public void setLevelDescription(int level, String description) {
        if (level > 0 && level <= maxLevel) {
            levelDescriptions.put(level, description);
        }
    }
    
    /**
     * Set a custom token cost for a specific level
     */
    public void setLevelCost(int level, int cost) {
        if (level > 0 && level <= maxLevel) {
            levelCosts.put(level, cost);
        }
    }
    
    /**
     * Is this node upgradable?
     */
    public boolean isUpgradable() {
        return maxLevel > 1;
    }
}