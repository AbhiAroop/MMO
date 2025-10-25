package com.server.islands.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for the Island Shop where players can buy special items using island tokens
 */
public class IslandShopGUI {
    
    private static final String GUI_TITLE = "§b§lIsland Shop";
    
    /**
     * Open the Island Shop GUI for a player
     */
    public static void open(Player player, IslandManager islandManager) {
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("You don't have an island!", NamedTextColor.RED));
                return;
            }
            
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
                    
                    // Add cyan borders first
                    addBorders(gui);
                    
                    // Fill empty slots with black glass
                    fillEmptySlots(gui);
                    
                    // Add player's island info
                    addIslandInfo(gui, island, player);
                    
                    // Add shop items
                    addShopItems(gui, island);
                    
                // Add back button
                addBackButton(gui);
                
                player.openInventory(gui);
            });
        });
    }
    
    /**
     * Add cyan border decoration around the GUI
     */
    private static void addBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(meta);
        
        // Top and bottom rows (0-8, 45-53)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border.clone());
            gui.setItem(45 + i, border.clone());
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border.clone());
            gui.setItem(row * 9 + 8, border.clone());
        }
    }
    
    /**
     * Fill empty slots with black glass decoration
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(meta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Add island information display
     */
    private static void addIslandInfo(Inventory gui, PlayerIsland island, Player player) {
        ItemStack info = new ItemStack(Material.EMERALD);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text("Island Tokens", NamedTextColor.GREEN, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Your Balance: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandTokens() + " Tokens", NamedTextColor.YELLOW))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Island Level: ", NamedTextColor.GRAY)
            .append(Component.text(island.getIslandLevel(), NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Earn tokens by completing", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("island challenges!", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        info.setItemMeta(meta);
        gui.setItem(4, info);
    }
    
    /**
     * Add shop items to the GUI
     */
    private static void addShopItems(Inventory gui, PlayerIsland island) {
        int currentLevel = island.getIslandLevel();
        int currentTokens = island.getIslandTokens();
        
        // Test item: Mycelium
        addShopItem(gui, 20, Material.MYCELIUM, 
            "Mycelium Block", 
            50, // cost in tokens
            1,  // required island level
            64, // quantity
            currentLevel, currentTokens,
            List.of(
                "§7A rare block that spreads",
                "§7and grows mushrooms!",
                "",
                "§eLeft-click to purchase 1 stack"
            ));
        
        // Placeholder for more items
        addPlaceholderItem(gui, 21, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 22, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 23, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 24, "Coming Soon!", Material.BARRIER);
        
        addPlaceholderItem(gui, 29, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 30, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 31, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 32, "Coming Soon!", Material.BARRIER);
        addPlaceholderItem(gui, 33, "Coming Soon!", Material.BARRIER);
    }
    
    /**
     * Add a shop item to the GUI
     */
    private static void addShopItem(Inventory gui, int slot, Material material, String name,
                                    int tokenCost, int requiredLevel, int quantity,
                                    int playerLevel, int playerTokens, List<String> description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        boolean canAfford = playerTokens >= tokenCost;
        boolean levelUnlocked = playerLevel >= requiredLevel;
        boolean canPurchase = canAfford && levelUnlocked;
        
        // Title color based on availability
        NamedTextColor titleColor = canPurchase ? NamedTextColor.GREEN : 
                                   !levelUnlocked ? NamedTextColor.RED : NamedTextColor.YELLOW;
        
        meta.displayName(Component.text(name, titleColor, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        
        // Description
        for (String line : description) {
            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        
        // Quantity
        lore.add(Component.text("Quantity: ", NamedTextColor.GRAY)
            .append(Component.text("x" + quantity, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false));
        
        // Cost
        Component costComponent = Component.text("Cost: ", NamedTextColor.GRAY)
            .append(Component.text(tokenCost + " Tokens", canAfford ? NamedTextColor.YELLOW : NamedTextColor.RED))
            .decoration(TextDecoration.ITALIC, false);
        lore.add(costComponent);
        
        // Level requirement
        Component levelComponent = Component.text("Required Level: ", NamedTextColor.GRAY)
            .append(Component.text(requiredLevel, levelUnlocked ? NamedTextColor.AQUA : NamedTextColor.RED))
            .decoration(TextDecoration.ITALIC, false);
        lore.add(levelComponent);
        
        lore.add(Component.empty());
        
        // Status message
        if (!levelUnlocked) {
            lore.add(Component.text("❌ Island level too low!", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        } else if (!canAfford) {
            lore.add(Component.text("❌ Not enough tokens!", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("✔ Click to purchase!", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }
    
    /**
     * Add a placeholder item for future shop items
     */
    private static void addPlaceholderItem(Inventory gui, int slot, String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("More items will be added", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("to the shop soon!", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }
    
    /**
     * Add back button to return to island menu
     */
    private static void addBackButton(Inventory gui) {
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.RED, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Return to island menu", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        backButton.setItemMeta(meta);
        gui.setItem(49, backButton);
    }
    
    /**
     * Check if an inventory is the Island Shop GUI
     */
    public static boolean isIslandShopGUI(Inventory inventory) {
        return inventory.getSize() == 54 && 
               GUI_TITLE.equals(inventory.getViewers().isEmpty() ? "" : 
               inventory.getViewers().get(0).getOpenInventory().title().toString());
    }
}
