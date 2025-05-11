package com.server.profiles.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

/**
 * GUI for profile management and player menu
 */
public class ProfileGUI {
    
    // Constants for GUI titles (used by GUIListener to prevent item removal)
    public static final String PLAYER_MENU_TITLE = ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Player Menu" + ChatColor.GOLD + " ✦";
    public static final String PROFILE_SELECTION_TITLE = ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Profile Selection" + ChatColor.GOLD + " ✦";
    
    /**
     * Open the profile selector GUI
     */
    public static void openProfileSelector(Player player) {
        // Create inventory with enhanced title and larger size (45 slots) for better symmetry
        Inventory gui = Bukkit.createInventory(null, 45, PROFILE_SELECTION_TITLE);
        
        // Get player profiles
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());

        // Add decorative border
        createBorder(gui);
        
        // Add info item at the top center
        ItemStack infoItem = createInfoItem(player, profiles);
        gui.setItem(4, infoItem);
        
        // Add profile slots in a more centralized layout (row 2, centered)
        // Now using slots 13, 22, 31 for a vertical centered layout
        for (int i = 0; i < 3; i++) {
            if (profiles[i] != null) {
                gui.setItem(13 + (i * 9), createProfileItem(profiles[i], i == activeSlot));
            } else {
                gui.setItem(13 + (i * 9), createEmptyProfileSlot(i + 1));
            }
        }
        
        // Add help button at the bottom right
        ItemStack helpButton = new ItemStack(Material.BOOK);
        ItemMeta helpMeta = helpButton.getItemMeta();
        helpMeta.setDisplayName(ChatColor.YELLOW + "Help & Information");
        
        List<String> helpLore = new ArrayList<>();
        helpLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        helpLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Click on a profile to select it");
        helpLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Click on an empty slot to create a new profile");
        helpLore.add(ChatColor.GRAY + "• " + ChatColor.RED + "Shift + Right click" + ChatColor.WHITE + " to delete a profile");
        helpLore.add("");
        helpLore.add(ChatColor.GRAY + "You can have up to 3 separate profiles");
        helpLore.add(ChatColor.GRAY + "Each with their own progress and inventory.");
        
        helpMeta.setLore(helpLore);
        helpButton.setItemMeta(helpMeta);
        gui.setItem(42, helpButton);
        
        // Add back button (bottom left)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to Menu");
        backButton.setItemMeta(backMeta);
        gui.setItem(36, backButton);
        
        // Fill empty slots
        fillEmptySlots(gui);

