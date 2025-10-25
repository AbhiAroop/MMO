package com.server.islands.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.plugin.java.JavaPlugin;

import com.server.islands.data.IslandInvite;
import com.server.islands.data.IslandMember;
import com.server.islands.data.IslandMember.IslandRole;
import com.server.islands.data.IslandStatistics;
import com.server.islands.data.IslandType;
import com.server.islands.data.PlayerIsland;

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
                "island_tokens INTEGER NOT NULL DEFAULT 0," +
                "size_level INTEGER NOT NULL," +
                "player_limit_level INTEGER NOT NULL," +
                "redstone_limit_level INTEGER NOT NULL," +
                "crop_growth_level INTEGER NOT NULL," +
                "weather_control INTEGER NOT NULL," +
                "current_biome TEXT NOT NULL," +
                "pvp_enabled INTEGER NOT NULL DEFAULT 0," +
                "visitors_enabled INTEGER NOT NULL DEFAULT 1," +
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
        
        String createInvitesTable = "CREATE TABLE IF NOT EXISTS island_invites (" +
                "island_id TEXT NOT NULL," +
                "invited_player TEXT NOT NULL," +
                "invited_by TEXT NOT NULL," +
                "invited_at BIGINT NOT NULL," +
                "expires_at BIGINT NOT NULL," +
                "PRIMARY KEY (island_id, invited_player)" +
                ")";        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIslandsTable);
            stmt.execute(createMembersTable);
            stmt.execute(createStatisticsTable);
            stmt.execute(createInvitesTable);
            
            // Add new columns if they don't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE player_islands ADD COLUMN pvp_enabled INTEGER NOT NULL DEFAULT 0");
            } catch (Exception e) {
                // Column already exists, ignore
            }
            try {
                stmt.execute("ALTER TABLE player_islands ADD COLUMN visitors_enabled INTEGER NOT NULL DEFAULT 1");
            } catch (Exception e) {
                // Column already exists, ignore
            }
            
            // Create indexes for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON player_islands(owner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_member_player ON island_members(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_invite_player ON island_invites(invited_player)");
        }
    }
    
    // ==================== Island Operations ====================
    
    /**
     * Saves an island to the database.
     */
    public CompletableFuture<Void> saveIsland(PlayerIsland island) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO player_islands VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
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
                stmt.setInt(10, island.getIslandTokens());
                stmt.setInt(11, island.getSizeLevel());
                stmt.setInt(12, island.getPlayerLimitLevel());
                stmt.setInt(13, island.getRedstoneLimitLevel());
                stmt.setInt(14, island.getCropGrowthLevel());
                stmt.setInt(15, island.hasWeatherControl() ? 1 : 0);
                stmt.setString(16, island.getCurrentBiome());
                stmt.setInt(17, island.isPvpEnabled() ? 1 : 0);
                stmt.setInt(18, island.isVisitorsEnabled() ? 1 : 0);
                stmt.setDouble(19, island.getSpawnX());
                stmt.setDouble(20, island.getSpawnY());
                stmt.setDouble(21, island.getSpawnZ());
                stmt.setFloat(22, island.getSpawnYaw());
                stmt.setFloat(23, island.getSpawnPitch());
                
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
            rs.getInt("island_tokens"),
            rs.getInt("size_level"),
            rs.getInt("player_limit_level"),
            rs.getInt("redstone_limit_level"),
            rs.getInt("crop_growth_level"),
            rs.getInt("weather_control") == 1,
            rs.getString("current_biome"),
            rs.getInt("pvp_enabled") == 1,
            rs.getInt("visitors_enabled") == 1,
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
                
                int result = stmt.executeUpdate();
                plugin.getLogger().info("[Island] Saved member " + member.getPlayerUuid() + " to island " + member.getIslandId() + " with role " + member.getRole().name() + " (rows affected: " + result + ")");
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save member: " + e.getMessage());
                e.printStackTrace();
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
    
    // ==================== Invitation Operations ====================
    
    /**
     * Saves an invitation to the database.
     */
    public CompletableFuture<Void> saveInvite(IslandInvite invite) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO island_invites VALUES (?,?,?,?,?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, invite.getIslandId().toString());
                stmt.setString(2, invite.getInvitedPlayer().toString());
                stmt.setString(3, invite.getInvitedBy().toString());
                stmt.setLong(4, invite.getInvitedAt());
                stmt.setLong(5, invite.getExpiresAt());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save invite: " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads pending invites for a player.
     */
    public CompletableFuture<List<IslandInvite>> loadInvitesForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<IslandInvite> invites = new ArrayList<>();
            String sql = "SELECT * FROM island_invites WHERE invited_player = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    IslandInvite invite = new IslandInvite(
                        UUID.fromString(rs.getString("island_id")),
                        UUID.fromString(rs.getString("invited_player")),
                        UUID.fromString(rs.getString("invited_by")),
                        rs.getLong("invited_at"),
                        rs.getLong("expires_at")
                    );
                    
                    // Only return non-expired invites
                    if (!invite.isExpired()) {
                        invites.add(invite);
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load invites: " + e.getMessage());
            }
            
            return invites;
        });
    }
    
    /**
     * Deletes an invitation from the database.
     */
    public CompletableFuture<Void> deleteInvite(UUID islandId, UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM island_invites WHERE island_id = ? AND invited_player = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, islandId.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete invite: " + e.getMessage());
            }
        });
    }
    
    /**
     * Deletes all expired invitations.
     */
    public CompletableFuture<Void> cleanupExpiredInvites() {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM island_invites WHERE expires_at < ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                int deleted = stmt.executeUpdate();
                
                if (deleted > 0) {
                    plugin.getLogger().info("Cleaned up " + deleted + " expired island invites.");
                }
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to cleanup expired invites: " + e.getMessage());
            }
        });
    }
    
    /**
     * Checks if a player has any island membership (as owner or member).
     */
    public CompletableFuture<Boolean> hasIslandMembership(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if player owns an island
            String sqlOwner = "SELECT COUNT(*) FROM player_islands WHERE owner_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlOwner)) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check island ownership: " + e.getMessage());
            }
            
            // Check if player is a member of any island
            String sqlMember = "SELECT COUNT(*) FROM island_members WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlMember)) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check island membership: " + e.getMessage());
            }
            
            return false;
        });
    }
    
    /**
     * Gets the island ID that a player is a member of (owner or member).
     */
    public CompletableFuture<UUID> getPlayerIslandId(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if player owns an island
            String sqlOwner = "SELECT island_id FROM player_islands WHERE owner_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlOwner)) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return UUID.fromString(rs.getString("island_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get player island: " + e.getMessage());
            }
            
            // Check if player is a member of any island
            String sqlMember = "SELECT island_id FROM island_members WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlMember)) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return UUID.fromString(rs.getString("island_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get player island membership: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * Updates island tokens in the database.
     */
    public CompletableFuture<Void> updateIslandTokens(UUID islandId, int newTokenAmount) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_islands SET island_tokens = ? WHERE island_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, newTokenAmount);
                stmt.setString(2, islandId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update island tokens: " + e.getMessage());
            }
        });
    }
}

