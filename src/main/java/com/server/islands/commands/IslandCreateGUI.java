package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.IslandType;
import com.server.islands.managers.IslandManager;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for creating/purchasing a new island.
 * Shows 3 island types with costs and descriptions.
 */
public class IslandCreateGUI {
    
    private static final String GUI_TITLE = "§6§l✦ §e§lCreate Island §6§l✦";
    
    /**
     * Opens the island creation GUI for a player
     */
    public static void open(Player player, IslandManager islandManager) {
        // Check if player already has an island
        islandManager.loadIsland(player.getUniqueId()).thenAccept(existingIsland -> {
            if (existingIsland != null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You already have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Open GUI on main thread
            Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
                
                // Get player's currency
                PlayerProfile profile = ProfileManager.getInstance().getActivePlayerProfile(player.getUniqueId());
                int playerUnits = (profile != null) ? profile.getUnits() : 0;
                
                // SKY Island (slot 11)
                gui.setItem(11, createIslandItem(IslandType.SKY, playerUnits));
                
                // OCEAN Island (slot 13)
                gui.setItem(13, createIslandItem(IslandType.OCEAN, playerUnits));
                
                // FOREST Island (slot 15)
                gui.setItem(15, createIslandItem(IslandType.FOREST, playerUnits));
                
                // Info item (slot 22)
                gui.setItem(22, createInfoItem(playerUnits));
                
                // Fill empty slots with glass panes
                fillEmptySlots(gui);
                
                player.openInventory(gui);
            });
        });
    }
    
    /**
     * Creates an island type item for the GUI
     */
    private static ItemStack createIslandItem(IslandType type, int playerUnits) {
        Material material;
        switch (type) {
            case SKY:
                material = Material.FEATHER;
                break;
            case OCEAN:
                material = Material.PRISMARINE_CRYSTALS;
                break;
            case FOREST:
                material = Material.OAK_SAPLING;
                break;
            default:
                material = Material.GRASS_BLOCK;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Title
        meta.displayName(Component.text(type.getDisplayName(), getTypeColor(type), TextDecoration.BOLD));
        
        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(type.getDescription(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Cost: ", NamedTextColor.YELLOW)
            .append(Component.text(formatNumber(type.getCost()) + " Units", NamedTextColor.GOLD, TextDecoration.BOLD)));
        lore.add(Component.empty());
        
        // Can afford?
        if (playerUnits >= type.getCost()) {
            lore.add(Component.text("✓ Click to purchase!", NamedTextColor.GREEN, TextDecoration.BOLD));
        } else {
            lore.add(Component.text("✗ Not enough units!", NamedTextColor.RED, TextDecoration.BOLD));
            lore.add(Component.text("Need: " + formatNumber(type.getCost() - playerUnits) + " more", NamedTextColor.DARK_RED));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Creates the info item at the bottom
     */
    private static ItemStack createInfoItem(int playerUnits) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("ℹ Your Currency", NamedTextColor.AQUA, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Balance: ", NamedTextColor.GRAY)
            .append(Component.text(formatNumber(playerUnits) + " Units", NamedTextColor.GOLD, TextDecoration.BOLD)));
        lore.add(Component.empty());
        lore.add(Component.text("Choose an island type above!", NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Fills empty slots with gray glass panes
     */
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
    
    /**
     * Handles clicking on an island type
     */
    public static void handleClick(Player player, IslandType type, IslandManager islandManager) {
        player.closeInventory();
        
        // Create the island
        islandManager.createIsland(player, type).thenAccept(island -> {
            if (island != null) {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Island created successfully!", NamedTextColor.GREEN)));
                player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY)
                    .append(Component.text(type.getDisplayName(), getTypeColor(type))));
                player.sendMessage(Component.text("Teleporting...", NamedTextColor.YELLOW));
                
                // Teleport to the island after a short delay
                Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                    islandManager.getPlugin().getLogger().info("[Island] Attempting to teleport " + player.getName() + " to island " + island.getIslandId());
                    islandManager.teleportToIsland(player, island).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Component.text("Welcome to your new island!", NamedTextColor.GREEN, TextDecoration.BOLD));
                            islandManager.getPlugin().getLogger().info("[Island] Successfully teleported " + player.getName() + " to island");
                        } else {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("Failed to teleport to island. Please try /island home", NamedTextColor.RED)));
                            islandManager.getPlugin().getLogger().warning("[Island] Failed to teleport " + player.getName() + " to island");
                        }
                    });
                }, 20L); // 1 second delay
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Failed to create island. You may not have enough currency or already have an island.", NamedTextColor.RED)));
            }
        });
    }
    
    /**
     * Gets the color for an island type
     */
    private static NamedTextColor getTypeColor(IslandType type) {
        switch (type) {
            case SKY:
                return NamedTextColor.AQUA;
            case OCEAN:
                return NamedTextColor.BLUE;
            case FOREST:
                return NamedTextColor.GREEN;
            default:
                return NamedTextColor.WHITE;
        }
    }
    
    /**
     * Formats a number with commas
     */
    private static String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
