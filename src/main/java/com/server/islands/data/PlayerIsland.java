package com.server.islands.data;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Represents a player's island with all its data and upgrade levels.
 */
public class PlayerIsland {
    
    // Core identifiers
    private final UUID islandId;
    private UUID ownerUuid;  // Mutable to allow ownership transfer
    private String worldName;
    
    // Island properties
    private String islandName;
    private IslandType islandType;
    private long createdAt;
    private long lastAccessed;
    
    // Progression
    private int islandLevel;
    private long islandValue;
    private int islandTokens; // Island tokens earned from challenges
    
    // Upgrade levels
    private int sizeLevel;
    private int playerLimitLevel;
    private int redstoneLimitLevel;
    private int cropGrowthLevel;
    
    // Features
    private boolean weatherControl;
    private String currentBiome;
    
    // Settings
    private boolean pvpEnabled;
    private boolean visitorsEnabled;
    
    // Statistics
    private IslandStatistics statistics;
    
    // Spawn location
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    
    /**
     * Creates a new island for a player.
     */
    public PlayerIsland(UUID ownerUuid, IslandType islandType) {
        this.islandId = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.islandType = islandType;
        this.islandName = "My Island";
        this.worldName = "island_" + islandId.toString();
        this.createdAt = System.currentTimeMillis();
        this.lastAccessed = System.currentTimeMillis();
        
        // Default values
        this.islandLevel = 1;
        this.islandValue = 0;
        this.islandTokens = 0;
        this.sizeLevel = 1;
        this.playerLimitLevel = 1;
        this.redstoneLimitLevel = 1;
        this.cropGrowthLevel = 1;
        this.weatherControl = false;
        this.currentBiome = getDefaultBiome();
        this.pvpEnabled = false; // PVP disabled by default
        this.visitorsEnabled = true; // Visitors allowed by default
        this.statistics = new IslandStatistics(islandId);
        
        // Default spawn (one block above platform at y=99)
        this.spawnX = 0.5; // Center of block
        this.spawnY = 101;
        this.spawnZ = 0.5; // Center of block
        this.spawnYaw = 0;
        this.spawnPitch = 0;
    }
    
    /**
     * Loads an existing island from database.
     */
    public PlayerIsland(UUID islandId, UUID ownerUuid, String islandName, IslandType islandType,
                       String worldName, long createdAt, long lastAccessed, int islandLevel,
                       long islandValue, int islandTokens, int sizeLevel, int playerLimitLevel, int redstoneLimitLevel,
                       int cropGrowthLevel, boolean weatherControl, String currentBiome,
                       boolean pvpEnabled, boolean visitorsEnabled,
                       double spawnX, double spawnY, double spawnZ, float spawnYaw, float spawnPitch) {
        this.islandId = islandId;
        this.ownerUuid = ownerUuid;
        this.islandName = islandName;
        this.islandType = islandType;
        this.worldName = worldName;
        this.createdAt = createdAt;
        this.lastAccessed = lastAccessed;
        this.islandLevel = islandLevel;
        this.islandValue = islandValue;
        this.islandTokens = islandTokens;
        this.sizeLevel = sizeLevel;
        this.playerLimitLevel = playerLimitLevel;
        this.redstoneLimitLevel = redstoneLimitLevel;
        this.cropGrowthLevel = cropGrowthLevel;
        this.weatherControl = weatherControl;
        this.currentBiome = currentBiome;
        this.pvpEnabled = pvpEnabled;
        this.visitorsEnabled = visitorsEnabled;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.spawnYaw = spawnYaw;
        this.spawnPitch = spawnPitch;
        this.statistics = new IslandStatistics(islandId);
    }
    
    // ==================== Size Methods ====================
    
    /**
     * Gets the current island size (always calculated from level).
     * Each level adds 2 blocks (1 in each direction).
     * Starting size: 25x25 (level 1)
     * Max size: 500x500 (level 238)
     */
    public int getCurrentSize() {
        // Base size is 25, each level adds 2 blocks
        return 25 + ((sizeLevel - 1) * 2);
    }
    
    /**
     * Gets the maximum size level allowed (500x500).
     */
    public int getMaxSizeLevel() {
        // (500 - 25) / 2 + 1 = 238 levels to reach 500x500
        return 238;
    }
    
    /**
     * Gets the next size upgrade level size.
     */
    public int getNextSize() {
        if (sizeLevel >= getMaxSizeLevel()) return getCurrentSize();
        return getCurrentSize() + 2;
    }
    
    /**
     * Gets the cost for the next size upgrade (1000 units per upgrade).
     */
    public int getSizeUpgradeCost() {
        if (sizeLevel >= getMaxSizeLevel()) return -1; // Max level
        return 1000;
    }
    
    // ==================== Player Limit Methods ====================
    
    /**
     * Gets the current player limit based on upgrade level.
     */
    public int getCurrentPlayerLimit() {
        switch (playerLimitLevel) {
            case 1: return 2;
            case 2: return 5;
            case 3: return 10;
            case 4: return 20;
            case 5: return 50;
            default: return 2;
        }
    }
    
    /**
     * Gets the cost for the next player limit upgrade.
     */
    public int getPlayerLimitUpgradeCost() {
        switch (playerLimitLevel + 1) {
            case 2: return 2000;
            case 3: return 10000;
            case 4: return 30000;
            case 5: return 100000;
            default: return -1; // Max level
        }
    }
    
    // ==================== Redstone Limit Methods ====================
    
    /**
     * Gets the current redstone limit based on upgrade level.
     * Starts at 10, increases by 5 per level, max 100 at level 19.
     */
    public int getCurrentRedstoneLimit() {
        return 10 + ((redstoneLimitLevel - 1) * 5);
    }
    
