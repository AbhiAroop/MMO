package com.server.profiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.items.ItemType;
import com.server.profiles.skills.data.PlayerSkillData;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.stats.PlayerStats;

public class PlayerProfile {
    private final UUID playerUUID;
    private final int slot;
    private String name;
    private PlayerStats stats;
    private long created;
    private long lastPlayed;
    private ItemStack[] inventoryContents;
    private ItemStack[] armorContents;
    private ItemStack[] extraContents; // For off-hand items
    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    private PlayerSkillData skillData;
    private PlayerSkillTreeData skillTreeData;

    // Economy currencies
    private int units;          // Basic currency for trading and shops
    private int premiumUnits;   // Rare currency for special items
    private int essence;        // Progression currency, non-tradeable
    private int bits;           // Premium currency from store, for cosmetics

    private final Map<ItemType, ItemStack> cosmetics = new HashMap<>();
    private final Set<String> unlockedAbilities = new HashSet<>();
    private final Set<String> enabledAbilities = new HashSet<>();

    public PlayerProfile(UUID playerUUID, int slot, String name) {
        this.playerUUID = playerUUID;
        this.slot = slot;
        this.name = name;
        this.stats = new PlayerStats();
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

        this.skillData = new PlayerSkillData();
        this.skillTreeData = new PlayerSkillTreeData();
        
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
        
        // IMPORTANT: Set health BEFORE teleporting to avoid health reset issues
        // First handle the max health attribute to ensure the player can hold the stored health value
        AttributeInstance maxHealthAttr;
        maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            // Remove any existing modifiers for clean state
            for (AttributeModifier mod : new HashSet<>(maxHealthAttr.getModifiers())) {
                maxHealthAttr.removeModifier(mod);
            }
            
            // Set base value to vanilla default
            maxHealthAttr.setBaseValue(20.0);
            
            // Apply health bonus from profile stats
            double healthBonus = stats.getHealth() - 20.0;
            if (healthBonus > 0) {
                AttributeModifier healthMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.max_health",
                    healthBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                maxHealthAttr.addModifier(healthMod);
            }
            
            // Now set current health to the saved value (capped by max health)
            double healthToSet = Math.min(stats.getCurrentHealth(), maxHealthAttr.getValue());
            player.setHealth(healthToSet);
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Profile load: set " + player.getName() + "'s health to " + 
                                healthToSet + "/" + maxHealthAttr.getValue());
            }
        }
        
        // Now teleport the player after health is set
        teleportPlayer(player);
        
        // Apply health display scale
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
        
        updateLastPlayed();
    }

    public void saveProfile(Player player) {
        saveInventory(player);
        saveLocation(player);
        
        // Important: Update the profile with current values before saving
        stats.updateFromPlayer(player);
        
        // CRITICAL: Always explicitly save current health value
        double currentHealth = player.getHealth();
        stats.setCurrentHealth(currentHealth);
        
        updateLastPlayed();
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Profile saved for " + player.getName() + 
                                    " with health: " + currentHealth + "/" + 
                                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        }
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public int getSlot() { return slot; }
    public String getName() { return name; }
    public PlayerStats getStats() { return stats; }
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

    public PlayerSkillData getSkillData() {
        return skillData;
    }
    
    /**
     * Get the player's skill tree data
     */
    public PlayerSkillTreeData getSkillTreeData() {
        return skillTreeData;
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

    /**
     * Check if an ability is unlocked
     */
    public boolean hasUnlockedAbility(String abilityId) {
        return unlockedAbilities.contains(abilityId);
    }

    /**
     * Unlock an ability
     */
    public void unlockAbility(String abilityId) {
        unlockedAbilities.add(abilityId);
        // By default, passive abilities are enabled when unlocked
        enabledAbilities.add(abilityId);
    }

    /**
     * Lock an ability
     */
    public void lockAbility(String abilityId) {
        unlockedAbilities.remove(abilityId);
        enabledAbilities.remove(abilityId);
    }

    /**
     * Check if an ability is enabled
     */
    public boolean isAbilityEnabled(String abilityId) {
        return enabledAbilities.contains(abilityId);
    }

    /**
     * Set whether an ability is enabled
     */
    public void setAbilityEnabled(String abilityId, boolean enabled) {
        if (enabled) {
            enabledAbilities.add(abilityId);
        } else {
            enabledAbilities.remove(abilityId);
        }
    }

    /**
     * Get all unlocked abilities
     */
    public Set<String> getUnlockedAbilities() {
        return new HashSet<>(unlockedAbilities);
    }

    /**
     * Get all enabled abilities
     */
    public Set<String> getEnabledAbilities() {
        return new HashSet<>(enabledAbilities);
    }
}