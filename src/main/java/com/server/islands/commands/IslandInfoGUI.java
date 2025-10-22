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
    
    private static final String GUI_TITLE = "§6§l ℹ §e§lIsland Info §6§l ℹ";
    
    /**
     * Opens the island info GUI
     */
    public static void open(Player player, UUID islandOwner, IslandManager islandManager) {
        islandManager.loadIsland(islandOwner).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("That player doesn't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Open GUI on main thread
            Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
                
                // Island Info (slot 13)
                gui.setItem(13, createIslandInfoItem(island));
                
                // Statistics (slot 22)
                gui.setItem(22, createStatisticsItem(island));
                
                // Upgrades (slot 31)
                gui.setItem(31, createUpgradesItem(island));
                
                // Close button (slot 49)
                gui.setItem(49, createCloseButton());
                
                // Fill empty slots
                fillEmptySlots(gui);
                
                player.openInventory(gui);
            });
        });
    }
    
    private static ItemStack createIslandInfoItem(PlayerIsland island) {
        ItemStack item = new ItemStack(getMaterialForType(island.getIslandType()));
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("🏝 Island Information", NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Owner: ", NamedTextColor.GRAY)
            .append(Component.text(Bukkit.getOfflinePlayer(island.getOwnerUuid()).getName(), NamedTextColor.WHITE)));
        lore.add(Component.text("Name: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandName(), NamedTextColor.AQUA)));
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandType().getDisplayName(), NamedTextColor.GREEN)));
        lore.add(Component.empty());
        lore.add(Component.text("Created: ", NamedTextColor.GRAY)
            .append(Component.text(formatDate(island.getCreatedAt()), NamedTextColor.WHITE)));
        lore.add(Component.text("Last Visited: ", NamedTextColor.GRAY)
            .append(Component.text(formatDate(island.getLastAccessed()), NamedTextColor.WHITE)));
        
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
        String redstoneLimitText = (redstoneLimit == -1) ? "UNLIMITED" : (redstoneLimit + " devices");
        lore.add(Component.text("Redstone Limit: ", NamedTextColor.GRAY)
            .append(Component.text(redstoneLimitText, NamedTextColor.WHITE))
            .append(Component.text(" (Lv " + island.getRedstoneLimitLevel() + "/5)", NamedTextColor.DARK_GRAY)));
        
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
    
    private static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("❌ Close", NamedTextColor.RED, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Click to close this menu", NamedTextColor.GRAY));
        
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
