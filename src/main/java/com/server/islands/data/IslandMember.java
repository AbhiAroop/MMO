package com.server.islands.data;

import java.util.UUID;

/**
 * Represents a member or trusted player on an island.
 */
public class IslandMember {
    
    private final UUID islandId;
    private final UUID playerUuid;
    private IslandRole role;  // Mutable to allow promotions/demotions
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
     * Set the member's role (for promotions/demotions).
     */
    public void setRole(IslandRole role) {
        this.role = role;
    }
    
    /**
     * Island roles for access control and permissions.
     */
    public enum IslandRole {
        OWNER("Owner", 100),          // Full control, can delete island, transfer ownership
        CO_OWNER("Co-Owner", 80),     // Nearly full control, cannot delete or transfer ownership
        ADMIN("Admin", 60),           // Can manage members, build, break, invite
        MOD("Moderator", 40),         // Can build, break, invite
        MEMBER("Member", 20),         // Can build and break
        VISITOR("Visitor", 1);        // Limited permissions
        
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
        
        /**
         * Check if this role can promote/demote to the target role.
         */
        public boolean canManageRole(IslandRole targetRole) {
            // Owners can manage anyone except other owners
            if (this == OWNER) {
                return targetRole != OWNER;
            }
            // Co-owners can manage admins, mods, and members
            if (this == CO_OWNER) {
                return targetRole.permission < CO_OWNER.permission;
            }
            // Admins can manage mods and members
            if (this == ADMIN) {
                return targetRole.permission < ADMIN.permission;
            }
            return false;
        }
    }
}
