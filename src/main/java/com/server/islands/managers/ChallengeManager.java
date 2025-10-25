package com.server.islands.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.server.islands.data.ChallengeProgress;
import com.server.islands.data.IslandChallenge;
import com.server.islands.data.IslandChallenge.ChallengeCategory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Manages island challenges and their progression.
 */
public class ChallengeManager {
    
    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private Connection connection;
    private final File databaseFile;
    
    // Registry of all challenges
    private final Map<String, IslandChallenge> challengeRegistry;
    
    // Cached progress data (islandId -> challengeId -> progress)
    private final Map<UUID, Map<String, ChallengeProgress>> islandProgressCache;
    
    // Cached progress data (playerId -> challengeId -> progress) for player-specific challenges
    private final Map<UUID, Map<String, ChallengeProgress>> playerProgressCache;
    
    public ChallengeManager(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.databaseFile = new File(plugin.getDataFolder(), "challenges.db");
        this.challengeRegistry = new HashMap<>();
        this.islandProgressCache = new HashMap<>();
        this.playerProgressCache = new HashMap<>();
    }
    
    /**
     * Initializes the challenge system and creates database tables.
     * This is now synchronous to ensure proper database setup before challenge registration.
     */
    public void initialize() {
        try {
            // Create data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Close existing connection if it exists (important for reloads)
            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("Closing existing challenge database connection...");
                connection.close();
            }
            
            // Connect to database
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            // Enable WAL mode for better concurrency
            connection.createStatement().execute("PRAGMA journal_mode=WAL");
            connection.createStatement().execute("PRAGMA synchronous=NORMAL");
            // Reduce busy timeout
            connection.createStatement().execute("PRAGMA busy_timeout=5000");
            
            // Create challenges table (stores challenge definitions)
            String createChallengesTable = "CREATE TABLE IF NOT EXISTS island_challenges (" +
                "challenge_id VARCHAR(100) PRIMARY KEY," +
                "name VARCHAR(200) NOT NULL," +
                "description TEXT," +
                "category VARCHAR(50) NOT NULL," +
                "difficulty VARCHAR(50) NOT NULL," +
                "token_reward INT NOT NULL," +
                "challenge_type VARCHAR(50) NOT NULL," +
                "target_key VARCHAR(100)," +
                "target_amount INT NOT NULL," +
                "is_island_wide BOOLEAN NOT NULL," +
                "prerequisites TEXT" +
                ")";
            
            // Create progress table (tracks completion per island/player)
            String createProgressTable = "CREATE TABLE IF NOT EXISTS island_challenge_progress (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "challenge_id VARCHAR(100) NOT NULL," +
                "island_id VARCHAR(36) NOT NULL," +
                "player_id VARCHAR(36)," + // null if island-wide
                "current_progress INT NOT NULL DEFAULT 0," +
                "completed BOOLEAN NOT NULL DEFAULT 0," +
                "completed_at BIGINT NOT NULL DEFAULT 0," +
                "UNIQUE (challenge_id, island_id, player_id)" +
                ")";
            
            connection.createStatement().execute(createChallengesTable);
            connection.createStatement().execute(createProgressTable);
            
            plugin.getLogger().info("Challenge database initialized successfully!");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize challenge tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Closes database connection.
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Challenge database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close challenge database: " + e.getMessage());
            }
        });
    }
    
    /**
     * Registers a challenge in the system.
     * Database persistence is handled synchronously to avoid lock conflicts.
     */
    public void registerChallenge(IslandChallenge challenge) {
        challengeRegistry.put(challenge.getId(), challenge);
        
        // Ensure connection is available before trying to use it
        if (connection == null) {
            plugin.getLogger().warning("Cannot register challenge " + challenge.getId() + " - database not initialized yet");
            return;
        }
        
        // Persist to database synchronously to avoid SQLite lock conflicts
        synchronized (this) {
            try {
                String sql = "INSERT OR REPLACE INTO island_challenges (challenge_id, name, description, category, " +
                    "difficulty, token_reward, challenge_type, target_key, target_amount, is_island_wide, prerequisites) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, challenge.getId());
                    stmt.setString(2, challenge.getName());
                    stmt.setString(3, challenge.getDescription());
                    stmt.setString(4, challenge.getCategory().name());
                    stmt.setString(5, challenge.getDifficulty().name());
                    stmt.setInt(6, challenge.getTokenReward());
                    stmt.setString(7, challenge.getType().name());
                    stmt.setString(8, challenge.getTargetKey());
                    stmt.setInt(9, challenge.getTargetAmount());
                    stmt.setBoolean(10, challenge.isIslandWide());
                    stmt.setString(11, String.join(",", challenge.getPrerequisites()));
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to register challenge " + challenge.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets a challenge by ID.
     */
    public IslandChallenge getChallenge(String challengeId) {
        return challengeRegistry.get(challengeId);
    }
    
    /**
     * Gets all challenges for a specific category.
     */
    public List<IslandChallenge> getChallengesByCategory(ChallengeCategory category) {
        return challengeRegistry.values().stream()
            .filter(c -> c.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all registered challenges.
     */
    public Collection<IslandChallenge> getAllChallenges() {
        return new ArrayList<>(challengeRegistry.values());
    }
    
    /**
     * Gets challenge progress for an island (island-wide challenge).
     */
    public CompletableFuture<ChallengeProgress> getIslandProgress(UUID islandId, String challengeId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            if (islandProgressCache.containsKey(islandId)) {
                ChallengeProgress cached = islandProgressCache.get(islandId).get(challengeId);
                if (cached != null) return cached;
            }
            
            // Load from database
            try {
                String sql = "SELECT * FROM island_challenge_progress WHERE challenge_id=? AND island_id=? AND player_id IS NULL";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, challengeId);
                stmt.setString(2, islandId.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    ChallengeProgress progress = new ChallengeProgress(
                        rs.getString("challenge_id"),
                        UUID.fromString(rs.getString("island_id")),
                        null,
                        rs.getInt("current_progress"),
                        rs.getBoolean("completed"),
                        rs.getLong("completed_at")
                    );
                    
                    // Cache it
                    islandProgressCache.computeIfAbsent(islandId, k -> new HashMap<>()).put(challengeId, progress);
                    return progress;
                } else {
                    // Create new progress entry
                    ChallengeProgress progress = new ChallengeProgress(challengeId, islandId, null);
                    islandProgressCache.computeIfAbsent(islandId, k -> new HashMap<>()).put(challengeId, progress);
                    return progress;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get island progress: " + e.getMessage());
                return new ChallengeProgress(challengeId, islandId, null);
            }
        });
    }
    
    /**
     * Gets challenge progress for a player (player-specific challenge).
     */
    public CompletableFuture<ChallengeProgress> getPlayerProgress(UUID playerId, UUID islandId, String challengeId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            if (playerProgressCache.containsKey(playerId)) {
                ChallengeProgress cached = playerProgressCache.get(playerId).get(challengeId);
                if (cached != null) return cached;
            }
            
            // Load from database
            try {
                String sql = "SELECT * FROM island_challenge_progress WHERE challenge_id=? AND island_id=? AND player_id=?";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, challengeId);
                stmt.setString(2, islandId.toString());
                stmt.setString(3, playerId.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    ChallengeProgress progress = new ChallengeProgress(
                        rs.getString("challenge_id"),
                        UUID.fromString(rs.getString("island_id")),
                        UUID.fromString(rs.getString("player_id")),
                        rs.getInt("current_progress"),
                        rs.getBoolean("completed"),
                        rs.getLong("completed_at")
                    );
                    
                    // Cache it
                    playerProgressCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(challengeId, progress);
                    return progress;
                } else {
                    // Create new progress entry
                    ChallengeProgress progress = new ChallengeProgress(challengeId, islandId, playerId);
                    playerProgressCache.computeIfAbsent(playerId, k -> new HashMap<>()).put(challengeId, progress);
                    return progress;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get player progress: " + e.getMessage());
                return new ChallengeProgress(challengeId, islandId, playerId);
            }
        });
    }
    
    /**
     * Increments progress for a challenge.
     */
    public CompletableFuture<Void> incrementProgress(UUID playerId, UUID islandId, String challengeId, int amount) {
        IslandChallenge challenge = getChallenge(challengeId);
        if (challenge == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        if (challenge.isIslandWide()) {
            return getIslandProgress(islandId, challengeId).thenAccept(progress -> {
                if (progress.isCompleted()) return; // Already completed
                
                progress.incrementProgress(amount);
                
                // Check if completed
                if (progress.isReadyToComplete(challenge.getTargetAmount())) {
                    completeChallenge(islandId, null, challenge, progress);
                } else {
                    saveProgress(progress);
                }
            });
        } else {
            return getPlayerProgress(playerId, islandId, challengeId).thenAccept(progress -> {
                if (progress.isCompleted()) return; // Already completed
                
                progress.incrementProgress(amount);
                
                // Check if completed
                if (progress.isReadyToComplete(challenge.getTargetAmount())) {
                    completeChallenge(islandId, playerId, challenge, progress);
                } else {
                    saveProgress(progress);
                }
            });
        }
    }
    
    /**
     * Completes a challenge and awards tokens.
     */
    private void completeChallenge(UUID islandId, UUID playerId, IslandChallenge challenge, ChallengeProgress progress) {
        progress.setCompleted(true);
        saveProgress(progress);
        
        // Award tokens to island
        islandManager.loadIsland(islandId).thenAccept(island -> {
            if (island != null) {
                island.addIslandTokens(challenge.getTokenReward());
                // Update in database
                islandManager.updateIslandTokens(islandId, island.getIslandTokens());
                
                // Notify players
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
                    
                    Component message = Component.empty()
                        .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("✦ CHALLENGE COMPLETED!", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text(challenge.getName(), NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("Reward: ", NamedTextColor.GRAY))
                        .append(Component.text("+" + challenge.getTokenReward() + " Island Tokens", NamedTextColor.AQUA))
                        .append(Component.newline())
                        .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                    
                    if (challenge.isIslandWide()) {
                        // Notify all island members
                        islandManager.getMembers(islandId).thenAccept(members -> {
                            members.forEach(member -> {
                                Player p = Bukkit.getPlayer(member.getPlayerUuid());
                                if (p != null && p.isOnline()) {
                                    p.sendMessage(message);
                                }
                            });
                        });
                    } else {
                        // Notify only the player who completed it
                        if (player != null && player.isOnline()) {
                            player.sendMessage(message);
                        }
                    }
                });
            }
        });
    }
    
    /**
     * Saves challenge progress to database.
     */
    private void saveProgress(ChallengeProgress progress) {
        CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT OR REPLACE INTO island_challenge_progress (challenge_id, island_id, player_id, current_progress, completed, completed_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
                
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, progress.getChallengeId());
                stmt.setString(2, progress.getIslandId().toString());
                stmt.setString(3, progress.getPlayerId() != null ? progress.getPlayerId().toString() : null);
                stmt.setInt(4, progress.getCurrentProgress());
                stmt.setBoolean(5, progress.isCompleted());
                stmt.setLong(6, progress.getCompletedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save challenge progress: " + e.getMessage());
            }
        });
    }
    
    /**
     * Checks if all prerequisites for a challenge are completed.
     */
    public CompletableFuture<Boolean> arePrerequisitesCompleted(UUID islandId, UUID playerId, IslandChallenge challenge) {
        if (!challenge.hasPrerequisites()) {
            return CompletableFuture.completedFuture(true);
        }
        
        List<CompletableFuture<Boolean>> checks = new ArrayList<>();
        
        for (String prerequisiteId : challenge.getPrerequisites()) {
            IslandChallenge prereq = getChallenge(prerequisiteId);
            if (prereq == null) continue;
            
            CompletableFuture<Boolean> check;
            if (prereq.isIslandWide()) {
                check = getIslandProgress(islandId, prerequisiteId)
                    .thenApply(ChallengeProgress::isCompleted);
            } else {
                check = getPlayerProgress(playerId, islandId, prerequisiteId)
                    .thenApply(ChallengeProgress::isCompleted);
            }
            checks.add(check);
        }
        
        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
            .thenApply(v -> checks.stream().allMatch(CompletableFuture::join));
    }
    
    /**
     * Gets all completed challenges for an island.
     */
    public CompletableFuture<List<String>> getCompletedChallenges(UUID islandId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> completed = new ArrayList<>();
            try {
                String sql;
                PreparedStatement stmt;
                
                if (playerId == null) {
                    sql = "SELECT challenge_id FROM island_challenge_progress WHERE island_id=? AND player_id IS NULL AND completed=1";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, islandId.toString());
                } else {
                    sql = "SELECT challenge_id FROM island_challenge_progress WHERE island_id=? AND (player_id=? OR player_id IS NULL) AND completed=1";
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, islandId.toString());
                    stmt.setString(2, playerId.toString());
                }
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    completed.add(rs.getString("challenge_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get completed challenges: " + e.getMessage());
            }
            return completed;
        });
    }
    
    /**
     * Force completes a challenge for an island (admin command).
     * Sets progress to target amount and marks as completed.
     */
    public CompletableFuture<Boolean> forceCompleteChallenge(UUID islandId, String challengeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IslandChallenge challenge = challengeRegistry.get(challengeId);
                if (challenge == null) {
                    return false;
                }
                
                // Check if already completed
                String checkSql = "SELECT completed FROM island_challenge_progress WHERE island_id=? AND challenge_id=?";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setString(1, islandId.toString());
                checkStmt.setString(2, challengeId);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next() && rs.getBoolean("completed")) {
                    return false; // Already completed
                }
                
                // Insert or update progress
                String sql = "INSERT OR REPLACE INTO island_challenge_progress (island_id, player_id, challenge_id, current_progress, completed, completed_at) VALUES (?, NULL, ?, ?, 1, ?)";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, islandId.toString());
                stmt.setString(2, challengeId);
                stmt.setInt(3, challenge.getTargetAmount());
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
                
                // Award tokens to island
                islandManager.loadIsland(islandId).thenAccept(island -> {
                    if (island != null) {
                        int tokenReward = challenge.getTokenReward();
                        island.addIslandTokens(tokenReward);
                        islandManager.getDataManager().saveIsland(island);
                        
                        plugin.getLogger().info("Admin force completed challenge: " + challengeId + 
                            " for island " + islandId + " (+" + tokenReward + " tokens)");
                    }
                });
                
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to force complete challenge: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Get all completed challenges for an island (including both island-wide and any player-specific)
     */
    public CompletableFuture<java.util.Set<String>> getCompletedChallenges(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.Set<String> completed = new java.util.HashSet<>();
            try {
                String sql = "SELECT DISTINCT challenge_id FROM island_challenge_progress WHERE island_id=? AND completed=1";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, islandId.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    completed.add(rs.getString("challenge_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get completed challenges: " + e.getMessage());
            }
            return completed;
        });
    }
    
    /**
     * Get challenge progress for a specific challenge
     */
    public CompletableFuture<Integer> getChallengeProgress(UUID islandId, UUID playerId, String challengeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IslandChallenge challenge = challengeRegistry.get(challengeId);
                if (challenge == null) {
                    return 0;
                }
                
                String sql;
                if (challenge.isIslandWide()) {
                    sql = "SELECT current_progress FROM island_challenge_progress WHERE island_id=? AND challenge_id=? AND player_id IS NULL";
                } else {
                    sql = "SELECT current_progress FROM island_challenge_progress WHERE island_id=? AND challenge_id=? AND player_id=?";
                }
                
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, islandId.toString());
                stmt.setString(2, challengeId);
                
                if (!challenge.isIslandWide()) {
                    stmt.setString(3, playerId.toString());
                }
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("current_progress");
                }
                
                return 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get challenge progress: " + e.getMessage());
                return 0;
            }
        });
    }
    
    /**
     * Get a challenge tree for a category
     */
    public com.server.islands.data.ChallengeCategoryTree getChallengeTree(ChallengeCategory category) {
        com.server.islands.data.ChallengeCategoryTree tree = new com.server.islands.data.ChallengeCategoryTree(category);
        
        // Add all challenges for this category to the tree
        for (IslandChallenge challenge : challengeRegistry.values()) {
            if (challenge.getCategory() == category) {
                tree.addChallenge(challenge);
            }
        }
        
        return tree;
    }
    
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
