package com.server.islands.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an island challenge that can be completed for island tokens.
 */
public class IslandChallenge {
    
    private final String id;
    private final String name;
    private final String description;
    private final ChallengeCategory category;
    private final ChallengeDifficulty difficulty;
    private final int tokenReward;
    private final List<String> prerequisites; // Challenge IDs that must be completed first
    private final ChallengeType type;
    private final String targetKey; // e.g., "WHEAT", "ZOMBIE", etc.
    private final int targetAmount;
    private final boolean isIslandWide; // true if all island members contribute, false if individual
    private final TreeGridPosition gridPosition; // Position in the challenge tree grid
    
    public IslandChallenge(String id, String name, String description, 
                          ChallengeCategory category, ChallengeDifficulty difficulty,
                          int tokenReward, List<String> prerequisites,
                          ChallengeType type, String targetKey, int targetAmount,
                          boolean isIslandWide, TreeGridPosition gridPosition) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.tokenReward = tokenReward;
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        this.type = type;
        this.targetKey = targetKey;
        this.targetAmount = targetAmount;
        this.isIslandWide = isIslandWide;
        this.gridPosition = gridPosition != null ? gridPosition : new TreeGridPosition(0, 0);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ChallengeCategory getCategory() {
        return category;
    }
    
    public ChallengeDifficulty getDifficulty() {
        return difficulty;
    }
    
    public int getTokenReward() {
        return tokenReward;
    }
    
    public List<String> getPrerequisites() {
        return new ArrayList<>(prerequisites);
    }
    
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
    
    public ChallengeType getType() {
        return type;
    }
    
    public String getTargetKey() {
        return targetKey;
    }
    
    public int getTargetAmount() {
        return targetAmount;
    }
    
    public boolean isIslandWide() {
        return isIslandWide;
    }
    
    public TreeGridPosition getGridPosition() {
        return gridPosition;
    }
    
    /**
     * Challenge types for different gameplay mechanics
     */
    public enum ChallengeType {
        HARVEST,        // Harvest crops on island
        MINING,         // Mine specific blocks on island
        KILL_MOBS,      // Kill specific mobs
        BREED_ANIMALS,  // Breed animals on island
        CRAFT_ITEMS,    // Craft specific items
        ENCHANT_ITEMS,  // Enchant items
        BUILD,          // Place specific blocks
        COLLECT,        // Collect specific items
        FISHING,        // Fish specific items
        TRADING,        // Trade with villagers
        EXPLORATION,    // Visit specific locations
        COOKING,        // Cook specific food
        ALCHEMY,        // Brew potions
        COMBAT,         // Combat-related challenges
        SOCIAL,         // Social interaction challenges
        ECONOMY,        // Economic challenges (spending/earning)
        ISLAND_LEVEL,   // Reach island level milestones
        CUSTOM          // Custom challenge with special logic
    }
    
    /**
     * Challenge difficulty levels affecting token rewards
     */
    public enum ChallengeDifficulty {
        STARTER(1, "Starter", "⭐"),           // 1-3 tokens
        EASY(2, "Easy", "⭐⭐"),               // 3-6 tokens
        MEDIUM(3, "Medium", "⭐⭐⭐"),         // 6-10 tokens
        HARD(4, "Hard", "⭐⭐⭐⭐"),           // 10-15 tokens
        EXPERT(5, "Expert", "⭐⭐⭐⭐⭐"),     // 15-20 tokens
        LEGENDARY(6, "Legendary", "✦✦✦✦✦✦"); // 20-25 tokens
        
        private final int level;
        private final String displayName;
        private final String icon;
        
        ChallengeDifficulty(int level, String displayName, String icon) {
            this.level = level;
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getIcon() {
            return icon;
        }
    }
    
    /**
     * Challenge categories for organization
     */
    public enum ChallengeCategory {
        FARMING("🌾", "Farming", "Harvest crops and raise animals"),
        MINING("⛏", "Mining", "Dig deep and gather resources"),
        COMBAT("⚔", "Combat", "Defeat powerful enemies"),
        BUILDING("🏗", "Building", "Construct magnificent structures"),
        CRAFTING("🔨", "Crafting", "Create items and equipment"),
        EXPLORATION("🗺", "Exploration", "Discover new locations"),
        ECONOMY("💰", "Economy", "Master the art of trade"),
        SOCIAL("👥", "Social", "Work together with others"),
        PROGRESSION("📈", "Progression", "Advance your island"),
        SPECIAL("✨", "Special", "Unique and challenging feats");
        
        private final String icon;
        private final String displayName;
        private final String description;
        
        ChallengeCategory(String icon, String displayName, String description) {
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getIcon() {
            return icon;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Builder pattern for creating challenges
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private ChallengeCategory category;
        private ChallengeDifficulty difficulty;
        private int tokenReward;
        private List<String> prerequisites = new ArrayList<>();
        private ChallengeType type;
        private String targetKey;
        private int targetAmount;
        private boolean isIslandWide = true;
        private TreeGridPosition gridPosition = new TreeGridPosition(0, 0);
        
        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder category(ChallengeCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder difficulty(ChallengeDifficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }
        
        public Builder tokenReward(int tokenReward) {
            this.tokenReward = tokenReward;
            return this;
        }
        
        public Builder prerequisite(String challengeId) {
            this.prerequisites.add(challengeId);
            return this;
        }
        
        public Builder prerequisites(List<String> challengeIds) {
            this.prerequisites.addAll(challengeIds);
            return this;
        }
        
        public Builder type(ChallengeType type) {
            this.type = type;
            return this;
        }
        
        public Builder targetKey(String targetKey) {
            this.targetKey = targetKey;
            return this;
        }
        
        public Builder targetAmount(int targetAmount) {
            this.targetAmount = targetAmount;
            return this;
        }
        
        public Builder isIslandWide(boolean isIslandWide) {
            this.isIslandWide = isIslandWide;
            return this;
        }
        
        public Builder gridPosition(int x, int y) {
            this.gridPosition = new TreeGridPosition(x, y);
            return this;
        }
        
        public Builder gridPosition(TreeGridPosition gridPosition) {
            this.gridPosition = gridPosition;
            return this;
        }
        
        public IslandChallenge build() {
            return new IslandChallenge(id, name, description, category, difficulty, 
                                      tokenReward, prerequisites, type, targetKey, 
                                      targetAmount, isIslandWide, gridPosition);
        }
    }
}
