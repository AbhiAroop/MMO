package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
    
    // Map of skill ID to token count
    private final Map<String, Integer> skillTokens;
    
    public PlayerSkillTreeData() {
        this.unlockedNodeLevels = new HashMap<>();
        this.skillTokens = new HashMap<>();
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
     */
    public int getNodeLevel(String skillId, String nodeId) {
        Map<String, Integer> nodes = unlockedNodeLevels.getOrDefault(skillId, new HashMap<>());
        return nodes.getOrDefault(nodeId, 0);
    }
    
    /**
     * Unlock a node at level 1
     */
    public void unlockNode(String skillId, String nodeId) {
        unlockNodeAtLevel(skillId, nodeId, 1);
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
     * @return The new level, or 0 if the node is not unlocked
     */
    public int upgradeNode(String skillId, String nodeId) {
        if (!isNodeUnlocked(skillId, nodeId)) {
            return 0;
        }
        
        int currentLevel = getNodeLevel(skillId, nodeId);
        int newLevel = currentLevel + 1;
        
        // Store the old level for benefit calculation
        int oldLevel = currentLevel;
        
        // Unlock the new level
        unlockNodeAtLevel(skillId, nodeId, newLevel);
        
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