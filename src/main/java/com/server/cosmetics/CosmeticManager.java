package com.server.cosmetics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.items.ItemType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CosmeticManager implements Listener {
    private static CosmeticManager instance;
    private final Main plugin;
    private final Map<UUID, ArmorStand> cosmeticStands = new HashMap<>();

    private CosmeticManager(Main plugin) {
        this.plugin = plugin;
        // Register this class as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new CosmeticManager(plugin);
        }
    }

    public static CosmeticManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CosmeticManager has not been initialized!");
        }
        return instance;
    }

    /**
     * Handle gamemode changes to show/hide cosmetics
     */
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameMode newGameMode = event.getNewGameMode();
        
        // Delay the cosmetic update to ensure gamemode change is complete
        new BukkitRunnable() {
            @Override
            public void run() {
                if (newGameMode == GameMode.SPECTATOR) {
                    // Remove cosmetics in spectator mode
                    hideCosmetics(player);
                } else if (newGameMode == GameMode.SURVIVAL || newGameMode == GameMode.CREATIVE || newGameMode == GameMode.ADVENTURE) {
                    // Show cosmetics in non-spectator modes
                    showCosmetics(player);
                }
            }
        }.runTaskLater(plugin, 1L); // 1 tick delay to ensure gamemode change is processed
    }

    /**
     * Hide cosmetics by removing the armor stand passenger
     */
    private void hideCosmetics(Player player) {
        ArmorStand stand = cosmeticStands.get(player.getUniqueId());
        if (stand != null && stand.isValid()) {
            // Remove the stand as a passenger but don't delete it
            if (stand.isInsideVehicle()) {
                player.removePassenger(stand);
            }
            // Move the stand far away and make it invisible
            stand.teleport(player.getLocation().add(0, -100, 0));
            stand.setVisible(false);
        }
    }

    /**
     * Show cosmetics by re-adding the armor stand passenger
     */
    private void showCosmetics(Player player) {
        ArmorStand stand = cosmeticStands.get(player.getUniqueId());
        if (stand != null && stand.isValid()) {
            // Teleport the stand back to the player and make it a passenger again
            stand.teleport(player.getLocation());
            stand.setVisible(false); // Keep it invisible (only equipment shows)
            player.addPassenger(stand);
        } else {
            // If no stand exists or it's invalid, create a new one
            updateCosmeticDisplay(player);
        }
    }

    public void updateCosmeticDisplay(Player player) {
        // Don't show cosmetics if player is in spectator mode
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Remove existing stand if present
        ArmorStand oldStand = cosmeticStands.get(player.getUniqueId());
        if (oldStand != null) {
            oldStand.remove();
        }

        // Create new stand
        ArmorStand stand = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        
        // Configure stand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setBasePlate(false);
        
        // Reset head pose
        stand.setHeadPose(stand.getHeadPose().setX(0));
        stand.setHeadPose(stand.getHeadPose().setY(0));
        stand.setHeadPose(stand.getHeadPose().setZ(0));
        
        // Make player carry the armor stand
        player.addPassenger(stand);
        
        // Store reference
        cosmeticStands.put(player.getUniqueId(), stand);

        // Apply cosmetics
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                Map<ItemType, ItemStack> cosmetics = profile.getCosmetics();
                
                // Clear current equipment
                stand.getEquipment().clear();
                
                // Apply cosmetics
                for (Map.Entry<ItemType, ItemStack> entry : cosmetics.entrySet()) {
                    switch (entry.getKey()) {
                        case COSMETIC_HELMET:
                            stand.getEquipment().setHelmet(entry.getValue());
                            break;
                        case COSMETIC_CHESTPLATE:
                            stand.getEquipment().setChestplate(entry.getValue());
                            break;
                        case COSMETIC_LEGGINGS:
                            stand.getEquipment().setLeggings(entry.getValue());
                            break;
                        case COSMETIC_BOOTS:
                            stand.getEquipment().setBoots(entry.getValue());
                            break;
                    }
                }
            }
        }
    }

    public void removeCosmetics(Player player) {
        ArmorStand stand = cosmeticStands.remove(player.getUniqueId());
        if (stand != null) {
            stand.remove();
        }
    }

    public void handlePlayerTeleport(Player player) {
        // Don't handle teleports for spectators
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // For teleports, sometimes we need to reapply the passenger
        // Wait a tick for teleport to complete
        ArmorStand stand = cosmeticStands.get(player.getUniqueId());
        if (stand != null) {
            // Remove old stand and create a new one
            removeCosmetics(player);
            updateCosmeticDisplay(player);
        }
    }

    // Add cleanup method for plugin disable
    public void cleanup() {
        // Remove all armor stands
        for (ArmorStand stand : cosmeticStands.values()) {
            if (stand != null) {
                stand.remove();
            }
        }
        cosmeticStands.clear();
    }

    public static void setInstance(CosmeticManager instance) {
        CosmeticManager.instance = instance;
    }
}