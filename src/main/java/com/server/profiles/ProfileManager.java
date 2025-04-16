package com.server.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.Main;

public class ProfileManager {
    private static ProfileManager instance;
    private final Map<UUID, PlayerProfile[]> profiles;
    private final Map<UUID, Integer> activeProfiles; // Track active profile for each player
    private final Main plugin;

    private ProfileManager() {
        profiles = new HashMap<>();
        activeProfiles = new HashMap<>();
        this.plugin = Main.getInstance();
    }

    public static ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    public PlayerProfile[] getProfiles(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> new PlayerProfile[3]);
    }

    public boolean createProfile(Player player, int slot, String name) {
        if (slot < 0 || slot >= 3) return false;
        
        PlayerProfile[] playerProfiles = getProfiles(player.getUniqueId());
        if (playerProfiles[slot] != null) {
            player.sendMessage(ChatColor.RED + "A profile already exists in slot #" + (slot + 1));
            return false;
        }

        // Create new profile with default stats
        PlayerProfile newProfile = new PlayerProfile(player.getUniqueId(), slot, name);
        
        // Only reset player state if they don't have an active profile
        Integer currentSlot = activeProfiles.get(player.getUniqueId());
        if (currentSlot == null) {
            // Reset player's state before saving new profile
            player.getInventory().clear();
            player.setHealth((double) player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExhaustion(0f);
            player.setLevel(0);
            player.setExp(0f);
            player.setFlying(false);
            player.setAllowFlight(false);
            
            // Save the reset state to new profile
            newProfile.saveProfile(player);
            activeProfiles.put(player.getUniqueId(), slot);
        } else {
            // Don't reset current player state, just initialize with defaults
            newProfile.getStats().resetToDefaults();
        }
        
        playerProfiles[slot] = newProfile;
        
        player.sendMessage(ChatColor.GREEN + "Successfully created profile '" + 
                         ChatColor.GOLD + name + ChatColor.GREEN + "' in slot #" + (slot + 1));
        return true;
    }

    public boolean selectProfile(Player player, int slot) {
        PlayerProfile[] playerProfiles = getProfiles(player.getUniqueId());
        if (playerProfiles[slot] == null) {
            player.sendMessage(ChatColor.RED + "No profile exists in slot #" + (slot + 1));
            return false;
        }

        // Save current profile if one exists
        Integer currentSlot = activeProfiles.get(player.getUniqueId());
        if (currentSlot != null && playerProfiles[currentSlot] != null) {
            playerProfiles[currentSlot].saveProfile(player);
        }

        PlayerProfile newProfile = playerProfiles[slot];
        
        // Check if this is the first time loading this profile
        if (newProfile.getLastPlayed() == newProfile.getCreated()) {
            // Reset player state for fresh profile
            player.getInventory().clear();
            player.setHealth((double) player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setExhaustion(0f);
            player.setLevel(0);
            player.setExp(0f);
            player.setFlying(false);
            player.setAllowFlight(false);
            
            // Save initial state
            newProfile.saveProfile(player);
        }

        // Load profile state
        newProfile.loadProfile(player);
        activeProfiles.put(player.getUniqueId(), slot);

        // Update the scoreboard for the player
        if (plugin != null && plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(player);
        }

        player.sendMessage(ChatColor.GREEN + "Selected profile '" + 
                         ChatColor.GOLD + newProfile.getName() + ChatColor.GREEN + "' from slot #" + (slot + 1));
        return true;
    }

    public boolean deleteProfile(Player player, int slot) {
        if (slot < 0 || slot >= 3) return false;
        
        PlayerProfile[] playerProfiles = getProfiles(player.getUniqueId());
        if (playerProfiles[slot] == null) {
            player.sendMessage(ChatColor.RED + "No profile exists in slot #" + (slot + 1));
            return false;
        }

        // Count existing profiles
        int profileCount = 0;
        for (PlayerProfile profile : playerProfiles) {
            if (profile != null) profileCount++;
        }

        // Prevent deleting last profile
        if (profileCount <= 1) {
            player.sendMessage(ChatColor.RED + "You cannot delete your last profile!");
            return false;
        }

        // If deleting active profile, clear active profile
        if (activeProfiles.get(player.getUniqueId()) == slot) {
            activeProfiles.remove(player.getUniqueId());
            player.getInventory().clear();
        }

        String profileName = playerProfiles[slot].getName();
        playerProfiles[slot] = null;
        player.sendMessage(ChatColor.YELLOW + "Deleted profile '" + 
                         ChatColor.GOLD + profileName + ChatColor.YELLOW + "' from slot #" + (slot + 1));
        return true;
    }

    public Integer getActiveProfile(UUID playerUUID) {
        return activeProfiles.get(playerUUID);
    }
}