package com.server.islands.managers;

import com.server.islands.data.PlayerIsland;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching system for active islands in memory.
 */
public class IslandCache {
    
    private final IslandManager manager;
    
    // Cache islands by ID
    private final Map<UUID, PlayerIsland> islands = new ConcurrentHashMap<>();
    
    // Cache islands by owner UUID for quick lookup
    private final Map<UUID, UUID> ownerToIsland = new ConcurrentHashMap<>();
    
    // Track which players are on which islands
    private final Map<UUID, Set<UUID>> islandPlayers = new ConcurrentHashMap<>();
    
    // Track unique visitors for statistics
    private final Map<UUID, Set<UUID>> uniqueVisitors = new ConcurrentHashMap<>();
    
    public IslandCache(IslandManager manager) {
        this.manager = manager;
    }
    
    /**
     * Caches an island in memory.
     */
    public void cacheIsland(PlayerIsland island) {
        islands.put(island.getIslandId(), island);
        ownerToIsland.put(island.getOwnerUuid(), island.getIslandId());
        islandPlayers.putIfAbsent(island.getIslandId(), ConcurrentHashMap.newKeySet());
        uniqueVisitors.putIfAbsent(island.getIslandId(), ConcurrentHashMap.newKeySet());
    }
    
    /**
     * Removes an island from cache.
     */
    public void removeIsland(UUID islandId) {
        PlayerIsland island = islands.remove(islandId);
        if (island != null) {
            ownerToIsland.remove(island.getOwnerUuid());
            islandPlayers.remove(islandId);
            uniqueVisitors.remove(islandId);
        }
    }
    
    /**
     * Gets a cached island by ID.
     */
    public PlayerIsland getIsland(UUID islandId) {
        return islands.get(islandId);
    }
    
    /**
     * Gets a cached island by owner UUID.
     */
    public PlayerIsland getIslandByOwner(UUID ownerUuid) {
        UUID islandId = ownerToIsland.get(ownerUuid);
        return islandId != null ? islands.get(islandId) : null;
    }
    
    /**
     * Gets all cached islands.
     */
    public Collection<PlayerIsland> getAllIslands() {
        return islands.values();
    }
    
    /**
     * Marks a player as visiting an island.
     */
    public void markPlayerVisit(UUID islandId, UUID playerUuid) {
        Set<UUID> players = islandPlayers.get(islandId);
        if (players != null) {
            players.add(playerUuid);
            
            // Track unique visitors
            Set<UUID> visitors = uniqueVisitors.get(islandId);
            if (visitors != null && !visitors.contains(playerUuid)) {
                visitors.add(playerUuid);
                
                // Update statistics if it's a new visitor
                PlayerIsland island = islands.get(islandId);
                if (island != null && !island.isOwner(playerUuid)) {
                    manager.getStatistics(islandId).thenAccept(stats -> {
                        if (stats != null) {
                            stats.incrementVisits();
                            if (visitors.size() > stats.getUniqueVisitors()) {
                                stats.incrementUniqueVisitors();
                            }
                            manager.updateStatistics(stats);
                        }
                    });
                }
            }
        }
    }
    
    /**
     * Marks a player as leaving an island.
     */
    public void markPlayerLeave(UUID islandId, UUID playerUuid) {
        Set<UUID> players = islandPlayers.get(islandId);
        if (players != null) {
            players.remove(playerUuid);
        }
    }
    
    /**
     * Gets all players currently on an island.
     */
    public Set<UUID> getPlayersOnIsland(UUID islandId) {
        Set<UUID> players = islandPlayers.get(islandId);
        return players != null ? new HashSet<>(players) : new HashSet<>();
    }
    
    /**
     * Clears all players from an island.
     */
    public void clearPlayers(UUID islandId) {
        Set<UUID> players = islandPlayers.get(islandId);
        if (players != null) {
            players.clear();
        }
    }
    
    /**
     * Checks if an island is loaded.
     */
    public boolean isLoaded(UUID islandId) {
        return islands.containsKey(islandId);
    }
    
    /**
     * Gets the number of cached islands.
     */
    public int getCachedCount() {
        return islands.size();
    }
    
    /**
     * Clears all cached data.
     */
    public void clear() {
        islands.clear();
        ownerToIsland.clear();
        islandPlayers.clear();
        uniqueVisitors.clear();
    }
}