        // Play sound effect
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Create an info item for the profile selector
     */
    private static ItemStack createInfoItem(Player player, PlayerProfile[] profiles) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.AQUA + player.getName() + "'s Profiles" + ChatColor.GOLD + " ✦");
        
        // Count active profiles
        int activeProfiles = 0;
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                activeProfiles++;
            }
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Select a profile to play with");
        lore.add(ChatColor.GRAY + "or create a new one.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Active Profiles: " + 
                 ChatColor.GOLD + activeProfiles + "/" + profiles.length);
        lore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Last Played: " + 
                 ChatColor.GOLD + getCurrentProfileName(player, profiles));
        
        meta.setLore(lore);
        // Add glow effect for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get the current profile name for display
     */
    private static String getCurrentProfileName(Player player, PlayerProfile[] profiles) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null && profiles[activeSlot] != null) {
            return profiles[activeSlot].getName();
        }
        return "None";
    }

    /**
     * Create an item representing a player profile
     */
    private static ItemStack createProfileItem(PlayerProfile profile, boolean isActive) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        // Add a prefix to show active profile with improved formatting
        ChatColor nameColor = isActive ? ChatColor.GREEN : ChatColor.GOLD;
        String displayName = isActive ? 
            nameColor + "» " + profile.getName() + " " + ChatColor.GREEN + "(Active)" : 
            nameColor + profile.getName();
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Slot: " + ChatColor.WHITE + (profile.getSlot() + 1));
        lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Created: " + ChatColor.WHITE + 
                 new java.text.SimpleDateFormat("MM/dd/yyyy").format(new java.util.Date(profile.getCreated())));
        
        // Add world and location with better formatting
        lore.add("");
        lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "World: " + ChatColor.WHITE + profile.getWorldName());
        lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Location: " + ChatColor.WHITE + 
                 String.format("%.0f, %.0f, %.0f", profile.getX(), profile.getY(), profile.getZ()));
        lore.add("");

        // Add appropriate action text based on active status
        if (isActive) {
            lore.add(ChatColor.GREEN + "✓ " + ChatColor.WHITE + "Currently Selected");
            lore.add("");
            lore.add(ChatColor.RED + "Shift + Right Click to delete");
            
            // Add glow effect to active profile
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(ChatColor.YELLOW + "Click to select this profile");
            lore.add("");
            lore.add(ChatColor.RED + "Shift + Right Click to delete");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item for an empty profile slot
     */
    private static ItemStack createEmptyProfileSlot(int slot) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE); // Changed to LIME for better visibility
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "✦ Create New Profile ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Slot #" + slot + " is empty.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to create a new profile!");
        lore.add(ChatColor.GRAY + "This will start a fresh character");
        lore.add(ChatColor.GRAY + "with default stats and equipment.");
        
        meta.setLore(lore);
        // Add glow effect for better visibility
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Open the main player menu
     */
    public static void openMainMenu(Player player) {
        // Create inventory with enhanced title
        Inventory menu = Bukkit.createInventory(null, 36, PLAYER_MENU_TITLE);

        // Add decorative border
        createBorder(menu);
        
        // Add player head in the center top
        ItemStack playerHead = createPlayerHeadItem(player);
        menu.setItem(4, playerHead);

        // Create Profile Selection button (Book)
        ItemStack profileButton = new ItemStack(Material.BOOK);
        ItemMeta profileMeta = profileButton.getItemMeta();
        profileMeta.setDisplayName(ChatColor.GOLD + "Profile Selection");
        
        List<String> profileLore = new ArrayList<>();
        profileLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        profileLore.add(ChatColor.GRAY + "Manage your player profiles");
        
        // Count active profiles
        int activeProfiles = 0;
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
        for (PlayerProfile profile : profiles) {
            if (profile != null) {
                activeProfiles++;
            }
        }
        
        profileLore.add(ChatColor.GRAY + "Current Profiles: " + ChatColor.YELLOW + activeProfiles + "/3");
        profileLore.add("");
        profileLore.add(ChatColor.YELLOW + "Click to open profile manager");
        
        profileMeta.setLore(profileLore);
        profileButton.setItemMeta(profileMeta);

        // Create Stats View button (Nether Star)
        ItemStack statsButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = statsButton.getItemMeta();
        statsMeta.setDisplayName(ChatColor.AQUA + "Player Stats");
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        statsLore.add(ChatColor.GRAY + "View your current profile stats");
        statsLore.add(ChatColor.GRAY + "including:");
        statsLore.add("");
        statsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Combat & Defense");
        statsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Farming & Fishing");
        statsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Mining & Resources");
        statsLore.add("");
        statsLore.add(ChatColor.YELLOW + "Click to view detailed stats");
        
        statsMeta.setLore(statsLore);
        statsButton.setItemMeta(statsMeta);
        
        // Create Skills button (Experience Bottle)
        ItemStack skillsButton = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta skillsMeta = skillsButton.getItemMeta();
        skillsMeta.setDisplayName(ChatColor.GREEN + "Skills Menu");
        
        List<String> skillsLore = new ArrayList<>();
        skillsLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        skillsLore.add(ChatColor.GRAY + "View and manage your skills");
        skillsLore.add("");
        skillsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Level up skills");
        skillsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Unlock abilities");
        skillsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Earn rewards");
        skillsLore.add("");
        skillsLore.add(ChatColor.YELLOW + "Click to view skills");
        
        skillsMeta.setLore(skillsLore);
        skillsButton.setItemMeta(skillsMeta);
        
        // Create Currency Display button (Gold Ingot)
        ItemStack currencyButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta currencyMeta = currencyButton.getItemMeta();
        currencyMeta.setDisplayName(ChatColor.YELLOW + "Currency Balances");
        
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
        
        // Format currency values
        List<String> currencyLore = new ArrayList<>();
        currencyLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        currencyLore.add(ChatColor.GRAY + "Your current balances:");
        currencyLore.add("");
        currencyLore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Units: " + 
                         ChatColor.WHITE + CurrencyFormatter.formatUnits(units));
        currencyLore.add(ChatColor.LIGHT_PURPLE + "» " + ChatColor.LIGHT_PURPLE + "Premium Units: " + 
                         ChatColor.WHITE + CurrencyFormatter.formatPremiumUnits(premiumUnits));
        currencyLore.add(ChatColor.AQUA + "» " + ChatColor.AQUA + "Essence: " + 
                         ChatColor.WHITE + CurrencyFormatter.formatEssence(essence));
        currencyLore.add(ChatColor.GREEN + "» " + ChatColor.GREEN + "Bits: " + 
                         ChatColor.WHITE + CurrencyFormatter.formatBits(bits));
        currencyLore.add("");
        currencyLore.add(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/balance" + 
                         ChatColor.GRAY + " to check currencies");
        currencyLore.add(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/pay" + 
                         ChatColor.GRAY + " to transfer Units/Premium");
        
        currencyMeta.setLore(currencyLore);
        currencyButton.setItemMeta(currencyMeta);

        // Add buttons in a diamond pattern for better visual appeal
        menu.setItem(11, profileButton);   // Top left
        menu.setItem(15, statsButton);     // Top right
        menu.setItem(21, skillsButton);    // Bottom left
        menu.setItem(23, currencyButton);  // Bottom right
        
        // Fill remaining empty slots
        fillEmptySlots(menu);
        
        // Play sound effect
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);

        // Open the inventory
        player.openInventory(menu);
    }
    
    /**
     * Create a player head item with player info
     */
    private static ItemStack createPlayerHeadItem(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.GREEN + player.getName() + ChatColor.GOLD + " ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Welcome to your player menu!");
        lore.add(ChatColor.GRAY + "Select an option below.");
        
        // Get current profile details if available
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Active Profile: " + 
                         ChatColor.GOLD + profile.getName());
            }
        }
        
        meta.setLore(lore);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        head.setItemMeta(meta);
        
        return head;
    }
    
    /**
     * Create a decorative border for the GUI
     */
    private static void createBorder(Inventory gui) {
        ItemStack bluePaneLight = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack bluePaneDark = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack corner = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        
        int size = gui.getSize();
        int rows = size / 9;
        
        // Set corners
        gui.setItem(0, corner);
        gui.setItem(8, corner);
        gui.setItem(size - 9, corner);
        gui.setItem(size - 1, corner);
        
        // Top and bottom borders with alternating colors
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? bluePaneDark : bluePaneLight);
            gui.setItem(size - 9 + i, i % 2 == 0 ? bluePaneDark : bluePaneLight);
        }
        
        // Side borders
        for (int i = 1; i < rows - 1; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? bluePaneLight : bluePaneDark);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? bluePaneLight : bluePaneDark);
        }
    }
    
    /**
     * Create a glass pane with an empty name
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Fill empty slots with black glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}