package com.server.profiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;

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

    /**
     * Create a new profile for a player
     */
    public boolean createProfile(Player player, int slot, String name) {
        if (slot < 0 || slot >= 3) return false;

        // Before creating a profile, initialize attributes
        initializePlayerAttributes(player);
        
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
            
            // CRITICAL: Set as active profile BEFORE starting scan to ensure item stats are captured
            activeProfiles.put(player.getUniqueId(), slot);
            
            // CRITICAL FIX: Start scanning immediately with the new profile
            // This ensures equipment stats are processed correctly on first profile creation
            plugin.getStatScanManager().startScanning(player);
            
            // Give a brief moment for stats to apply before forcing a full scan
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Force an immediate scan to update all stats from equipment
                        plugin.getStatScanManager().scanAndUpdatePlayerStats(player);
                        
                        if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                            plugin.debugLog(DebugSystem.PROFILE,"Applied immediate item scan for new profile creation: " + player.getName());
                        }
                    }
                }
            }.runTaskLater(plugin, 5L);
        } else {
            // Don't reset current player state, just initialize with defaults
            newProfile.getStats().resetToDefaults();
            
            // CRITICAL: Set as active profile
            activeProfiles.put(player.getUniqueId(), slot);
        }
        
        playerProfiles[slot] = newProfile;
        
        player.sendMessage(ChatColor.GREEN + "Successfully created profile '" + 
                        ChatColor.GOLD + name + ChatColor.GREEN + "' in slot #" + (slot + 1));
        
        // Kill the player when a new profile is created to ensure fresh start
        final boolean isFirstProfile = !hasAnyOtherProfile(player.getUniqueId(), slot);
        
        if (isFirstProfile) {
            // Kill the player after a short delay to ensure profile is properly set
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setHealth(0); // Kill the player
                        player.sendMessage(ChatColor.YELLOW + "Starting fresh with your new profile!");
                        
                        if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                            plugin.debugLog(DebugSystem.PROFILE,"Killed " + player.getName() + " after creating first profile " + slot);
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

        // Before selecting a profile, initialize attributes
        initializePlayerAttributes(player);

        // CRITICAL: Save current profile's health if one exists
        Integer currentSlot = activeProfiles.get(player.getUniqueId());
        if (currentSlot != null && playerProfiles[currentSlot] != null) {
            // Explicitly save current health before switching
            PlayerProfile currentProfile = playerProfiles[currentSlot];
            currentProfile.getStats().setCurrentHealth(player.getHealth());
            
            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                plugin.debugLog(DebugSystem.PROFILE,"Saving " + player.getName() + "'s health (" + player.getHealth() + 
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
        
        // IMPORTANT FIX: Initialize mining speed attribute properly before loading profile
        if (isFirstProfileAccess) {
            try {
                AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
                if (miningSpeedAttr != null) {
                    // Remove any existing modifiers
                    for (AttributeModifier mod : new HashSet<>(miningSpeedAttr.getModifiers())) {
                        miningSpeedAttr.removeModifier(mod);
                    }
                    
                    // Set base value to our default (0.5)
                    miningSpeedAttr.setBaseValue(0.5);
                    
                    // Extra debug logging
                    if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                        plugin.debugLog(DebugSystem.PROFILE,"Initialized mining speed attribute to 0.5 for first profile access: " + player.getName());
                    }
                }
            } catch (Exception e) {
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Error initializing mining speed attribute: " + e.getMessage());
                }
            }
        }
    
    // Set profile as active before loading to ensure it's recognized
    activeProfiles.put(player.getUniqueId(), slot);
    
    // Load the profile (this will set proper health from profile)
    newProfile.loadProfile(player);
        
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
                        
                        if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                            plugin.debugLog(DebugSystem.PROFILE,"Killed " + player.getName() + " for first access of profile " + slot);
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

    /**
     * Initialize all custom attributes for a player
     */
    private void initializePlayerAttributes(Player player) {
        try {
            // CRITICAL FIX: Add baseline modifiers for all attributes
            // These baseline modifiers ensure the attributes are properly registered 
            // in the game and recognized for equipment bonuses

            // Initialize mining speed attribute
            AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(miningSpeedAttr.getModifiers())) {
                    miningSpeedAttr.removeModifier(mod);
                }
                
                // Set base value to our default (0.5)
                miningSpeedAttr.setBaseValue(0.5);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.mining_speed.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                miningSpeedAttr.addModifier(baselineMod);
            }
            
            // Initialize scale attribute
            AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(scaleAttr.getModifiers())) {
                    scaleAttr.removeModifier(mod);
                }
                
                // Set base value to default (1.0)
                scaleAttr.setBaseValue(1.0);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.size.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                scaleAttr.addModifier(baselineMod);
            }
            
            // Initialize attack range attribute
            AttributeInstance rangeAttr = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (rangeAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(rangeAttr.getModifiers())) {
                    rangeAttr.removeModifier(mod);
                }
                
                // Set base value to default (3.0)
                rangeAttr.setBaseValue(3.0);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.attack_range.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                rangeAttr.addModifier(baselineMod);
            }
            
            // Initialize attack speed attribute
            AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(attackSpeedAttr.getModifiers())) {
                    attackSpeedAttr.removeModifier(mod);
                }
                
                // Set base value to our default (0.5)
                attackSpeedAttr.setBaseValue(0.5);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.attack_speed.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                attackSpeedAttr.addModifier(baselineMod);
            }

            // Initialize build range attribute
            AttributeInstance buildRangeAttr = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
            if (buildRangeAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(buildRangeAttr.getModifiers())) {
                    buildRangeAttr.removeModifier(mod);
                }
                
                // Set base value to default (5.0)
                buildRangeAttr.setBaseValue(5.0);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.build_range.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                buildRangeAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE, "Initialized build range attribute for " + player.getName() + 
                        " to default value (5.0)");
                }
            }

        } catch (Exception e) {
            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                plugin.debugLog(DebugSystem.PROFILE,"Error initializing attributes: " + e.getMessage());
            }
        }
    }
}