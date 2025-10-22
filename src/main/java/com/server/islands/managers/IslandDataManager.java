package com.server.islands.managers;

import com.server.islands.data.*;
import com.server.islands.data.IslandMember.IslandRole;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages database operations for islands.
 * Uses SQLite for local storage.
 */
public class IslandDataManager {
    
    private final JavaPlugin plugin;
    private Connection connection;
    private final File databaseFile;
    
    public IslandDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "islands.db");
    }
    
    /**
     * Initializes the database connection and creates tables.
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create data folder if it doesn't exist
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                
                // Connect to database
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                
                // Create tables
                createTables();
                
                plugin.getLogger().info("Island database initialized successfully.");
                
            } catch (ClassNotFoundException | SQLException e) {
                plugin.getLogger().severe("Failed to initialize island database: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Closes the database connection.
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Island database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        });
    }
    
    /**
     * Creates the necessary database tables.
     */
    private void createTables() throws SQLException {
        String createIslandsTable = "CREATE TABLE IF NOT EXISTS player_islands (" +
                "island_id TEXT PRIMARY KEY," +
                "owner_uuid TEXT NOT NULL," +
                "island_name TEXT NOT NULL," +
                "island_type TEXT NOT NULL," +
                "world_name TEXT NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "last_accessed BIGINT NOT NULL," +
                "island_level INTEGER NOT NULL," +
                "island_value BIGINT NOT NULL," +
                "size_level INTEGER NOT NULL," +
                "player_limit_level INTEGER NOT NULL," +
                "redstone_limit_level INTEGER NOT NULL," +
                "crop_growth_level INTEGER NOT NULL," +
                "weather_control INTEGER NOT NULL," +
                "current_biome TEXT NOT NULL," +
                "spawn_x DOUBLE NOT NULL," +
                "spawn_y DOUBLE NOT NULL," +
                "spawn_z DOUBLE NOT NULL," +
                "spawn_yaw FLOAT NOT NULL," +
                "spawn_pitch FLOAT NOT NULL" +
                ")";
        
        String createMembersTable = "CREATE TABLE IF NOT EXISTS island_members (" +
                "island_id TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "added_at BIGINT NOT NULL," +
                "last_visit BIGINT NOT NULL," +
                "PRIMARY KEY (island_id, player_uuid)" +
                ")";
        
        String createStatisticsTable = "CREATE TABLE IF NOT EXISTS island_statistics (" +
                "island_id TEXT PRIMARY KEY," +
                "total_visits INTEGER NOT NULL," +
                "unique_visitors INTEGER NOT NULL," +
                "blocks_placed BIGINT NOT NULL," +
                "blocks_broken BIGINT NOT NULL," +
                "mobs_killed BIGINT NOT NULL," +
                "players_killed BIGINT NOT NULL," +
                "total_playtime BIGINT NOT NULL" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIslandsTable);
            stmt.execute(createMembersTable);
            stmt.execute(createStatisticsTable);
            
            // Create indexes for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON player_islands(owner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_member_player ON island_members(player_uuid)");
        }
    }
    
    // ==================== Island Operations ====================
    
    /**
     * Saves an island to the database.
     */
    public CompletableFuture<Void> saveIsland(PlayerIsland island) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO player_islands VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, island.getIslandId().toString());
                stmt.setString(2, island.getOwnerUuid().toString());
                stmt.setString(3, island.getIslandName());
                stmt.setString(4, island.getIslandType().name());
                stmt.setString(5, island.getWorldName());
                stmt.setLong(6, island.getCreatedAt());
                stmt.setLong(7, island.getLastAccessed());
                stmt.setInt(8, island.getIslandLevel());
                stmt.setLong(9, island.getIslandValue());
                stmt.setInt(10, island.getSizeLevel());
                stmt.setInt(11, island.getPlayerLimitLevel());
                stmt.setInt(12, island.getRedstoneLimitLevel());
                stmt.setInt(13, island.getCropGrowthLevel());
                stmt.setInt(14, island.hasWeatherControl() ? 1 : 0);
                stmt.setString(15, island.getCurrentBiome());
                stmt.setDouble(16, island.getSpawnX());
                stmt.setDouble(17, island.getSpawnY());
                stmt.setDouble(18, island.getSpawnZ());
                stmt.setFloat(19, island.getSpawnYaw());
                stmt.setFloat(20, island.getSpawnPitch());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save island: " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads an island from the database.
     */
    public CompletableFuture<PlayerIsland> loadIsland(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_islands WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return extractIslandFromResultSet(rs);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load island: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * Loads an island by owner UUID.
     */
    public CompletableFuture<PlayerIsland> loadIslandByOwner(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_islands WHERE owner_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ownerUuid.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return extractIslandFromResultSet(rs);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load island by owner: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * Deletes an island from the database.
     */
    public CompletableFuture<Void> deleteIsland(UUID islandId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_islands WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete island: " + e.getMessage());
            }
        });
    }
    
    /**
     * Extracts island data from a ResultSet.
     */
    private PlayerIsland extractIslandFromResultSet(ResultSet rs) throws SQLException {
        return new PlayerIsland(
            UUID.fromString(rs.getString("island_id")),
            UUID.fromString(rs.getString("owner_uuid")),
            rs.getString("island_name"),
            IslandType.valueOf(rs.getString("island_type")),
            rs.getString("world_name"),
            rs.getLong("created_at"),
            rs.getLong("last_accessed"),
            rs.getInt("island_level"),
            rs.getLong("island_value"),
            rs.getInt("size_level"),
            rs.getInt("player_limit_level"),
            rs.getInt("redstone_limit_level"),
            rs.getInt("crop_growth_level"),
            rs.getInt("weather_control") == 1,
            rs.getString("current_biome"),
            rs.getDouble("spawn_x"),
            rs.getDouble("spawn_y"),
            rs.getDouble("spawn_z"),
            rs.getFloat("spawn_yaw"),
            rs.getFloat("spawn_pitch")
        );
    }
    
    // ==================== Member Operations ====================
    
    /**
     * Saves a member to the database.
     */
    public CompletableFuture<Void> saveMember(IslandMember member) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO island_members VALUES (?,?,?,?,?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, member.getIslandId().toString());
                stmt.setString(2, member.getPlayerUuid().toString());
                stmt.setString(3, member.getRole().name());
                stmt.setLong(4, member.getAddedAt());
                stmt.setLong(5, member.getLastVisit());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save member: " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads all members of an island.
     */
    public CompletableFuture<List<IslandMember>> loadMembers(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            List<IslandMember> members = new ArrayList<>();
            String sql = "SELECT * FROM island_members WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    members.add(new IslandMember(
                        UUID.fromString(rs.getString("island_id")),
                        UUID.fromString(rs.getString("player_uuid")),
                        IslandRole.valueOf(rs.getString("role")),
                        rs.getLong("added_at"),
                        rs.getLong("last_visit")
                    ));
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load members: " + e.getMessage());
            }
            
            return members;
        });
    }
    
    /**
     * Deletes a member from an island.
     */
    public CompletableFuture<Void> deleteMember(UUID islandId, UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM island_members WHERE island_id = ? AND player_uuid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete member: " + e.getMessage());
            }
        });
    }
    
    /**
     * Deletes all members of an island.
     */
    public CompletableFuture<Void> deleteMembers(UUID islandId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM island_members WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete members: " + e.getMessage());
            }
        });
    }
    
    // ==================== Statistics Operations ====================
    
    /**
     * Saves statistics to the database.
     */
    public CompletableFuture<Void> saveStatistics(IslandStatistics stats) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO island_statistics VALUES (?,?,?,?,?,?,?,?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, stats.getIslandId().toString());
                stmt.setInt(2, stats.getTotalVisits());
                stmt.setInt(3, stats.getUniqueVisitors());
                stmt.setLong(4, stats.getBlocksPlaced());
                stmt.setLong(5, stats.getBlocksBroken());
                stmt.setLong(6, stats.getMobsKilled());
                stmt.setLong(7, stats.getPlayersKilled());
                stmt.setLong(8, stats.getTotalPlayTime());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save statistics: " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads statistics from the database.
     */
    public CompletableFuture<IslandStatistics> loadStatistics(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM island_statistics WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new IslandStatistics(
                        UUID.fromString(rs.getString("island_id")),
                        rs.getInt("total_visits"),
                        rs.getInt("unique_visitors"),
                        rs.getLong("blocks_placed"),
                        rs.getLong("blocks_broken"),
                        rs.getLong("mobs_killed"),
                        rs.getLong("players_killed"),
                        rs.getLong("total_playtime")
                    );
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load statistics: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * Deletes statistics from the database.
     */
    public CompletableFuture<Void> deleteStatistics(UUID islandId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM island_statistics WHERE island_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete statistics: " + e.getMessage());
            }
        });
    }
}
