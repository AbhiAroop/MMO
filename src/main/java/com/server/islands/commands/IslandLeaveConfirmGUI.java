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
 * Confirmation GUI for leaving island
 */
public class IslandLeaveConfirmGUI {
    
    public static void open(Player player, IslandManager islandManager) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("⚠ Leave Island?"));
        
        // Confirm button (green)
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("§a§lCONFIRM LEAVE").decoration(TextDecoration.ITALIC, false));
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text("§7Click to leave your island").decoration(TextDecoration.ITALIC, false));
        confirmLore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
        confirmLore.add(Component.text("§cThis cannot be undone!").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        gui.setItem(11, confirm);
        
        // Cancel button (red)
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("§c§lCANCEL").decoration(TextDecoration.ITALIC, false));
        List<Component> cancelLore = new ArrayList<>();
        cancelLore.add(Component.text("§7Return to island menu").decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        gui.setItem(15, cancel);
        
        // Warning sign
        ItemStack warning = new ItemStack(Material.BARRIER);
        ItemMeta warningMeta = warning.getItemMeta();
        warningMeta.displayName(Component.text("§c§lWARNING").decoration(TextDecoration.ITALIC, false));
        List<Component> warningLore = new ArrayList<>();
        warningLore.add(Component.text("§7You are about to leave").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§7your current island!").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§eYou can create or join").decoration(TextDecoration.ITALIC, false));
        warningLore.add(Component.text("§eanother island afterwards.").decoration(TextDecoration.ITALIC, false));
        warningMeta.lore(warningLore);
        warning.setItemMeta(warningMeta);
        gui.setItem(13, warning);
        
        fillEmpty(gui);
        player.openInventory(gui);
    }
    
    private static void fillEmpty(Inventory gui) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        
        ItemStack borderPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        borderPane.setItemMeta(borderMeta);
        
        // Fill border (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, borderPane);
            if (gui.getItem(18 + i) == null) gui.setItem(18 + i, borderPane);
        }
        
        // Fill remaining empty slots
        for (int i = 9; i < 18; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, pane);
        }
    }
}
