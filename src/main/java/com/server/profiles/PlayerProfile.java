package com.server.profiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
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

    // Profile Level System
    private int profileLevel;           // Current profile level
    private double profileCurrentXp;   // Current XP towards next profile level
    private double profileTotalXp;     // Total XP earned for this profile
    private static final int MAX_PROFILE_LEVEL = 100; // Maximum profile level

    private long totalPlaytimeMillis;    // Total time spent on this profile
    private long sessionStartTime;       // When current session started (0 if not active)

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

        // Initialize profile level system
        this.profileLevel = 1;      // Start at level 1
        this.profileCurrentXp = 0;  // No XP towards next level
        this.profileTotalXp = 0;    // No total XP earned

        // Initialize playtime tracking
        this.totalPlaytimeMillis = 0;
        this.sessionStartTime = 0;

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
        startPlaytimeSession();
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
        endPlaytimeSession();
        
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

    /**
     * Start tracking playtime for this session
     */
    public void startPlaytimeSession() {
        if (sessionStartTime == 0) {
            sessionStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Stop tracking playtime and add session time to total
     */
    public void endPlaytimeSession() {
        if (sessionStartTime > 0) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            totalPlaytimeMillis += sessionDuration;
            sessionStartTime = 0;
        }
    }
    
    /**
     * Get total playtime in milliseconds (including current session if active)
     */
    public long getTotalPlaytimeMillis() {
        long totalTime = totalPlaytimeMillis;
        
        // Add current session time if active
        if (sessionStartTime > 0) {
            totalTime += (System.currentTimeMillis() - sessionStartTime);
        }
        
        return totalTime;
    }
    
    /**
     * Get formatted playtime string (e.g., "2d 5h 30m")
     */
    public String getFormattedPlaytime() {
        return formatPlaytime(getTotalPlaytimeMillis());
    }
    
    /**
     * Format milliseconds into a readable playtime string
     */
    public static String formatPlaytime(long millis) {
        if (millis <= 0) return "0m";
        
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        
        StringBuilder formatted = new StringBuilder();
        
        if (days > 0) {
            formatted.append(days).append("d ");
        }
        if (hours > 0) {
            formatted.append(hours).append("h ");
        }
        if (minutes > 0 || formatted.length() == 0) {
            formatted.append(minutes).append("m");
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Get creation date as formatted string
     */
    public String getFormattedCreationDate() {
        return new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(created));
    }
    
    /**
     * Check if this profile is currently active (has an active session)
     */
    public boolean isActiveSession() {
        return sessionStartTime > 0;
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

    // =============================================================================
    // PROFILE LEVEL SYSTEM METHODS
    // =============================================================================

    /**
     * Get the current profile level
     * @return The current profile level (1-100)
     */
    public int getProfileLevel() {
        return profileLevel;
    }

    /**
     * Get the current XP towards the next profile level
     * @return The current XP progress
     */
    public double getProfileCurrentXp() {
        return profileCurrentXp;
    }

    /**
     * Get the total XP earned for this profile
     * @return The total XP earned
     */
    public double getProfileTotalXp() {
        return profileTotalXp;
    }

    /**
     * Get the maximum profile level
     * @return The maximum profile level
     */
    public static int getMaxProfileLevel() {
        return MAX_PROFILE_LEVEL;
    }

    /**
     * Calculate XP required for a specific profile level
     * Uses a scaled formula that starts at 10 XP for level 2 and reaches 1000 XP for level 100
     * @param level The level to calculate XP for
     * @return The XP required for that level
     */
    public static double getXpForProfileLevel(int level) {
        if (level <= 1) return 0; // Level 1 requires no XP
        
        // Use a formula that scales from 10 XP (level 2) to 1000 XP (level 100)
        // Formula: 10 + (level - 2) * (990 / 98)
        // This creates a linear progression from level 2 to 100
        double baseXp = 10.0;
        double maxXp = 1000.0;
        double levelRange = 98.0; // From level 2 to level 100
        
        double xpIncrease = (maxXp - baseXp) / levelRange;
        return baseXp + ((level - 2) * xpIncrease);
    }

    /**
     * Get XP required for the next profile level
     * @return XP needed for next level, or 0 if already max level
     */
    public double getXpForNextProfileLevel() {
        if (profileLevel >= MAX_PROFILE_LEVEL) {
            return 0; // Already at max level
        }
        return getXpForProfileLevel(profileLevel + 1);
    }

    /**
     * Get the progress towards the next profile level as a percentage (0.0 to 1.0)
     * @return Progress percentage
     */
    public double getProfileLevelProgress() {
        if (profileLevel >= MAX_PROFILE_LEVEL) {
            return 1.0; // 100% if at max level
        }
        
        double xpForNext = getXpForNextProfileLevel();
        if (xpForNext <= 0) return 1.0;
        
        return Math.min(1.0, profileCurrentXp / xpForNext);
    }

    /**
     * Add experience to the profile level
     * @param amount The amount of XP to add
     * @return True if the profile leveled up, false otherwise
     */
    public boolean addProfileExperience(double amount) {
        if (amount <= 0 || profileLevel >= MAX_PROFILE_LEVEL) {
            return false; // No XP to add or already max level
        }

        // Add to total XP
        profileTotalXp += amount;
        profileCurrentXp += amount;

        boolean leveledUp = false;
        
        // Check for level ups (can level up multiple times in one go)
        while (profileLevel < MAX_PROFILE_LEVEL) {
            double xpForNext = getXpForNextProfileLevel();
            
            if (profileCurrentXp >= xpForNext) {
                // Level up!
                profileLevel++;
                profileCurrentXp -= xpForNext;
                leveledUp = true;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.PROFILE)) {
                    Main.getInstance().debugLog(DebugSystem.PROFILE, 
                        "Profile " + name + " leveled up to " + profileLevel);
                }
            } else {
                break; // No more level ups possible
            }
        }

        // Cap current XP if at max level
        if (profileLevel >= MAX_PROFILE_LEVEL) {
            profileCurrentXp = 0;
        }

        return leveledUp;
    }

    /**
     * Remove experience from the profile level
     * Note: This will not reduce the profile level, only the current XP progress
     * @param amount The amount of XP to remove
     * @return The actual amount removed
     */
    public double removeProfileExperience(double amount) {
        if (amount <= 0) {
            return 0;
        }

        double actualRemoved = Math.min(amount, profileCurrentXp);
        profileCurrentXp -= actualRemoved;
        profileTotalXp -= actualRemoved;

        // Ensure values don't go negative
        profileCurrentXp = Math.max(0, profileCurrentXp);
        profileTotalXp = Math.max(0, profileTotalXp);

        return actualRemoved;
    }

    /**
     * Set the profile level directly (for admin commands)
     * @param level The level to set (1-100)
     * @param resetXp Whether to reset current XP to 0
     * @return True if the level was changed, false otherwise
     */
    public boolean setProfileLevel(int level, boolean resetXp) {
        if (level < 1 || level > MAX_PROFILE_LEVEL) {
            return false;
        }

        int oldLevel = this.profileLevel;
        this.profileLevel = level;

        if (resetXp) {
            this.profileCurrentXp = 0;
        }

        // Recalculate total XP based on new level
        if (level > 1) {
            double totalXpForLevel = 0;
            for (int i = 2; i <= level; i++) {
                totalXpForLevel += getXpForProfileLevel(i);
            }
            this.profileTotalXp = totalXpForLevel + this.profileCurrentXp;
        } else {
            this.profileTotalXp = this.profileCurrentXp;
        }

        if (Main.getInstance().isDebugEnabled(DebugSystem.PROFILE)) {
            Main.getInstance().debugLog(DebugSystem.PROFILE, 
                "Profile " + name + " level changed from " + oldLevel + " to " + level);
        }

        return true;
    }

    /**
     * Add profile experience and handle level up notifications
     * This is the main method that should be called when granting profile XP
     * @param player The player to grant XP to (for notifications)
     * @param amount The amount of XP to add
     * @param source A description of what granted the XP (for logging/notifications)
     * @return True if leveled up, false otherwise
     */
    public boolean addProfileExperienceWithNotification(Player player, double amount, String source) {
        if (amount <= 0 || profileLevel >= MAX_PROFILE_LEVEL) {
            return false;
        }

        int oldLevel = profileLevel;
        boolean leveledUp = addProfileExperience(amount);

        // Log the XP gain
        if (Main.getInstance().isDebugEnabled(DebugSystem.PROFILE)) {
            Main.getInstance().debugLog(DebugSystem.PROFILE, 
                "Added " + amount + " profile XP to " + player.getName() + " from " + source);
        }

        // Handle level up notifications
        if (leveledUp && player != null && player.isOnline()) {
            int newLevel = profileLevel;
            
            // Send level up message
            player.sendMessage(ChatColor.GREEN + "✦ " + ChatColor.GOLD + "PROFILE LEVEL UP" + 
                              ChatColor.GREEN + " ✦ " + ChatColor.YELLOW + "Profile Level " + 
                              ChatColor.GREEN + oldLevel + " → " + ChatColor.YELLOW + newLevel);

            // Play level up sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

            // Show title
            player.sendTitle(
                ChatColor.GOLD + "PROFILE LEVEL UP!",
                ChatColor.YELLOW + "Level " + newLevel,
                10, 70, 20
            );

            // Log level up
            Main.getInstance().getLogger().info("Profile " + name + " (" + player.getName() + 
                                              ") leveled up to " + newLevel + " from " + source);
        }

        return leveledUp;
    }

    /**
     * Get a formatted string showing profile level progress
     * @return Formatted string like "Level 15 (450/2250 XP)"
     */
    public String getFormattedProfileProgress() {
        if (profileLevel >= MAX_PROFILE_LEVEL) {
            return "Level " + profileLevel + " (MAX)";
        }
        
        return "Level " + profileLevel + " (" + 
               String.format("%.0f", profileCurrentXp) + "/" + 
               String.format("%.0f", getXpForNextProfileLevel()) + " XP)";
    }

    /**
     * Check if the profile is at maximum level
     * @return True if at max level, false otherwise
     */
    public boolean isMaxProfileLevel() {
        return profileLevel >= MAX_PROFILE_LEVEL;
    }

    
}