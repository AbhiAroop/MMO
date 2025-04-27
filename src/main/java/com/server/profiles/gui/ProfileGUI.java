package com.server.profiles.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

public class ProfileGUI {
    
    public static void openProfileSelector(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Profile Selection");
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());

        // Add profile slots
        for (int i = 0; i < 3; i++) {
            if (profiles[i] != null) {
                gui.setItem(11 + (i * 2), createProfileItem(profiles[i], i == activeSlot));
            } else {
                gui.setItem(11 + (i * 2), createEmptyProfileSlot(i + 1));
            }
        }

        player.openInventory(gui);
    }

    private static ItemStack createProfileItem(PlayerProfile profile, boolean isActive) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        // Add a prefix to show active profile
        String displayName = isActive ? 
            "§a➤ §6Profile " + profile.getSlot() + " §a(Active)" : 
            "§6Profile " + profile.getSlot();
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>(Arrays.asList(
            "§7Name: " + profile.getName(),
            "§7Created: " + new java.util.Date(profile.getCreated()),
            "§7World: " + profile.getWorldName(),
            String.format("§7Location: %.0f, %.0f, %.0f", profile.getX(), profile.getY(), profile.getZ()),
            ""
        ));

        // Add appropriate action text based on active status
        if (isActive) {
            lore.add("§eCurrently Selected");
            lore.add("§cShift + Right Click to delete");
        } else {
            lore.add("§eClick to select");
            lore.add("§cShift + Right Click to delete");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createEmptyProfileSlot(int slot) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Empty Profile Slot #" + slot);
        meta.setLore(Arrays.asList("§eClick to create new profile!"));
        item.setItemMeta(meta);
        return item;
    }

    public static void openMainMenu(Player player) {
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
        
        // Create Skills button (Experience Bottle)
        ItemStack skillsButton = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta skillsMeta = skillsButton.getItemMeta();
        skillsMeta.setDisplayName("§a§lSkills");
        skillsMeta.setLore(Arrays.asList(
            "§7View and manage your skills",
            "§7Level up skills to earn rewards",
            "",
            "§eClick to view skills"
        ));
        skillsButton.setItemMeta(skillsMeta);
        
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
        menu.setItem(10, profileButton);  // Left position
        menu.setItem(12, skillsButton);   // Left-center position
        menu.setItem(14, currencyButton); // Right-center position
        menu.setItem(16, statsButton);    // Right position

        player.openInventory(menu);
    }

    
}