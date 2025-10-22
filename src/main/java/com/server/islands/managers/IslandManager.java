package com.server.islands.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.server.islands.data.IslandMember;
import com.server.islands.data.IslandStatistics;
import com.server.islands.data.IslandType;
import com.server.islands.data.PlayerIsland;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Main manager for island operations.
 * Handles island creation, loading, unloading, and player interactions.
 */
public class IslandManager {
    
    private final JavaPlugin plugin;
    private final IslandWorldManager worldManager;
    private final IslandDataManager dataManager;
    private final IslandCache cache;
    
    // Track which island each player is currently on
    private final Map<UUID, UUID> playerLocations = new HashMap<>();
    
    public IslandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new IslandWorldManager(plugin);
        this.dataManager = new IslandDataManager(plugin);
        this.cache = new IslandCache(this);
        
        // Start auto-unload task (check every minute)
        startAutoUnloadTask();
    }
    
    /**
     * Initializes the island system.
     */
    public CompletableFuture<Void> initialize() {
        return dataManager.initialize();
    }
    
    /**
     * Shuts down the island system.
     */
    public CompletableFuture<Void> shutdown() {
        cache.clear();
        return dataManager.shutdown();
    }
    
    // ==================== Island Creation ====================
    
    /**
     * Creates a new island for a player.
     */
    public CompletableFuture<PlayerIsland> createIsland(Player player, IslandType type) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            
            // Check if player already has an island
            if (hasIsland(playerUuid)) {
                return null;
            }
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < type.getCost()) {
                return null;
            }
            
            // Deduct cost
            profile.removeUnits(type.getCost());
            
            // Create island data
            PlayerIsland island = new PlayerIsland(playerUuid, type);
            
            // Save to database
            dataManager.saveIsland(island).join();
            
            // Add owner as member
            IslandMember owner = new IslandMember(island.getIslandId(), playerUuid, IslandMember.IslandRole.OWNER);
            dataManager.saveMember(owner).join();
            
            // Create statistics
            IslandStatistics stats = new IslandStatistics(island.getIslandId());
            dataManager.saveStatistics(stats).join();
            
            // Generate world
            worldManager.generateIslandWorld(island).join();
            
            // Cache the island
            cache.cacheIsland(island);
            
            return island;
        });
    }
    
    /**
     * Deletes a player's island.
     */
    public CompletableFuture<Boolean> deleteIsland(UUID ownerUuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        CompletableFuture.supplyAsync(() -> {
            PlayerIsland island = getIslandByOwner(ownerUuid).join();
            if (island == null) {
                future.complete(false);
                return null;
            }
            
            UUID islandId = island.getIslandId();
            String worldName = island.getWorldName();
            
            // Kick all players from the island (runs on main thread)
            Bukkit.getScheduler().runTask(plugin, () -> {
                kickAllPlayersSync(islandId);
                
                // Wait a bit for teleports to complete, then proceed with deletion
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Unload world
                    worldManager.unloadWorld(worldName).thenAccept(unloaded -> {
                        // Delete world files
                        worldManager.deleteWorld(worldName);
                        
                        // Remove from cache
                        cache.removeIsland(islandId);
                        
                        // Delete from database
                        CompletableFuture.allOf(
                            dataManager.deleteIsland(islandId),
                            dataManager.deleteMembers(islandId),
                            dataManager.deleteStatistics(islandId)
                        ).thenRun(() -> {
                            future.complete(true);
                        });
                    });
                }, 5L); // Wait 5 ticks (250ms) for teleports to complete
            });
            
            return null;
        });
        
        return future;
    }
    
    // ==================== Island Loading ====================
    
    /**
     * Loads an island by ID.
     */
    public CompletableFuture<PlayerIsland> loadIsland(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            PlayerIsland cached = cache.getIsland(islandId);
            if (cached != null) {
                cached.updateLastAccessed();
                return cached;
            }
            
            // Load from database
            PlayerIsland island = dataManager.loadIsland(islandId).join();
            if (island == null) {
                return null;
            }
            
            // Load the world
            worldManager.loadWorld(island).join();
            
            // Cache it
            cache.cacheIsland(island);
            island.updateLastAccessed();
            
            return island;
        });
    }
    
    /**
     * Gets an island by owner UUID (loads it if not cached).
     */
    public CompletableFuture<PlayerIsland> getIsland(UUID ownerUuid) {
        return getIslandByOwner(ownerUuid);
    }
    
    /**
     * Gets an island by owner UUID.
     */
    public CompletableFuture<PlayerIsland> getIslandByOwner(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            PlayerIsland cached = cache.getIslandByOwner(ownerUuid);
            if (cached != null) {
                return cached;
            }
            
            // Load from database
            PlayerIsland island = dataManager.loadIslandByOwner(ownerUuid).join();
            if (island != null) {
                // Load the world
                worldManager.loadWorld(island).join();
                // Cache it
                cache.cacheIsland(island);
            }
            return island;
        });
    }
    
    /**
     * Checks if a player has an island.
     */
    public boolean hasIsland(UUID playerUuid) {
        return getIslandByOwner(playerUuid).join() != null;
    }
    
    // ==================== Teleportation ====================
    
    /**
     * Teleports a player to their island.
     */
    public CompletableFuture<Boolean> teleportToIsland(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerIsland island = getIslandByOwner(player.getUniqueId()).join();
            if (island == null) {
                return false;
            }
            
            return teleportToIsland(player, island).join();
        });
    }
    
    /**
     * Teleports a player to an island by owner UUID.
     */
    public CompletableFuture<Boolean> teleportToIsland(Player player, UUID islandOwner) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerIsland island = getIslandByOwner(islandOwner).join();
            if (island == null) {
                return false;
            }
            
            return teleportToIsland(player, island).join();
        });
    }
    
    /**
     * Teleports a player to a specific island.
     */
    public CompletableFuture<Boolean> teleportToIsland(Player player, PlayerIsland island) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Load the island if not loaded
        loadIsland(island.getIslandId()).thenAccept(loadedIsland -> {
            plugin.getLogger().info("[Island] Island loaded successfully for " + island.getWorldName());
            
            // Give the world a tick to fully register with Bukkit
            // Run on main thread after a small delay to ensure world is accessible
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Get the world
                    World world = Bukkit.getWorld(island.getWorldName());
                    if (world == null) {
                        plugin.getLogger().warning("[Island] World not found after loading: " + island.getWorldName());
                        plugin.getLogger().warning("[Island] Available worlds: " + Bukkit.getWorlds().stream()
                            .map(World::getName).toList());
                        future.complete(false);
                        return;
                    }
                    
                    plugin.getLogger().info("[Island] World found: " + world.getName());
                    
                    // Get spawn location
                    Location spawn = island.getSpawnLocation(world);
                    plugin.getLogger().info("[Island] Teleporting " + player.getName() + " to " + spawn);
                    
                    // Ensure chunk is loaded before teleporting
                    int chunkX = spawn.getBlockX() >> 4;
                    int chunkZ = spawn.getBlockZ() >> 4;
                    plugin.getLogger().info("[Island] Loading chunk at " + chunkX + ", " + chunkZ);
                    
                    // Load the chunk synchronously
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        world.loadChunk(chunkX, chunkZ);
                        plugin.getLogger().info("[Island] Chunk loaded");
                    } else {
                        plugin.getLogger().info("[Island] Chunk already loaded");
                    }
                    
                    // Check if spawn location is safe
                    org.bukkit.block.Block blockAt = spawn.getBlock();
                    org.bukkit.block.Block blockAbove = spawn.clone().add(0, 1, 0).getBlock();
                    org.bukkit.block.Block blockBelow = spawn.clone().add(0, -1, 0).getBlock();
                    plugin.getLogger().info("[Island] Block at spawn: " + blockAt.getType() + " (y=" + blockAt.getY() + ")");
                    plugin.getLogger().info("[Island] Block above spawn: " + blockAbove.getType() + " (y=" + blockAbove.getY() + ")");
                    plugin.getLogger().info("[Island] Block below spawn: " + blockBelow.getType() + " (y=" + blockBelow.getY() + ")");
                    plugin.getLogger().info("[Island] Player current world: " + player.getWorld().getName());
                    plugin.getLogger().info("[Island] Target world: " + spawn.getWorld().getName());
                    plugin.getLogger().info("[Island] Worlds equal: " + (player.getWorld().equals(spawn.getWorld())));
                    
                    // Force load the player into the world first if different worlds
                    if (!player.getWorld().equals(spawn.getWorld())) {
                        plugin.getLogger().info("[Island] Cross-world teleport detected, using vanilla command");
                    }
                    
                    // Use vanilla execute command for cross-world teleportation (Bukkit API fails for SlimeWorlds)
                    // This mimics: /execute in <world> run tp @s <x> <y> <z>
                    String command = String.format("execute in %s run tp %s %.2f %.2f %.2f %.2f %.2f",
                        world.getName(),
                        player.getName(),
                        spawn.getX(),
                        spawn.getY(),
                        spawn.getZ(),
                        spawn.getYaw(),
                        spawn.getPitch()
                    );
                    
                    plugin.getLogger().info("[Island] Executing command: /" + command);
                    boolean teleported = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    plugin.getLogger().info("[Island] Teleport result: " + teleported);
                    
                    if (!teleported) {
                        plugin.getLogger().warning("[Island] Teleport failed. Player state:");
                        plugin.getLogger().warning("[Island]   - Is online: " + player.isOnline());
                        plugin.getLogger().warning("[Island]   - Is valid: " + player.isValid());
                        plugin.getLogger().warning("[Island]   - Health: " + player.getHealth() + "/" + player.getMaxHealth());
                    }
                    
                    if (teleported) {
                        // Track player location
                        playerLocations.put(player.getUniqueId(), island.getIslandId());
                        
                        // Mark player as visiting
                        cache.markPlayerVisit(island.getIslandId(), player.getUniqueId());
                    }
                    
                    future.complete(teleported);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Island] Error during teleport: " + e.getMessage());
                    e.printStackTrace();
                    future.complete(false);
                }
            }, 2L); // Wait 2 ticks (100ms) for world to fully register
        });
        
        return future;
    }
    
    /**
     * Teleports a player to another player's island.
     */
    public CompletableFuture<Boolean> visitIsland(Player visitor, UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerIsland island = getIslandByOwner(ownerUuid).join();
            if (island == null) {
                return false;
            }
            
            // Check if island allows visitors
            int currentPlayers = cache.getPlayersOnIsland(island.getIslandId()).size();
            if (currentPlayers >= island.getCurrentPlayerLimit()) {
                return false;
            }
            
            return teleportToIsland(visitor, island).join();
        });
    }
    
    // ==================== Player Management ====================
    
    /**
     * Gets the island a player is currently on.
     */
    public UUID getCurrentIsland(UUID playerUuid) {
        return playerLocations.get(playerUuid);
    }
    
    /**
     * Marks a player as leaving an island.
     */
    public void playerLeftIsland(UUID playerUuid) {
        UUID islandId = playerLocations.remove(playerUuid);
        if (islandId != null) {
            cache.markPlayerLeave(islandId, playerUuid);
        }
    }
    
    /**
     * Kicks all players from an island.
     */
    /**
     * Kicks all players from an island synchronously (must be called on main thread)
     */
    private void kickAllPlayersSync(UUID islandId) {
        Set<UUID> players = cache.getPlayersOnIsland(islandId);
        for (UUID playerUuid : players) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                World mainWorld = Bukkit.getWorld("world");
                if (mainWorld == null) {
                    mainWorld = Bukkit.getWorlds().get(0);
                }
                Location spawn = mainWorld.getSpawnLocation();
                
                // Use vanilla command for reliable cross-world teleportation
                // Get the world key properly - for overworld it's "minecraft:overworld"
                String worldKey = mainWorld.key().asString();
                String command = String.format("execute in %s run tp %s %.2f %.2f %.2f",
                    worldKey,
                    player.getName(),
                    spawn.getX(),
                    spawn.getY(),
                    spawn.getZ()
                );
                
                plugin.getLogger().info("[Island] Kicking " + player.getName() + " from island, teleporting to spawn");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.sendMessage("§cThe island you were on has been deleted.");
            }
            playerLocations.remove(playerUuid);
        }
        cache.clearPlayers(islandId);
    }
    
    // ==================== Statistics ====================
    
    /**
     * Gets island statistics.
     */
    public CompletableFuture<IslandStatistics> getStatistics(UUID islandId) {
        return dataManager.loadStatistics(islandId);
    }
    
    /**
     * Updates island statistics.
     */
    public CompletableFuture<Void> updateStatistics(IslandStatistics stats) {
        return dataManager.saveStatistics(stats);
    }
    
    // ==================== Members ====================
    
    /**
     * Gets all members of an island.
     */
    public CompletableFuture<List<IslandMember>> getMembers(UUID islandId) {
        return dataManager.loadMembers(islandId);
    }
    
    /**
     * Adds a member to an island.
     */
    public CompletableFuture<Void> addMember(UUID islandId, UUID playerUuid, IslandMember.IslandRole role) {
        return CompletableFuture.supplyAsync(() -> {
            IslandMember member = new IslandMember(islandId, playerUuid, role);
            dataManager.saveMember(member).join();
            return null;
        });
    }
    
    /**
     * Removes a member from an island.
     */
    public CompletableFuture<Void> removeMember(UUID islandId, UUID playerUuid) {
        return dataManager.deleteMember(islandId, playerUuid);
    }
    
    // ==================== Auto-Unload System ====================
    
    /**
     * Starts the auto-unload task to unload inactive islands.
     */
    private void startAutoUnloadTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long unloadThreshold = 5 * 60 * 1000; // 5 minutes
            
            List<UUID> toUnload = new ArrayList<>();
            
            for (PlayerIsland island : cache.getAllIslands()) {
                // Skip if players are on the island
                if (!cache.getPlayersOnIsland(island.getIslandId()).isEmpty()) {
                    continue;
                }
                
                // Check if island has been inactive
                if (currentTime - island.getLastAccessed() > unloadThreshold) {
                    toUnload.add(island.getIslandId());
                }
            }
            
            // Unload inactive islands
            for (UUID islandId : toUnload) {
                PlayerIsland island = cache.getIsland(islandId);
                if (island != null) {
                    // Save to database
                    dataManager.saveIsland(island).join();
                    
                    // Unload world
                    worldManager.unloadWorld(island.getWorldName()).join();
                    
                    // Remove from cache
                    cache.removeIsland(islandId);
                    
                    plugin.getLogger().info("Auto-unloaded island: " + island.getWorldName());
                }
            }
            
        }, 1200L, 1200L); // Run every minute (1200 ticks)
    }
    
    // ==================== Getters ====================
    
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    public IslandWorldManager getWorldManager() {
        return worldManager;
    }
    
    public IslandDataManager getDataManager() {
        return dataManager;
    }
    
    public IslandCache getCache() {
        return cache;
    }
    
    public IslandUpgradeManager getUpgradeManager() {
        return new IslandUpgradeManager(plugin, this);
    }
}