    /**
     * Gets the maximum redstone limit level (level 19 = 100 redstone items).
     */
    public int getMaxRedstoneLevel() {
        // (100 - 10) / 5 + 1 = 19 levels to reach 100
        return 19;
    }
    
    /**
     * Gets the next redstone limit value.
     */
    public int getNextRedstoneLimit() {
        if (redstoneLimitLevel >= getMaxRedstoneLevel()) {
            return getCurrentRedstoneLimit();
        }
        return getCurrentRedstoneLimit() + 5;
    }
    
    /**
     * Gets the cost for the next redstone limit upgrade.
     * Fixed cost of 1000 units per upgrade.
     */
    public int getRedstoneLimitUpgradeCost() {
        if (redstoneLimitLevel >= getMaxRedstoneLevel()) {
            return -1; // Max level
        }
        return 1000;
    }
    
    // ==================== Crop Growth Methods ====================
    
    /**
     * Gets the current crop growth multiplier based on upgrade level.
     */
    public double getCropGrowthMultiplier() {
        switch (cropGrowthLevel) {
            case 1: return 1.0;
            case 2: return 1.5;
            case 3: return 2.0;
            case 4: return 3.0;
            default: return 1.0;
        }
    }
    
    /**
     * Gets the cost for the next crop growth upgrade.
     */
    public int getCropGrowthUpgradeCost() {
        switch (cropGrowthLevel + 1) {
            case 2: return 10000;
            case 3: return 30000;
            case 4: return 100000;
            default: return -1; // Max level
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Gets the default biome for the island type.
     */
    private String getDefaultBiome() {
        switch (islandType) {
            case SKY: return "PLAINS";
            case OCEAN: return "BEACH";
            case FOREST: return "FOREST";
            default: return "PLAINS";
        }
    }
    
    /**
     * Gets the spawn location as a Bukkit Location.
     */
    public Location getSpawnLocation(World world) {
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }
    
    /**
     * Sets the spawn location from a Bukkit Location.
     */
    public void setSpawnLocation(Location location) {
        this.spawnX = location.getX();
        this.spawnY = location.getY();
        this.spawnZ = location.getZ();
        this.spawnYaw = location.getYaw();
        this.spawnPitch = location.getPitch();
    }
    
    /**
     * Updates the last accessed timestamp.
     */
    public void updateLastAccessed() {
        this.lastAccessed = System.currentTimeMillis();
    }
    
    /**
     * Checks if a player is the owner of this island.
     */
    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(ownerUuid);
    }
    
    /**
     * Checks if a player is the owner of this island by UUID.
     */
    public boolean isOwner(UUID playerUuid) {
        return playerUuid.equals(ownerUuid);
    }
    
    // ==================== Getters and Setters ====================
    
    public UUID getIslandId() {
        return islandId;
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public String getIslandName() {
        return islandName;
    }
    
    public void setIslandName(String islandName) {
        this.islandName = islandName;
    }
    
    public IslandType getIslandType() {
        return islandType;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
    
    public int getIslandLevel() {
        return islandLevel;
    }
    
    public void setIslandLevel(int islandLevel) {
        this.islandLevel = islandLevel;
    }
    
    public long getIslandValue() {
        return islandValue;
    }
    
    public void setIslandValue(long islandValue) {
        this.islandValue = islandValue;
    }
    
    public int getIslandTokens() {
        return islandTokens;
    }
    
    public void setIslandTokens(int islandTokens) {
        this.islandTokens = islandTokens;
    }
    
    public void addIslandTokens(int amount) {
        this.islandTokens += amount;
    }
    
    public boolean hasEnoughTokens(int amount) {
        return islandTokens >= amount;
    }
    
    public boolean removeIslandTokens(int amount) {
        if (hasEnoughTokens(amount)) {
            this.islandTokens -= amount;
            return true;
        }
        return false;
    }
    
    public int getSizeLevel() {
        return sizeLevel;
    }
    
    public void setSizeLevel(int sizeLevel) {
        this.sizeLevel = sizeLevel;
    }
    
    public int getPlayerLimitLevel() {
        return playerLimitLevel;
    }
    
    public void setPlayerLimitLevel(int playerLimitLevel) {
        this.playerLimitLevel = playerLimitLevel;
    }
    
    public int getRedstoneLimitLevel() {
        return redstoneLimitLevel;
    }
    
    public void setRedstoneLimitLevel(int redstoneLimitLevel) {
        this.redstoneLimitLevel = redstoneLimitLevel;
    }
    
    public int getCropGrowthLevel() {
        return cropGrowthLevel;
    }
    
    public void setCropGrowthLevel(int cropGrowthLevel) {
        this.cropGrowthLevel = cropGrowthLevel;
    }
    
    public boolean hasWeatherControl() {
        return weatherControl;
    }
    
    public void setWeatherControl(boolean weatherControl) {
        this.weatherControl = weatherControl;
    }
    
    public String getCurrentBiome() {
        return currentBiome;
    }
    
    public void setCurrentBiome(String currentBiome) {
        this.currentBiome = currentBiome;
    }
    
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
    
    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }
    
    public boolean isVisitorsEnabled() {
        return visitorsEnabled;
    }
    
    public void setVisitorsEnabled(boolean visitorsEnabled) {
        this.visitorsEnabled = visitorsEnabled;
    }
    
    public double getSpawnX() {
        return spawnX;
    }
    
    public double getSpawnY() {
        return spawnY;
    }
    
    public double getSpawnZ() {
        return spawnZ;
    }
    
    public float getSpawnYaw() {
        return spawnYaw;
    }
    
    public float getSpawnPitch() {
        return spawnPitch;
    }
    
    public IslandStatistics getStatistics() {
        return statistics;
    }
}
