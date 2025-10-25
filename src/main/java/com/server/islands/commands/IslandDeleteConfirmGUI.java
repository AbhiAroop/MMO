package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Confirmation GUI for deleting island
 */
public class IslandDeleteConfirmGUI {
    
    public static void open(Player player, IslandManager islandManager) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("💀 Delete Island?"));
        
        // Confirm button
        ItemStack confirm = new ItemStack(Material.TNT);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("§4§lCONFIRM DELETE").decoration(TextDecoration.ITALIC, false));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text("§7Permanently delete your island").decoration(TextDecoration.ITALIC, false));
        confirmLore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
        confirmLore.add(Component.text("§c⚠ THIS CANNOT BE UNDONE!").decoration(TextDecoration.ITALIC, false));
        confirmLore.add(Component.text("§cAll members will be removed!").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        gui.setItem(11, confirm);
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("§a§lCANCEL").decoration(TextDecoration.ITALIC, false));
        List<Component> cancelLore = new ArrayList<>();
        cancelLore.add(Component.text("§7Return to island menu").decoration(TextDecoration.ITALIC, false));
        cancelLore.add(Component.text("§7Keep your island safe").decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        gui.setItem(15, cancel);
        
        // Warning
        ItemStack warning = new ItemStack(Material.BARRIER);
        ItemMeta warningMeta = warning.getItemMeta();
        warningMeta.displayName(Component.text("§4§lDANGER!").decoration(TextDecoration.ITALIC, false));
        List<Component> warningLore = new ArrayList<>();
        warningLore.add(Component.text("§cDeleting your island will:").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§7• Remove all blocks").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§7• Kick all members").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§7• Delete all progress").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§4This is PERMANENT!").decoration(TextDecoration.ITALIC, false));
        warningMeta.lore(warningLore);
        warning.setItemMeta(warningMeta);
        gui.setItem(13, warning);
        
        fillEmpty(gui);
        player.openInventory(gui);
    }
    
    private static void fillEmpty(Inventory gui) {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        
        ItemStack borderPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        borderPane.setItemMeta(borderMeta);
        
        // Fill border (top and bottom rows) with black for danger
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, borderPane);
            if (gui.getItem(18 + i) == null) gui.setItem(18 + i, borderPane);
        }
        
        // Fill remaining empty slots with red
        for (int i = 9; i < 18; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, pane);
        }
    }
}
