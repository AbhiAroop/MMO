package com.server.profiles.skills.skills.farming.botany;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;

import java.util.UUID;

/**
 * Represents a custom breeder block using an armor stand with custom model data
 */
public class BreederBlock {
    
    private static final int BREEDER_CMD = 200001; // Custom model data for breeder block
    
    private final UUID armorStandId;
    private final Location location;
    private ArmorStand armorStand;
    
    public BreederBlock(Location location) {
        this.location = location.clone();
        this.armorStand = spawnArmorStand(location);
        this.armorStandId = armorStand.getUniqueId();
    }
    
    /**
     * Spawn the armor stand with custom model data helmet
     */
    private ArmorStand spawnArmorStand(Location loc) {
        Location spawnLoc = loc.clone().add(0.5, 0, 0.5);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        
        // Configure armor stand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(false);
        stand.setMarker(true); // Prevents collision
        stand.setCustomNameVisible(false);
        
        // Set head rotation to look like a block
        stand.setHeadPose(new EulerAngle(0, 0, 0));
        
        // Create custom model data helmet
        ItemStack helmet = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(BREEDER_CMD);
            helmet.setItemMeta(meta);
        }
        stand.getEquipment().setHelmet(helmet);
        
        return stand;
    }
    
    /**
     * Update the nameplate above the breeder
     */
    public void updateNameplate(String status, int remainingSeconds) {
        if (armorStand != null && armorStand.isValid()) {
            if (remainingSeconds > 0) {
                armorStand.setCustomName("§6§l[Breeder] §e" + status + " §7(" + remainingSeconds + "s)");
                armorStand.setCustomNameVisible(true);
            } else {
                armorStand.setCustomName("§6§l[Breeder] §a" + status);
                armorStand.setCustomNameVisible(true);
            }
        }
    }
    
    /**
     * Hide the nameplate
     */
    public void hideNameplate() {
        if (armorStand != null && armorStand.isValid()) {
            armorStand.setCustomNameVisible(false);
        }
    }
    
    /**
     * Open the breeder GUI for a player
     */
    public void openGUI(Player player) {
        BreederData data = BotanyManager.getInstance().getBreederData(armorStandId);
        if (data == null) {
            data = new BreederData(armorStandId, location);
            BotanyManager.getInstance().registerBreederData(data);
        }
        
        BreederGUI gui = new BreederGUI(data);
        gui.open(player);
    }
    
    /**
     * Remove this breeder block
     */
    public void remove() {
        if (armorStand != null && armorStand.isValid()) {
            armorStand.remove();
        }
        BotanyManager.getInstance().removeBreederData(armorStandId);
    }
    
    /**
     * Check if the armor stand is still valid
     */
    public boolean isValid() {
        if (armorStand == null || !armorStand.isValid()) {
            // Try to find it again
            armorStand = (ArmorStand) location.getWorld().getNearbyEntities(location, 1, 1, 1)
                .stream()
                .filter(e -> e.getUniqueId().equals(armorStandId))
                .findFirst()
                .orElse(null);
        }
        return armorStand != null && armorStand.isValid();
    }
    
    // Getters
    public UUID getArmorStandId() {
        return armorStandId;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public ArmorStand getArmorStand() {
        return armorStand;
    }
    
    public static int getBreederCMD() {
        return BREEDER_CMD;
    }
    
    /**
     * Create a breeder block item for giving to players
     */
    public static ItemStack createBreederItem() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(BREEDER_CMD);
            meta.setDisplayName("§6§lCrop Breeder");
            meta.setLore(java.util.Arrays.asList(
                "§7Place this block to create a breeder",
                "§7Right-click to open the breeding GUI",
                "§7",
                "§eBreed custom crops together!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
