package com.server.islands.managers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.server.islands.data.PlayerIsland;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Manages island upgrades (size, player limit, redstone, crop growth).
 */
public class IslandUpgradeManager {
    
    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    
    public IslandUpgradeManager(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }
    
    /**
     * Calculate island token cost for an upgrade based on level
     * Must match the costs shown in IslandUpgradeGUI
     */
    private static int calculateTokenCost(String upgradeType, int currentLevel) {
        switch (upgradeType.toLowerCase()) {
            case "size":
                if (currentLevel < 3) return 1;
                if (currentLevel < 5) return 2;
                if (currentLevel < 10) return 3;
                if (currentLevel < 15) return 5;
                if (currentLevel < 25) return 8;
                if (currentLevel < 40) return 10;
                if (currentLevel < 60) return 15;
                if (currentLevel < 80) return 20;
                if (currentLevel < 120) return 30;
                if (currentLevel < 180) return 40;
                return 50;
            case "player_limit":
                return 5 + (currentLevel * 5);
            case "redstone":
                return 3 + (currentLevel * 5);
            case "crop_growth":
                return 5 + (currentLevel * 7);
            case "weather":
                return 50;
            default:
                return 5;
        }
    }
    
    /**
     * Upgrades the island size by 2 blocks (1 in each direction).
     * Cost: Units + Island Tokens (based on level)
     * Max size: 500x500
     */
    public CompletableFuture<UpgradeResult> upgradeSizeLevel(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level (500x500)
            if (island.getSizeLevel() >= island.getMaxSizeLevel()) {
                return new UpgradeResult(false, "Your island is already at maximum size (500x500)!");
            }
            
            int unitCost = island.getSizeUpgradeCost();
            int tokenCost = calculateTokenCost("size", island.getSizeLevel());
            
            // Check if player can afford units
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < unitCost) {
                return new UpgradeResult(false, "You need " + unitCost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Check if island has enough tokens
            if (island.getIslandTokens() < tokenCost) {
                return new UpgradeResult(false, "You need " + tokenCost + " island tokens to upgrade! (You have " + 
                    island.getIslandTokens() + ")");
            }
            
            // Deduct costs
            profile.removeUnits(unitCost);
            island.removeIslandTokens(tokenCost);
            
            // Upgrade level
            int oldSize = island.getCurrentSize();
            island.setSizeLevel(island.getSizeLevel() + 1);
            int newSize = island.getCurrentSize();
            
            // Update world border
            islandManager.getWorldManager().updateWorldBorder(island, newSize);
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "Island size upgraded from " + oldSize + "x" + oldSize + 
                " to " + newSize + "x" + newSize + "! (" + island.getSizeLevel() + "/" + 
                island.getMaxSizeLevel() + ") [-" + unitCost + " units, -" + tokenCost + " tokens]");
        });
    }
    
    /**
     * Upgrades the player limit.
     * Cost: Units + Island Tokens (based on level)
     */
    public CompletableFuture<UpgradeResult> upgradePlayerLimit(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level
            if (island.getPlayerLimitLevel() >= 5) {
                return new UpgradeResult(false, "Your player limit is already at maximum!");
            }
            
            int unitCost = island.getPlayerLimitUpgradeCost();
            int tokenCost = calculateTokenCost("player_limit", island.getPlayerLimitLevel());
            
            // Check if player can afford units
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < unitCost) {
                return new UpgradeResult(false, "You need " + unitCost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Check if island has enough tokens
            if (island.getIslandTokens() < tokenCost) {
                return new UpgradeResult(false, "You need " + tokenCost + " island tokens to upgrade! (You have " + 
                    island.getIslandTokens() + ")");
            }
            
            // Deduct costs
            profile.removeUnits(unitCost);
            island.removeIslandTokens(tokenCost);
            
            // Upgrade level
            int oldLimit = island.getCurrentPlayerLimit();
            island.setPlayerLimitLevel(island.getPlayerLimitLevel() + 1);
            int newLimit = island.getCurrentPlayerLimit();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aPlayer limit upgraded from " + oldLimit + 
                " to " + newLimit + " players! [-" + unitCost + " units, -" + tokenCost + " tokens]");
        });
    }
    
    /**
     * Upgrades the redstone limit.
     * Cost: Units + Island Tokens (based on level)
     * Starts at 10, increases by 5 per level, max 100 at level 19
     */
    public CompletableFuture<UpgradeResult> upgradeRedstoneLimit(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level
            if (island.getRedstoneLimitLevel() >= island.getMaxRedstoneLevel()) {
                return new UpgradeResult(false, "Your redstone limit is already at maximum (100 items)!");
            }
            
            int unitCost = island.getRedstoneLimitUpgradeCost();
            int tokenCost = calculateTokenCost("redstone", island.getRedstoneLimitLevel());
            
            // Check if player can afford units
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < unitCost) {
                return new UpgradeResult(false, "You need " + unitCost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Check if island has enough tokens
            if (island.getIslandTokens() < tokenCost) {
                return new UpgradeResult(false, "You need " + tokenCost + " island tokens to upgrade! (You have " + 
                    island.getIslandTokens() + ")");
            }
            
            // Deduct costs
            profile.removeUnits(unitCost);
            island.removeIslandTokens(tokenCost);
            
            // Upgrade level
            int oldLimit = island.getCurrentRedstoneLimit();
            island.setRedstoneLimitLevel(island.getRedstoneLimitLevel() + 1);
            int newLimit = island.getCurrentRedstoneLimit();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "Redstone limit upgraded from " + oldLimit + 
                " to " + newLimit + " items! (" + island.getRedstoneLimitLevel() + "/" + 
                island.getMaxRedstoneLevel() + ") [-" + unitCost + " units, -" + tokenCost + " tokens]");
        });
    }
    
    /**
     * Upgrades the crop growth speed.
     * Cost: Units + Island Tokens (based on level)
     */
    public CompletableFuture<UpgradeResult> upgradeCropGrowth(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level
            if (island.getCropGrowthLevel() >= 4) {
                return new UpgradeResult(false, "Your crop growth is already at maximum speed!");
            }
            
            int unitCost = island.getCropGrowthUpgradeCost();
            int tokenCost = calculateTokenCost("crop_growth", island.getCropGrowthLevel());
            
            // Check if player can afford units
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < unitCost) {
                return new UpgradeResult(false, "You need " + unitCost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Check if island has enough tokens
            if (island.getIslandTokens() < tokenCost) {
                return new UpgradeResult(false, "You need " + tokenCost + " island tokens to upgrade! (You have " + 
                    island.getIslandTokens() + ")");
            }
            
            // Deduct costs
            profile.removeUnits(unitCost);
            island.removeIslandTokens(tokenCost);
            
            // Upgrade level
            double oldMultiplier = island.getCropGrowthMultiplier();
            island.setCropGrowthLevel(island.getCropGrowthLevel() + 1);
            double newMultiplier = island.getCropGrowthMultiplier();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aCrop growth upgraded from " + oldMultiplier + 
                "x to " + newMultiplier + "x speed! [-" + unitCost + " units, -" + tokenCost + " tokens]");
        });
    }
    
    /**
     * Toggles weather control for an island (one-time purchase).
     * Cost: 50000 Units + 50 Island Tokens
     */
    public CompletableFuture<UpgradeResult> enableWeatherControl(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already enabled
            if (island.hasWeatherControl()) {
                return new UpgradeResult(false, "You already have weather control!");
            }
            
            int unitCost = 50000; // Fixed cost for weather control
            int tokenCost = calculateTokenCost("weather", 0); // Level doesn't matter, it's always 50
            
            // Check if player can afford units
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < unitCost) {
                return new UpgradeResult(false, "You need " + unitCost + " units to enable weather control! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Check if island has enough tokens
            if (island.getIslandTokens() < tokenCost) {
                return new UpgradeResult(false, "You need " + tokenCost + " island tokens to enable weather control! (You have " + 
                    island.getIslandTokens() + ")");
            }
            
            // Deduct costs
            profile.removeUnits(unitCost);
            island.removeIslandTokens(tokenCost);
            
            // Enable weather control
            island.setWeatherControl(true);
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aWeather control enabled! Use /island weather to control your island's weather. [-" + 
                unitCost + " units, -" + tokenCost + " tokens]");
        });
    }
    
    /**
     * Result of an upgrade operation.
     */
    public static class UpgradeResult {
        private final boolean success;
        private final String message;
        
        public UpgradeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
