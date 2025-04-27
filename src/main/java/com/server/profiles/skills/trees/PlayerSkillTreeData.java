package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores a player's progress in skill trees
 */
public class PlayerSkillTreeData {
    // Map of skill ID to unlocked node IDs
    private final Map<String, Set<String>> unlockedNodes;
    
    // Map of skill ID to token count
    private final Map<String, Integer> skillTokens;
    
    public PlayerSkillTreeData() {
        this.unlockedNodes = new HashMap<>();
        this.skillTokens = new HashMap<>();
    }
    
    /**
     * Get all unlocked nodes for a skill
     */
    public Set<String> getUnlockedNodes(String skillId) {
        return new HashSet<>(unlockedNodes.getOrDefault(skillId, new HashSet<>()));
    }
    
    /**
     * Check if a node is unlocked
     */
    public boolean isNodeUnlocked(String skillId, String nodeId) {
        Set<String> nodes = unlockedNodes.getOrDefault(skillId, new HashSet<>());
        return nodes.contains(nodeId);
    }
    
    /**
     * Unlock a node
     */
    public void unlockNode(String skillId, String nodeId) {
        if (!unlockedNodes.containsKey(skillId)) {
            unlockedNodes.put(skillId, new HashSet<>());
        }
        unlockedNodes.get(skillId).add(nodeId);
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
}