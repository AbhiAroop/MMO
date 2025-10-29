package com.server.profiles.skills.skills.fishing.minigame;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.server.profiles.skills.skills.fishing.baits.FishingBait;
import com.server.profiles.skills.skills.fishing.types.FishingType;

/**
 * Represents an active fishing session for a player
 */
public class FishingSession {
    private final UUID playerId;
    private final Player player;
    private final FishingType fishingType;
    private final FishingMinigame minigame;
    private FishingBait bait; // The bait used for this session
    
    private BukkitTask minigameTask;
    private boolean active;
    private final long startTime;
    
    // Session statistics
    private int successfulCatches;
    private int totalRounds;
    private int perfectCatches;
    
    public FishingSession(Player player, FishingType fishingType) {
        this.playerId = player.getUniqueId();
        this.player = player;
        this.fishingType = fishingType;
        this.minigame = new FishingMinigame(this);
        this.active = true;
        this.startTime = System.currentTimeMillis();
        this.successfulCatches = 0;
        this.totalRounds = 0;
        this.perfectCatches = 0;
        this.bait = null;
    }
    
    /**
     * Start the fishing minigame
     */
    public void startMinigame() {
        if (!active) {
            return;
        }
        minigame.start();
    }
    
    /**
     * Handle player attempting to catch (right-click during minigame)
     */
    public void attemptCatch() {
        if (!active || minigameTask == null) {
            return;
        }
        minigame.attemptCatch();
    }
    
    /**
     * Complete the fishing session successfully
     */
    public void complete() {
        if (!active) {
            return;
        }
        active = false;
        cleanup();
        minigame.onComplete();
        
        // Remove from session manager (don't call endSession to avoid recursion)
        FishingSessionManager.getInstance().removeSession(playerId);
    }
    
    /**
     * Cancel the fishing session (player stopped fishing)
     */
    public void cancel() {
        if (!active) {
            return;
        }
        active = false;
        cleanup();
        minigame.onCancel();
        
        // Remove from session manager (don't call endSession to avoid recursion)
        FishingSessionManager.getInstance().removeSession(playerId);
    }
    
    /**
     * Fail the fishing session (missed too many catches)
     */
    public void fail() {
        if (!active) {
            return;
        }
        active = false;
        cleanup();
        minigame.onFail();
        
        // Remove from session manager (don't call endSession to avoid recursion)
        FishingSessionManager.getInstance().removeSession(playerId);
    }
    
    /**
     * Cleanup session resources
     */
    private void cleanup() {
        if (minigameTask != null) {
            minigameTask.cancel();
            minigameTask = null;
        }
        minigame.cleanup();
    }
    
    // Getters and setters
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public FishingType getFishingType() {
        return fishingType;
    }
    
    public FishingMinigame getMinigame() {
        return minigame;
    }
    
    public BukkitTask getMinigameTask() {
        return minigameTask;
    }
    
    public void setMinigameTask(BukkitTask minigameTask) {
        this.minigameTask = minigameTask;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
    
    // Statistics
    
    public void incrementSuccessfulCatches() {
        this.successfulCatches++;
    }
    
    public void incrementTotalRounds() {
        this.totalRounds++;
    }
    
    public void incrementPerfectCatches() {
        this.perfectCatches++;
    }
    
    public int getSuccessfulCatches() {
        return successfulCatches;
    }
    
    public int getTotalRounds() {
        return totalRounds;
    }
    
    public int getPerfectCatches() {
        return perfectCatches;
    }
    
    public double getAccuracy() {
        if (totalRounds == 0) {
            return 0.0;
        }
        return (double) successfulCatches / totalRounds * 100.0;
    }
    
    public FishingBait getBait() {
        return bait;
    }
    
    public void setBait(FishingBait bait) {
        this.bait = bait;
    }
}
