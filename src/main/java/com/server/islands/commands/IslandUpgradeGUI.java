package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;
import com.server.islands.managers.IslandUpgradeManager;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for upgrading islands.
 * Shows all 5 upgrade types with current levels and costs.
 */
public class IslandUpgradeGUI {
    
    private static final String GUI_TITLE = "§6§l⚒ §e§lIsland Upgrades §6§l⚒";
    
    /**
     * Opens the island upgrade GUI for a player
     */
    public static void open(Player player, PlayerIsland island, IslandManager islandManager) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        IslandUpgradeManager upgradeManager = islandManager.getUpgradeManager();
        
        // Get player's currency
        PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(player.getUniqueId());
        int playerUnits = (profile != null) ? profile.getUnits() : 0;
        
        // Size Upgrade (slot 11)
        gui.setItem(11, createSizeUpgradeItem(island, playerUnits));
        
        // Player Limit Upgrade (slot 13)
        gui.setItem(13, createPlayerLimitUpgradeItem(island, playerUnits));
        
        // Redstone Limit Upgrade (slot 15)
        gui.setItem(15, createRedstoneLimitUpgradeItem(island, playerUnits));
        
        // Crop Growth Upgrade (slot 29)
        gui.setItem(29, createCropGrowthUpgradeItem(island, playerUnits));
        
        // Weather Control (slot 31)
        gui.setItem(31, createWeatherControlItem(island, playerUnits));
        
        // Currency Info (slot 49)
        gui.setItem(49, createCurrencyInfoItem(playerUnits));
        
        // Fill empty slots
        fillEmptySlots(gui);
        
