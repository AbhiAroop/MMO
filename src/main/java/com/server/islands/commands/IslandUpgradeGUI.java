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
        int islandTokens = island.getIslandTokens();
        
        // Add cyan borders first
        addBorders(gui);
        
        // Size Upgrade (slot 11)
        gui.setItem(11, createSizeUpgradeItem(island, playerUnits, islandTokens));
        
        // Player Limit Upgrade (slot 13)
        gui.setItem(13, createPlayerLimitUpgradeItem(island, playerUnits, islandTokens));
        
        // Redstone Limit Upgrade (slot 15)
        gui.setItem(15, createRedstoneLimitUpgradeItem(island, playerUnits, islandTokens));
        
        // Crop Growth Upgrade (slot 29)
        gui.setItem(29, createCropGrowthUpgradeItem(island, playerUnits, islandTokens));
        
        // Weather Control (slot 31)
        gui.setItem(31, createWeatherControlItem(island, playerUnits, islandTokens));
        
        // Currency Info (slot 4)
        gui.setItem(4, createCurrencyInfoItem(playerUnits, islandTokens));
        
        // Back button (slot 49)
        gui.setItem(49, createBackButton());
        
        // Fill empty slots
        fillEmptySlots(gui);
        
        player.openInventory(gui);
    }
    
    /**
     * Calculate island token cost for an upgrade based on level
     * Tokens scale with level to match challenge rewards progression
     * Lower costs for early game to encourage progression
     */
    private static int calculateTokenCost(String upgradeType, int currentLevel) {
        switch (upgradeType.toLowerCase()) {
            case "size":
                // Size: 1-1-2-3-5-8-10-15-20-30-40-50+ tokens (progressive, low early game)
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
                // Player limit: 5-10-20-30-50 tokens (5 levels, more affordable)
                return 5 + (currentLevel * 5);
            case "redstone":
                // Redstone: 3-8-15-25-40 tokens (5 levels, affordable early)
                return 3 + (currentLevel * 5);
            case "crop_growth":
                // Crop growth: 5-12-20-30-45 tokens (5 levels)
                return 5 + (currentLevel * 7);
            case "weather":
                // Weather control: 50 tokens (unlock, reduced from 100)
                return 50;
            default:
                return 5;
        }
    }
    
    private static ItemStack createSizeUpgradeItem(PlayerIsland island, int playerUnits, int islandTokens) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getSizeLevel();
        int currentSize = island.getCurrentSize();
        int maxLevel = island.getMaxSizeLevel();
        int nextUnitCost = island.getSizeUpgradeCost();
        int nextTokenCost = calculateTokenCost("size", currentLevel);
        
        meta.displayName(Component.text("🗺 Island Size", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Size: ", NamedTextColor.GRAY)
            .append(Component.text(currentSize + "x" + currentSize, NamedTextColor.WHITE, TextDecoration.BOLD)));
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/" + maxLevel, NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < maxLevel) {
            int nextSize = island.getNextSize();
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextSize + "x" + nextSize, NamedTextColor.WHITE))
                .append(Component.text(" (+2 blocks)", NamedTextColor.GRAY)));
            lore.add(Component.empty());
            lore.add(Component.text("━━━ Cost ━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
            lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
                .append(Component.text(formatNumber(nextUnitCost) + " Units", 
                    playerUnits >= nextUnitCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
                .append(Component.text(nextTokenCost + " Island Tokens", 
                    islandTokens >= nextTokenCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            lore.add(Component.text("Each upgrade adds 1 block", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("in each direction (2 total).", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            
            if (playerUnits >= nextUnitCost && islandTokens >= nextTokenCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                if (playerUnits < nextUnitCost) {
                    lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED));
                }
                if (islandTokens < nextTokenCost) {
                    lore.add(Component.text("✗ Not enough tokens!", NamedTextColor.RED));
                }
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL (500x500)", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createPlayerLimitUpgradeItem(PlayerIsland island, int playerUnits, int islandTokens) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getPlayerLimitLevel();
        int currentLimit = island.getCurrentPlayerLimit();
        int nextUnitCost = island.getPlayerLimitUpgradeCost();
        int nextTokenCost = calculateTokenCost("player_limit", currentLevel);
        
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
            lore.add(Component.empty());
            lore.add(Component.text("━━━ Cost ━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
            lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
                .append(Component.text(formatNumber(nextUnitCost) + " Units", 
                    playerUnits >= nextUnitCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
                .append(Component.text(nextTokenCost + " Island Tokens", 
                    islandTokens >= nextTokenCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextUnitCost && islandTokens >= nextTokenCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                if (playerUnits < nextUnitCost) {
                    lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED));
                }
                if (islandTokens < nextTokenCost) {
                    lore.add(Component.text("✗ Not enough tokens!", NamedTextColor.RED));
                }
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createRedstoneLimitUpgradeItem(PlayerIsland island, int playerUnits, int islandTokens) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = island.getRedstoneLimitLevel();
        int currentLimit = island.getCurrentRedstoneLimit();
        int maxLevel = island.getMaxRedstoneLevel();
        int nextCost = island.getRedstoneLimitUpgradeCost();
        
        meta.displayName(Component.text("⚡ Redstone Limit", NamedTextColor.RED, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Limit: ", NamedTextColor.GRAY)
            .append(Component.text(currentLimit + " items", NamedTextColor.WHITE, TextDecoration.BOLD)));
        lore.add(Component.text("Level: ", NamedTextColor.GRAY)
            .append(Component.text(currentLevel + "/" + maxLevel, NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        if (currentLevel < maxLevel) {
            int nextLimit = island.getNextRedstoneLimit();
            int nextTokenCost = calculateTokenCost("redstone", currentLevel);
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextLimit + " items", NamedTextColor.WHITE))
                .append(Component.text(" (+5)", NamedTextColor.GRAY)));
            lore.add(Component.empty());
            lore.add(Component.text("━━━ Cost ━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
            lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
                .append(Component.text(formatNumber(nextCost) + " Units", 
                    playerUnits >= nextCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
                .append(Component.text(nextTokenCost + " Island Tokens", 
                    islandTokens >= nextTokenCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            lore.add(Component.text("Tracks redstone dust, torches,", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("repeaters, comparators, etc.", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost && islandTokens >= nextTokenCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                if (playerUnits < nextCost) {
                    lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED));
                }
                if (islandTokens < nextTokenCost) {
                    lore.add(Component.text("✗ Not enough tokens!", NamedTextColor.RED));
                }
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL (100 items)", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createCropGrowthUpgradeItem(PlayerIsland island, int playerUnits, int islandTokens) {
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
            int nextTokenCost = calculateTokenCost("crop_growth", currentLevel);
            lore.add(Component.text("Next: ", NamedTextColor.GREEN)
                .append(Component.text(nextMultiplier + "x", NamedTextColor.WHITE)));
            lore.add(Component.empty());
            lore.add(Component.text("━━━ Cost ━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
            lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
                .append(Component.text(formatNumber(nextCost) + " Units", 
                    playerUnits >= nextCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
                .append(Component.text(nextTokenCost + " Island Tokens", 
                    islandTokens >= nextTokenCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            
            if (playerUnits >= nextCost && islandTokens >= nextTokenCost) {
                lore.add(Component.text("✓ Click to upgrade!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                if (playerUnits < nextCost) {
                    lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED));
                }
                if (islandTokens < nextTokenCost) {
                    lore.add(Component.text("✗ Not enough tokens!", NamedTextColor.RED));
                }
            }
        } else {
            lore.add(Component.text("✓ MAX LEVEL", NamedTextColor.GREEN, TextDecoration.BOLD));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createWeatherControlItem(PlayerIsland island, int playerUnits, int islandTokens) {
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
            int weatherTokenCost = calculateTokenCost("weather", 0);
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
            lore.add(Component.empty());
            lore.add(Component.text("Purchase to control weather", NamedTextColor.GRAY));
            lore.add(Component.text("on your island!", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("━━━ Cost ━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
            lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
                .append(Component.text("50,000 Units", 
                    playerUnits >= 50000 ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
                .append(Component.text(weatherTokenCost + " Island Tokens", 
                    islandTokens >= weatherTokenCost ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            
            if (playerUnits >= 50000 && islandTokens >= weatherTokenCost) {
                lore.add(Component.text("✓ Click to purchase!", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                if (playerUnits < 50000) {
                    lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED));
                }
                if (islandTokens < weatherTokenCost) {
                    lore.add(Component.text("✗ Not enough tokens!", NamedTextColor.RED));
                }
            }
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createCurrencyInfoItem(int playerUnits, int islandTokens) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("💰 Your Balance", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        lore.add(Component.text("  ⛃ ", NamedTextColor.GOLD)
            .append(Component.text(formatNumber(playerUnits) + " Units", NamedTextColor.YELLOW, TextDecoration.BOLD)));
        lore.add(Component.text("  ⭐ ", NamedTextColor.AQUA)
            .append(Component.text(islandTokens + " Island Tokens", NamedTextColor.AQUA, TextDecoration.BOLD)));
        lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        lore.add(Component.empty());
        lore.add(Component.text("All upgrades now require", NamedTextColor.GRAY));
        lore.add(Component.text("both Units and Island Tokens!", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Complete challenges to earn", NamedTextColor.YELLOW));
        lore.add(Component.text("Island Tokens!", NamedTextColor.YELLOW));
        
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
     * Adds cyan borders to the GUI
     */
    private static void addBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);
        
        // Top row
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
        }
        
        // Bottom row
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, border);
        }
        
        // Left and right columns
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, border);
            gui.setItem(i * 9 + 8, border);
        }
    }
    
    /**
     * Creates the back button
     */
    private static ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("❌ Back", NamedTextColor.RED, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Return to island menu", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
            // Reopen GUI with updated values on main thread
            Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                open(player, island, islandManager);
            });
        } else {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(result.getMessage(), NamedTextColor.RED)));
        }
    }
    
    // Helper methods for calculating next values
    private static int getNextPlayerLimit(int currentLevel) {
        switch (currentLevel) {
            case 0: return 5;
            case 1: return 10;
            case 2: return 20;
            case 3: return 50;
            default: return 50;
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

