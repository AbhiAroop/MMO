package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.tokens.SkillToken;

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
    private final SkillToken.TokenTier requiredTokenTier; // NEW: Required token tier
    private boolean isSpecialNode;
    private Integer customModelData;
    
    /**
     * Create a simple non-upgradable node with token tier requirement
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost, 
                         SkillToken.TokenTier requiredTier) {
        this(id, name, description, icon, color, gridX, gridY, tokenCost, 1, requiredTier);
    }
    
    /**
     * Create a simple non-upgradable node (defaults to Basic tier)
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost) {
        this(id, name, description, icon, color, gridX, gridY, tokenCost, 1, SkillToken.TokenTier.BASIC);
    }
    
    /**
     * Create an upgradable node with token tier requirement
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost, int maxLevel,
                         SkillToken.TokenTier requiredTier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.gridPosition = new TreeGridPosition(gridX, gridY);
        this.tokenCost = tokenCost;
        this.maxLevel = maxLevel;
        this.requiredTokenTier = requiredTier;
        this.isSpecialNode = false;
        this.customModelData = null; // Initialize as null
        
        // Initialize level descriptions and costs
        this.levelDescriptions = new HashMap<>();
        this.levelCosts = new HashMap<>();
        
        // Set default values for single-level nodes
        if (maxLevel == 1) {
            levelDescriptions.put(1, description);
            levelCosts.put(1, tokenCost);
        }
    }
    
    /**
     * Create an upgradable node (defaults to Basic tier)
     */
    public SkillTreeNode(String id, String name, String description, Material icon, 
                         ChatColor color, int gridX, int gridY, int tokenCost, int maxLevel) {
        this(id, name, description, icon, color, gridX, gridY, tokenCost, maxLevel, SkillToken.TokenTier.BASIC);
    }
    
    /**
     * Create a fully customized upgradable node with token tier
     */
    public SkillTreeNode(String id, String name, Material icon, ChatColor color, 
                        int gridX, int gridY, Map<Integer, String> levelDescriptions,
                        Map<Integer, Integer> levelCosts, SkillToken.TokenTier requiredTier) {
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
        this.requiredTokenTier = requiredTier;
    }

    /**
     * Create a fully customized upgradable node (defaults to Basic tier)
     */
    public SkillTreeNode(String id, String name, Material icon, ChatColor color, 
                        int gridX, int gridY, Map<Integer, String> levelDescriptions,
                        Map<Integer, Integer> levelCosts) {
        this(id, name, icon, color, gridX, gridY, levelDescriptions, levelCosts, SkillToken.TokenTier.BASIC);
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

    /**
     * Mark this node as a special node (doesn't refund tokens on reset and keeps progress)
     */
    public void setSpecialNode(boolean isSpecialNode) {
        this.isSpecialNode = isSpecialNode;
    }
    
    /**
     * Check if this node is a special node
     */
    public boolean isSpecialNode() {
        return isSpecialNode;
    }

    /**
     * Get the required token tier for this node
     */
    public SkillToken.TokenTier getRequiredTokenTier() {
        return requiredTokenTier;
    }
    
    /**
     * Check if a token tier meets the requirement for this node
     */
    public boolean acceptsTokenTier(SkillToken.TokenTier tier) {
        return tier.getLevel() >= requiredTokenTier.getLevel();
    }

    /**
     * Set custom model data for this node's icon
     */
    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }
    
    /**
     * Get custom model data for this node's icon
     */
    public Integer getCustomModelData() {
        return customModelData;
    }
    
    /**
     * Check if this node has custom model data
     */
    public boolean hasCustomModelData() {
        return customModelData != null;
    }
}