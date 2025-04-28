package com.server.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
        
        // Kill the player when a new profile is created to ensure fresh start
        final boolean isFirstProfile = !hasAnyOtherProfile(player.getUniqueId(), slot);
        
        if (isFirstProfile) {
            // Set as active profile
            activeProfiles.put(player.getUniqueId(), slot);
            
            // Kill the player after a short delay to ensure profile is properly set
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setHealth(0); // Kill the player
                        player.sendMessage(ChatColor.YELLOW + "Starting fresh with your new profile!");
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Killed " + player.getName() + " after creating first profile " + slot);
                        }
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
        
        return true;
    }

    /**
     * Check if a player has any other profiles besides the specified slot
     */
    private boolean hasAnyOtherProfile(UUID playerUUID, int excludedSlot) {
        PlayerProfile[] profiles = getProfiles(playerUUID);
        for (int i = 0; i < profiles.length; i++) {
            if (i != excludedSlot && profiles[i] != null) {
                return true;
            }
        }
        return false;
    }

    public boolean selectProfile(Player player, int slot) {
        PlayerProfile[] playerProfiles = getProfiles(player.getUniqueId());
        if (playerProfiles[slot] == null) {
            player.sendMessage(ChatColor.RED + "No profile exists in slot #" + (slot + 1));
            return false;
        }

        // CRITICAL: Save current profile's health if one exists
        Integer currentSlot = activeProfiles.get(player.getUniqueId());
        if (currentSlot != null && playerProfiles[currentSlot] != null) {
            // Explicitly save current health before switching
            PlayerProfile currentProfile = playerProfiles[currentSlot];
            currentProfile.getStats().setCurrentHealth(player.getHealth());
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Saving " + player.getName() + "'s health (" + player.getHealth() + 
                            ") before switching from profile " + currentSlot + " to " + slot);
            }
            
            // Save full profile state
            playerProfiles[currentSlot].saveProfile(player);
        }
            
        // Load the new profile (which will set correct health value)
        PlayerProfile newProfile = playerProfiles[slot];
        
        // Check if this is the player's first-ever profile selection
        boolean isFirstEverSelection = !playerHasPlayedBefore(player.getUniqueId());
        
        // If not first ever, check if this is first time accessing this specific profile
        boolean isFirstProfileAccess = !isFirstEverSelection && newProfile.getLastPlayed() == newProfile.getCreated();
        
        // Set profile as active before loading to ensure it's recognized
        activeProfiles.put(player.getUniqueId(), slot);
        
        // Load the profile (this will set proper health from profile)
        newProfile.loadProfile(player);
        
        // If this is the first-ever profile or first time accessing this profile, kill the player
        // This ensures they spawn at the proper location with default stats
        if (isFirstEverSelection || isFirstProfileAccess) {
            // Kill the player with a delay to ensure the profile is fully loaded
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setHealth(0); // Kill the player
                        player.sendMessage(ChatColor.YELLOW + "Starting fresh with your new profile!");
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Killed " + player.getName() + " for first access of profile " + slot);
                        }
                    }
                }
            }.runTaskLater(plugin, 5L); // Short delay to ensure profile is fully loaded
        }
        
        // Update the scoreboard for the player
        if (plugin != null && plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().startTracking(player);
        }

        player.sendMessage(ChatColor.GREEN + "Selected profile '" + 
                        ChatColor.GOLD + newProfile.getName() + ChatColor.GREEN + "' from slot #" + (slot + 1));
        return true;
    }

    /**
     * Check if a player has ever played before (accessed any profile)
     */
    private boolean playerHasPlayedBefore(UUID playerUUID) {
        PlayerProfile[] profiles = getProfiles(playerUUID);
        for (PlayerProfile profile : profiles) {
            if (profile != null && profile.getLastPlayed() > profile.getCreated()) {
                return true;
            }
        }
        return false;
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