        player.openInventory(gui);
    }
    
    private static ItemStack createSizeUpgradeItem(PlayerIsland island, int playerUnits) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getSizeLevel();
        int currentSize = island.getCurrentSize();
        int nextCost = island.getSizeUpgradeCost();
        
        meta.displayName(Component.text("🗺 Island Size", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Size: ", NamedTextColor.GRAY)
            .append(Component.text(currentSize + "x" + currentSize, NamedTextColor.WHITE, TextDecoration.BOLD)));
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/7", NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < 7) {
            int nextSize = getNextSize(currentLevel);
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextSize + "x" + nextSize, NamedTextColor.WHITE)));
            lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text(formatNumber(nextCost) + " Units", NamedTextColor.GOLD)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createPlayerLimitUpgradeItem(PlayerIsland island, int playerUnits) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getPlayerLimitLevel();
        int currentLimit = island.getCurrentPlayerLimit();
        int nextCost = island.getPlayerLimitUpgradeCost();
        
        meta.displayName(Component.text("👥 Player Limit", NamedTextColor.AQUA, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Limit: ", NamedTextColor.GRAY)
            .append(Component.text(currentLimit + " players", NamedTextColor.WHITE, TextDecoration.BOLD)));
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/5", NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < 5) {
            int nextLimit = getNextPlayerLimit(currentLevel);
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextLimit + " players", NamedTextColor.WHITE)));
            lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text(formatNumber(nextCost) + " Units", NamedTextColor.GOLD)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createRedstoneLimitUpgradeItem(PlayerIsland island, int playerUnits) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getRedstoneLimitLevel();
        int currentLimit = island.getCurrentRedstoneLimit();
        int nextCost = island.getRedstoneLimitUpgradeCost();
        
        meta.displayName(Component.text("⚡ Redstone Devices", NamedTextColor.RED, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if (currentLimit == -1) {
            lore.add(Component.text("Current Limit: ", NamedTextColor.GRAY)
                .append(Component.text("UNLIMITED", NamedTextColor.GREEN, TextDecoration.BOLD)));
        } else {
            lore.add(Component.text("Current Limit: ", NamedTextColor.GRAY)
                .append(Component.text(currentLimit + " devices", NamedTextColor.WHITE, TextDecoration.BOLD)));
        }
        
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/5", NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < 5) {
            int nextLimit = getNextRedstoneLimit(currentLevel);
            String nextLimitText = (nextLimit == -1) ? "UNLIMITED" : (nextLimit + " devices");
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextLimitText, NamedTextColor.WHITE)));
            lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text(formatNumber(nextCost) + " Units", NamedTextColor.GOLD)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createCropGrowthUpgradeItem(PlayerIsland island, int playerUnits) {
        ItemStack item = new ItemStack(Material.WHEAT);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getCropGrowthLevel();
        double currentMultiplier = island.getCropGrowthMultiplier();
        int nextCost = island.getCropGrowthUpgradeCost();
        
        meta.displayName(Component.text("🌾 Crop Growth Speed", NamedTextColor.GREEN, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Speed: ", NamedTextColor.GRAY)
            .append(Component.text(currentMultiplier + "x", NamedTextColor.WHITE, TextDecoration.BOLD)));
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/4", NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < 4) {
            double nextMultiplier = getNextCropMultiplier(currentLevel);
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextMultiplier + "x", NamedTextColor.WHITE)));
            lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text(formatNumber(nextCost) + " Units", NamedTextColor.GOLD)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createWeatherControlItem(PlayerIsland island, int playerUnits) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        
        boolean hasWeatherControl = island.hasWeatherControl();
        
        meta.displayName(Component.text("☀ Weather Control", NamedTextColor.YELLOW, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if (hasWeatherControl) {
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("ENABLED", NamedTextColor.GREEN, TextDecoration.BOLD)));
            lore.add(Component.empty());
            lore.add(Component.text("You can control the weather", NamedTextColor.GRAY));
            lore.add(Component.text("on your island!", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
            lore.add(Component.empty());
            lore.add(Component.text("Purchase to control weather", NamedTextColor.GRAY));
            lore.add(Component.text("on your island!", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
                .append(Component.text("50,000 Units", NamedTextColor.GOLD)));
            lore.add(Component.empty());
            
            if (playerUnits >= 50000) {
                lore.add(Component.text("✓ Click to purchase!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            }
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createCurrencyInfoItem(int playerUnits) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("💰 Your Balance", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Units: ", NamedTextColor.GRAY)
            .append(Component.text(formatNumber(playerUnits), NamedTextColor.YELLOW, TextDecoration.BOLD)));
        lore.add(Component.empty());
        lore.add(Component.text("Click an upgrade above to", NamedTextColor.GRAY));
        lore.add(Component.text("improve your island!", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Handles clicking on an upgrade
     */
    public static void handleUpgradeClick(Player player, PlayerIsland island, int slot, IslandManager islandManager) {
        IslandUpgradeManager upgradeManager = islandManager.getUpgradeManager();
        
        // Determine which upgrade to perform
        switch (slot) {
            case 11: // Size upgrade
                upgradeManager.upgradeSizeLevel(player).thenAccept(result -> {
                    handleUpgradeResult(player, island, result, islandManager);
                });
                break;
            case 13: // Player limit upgrade
                upgradeManager.upgradePlayerLimit(player).thenAccept(result -> {
                    handleUpgradeResult(player, island, result, islandManager);
                });
                break;
            case 15: // Redstone limit upgrade
                upgradeManager.upgradeRedstoneLimit(player).thenAccept(result -> {
                    handleUpgradeResult(player, island, result, islandManager);
                });
                break;
            case 29: // Crop growth upgrade
                upgradeManager.upgradeCropGrowth(player).thenAccept(result -> {
                    handleUpgradeResult(player, island, result, islandManager);
                });
                break;
            case 31: // Weather control
                upgradeManager.enableWeatherControl(player).thenAccept(result -> {
                    handleUpgradeResult(player, island, result, islandManager);
                });
                break;
            default:
                return; // Not an upgrade slot
        }
    }
    
    /**
     * Handles the result of an upgrade operation.
     */
    private static void handleUpgradeResult(Player player, PlayerIsland island, IslandUpgradeManager.UpgradeResult result, IslandManager islandManager) {
        if (result.isSuccess()) {
            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(result.getMessage(), NamedTextColor.GREEN)));
            // Reopen GUI with updated values
            open(player, island, islandManager);
        } else {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(result.getMessage(), NamedTextColor.RED)));
        }
    }
    
    // Helper methods for calculating next values
    private static int getNextSize(int currentLevel) {
        switch (currentLevel) {
            case 0: return 50;
            case 1: return 100;
            case 2: return 200;
            case 3: return 300;
            case 4: return 400;
            case 5: return 500;
            default: return 500;
        }
    }
    
    private static int getNextPlayerLimit(int currentLevel) {
        switch (currentLevel) {
            case 0: return 5;
            case 1: return 10;
            case 2: return 20;
            case 3: return 50;
            default: return 50;
        }
    }
    
    private static int getNextRedstoneLimit(int currentLevel) {
        switch (currentLevel) {
            case 0: return 100;
            case 1: return 200;
            case 2: return 500;
            case 3: return -1; // Unlimited
            default: return -1;
        }
    }
    
    private static double getNextCropMultiplier(int currentLevel) {
        switch (currentLevel) {
            case 0: return 1.5;
            case 1: return 2.0;
            case 2: return 3.0;
            default: return 3.0;
        }
    }
    
    private static String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
