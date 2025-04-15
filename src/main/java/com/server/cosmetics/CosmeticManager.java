package com.server.cosmetics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.items.ItemType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CosmeticManager {
    private static CosmeticManager instance;
    private final Main plugin;
    private final Map<UUID, ArmorStand> cosmeticStands = new HashMap<>();

    private CosmeticManager(Main plugin) {
        this.plugin = plugin;
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

    public void updateCosmeticDisplay(Player player) {
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