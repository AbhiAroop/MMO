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
 * GUI for managing island roles (promote/demote/kick)
 */
public class IslandManageRolesGUI {
    
    public static void open(Player player, IslandManager islandManager) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) return;
            
            // Check if player has permission to manage roles (ADMIN+)
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role == null || !role.hasPermission(IslandMember.IslandRole.ADMIN)) {
                    player.sendMessage(Component.text("✗ ", net.kyori.adventure.text.format.NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .append(Component.text("You need to be at least an Admin to manage roles!", net.kyori.adventure.text.format.NamedTextColor.RED)));
                    return;
                }
                
                islandManager.getMembers(islandId).thenAccept(members -> {
                    Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                        Inventory gui = Bukkit.createInventory(null, 54, Component.text("⚙ Manage Roles"));
                    
                    int slot = 10;
                    for (IslandMember member : members) {
                        if (member.getRole() == IslandMember.IslandRole.OWNER) continue;
                        if (slot >= 44) break;
                        
                        String playerName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
                        if (playerName == null) playerName = "Unknown";
                        
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getPlayerUuid()));
                        
                        String roleColor = getRoleColor(member.getRole());
                        meta.displayName(Component.text(roleColor + "§l" + playerName)
                            .decoration(TextDecoration.ITALIC, false));
                        
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text("§7Current Role: " + roleColor + member.getRole().getDisplayName())
                            .decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("§eLeft-Click §7to promote").decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("§eRight-Click §7to demote").decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("§eShift+Right-Click §7to kick").decoration(TextDecoration.ITALIC, false));
                        
                        meta.lore(lore);
                        skull.setItemMeta(meta);
                        
                        gui.setItem(slot, skull);
                        slot++;
                        if (slot % 9 == 8) slot += 2;
                    }
                    
                    gui.setItem(49, createBackButton());
                    fillEmpty(gui);
                    player.openInventory(gui);
                    });
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
    
    private static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
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
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        
        ItemStack borderPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        borderPane.setItemMeta(borderMeta);
        
        // Fill border (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, borderPane);
            if (gui.getItem(45 + i) == null) gui.setItem(45 + i, borderPane);
        }
        
        // Fill side borders (left and right columns)
        for (int i = 1; i < 5; i++) {
            if (gui.getItem(i * 9) == null) gui.setItem(i * 9, borderPane); // Left side
            if (gui.getItem(i * 9 + 8) == null) gui.setItem(i * 9 + 8, borderPane); // Right side
        }
        
        // Fill remaining empty slots
        for (int i = 9; i < 45; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, pane);
        }
    }
}
