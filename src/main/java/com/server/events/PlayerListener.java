package com.server.events;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
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
            // Start tracking stats for returning player
            statsManager.startTracking(player);
        }
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
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        if (!event.getView().getTitle().equals("Profile Selection")) return;

        // Check if player has no profiles
        ProfileManager pm = ProfileManager.getInstance();
        PlayerProfile[] profiles = pm.getProfiles(player.getUniqueId());
        boolean hasProfiles = false;
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                hasProfiles = true;
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ProfileManager pm = ProfileManager.getInstance();
        Integer activeSlot = pm.getActiveProfile(event.getPlayer().getUniqueId());
        CosmeticManager.getInstance().removeCosmetics(event.getPlayer());
        
        if (activeSlot != null) {
            // Save inventory and location to active profile
            PlayerProfile[] profiles = pm.getProfiles(event.getPlayer().getUniqueId());
            if (profiles[activeSlot] != null) {
                profiles[activeSlot].saveProfile(event.getPlayer());
            }
        }
    }
}