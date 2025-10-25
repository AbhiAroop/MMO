package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.server.islands.data.IslandMember;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for viewing island members
 */
public class IslandMembersGUI {
    
    private static final String GUI_TITLE = "👥 Island Members";
    
    public static void open(Player player, IslandManager islandManager) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("§c✗ You don't have an island!"));
                return;
            }
            
            islandManager.getMembers(islandId).thenAccept(members -> {
                Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));
                    
                    int slot = 10;
                    for (IslandMember member : members) {
                        if (slot >= 44) break; // Don't overflow
                        
                        String playerName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
                        if (playerName == null) playerName = "Unknown";
                        
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getPlayerUuid()));
                        
                        String roleColor = getRoleColor(member.getRole());
                        meta.displayName(Component.text(roleColor + "§l" + playerName)
                            .decoration(TextDecoration.ITALIC, false));
                        
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text("§7Role: " + roleColor + member.getRole().getDisplayName())
                            .decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("§7Joined: §e" + formatTimestamp(member.getAddedAt()))
                            .decoration(TextDecoration.ITALIC, false));
                        
                        if (Bukkit.getOfflinePlayer(member.getPlayerUuid()).isOnline()) {
                            lore.add(Component.text("§a● Online").decoration(TextDecoration.ITALIC, false));
                        } else {
                            lore.add(Component.text("§8● Offline").decoration(TextDecoration.ITALIC, false));
                        }
                        
                        meta.lore(lore);
                        skull.setItemMeta(meta);
                        
                        gui.setItem(slot, skull);
                        slot++;
                        if (slot % 9 == 8) slot += 2; // Skip to next row
                    }
                    
                    // Back button
                    gui.setItem(49, createBackButton());
                    
                    // Decoration
                    fillEmpty(gui);
                    
                    player.openInventory(gui);
                });
            });
        });
    }
    
    private static String getRoleColor(IslandMember.IslandRole role) {
        switch (role) {
            case OWNER: return "§6";
            case CO_OWNER: return "§e";
            case ADMIN: return "§c";
            case MOD: return "§9";
            case MEMBER: return "§a";
            default: return "§7";
        }
    }
    
    private static String formatTimestamp(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long days = diff / (24 * 60 * 60 * 1000);
        if (days > 0) return days + "d ago";
        long hours = diff / (60 * 60 * 1000);
        if (hours > 0) return hours + "h ago";
        long minutes = diff / (60 * 1000);
        return minutes + "m ago";
    }
    
    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("❌ Back", net.kyori.adventure.text.format.NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Return to island menu", net.kyori.adventure.text.format.NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private static void fillEmpty(Inventory gui) {
        ItemStack decoration = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = decoration.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        decoration.setItemMeta(meta);
        
        ItemStack borderDecoration = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderDecoration.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        borderDecoration.setItemMeta(borderMeta);
        
        // Fill border (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, borderDecoration);
            if (gui.getItem(45 + i) == null) gui.setItem(45 + i, borderDecoration);
        }
        
        // Fill side borders (left and right columns)
        for (int i = 1; i < 5; i++) {
            if (gui.getItem(i * 9) == null) gui.setItem(i * 9, borderDecoration); // Left side
            if (gui.getItem(i * 9 + 8) == null) gui.setItem(i * 9 + 8, borderDecoration); // Right side
        }
        
        // Fill remaining empty slots
        for (int i = 9; i < 45; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, decoration);
            }
        }
    }
}
