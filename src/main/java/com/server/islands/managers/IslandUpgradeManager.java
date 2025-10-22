package com.server.islands.managers;

import com.server.islands.data.PlayerIsland;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
     * Upgrades the island size.
     */
    public CompletableFuture<UpgradeResult> upgradeSizeLevel(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level
            if (island.getSizeLevel() >= 7) {
                return new UpgradeResult(false, "Your island is already at maximum size!");
            }
            
            int cost = island.getSizeUpgradeCost();
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < cost) {
                return new UpgradeResult(false, "You need " + cost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Deduct cost
            profile.removeUnits(cost);
            
            // Upgrade level
            int oldSize = island.getCurrentSize();
            island.setSizeLevel(island.getSizeLevel() + 1);
            int newSize = island.getCurrentSize();
            
            // Update world border
            islandManager.getWorldManager().updateWorldBorder(island, newSize);
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aIsland size upgraded from " + oldSize + "x" + oldSize + 
                " to " + newSize + "x" + newSize + "!");
        });
    }
    
    /**
     * Upgrades the player limit.
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
            
            int cost = island.getPlayerLimitUpgradeCost();
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < cost) {
                return new UpgradeResult(false, "You need " + cost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Deduct cost
            profile.removeUnits(cost);
            
            // Upgrade level
            int oldLimit = island.getCurrentPlayerLimit();
            island.setPlayerLimitLevel(island.getPlayerLimitLevel() + 1);
            int newLimit = island.getCurrentPlayerLimit();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aPlayer limit upgraded from " + oldLimit + 
                " to " + newLimit + " players!");
        });
    }
    
    /**
     * Upgrades the redstone limit.
     */
    public CompletableFuture<UpgradeResult> upgradeRedstoneLimit(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = player.getUniqueId();
            PlayerIsland island = islandManager.getIslandByOwner(playerUuid).join();
            
            if (island == null) {
                return new UpgradeResult(false, "You don't have an island!");
            }
            
            // Check if already max level
            if (island.getRedstoneLimitLevel() >= 5) {
                return new UpgradeResult(false, "Your redstone limit is already at maximum (unlimited)!");
            }
            
            int cost = island.getRedstoneLimitUpgradeCost();
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < cost) {
                return new UpgradeResult(false, "You need " + cost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Deduct cost
            profile.removeUnits(cost);
            
            // Upgrade level
            int oldLimit = island.getCurrentRedstoneLimit();
            island.setRedstoneLimitLevel(island.getRedstoneLimitLevel() + 1);
            int newLimit = island.getCurrentRedstoneLimit();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            String newLimitStr = newLimit == -1 ? "unlimited" : String.valueOf(newLimit);
            return new UpgradeResult(true, "§aRedstone limit upgraded from " + oldLimit + 
                " to " + newLimitStr + " devices!");
        });
    }
    
    /**
     * Upgrades the crop growth speed.
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
            
            int cost = island.getCropGrowthUpgradeCost();
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < cost) {
                return new UpgradeResult(false, "You need " + cost + " units to upgrade! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Deduct cost
            profile.removeUnits(cost);
            
            // Upgrade level
            double oldMultiplier = island.getCropGrowthMultiplier();
            island.setCropGrowthLevel(island.getCropGrowthLevel() + 1);
            double newMultiplier = island.getCropGrowthMultiplier();
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aCrop growth upgraded from " + oldMultiplier + 
                "x to " + newMultiplier + "x speed!");
        });
    }
    
    /**
     * Toggles weather control for an island (one-time purchase).
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
            
            int cost = 50000; // Fixed cost for weather control
            
            // Check if player can afford it
            PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(playerUuid);
            if (profile == null || profile.getUnits() < cost) {
                return new UpgradeResult(false, "You need " + cost + " units to enable weather control! (You have " + 
                    (profile != null ? profile.getUnits() : 0) + ")");
            }
            
            // Deduct cost
            profile.removeUnits(cost);
            
            // Enable weather control
            island.setWeatherControl(true);
            
            // Save island
            islandManager.getDataManager().saveIsland(island).join();
            
            return new UpgradeResult(true, "§aWeather control enabled! Use /island weather to control your island's weather.");
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
