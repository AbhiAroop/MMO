package com.server.crafting.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.crafting.manager.CustomCraftingManager;

/**
 * GUI for the custom crafting table
 */
public class CustomCraftingGUI {
    
    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "Crafting Table" + ChatColor.DARK_GRAY + " ✦";
    
    // Slot positions for the 3x3 crafting grid (centered in the GUI)
    public static final int[] CRAFTING_SLOTS = {
        11, 12, 13,  // Top row
        20, 21, 22,  // Middle row
        29, 30, 31   // Bottom row
    };
    
    // Output slot position
    public static final int OUTPUT_SLOT = 25;
    
    // Store active crafting inventories for each player
    private static final Map<Player, Inventory> activeCraftingGUIs = new HashMap<>();
    
    /**
     * Open the custom crafting table for a player
     */
    public static void openCraftingTable(Player player) {
        // Create 5 row inventory (45 slots)
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        
        // Create the decorative border and layout
        createCraftingLayout(gui);
        
        // Store the GUI for this player
        activeCraftingGUIs.put(player, gui);
        
        // Open the inventory
        player.openInventory(gui);
    }
    
    /**
     * Create the crafting table layout with borders and indicators
     */
    private static void createCraftingLayout(Inventory gui) {
        // Fill all slots with border glass initially
        fillBorder(gui);
        
        // Clear the crafting grid slots (make them available for items)
        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }
        
        // Clear the output slot initially and place barrier
        gui.setItem(OUTPUT_SLOT, createOutputBarrier());
        
        // Add crafting grid indicators
        addCraftingIndicators(gui);
        
        // Add decorative elements
        addDecorativeElements(gui);
    }
    
    /**
     * Create a decorative border around the GUI
     */
    private static void fillBorder(Inventory gui) {
        // Create different glass pane types for visual appeal
        ItemStack blueBorder = createGlassPane(Material.BLUE_STAINED_GLASS_PANE, " ");
        ItemStack lightBlueBorder = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        ItemStack cyanBorder = createGlassPane(Material.CYAN_STAINED_GLASS_PANE, " ");
        ItemStack blackFiller = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // Fill all slots with black glass initially
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, blackFiller);
        }
        
        // Create decorative border pattern
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, i % 2 == 0 ? blueBorder : lightBlueBorder);
            gui.setItem(36 + i, i % 2 == 0 ? lightBlueBorder : blueBorder);
        }
        
        // Side borders
        for (int i = 1; i < 4; i++) {
            gui.setItem(i * 9, cyanBorder);
            gui.setItem(i * 9 + 8, cyanBorder);
        }
        
        // Corners with special color
        gui.setItem(0, cyanBorder);
        gui.setItem(8, cyanBorder);
        gui.setItem(36, cyanBorder);
        gui.setItem(44, cyanBorder);
    }
    
    /**
     * Add visual indicators around the crafting grid
     */
    private static void addCraftingIndicators(Inventory gui) {
        // Create crafting grid border indicators
        ItemStack craftingBorder = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, 
            ChatColor.GOLD + "Crafting Grid");
        
        // Surround the 3x3 grid with orange glass (but don't override existing border)
        int[] borderSlots = {
            10, 14,     // Top border sides
            19, 23,     // Middle border sides  
            28, 32      // Bottom border sides
        };
        
        for (int slot : borderSlots) {
            gui.setItem(slot, craftingBorder);
        }
        
        // Add output slot border
        ItemStack outputBorder = createGlassPane(Material.LIME_STAINED_GLASS_PANE, 
            ChatColor.GREEN + "Result");
        gui.setItem(16, outputBorder);  // Right side of output
        gui.setItem(34, outputBorder);  // Below output
    }
    
    /**
     * Add decorative elements to make the GUI more appealing
     */
    private static void addDecorativeElements(Inventory gui) {
        // Add crafting table icon
        ItemStack craftingIcon = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta iconMeta = craftingIcon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.GOLD + "✦ Crafting Table ✦");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place items in the 3x3 grid",
            ChatColor.GRAY + "to craft new items!",
            "",
            ChatColor.YELLOW + "• Supports all vanilla recipes",
            ChatColor.YELLOW + "• Shift-click for bulk crafting",
            ChatColor.YELLOW + "• Custom recipes coming soon!"
        ));
        craftingIcon.setItemMeta(iconMeta);
        gui.setItem(4, craftingIcon); // Top center
        
        // Add arrow pointing to output
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.GREEN + "Result");
        arrowMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Your crafted item appears here",
            "",
            ChatColor.YELLOW + "Click to take 1 item",
            ChatColor.YELLOW + "Shift-click for bulk crafting"
        ));
        arrow.setItemMeta(arrowMeta);
        gui.setItem(24, arrow); // Left of output slot
    }
    
    /**
     * Create the barrier item for empty output slot
     */
    public static ItemStack createOutputBarrier() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "No Recipe");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place items in the crafting grid",
            ChatColor.GRAY + "to see the result here."
        ));
        barrier.setItemMeta(meta);
        return barrier;
    }
    
    /**
     * Create a glass pane with specified material and name
     */
    private static ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Check if a slot is part of the crafting grid
     */
    public static boolean isCraftingSlot(int slot) {
        for (int craftingSlot : CRAFTING_SLOTS) {
            if (craftingSlot == slot) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a slot is the output slot
     */
    public static boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }
    
    /**
     * Get the crafting grid contents from the GUI
     */
    public static ItemStack[] getCraftingGrid(Inventory gui) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < CRAFTING_SLOTS.length; i++) {
            grid[i] = gui.getItem(CRAFTING_SLOTS[i]);
        }
        return grid;
    }
    
    /**
     * Set the output slot item
     */
    public static void setOutputSlot(Inventory gui, ItemStack result) {
        if (result == null || result.getType() == Material.AIR) {
            gui.setItem(OUTPUT_SLOT, createOutputBarrier());
        } else {
            gui.setItem(OUTPUT_SLOT, result);
        }
    }
    
    /**
     * Update the crafting result when the grid changes
     */
    public static void updateCraftingResult(Inventory gui, Player player) {
        ItemStack[] craftingGrid = getCraftingGrid(gui);
        ItemStack result = CustomCraftingManager.getInstance().getRecipeResult(craftingGrid);
        setOutputSlot(gui, result);
    }
    
    /**
     * Get the active crafting GUI for a player
     */
    public static Inventory getActiveCraftingGUI(Player player) {
        return activeCraftingGUIs.get(player);
    }
    
    /**
     * Remove a player's active crafting GUI
     */
    public static void removeActiveCraftingGUI(Player player) {
        activeCraftingGUIs.remove(player);
    }
    
    /**
     * Check if an inventory is a custom crafting GUI
     */
    public static boolean isCustomCraftingGUI(Inventory inventory) {
        return inventory.getSize() == 45 && GUI_TITLE.equals(inventory.getViewers().isEmpty() ? 
            null : inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    /**
     * Clear the crafting grid for a player
     */
    public static void clearCraftingGrid(Inventory gui, Player player) {
        for (int slot : CRAFTING_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // Return items to player inventory
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                // Drop any items that don't fit
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                gui.setItem(slot, null);
            }
        }
        // Update the result
        updateCraftingResult(gui, player);
    }
}