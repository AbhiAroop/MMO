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
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

public class ScoreboardManager {
    
    private final Main plugin;
    private final Map<UUID, BukkitTask> playerScoreboardTasks = new HashMap<>();
    private static final int UPDATE_INTERVAL = 20; // Ticks (1 second)
    
    // For animated title
    private final String baseTitle = "MMO Server";
    private final List<String> titleFrames = new ArrayList<>();
    private int currentTitleFrame = 0;
    
    // For empty lines (to create spacing)
    private final String[] spacers = {
        "", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };
    
    private BukkitTask titleAnimationTask;
    
    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        initializeTitleAnimation();
        startTitleAnimation();
    }
    
    private void initializeTitleAnimation() {
        // Generate colorful title animation frames
        String[] colors = {"§c", "§6", "§e", "§a", "§b", "§9", "§d"};
        
        // Static frames
        titleFrames.add("§6§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
        titleFrames.add("§f§l" + baseTitle);
        
        // Moving gradient frames
        for (int i = 0; i < colors.length; i++) {
            StringBuilder title = new StringBuilder();
            for (int j = 0; j < baseTitle.length(); j++) {
                int colorIndex = (i + j) % colors.length;
                title.append(colors[colorIndex]).append("§l").append(baseTitle.charAt(j));
            }
            titleFrames.add(title.toString());
        }
        
        // Pulsing frames
        titleFrames.add("§6§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
        titleFrames.add("§f§l" + baseTitle);
        titleFrames.add("§e§l" + baseTitle);
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
    
    private void updatePlayerScoreboard(Player player) {
        // Get Bukkit's scoreboard manager
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) return;
        
        // Create a new scoreboard
        Scoreboard board = bukkitManager.getNewScoreboard();
        
        // Create an objective
        Objective objective = board.registerNewObjective("mmoStats", "dummy", titleFrames.get(currentTitleFrame));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Add entries to the scoreboard (in reverse order for correct display)
        int scoreValue = 15; // Start from 15 and decrease for each line
        
        // Footer
        Score footer = objective.getScore("§7§ostore.mmo.com");
        footer.setScore(scoreValue--);
        
        // Empty line
        Score emptyLine1 = objective.getScore(spacers[1]);
        emptyLine1.setScore(scoreValue--);
        
        // Future content placeholders - can be expanded later
        Score placeholder1 = objective.getScore("§7Coming Soon...");
        placeholder1.setScore(scoreValue--);
        
        // Empty line 
        Score emptyLine2 = objective.getScore(spacers[2]);
        emptyLine2.setScore(scoreValue--);
        
        // UPDATED: Profile level instead of vanilla level
        Score profileLevel = objective.getScore("§6§lProfile Level: §f" + profile.getProfileLevel());
        profileLevel.setScore(scoreValue--);
        
        // Empty line
        Score emptyLine3 = objective.getScore(spacers[3]);
        emptyLine3.setScore(scoreValue--);
        
        // Currencies
        Score units = objective.getScore("§e§lUnits: §f" + CurrencyFormatter.formatUnits(profile.getUnits()));
        units.setScore(scoreValue--);
        
        Score premium = objective.getScore("§d§lPremium: §f" + CurrencyFormatter.formatPremiumUnits(profile.getPremiumUnits()));
        premium.setScore(scoreValue--);
        
        Score essence = objective.getScore("§b§lEssence: §f" + CurrencyFormatter.formatEssence(profile.getEssence()));
        essence.setScore(scoreValue--);
        
        Score bits = objective.getScore("§a§lBits: §f" + CurrencyFormatter.formatBits(profile.getBits()));
        bits.setScore(scoreValue--);
        
        // Empty line
        Score emptyLine4 = objective.getScore(spacers[4]);
        emptyLine4.setScore(scoreValue--);
        
        // Header
        Score header = objective.getScore("§6§l⚔ §e§lPlayer Stats §6§l⚔");
        header.setScore(scoreValue);
        
        // Apply the scoreboard to the player
        player.setScoreboard(board);
    }
}