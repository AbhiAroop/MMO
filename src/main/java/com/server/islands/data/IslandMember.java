package com.server.islands.data;

import java.util.UUID;

/**
 * Represents a member or trusted player on an island.
 */
public class IslandMember {
    
    private final UUID islandId;
    private final UUID playerUuid;
    private final IslandRole role;
    private final long addedAt;
    private long lastVisit;
    
    public IslandMember(UUID islandId, UUID playerUuid, IslandRole role) {
        this.islandId = islandId;
        this.playerUuid = playerUuid;
        this.role = role;
        this.addedAt = System.currentTimeMillis();
        this.lastVisit = System.currentTimeMillis();
    }
    
    public IslandMember(UUID islandId, UUID playerUuid, IslandRole role, long addedAt, long lastVisit) {
        this.islandId = islandId;
        this.playerUuid = playerUuid;
        this.role = role;
        this.addedAt = addedAt;
        this.lastVisit = lastVisit;
    }
    
    /**
     * Updates the last visit timestamp.
     */
    public void updateLastVisit() {
        this.lastVisit = System.currentTimeMillis();
    }
    
    // ==================== Getters ====================
    
    public UUID getIslandId() {
        return islandId;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public IslandRole getRole() {
        return role;
    }
    
    public long getAddedAt() {
        return addedAt;
    }
    
    public long getLastVisit() {
        return lastVisit;
    }
    
    /**
     * Island roles for access control.
     */
    public enum IslandRole {
        OWNER("Owner", 3),
        TRUSTED("Trusted", 2),
        VISITOR("Visitor", 1);
        
        private final String displayName;
        private final int permission;
        
        IslandRole(String displayName, int permission) {
            this.displayName = displayName;
            this.permission = permission;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getPermission() {
            return permission;
        }
        
        public boolean hasPermission(IslandRole required) {
            return this.permission >= required.permission;
        }
    }
}
