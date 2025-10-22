package com.server.islands.data;

import java.util.UUID;

/**
 * Represents an invitation to join an island.
 */
public class IslandInvite {
    
    private final UUID islandId;
    private final UUID invitedPlayer;
    private final UUID invitedBy;
    private final long invitedAt;
    private final long expiresAt;
    
    /**
     * Creates a new island invitation that expires in 5 minutes.
     */
    public IslandInvite(UUID islandId, UUID invitedPlayer, UUID invitedBy) {
        this.islandId = islandId;
        this.invitedPlayer = invitedPlayer;
        this.invitedBy = invitedBy;
        this.invitedAt = System.currentTimeMillis();
        this.expiresAt = invitedAt + (5 * 60 * 1000); // 5 minutes
    }
    
    public IslandInvite(UUID islandId, UUID invitedPlayer, UUID invitedBy, long invitedAt, long expiresAt) {
        this.islandId = islandId;
        this.invitedPlayer = invitedPlayer;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Check if this invitation has expired.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    // ==================== Getters ====================
    
    public UUID getIslandId() {
        return islandId;
    }
    
    public UUID getInvitedPlayer() {
        return invitedPlayer;
    }
    
    public UUID getInvitedBy() {
        return invitedBy;
    }
    
    public long getInvitedAt() {
        return invitedAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
}
