package com.server.profiles.skills.skills.fishing.minigame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Manages active fishing sessions for all players
 */
public class FishingSessionManager {
    private static FishingSessionManager instance;
    
    private final Map<UUID, FishingSession> activeSessions;
    
    private FishingSessionManager() {
        this.activeSessions = new HashMap<>();
    }
    
    public static FishingSessionManager getInstance() {
        if (instance == null) {
            instance = new FishingSessionManager();
        }
        return instance;
    }
    
    /**
     * Create a new fishing session for a player
     */
    public FishingSession createSession(Player player, com.server.profiles.skills.skills.fishing.types.FishingType fishingType) {
        // End any existing session
        endSession(player);
        
        FishingSession session = new FishingSession(player, fishingType);
        activeSessions.put(player.getUniqueId(), session);
        return session;
    }
    
    /**
     * Get active session for a player
     */
    public FishingSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }
    
    /**
     * Get active session by UUID
     */
    public FishingSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    /**
     * Check if player has an active session
     */
    public boolean hasActiveSession(Player player) {
        FishingSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.isActive();
    }
    
    /**
     * End a player's fishing session
     */
    public void endSession(Player player) {
        FishingSession session = activeSessions.remove(player.getUniqueId());
        if (session != null && session.isActive()) {
            session.cancel();
        }
    }
    
    /**
     * End session by UUID
     */
    public void endSession(UUID playerId) {
        FishingSession session = activeSessions.remove(playerId);
        if (session != null && session.isActive()) {
            session.cancel();
        }
    }
    
    /**
     * End all active sessions (for plugin disable)
     */
    public void endAllSessions() {
        for (FishingSession session : activeSessions.values()) {
            if (session.isActive()) {
                session.cancel();
            }
        }
        activeSessions.clear();
    }
    
    /**
     * Get count of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
