package com.server.islands.commands;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.IslandStatistics;
import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for displaying island information and statistics.
 */
public class IslandInfoGUI {
    
    private static final String GUI_TITLE = "§b§lIsland Information";
    
    /**
     * Opens the island info GUI for the player's own island
     * This method is used when clicking from the island menu
     */
    public static void openForOwnIsland(Player player, PlayerIsland island, IslandManager islandManager) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
            
            // Add cyan borders first
            addBorders(gui);
            
            // Title/Header (slot 4)
            gui.setItem(4, createHeaderItem(island));
            
            // Island Basic Info (slot 20)
            gui.setItem(20, createIslandInfoItem(island));
            
            // Statistics (slot 22)
            gui.setItem(22, createStatisticsItem(island));
            
            // Upgrades Info (slot 24)
            gui.setItem(24, createUpgradesItem(island));
            
            // Members Info (slot 29)
            gui.setItem(29, createMembersItem(island));
            
            // Economy Info (slot 31)
            gui.setItem(31, createEconomyItem(island));
            
            // Settings Info (slot 33)
            gui.setItem(33, createSettingsItem(island));
            
            // Back button (slot 49)
            gui.setItem(49, createBackButton());
            
            // Fill empty slots
            fillEmptySlots(gui);
            
            player.openInventory(gui);
        });
    }
    
    /**
     * Opens the island info GUI for any player's island (used by command)
     */
    public static void open(Player player, UUID targetPlayer, IslandManager islandManager) {
        // First get the target player's island ID (whether they're owner or member)
        islandManager.getPlayerIslandId(targetPlayer).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("That player doesn't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Now load the island by ID
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Failed to load island data!", NamedTextColor.RED)));
                    return;
                }
                
                // Open GUI on main thread
                Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
                    
                    // Add cyan borders first
                    addBorders(gui);
                    
                    // Title/Header (slot 4)
                    gui.setItem(4, createHeaderItem(island));
                    
                    // Island Basic Info (slot 20)
                    gui.setItem(20, createIslandInfoItem(island));
                    
                    // Statistics (slot 22)
                    gui.setItem(22, createStatisticsItem(island));
                    
                    // Upgrades Info (slot 24)
                    gui.setItem(24, createUpgradesItem(island));
                    
                    // Members Info (slot 29)
                    gui.setItem(29, createMembersItem(island));
                    
                    // Economy Info (slot 31)
                    gui.setItem(31, createEconomyItem(island));
                    
                    // Settings Info (slot 33)
                    gui.setItem(33, createSettingsItem(island));
                    
                    // Back button (slot 49)
                    gui.setItem(49, createBackButton());
                    
                    // Fill empty slots
                    fillEmptySlots(gui);
                    
                    player.openInventory(gui);
                });
            });
        });
    }
    
    /**
     * Add cyan border decoration around the GUI
     */
    private static void addBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(meta);
        
        // Top and bottom rows (0-8, 45-53)
        for (int i = 0; i < 9; i++) {
            if (i != 4 && i != 49) { // Skip title and back button slots
                gui.setItem(i, border.clone());
            }
            if (i != 49 - 45) { // Adjust for bottom row
                gui.setItem(45 + i, border.clone());
            }
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border.clone());
            gui.setItem(row * 9 + 8, border.clone());
        }
    }
    
    /**
     * Create header item showing island name and type
     */
    private static ItemStack createHeaderItem(PlayerIsland island) {
        ItemStack item = new ItemStack(getMaterialForType(island.getIslandType()));
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("🏝 " + island.getIslandName(), NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandType().getDisplayName(), NamedTextColor.GREEN))
            .decoration(TextDecoration.ITALIC, false));
        String ownerName = Bukkit.getOfflinePlayer(island.getOwnerUuid()).getName();
        if (ownerName == null) {
            ownerName = "Unknown";
        }
        lore.add(Component.text("Owner: ", NamedTextColor.GRAY)
            .append(Component.text(ownerName, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("View detailed information below", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, true));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createIslandInfoItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("📍 Basic Information", NamedTextColor.AQUA, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Island Name: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandName(), NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandType().getDisplayName(), NamedTextColor.GREEN))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Size: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCurrentSize() + "x" + island.getCurrentSize(), NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Created: ", NamedTextColor.GRAY)
            .append(Component.text(formatDate(island.getCreatedAt()), NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Last Visited: ", NamedTextColor.GRAY)
            .append(Component.text(formatDate(island.getLastAccessed()), NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createStatisticsItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        IslandStatistics stats = island.getStatistics();
        
        meta.displayName(Component.text("📊 Statistics", NamedTextColor.AQUA, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Total Visits: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(stats.getTotalVisits()), NamedTextColor.WHITE)));
        lore.add(Component.text("Unique Visitors: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(stats.getUniqueVisitors()), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Blocks Placed: ", NamedTextColor.GRAY)
            .append(Component.text(formatNumber(stats.getBlocksPlaced()), NamedTextColor.GREEN)));
        lore.add(Component.text("Blocks Broken: ", NamedTextColor.GRAY)
            .append(Component.text(formatNumber(stats.getBlocksBroken()), NamedTextColor.RED)));
        lore.add(Component.empty());
        lore.add(Component.text("Mobs Killed: ", NamedTextColor.GRAY)
            .append(Component.text(formatNumber(stats.getMobsKilled()), NamedTextColor.YELLOW)));
        lore.add(Component.text("Players Killed: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(stats.getPlayersKilled()), NamedTextColor.DARK_RED)));
        lore.add(Component.empty());
        lore.add(Component.text("Total Playtime: ", NamedTextColor.GRAY)
            .append(Component.text(formatPlaytime(stats.getTotalPlayTime()), NamedTextColor.GOLD)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createUpgradesItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("⚒ Upgrades", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Island Size: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCurrentSize() + "x" + island.getCurrentSize(), NamedTextColor.WHITE))
            .append(Component.text(" (Lv " + island.getSizeLevel() + "/7)", NamedTextColor.DARK_GRAY)));
        lore.add(Component.text("Player Limit: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCurrentPlayerLimit() + " players", NamedTextColor.WHITE))
            .append(Component.text(" (Lv " + island.getPlayerLimitLevel() + "/5)", NamedTextColor.DARK_GRAY)));
        lore.add(Component.empty());
        
        int redstoneLimit = island.getCurrentRedstoneLimit();
        lore.add(Component.text("Redstone Limit: ", NamedTextColor.GRAY)
            .append(Component.text(redstoneLimit + " items", NamedTextColor.WHITE))
            .append(Component.text(" (Lv " + island.getRedstoneLimitLevel() + "/" + island.getMaxRedstoneLevel() + ")", NamedTextColor.DARK_GRAY)));
        
        lore.add(Component.text("Crop Growth: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCropGrowthMultiplier() + "x", NamedTextColor.WHITE))
            .append(Component.text(" (Lv " + island.getCropGrowthLevel() + "/4)", NamedTextColor.DARK_GRAY)));
        lore.add(Component.empty());
        lore.add(Component.text("Weather Control: ", NamedTextColor.GRAY)
            .append(Component.text(island.hasWeatherControl() ? "ENABLED" : "DISABLED", 
                island.hasWeatherControl() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createMembersItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("👥 Owner & Members", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        String ownerName = Bukkit.getOfflinePlayer(island.getOwnerUuid()).getName();
        lore.add(Component.text("Owner: ", NamedTextColor.GRAY)
            .append(Component.text(ownerName != null ? ownerName : "Unknown", NamedTextColor.GOLD))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Max Members: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCurrentPlayerLimit(), NamedTextColor.GREEN))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Member management coming soon!", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, true));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createEconomyItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("💰 Economy", NamedTextColor.GREEN, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Island Level: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandLevel(), NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Island Tokens: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandTokens(), NamedTextColor.YELLOW))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Earn tokens by completing", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("island challenges!", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createSettingsItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("⚙ Settings", NamedTextColor.YELLOW, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Weather Control: ", NamedTextColor.GRAY)
            .append(Component.text(island.hasWeatherControl() ? "✓ Unlocked" : "✗ Locked", 
                island.hasWeatherControl() ? NamedTextColor.GREEN : NamedTextColor.RED))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Current Biome: ", NamedTextColor.GRAY)
            .append(Component.text(island.getCurrentBiome(), NamedTextColor.GREEN))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("More settings coming soon!", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, true));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
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
    
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    private static Material getMaterialForType(com.server.islands.data.IslandType type) {
        switch (type) {
            case SKY:
                return Material.FEATHER;
            case OCEAN:
                return Material.PRISMARINE_CRYSTALS;
            case FOREST:
                return Material.OAK_SAPLING;
            default:
                return Material.GRASS_BLOCK;
        }
    }
    
    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(new Date(timestamp));
    }
    
    private static String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    private static String formatPlaytime(long milliseconds) {
        long hours = milliseconds / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
