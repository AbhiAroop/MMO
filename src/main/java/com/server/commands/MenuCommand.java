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

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

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
        
        // Create Currency Display button (Gold Ingot)
        ItemStack currencyButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta currencyMeta = currencyButton.getItemMeta();
        currencyMeta.setDisplayName("§e§lCurrency Balances");
        
        // Get profile currencies
        int units = 0;
        int premiumUnits = 0;
        int essence = 0;
        int bits = 0;
        
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                units = profile.getUnits();
                premiumUnits = profile.getPremiumUnits();
                essence = profile.getEssence();
                bits = profile.getBits();
            }
        }
        
        // Format currency values using the CurrencyFormatter
        currencyMeta.setLore(Arrays.asList(
            "§7Your current balances:",
            "",
            "§e§lUnits: §f" + CurrencyFormatter.formatUnits(units),
            "§d§lPremium Units: §f" + CurrencyFormatter.formatPremiumUnits(premiumUnits),
            "§b§lEssence: §f" + CurrencyFormatter.formatEssence(essence),
            "§a§lBits: §f" + CurrencyFormatter.formatBits(bits),
            "",
            "§7Use /balance to check currencies",
            "§7Use /pay to transfer Units/Premium"
        ));
        currencyButton.setItemMeta(currencyMeta);

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
        menu.setItem(11, profileButton);  // Left position
        menu.setItem(13, currencyButton); // Center position
        menu.setItem(15, statsButton);    // Right position

        player.openInventory(menu);
    }
}