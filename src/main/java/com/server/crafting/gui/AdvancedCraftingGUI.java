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

import com.server.Main;
import com.server.crafting.manager.CustomCraftingManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Advanced 4x4 crafting GUI with 2x2 output grid
 */
public class AdvancedCraftingGUI {
    
    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "Advanced Crafting Table" + ChatColor.DARK_GRAY + " ✦";
    
    // Slot positions for the 4x4 crafting grid (centered in the GUI)
    public static final int[] CRAFTING_SLOTS = {
        10, 11, 12, 13,  // Top row
        19, 20, 21, 22,  // Second row
        28, 29, 30, 31,  // Third row
        37, 38, 39, 40   // Bottom row
    };
    
    // Output slot positions (2x2 grid)
    public static final int[] OUTPUT_SLOTS = {
        15, 16,  // Top output row
        24, 25   // Bottom output row
    };
    
    // Store active advanced crafting inventories for each player
    private static final Map<Player, Inventory> activeAdvancedCraftingGUIs = new HashMap<>();
    
    /**
     * Open the advanced 4x4 crafting table for a player
     */
    public static void openAdvancedCraftingTable(Player player) {
        // Create 6 row inventory (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Create the decorative border and layout
        createAdvancedCraftingLayout(gui);
        
        // Store the GUI for this player
        activeAdvancedCraftingGUIs.put(player, gui);
        
        // Open the inventory
        player.openInventory(gui);
    }
    
    /**
     * Create the advanced crafting table layout with borders and indicators
     */
    private static void createAdvancedCraftingLayout(Inventory gui) {
        // Fill all slots with border glass initially
        fillAdvancedBorder(gui);
        
        // Clear the crafting grid slots (make them available for items)
        for (int slot : CRAFTING_SLOTS) {
            gui.setItem(slot, null);
        }
        
        // Clear the output slots initially and place barriers
        for (int slot : OUTPUT_SLOTS) {
            gui.setItem(slot, createOutputBarrier());
        }
        
        // Add crafting grid indicators
        addAdvancedCraftingIndicators(gui);
        
        // Add decorative elements
        addAdvancedDecorativeElements(gui);
        
        // Add navigation arrow back to 3x3 crafting
        addNavigationArrow(gui);
    }
    
