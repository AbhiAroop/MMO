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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for managing island settings
 */
public class IslandSettingsGUI {
    
    private static final String GUI_TITLE = "⚙ Island Settings";
    
    public static void open(Player player, IslandManager islandManager) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Check if player is owner or co-owner
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role == null || !role.hasPermission(com.server.islands.data.IslandMember.IslandRole.CO_OWNER)) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Only the owner or co-owner can change island settings!", NamedTextColor.RED)));
                    return;
                }
                
                // Load island
                islandManager.loadIsland(islandId).thenAccept(island -> {
                    if (island == null) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Failed to load island data!", NamedTextColor.RED)));
                        return;
                    }
                    
                    // Debug logging
                    islandManager.getPlugin().getLogger().info("[Island] [Settings GUI] Opening for player " + player.getName() + ", visitorsEnabled=" + island.isVisitorsEnabled() + ", pvpEnabled=" + island.isPvpEnabled());
                    
                    Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                        Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));
                        
                        // Add borders
                        addBorders(gui);
                        
                        // Island Name (slot 13)
                        gui.setItem(13, createNameChangeItem(island));
                        
                        // PVP Toggle (slot 20)
                        gui.setItem(20, createPvPToggleItem(island));
                        
                        // Visitor Access Toggle (slot 24)
                        ItemStack visitorItem = createVisitorToggleItem(island);
                        gui.setItem(24, visitorItem);
                        islandManager.getPlugin().getLogger().info("[Island] [Settings GUI] Set slot 24 to material=" + visitorItem.getType() + " for visitorsEnabled=" + island.isVisitorsEnabled());
                        
                        // Back button (slot 49)
                        gui.setItem(49, createBackButton());
                        
                        // Fill empty slots
                        fillEmptySlots(gui);
                        
                        player.openInventory(gui);
                    });
                });
            });
        });
    }
    
    private static ItemStack createNameChangeItem(PlayerIsland island) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("✏ Change Island Name", NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Current name: §f" + island.getIslandName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§7Click to change your island's name", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7(Max 32 characters)", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§e➤ Click to rename", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createPvPToggleItem(PlayerIsland island) {
        boolean pvpEnabled = island.isPvpEnabled();
        
        Material material = pvpEnabled ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String status = pvpEnabled ? "§a§lENABLED" : "§c§lDISABLED";
        meta.displayName(Component.text("⚔ Island PVP", NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Status: " + status).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§7When enabled, island members", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7can fight each other on the island", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (pvpEnabled) {
            lore.add(Component.text("§c✗ Click to DISABLE PVP", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("§a✓ Click to ENABLE PVP", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createVisitorToggleItem(PlayerIsland island) {
        boolean visitorsEnabled = island.isVisitorsEnabled();
        
        // Debug logging
        org.bukkit.Bukkit.getLogger().info("[Island] [Settings GUI] createVisitorToggleItem called with visitorsEnabled=" + visitorsEnabled);
        
        Material material = visitorsEnabled ? Material.OAK_DOOR : Material.IRON_DOOR;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        org.bukkit.Bukkit.getLogger().info("[Island] [Settings GUI] Using material=" + material + " for visitorsEnabled=" + visitorsEnabled);
        
        String status = visitorsEnabled ? "§a§lALLOWED" : "§c§lBLOCKED";
        meta.displayName(Component.text("🚪 Visitor Access", NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Status: " + status).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§7When enabled, other players", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7can visit your island", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        if (visitorsEnabled) {
            lore.add(Component.text("§c✗ Click to BLOCK visitors", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("§a✓ Click to ALLOW visitors", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        }
        
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
    
    private static void addBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(meta);
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border.clone());
            gui.setItem(45 + i, border.clone());
        }
        
        // Left and right columns
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, border.clone());
            gui.setItem(i * 9 + 8, border.clone());
        }
    }
    
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(meta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}
