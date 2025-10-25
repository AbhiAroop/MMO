package com.server.islands.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages a tree structure of challenges for a specific category
 */
public class ChallengeCategoryTree {
    private final IslandChallenge.ChallengeCategory category;
    private final Map<String, IslandChallenge> challenges;
    private final Map<String, Set<String>> connections; // challengeId -> set of connected challengeIds
    private final Map<TreeGridPosition, IslandChallenge> positionMap;
    
    public ChallengeCategoryTree(IslandChallenge.ChallengeCategory category) {
        this.category = category;
        this.challenges = new HashMap<>();
        this.connections = new HashMap<>();
        this.positionMap = new HashMap<>();
    }
    
    /**
     * Add a challenge to the tree
     */
    public void addChallenge(IslandChallenge challenge) {
        if (challenge.getCategory() != category) {
            throw new IllegalArgumentException("Challenge category does not match tree category");
        }
        
        challenges.put(challenge.getId(), challenge);
        positionMap.put(challenge.getGridPosition(), challenge);
        
        // Initialize connections set for this challenge
        connections.putIfAbsent(challenge.getId(), new HashSet<>());
        
        // Add connections to prerequisites
        for (String prerequisiteId : challenge.getPrerequisites()) {
            connections.putIfAbsent(prerequisiteId, new HashSet<>());
            connections.get(prerequisiteId).add(challenge.getId());
        }
    }
    
    /**
     * Get a challenge by ID
     */
    public IslandChallenge getChallenge(String challengeId) {
        return challenges.get(challengeId);
    }
    
    /**
     * Get a challenge at a specific grid position
     */
    public IslandChallenge getChallengeAtPosition(int x, int y) {
        return positionMap.get(new TreeGridPosition(x, y));
    }
    
    /**
     * Get all challenges in this tree
     */
    public Collection<IslandChallenge> getAllChallenges() {
        return new ArrayList<>(challenges.values());
    }
    
    /**
     * Get all connections in this tree
     */
    public Map<String, Set<String>> getAllConnections() {
        return new HashMap<>(connections);
    }
    
    /**
     * Get challenges connected to a specific challenge
     */
    public Set<String> getConnectedChallenges(String challengeId) {
        return new HashSet<>(connections.getOrDefault(challengeId, Collections.emptySet()));
    }
    
    /**
     * Check if a challenge's prerequisites are met
     */
    public boolean arePrerequisitesMet(String challengeId, Set<String> completedChallenges) {
        IslandChallenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            return false;
        }
        
        // If no prerequisites, it's available
        if (!challenge.hasPrerequisites()) {
            return true;
        }
        
        // Check if all prerequisites are completed
        for (String prerequisiteId : challenge.getPrerequisites()) {
            if (!completedChallenges.contains(prerequisiteId)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get the root challenge (challenge with no prerequisites)
     */
    public IslandChallenge getRootChallenge() {
        for (IslandChallenge challenge : challenges.values()) {
            if (!challenge.hasPrerequisites()) {
                return challenge;
            }
        }
        return null;
    }
    
    /**
     * Get all root challenges (challenges with no prerequisites)
     */
    public List<IslandChallenge> getRootChallenges() {
        List<IslandChallenge> roots = new ArrayList<>();
        for (IslandChallenge challenge : challenges.values()) {
            if (!challenge.hasPrerequisites()) {
                roots.add(challenge);
            }
        }
        return roots;
    }
    
    /**
     * Get category
     */
    public IslandChallenge.ChallengeCategory getCategory() {
        return category;
    }
    
    /**
     * Get available challenges (root challenges or challenges with completed prerequisites)
     */
    public List<IslandChallenge> getAvailableChallenges(Set<String> completedChallenges) {
        List<IslandChallenge> available = new ArrayList<>();
        
        for (IslandChallenge challenge : challenges.values()) {
            // Skip if already completed
            if (completedChallenges.contains(challenge.getId())) {
                continue;
            }
            
            // Check if prerequisites are met
            if (arePrerequisitesMet(challenge.getId(), completedChallenges)) {
                available.add(challenge);
            }
        }
        
        return available;
    }
    
    /**
     * Get the total number of challenges in this tree
     */
    public int size() {
        return challenges.size();
    }
}
