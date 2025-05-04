package com.server.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("JOIN: " + player.getName() + "'s stored health: " + storedHealth + 
                                    ", current: " + player.getHealth() + ", stored mining speed: " + storedMiningSpeed);
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
                            
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("Applied temporary health fix for " + player.getName() + 
                                            ": " + storedHealth);
                            }
                        }
                        
                        // Apply temporary mining speed fix - crucial for first join
                        AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
                        if (miningSpeedAttr != null && Math.abs(miningSpeedAttr.getValue() - 1.0) < 0.1 && storedMiningSpeed > 0.0) {
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
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Applied temporary mining speed fix for " + player.getName() + 
                                                ": " + storedMiningSpeed);
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error fixing attributes on join: " + e.getMessage());
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
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Final health restoration for " + player.getName() + 
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
        if (!event.getView().getTitle().equals("Profile Selection")) return;

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
                    ProfileGUI.openProfileSelector(player);
                    player.sendMessage(ChatColor.RED + "You must create at least one profile!");
                }
            }.runTaskLater(plugin, 1L);
        } else {
            // Player has at least one profile, check if it's newly created
            if (activeSlot != null) {
                PlayerProfile activeProfile = profiles[activeSlot];
                
                // If this is a newly created profile, apply default stats
                if (activeProfile.getLastPlayed() == activeProfile.getCreated()) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Setting up new profile for " + player.getName());
                    }
                    
                    // Create a final copy of activeSlot for use in the inner class
                    final Integer finalActiveSlot = activeSlot;
                    
                    // Initialize profile with a short delay
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                // Set profile as active if it's not already
                                if (pm.getActiveProfile(player.getUniqueId()) == null) {
                                    pm.selectProfile(player, finalActiveSlot);
                                }
                                
                                // Start the stat scanning system to set default values
                                plugin.getStatScanManager().startScanning(player);
                                
                                // Start health regeneration
                                plugin.getHealthRegenerationManager().startTracking(player);
                                
                                // Update scoreboard
                                if (plugin.getScoreboardManager() != null) {
                                    plugin.getScoreboardManager().startTracking(player);
                                }
                                
                                // Notify player
                                player.sendMessage(ChatColor.GREEN + "Profile created with default stats!");
                            }
                        }
                    }.runTaskLater(plugin, 5L); // Short delay to ensure everything is ready
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
        CosmeticManager.getInstance().updateCosmeticDisplay(event.getPlayer());
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
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Saved " + player.getName() + "'s health (" + currentHealth + 
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
        // Create a list of all custom attributes we need to initialize
        List<AttributeInitData> attributes = Arrays.asList(
            // Format: Attribute, base value, modifier name
            new AttributeInitData(Attribute.PLAYER_BLOCK_BREAK_SPEED, 0.5, "mmo.mining_speed"),
            new AttributeInitData(Attribute.GENERIC_SCALE, 1.0, "mmo.size"),
            new AttributeInitData(Attribute.PLAYER_ENTITY_INTERACTION_RANGE, 3.0, "mmo.attack_range"),
            new AttributeInitData(Attribute.GENERIC_ATTACK_SPEED, 0.5, "mmo.attackspeed")
        );
        
        // Initialize each attribute
        for (AttributeInitData attrData : attributes) {
            try {
                AttributeInstance attr = player.getAttribute(attrData.attribute);
                if (attr != null) {
                    // First remove any existing modifiers
                    for (AttributeModifier mod : new HashSet<>(attr.getModifiers())) {
                        if (mod.getName().contains("mmo.")) {
                            attr.removeModifier(mod);
                        }
                    }
                    
                    // Set base value to our custom default
                    attr.setBaseValue(attrData.baseValue);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Initialized attribute " + attrData.attribute.name() + 
                            " to " + attrData.baseValue + " for new player: " + player.getName());
                    }
                }
            } catch (Exception e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Error initializing " + attrData.attribute.name() + 
                        " attribute: " + e.getMessage());
                }
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