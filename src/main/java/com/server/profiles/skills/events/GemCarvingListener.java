package com.server.profiles.skills.events;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.server.Main;
import com.server.profiles.skills.minigames.GemCarvingMinigame;
import com.server.utils.NamespacedKeyUtils;

/**
 * Listener for gem carving interactions
 */
public class GemCarvingListener implements Listener {
    
    private final Main plugin;
    private final GemCarvingMinigame minigame;
    
    public GemCarvingListener(Main plugin, GemCarvingMinigame minigame) {
        this.plugin = plugin;
        this.minigame = minigame;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle left clicks in air or at blocks
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        // Ignore off-hand clicks
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // If player is already in a game, this click is for the minigame
        if (minigame.isPlayerInGame(player)) {            
            // Handle the click in the minigame
            minigame.handleClick(player, null);
            event.setCancelled(true);
            return;
        }
        
        // Not in a game, check if they're trying to start one by clicking near a crystal
        if (event.getAction() == Action.LEFT_CLICK_AIR) {
            // Check if they're looking at a crystal
            ArmorStand crystal = findTargetCrystal(player, 3.0);
            if (crystal != null) {
                // Try to start a game with this crystal
                boolean gameStarted = minigame.tryStartGame(player, crystal.getLocation());
                if (gameStarted) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Check for nearby crystals
            for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof ArmorStand) {
                    ArmorStand stand = (ArmorStand) entity;
                    PersistentDataContainer container = stand.getPersistentDataContainer();
                    
                    if (container.has(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING)) {
                        boolean gameStarted = minigame.tryStartGame(player, stand.getLocation());
                        if (gameStarted) {
                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get the location where the player is looking using ray tracing
     */
    private Location getRayTraceLocation(Player player, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Location targetLocation = eyeLocation.clone().add(eyeLocation.getDirection().multiply(maxDistance));
        return targetLocation;
    }
    
    /**
     * Find a crystal that the player is looking at
     */
    private ArmorStand findTargetCrystal(Player player, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        
        for (Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                PersistentDataContainer container = stand.getPersistentDataContainer();
                
                if (container.has(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING)) {
                    // Check if player is looking towards this crystal
                    if (isLookingAt(eyeLocation, entity.getLocation(), 0.5)) {
                        return stand;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a location is in the line of sight
     */
    private boolean isLookingAt(Location eye, Location target, double threshold) {
        // Get the direction from eye to target
        Location directionVector = target.clone().subtract(eye).toVector().normalize().toLocation(eye.getWorld());
        
        // Get the player's looking direction
        Location playerDirection = eye.getDirection().normalize().toLocation(eye.getWorld());
        
        // Calculate dot product to determine alignment (1 = perfect alignment)
        double dotProduct = directionVector.getX() * playerDirection.getX() +
                           directionVector.getY() * playerDirection.getY() +
                           directionVector.getZ() * playerDirection.getZ();
        
        // If the dot product is greater than the threshold, they're looking at it
        return dotProduct > (1 - threshold);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // End any active session when player quits
        minigame.endSession(event.getPlayer().getUniqueId(), true);
    }
}