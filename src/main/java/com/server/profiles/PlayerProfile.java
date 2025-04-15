package com.server.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.items.ItemType;
import com.server.profiles.skills.PlayerSkills;
import com.server.profiles.stats.PlayerStats;

public class PlayerProfile {
    private final UUID playerUUID;
    private final int slot;
    private String name;
    private PlayerStats stats;
    private PlayerSkills skills;
    private long created;
    private long lastPlayed;
    private ItemStack[] inventoryContents;
    private ItemStack[] armorContents;
    private ItemStack[] extraContents; // For off-hand items
    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    private final Map<ItemType, ItemStack> cosmetics = new HashMap<>();

    public PlayerProfile(UUID playerUUID, int slot, String name) {
        this.playerUUID = playerUUID;
        this.slot = slot;
        this.name = name;
        this.stats = new PlayerStats();
        this.skills = new PlayerSkills();
        this.created = System.currentTimeMillis();
        this.lastPlayed = System.currentTimeMillis();
        this.inventoryContents = new ItemStack[36]; // Main inventory
        this.armorContents = new ItemStack[4]; // Armor slots
        this.extraContents = new ItemStack[1]; // Off-hand slot
        this.worldName = "world"; // Default world
        this.x = 0;
        this.y = 64; // Default spawn height
        this.z = 0;
        this.yaw = 0;
        this.pitch = 0;
    }

    public void saveInventory(Player player) {
        this.inventoryContents = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        this.extraContents = player.getInventory().getExtraContents().clone();
        updateLastPlayed();
    }

    public void loadInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(inventoryContents.clone());
        player.getInventory().setArmorContents(armorContents.clone());
        player.getInventory().setExtraContents(extraContents.clone());
        updateLastPlayed();
    }

    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }

    public void saveLocation(Player player) {
        Location loc = player.getLocation();
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    public void teleportPlayer(Player player) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Error: World '" + worldName + "' not found!");
            return;
        }

        Location loc = new Location(world, x, y, z, yaw, pitch);
        player.teleport(loc);
    }

    public void loadProfile(Player player) {
        loadInventory(player);
        teleportPlayer(player);
        stats.applyToPlayer(player);
        updateLastPlayed();
    }

    public void saveProfile(Player player) {
        saveInventory(player);
        saveLocation(player);
        stats.updateFromPlayer(player);
        updateLastPlayed();
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public int getSlot() { return slot; }
    public String getName() { return name; }
    public PlayerStats getStats() { return stats; }
    public PlayerSkills getSkills() { return skills; }
    public long getCreated() { return created; }
    public long getLastPlayed() { return lastPlayed; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public Map<ItemType, ItemStack> getCosmetics() {
    return cosmetics;
    }   

    public void setCosmetic(ItemType type, ItemStack item) {
        cosmetics.put(type, item);
    }

    public void removeCosmetic(ItemType type) {
        cosmetics.remove(type);
    }

}