package com.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;

/**
 * Command to open the player's main menu
 */
public class MenuCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        // Use the ProfileGUI's openMainMenu method to ensure consistency
        ProfileGUI.openMainMenu(player);
        return true;
    }

    /**
     * Legacy method for backward compatibility - redirects to ProfileGUI
     * This will be used in case any other plugin calls this method directly
     */
    @Deprecated
    public void openMenu(Player player) {
        ProfileGUI.openMainMenu(player);
    }
    
    /**
     * Create a player head item with custom name and lore including playtime
     */
    public static ItemStack createPlayerHeadItem(Player player, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(displayName);
        
        // Create enhanced lore with playtime information
        List<String> enhancedLore = new ArrayList<>();
        
        // Add the original lore first
        if (lore != null) {
            enhancedLore.addAll(lore);
        }
        
        // Get active profile information
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile activeProfile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (activeProfile != null) {
                // Add playtime and creation date information
                enhancedLore.add("");
                enhancedLore.add(ChatColor.GRAY + "▬▬▬▬▬ " + ChatColor.AQUA + "Profile Info" + ChatColor.GRAY + " ▬▬▬▬▬");
                enhancedLore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Created: " + 
                            ChatColor.GOLD + activeProfile.getFormattedCreationDate());
                enhancedLore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Total Playtime: " + 
                            ChatColor.GREEN + activeProfile.getFormattedPlaytime());
                
                // Add session indicator if currently active
                if (activeProfile.isActiveSession()) {
                    enhancedLore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Status: " + 
                                ChatColor.GREEN + "Currently Playing");
                }
            }
        }
        
        meta.setLore(enhancedLore);
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        head.setItemMeta(meta);
        return head;
    }
    
    /**
     * Create a menu button with custom material, name, and lore
     */
    public static ItemStack createMenuButton(Material material, String displayName, List<String> lore, boolean enchanted) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        
        // Add enchant glow if specified
        if (enchanted) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Hide attributes to keep the tooltip clean
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * Create a glass pane with empty name for decoration
     */
    public static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Create a decorative border for a menu with alternating colors
     */
    public static void createBorder(Inventory menu) {
        ItemStack bluePane = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack lightBluePane = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack cornerPane = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        
        int size = menu.getSize();
        int rows = size / 9;
        
        // Set corners
        menu.setItem(0, cornerPane);
        menu.setItem(8, cornerPane);
        menu.setItem(size - 9, cornerPane);
        menu.setItem(size - 1, cornerPane);
        
        // Top and bottom borders with alternating colors
        for (int i = 1; i < 8; i++) {
            menu.setItem(i, i % 2 == 0 ? bluePane : lightBluePane);
            menu.setItem(size - 9 + i, i % 2 == 0 ? bluePane : lightBluePane);
        }
        
        // Side borders
        for (int i = 1; i < rows - 1; i++) {
            menu.setItem(i * 9, i % 2 == 0 ? lightBluePane : bluePane);
            menu.setItem(i * 9 + 8, i % 2 == 0 ? lightBluePane : bluePane);
        }
    }
    
    /**
     * Fill empty inventory slots with black glass panes
     */
    public static void fillEmptySlots(Inventory menu) {
        ItemStack filler = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }
    }
    
    /**
     * Counts the number of active profiles for a player
     */
    public static int countActiveProfiles(Player player) {
        int activeProfiles = 0;
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
        
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                activeProfiles++;
            }
        }
        
        return activeProfiles;
    }
    
    /**
     * Gets the name of the player's current active profile
     */
    public static String getActiveProfileName(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                return profile.getName();
            }
        }
        return "None";
    }
}