package com.server.islands.data;

import java.util.UUID;

/**
 * Tracks statistics and activity for an island.
 */
public class IslandStatistics {
    
    private final UUID islandId;
    
    // Visit tracking
    private int totalVisits;
    private int uniqueVisitors;
    
    // Block tracking
    private long blocksPlaced;
    private long blocksBroken;
    
    // Entity tracking
    private long mobsKilled;
    private long playersKilled;
    
    // Time tracking
    private long totalPlayTime; // In milliseconds
    
    public IslandStatistics(UUID islandId) {
        this.islandId = islandId;
        this.totalVisits = 0;
        this.uniqueVisitors = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.mobsKilled = 0;
        this.playersKilled = 0;
        this.totalPlayTime = 0;
    }
    
    public IslandStatistics(UUID islandId, int totalVisits, int uniqueVisitors, 
                           long blocksPlaced, long blocksBroken, long mobsKilled, 
                           long playersKilled, long totalPlayTime) {
        this.islandId = islandId;
        this.totalVisits = totalVisits;
        this.uniqueVisitors = uniqueVisitors;
        this.blocksPlaced = blocksPlaced;
        this.blocksBroken = blocksBroken;
        this.mobsKilled = mobsKilled;
        this.playersKilled = playersKilled;
        this.totalPlayTime = totalPlayTime;
    }
    
    // ==================== Incrementer Methods ====================
    
    public void incrementVisits() {
        this.totalVisits++;
    }
    
    public void incrementUniqueVisitors() {
        this.uniqueVisitors++;
    }
    
    public void incrementBlocksPlaced(int amount) {
        this.blocksPlaced += amount;
    }
    
    public void incrementBlocksBroken(int amount) {
        this.blocksBroken += amount;
    }
    
    public void incrementMobsKilled() {
        this.mobsKilled++;
    }
    
    public void incrementPlayersKilled() {
        this.playersKilled++;
    }
    
    public void addPlayTime(long milliseconds) {
        this.totalPlayTime += milliseconds;
    }
    
    // ==================== Getters ====================
    
    public UUID getIslandId() {
        return islandId;
    }
    
    public int getTotalVisits() {
        return totalVisits;
    }
    
    public int getUniqueVisitors() {
        return uniqueVisitors;
    }
    
    public long getBlocksPlaced() {
        return blocksPlaced;
    }
    
    public long getBlocksBroken() {
        return blocksBroken;
    }
    
    public long getMobsKilled() {
        return mobsKilled;
    }
    
    public long getPlayersKilled() {
        return playersKilled;
    }
    
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    /**
     * Gets total play time in hours.
     */
    public double getTotalPlayTimeHours() {
        return totalPlayTime / (1000.0 * 60.0 * 60.0);
    }
}
