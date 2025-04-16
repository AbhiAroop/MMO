package com.server.events;

import java.util.HashSet;
import java.util.Set;
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
import com.server.profiles.stats.PlayerStats;
import com.server.profiles.stats.StatsUpdateManager;

public class PlayerListener implements Listener {
    private final Main plugin;
    private final StatsUpdateManager statsManager;
    private final RangedCombatManager rangedCombatManager;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.statsManager = new StatsUpdateManager(plugin);
        this.rangedCombatManager = new RangedCombatManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ProfileManager pm = ProfileManager.getInstance();
        PlayerProfile[] profiles = pm.getProfiles(player.getUniqueId());

        // Always set health display scale - but don't modify actual health yet
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
        
        // Check if player has no profiles (first time joining)
        boolean hasProfiles = false;
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                hasProfiles = true;
                break;
            }
        }

        if (!hasProfiles) {
            // Reset player's state for first time join
            resetPlayerState(player);
            
            // Open profile creation menu in the next tick
            new BukkitRunnable() {
                @Override
                public void run() {
                    ProfileGUI.openProfileSelector(player);
                }
            }.runTaskLater(plugin, 1L);
        } else {
            // Start tracking stats for returning player with an active profile
            Integer activeSlot = pm.getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                // Apply default health for the existing profile
                PlayerProfile activeProfile = profiles[activeSlot];
                if (activeProfile != null) {
                    // Apply health after a short delay to ensure the player is fully loaded
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                // Apply the health setting from the profile
                                int profileHealth = activeProfile.getStats().getHealth();
                                applyDefaultHealth(player, profileHealth);
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Applied profile health to returning player: " + 
                                                        profileHealth + " for " + player.getName());
                                }
                            }
                        }
                    }.runTaskLater(plugin, 5L);
                }
            }
            
            // Start tracking stats
            statsManager.startTracking(player);
        }

        // Start scoreboard tracking with a delay to ensure everything is properly loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (plugin != null && plugin.getScoreboardManager() != null) {
                        plugin.getScoreboardManager().startTracking(player);
                    }
                }
            }
        }.runTaskLater(plugin, 20L); // Delay to ensure player is fully loaded
    }

    private void resetPlayerState(Player player) {
        // Clear inventory
        player.getInventory().clear();
        
        // Reset attributes to vanilla defaults
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        }
        if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1.0);
        }
        if (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
        }
        if (player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(3.0);
        }
        if (player.getAttribute(Attribute.GENERIC_ARMOR) != null) {
            player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(0.0);
        }
        
        // Reset basic stats
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setExhaustion(0f);
        player.setLevel(0);
        player.setExp(0f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        
        // Reset flight
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Clear potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    // Add the needed null checks in the onInventoryClose method:
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
                        plugin.getLogger().info("Applying default stats to new profile for " + player.getName());
                    }
                    
                    // Create a final copy of activeSlot for use in the inner class
                    final Integer finalActiveSlot = activeSlot;
                    
                    // Start tracking stats - this will apply the proper health values
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                // Set profile as active if it's not already
                                if (pm.getActiveProfile(player.getUniqueId()) == null) {
                                    pm.selectProfile(player, finalActiveSlot);
                                }
                                
                                // Apply default stats from PlayerStats class
                                PlayerStats defaultStats = activeProfile.getStats();
                                
                                // Apply health stat
                                int defaultHealth = defaultStats.getDefaultHealth();
                                applyDefaultHealth(player, defaultHealth);
                                
                                // Apply other default attributes
                                applyDefaultAttributes(player, defaultStats);
                                
                                // Start tracking to keep stats updated with equipment
                                if (statsManager != null) {
                                    statsManager.startTracking(player);
                                }
                                
                                // Update scoreboard
                                if (plugin != null && plugin.getScoreboardManager() != null) {
                                    plugin.getScoreboardManager().startTracking(player);
                                }
                                
                                // Update action bar with new stats
                                player.sendMessage(ChatColor.GREEN + "Profile created with default stats! Health: " + defaultHealth);
                            }
                        }
                    }.runTaskLater(plugin, 5L); // Short delay to ensure everything is ready
                }
            }
        }
    }
   /**
     * Applies the default health stat to the player
     */
    private void applyDefaultHealth(Player player, int defaultHealth) {
        try {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Applying health of " + defaultHealth + " to " + player.getName());
            }
            
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                // Clear existing modifiers
                Set<AttributeModifier> healthModifiers = new HashSet<>(maxHealth.getModifiers());
                for (AttributeModifier mod : healthModifiers) {
                    maxHealth.removeModifier(mod);
                }
                
                // Set base value to vanilla default
                maxHealth.setBaseValue(20.0);
                
                // Create a modifier to add the difference between default and vanilla
                double healthBonus = defaultHealth - 20.0;
                if (healthBonus != 0) {
                    AttributeModifier healthMod = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.health",
                        healthBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    maxHealth.addModifier(healthMod);
                }
                
                // Give the attribute time to update
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            // Set health to max
                            double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                            player.setHealth(currentMaxHealth);
                            
                            // Set health display to always show 10 hearts
                            player.setHealthScaled(true);
                            player.setHealthScale(20.0); // 20.0 = 10 hearts
                            
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("Health set for " + player.getName() + ": " + 
                                                    player.getHealth() + "/" + currentMaxHealth);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied default health stat to " + player.getName() + ": " + 
                                        defaultHealth + " (final value: " + maxHealth.getValue() + ")");
                    plugin.getLogger().info("Set health display scale to 10 hearts");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying default health: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Applies other default attributes from PlayerStats to the player
     */
    private void applyDefaultAttributes(Player player, PlayerStats stats) {
        try {
            // Apply size if available
            try {
                AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
                if (scaleAttribute != null) {
                    double defaultSize = stats.getDefaultSize();
                    
                    // Clear modifiers
                    Set<AttributeModifier> scaleModifiers = new HashSet<>(scaleAttribute.getModifiers());
                    for (AttributeModifier mod : scaleModifiers) {
                        scaleAttribute.removeModifier(mod);
                    }
                    
                    // Set base value to vanilla default
                    scaleAttribute.setBaseValue(1.0);
                    
                    // Add modifier for default size if not 1.0
                    if (defaultSize != 1.0) {
                        double sizeBonus = defaultSize - 1.0;
                        AttributeModifier sizeMod = new AttributeModifier(
                            UUID.randomUUID(),
                            "mmo.size",
                            sizeBonus,
                            AttributeModifier.Operation.ADD_NUMBER
                        );
                        scaleAttribute.addModifier(sizeMod);
                    }
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Applied default size to " + player.getName() + ": " + 
                                            defaultSize + " (final value: " + scaleAttribute.getValue() + ")");
                    }
                }
            } catch (Exception e) {
                // Ignore if scale attribute isn't available in this version
            }
            
            // Apply attack range if available
            try {
                AttributeInstance rangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
                if (rangeAttribute != null) {
                    double defaultRange = stats.getDefaultAttackRange();
                    
                    // Clear modifiers
                    Set<AttributeModifier> rangeModifiers = new HashSet<>(rangeAttribute.getModifiers());
                    for (AttributeModifier mod : rangeModifiers) {
                        rangeAttribute.removeModifier(mod);
                    }
                    
                    // Set base value to vanilla default
                    rangeAttribute.setBaseValue(3.0);
                    
                    // Add modifier for default range if not 3.0
                    if (defaultRange != 3.0) {
                        double rangeBonus = defaultRange - 3.0;
                        AttributeModifier rangeMod = new AttributeModifier(
                            UUID.randomUUID(),
                            "mmo.attack_range",
                            rangeBonus,
                            AttributeModifier.Operation.ADD_NUMBER
                        );
                        rangeAttribute.addModifier(rangeMod);
                    }
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Applied default attack range to " + player.getName() + ": " + 
                                            defaultRange + " (final value: " + rangeAttribute.getValue() + ")");
                    }
                }
            } catch (Exception e) {
                // Ignore if range attribute isn't available in this version
            }
            
            // Apply attack speed
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                double defaultAttackSpeed = stats.getDefaultAttackSpeed();
                
                // Clear modifiers
                Set<AttributeModifier> speedModifiers = new HashSet<>(attackSpeedAttribute.getModifiers());
                for (AttributeModifier mod : speedModifiers) {
                    attackSpeedAttribute.removeModifier(mod);
                }
                
                // Set base value to vanilla default
                attackSpeedAttribute.setBaseValue(4.0);
                
                // Add modifier for default attack speed if not 4.0
                if (defaultAttackSpeed != 4.0) {
                    double speedBonus = defaultAttackSpeed - 4.0;
                    AttributeModifier speedMod = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.attackspeed",
                        speedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackSpeedAttribute.addModifier(speedMod);
                }
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied default attack speed to " + player.getName() + ": " + 
                                        defaultAttackSpeed + " (final value: " + attackSpeedAttribute.getValue() + ")");
                }
            }
            
            // Apply movement speed
            AttributeInstance movementSpeedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeedAttribute != null) {
                double defaultSpeed = stats.getDefaultSpeed();
                movementSpeedAttribute.setBaseValue(defaultSpeed);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied default movement speed to " + player.getName() + ": " + defaultSpeed);
                }
            }
            
            // Set mana to full
            stats.setMana(stats.getDefaultMana());
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Set mana to default value: " + stats.getDefaultMana());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying default attributes: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        CosmeticManager.getInstance().handlePlayerTeleport(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        CosmeticManager.getInstance().updateCosmeticDisplay(event.getPlayer());
    }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up tasks
        statsManager.stopTracking(player);
        if (plugin != null && plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().stopTracking(player);
        }

        // Save current profile before player quits
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                // Important: Update the profile with current values before saving
                profile.saveProfile(player);
            }
        }
        
        // Reset attributes to vanilla defaults to prevent issues when rejoining
        resetAttributesToDefaults(player);
    }

    /**
     * Reset all attributes to their vanilla Minecraft defaults
     * This prevents attribute stacking issues when the player rejoins
     */
    private void resetAttributesToDefaults(Player player) {
        // Reset all attributes to default values
        try {
            if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                Set<AttributeModifier> healthModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getModifiers());
                for (AttributeModifier mod : healthModifiers) {
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(mod);
                }
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
            
            if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                Set<AttributeModifier> damageModifiers;
                damageModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getModifiers());
                for (AttributeModifier mod : damageModifiers) {
                    player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).removeModifier(mod);
                }
                player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1.0);
            }
            
            if (player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) != null) {
                Set<AttributeModifier> speedModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getModifiers());
                for (AttributeModifier mod : speedModifiers) {
                    player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(mod);
                }
                player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0);
            }
            
            if (player.getAttribute(Attribute.GENERIC_ARMOR) != null) {
                Set<AttributeModifier> armorModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_ARMOR).getModifiers());
                for (AttributeModifier mod : armorModifiers) {
                    player.getAttribute(Attribute.GENERIC_ARMOR).removeModifier(mod);
                }
                player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(0.0);
            }
            
            if (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                Set<AttributeModifier> moveSpeedModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getModifiers());
                for (AttributeModifier mod : moveSpeedModifiers) {
                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(mod);
                }
                player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
            }
            
            // Reset SCALE attribute (1.20.5+)
            try {
                if (player.getAttribute(Attribute.GENERIC_SCALE) != null) {
                    Set<AttributeModifier> scaleModifiers = new HashSet<>(player.getAttribute(Attribute.GENERIC_SCALE).getModifiers());
                    for (AttributeModifier mod : scaleModifiers) {
                        player.getAttribute(Attribute.GENERIC_SCALE).removeModifier(mod);
                    }
                    player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.0);
                }
            } catch (Exception e) {
                // Ignore if the attribute doesn't exist
            }
            
            // Reset PLAYER_ENTITY_INTERACTION_RANGE attribute (1.20.5+)
            try {
                if (player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE) != null) {
                    Set<AttributeModifier> rangeModifiers = new HashSet<>(player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).getModifiers());
                    for (AttributeModifier mod : rangeModifiers) {
                        player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).removeModifier(mod);
                    }
                    player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(3.0);
                }
            } catch (Exception e) {
                // Ignore if the attribute doesn't exist
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting attributes for " + player.getName() + ": " + e.getMessage());
        }
    }
}