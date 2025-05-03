package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;

/**
 * Stores a player's progress in skill trees
 */
public class PlayerSkillTreeData {
    // Map of skill ID to a map of node IDs to their unlocked level
    private final Map<String, Map<String, Integer>> unlockedNodeLevels;
    
    // NEW: Separate permanent storage for special node levels
    // This map will never be reset or modified during skill tree resets
    private final Map<String, Map<String, Integer>> permanentSpecialNodeLevels;
    
    // Map of skill ID to token count
    private final Map<String, Integer> skillTokens;
    
    public PlayerSkillTreeData() {
        this.unlockedNodeLevels = new HashMap<>();
        this.skillTokens = new HashMap<>();
        this.permanentSpecialNodeLevels = new HashMap<>();
    }
    
    /**
     * Get all unlocked nodes for a skill
     */
    public Set<String> getUnlockedNodes(String skillId) {
        Map<String, Integer> nodes = unlockedNodeLevels.getOrDefault(skillId, new HashMap<>());
        return new HashSet<>(nodes.keySet());
    }
    
    /**
     * Check if a node is unlocked (at any level)
     */
    public boolean isNodeUnlocked(String skillId, String nodeId) {
        Map<String, Integer> nodes = unlockedNodeLevels.getOrDefault(skillId, new HashMap<>());
        int level = nodes.getOrDefault(nodeId, 0);
        return level > 0; // Only consider it unlocked if level is greater than 0
    }
    
    /**
     * Get the current level of an unlocked node
     * Returns 0 if the node is not unlocked
     * For special nodes, also checks the permanent storage
     */
    public int getNodeLevel(String skillId, String nodeId) {
        Map<String, Integer> nodes = unlockedNodeLevels.getOrDefault(skillId, new HashMap<>());
        int normalLevel = nodes.getOrDefault(nodeId, 0);
        
        // For special nodes, check in permanent storage if not in regular storage
        if (normalLevel == 0) {
            // Get the tree to check if this is a special node
            SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
            if (tree != null) {
                SkillTreeNode node = tree.getNode(nodeId);
                if (node != null && node.isSpecialNode()) {
                    return getSpecialNodeLevel(skillId, nodeId);
                }
            }
        }
        
        return normalLevel;
    }
        
    /**
     * Unlock a node at a specific level
     */
    public void unlockNodeAtLevel(String skillId, String nodeId, int level) {
        if (!unlockedNodeLevels.containsKey(skillId)) {
            unlockedNodeLevels.put(skillId, new HashMap<>());
        }
        unlockedNodeLevels.get(skillId).put(nodeId, level);
    }
    
   /**
     * Upgrade a node to the next level
     * For special nodes, also update the permanent storage
     * @return The new level, or 0 if the node is not unlocked
     */
    public int upgradeNode(String skillId, String nodeId) {
        if (!isNodeUnlocked(skillId, nodeId)) {
            return 0;
        }
        
        // Get the skill tree and node
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return 0;
        
        SkillTreeNode node = tree.getNode(nodeId);
        if (node == null) return 0;
        
        int currentLevel = getNodeLevel(skillId, nodeId);
        
        // Check if already at max level
        if (currentLevel >= node.getMaxLevel()) {
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("[PlayerSkillTreeData] Node " + nodeId + 
                                                " is already at max level (" + node.getMaxLevel() + ")");
            }
            return currentLevel; // Already at max level, can't upgrade further
        }
        
        int newLevel = currentLevel + 1;
        
        // Store the old level for benefit calculation
        int oldLevel = currentLevel;
        
        // Unlock the new level
        unlockNodeAtLevel(skillId, nodeId, newLevel);
        
