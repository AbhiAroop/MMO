package com.server.crafting.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.crafting.data.FurnaceData;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * GUI for the custom furnace system
 */
public class CustomFurnaceGUI {
    
    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "Furnace" + ChatColor.DARK_GRAY + " ✦";
    
    // Slot positions
    public static final int INPUT_SLOT = 10;        // Input item slot (left side)
    public static final int FUEL_SLOT = 19;         // Fuel slot (below input)
    public static final int OUTPUT_SLOT = 16;       // Output slot (right side)
    public static final int COOK_TIMER_SLOT = 13;   // Cook timer indicator (center)
    public static final int FUEL_TIMER_SLOT = 22;   // Fuel timer indicator (below center)
    private static long lastLoadLogTime = 0;  // Throttling for debug messages

    // Store active furnace GUIs for each player with their furnace location
    private static final Map<Player, Location> activeFurnaceGUIs = new HashMap<>();
    
    /**
     * Open the custom furnace GUI for a player at a specific location
     */
    public static void openFurnaceGUI(Player player, Location furnaceLocation) {
        // Create 4 row inventory (36 slots)
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        
        // Get or create furnace data for this location
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Opening furnace GUI for " + player.getName() + 
                " at " + furnaceLocation.getBlockX() + "," + furnaceLocation.getBlockY() + "," + furnaceLocation.getBlockZ());
        }
        
        // Create the decorative layout
        createFurnaceLayout(gui, furnaceData);
        
        // Load current furnace contents AFTER creating layout
        loadFurnaceContents(gui, furnaceData);
        
        // Store the GUI association
        activeFurnaceGUIs.put(player, furnaceLocation);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Stored GUI association for " + player.getName());
        }
        
        // Open the inventory
        player.openInventory(gui);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Opened inventory for " + player.getName());
        }
    }
    
    /**
     * Create the furnace layout with decorative elements
     */
    private static void createFurnaceLayout(Inventory gui, FurnaceData furnaceData) {
        // Fill border
        fillFurnaceBorder(gui);
        
        // Clear functional slots
        gui.setItem(INPUT_SLOT, null);
        gui.setItem(FUEL_SLOT, null);
        gui.setItem(OUTPUT_SLOT, null);
        
        // Add slot indicators
        addSlotIndicators(gui);
        
        // Add decorative elements
        addFurnaceDecorations(gui);
        
        // Initialize timer indicators
        updateCookTimer(gui, furnaceData);
        updateFuelTimer(gui, furnaceData);
    }
    
    /**
     * Create decorative border around the GUI
     */
    private static void fillFurnaceBorder(Inventory gui) {
        // Create different colored glass panes
        ItemStack grayBorder = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack darkGrayBorder = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack orangeBorder = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        
        // Fill all slots with gray glass initially
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, grayBorder);
        }
        
        // Create border pattern
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, darkGrayBorder);
            gui.setItem(27 + i, darkGrayBorder);
        }
        
        // Side borders
        for (int i = 1; i < 3; i++) {
            gui.setItem(i * 9, darkGrayBorder);
            gui.setItem(i * 9 + 8, darkGrayBorder);
        }
        
        // Corners with orange accent
        gui.setItem(0, orangeBorder);
        gui.setItem(8, orangeBorder);
        gui.setItem(27, orangeBorder);
        gui.setItem(35, orangeBorder);
    }
    
    /**
     * Add decorative elements to make the GUI appealing
     */
    private static void addFurnaceDecorations(Inventory gui) {
        // Add furnace icon
        ItemStack furnaceIcon = new ItemStack(Material.FURNACE);
        ItemMeta iconMeta = furnaceIcon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.GOLD + "✦ Custom Furnace ✦");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Smelt items with enhanced efficiency!",
            "",
            ChatColor.YELLOW + "• Supports all vanilla recipes",
            ChatColor.YELLOW + "• Live progress tracking",
            ChatColor.YELLOW + "• Enhanced visual feedback"
        ));
        furnaceIcon.setItemMeta(iconMeta);
        gui.setItem(4, furnaceIcon); // Top center
    }
    
    /**
     * Add slot indicators around functional slots
     */
    private static void addSlotIndicators(Inventory gui) {
        // Input slot indicator
        ItemStack inputIndicator = createGlassPane(Material.BLUE_STAINED_GLASS_PANE, 
            ChatColor.BLUE + "Input Slot");
        gui.setItem(INPUT_SLOT - 1, inputIndicator);  // Left of input
        
        // Fuel slot indicator
        ItemStack fuelIndicator = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, 
            ChatColor.GOLD + "Fuel Slot");
        gui.setItem(FUEL_SLOT - 1, fuelIndicator);   // Left of fuel
        
        // Output slot indicator
        ItemStack outputIndicator = createGlassPane(Material.LIME_STAINED_GLASS_PANE, 
            ChatColor.GREEN + "Output Slot");
        gui.setItem(OUTPUT_SLOT + 1, outputIndicator);  // Right of output
    }
    
    /**
     * Update the cook timer indicator based on current smelting progress - ENHANCED: Shows paused state
     */
    public static void updateCookTimer(Inventory gui, FurnaceData furnaceData) {
        ItemStack timerItem;
        
        if (furnaceData.isActive()) {
            // Check if cooking is paused due to full output
            if (furnaceData.isCookingPaused()) {
                // Show paused state
                timerItem = new ItemStack(Material.ORANGE_STAINED_GLASS);
                ItemMeta meta = timerItem.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "⏸ Cooking Paused");
                
                double progress = furnaceData.getSmeltingProgress();
                String progressBar = createProgressBar(progress, 20, '█', '░');
                
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Progress: " + ChatColor.WHITE + String.format("%.1f%%", progress * 100),
                    ChatColor.GRAY + "Status: " + ChatColor.GOLD + "Output slot full!",
                    "",
                    ChatColor.GOLD + progressBar,
                    "",
                    ChatColor.YELLOW + "⚠ Remove items from output to continue"
                ));
                
                timerItem.setItemMeta(meta);
            } else {
                // Normal active cooking state
                double progress = furnaceData.getSmeltingProgress();
                
                // Choose material and color based on progress
                Material timerMaterial = Material.CLOCK;
                ChatColor progressColor;
                String statusText;
                
                if (progress < 0.25) {
                    progressColor = ChatColor.RED;
                    statusText = "⚡ Starting";
                } else if (progress < 0.5) {
                    progressColor = ChatColor.GOLD;
                    statusText = "🔥 Heating";
                } else if (progress < 0.75) {
                    progressColor = ChatColor.YELLOW;
                    statusText = "⚡ Processing";
                } else {
                    progressColor = ChatColor.GREEN;
                    statusText = "✨ Almost Done";
                }
                
                timerItem = new ItemStack(timerMaterial);
                ItemMeta meta = timerItem.getItemMeta();
                meta.setDisplayName(progressColor + "⟲ Cooking Progress");
                
                // Create progress bar
                String progressBar = createProgressBar(progress, 20, '█', '░');
                int remainingTime = furnaceData.getRemainingSmeltTime();
                
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Progress: " + ChatColor.WHITE + String.format("%.1f%%", progress * 100),
                    ChatColor.GRAY + "Time Left: " + ChatColor.WHITE + formatTime(remainingTime),
                    "",
                    progressColor + progressBar,
                    "",
                    progressColor + statusText
                ));
                
                timerItem.setItemMeta(meta);
            }
        } else {
            // No active cooking
            timerItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = timerItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "✗ Not Cooking");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place items in the input slot",
                ChatColor.GRAY + "and fuel in the fuel slot",
                ChatColor.GRAY + "to start cooking!"
            ));
            timerItem.setItemMeta(meta);
        }
        
        gui.setItem(COOK_TIMER_SLOT, timerItem);
    }
    
    /**
     * Update the fuel timer indicator based on fuel status
     */
    public static void updateFuelTimer(Inventory gui, FurnaceData furnaceData) {
        ItemStack timerItem;
        
        if (furnaceData.hasFuel()) {
            // Calculate fuel remaining percentage
            double fuelProgress = furnaceData.getFuelProgress();
            
            // Choose flame material and color based on fuel level
            Material flameMaterial;
            ChatColor flameColor;
            String statusText;
            
            if (fuelProgress > 0.6) {
                flameMaterial = Material.TORCH;
                flameColor = ChatColor.GOLD;
                statusText = "🔥 Strong Flame";
            } else if (fuelProgress > 0.3) {
                flameMaterial = Material.SOUL_TORCH;
                flameColor = ChatColor.YELLOW;
                statusText = "🔥 Burning";
            } else {
                flameMaterial = Material.REDSTONE_TORCH;
                flameColor = ChatColor.RED;
                statusText = "🔥 Low Fuel";
            }
            
            timerItem = new ItemStack(flameMaterial);
            ItemMeta meta = timerItem.getItemMeta();
            meta.setDisplayName(flameColor + "🔥 Fuel Status");
            
            String fuelBar = createProgressBar(fuelProgress, 15, '█', '░');
            int remainingFuelTime = furnaceData.getRemainingFuelTime();
            
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Fuel Level: " + ChatColor.WHITE + String.format("%.1f%%", fuelProgress * 100),
                ChatColor.GRAY + "Fuel Time: " + ChatColor.WHITE + formatTime(remainingFuelTime),
                "",
                ChatColor.GOLD + fuelBar,
                "",
                flameColor + statusText
            ));
            
            timerItem.setItemMeta(meta);
        } else {
            // No fuel
            timerItem = new ItemStack(Material.COAL);
            ItemMeta meta = timerItem.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_GRAY + "⚫ No Fuel");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Add fuel to start cooking:",
                ChatColor.YELLOW + "• Coal, Charcoal",
                ChatColor.YELLOW + "• Wood items",
                ChatColor.YELLOW + "• Blaze Rod, Lava Bucket",
                ChatColor.YELLOW + "• And more..."
            ));
            timerItem.setItemMeta(meta);
        }
        
        gui.setItem(FUEL_TIMER_SLOT, timerItem);
    }
    
    /**
     * Update all open furnace GUIs
     */
    public static void updateAllFurnaceGUIs() {
        for (Map.Entry<Player, Location> entry : activeFurnaceGUIs.entrySet()) {
            Player player = entry.getKey();
            Location location = entry.getValue();
            
            if (player.isOnline() && player.getOpenInventory() != null && 
                GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(location);
                Inventory gui = player.getOpenInventory().getTopInventory();
                
                updateCookTimer(gui, furnaceData);
                updateFuelTimer(gui, furnaceData);
                
                loadFurnaceContents(gui, furnaceData);
            }
        }
    }
    
    /**
     * Save GUI contents back to furnace data - FIXED: Proper null handling
     */
    public static void saveFurnaceContents(Inventory gui, FurnaceData furnaceData) {
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Saving furnace contents...");
        }
        
        // Save input item
        ItemStack inputItem = gui.getItem(INPUT_SLOT);
        if (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0) {
            furnaceData.setInputItem(inputItem.clone());
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Saved input: " + inputItem.getAmount() + "x " + inputItem.getType());
            }
        } else {
            furnaceData.setInputItem(null);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Cleared input slot");
            }
        }
        
        // Save fuel item
        ItemStack fuelItem = gui.getItem(FUEL_SLOT);
        if (fuelItem != null && fuelItem.getType() != Material.AIR && fuelItem.getAmount() > 0) {
            furnaceData.setFuelItem(fuelItem.clone());
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Saved fuel: " + fuelItem.getAmount() + "x " + fuelItem.getType());
            }
        } else {
            furnaceData.setFuelItem(null);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Cleared fuel slot");
            }
        }
        
        // Save output item
        ItemStack outputItem = gui.getItem(OUTPUT_SLOT);
        if (outputItem != null && outputItem.getType() != Material.AIR && outputItem.getAmount() > 0) {
            furnaceData.setOutputItem(outputItem.clone());
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Saved output: " + outputItem.getAmount() + "x " + outputItem.getType());
            }
        } else {
            furnaceData.setOutputItem(null);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Cleared output slot");
            }
        }
    }

    /**
     * Load current furnace contents into the GUI - FIXED: Throttled logging
     */
    public static void loadFurnaceContents(Inventory gui, FurnaceData furnaceData) {
        // Heavily throttle debug logging to prevent spam
        boolean shouldLog = Main.getInstance().isDebugEnabled(DebugSystem.GUI);
        
        if (shouldLog) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLoadLogTime > 10000) { // Only log every 10 seconds
                Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Loading furnace contents...");
                lastLoadLogTime = currentTime;
            }
        }
        
        // Load input item
        ItemStack inputItem = furnaceData.getInputItem();
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            gui.setItem(INPUT_SLOT, inputItem.clone());
        } else {
            gui.setItem(INPUT_SLOT, null);
        }
        
        // Load fuel item
        ItemStack fuelItem = furnaceData.getFuelItem();
        if (fuelItem != null && fuelItem.getType() != Material.AIR) {
            gui.setItem(FUEL_SLOT, fuelItem.clone());
        } else {
            gui.setItem(FUEL_SLOT, null);
        }
        
        // Load output item
        ItemStack outputItem = furnaceData.getOutputItem();
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            gui.setItem(OUTPUT_SLOT, outputItem.clone());
        } else {
            gui.setItem(OUTPUT_SLOT, null);
        }
    }
    
    /**
     * Check if a slot is a functional slot (can be interacted with)
     */
    public static boolean isFunctionalSlot(int slot) {
        return slot == INPUT_SLOT || slot == FUEL_SLOT || slot == OUTPUT_SLOT;
    }
    
    /**
     * Check if a slot is the input slot
     */
    public static boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT;
    }
    
    /**
     * Check if a slot is the fuel slot
     */
    public static boolean isFuelSlot(int slot) {
        return slot == FUEL_SLOT;
    }
    
    /**
     * Check if a slot is the output slot
     */
    public static boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }
    
    /**
     * Get the furnace location for a player
     */
    public static Location getFurnaceLocation(Player player) {
        return activeFurnaceGUIs.get(player);
    }
    
    /**
     * Remove a player's active furnace GUI
     */
    public static void removeActiveFurnaceGUI(Player player) {
        activeFurnaceGUIs.remove(player);
    }
    
    /**
     * Check if an inventory is a custom furnace GUI
     */
    public static boolean isCustomFurnaceGUI(Inventory inventory) {
        return inventory != null && 
               !inventory.getViewers().isEmpty() && 
               GUI_TITLE.equals(inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    /**
     * Create a progress bar string
     */
    private static String createProgressBar(double progress, int length, char filled, char empty) {
        int filledLength = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filledLength; i++) {
            bar.append(filled);
        }
        
        bar.append(ChatColor.DARK_GRAY);
        for (int i = filledLength; i < length; i++) {
            bar.append(empty);
        }
        
        return bar.toString();
    }
    
    /**
     * Format time in ticks to a readable string
     */
    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
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
}