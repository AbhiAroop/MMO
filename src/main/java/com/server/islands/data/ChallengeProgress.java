package com.server.islands.data;

import java.util.UUID;

/**
 * Tracks progress for a challenge (either island-wide or player-specific)
 */
public class ChallengeProgress {
    
    private final String challengeId;
    private final UUID islandId;
    private final UUID playerId; // null if island-wide challenge
    private int currentProgress;
    private boolean completed;
    private long completedAt; // timestamp when completed (0 if not completed)
    
    public ChallengeProgress(String challengeId, UUID islandId, UUID playerId) {
        this.challengeId = challengeId;
        this.islandId = islandId;
        this.playerId = playerId;
        this.currentProgress = 0;
        this.completed = false;
        this.completedAt = 0;
    }
    
    public ChallengeProgress(String challengeId, UUID islandId, UUID playerId, 
                           int currentProgress, boolean completed, long completedAt) {
        this.challengeId = challengeId;
        this.islandId = islandId;
        this.playerId = playerId;
        this.currentProgress = currentProgress;
        this.completed = completed;
        this.completedAt = completedAt;
    }
    
    public String getChallengeId() {
        return challengeId;
    }
    
    public UUID getIslandId() {
        return islandId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isIslandWide() {
        return playerId == null;
    }
    
    public int getCurrentProgress() {
        return currentProgress;
    }
    
    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }
    
    public void incrementProgress(int amount) {
        this.currentProgress += amount;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completedAt == 0) {
            this.completedAt = System.currentTimeMillis();
        }
    }
    
    public long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Get progress percentage (0-100)
     */
    public int getProgressPercentage(int targetAmount) {
        if (targetAmount <= 0) return 0;
        return Math.min(100, (currentProgress * 100) / targetAmount);
    }
    
    /**
     * Check if challenge is ready to complete based on target
     */
    public boolean isReadyToComplete(int targetAmount) {
        return currentProgress >= targetAmount;
    }
}
