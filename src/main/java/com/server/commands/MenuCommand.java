package com.server.commands;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "Player Menu");

        // Create Profile Selection button (Book)
        ItemStack profileButton = new ItemStack(Material.BOOK);
        ItemMeta profileMeta = profileButton.getItemMeta();
        profileMeta.setDisplayName("§6§lProfile Selection");
        profileMeta.setLore(Arrays.asList(
            "§7Manage your player profiles",
            "§7Current Profiles: §e0/3",
            "",
            "§eClick to open profile manager"
        ));
        profileButton.setItemMeta(profileMeta);

        // Create Stats View button (Nether Star)
        ItemStack statsButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = statsButton.getItemMeta();
        statsMeta.setDisplayName("§b§lPlayer Stats");
        statsMeta.setLore(Arrays.asList(
            "§7View your current profile stats",
            "§7Includes combat, defense and more",
            "",
            "§eClick to view stats"
        ));
        statsButton.setItemMeta(statsMeta);

        // Fill empty slots with black glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        // Fill all slots with filler first
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, filler);
        }

        // Add buttons in specific slots (centered)
        menu.setItem(11, profileButton);  // Left-center position
        menu.setItem(15, statsButton);    // Right-center position

        player.openInventory(menu);
    }
}