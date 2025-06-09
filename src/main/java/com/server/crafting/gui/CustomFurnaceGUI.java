package com.server.crafting.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.crafting.fuel.FuelRegistry;
import com.server.crafting.furnace.FurnaceData;
import com.server.crafting.furnace.FurnaceType;
import com.server.crafting.recipes.FurnaceRecipe;
import com.server.crafting.temperature.TemperatureSystem;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Dynamic GUI system for custom furnaces with type-specific layouts
 * Step 3: GUI foundation system
 */
public class CustomFurnaceGUI {
    
    // GUI title patterns for different furnace types
    private static final String GUI_TITLE_PATTERN = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "%s" + ChatColor.DARK_GRAY + " ✦";
    
    // Store active furnace GUIs for each player
    private static final Map<Player, Inventory> activeFurnaceGUIs = new ConcurrentHashMap<>();
    private static final Map<Player, FurnaceData> playerFurnaceData = new ConcurrentHashMap<>();
    
    // Dynamic slot layouts based on furnace type
    private static final Map<FurnaceType, FurnaceGUILayout> furnaceLayouts = new HashMap<>();
    
    static {
        initializeFurnaceLayouts();
    }
    
    /**
     * Layout configuration for different furnace types
     * Step 3: Dynamic layout system
     */
    public static class FurnaceGUILayout {
        public final int guiSize;
        public final int[] inputSlots;
        public final int[] fuelSlots;
        public final int[] outputSlots;
        public final int temperatureDisplaySlot;
        public final int fuelTimerSlot;
        public final int cookTimerSlot;
        public final int statusSlot;
        
        public FurnaceGUILayout(int guiSize, int[] inputSlots, int[] fuelSlots, int[] outputSlots,
                               int temperatureDisplaySlot, int fuelTimerSlot, int cookTimerSlot, int statusSlot) {
            this.guiSize = guiSize;
            this.inputSlots = inputSlots.clone();
            this.fuelSlots = fuelSlots.clone();
            this.outputSlots = outputSlots.clone();
            this.temperatureDisplaySlot = temperatureDisplaySlot;
            this.fuelTimerSlot = fuelTimerSlot;
            this.cookTimerSlot = cookTimerSlot;
            this.statusSlot = statusSlot;
        }
    }
    
    /**
     * Initialize GUI layouts for each furnace type
     * Step 3: Layout configuration
     */
    private static void initializeFurnaceLayouts() {
        // Stone Furnace (1 input, 1 fuel, 1 output) - 4 rows
        furnaceLayouts.put(FurnaceType.STONE_FURNACE, new FurnaceGUILayout(
            36, // 4 rows
            new int[]{11}, // input slots
            new int[]{13}, // fuel slots  
            new int[]{15}, // output slots
            4,  // temperature display
            22, // fuel timer
            20, // cook timer
            31  // status
        ));
        
        // Clay Kiln (2 input, 1 fuel, 2 output) - 5 rows
        furnaceLayouts.put(FurnaceType.CLAY_KILN, new FurnaceGUILayout(
            45, // 5 rows
            new int[]{10, 11}, // input slots
            new int[]{13}, // fuel slots
            new int[]{15, 16}, // output slots
            4,  // temperature display
            31, // fuel timer
            29, // cook timer
            40  // status
        ));
        
        // Iron Forge (2 input, 2 fuel, 2 output) - 5 rows
        furnaceLayouts.put(FurnaceType.IRON_FORGE, new FurnaceGUILayout(
            45, // 5 rows
            new int[]{10, 11}, // input slots
            new int[]{12, 13}, // fuel slots
            new int[]{15, 16}, // output slots
            4,  // temperature display
            31, // fuel timer
            29, // cook timer
            40  // status
        ));
        
        // Steel Furnace (3 input, 2 fuel, 3 output) - 6 rows
        furnaceLayouts.put(FurnaceType.STEEL_FURNACE, new FurnaceGUILayout(
            54, // 6 rows
            new int[]{10, 11, 12}, // input slots
            new int[]{19, 20}, // fuel slots
            new int[]{15, 16, 17}, // output slots
            4,  // temperature display
            40, // fuel timer
            38, // cook timer
            49  // status
        ));
        
        // Magmatic Forge (3 input, 3 fuel, 3 output) - 6 rows
        furnaceLayouts.put(FurnaceType.MAGMATIC_FORGE, new FurnaceGUILayout(
            54, // 6 rows
            new int[]{10, 11, 12}, // input slots
            new int[]{19, 20, 21}, // fuel slots
            new int[]{15, 16, 17}, // output slots
            4,  // temperature display
            40, // fuel timer
            38, // cook timer
            49  // status
        ));
        
        // Arcane Crucible (4 input, 3 fuel, 4 output) - 6 rows
        furnaceLayouts.put(FurnaceType.ARCANE_CRUCIBLE, new FurnaceGUILayout(
            54, // 6 rows
            new int[]{9, 10, 11, 12}, // input slots
            new int[]{19, 20, 21}, // fuel slots
            new int[]{14, 15, 16, 17}, // output slots
            4,  // temperature display
            40, // fuel timer
            38, // cook timer
            49  // status
        ));
        
        // Void Extractor (2 input, 2 fuel, 1 output) - 5 rows
        furnaceLayouts.put(FurnaceType.VOID_EXTRACTOR, new FurnaceGUILayout(
            45, // 5 rows
            new int[]{10, 11}, // input slots
            new int[]{12, 13}, // fuel slots
            new int[]{16}, // output slots
            4,  // temperature display
            31, // fuel timer
            29, // cook timer
            40  // status
        ));
    }
    
