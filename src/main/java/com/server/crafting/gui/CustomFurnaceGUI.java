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
    
    // Updated slot positions for 45-slot GUI (5 rows) with better center positioning
    public static final int INPUT_SLOT = 11;        // Input item slot (left-center)
    public static final int FUEL_SLOT = 29;         // Fuel slot (below input with 1 slot gap)
    public static final int OUTPUT_SLOT = 15;       // Output slot (right-center)
    public static final int COOK_TIMER_SLOT = 13;   // Cook timer indicator (center)
    public static final int FUEL_TIMER_SLOT = 31;   // Fuel timer indicator (below center)
    private static long lastLoadLogTime = 0;  // Throttling for debug messages

    // Store active furnace GUIs for each player with their furnace location
    private static final Map<Player, Location> activeFurnaceGUIs = new HashMap<>();
    
    /**
     * Open the custom furnace GUI for a player at a specific location - ENHANCED: Access control
     */
    public static void openFurnaceGUI(Player player, Location furnaceLocation) {
        // CRITICAL: Check if furnace is available for access
        if (!CustomFurnaceManager.getInstance().acquireFurnaceAccess(furnaceLocation, player)) {
            // Player will already receive a message from acquireFurnaceAccess
            return;
        }
        
        // Create 5 row inventory (45 slots)
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        
        // Get or create furnace data for this location
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(furnaceLocation);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Opening furnace GUI for " + player.getName() + 
                " at " + furnaceLocation.getBlockX() + "," + furnaceLocation.getBlockY() + "," + furnaceLocation.getBlockZ());
        }
        
        // Create the simple decorative layout
        createSimpleFurnaceLayout(gui, furnaceData);
        
        // Load current furnace contents AFTER creating layout
        loadFurnaceContents(gui, furnaceData);
        
        // Store the GUI association
        activeFurnaceGUIs.put(player, furnaceLocation);
        
        // Open the inventory
        player.openInventory(gui);
    }

    /**
     * Remove a player's active furnace GUI - ENHANCED: Release furnace access
     */
    public static void removeActiveFurnaceGUI(Player player) {
        Location furnaceLocation = activeFurnaceGUIs.remove(player);
        if (furnaceLocation != null) {
            // CRITICAL: Release furnace access when GUI is closed
            CustomFurnaceManager.getInstance().releaseFurnaceAccess(furnaceLocation);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Removed GUI association and released access for " + player.getName());
            }
        }
    }
    
    /**
     * Create simple decorative border around the GUI - SIMPLIFIED
     */
    private static void fillSimpleFurnaceBorder(Inventory gui) {
        // Create simple glass panes
        ItemStack grayBorder = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack orangeBorder = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        
        // Fill all slots with gray glass initially
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, grayBorder);
        }
        
        // Simple border pattern - just outline
        // Top row
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, orangeBorder);
        }
        // Bottom row
        for (int i = 36; i < 45; i++) {
            gui.setItem(i, orangeBorder);
        }
        // Side borders
        for (int i = 1; i < 4; i++) {
            gui.setItem(i * 9, orangeBorder);        // Left border
            gui.setItem(i * 9 + 8, orangeBorder);    // Right border
        }
    }
    
    /**
     * Add simple slot indicators - SIMPLIFIED
     */
    private static void addSimpleSlotIndicators(Inventory gui) {
        // Input slot indicator (simple blue glass)
        ItemStack inputIndicator = createGlassPane(Material.BLUE_STAINED_GLASS_PANE, 
            ChatColor.BLUE + "Input Items");
        gui.setItem(10, inputIndicator);  // Left of input slot
        gui.setItem(2, inputIndicator);   // Above input slot
        
        // Fuel slot indicator (simple orange glass)
        ItemStack fuelIndicator = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, 
            ChatColor.GOLD + "Fuel Items");
        gui.setItem(28, fuelIndicator);  // Left of fuel slot
        gui.setItem(38, fuelIndicator);  // Below fuel slot
        
        // Output slot indicator (simple green glass)
        ItemStack outputIndicator = createGlassPane(Material.LIME_STAINED_GLASS_PANE, 
            ChatColor.GREEN + "Cooked Items");
        gui.setItem(6, outputIndicator);   // Above output slot
        gui.setItem(16, outputIndicator);  // Right of output slot
        
        // Simple arrow showing process
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.YELLOW + "→ Smelting Process");
        arrowMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Items cook from input to output"
        ));
        arrow.setItemMeta(arrowMeta);
        gui.setItem(12, arrow);  // Between input and cook timer
        gui.setItem(14, arrow);  // Between cook timer and output
    }
    
    /**
     * Add minimal decorative elements - SIMPLIFIED
     */
    private static void addSimpleDecorations(Inventory gui) {
        // Simple furnace icon at top center
        ItemStack furnaceIcon = new ItemStack(Material.FURNACE);
        ItemMeta iconMeta = furnaceIcon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.GOLD + "Enhanced Furnace");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Smelt items efficiently!",
            "",
            ChatColor.YELLOW + "• Place items in blue slot",
            ChatColor.YELLOW + "• Add fuel in orange slot",
            ChatColor.YELLOW + "• Collect results from green slot"
        ));
        furnaceIcon.setItemMeta(iconMeta);
        gui.setItem(4, furnaceIcon); // Top center
    }

    /**
     * Create the simple furnace layout - MUCH CLEANER
     */
    private static void createSimpleFurnaceLayout(Inventory gui, FurnaceData furnaceData) {
        // Fill simple border
        fillSimpleFurnaceBorder(gui);
        
        // Clear functional slots
        gui.setItem(INPUT_SLOT, null);
        gui.setItem(FUEL_SLOT, null);
        gui.setItem(OUTPUT_SLOT, null);
        
        // Add simple slot indicators
        addSimpleSlotIndicators(gui);
        
        // Add minimal decorative elements
        addSimpleDecorations(gui);
        
        // Initialize timer indicators
        updateCookTimer(gui, furnaceData);
        updateFuelTimer(gui, furnaceData);
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
     * Update the cook timer using the helper method
     */
    public static void updateCookTimer(Inventory gui, FurnaceData furnaceData) {
        ItemStack timerItem = createCookTimerItem(furnaceData);
        gui.setItem(COOK_TIMER_SLOT, timerItem);
    }

    /**
     * Update the fuel timer using the helper method
     */
    public static void updateFuelTimer(Inventory gui, FurnaceData furnaceData) {
        ItemStack timerItem = createFuelTimerItem(furnaceData);
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
     * Check if an inventory is a custom furnace GUI
     */
    public static boolean isCustomFurnaceGUI(Inventory inventory) {
        return inventory != null && 
               !inventory.getViewers().isEmpty() && 
               GUI_TITLE.equals(inventory.getViewers().get(0).getOpenInventory().getTitle());
    }

    /**
     * Create cook timer item without setting it in GUI - NEW METHOD
     */
    public static ItemStack createCookTimerItem(FurnaceData furnaceData) {
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
        
        return timerItem;
    }

    /**
     * Create fuel timer item without setting it in GUI - NEW METHOD
     */
    public static ItemStack createFuelTimerItem(FurnaceData furnaceData) {
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
        
        return timerItem;
    }


    
    /**
     * Create a progress bar string
     */
    public static String createProgressBar(double progress, int length, char filled, char empty) {
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
    public static String formatTime(int ticks) {
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
     * Get the active furnace GUIs map - UTILITY METHOD for manager access
     */
    public static Map<Player, Location> getActiveFurnaceGUIs() {
        return new HashMap<>(activeFurnaceGUIs);
    }
    
}