        // If this is a special node, update permanent storage
        if (node.isSpecialNode()) {
            storeSpecialNodeLevel(skillId, nodeId, newLevel);
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("[PlayerSkillTreeData] Updated special node " + 
                                                nodeId + " to level " + newLevel + " in permanent storage");
            }
        }
        
        // Apply benefits if this is the OreExtraction skill tree
        if (skillId.equals("ore_extraction")) {
            // Get the player this belongs to
            Player player = findPlayerForSkillTree();
            if (player != null) {
                // Get the OreExtraction skill
                Skill skill = SkillRegistry.getInstance().getSubskill(SubskillType.ORE_EXTRACTION);
                if (skill instanceof OreExtractionSubskill) {
                    OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
                    // Apply the benefits from the upgrade
                    oreSkill.applyNodeUpgrade(player, nodeId, oldLevel, newLevel);
                }
            }
        }
        
        return newLevel;
    }

    /**
     * Find the player that owns this skill tree data
     * This is a helper method for applying benefits
     */
    private Player findPlayerForSkillTree() {
        // This might need to be implemented based on your system architecture
        // You'll need a way to get the player from the skill tree data
        // This could involve searching through online players and checking their profiles
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) continue;
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile == null) continue;
            
            if (profile.getSkillTreeData() == this) {
                return player;
            }
        }
        
        return null;
    }

   /**
     * Reset a skill tree, refunding tokens
     * Special nodes will keep their progress but will be locked until prerequisites are met again
     * 
     * @param skillId The skill ID to reset
     * @return The number of tokens refunded
     */
    public int resetSkillTree(String skillId) {
        int tokensRefunded = 0;
        
        // Get the skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return 0;
        
        // Get all current nodes data
        Set<String> unlockedNodes = getUnlockedNodes(skillId);
        Map<String, Integer> nodeLevels = getNodeLevels(skillId);
        
        // Debug log before resetting
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Reset tree " + skillId + 
                ", nodes before: " + nodeLevels);
        }
        
        // First: Ensure our permanent storage has the latest special node levels
        for (String nodeId : new HashSet<>(nodeLevels.keySet())) {
            SkillTreeNode node = tree.getNode(nodeId);
            if (node != null && node.isSpecialNode()) {
                int level = nodeLevels.get(nodeId);
                if (level > 0) {
                    // Store in our permanent storage
                    storeSpecialNodeLevel(skillId, nodeId, level);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("[PlayerSkillTreeData] Stored special node " + 
                            nodeId + " at level " + level + " in permanent storage");
                    }
                }
            }
        }
        
        // Second: Calculate refunds for non-special nodes only
        for (String nodeId : unlockedNodes) {
            SkillTreeNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            // Skip refund for special nodes
            if (node.isSpecialNode()) {
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("[PlayerSkillTreeData] Skipping refund for special node: " + nodeId);
                }
                continue;
            }
            
            // Calculate refund for normal nodes
            int level = nodeLevels.getOrDefault(nodeId, 0);
            for (int i = 1; i <= level; i++) {
                tokensRefunded += node.getTokenCost(i);
            }
        }
        
        // Third: Completely clear the regular node storage
        if (unlockedNodeLevels.containsKey(skillId)) {
            unlockedNodeLevels.get(skillId).clear();
        }
        
        // Debug log after resetting
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Reset tree " + skillId + 
                ", nodes after reset (should be empty): " + getNodeLevels(skillId));
            
            if (skillId.equals("ore_extraction")) {
                Main.getInstance().getLogger().info("[PlayerSkillTreeData] Permanent storage for mining_xp_boost: " + 
                    getSpecialNodeLevel(skillId, "mining_xp_boost"));
            }
        }
        
        // Add the refunded tokens
        addTokens(skillId, tokensRefunded);
        
        return tokensRefunded;
    }

    /**
     * Store a special node's level in permanent storage
     */
    private void storeSpecialNodeLevel(String skillId, String nodeId, int level) {
        // Initialize map for this skill if needed
        if (!permanentSpecialNodeLevels.containsKey(skillId)) {
            permanentSpecialNodeLevels.put(skillId, new HashMap<>());
        }
        
        // Store the level (only if it's higher than what we already have)
        int currentStoredLevel = getSpecialNodeLevel(skillId, nodeId);
        if (level > currentStoredLevel) {
            permanentSpecialNodeLevels.get(skillId).put(nodeId, level);
        }
    }
    
    /**
     * Get a special node's level from permanent storage
     * Returns 0 if not found
     */
    private int getSpecialNodeLevel(String skillId, String nodeId) {
        if (!permanentSpecialNodeLevels.containsKey(skillId)) {
            return 0;
        }
        
        return permanentSpecialNodeLevels.get(skillId).getOrDefault(nodeId, 0);
    }
    
    /**
     * Check if a node can be unlocked based on tree connections
     * This adds special handling for special nodes that were previously unlocked
     * 
     * @param skillId The skill ID to check
     * @param nodeId The node ID to check
     * @return true if the node can be unlocked, false otherwise
     */
    public boolean canUnlockNode(String skillId, String nodeId) {
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return false;
        
        SkillTreeNode node = tree.getNode(nodeId);
        if (node == null) return false;
        
        Set<String> unlockedNodes = getUnlockedNodes(skillId);
        Map<String, Integer> nodeLevels = getNodeLevels(skillId);
        
        // Special handling for special nodes with preserved level
        if (node.isSpecialNode() && nodeLevels.containsKey(nodeId) && nodeLevels.get(nodeId) > 0) {
            // For special nodes, check if ANY parent node is unlocked
            Map<String, Set<String>> connections = tree.getAllConnections();
            
            for (Map.Entry<String, Set<String>> entry : connections.entrySet()) {
                String fromNodeId = entry.getKey();
                Set<String> toNodeIds = entry.getValue();
                
                // If this connection leads to our special node and the source is unlocked
                if (toNodeIds.contains(nodeId) && unlockedNodes.contains(fromNodeId)) {
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("[PlayerSkillTreeData] Special node " + nodeId + 
                                                        " can be unlocked because parent " + fromNodeId + " is unlocked");
                    }
                    return true;
                }
            }
        }
        
        // Regular node availability check
        return tree.isNodeAvailable(nodeId, unlockedNodes, nodeLevels);
    }

    /**
     * Unlock a node at level 1 (or at its preserved level for special nodes)
     */
    public void unlockNode(String skillId, String nodeId) {
        // Get the skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return;
        
        // Get the node
        SkillTreeNode node = tree.getNode(nodeId);
        if (node == null) return;
        
        // Check if this is a special node with a preserved level
        if (node.isSpecialNode()) {
            // Get the existing level from our node levels
            int currentLevel = getNodeLevel(skillId, nodeId);
            int specialNodeLevel = getSpecialNodeLevel(skillId, nodeId);
            
            // Use the higher of the two levels (current or special storage)
            int levelToUse = Math.max(currentLevel, specialNodeLevel);
            
            if (levelToUse > 0) {
                // Use the preserved level (which could be from regular storage or special storage)
                unlockNodeAtLevel(skillId, nodeId, levelToUse);
                
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("[PlayerSkillTreeData] Re-unlocked special node " + 
                                                    nodeId + " with preserved level " + levelToUse);
                }
                return;
            }
        }
        
        // Regular node or special node without a preserved level - unlock at level 1
        unlockNodeAtLevel(skillId, nodeId, 1);
    }

    /**
     * Helper method to find all special nodes in a skill tree
     */
    private Set<String> findSpecialNodes(SkillTree tree) {
        Set<String> specialNodes = new HashSet<>();
        
        if (tree == null) return specialNodes;
        
        for (SkillTreeNode node : tree.getAllNodes().values()) {
            if (node.isSpecialNode()) {
                specialNodes.add(node.getId());
            }
        }
        
        return specialNodes;
    }

    /**
     * Check if a node is special (should preserve level during resets)
     * 
     * @param skillId The skill ID to check
     * @param nodeId The node ID to check
     * @return true if the node is special, false otherwise
     */
    public boolean isSpecialNode(String skillId, String nodeId) {
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return false;
        
        SkillTreeNode node = tree.getNode(nodeId);
        if (node == null) return false;
        
        return node.isSpecialNode();
    }

    /**
     * Set the level of a node directly without unlocking it
     */
    private void setNodeLevel(String skillId, String nodeId, int level) {
        // Make sure the maps exist
        if (!unlockedNodeLevels.containsKey(skillId)) {
            unlockedNodeLevels.put(skillId, new HashMap<>());
        }
        unlockedNodeLevels.get(skillId).put(nodeId, level);
    }
    
    /**
     * Get the number of tokens a player has for a skill
     */
    public int getTokenCount(String skillId) {
        return skillTokens.getOrDefault(skillId, 0);
    }
    
    /**
     * Add tokens for a skill
     */
    public void addTokens(String skillId, int count) {
        int current = skillTokens.getOrDefault(skillId, 0);
        skillTokens.put(skillId, current + count);
    }
    
    /**
     * Use tokens for a skill
     * @return true if enough tokens were available, false otherwise
     */
    public boolean useTokens(String skillId, int count) {
        int current = skillTokens.getOrDefault(skillId, 0);
        if (current < count) {
            return false;
        }
        skillTokens.put(skillId, current - count);
        return true;
    }
    
    /**
     * Set the token count for a skill
     */
    public void setTokenCount(String skillId, int count) {
        skillTokens.put(skillId, Math.max(0, count));
    }
    
    /**
     * Get all node levels for a skill
     */
    public Map<String, Integer> getNodeLevels(String skillId) {
        return new HashMap<>(unlockedNodeLevels.getOrDefault(skillId, new HashMap<>()));
    }
    
    /**
     * Completely remove a node from unlocked nodes
     * This properly locks a node rather than just setting its level to 0
     */
    public void removeNode(String skillId, String nodeId) {
        if (unlockedNodeLevels.containsKey(skillId)) {
            unlockedNodeLevels.get(skillId).remove(nodeId);
        }
    }
}