    /**
     * Open furnace GUI for a player
     * Step 3: Dynamic GUI opening
     */
    public static void openFurnaceGUI(Player player, FurnaceData furnaceData) {
        if (furnaceData == null) {
            player.sendMessage(ChatColor.RED + "Error: No furnace data found!");
            return;
        }
        
        FurnaceType furnaceType = furnaceData.getFurnaceType();
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceType);
        
        if (layout == null) {
            player.sendMessage(ChatColor.RED + "Error: Unsupported furnace type!");
            return;
        }
        
        // Create GUI with appropriate size
        String title = String.format(GUI_TITLE_PATTERN, furnaceType.getDisplayName());
        Inventory gui = Bukkit.createInventory(null, layout.guiSize, title);
        
        // Create the furnace layout
        createFurnaceLayout(gui, furnaceData, layout);
        
        // Store GUI and data references
        activeFurnaceGUIs.put(player, gui);
        playerFurnaceData.put(player, furnaceData);
        
        // Play sound effect
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, 1.0f);
        
        // Open the inventory
        player.openInventory(gui);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Custom Furnace] Opened " + furnaceType.getDisplayName() + 
                " GUI for " + player.getName());
        }
    }
    
    /**
     * Create the furnace layout with borders and functional slots
     * Step 3: Layout creation system
     */
    private static void createFurnaceLayout(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        // Fill all slots with border glass initially
        fillFurnaceBorder(gui);
        
        // Clear functional slots
        clearFunctionalSlots(gui, layout);
        
        // Load current furnace contents
        loadFurnaceContents(gui, furnaceData, layout);
        
        // Add slot indicators
        addSlotIndicators(gui, layout);
        
        // Add decorative elements
        addDecorativeElements(gui, furnaceData, layout);
        
        // Update real-time displays
        updateTemperatureDisplay(gui, furnaceData, layout);
        updateFuelTimer(gui, furnaceData, layout);
        updateCookTimer(gui, furnaceData, layout);
        updateStatusDisplay(gui, furnaceData, layout);
    }
    
    /**
     * Fill GUI border with decorative glass panes
     * Step 3: Border system
     */
    private static void fillFurnaceBorder(Inventory gui) {
        ItemStack borderGlass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack cornerGlass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        int size = gui.getSize();
        int rows = size / 9;
        
        // Fill all slots with border glass
        for (int i = 0; i < size; i++) {
            gui.setItem(i, borderGlass);
        }
        
        // Set corners with special glass
        gui.setItem(0, cornerGlass);
        gui.setItem(8, cornerGlass);
        gui.setItem(size - 9, cornerGlass);
        gui.setItem(size - 1, cornerGlass);
    }
    
    /**
     * Clear functional slots for item placement
     * Step 3: Slot clearing
     */
    private static void clearFunctionalSlots(Inventory gui, FurnaceGUILayout layout) {
        // Clear input slots
        for (int slot : layout.inputSlots) {
            gui.setItem(slot, null);
        }
        
        // Clear fuel slots
        for (int slot : layout.fuelSlots) {
            gui.setItem(slot, null);
        }
        
        // Clear output slots
        for (int slot : layout.outputSlots) {
            gui.setItem(slot, null);
        }
    }
    
    /**
     * Load current furnace contents into GUI
     * Step 3: Content loading
     */
    private static void loadFurnaceContents(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        // Load input items
        for (int i = 0; i < layout.inputSlots.length; i++) {
            ItemStack inputItem = furnaceData.getInputSlot(i);
            if (inputItem != null) {
                gui.setItem(layout.inputSlots[i], inputItem);
            }
        }
        
        // Load fuel items with enhanced lore
        for (int i = 0; i < layout.fuelSlots.length; i++) {
            ItemStack fuelItem = furnaceData.getFuelSlot(i);
            if (fuelItem != null) {
                // Enhance fuel item with temperature information
                ItemStack enhancedFuel = FuelRegistry.getInstance().enhanceFuelItem(fuelItem);
                gui.setItem(layout.fuelSlots[i], enhancedFuel);
            }
        }
        
        // Load output items
        for (int i = 0; i < layout.outputSlots.length; i++) {
            ItemStack outputItem = furnaceData.getOutputSlot(i);
            if (outputItem != null) {
                gui.setItem(layout.outputSlots[i], outputItem);
            }
        }
    }
    
    /**
     * Add visual slot indicators
     * Step 3: Visual indicators
     */
    private static void addSlotIndicators(Inventory gui, FurnaceGUILayout layout) {
        // Input slot indicators (above input slots)
        for (int slot : layout.inputSlots) {
            if (slot >= 9) { // Make sure we don't go above row 0
                gui.setItem(slot - 9, createSlotIndicator(Material.ORANGE_STAINED_GLASS_PANE, 
                    ChatColor.GOLD + "⬇ Input Slot", 
                    Arrays.asList(ChatColor.GRAY + "Place items to smelt here")));
            }
        }
        
        // Fuel slot indicators (below fuel slots if possible)
        for (int slot : layout.fuelSlots) {
            if (slot + 9 < gui.getSize()) { // Make sure we don't go below last row
                gui.setItem(slot + 9, createSlotIndicator(Material.RED_STAINED_GLASS_PANE, 
                    ChatColor.RED + "⬆ Fuel Slot", 
                    Arrays.asList(
                        ChatColor.GRAY + "Place fuel items here",
                        ChatColor.YELLOW + "• Coal, Wood, Lava Bucket",
                        ChatColor.YELLOW + "• Custom fuels supported"
                    )));
            }
        }
        
        // Output slot indicators (above output slots)
        for (int slot : layout.outputSlots) {
            if (slot >= 9) { // Make sure we don't go above row 0
                gui.setItem(slot - 9, createSlotIndicator(Material.GREEN_STAINED_GLASS_PANE, 
                    ChatColor.GREEN + "⬇ Output Slot", 
                    Arrays.asList(ChatColor.GRAY + "Smelted items appear here")));
            }
        }
    }
    
    /**
     * Add decorative elements specific to furnace type
     * Step 3: Decorative system
     */
    private static void addDecorativeElements(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        FurnaceType type = furnaceData.getFurnaceType();
        
        // Add furnace type specific decorations
        Material decorativeMaterial;
        String decorativeName;
        
        switch (type) {
            case STONE_FURNACE:
                decorativeMaterial = Material.COBBLESTONE;
                decorativeName = ChatColor.GRAY + "Stone Construction";
                break;
            case CLAY_KILN:
                decorativeMaterial = Material.TERRACOTTA;
                decorativeName = ChatColor.GOLD + "Clay Construction";
                break;
            case IRON_FORGE:
                decorativeMaterial = Material.IRON_BLOCK;
                decorativeName = ChatColor.WHITE + "Iron Reinforcement";
                break;
            case STEEL_FURNACE:
                decorativeMaterial = Material.ANVIL;
                decorativeName = ChatColor.DARK_GRAY + "Steel Framework";
                break;
            case MAGMATIC_FORGE:
                decorativeMaterial = Material.MAGMA_BLOCK;
                decorativeName = ChatColor.RED + "Magmatic Core";
                break;
            case ARCANE_CRUCIBLE:
                decorativeMaterial = Material.ENCHANTING_TABLE;
                decorativeName = ChatColor.DARK_PURPLE + "Arcane Matrix";
                break;
            case VOID_EXTRACTOR:
                decorativeMaterial = Material.END_STONE;
                decorativeName = ChatColor.DARK_AQUA + "Void Conduit";
                break;
            default:
                decorativeMaterial = Material.FURNACE;
                decorativeName = ChatColor.GRAY + "Standard Furnace";
                break;
        }
        
        // Place decorative elements in specific corners if space allows
        int size = gui.getSize();
        if (size >= 45) { // 5+ rows
            ItemStack decoration = new ItemStack(decorativeMaterial);
            ItemMeta meta = decoration.getItemMeta();
            meta.setDisplayName(decorativeName);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Furnace Type: " + type.getColoredName(),
                ChatColor.GRAY + "Decorative Element"
            ));
            decoration.setItemMeta(meta);
            
            // Place in bottom corners if they're not functional slots
            if (!isSlotFunctional(size - 18, layout)) {
                gui.setItem(size - 18, decoration);
            }
            if (!isSlotFunctional(size - 10, layout)) {
                gui.setItem(size - 10, decoration);
            }
        }
    }
    
    /**
     * Update temperature display
     * Step 3: Real-time temperature display
     */
    private static void updateTemperatureDisplay(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        int currentTemp = furnaceData.getCurrentTemperature();
        int targetTemp = furnaceData.getTargetTemperature();
        FurnaceType type = furnaceData.getFurnaceType();
        
        // Choose thermometer material based on temperature
        Material thermometerMaterial;
        ChatColor tempColor;
        
        if (currentTemp <= 100) {
            thermometerMaterial = Material.BLUE_ICE;
            tempColor = ChatColor.AQUA;
        } else if (currentTemp <= 400) {
            thermometerMaterial = Material.YELLOW_TERRACOTTA;
            tempColor = ChatColor.YELLOW;
        } else if (currentTemp <= 800) {
            thermometerMaterial = Material.ORANGE_TERRACOTTA;
            tempColor = ChatColor.GOLD;
        } else if (currentTemp <= 1500) {
            thermometerMaterial = Material.RED_TERRACOTTA;
            tempColor = ChatColor.RED;
        } else {
            thermometerMaterial = Material.MAGMA_BLOCK;
            tempColor = ChatColor.DARK_RED;
        }
        
        ItemStack thermometer = new ItemStack(thermometerMaterial);
        ItemMeta meta = thermometer.getItemMeta();
        meta.setDisplayName(tempColor + "🌡 Temperature Monitor");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + TemperatureSystem.formatTemperature(currentTemp));
        lore.add(ChatColor.GRAY + "Target: " + TemperatureSystem.formatTemperature(targetTemp));
        lore.add("");
        lore.add(type.getTemperatureRange());
        lore.add("");
        
        // Temperature status
        if (furnaceData.isOverheating()) {
            lore.add(ChatColor.RED + "⚠ OVERHEATING! ⚠");
            lore.add(ChatColor.RED + "Time: " + furnaceData.getOverheatingTime() + " ticks");
        } else if (furnaceData.isHeating()) {
            lore.add(ChatColor.YELLOW + "🔥 Heating Up");
        } else if (furnaceData.isCooling()) {
            lore.add(ChatColor.AQUA + "❄ Cooling Down");
        } else {
            lore.add(ChatColor.GREEN + "✓ Stable Temperature");
        }
        
        // Safety warnings
        if (furnaceData.willExplode()) {
            lore.add("");
            lore.add(ChatColor.DARK_RED + "💥 EXPLOSION IMMINENT!");
            lore.add(ChatColor.RED + "Countdown: " + furnaceData.getExplosionCountdown() + " ticks");
        } else if (furnaceData.isEmergencyShutdown()) {
            lore.add("");
            lore.add(ChatColor.DARK_RED + "🛑 EMERGENCY SHUTDOWN");
            lore.add(ChatColor.YELLOW + "Cool down to restart");
        }
        
        meta.setLore(lore);
        thermometer.setItemMeta(meta);
        
        gui.setItem(layout.temperatureDisplaySlot, thermometer);
    }
    
    /**
     * Update fuel timer display
     * Step 3: Real-time fuel display
     */
    private static void updateFuelTimer(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        ItemStack timerItem;
        
        if (furnaceData.hasFuel()) {
            // Calculate fuel remaining percentage
            double fuelProgress = furnaceData.getFuelProgress();
            
            // Choose flame material based on fuel level
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
            int remainingFuelTime = furnaceData.getFuelTime();
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Fuel Level: " + ChatColor.WHITE + String.format("%.1f%%", fuelProgress * 100));
            lore.add(ChatColor.GRAY + "Fuel Time: " + ChatColor.WHITE + formatTime(remainingFuelTime));
            lore.add("");
            lore.add(ChatColor.GOLD + fuelBar);
            lore.add("");
            lore.add(flameColor + statusText);
            
            meta.setLore(lore);
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
        
        gui.setItem(layout.fuelTimerSlot, timerItem);
    }
    
    /**
     * Update cooking timer display - ENHANCED: Recipe information
     * Step 4: Recipe progress display
     */
    private static void updateCookTimer(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        ItemStack timerItem;
        
        if (furnaceData.isActive()) {
            FurnaceRecipe currentRecipe = furnaceData.getCurrentRecipe();
            
            if (currentRecipe != null) {
                // Calculate cooking progress
                double cookProgress = furnaceData.getCookProgress();
                
                // Choose material based on recipe type
                Material cookMaterial = getRecipeTypeMaterial(currentRecipe.getRecipeType());
                ChatColor cookColor = getRecipeTypeColor(currentRecipe.getRecipeType());
                
                timerItem = new ItemStack(cookMaterial);
                ItemMeta meta = timerItem.getItemMeta();
                meta.setDisplayName(cookColor + "⚗ " + currentRecipe.getDisplayName());
                
                String cookBar = createProgressBar(cookProgress, 15, '█', '░');
                int remainingTime = furnaceData.getEstimatedTimeRemaining();
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Recipe Type: " + ChatColor.WHITE + currentRecipe.getRecipeType().getDisplayName());
                lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + String.format("%.1f%%", cookProgress * 100));
                lore.add(ChatColor.GRAY + "Time Left: " + ChatColor.WHITE + formatTime(remainingTime));
                lore.add("");
                lore.add(ChatColor.GREEN + cookBar);
                lore.add("");
                lore.add(ChatColor.GRAY + "Required Temp: " + currentRecipe.getFormattedTemperature());
                lore.add(ChatColor.GRAY + "Cook Time: " + ChatColor.WHITE + currentRecipe.getFormattedCookTime());
                
                // Temperature efficiency indicator
                int currentTemp = furnaceData.getCurrentTemperature();
                double efficiency = com.server.crafting.temperature.TemperatureSystem
                    .getTemperatureEfficiency(currentTemp, currentRecipe.getRequiredTemperature());
                
                if (efficiency > 1.0) {
                    lore.add("");
                    lore.add(ChatColor.GREEN + "⚡ Temperature Bonus: " + String.format("%.0f%%", (efficiency - 1.0) * 100));
                }
                
                if (furnaceData.isPaused()) {
                    lore.add("");
                    lore.add(ChatColor.RED + "⏸ Paused - Temperature too low");
                    lore.add(ChatColor.RED + "Current: " + furnaceData.getFormattedTemperature());
                    lore.add(ChatColor.RED + "Required: " + currentRecipe.getFormattedTemperature());
                }
                
                meta.setLore(lore);
                timerItem.setItemMeta(meta);
            } else {
                // Active but no recipe found
                timerItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = timerItem.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "⚠ No Valid Recipe");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current inputs don't match any recipe",
                    ChatColor.YELLOW + "Check recipe requirements"
                ));
                timerItem.setItemMeta(meta);
            }
        } else {
            // Not cooking - show available recipes
            timerItem = new ItemStack(Material.BOOK);
            ItemMeta meta = timerItem.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "📖 Recipe Book");
            
            // Get current inputs and suggest recipes
            List<org.bukkit.inventory.ItemStack> currentInputs = new ArrayList<>();
            for (int i = 0; i < furnaceData.getFurnaceType().getInputSlots(); i++) {
                org.bukkit.inventory.ItemStack item = furnaceData.getInputSlot(i);
                if (item != null && item.getType() != Material.AIR) {
                    currentInputs.add(item);
                }
            }
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Add items and fuel to start cooking");
            lore.add("");
            
            if (!currentInputs.isEmpty()) {
                // Find potential recipes for current inputs
                com.server.crafting.recipes.FurnaceRecipe matchingRecipe = 
                    com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().findRecipe(currentInputs);
                
                if (matchingRecipe != null) {
                    lore.add(ChatColor.GREEN + "✓ Recipe Available:");
                    lore.add(ChatColor.WHITE + "  " + matchingRecipe.getDisplayName());
                    lore.add(ChatColor.GRAY + "  " + matchingRecipe.getFormattedTemperature());
                    lore.add(ChatColor.GRAY + "  " + matchingRecipe.getFormattedCookTime());
                } else {
                    lore.add(ChatColor.YELLOW + "No recipe found for current items");
                }
            } else {
                // Show furnace capabilities
                lore.add(ChatColor.AQUA + "Furnace Capabilities:");
                lore.add(ChatColor.GRAY + "Max Temperature: " + 
                    com.server.crafting.temperature.TemperatureSystem.formatTemperature(
                        furnaceData.getFurnaceType().getMaxTemperature()));
                
                // Show compatible recipe types
                List<com.server.crafting.recipes.FurnaceRecipe> compatibleRecipes = 
                    com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance()
                        .getRecipesForTemperature(furnaceData.getFurnaceType().getMaxTemperature());
                
                Set<com.server.crafting.recipes.FurnaceRecipe.RecipeType> compatibleTypes = new HashSet<>();
                for (com.server.crafting.recipes.FurnaceRecipe recipe : compatibleRecipes) {
                    compatibleTypes.add(recipe.getRecipeType());
                }
                
                lore.add("");
                lore.add(ChatColor.GOLD + "Compatible Recipe Types:");
                for (com.server.crafting.recipes.FurnaceRecipe.RecipeType type : compatibleTypes) {
                    lore.add(ChatColor.WHITE + "• " + type.getDisplayName());
                }
            }
            
            meta.setLore(lore);
            timerItem.setItemMeta(meta);
        }
        
        gui.setItem(layout.cookTimerSlot, timerItem);
    }

    /**
     * Get material for recipe type display
     */
    private static Material getRecipeTypeMaterial(com.server.crafting.recipes.FurnaceRecipe.RecipeType type) {
        switch (type) {
            case SMELTING: return Material.FURNACE;
            case ALLOYING: return Material.ANVIL;
            case REFINING: return Material.CAULDRON;
            case CRYSTALLIZATION: return Material.AMETHYST_CLUSTER;
            case EXTRACTION: return Material.BREWING_STAND;
            case TRANSMUTATION: return Material.ENCHANTING_TABLE;
            default: return Material.CRAFTING_TABLE;
        }
    }

    /**
     * Get color for recipe type display
     */
    private static ChatColor getRecipeTypeColor(com.server.crafting.recipes.FurnaceRecipe.RecipeType type) {
        switch (type) {
            case SMELTING: return ChatColor.GRAY;
            case ALLOYING: return ChatColor.YELLOW;
            case REFINING: return ChatColor.AQUA;
            case CRYSTALLIZATION: return ChatColor.LIGHT_PURPLE;
            case EXTRACTION: return ChatColor.GREEN;
            case TRANSMUTATION: return ChatColor.DARK_PURPLE;
            default: return ChatColor.WHITE;
        }
    }
    
    /**
     * Update status display
     * Step 3: Status information display
     */
    private static void updateStatusDisplay(Inventory gui, FurnaceData furnaceData, FurnaceGUILayout layout) {
        FurnaceType type = furnaceData.getFurnaceType();
        
        Material statusMaterial;
        ChatColor statusColor;
        String statusTitle;
        
        if (furnaceData.isEmergencyShutdown()) {
            statusMaterial = Material.BARRIER;
            statusColor = ChatColor.DARK_RED;
            statusTitle = "🛑 Emergency Shutdown";
        } else if (furnaceData.willExplode()) {
            statusMaterial = Material.TNT;
            statusColor = ChatColor.DARK_RED;
            statusTitle = "💥 Critical Danger";
        } else if (furnaceData.isOverheating()) {
            statusMaterial = Material.FIRE_CHARGE;
            statusColor = ChatColor.RED;
            statusTitle = "⚠ Overheating";
        } else if (furnaceData.isActive()) {
            statusMaterial = Material.EMERALD;
            statusColor = ChatColor.GREEN;
            statusTitle = "✓ Operating";
        } else if (furnaceData.hasFuel()) {
            statusMaterial = Material.YELLOW_DYE;
            statusColor = ChatColor.YELLOW;
            statusTitle = "⏳ Ready to Cook";
        } else {
            statusMaterial = Material.GRAY_DYE;
            statusColor = ChatColor.GRAY;
            statusTitle = "⚫ Idle";
        }
        
        ItemStack statusItem = new ItemStack(statusMaterial);
        ItemMeta meta = statusItem.getItemMeta();
        meta.setDisplayName(statusColor + statusTitle);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Furnace: " + type.getColoredName());
        lore.add("");
        
        // Current status details
        lore.add(ChatColor.AQUA + "Current Status:");
        lore.add(ChatColor.GRAY + "• Temperature: " + furnaceData.getFormattedTemperature());
        lore.add(ChatColor.GRAY + "• Fuel: " + (furnaceData.hasFuel() ? 
            ChatColor.GREEN + "Available" : ChatColor.RED + "Empty"));
        lore.add(ChatColor.GRAY + "• Cooking: " + (furnaceData.isActive() ? 
            ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
        
        // Slot configuration
        lore.add("");
        lore.add(ChatColor.GOLD + "Configuration:");
        lore.add(ChatColor.GRAY + "• Input Slots: " + ChatColor.WHITE + type.getInputSlots());
        lore.add(ChatColor.GRAY + "• Fuel Slots: " + ChatColor.WHITE + type.getFuelSlots());
        lore.add(ChatColor.GRAY + "• Output Slots: " + ChatColor.WHITE + type.getOutputSlots());
        
        meta.setLore(lore);
        statusItem.setItemMeta(meta);
        
        gui.setItem(layout.statusSlot, statusItem);
    }
    
    /**
     * Check if a slot is functional (used for item placement)
     * Step 3: Slot validation
     */
    private static boolean isSlotFunctional(int slot, FurnaceGUILayout layout) {
        // Check input slots
        for (int inputSlot : layout.inputSlots) {
            if (inputSlot == slot) return true;
        }
        
        // Check fuel slots
        for (int fuelSlot : layout.fuelSlots) {
            if (fuelSlot == slot) return true;
        }
        
        // Check output slots
        for (int outputSlot : layout.outputSlots) {
            if (outputSlot == slot) return true;
        }
        
        return false;
    }
    
    /**
     * Check if a slot is an input slot
     * Step 3: Slot type checking
     */
    public static boolean isInputSlot(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return false;
        
        for (int inputSlot : layout.inputSlots) {
            if (inputSlot == slot) return true;
        }
        return false;
    }
    
    /**
     * Check if a slot is a fuel slot
     * Step 3: Slot type checking
     */
    public static boolean isFuelSlot(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return false;
        
        for (int fuelSlot : layout.fuelSlots) {
            if (fuelSlot == slot) return true;
        }
        return false;
    }
    
    /**
     * Check if a slot is an output slot
     * Step 3: Slot type checking
     */
    public static boolean isOutputSlot(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return false;
        
        for (int outputSlot : layout.outputSlots) {
            if (outputSlot == slot) return true;
        }
        return false;
    }
    
    /**
     * Get input slot index from GUI slot
     * Step 3: Slot mapping
     */
    public static int getInputSlotIndex(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return -1;
        
        for (int i = 0; i < layout.inputSlots.length; i++) {
            if (layout.inputSlots[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Get fuel slot index from GUI slot
     * Step 3: Slot mapping
     */
    public static int getFuelSlotIndex(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return -1;
        
        for (int i = 0; i < layout.fuelSlots.length; i++) {
            if (layout.fuelSlots[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Get output slot index from GUI slot
     * Step 3: Slot mapping
     */
    public static int getOutputSlotIndex(int slot, FurnaceData furnaceData) {
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) return -1;
        
        for (int i = 0; i < layout.outputSlots.length; i++) {
            if (layout.outputSlots[i] == slot) return i;
        }
        return -1;
    }
    
    /**
     * Update GUI for all viewers of a specific furnace - FIXED
     * Step 3: Real-time updates
     */
    public static void updateFurnaceGUI(FurnaceData furnaceData) {
        // Find all players viewing this furnace
        for (Map.Entry<Player, FurnaceData> entry : playerFurnaceData.entrySet()) {
            if (entry.getValue() == furnaceData) {
                Player player = entry.getKey();
                Inventory gui = activeFurnaceGUIs.get(player);
                
                if (gui != null && player.getOpenInventory().getTopInventory() == gui) {
                    FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
                    if (layout != null) {
                        // CRITICAL FIX: Reload items from furnace data first
                        loadFurnaceContents(gui, furnaceData, layout);
                        
                        // Then update real-time displays
                        updateTemperatureDisplay(gui, furnaceData, layout);
                        updateFuelTimer(gui, furnaceData, layout);
                        updateCookTimer(gui, furnaceData, layout);
                        updateStatusDisplay(gui, furnaceData, layout);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI,
                                "[Custom Furnace GUI] Updated GUI for player " + player.getName() + 
                                " viewing " + furnaceData.getFurnaceType().getDisplayName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Sync GUI contents back to furnace data - ENHANCED with debugging
     * Step 3: Data synchronization
     */
    public static void syncGUIToFurnaceData(Player player) {
        Inventory gui = activeFurnaceGUIs.get(player);
        FurnaceData furnaceData = playerFurnaceData.get(player);
        
        if (gui == null || furnaceData == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Custom Furnace GUI] Sync failed - GUI or FurnaceData is null for " + player.getName());
            }
            return;
        }
        
        FurnaceGUILayout layout = furnaceLayouts.get(furnaceData.getFurnaceType());
        if (layout == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Custom Furnace GUI] Sync failed - Layout is null for " + furnaceData.getFurnaceType());
            }
            return;
        }
        
        // Sync input slots
        for (int i = 0; i < layout.inputSlots.length; i++) {
            ItemStack item = gui.getItem(layout.inputSlots[i]);
            furnaceData.setInputSlot(i, item);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && item != null) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Custom Furnace GUI] Synced input slot " + i + ": " + 
                    item.getType().name() + " x" + item.getAmount());
            }
        }
        
        // Sync fuel slots
        for (int i = 0; i < layout.fuelSlots.length; i++) {
            ItemStack item = gui.getItem(layout.fuelSlots[i]);
            furnaceData.setFuelSlot(i, item);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && item != null) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Custom Furnace GUI] Synced fuel slot " + i + ": " + 
                    item.getType().name() + " x" + item.getAmount());
            }
        }
        
        // Sync output slots
        for (int i = 0; i < layout.outputSlots.length; i++) {
            ItemStack item = gui.getItem(layout.outputSlots[i]);
            furnaceData.setOutputSlot(i, item);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && item != null) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Custom Furnace GUI] Synced output slot " + i + ": " + 
                    item.getType().name() + " x" + item.getAmount());
            }
        }
    }
    
    /**
     * Get active furnace GUI for a player
     * Step 3: GUI access
     */
    public static Inventory getActiveFurnaceGUI(Player player) {
        return activeFurnaceGUIs.get(player);
    }
    
    /**
     * Get furnace data for a player's open GUI
     * Step 3: Data access
     */
    public static FurnaceData getPlayerFurnaceData(Player player) {
        return playerFurnaceData.get(player);
    }
    
    /**
     * Check if an inventory is a custom furnace GUI
     * Step 3: GUI validation
     */
    public static boolean isFurnaceGUI(Inventory inventory) {
        if (inventory == null) return false;
        
        // Check if this inventory is one of our active furnace GUIs
        return activeFurnaceGUIs.containsValue(inventory);
    }
    
    /**
     * Remove player's active furnace GUI
     * Step 3: Cleanup
     */
    public static void removeActiveFurnaceGUI(Player player) {
        activeFurnaceGUIs.remove(player);
        playerFurnaceData.remove(player);
    }
    
    // Utility methods
    
    /**
     * Create a glass pane with specified properties
     */
    private static ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Create a slot indicator item
     */
    private static ItemStack createSlotIndicator(Material material, String name, List<String> lore) {
        ItemStack indicator = new ItemStack(material);
        ItemMeta meta = indicator.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        indicator.setItemMeta(meta);
        return indicator;
    }
    
    /**
     * Create a progress bar string
     */
    private static String createProgressBar(double progress, int length, char filled, char empty) {
        StringBuilder bar = new StringBuilder();
        int filledBars = (int) Math.round(progress * length);
        
        bar.append(ChatColor.GRAY + "[");
        
        for (int i = 0; i < length; i++) {
            if (i < filledBars) {
                if (progress < 0.33) {
                    bar.append(ChatColor.RED);
                } else if (progress < 0.66) {
                    bar.append(ChatColor.YELLOW);
                } else {
                    bar.append(ChatColor.GREEN);
                }
                bar.append(filled);
            } else {
                bar.append(ChatColor.DARK_GRAY).append(empty);
            }
        }
        
        bar.append(ChatColor.GRAY + "]");
        return bar.toString();
    }
    
    /**
     * Format time in ticks to readable format
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
}