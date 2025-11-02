package com.server.nametags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

/**
 * Manages custom player nametags using NMS packets
 * Sends ClientboundSetPlayerTeamPacket to display custom nametags above player heads
 * 
 * Format:
 * [{rank}] {playername} ({level}) | HP: {currenthealth}/{maxhealth} ♥
 */
public class NametagManager {
    
    private static NametagManager instance;
    private final Map<UUID, BukkitRunnable> updateTasks;
    private final Map<UUID, Boolean> initializedPlayers;
    
    private NametagManager() {
        this.updateTasks = new HashMap<>();
        this.initializedPlayers = new HashMap<>();
        
        // Check if packet handler initialized successfully
        if (!PacketNametagHandler.isInitialized()) {
            Bukkit.getLogger().severe("[NametagManager] Failed to initialize packet handler!");
            Bukkit.getLogger().severe("[NametagManager] Error: " + PacketNametagHandler.getError());
            Bukkit.getLogger().severe("[NametagManager] Custom nametags will NOT work!");
        } else {
            Bukkit.getLogger().info("[NametagManager] Packet handler initialized successfully!");
        }
    }
    
    public static NametagManager getInstance() {
        if (instance == null) {
            instance = new NametagManager();
        }
        return instance;
    }
    
    /**
     * Initialize nametags for a player
     * Creates a team via packets and sets up periodic updates
     */
    public void initializePlayer(Player player) {
        if (!PacketNametagHandler.isInitialized()) {
            return;
        }
        
        // Get team name
        String teamName = getTeamName(player);
        
        // Get initial nametag data [displayName, prefix, suffix]
        String[] nametagData = buildNametagData(player);
        
        // Create team via packet (mode 0)
        PacketNametagHandler.createTeam(player, teamName, nametagData[0], nametagData[1], nametagData[2]);
        initializedPlayers.put(player.getUniqueId(), true);
        
        // Start periodic update task (every 10 ticks = 0.5 seconds)
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    updateTasks.remove(player.getUniqueId());
                    return;
                }
                
                updatePlayerNametag(player);
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
        
        initializedPlayers.remove(player.getUniqueId());
        
        // Remove team via packet (mode 1)
        if (PacketNametagHandler.isInitialized()) {
            String teamName = getTeamName(player);
            PacketNametagHandler.removeTeam(teamName);
        }
    }
    
    /**
     * Update a player's nametag with current stats
     */
    private void updatePlayerNametag(Player player) {
        if (!PacketNametagHandler.isInitialized()) {
            return;
        }
        
        // Get team name
        String teamName = getTeamName(player);
        
        // Get current nametag data [displayName, prefix, suffix]
        String[] nametagData = buildNametagData(player);
        
        // Update team via packet (mode 2)
        PacketNametagHandler.updateTeam(player, teamName, nametagData[0], nametagData[1], nametagData[2]);
    }
    
    /**
     * Get team name for a player
     */
    private String getTeamName(Player player) {
        return "nt_" + player.getUniqueId().toString().substring(0, 10);
    }
    
    /**
     * Build the nametag data for a player
     * Returns [displayName, prefix, suffix]
     * DisplayName contains the full multi-line nametag
     */
    private String[] buildNametagData(Player player) {
        // Get player profile and stats
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        
        String rank = "§7[§fMember§7]"; // Default rank (placeholder for future rank system)
        int level = 1;
        double currentHealth = player.getHealth();
        
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                PlayerStats stats = profile.getStats();
                
                // Get player level from profile
                level = profile.getProfileLevel();
                
                // Get health value (actual current health from Bukkit, capped at max)
                double maxHealth = stats.getHealth();
                currentHealth = Math.min(currentHealth, maxHealth); // Cap current at max
            }
        }
        
        // Format health value (round to 1 decimal place)
        String healthStr = String.format("%.1f", currentHealth);
        
        // Build single-line nametag: [Member] PlayerName (Level) CurrentHealth♥
        // Use prefix for rank, player name in middle, suffix for level and health
        String prefix = rank + " ";
        String suffix = " §7(§e" + level + "§7) §c" + healthStr + "♥";
        
        // Player name will appear between prefix and suffix
        return new String[] { player.getName(), prefix, suffix };
    }
    
    /**
     * Force update all nametags for all players
     * Useful for rank changes or other global updates
     */
    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerNametag(player);
        }
    }
    
    /**
     * Update a specific player's nametag immediately (called from listeners)
     */
    public void updateNametag(Player player) {
        updatePlayerNametag(player);
    }
}
