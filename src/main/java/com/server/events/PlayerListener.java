package com.server.events;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.cosmetics.CosmeticManager;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;

/**
 * Handles player-related events like joining, quitting, and profile selection
 */
public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    // In PlayerListener.java
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ProfileManager pm = ProfileManager.getInstance();
        PlayerProfile[] profiles = pm.getProfiles(player.getUniqueId());

        initializeAttributes(player);

        // Check if player has no profiles (first time joining)
        boolean hasProfiles = false;
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                hasProfiles = true;
                break;
            }
        }

        if (!hasProfiles) {
            // First time player logic remains the same
            resetPlayerState(player);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    ProfileGUI.openProfileSelector(player);
                }
            }.runTaskLater(plugin, 1L);
        } else {
            // For returning players, grab health value BEFORE starting stat scanning
            Integer activeSlot = pm.getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                PlayerProfile activeProfile = profiles[activeSlot];
                if (activeProfile != null) {
                    // Store the player's saved health from the profile
                    final double storedHealth = activeProfile.getStats().getCurrentHealth();
                    final double storedMiningSpeed = activeProfile.getStats().getMiningSpeed();
                    final double storedSize = activeProfile.getStats().getSize();
                    final double storedAttackRange = activeProfile.getStats().getAttackRange();

                    if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                        plugin.debugLog(DebugSystem.PROFILE,"JOIN: " + player.getName() + "'s stored health: " + storedHealth + 
                                    ", current: " + player.getHealth() + 
                                    ", stored mining speed: " + storedMiningSpeed +
                                    ", stored size: " + storedSize +
                                    ", stored attack range: " + storedAttackRange);
                    }

                    // Apply temporary attribute fixes BEFORE stat scanning
                    try {
                        // Apply temporary health increase to hold our value
                        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttr != null && Math.abs(player.getHealth() - 20.0) < 0.1 && storedHealth > 20.0) {
                            // Remove any existing modifiers first
                            for (AttributeModifier mod : new HashSet<>(maxHealthAttr.getModifiers())) {
                                maxHealthAttr.removeModifier(mod);
                            }
                            
                            // Apply temp modifier (+200 ensures plenty of room)
                            AttributeModifier tempMod = new AttributeModifier(
                                UUID.randomUUID(),
                                "mmo.temp_health_fix",
                                200.0,
                                AttributeModifier.Operation.ADD_NUMBER
                            );
                            maxHealthAttr.addModifier(tempMod);
                            
                            // Set health to saved value immediately
                            player.setHealth(storedHealth);
                            
                            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                plugin.debugLog(DebugSystem.PROFILE,"Applied temporary health fix for " + player.getName() + 
                                            ": " + storedHealth);
                            }
                        }
                        
                        // Apply temporary mining speed fix - crucial for first join
                        AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
                        if (miningSpeedAttr != null) {
                            // Always apply, regardless of current value
                            // Remove any existing modifiers first
                            for (AttributeModifier mod : new HashSet<>(miningSpeedAttr.getModifiers())) {
                                miningSpeedAttr.removeModifier(mod);
                            }
                            
                            // Set base value to our default (0.5)
                            miningSpeedAttr.setBaseValue(0.5);
                            
                            // Apply temp modifier with the mining speed bonus
                            double miningSpeedBonus = storedMiningSpeed - 0.5;
                            if (miningSpeedBonus > 0) {
                                AttributeModifier tempMod = new AttributeModifier(
                                    UUID.randomUUID(),
                                    "mmo.temp_mining_speed_fix",
                                    miningSpeedBonus,
                                    AttributeModifier.Operation.ADD_NUMBER
                                );
                                miningSpeedAttr.addModifier(tempMod);
                                
                                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                    plugin.debugLog(DebugSystem.PROFILE,"Applied temporary mining speed fix for " + player.getName() + 
                                                ": " + storedMiningSpeed);
                                }
                            }
                        }
                        
                        // Apply temporary size fix - crucial for first join
                        AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
                        if (scaleAttr != null) {
                            // Always apply, regardless of current value
                            // Remove any existing modifiers first
                            for (AttributeModifier mod : new HashSet<>(scaleAttr.getModifiers())) {
                                scaleAttr.removeModifier(mod);
                            }
                            
                            // Set base value to default (1.0)
                            scaleAttr.setBaseValue(1.0);
                            
                            // Apply temp modifier with the size bonus
                            double sizeBonus = storedSize - 1.0;
                            if (sizeBonus != 0) {
                                AttributeModifier tempMod = new AttributeModifier(
                                    UUID.randomUUID(),
                                    "mmo.temp_size_fix",
                                    sizeBonus,
                                    AttributeModifier.Operation.ADD_NUMBER
                                );
                                scaleAttr.addModifier(tempMod);
                                
                                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                    plugin.debugLog(DebugSystem.PROFILE,"Applied temporary size fix for " + player.getName() + 
                                                ": " + storedSize);
                                }
                            }
                        }
                        
                        // Apply temporary attack range fix - crucial for first join
                        AttributeInstance rangeAttr = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
                        if (rangeAttr != null) {
                            // Always apply, regardless of current value
                            // Remove any existing modifiers first
                            for (AttributeModifier mod : new HashSet<>(rangeAttr.getModifiers())) {
                                rangeAttr.removeModifier(mod);
                            }
                            
                            // Set base value to default (3.0)
                            rangeAttr.setBaseValue(3.0);
                            
                            // Apply temp modifier with the range bonus
                            double rangeBonus = storedAttackRange - 3.0;
                            if (rangeBonus != 0) {
                                AttributeModifier tempMod = new AttributeModifier(
                                    UUID.randomUUID(),
                                    "mmo.temp_range_fix",
                                    rangeBonus,
                                    AttributeModifier.Operation.ADD_NUMBER
                                );
                                rangeAttr.addModifier(tempMod);
                                
                                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                    plugin.debugLog(DebugSystem.PROFILE,"Applied temporary attack range fix for " + player.getName() + 
                                                ": " + storedAttackRange);
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.debugLog(DebugSystem.PROFILE,"Error fixing attributes on join: " + e.getMessage());
                        if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                            e.printStackTrace();
                        }
                    }

                    // Start stat scanning to apply proper attribute values
                    plugin.getStatScanManager().startScanning(player);
                    
                    // Apply the final health value after stat scanning has applied all modifiers
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                                double healthToSet = Math.min(storedHealth, maxHealth);
                                
                                // Set health directly - don't check current value as it might be wrong
                                player.setHealth(healthToSet);
                                activeProfile.getStats().setCurrentHealth(healthToSet);
                                
                                // Now set the health display scale AFTER setting health
                                player.setHealthScaled(true);
                                player.setHealthScale(20.0);
                                
                                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                    plugin.debugLog(DebugSystem.PROFILE,"Final health restoration for " + player.getName() + 
                                            ": " + healthToSet + "/" + maxHealth);
                                }
                            }
                        }
                    }.runTaskLater(plugin, 10L); // Use a longer delay to ensure stats are fully applied
                }
            }
        }

        // Start health regeneration tracking
        plugin.getHealthRegenerationManager().startTracking(player);

        // Start scoreboard tracking
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    plugin.getScoreboardManager().startTracking(player);
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Reset a player's state to vanilla defaults (for new profile creation)
     */
    private void resetPlayerState(Player player) {
        // Clear inventory
        player.getInventory().clear();
        
        // Initialize all custom attributes
        initializeAttributes(player);
        
        // Reset health attribute to vanilla default first
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            // First remove any existing modifiers
            for (AttributeModifier mod : new HashSet<>(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getModifiers())) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(mod);
            }
            
            // Set base value to vanilla (20.0)
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            
            // Apply health bonus for 100 health instead of 20
            AttributeModifier healthMod = new AttributeModifier(
                UUID.randomUUID(),
                "mmo.initial_health",
                80.0, // +80 to reach 100 total
                AttributeModifier.Operation.ADD_NUMBER
            );
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(healthMod);
            
            // Set health to full (100) - default health stat
            player.setHealth(100.0);
        }
        
        // Reset other basic stats
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setExhaustion(0f);
        player.setLevel(0);
        player.setExp(0f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        
        // Clear potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
    /**
     * Handle profile selection when inventory is closed
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Use the constant title from ProfileGUI for consistency
        if (!event.getView().getTitle().equals(ProfileGUI.PROFILE_SELECTION_TITLE)) return;

        // Check if player has profiles
        ProfileManager pm = ProfileManager.getInstance();
        PlayerProfile[] profiles = pm.getProfiles(player.getUniqueId());
        boolean hasProfiles = false;
        Integer activeSlot = null;
        
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i] != null) {
                hasProfiles = true;
                activeSlot = i;
                break;
            }
        }

        // If player has no profiles, reopen the menu
        if (!hasProfiles) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        ProfileGUI.openProfileSelector(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        } else {
            // Player has at least one profile, check if it's newly created
            if (activeSlot != null) {
                PlayerProfile activeProfile = profiles[activeSlot];
                
                // If this is a newly created profile, apply default stats and START STAT SCANNING
                if (activeProfile.getLastPlayed() == activeProfile.getCreated()) {
                    // CRITICAL ADDITION: First, explicitly initialize all attributes
                    initializeAttributes(player);
                    
                    // Stop any existing scanning (clean slate)
                    plugin.getStatScanManager().stopScanning(player);
                    
                    // Start scanning with a slight delay to ensure profile is fully loaded
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.sendMessage(ChatColor.GREEN + "Setting up your character...");
                                
                                // Start stat scanning to apply equipment bonuses
                                plugin.getStatScanManager().startScanning(player);
                                
                                // Force an immediate scan after starting scanning
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (player.isOnline()) {
                                            plugin.getStatScanManager().scanAndUpdatePlayerStats(player);
                                            player.sendMessage(ChatColor.GREEN + "Your equipment has been properly initialized!");
                                            
                                            // Add debug logging
                                            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                                                plugin.debugLog(DebugSystem.PROFILE,"Forced equipment scan for " + player.getName() + " after profile creation");
                                                
                                                // Log mining speed attribute status
                                                AttributeInstance miningSpeed = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
                                                if (miningSpeed != null) {
                                                    plugin.debugLog(DebugSystem.PROFILE,"Mining speed attribute: base=" + miningSpeed.getBaseValue() + 
                                                        ", value=" + miningSpeed.getValue() + 
                                                        ", modifiers=" + miningSpeed.getModifiers().size());
                                                }
                                            }
                                        }
                                    }
                                }.runTaskLater(plugin, 5L);
                            }
                        }
                    }.runTaskLater(plugin, 5L);
                }
            }
        }
    }

    /**
     * Handle cosmetic updates on teleportation
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        CosmeticManager.getInstance().handlePlayerTeleport(event.getPlayer());
    }

    /**
     * Handle cosmetic updates on world changes
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        CosmeticManager.getInstance().updateCosmeticDisplay(player);
        
        // Update scoreboard immediately when changing worlds (for island detection)
        if (plugin.getScoreboardManager() != null) {
            // Run next tick to ensure world change is fully processed
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getScoreboardManager().updatePlayerScoreboard(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Handle cosmetic updates on respawn
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Delay cosmetic update until after respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                CosmeticManager.getInstance().updateCosmeticDisplay(event.getPlayer());
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
    * Handle player quitting the server
    */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Stop tracking health regeneration
        plugin.getHealthRegenerationManager().stopTracking(player);

        // Handle profile playtime tracking
        ProfileManager.getInstance().handlePlayerDisconnect(player);
        
        // Stop stat scanning
        plugin.getStatScanManager().stopScanning(player);
        
        // Stop scoreboard tracking
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().stopTracking(player);
        }

        // Save current profile before player quits
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                // CRITICAL: Always explicitly save current health
                double currentHealth = player.getHealth();
                profile.getStats().setCurrentHealth(currentHealth);
                
                // Now save the full profile
                profile.saveProfile(player);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Saved " + player.getName() + "'s health (" + currentHealth + 
                                ") to profile " + activeSlot);
                }
            }
        }
        
        // Reset attributes to vanilla defaults to prevent issues when rejoining
        plugin.getStatScanManager().resetAttributes(player);
    }

    /**
     * Initialize all custom attributes for a new player
     * This must happen BEFORE a profile is created to ensure proper initialization
     */
    private void initializeAttributes(Player player) {
        try {
            // CRITICAL: Initialize all attributes with baseline modifiers
            // These baseline modifiers ensure the attributes are properly registered in the game

            // HEALTH - Set base value and apply baseline
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(healthAttr.getModifiers())) {
                    healthAttr.removeModifier(mod);
                }
                
                // Set base value to vanilla default
                healthAttr.setBaseValue(20.0);
                
                // Apply baseline modifier for health
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.health.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                healthAttr.addModifier(baselineMod);
                
                // Apply health bonus for 100 health instead of 20
                AttributeModifier healthMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.initial_health",
                    80.0, // +80 to reach 100 total
                    AttributeModifier.Operation.ADD_NUMBER
                );
                healthAttr.addModifier(healthMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Initialized health attribute for " + player.getName() + 
                        " to 100.0 (base 20.0 + modifier 80.0)");
                }
            }
            
            // MINING SPEED attribute
            AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(miningSpeedAttr.getModifiers())) {
                    miningSpeedAttr.removeModifier(mod);
                }
                
                // Set base value to our default (0.5)
                miningSpeedAttr.setBaseValue(0.5);
                
                // Apply a permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.mining_speed.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                miningSpeedAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Initialized mining speed attribute for " + player.getName() + 
                        " to 0.5 (default value)");
                }
            }
            
            // SCALE attribute
            AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(scaleAttr.getModifiers())) {
                    scaleAttr.removeModifier(mod);
                }
                
                // Set base value to default (1.0)
                scaleAttr.setBaseValue(1.0);
                
                // Apply a permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.size.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                scaleAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Initialized scale attribute for " + player.getName() + 
                        " to 1.0 (default value)");
                }
            }
            
            // ATTACK RANGE attribute
            AttributeInstance rangeAttr = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (rangeAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(rangeAttr.getModifiers())) {
                    rangeAttr.removeModifier(mod);
                }
                
                // Set base value to default (3.0)
                rangeAttr.setBaseValue(3.0);
                
                // Apply a permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.attack_range.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                rangeAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Initialized attack range attribute for " + player.getName() + 
                        " to 3.0 (default value)");
                }
            }
            
            // ATTACK SPEED attribute
            AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(attackSpeedAttr.getModifiers())) {
                    attackSpeedAttr.removeModifier(mod);
                }
                
                // Set base value to our default (0.5)
                attackSpeedAttr.setBaseValue(0.5);
                
                // Apply a permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.attack_speed.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                attackSpeedAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE,"Initialized attack speed attribute for " + player.getName() + 
                        " to 0.5 (default value)");
                }
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.PROFILE,"Error initializing attributes: " + e.getMessage());
            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                e.printStackTrace();
            }
        }
        // Initialize step height attribute
        try {
            AttributeInstance stepHeightAttr = player.getAttribute(Attribute.GENERIC_STEP_HEIGHT);
            if (stepHeightAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(stepHeightAttr.getModifiers())) {
                    stepHeightAttr.removeModifier(mod);
                }
                
                // Set base value to default (0.6)
                stepHeightAttr.setBaseValue(0.6);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.step_height.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                stepHeightAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE, "Initialized step height attribute for " + player.getName() + 
                        " to default value (0.6)");
                }
            }
        } catch (Exception e) {
            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                plugin.debugLog(DebugSystem.PROFILE, "Step height attribute not supported: " + e.getMessage());
            }
        }
        // Initialize jump strength attribute
        try {
            AttributeInstance jumpAttr = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
            if (jumpAttr != null) {
                // Remove any existing modifiers
                for (AttributeModifier mod : new HashSet<>(jumpAttr.getModifiers())) {
                    jumpAttr.removeModifier(mod);
                }
                
                // Set base value to default (0.42)
                jumpAttr.setBaseValue(0.42);
                
                // Add permanent baseline modifier
                AttributeModifier baselineMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.jump_strength.baseline",
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                jumpAttr.addModifier(baselineMod);
                
                if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                    plugin.debugLog(DebugSystem.PROFILE, "Initialized jump strength attribute for " + player.getName() + 
                        " to default value (0.42)");
                }
            }
        } catch (Exception e) {
            if (plugin.isDebugEnabled(DebugSystem.PROFILE)) {
                plugin.debugLog(DebugSystem.PROFILE, "Jump strength attribute not supported: " + e.getMessage());
            }
        }
    }

    /**
     * Helper class to hold attribute initialization data
     */
    private static class AttributeInitData {
        public final Attribute attribute;
        public final double baseValue;
        public final String modifierName;
        
        public AttributeInitData(Attribute attribute, double baseValue, String modifierName) {
            this.attribute = attribute;
            this.baseValue = baseValue;
            this.modifierName = modifierName;
        }
    }
}