    /**
     * Create a decorative border around the GUI
     */
    private static void fillAdvancedBorder(Inventory gui) {
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
            gui.setItem(45 + i, i % 2 == 0 ? lightBlueBorder : blueBorder);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, cyanBorder);
            gui.setItem(i * 9 + 8, cyanBorder);
        }
        
        // Corners with special color
        gui.setItem(0, cyanBorder);
        gui.setItem(8, cyanBorder);
        gui.setItem(45, cyanBorder);
        gui.setItem(53, cyanBorder);
    }
    
    /**
     * Add visual indicators around the crafting grid
     */
    private static void addAdvancedCraftingIndicators(Inventory gui) {
        // Create crafting grid border indicators
        ItemStack craftingBorder = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, 
            ChatColor.GOLD + "Advanced Crafting Grid");
        
        // Surround the 4x4 grid with orange glass
        int[] borderSlots = {
            9, 14,      // Top border sides
            18, 23,     // Second row border sides  
            27, 32,     // Third row border sides
            36, 41      // Bottom border sides
        };
        
        for (int slot : borderSlots) {
            gui.setItem(slot, craftingBorder);
        }
        
        // Add output slot borders
        ItemStack outputBorder = createGlassPane(Material.LIME_STAINED_GLASS_PANE, 
            ChatColor.GREEN + "Results");
        
        // Border around 2x2 output grid
        gui.setItem(6, outputBorder);   // Above top-left output
        gui.setItem(7, outputBorder);   // Above top-right output
        gui.setItem(17, outputBorder);  // Left of top-left output
        gui.setItem(26, outputBorder);  // Left of bottom-left output
        gui.setItem(33, outputBorder);  // Below bottom-left output
        gui.setItem(34, outputBorder);  // Below bottom-right output
    }
    
    /**
     * Add decorative elements to make the GUI more appealing
     */
    private static void addAdvancedDecorativeElements(Inventory gui) {
        // Add advanced crafting table icon
        ItemStack craftingIcon = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta iconMeta = craftingIcon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.GOLD + "✦ Advanced Crafting Table ✦");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place items in the 4x4 grid",
            ChatColor.GRAY + "to craft advanced items!",
            "",
            ChatColor.YELLOW + "• Supports all vanilla recipes",
            ChatColor.YELLOW + "• Advanced 4x4 custom recipes",
            ChatColor.YELLOW + "• Multiple output items",
            ChatColor.YELLOW + "• Shift-click for bulk crafting"
        ));
        craftingIcon.setItemMeta(iconMeta);
        gui.setItem(4, craftingIcon); // Top center
        
        // Add arrows pointing to outputs
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.GREEN + "Results");
        arrowMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Your crafted items appear here",
            "",
            ChatColor.YELLOW + "Click to take 1 item",
            ChatColor.YELLOW + "Shift-click for bulk crafting",
            ChatColor.YELLOW + "Multiple outputs supported!"
        ));
        arrow.setItemMeta(arrowMeta);
        gui.setItem(42, arrow); // Below crafting grid, pointing to outputs
    }
    
    /**
     * Add navigation arrow back to 3x3 crafting
     */
    private static void addNavigationArrow(Inventory gui) {
        ItemStack backArrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = backArrow.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "← Back to 3x3 Crafting");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Return to the standard",
            ChatColor.GRAY + "3x3 crafting table",
            "",
            ChatColor.YELLOW + "Click to switch!"
        ));
        backArrow.setItemMeta(meta);
        gui.setItem(45, backArrow); // Bottom left corner
    }
    
    /**
     * Create the barrier item for empty output slots
     */
    public static ItemStack createOutputBarrier() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "No Recipe");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place items in the crafting grid",
            ChatColor.GRAY + "to see results here."
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
     * Check if a slot is part of the 4x4 crafting grid
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
     * Check if a slot is an output slot
     */
    public static boolean isOutputSlot(int slot) {
        for (int outputSlot : OUTPUT_SLOTS) {
            if (outputSlot == slot) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a slot is the navigation arrow
     */
    public static boolean isNavigationSlot(int slot) {
        return slot == 45; // Bottom left corner
    }
    
    /**
     * Get the 4x4 crafting grid contents from the GUI
     */
    public static ItemStack[] getCraftingGrid(Inventory gui) {
        ItemStack[] grid = new ItemStack[16];
        for (int i = 0; i < CRAFTING_SLOTS.length; i++) {
            grid[i] = gui.getItem(CRAFTING_SLOTS[i]);
        }
        return grid;
    }
        
    /**
     * Update the crafting result based on the current 4x4 crafting grid
     */
    public static void updateCraftingResult(Inventory inventory, Player player) {
        // Get 4x4 crafting grid items
        ItemStack[] craftingGrid = new ItemStack[16];
        for (int i = 0; i < CRAFTING_SLOTS.length; i++) {
            craftingGrid[i] = inventory.getItem(CRAFTING_SLOTS[i]);
            if (craftingGrid[i] == null) {
                craftingGrid[i] = new ItemStack(Material.AIR);
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            // Debug what's actually in the 4x4 grid with slot mapping
            boolean hasNonAirItems = false;
            StringBuilder gridContents = new StringBuilder("[Advanced Crafting] 4x4 Grid update - Contents:\n");
            gridContents.append("Slot mapping (GUI slot -> Grid index):\n");
            
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int gridIndex = row * 4 + col;
                    int guiSlot = CRAFTING_SLOTS[gridIndex];
                    ItemStack item = craftingGrid[gridIndex];
                    
                    String content = item == null ? "NULL" : 
                                item.getType() == Material.AIR ? "AIR" : item.getType().name();
                    if (item != null && item.getType() != Material.AIR) {
                        hasNonAirItems = true;
                    }
                    
                    gridContents.append(String.format("GUI[%2d]->Grid[%2d]:%8s ", guiSlot, gridIndex, content));
                }
                gridContents.append("\n");
            }
            
            if (hasNonAirItems) {
                Main.getInstance().debugLog(DebugSystem.GUI, gridContents.toString());
            }
        }
        
        // Check for 4x4 recipe match
        ItemStack[] results = CustomCraftingManager.getInstance().getAdvancedRecipeResult(craftingGrid, player);
        
        setOutputSlots(inventory, results);
    }

    /**
     * Set the output slots with results (supports multiple items)
     */
    public static void setOutputSlots(Inventory gui, ItemStack[] results) {
        // Clear all output slots first
        for (int slot : OUTPUT_SLOTS) {
            gui.setItem(slot, createOutputBarrier());
        }
        
        // Set results if available
        if (results != null) {
            for (int i = 0; i < Math.min(results.length, OUTPUT_SLOTS.length); i++) {
                if (results[i] != null && results[i].getType() != Material.AIR) {
                    gui.setItem(OUTPUT_SLOTS[i], results[i]);
                }
            }
        }
    }
    
    /**
     * Get the active advanced crafting GUI for a player
     */
    public static Inventory getActiveAdvancedCraftingGUI(Player player) {
        return activeAdvancedCraftingGUIs.get(player);
    }
    
    /**
     * Remove a player's active advanced crafting GUI
     */
    public static void removeActiveAdvancedCraftingGUI(Player player) {
        activeAdvancedCraftingGUIs.remove(player);
    }
    
    /**
     * Check if an inventory is an advanced crafting GUI
     */
    public static boolean isAdvancedCraftingGUI(Inventory inventory) {
        return inventory.getSize() == 54 && GUI_TITLE.equals(inventory.getViewers().isEmpty() ? 
            null : inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    /**
     * Clear the 4x4 crafting grid for a player
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