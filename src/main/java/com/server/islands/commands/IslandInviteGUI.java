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

import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for inviting players to island
 */
public class IslandInviteGUI {
    
    private static final String GUI_TITLE = "📨 Invite Player";
    
    public static void open(Player player, IslandManager islandManager) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));
        
        int slot = 10;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue;
            if (slot >= 44) break;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            
            meta.setOwningPlayer(onlinePlayer);
            meta.displayName(Component.text("§a§l" + onlinePlayer.getName())
                .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Click to invite this player").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§7to your island").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§eClick to invite!").decoration(TextDecoration.ITALIC, false));
            
            meta.lore(lore);
            skull.setItemMeta(meta);
            
            gui.setItem(slot, skull);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        
        // Back button
        gui.setItem(49, createBackButton());
        
        fillEmpty(gui);
        player.openInventory(gui);
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
