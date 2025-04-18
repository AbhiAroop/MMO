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

    // Economy currencies
    private int units;          // Basic currency for trading and shops
    private int premiumUnits;   // Rare currency for special items
    private int essence;        // Progression currency, non-tradeable
    private int bits;           // Premium currency from store, for cosmetics

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
        
        // Initialize currencies with default values
        this.units = 0;
        this.premiumUnits = 0;
        this.essence = 0;
        this.bits = 0;
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

        double currentHealth = player.getHealth();

        stats.applyToPlayerWithoutHealth(player);

        player.setHealthScaled(true);
        player.setHealthScale(20.0);

        updateLastPlayed();
    }

    public void saveProfile(Player player) {
        saveInventory(player);
        saveLocation(player);
        
        // Important: Update the profile with current values before saving
        stats.updateFromPlayer(player);
        
        // Critical: Explicitly save current health value
        if (player.getHealth() > 0) {
            stats.setCurrentHealth(player.getHealth());
        }
        
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

    // Currency getters and setters
    
    /**
     * Get the player's Units balance
     * Units are the basic in-game currency used for trading and shops
     * @return The amount of Units
     */
    public int getUnits() {
        return units;
    }
    
    /**
     * Set the player's Units balance
     * @param units The new amount of Units
     */
    public void setUnits(int units) {
        this.units = Math.max(0, units); // Prevent negative balance
    }
    
    /**
     * Add Units to the player's balance
     * @param amount The amount to add
     * @return The new balance
     */
    public int addUnits(int amount) {
        if (amount > 0) {
            this.units += amount;
        }
        return this.units;
    }
    
    /**
     * Remove Units from the player's balance
     * @param amount The amount to remove
     * @return True if the player had enough Units and they were removed, false otherwise
     */
    public boolean removeUnits(int amount) {
        if (amount <= 0) return true;
        
        if (this.units >= amount) {
            this.units -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Get the player's Premium Units balance
     * Premium Units are a rare currency used for special items
     * @return The amount of Premium Units
     */
    public int getPremiumUnits() {
        return premiumUnits;
    }
    
    /**
     * Set the player's Premium Units balance
     * @param premiumUnits The new amount of Premium Units
     */
    public void setPremiumUnits(int premiumUnits) {
        this.premiumUnits = Math.max(0, premiumUnits); // Prevent negative balance
    }
    
    /**
     * Add Premium Units to the player's balance
     * @param amount The amount to add
     * @return The new balance
     */
    public int addPremiumUnits(int amount) {
        if (amount > 0) {
            this.premiumUnits += amount;
        }
        return this.premiumUnits;
    }
    
    /**
     * Remove Premium Units from the player's balance
     * @param amount The amount to remove
     * @return True if the player had enough Premium Units and they were removed, false otherwise
     */
    public boolean removePremiumUnits(int amount) {
        if (amount <= 0) return true;
        
        if (this.premiumUnits >= amount) {
            this.premiumUnits -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Get the player's Essence balance
     * Essence is a non-tradeable currency used for progression
     * @return The amount of Essence
     */
    public int getEssence() {
        return essence;
    }
    
    /**
     * Set the player's Essence balance
     * @param essence The new amount of Essence
     */
    public void setEssence(int essence) {
        this.essence = Math.max(0, essence); // Prevent negative balance
    }
    
    /**
     * Add Essence to the player's balance
     * @param amount The amount to add
     * @return The new balance
     */
    public int addEssence(int amount) {
        if (amount > 0) {
            this.essence += amount;
        }
        return this.essence;
    }
    
    /**
     * Remove Essence from the player's balance
     * @param amount The amount to remove
     * @return True if the player had enough Essence and it was removed, false otherwise
     */
    public boolean removeEssence(int amount) {
        if (amount <= 0) return true;
        
        if (this.essence >= amount) {
            this.essence -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Get the player's Bits balance
     * Bits are a premium currency purchased with real money, used for cosmetics
     * @return The amount of Bits
     */
    public int getBits() {
        return bits;
    }
    
    /**
     * Set the player's Bits balance
     * @param bits The new amount of Bits
     */
    public void setBits(int bits) {
        this.bits = Math.max(0, bits); // Prevent negative balance
    }
    
    /**
     * Add Bits to the player's balance
     * @param amount The amount to add
     * @return The new balance
     */
    public int addBits(int amount) {
        if (amount > 0) {
            this.bits += amount;
        }
        return this.bits;
    }
    
    /**
     * Remove Bits from the player's balance
     * @param amount The amount to remove
     * @return True if the player had enough Bits and they were removed, false otherwise
     */
    public boolean removeBits(int amount) {
        if (amount <= 0) return true;
        
        if (this.bits >= amount) {
            this.bits -= amount;
            return true;
        }
        return false;
    }
}