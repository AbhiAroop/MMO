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
import com.server.profiles.skills.tokens.SkillToken;

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
    private final Map<String, Map<String, Integer>> tieredSkillTokens;
    
    public PlayerSkillTreeData() {
        this.unlockedNodeLevels = new HashMap<>();
        this.skillTokens = new HashMap<>();
        this.tieredSkillTokens = new HashMap<>();
        this.permanentSpecialNodeLevels = new HashMap<>();
    }

    /**
     * Get the number of tokens a player has for a skill and tier
     */
    public int getTokenCount(String skillId, SkillToken.TokenTier tier) {
        String tierKey = skillId + "_tier_" + tier.getLevel();
        return tieredSkillTokens
            .getOrDefault(skillId, new HashMap<>())
            .getOrDefault(tierKey, 0);
    }
    
    /**
     * Get the number of tokens a player has for a skill (all tiers combined)
     * Used for backward compatibility and display purposes
     */
    public int getTokenCount(String skillId) {
        int total = 0;
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            total += getTokenCount(skillId, tier);
        }
        return total;
    }
    
    /**
     * Add tokens for a specific skill and tier
     */
    public void addTokens(String skillId, SkillToken.TokenTier tier, int count) {
        if (!tieredSkillTokens.containsKey(skillId)) {
            tieredSkillTokens.put(skillId, new HashMap<>());
        }
        
        String tierKey = skillId + "_tier_" + tier.getLevel();
        int current = tieredSkillTokens.get(skillId).getOrDefault(tierKey, 0);
        tieredSkillTokens.get(skillId).put(tierKey, current + count);
    }
    
    /**
     * Add tokens for a skill (defaults to Basic tier for backward compatibility)
     */
    public void addTokens(String skillId, int count) {
        addTokens(skillId, SkillToken.TokenTier.BASIC, count);
    }
    
    /**
     * Use tokens for a specific skill and tier
     * @return true if enough tokens were available, false otherwise
     */
    public boolean useTokens(String skillId, SkillToken.TokenTier tier, int count) {
        String tierKey = skillId + "_tier_" + tier.getLevel();
        int current = tieredSkillTokens
            .getOrDefault(skillId, new HashMap<>())
            .getOrDefault(tierKey, 0);
            
        if (current < count) {
            return false;
        }
        
        if (!tieredSkillTokens.containsKey(skillId)) {
            tieredSkillTokens.put(skillId, new HashMap<>());
        }
        
        tieredSkillTokens.get(skillId).put(tierKey, current - count);
        return true;
    }
    
    /**
     * Use tokens for a skill (tries tiers from highest to lowest)
     * @return true if enough tokens were available, false otherwise
     */
    public boolean useTokens(String skillId, int count) {
        // Try to use tokens starting from the highest tier
        SkillToken.TokenTier[] tiers = {
            SkillToken.TokenTier.MASTER,
            SkillToken.TokenTier.ADVANCED,
            SkillToken.TokenTier.BASIC
        };
        
        int remaining = count;
        Map<SkillToken.TokenTier, Integer> toDeduct = new HashMap<>();
        
        // Plan the deduction
        for (SkillToken.TokenTier tier : tiers) {
            int available = getTokenCount(skillId, tier);
            int toUse = Math.min(remaining, available);
            
            if (toUse > 0) {
                toDeduct.put(tier, toUse);
                remaining -= toUse;
            }
            
            if (remaining <= 0) break;
        }
        
        // Check if we have enough tokens total
        if (remaining > 0) {
            return false;
        }
        
        // Actually deduct the tokens
        for (Map.Entry<SkillToken.TokenTier, Integer> entry : toDeduct.entrySet()) {
            useTokens(skillId, entry.getKey(), entry.getValue());
        }
        
        return true;
    }
    
    /**
     * Set the token count for a specific skill and tier
     */
    public void setTokenCount(String skillId, SkillToken.TokenTier tier, int count) {
        if (!tieredSkillTokens.containsKey(skillId)) {
            tieredSkillTokens.put(skillId, new HashMap<>());
        }
        
        String tierKey = skillId + "_tier_" + tier.getLevel();
        tieredSkillTokens.get(skillId).put(tierKey, Math.max(0, count));
    }
    
    /**
     * Get all token counts for a skill (by tier)
     */
    public Map<SkillToken.TokenTier, Integer> getAllTokenCounts(String skillId) {
        Map<SkillToken.TokenTier, Integer> result = new HashMap<>();
        
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            result.put(tier, getTokenCount(skillId, tier));
        }
        
        return result;
    }
    
    /**
     * Check if a player can afford a node upgrade
     */
    public boolean canAffordNode(SkillTreeNode node, int currentLevel) {
        int cost = node.getTokenCost(currentLevel + 1);
        SkillToken.TokenTier requiredTier = node.getRequiredTokenTier();
        
        // Check if player has enough tokens of the required tier or higher
        int availableTokens = 0;
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            if (tier.getLevel() >= requiredTier.getLevel()) {
                availableTokens += getTokenCount(node.getId().split("_")[0], tier); // Extract skill from node ID
            }
        }
        
        return availableTokens >= cost;
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
     * Upgrade a node to the next level - SIMPLIFIED for main skills only
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
        
        // Get the player this belongs to
        Player player = findPlayerForSkillTree();
        if (player != null) {
            // Apply benefits based on the skill tree type
            
            // Main skill upgrades - delegate to the skill class
            Skill skill = SkillRegistry.getInstance().getSkill(skillId);
            if (skill != null && skill.isMainSkill()) {
                // Apply upgrades through the main skill
                if (skill instanceof com.server.profiles.skills.skills.mining.MiningSkill) {
                    com.server.profiles.skills.skills.mining.MiningSkill miningSkill = 
                        (com.server.profiles.skills.skills.mining.MiningSkill) skill;
                    miningSkill.applyNodeUpgrade(player, nodeId, oldLevel, newLevel);
                }
                // Add other main skills here as they are implemented
            }
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("[PlayerSkillTreeData] Applied upgrade for " + 
                                                nodeId + " from level " + oldLevel + " to " + newLevel);
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
     * @return The number of tokens refunded (for backwards compatibility)
     */
    public int resetSkillTree(String skillId) {
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Starting reset for skill: " + skillId);
        }
        
        // Get skill tree for proper token calculation
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) {
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().warning("[PlayerSkillTreeData] No skill tree found for: " + skillId);
            }
            return 0;
        }
        
        // Get current progress before clearing
        Set<String> unlockedNodes = getUnlockedNodes(skillId);
        Map<String, Integer> nodeLevels = getNodeLevels(skillId);
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Nodes before reset: " + unlockedNodes);
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Node levels before reset: " + nodeLevels);
        }
        
        // Calculate tokens to refund by tier
        Map<SkillToken.TokenTier, Integer> tierRefunds = new HashMap<>();
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            tierRefunds.put(tier, 0);
        }
        
        int totalTokensRefunded = 0;
        
        // Calculate refunds for each unlocked node
        for (String nodeId : unlockedNodes) {
            SkillTreeNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            // Skip root node and special nodes for token refund
            if (nodeId.equals("root") || node.isSpecialNode()) {
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("[PlayerSkillTreeData] Skipping refund for special/root node: " + nodeId);
                }
                continue;
            }
            
            // Calculate refund for normal nodes by tier
            int level = nodeLevels.getOrDefault(nodeId, 0);
            SkillToken.TokenTier requiredTier = node.getRequiredTokenTier();
            
            if (node.isUpgradable()) {
                // For upgradable nodes, refund cost for each level
                for (int i = 1; i <= level; i++) {
                    int cost = node.getTokenCost(i);
                    tierRefunds.put(requiredTier, tierRefunds.get(requiredTier) + cost);
                    totalTokensRefunded += cost;
                }
            } else {
                // For non-upgradable nodes, refund the base cost
                int cost = node.getTokenCost();
                tierRefunds.put(requiredTier, tierRefunds.get(requiredTier) + cost);
                totalTokensRefunded += cost;
            }
        }
        
        // Clear the regular node storage (but preserve special nodes)
        if (unlockedNodeLevels.containsKey(skillId)) {
            Map<String, Integer> skillNodes = unlockedNodeLevels.get(skillId);
            
            // Remove only non-special nodes
            skillNodes.entrySet().removeIf(entry -> {
                String nodeId = entry.getKey();
                SkillTreeNode node = tree.getNode(nodeId);
                return node != null && !node.isSpecialNode() && !nodeId.equals("root");
            });
            
            // If no nodes remain, remove the skill entry entirely
            if (skillNodes.isEmpty()) {
                unlockedNodeLevels.remove(skillId);
            }
        }
        
        // Add the refunded tokens by tier
        for (Map.Entry<SkillToken.TokenTier, Integer> entry : tierRefunds.entrySet()) {
            SkillToken.TokenTier tier = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0) {
                addTokens(skillId, tier, count);
                
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("[PlayerSkillTreeData] Refunded " + count + 
                                                    " " + tier.getDisplayName() + " tokens for " + skillId);
                }
            }
        }
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Reset complete for " + skillId + 
                                            ", total tokens refunded: " + totalTokensRefunded);
        }
        
        return totalTokensRefunded;
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
     * Set the level of a node directly - PUBLIC method for SkillTreeGUI
     */
    public void setNodeLevel(String skillId, String nodeId, int level) {
        // Make sure the maps exist
        if (!unlockedNodeLevels.containsKey(skillId)) {
            unlockedNodeLevels.put(skillId, new HashMap<>());
        }
        unlockedNodeLevels.get(skillId).put(nodeId, level);
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[PlayerSkillTreeData] Set node " + nodeId + 
                                            " to level " + level + " for skill " + skillId);
        }
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