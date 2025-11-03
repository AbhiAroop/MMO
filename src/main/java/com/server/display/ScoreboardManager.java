package com.server.display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.server.Main;
import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

public class ScoreboardManager {
    
    private final Main plugin;
    private final IslandManager islandManager;
    private final Map<UUID, BukkitTask> playerScoreboardTasks = new HashMap<>();
    private static final int UPDATE_INTERVAL = 20; // Ticks (1 second)
    
    // For animated title
    private final String baseTitle = "MMO Server";
    private final String islandTitle = "Island";
    private final List<String> titleFrames = new ArrayList<>();
    private final List<String> islandTitleFrames = new ArrayList<>();
    private int currentTitleFrame = 0;
    
    // For empty lines (to create spacing)
    private final String[] spacers = {
        "", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };
    
    private BukkitTask titleAnimationTask;
    
    public ScoreboardManager(Main plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        initializeTitleAnimation();
        startTitleAnimation();
    }
    
    private void initializeTitleAnimation() {
        // Generate colorful title animation frames for main server
        String[] colors = {"§c", "§6", "§e", "§a", "§b", "§9", "§d"};
        
        // Main title frames
        titleFrames.add("§6§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
        titleFrames.add("§f§l" + baseTitle);
        
        for (int i = 0; i < colors.length; i++) {
            StringBuilder title = new StringBuilder();
            for (int j = 0; j < baseTitle.length(); j++) {
                int colorIndex = (i + j) % colors.length;
                title.append(colors[colorIndex]).append("§l").append(baseTitle.charAt(j));
            }
            titleFrames.add(title.toString());
        }
        
        titleFrames.add("§6§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
        titleFrames.add("§f§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
        
        // Island title frames (aqua/green theme)
        String[] islandColors = {"§b", "§3", "§a", "§2"};
        
        islandTitleFrames.add("§b§l" + islandTitle);
        islandTitleFrames.add("§3§l" + islandTitle);
        islandTitleFrames.add("§a§l" + islandTitle);
        
        for (int i = 0; i < islandColors.length; i++) {
            StringBuilder title = new StringBuilder();
            for (int j = 0; j < islandTitle.length(); j++) {
                int colorIndex = (i + j) % islandColors.length;
                title.append(islandColors[colorIndex]).append("§l").append(islandTitle.charAt(j));
            }
            islandTitleFrames.add(title.toString());
        }
        
        islandTitleFrames.add("§b§l" + islandTitle);
        islandTitleFrames.add("§3§l" + islandTitle);
        islandTitleFrames.add("§a§l" + islandTitle);
        islandTitleFrames.add("§2§l" + islandTitle);
    }
    
    private void startTitleAnimation() {
        if (titleAnimationTask != null) {
            titleAnimationTask.cancel();
        }
        
        titleAnimationTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentTitleFrame = (currentTitleFrame + 1) % titleFrames.size();
                // Update all player scoreboards with new title
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 10, 10); // Change frame every half second
    }
    
    public void cleanup() {
        if (titleAnimationTask != null) {
            titleAnimationTask.cancel();
            titleAnimationTask = null;
        }
        
        // Cancel all player scoreboard tasks
        for (BukkitTask task : playerScoreboardTasks.values()) {
            task.cancel();
        }
        playerScoreboardTasks.clear();
    }
    
    public void startTracking(Player player) {
        stopTracking(player); // Stop any existing tracking first
        
        // Create initial scoreboard
        updatePlayerScoreboard(player);
        
        // Schedule regular updates
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    updatePlayerScoreboard(player);
                } else {
                    this.cancel();
                    playerScoreboardTasks.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
        
        // Store the task reference
        playerScoreboardTasks.put(player.getUniqueId(), task);
    }
    
    public void stopTracking(Player player) {
        BukkitTask existingTask = playerScoreboardTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
    }
    
    private String formatLargeNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
    
    private void updatePlayerScoreboard(Player player) {
        // Get Bukkit's scoreboard manager
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Check if player is on an island
        UUID currentIslandId = islandManager.getCurrentIsland(player.getUniqueId());
        if (currentIslandId != null) {
            // Display island scoreboard
            updateIslandScoreboard(player, profile, currentIslandId, bukkitManager);
        } else {
            // Display default scoreboard
            updateDefaultScoreboard(player, profile, bukkitManager);
        }
    }
    
    private void updateDefaultScoreboard(Player player, PlayerProfile profile, org.bukkit.scoreboard.ScoreboardManager bukkitManager) {
        // Create a new scoreboard
        Scoreboard board = bukkitManager.getNewScoreboard();
        
        // Create an objective
        Objective objective = board.registerNewObjective("mmoStats", "dummy", titleFrames.get(currentTitleFrame));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Add entries to the scoreboard (in reverse order for correct display)
        int scoreValue = 15;
        
        // Footer
        objective.getScore("§7§ostore.mmo.com").setScore(scoreValue--);
        objective.getScore(spacers[1]).setScore(scoreValue--);
        
        // Currencies section
        objective.getScore("§6§l┃ §e§lCurrencies §6§l┃").setScore(scoreValue--);
        objective.getScore(spacers[2]).setScore(scoreValue--);
        
        objective.getScore("  §e⛃ §fUnits: §e" + CurrencyFormatter.formatUnits(profile.getUnits())).setScore(scoreValue--);
        objective.getScore("  §d◆ §fPremium: §d" + CurrencyFormatter.formatPremiumUnits(profile.getPremiumUnits())).setScore(scoreValue--);
        objective.getScore("  §b✦ §fEssence: §b" + CurrencyFormatter.formatEssence(profile.getEssence())).setScore(scoreValue--);
        objective.getScore("  §a❖ §fBits: §a" + CurrencyFormatter.formatBits(profile.getBits())).setScore(scoreValue--);
        
        objective.getScore(spacers[3]).setScore(scoreValue--);
        
        // Player info section
        objective.getScore("§6§l┃ §e§lPlayer Info §6§l┃").setScore(scoreValue--);
        objective.getScore(spacers[4]).setScore(scoreValue--);
        
        objective.getScore("  §6★ §fLevel: §e" + profile.getProfileLevel()).setScore(scoreValue--);
        objective.getScore("  §c❤ §fHealth: §c" + String.format("%.1f", player.getHealth()) + " §7/ §c" + String.format("%.1f", player.getMaxHealth())).setScore(scoreValue--);
        
        objective.getScore(spacers[5]).setScore(scoreValue--);
        
        // Header
        objective.getScore("§6§l━━━━━━━━━━━━━━━").setScore(scoreValue);
        
        // Apply the scoreboard to the player
        player.setScoreboard(board);
    }
    
    private void updateIslandScoreboard(Player player, PlayerProfile profile, UUID islandId, org.bukkit.scoreboard.ScoreboardManager bukkitManager) {
        // Create a new scoreboard
        Scoreboard board = bukkitManager.getNewScoreboard();
        
        // Create an objective with island title (use modulo to ensure index is within bounds)
        int islandFrameIndex = currentTitleFrame % islandTitleFrames.size();
        Objective objective = board.registerNewObjective("islandStats", "dummy", islandTitleFrames.get(islandFrameIndex));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Get island data
        PlayerIsland island = islandManager.getCache().getIsland(islandId);
        
        int scoreValue = 15;
        
        // Footer
        objective.getScore("§7§ostore.mmo.com").setScore(scoreValue--);
        objective.getScore(spacers[1]).setScore(scoreValue--);
        
        // Player info (condensed)
        objective.getScore("§b§l┃ §3§lPlayer §b§l┃").setScore(scoreValue--);
        objective.getScore(spacers[2]).setScore(scoreValue--);
        objective.getScore("  §6★ §fLevel: §e" + profile.getProfileLevel()).setScore(scoreValue--);
        objective.getScore("  §c❤ §fHP: §c" + String.format("%.0f", player.getHealth()) + "§7/§c" + String.format("%.0f", player.getMaxHealth())).setScore(scoreValue--);
        
        objective.getScore(spacers[3]).setScore(scoreValue--);
        
        if (island != null) {
            // Island info section
            objective.getScore("§b§l┃ §3§lIsland Info §b§l┃").setScore(scoreValue--);
            objective.getScore(spacers[4]).setScore(scoreValue--);
            
            objective.getScore("  §f" + island.getIslandName()).setScore(scoreValue--);
            objective.getScore("  §7Type: §f" + island.getIslandType().toString()).setScore(scoreValue--);
            objective.getScore("  §3⬆ §fLevel: §b" + island.getIslandLevel()).setScore(scoreValue--);
            objective.getScore("  §e⛃ §fValue: §6" + formatLargeNumber(island.getIslandValue())).setScore(scoreValue--);
            objective.getScore("  §a◈ §fTokens: §2" + island.getIslandTokens()).setScore(scoreValue--);
        } else {
            // Fallback if island data not available
            objective.getScore("§b§l┃ §3§lIsland §b§l┃").setScore(scoreValue--);
            objective.getScore(spacers[4]).setScore(scoreValue--);
            objective.getScore("  §7Loading...").setScore(scoreValue--);
        }
        
        objective.getScore(spacers[5]).setScore(scoreValue--);
        
        // Header
        objective.getScore("§b§l━━━━━━━━━━━━━━━").setScore(scoreValue);
        
        // Apply the scoreboard to the player
        player.setScoreboard(board);
    }
}