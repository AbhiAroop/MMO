package com.server.nametags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

/**
 * Manages custom player nametags using scoreboard teams
 * 
 * Format:
 * Line 1: [{rank}] {playername} ({level})
 * Line 2: {currenthealth}/{maxhealth} ♥
 */
public class NametagManager {
    
    private static NametagManager instance;
    private final Map<UUID, BukkitRunnable> updateTasks;
    private final Scoreboard scoreboard;
    
    private NametagManager() {
        this.updateTasks = new HashMap<>();
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }
    
    public static NametagManager getInstance() {
        if (instance == null) {
            instance = new NametagManager();
        }
        return instance;
    }
    
    /**
     * Initialize nametags for a player
     * Starts periodic updates for all other players
     */
    public void initializePlayer(Player player) {
        // Create or get team for this player
        getOrCreateTeam(player);
        
        // Update this player's nametag
        updateNametag(player);
        
        // Start periodic update task (every 10 ticks = 0.5 seconds)
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    updateTasks.remove(player.getUniqueId());
                    return;
                }
                
                updateNametag(player);
            }
        };
        
        updateTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(Main.getInstance(), 10L, 10L);
    }
    
    /**
     * Remove nametag updates for a player
     */
    public void removePlayer(Player player) {
        BukkitRunnable task = updateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        
        // Remove from team
        Team team = scoreboard.getTeam(getTeamName(player));
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }
    
    /**
     * Get or create a team for a player
     */
    private Team getOrCreateTeam(Player player) {
        String teamName = getTeamName(player);
        Team team = scoreboard.getTeam(teamName);
        
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setCanSeeFriendlyInvisibles(false);
        }
        
        // Add player to their team if not already
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        
        return team;
    }
    
    /**
     * Get unique team name for a player
     */
    private String getTeamName(Player player) {
        // Team names have a 16 character limit
        // Use first 10 chars of UUID to keep it unique
        return "nametag_" + player.getUniqueId().toString().substring(0, 6);
    }
    
    /**
     * Update a player's nametag
     */
    public void updateNametag(Player player) {
        Team team = getOrCreateTeam(player);
        String[] nametagLines = buildNametagLines(player);
        
        // Set prefix (line above name) and suffix (line below name)
        // Note: Prefix + playername + suffix must not exceed 256 characters total
        team.setPrefix(nametagLines[0]);
        team.setSuffix(nametagLines[1]);
    }
    
    /**
     * Build the nametag lines for a player
     * Returns [prefix, suffix] where prefix is above name and suffix is below
     */
    private String[] buildNametagLines(Player player) {
        // Get player profile and stats
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        
        String rank = "§7[§fMember§7]"; // Default rank (placeholder for future rank system)
        int level = 1;
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                PlayerStats stats = profile.getStats();
                
                // Get player level from profile
                level = profile.getProfileLevel();
                
                // Get health values (actual current health from Bukkit, max from stats)
                maxHealth = stats.getHealth();
                currentHealth = Math.min(currentHealth, maxHealth); // Cap current at max
            }
        }
        
        // Format health values (round to 1 decimal place)
        String healthStr = String.format("%.1f", currentHealth);
        String maxHealthStr = String.format("%.1f", maxHealth);
        
        // Build nametag lines
        // Prefix (above name): [Rank] (Level)
        // The player's actual name appears in the middle (automatically)
        // Suffix (below name): CurrentHealth/MaxHealth ♥
        String prefix = rank + " §7(§e" + level + "§7) §f";
        String suffix = "\n§c" + healthStr + "§7/§c" + maxHealthStr + " §c♥";
        
        return new String[]{prefix, suffix};
    }
    
    /**
     * Force update all nametags for all players
     * Useful for rank changes or other global updates
     */
    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNametag(player);
        }
    }
}
