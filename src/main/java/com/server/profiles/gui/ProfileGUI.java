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
}