package com.server.crafting.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.crafting.manager.CustomCraftingManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Auto-crafting GUI that shows all possible craftable items from player's inventory
 */
public class AutoCraftingGUI {
    
    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + "Auto Crafting" + ChatColor.DARK_GRAY + " ✦";
    public static final String OVERFLOW_GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + "Auto Crafting" + ChatColor.DARK_GRAY + " ✦ " + ChatColor.YELLOW + "(Page 2)";
    
    // Navigation and control slots
    private static final int BACK_ARROW_SLOT = 45; // Bottom left - back to 3x3 crafting
    private static final int NEXT_PAGE_SLOT = 53; // Bottom right - next page
    private static final int REFRESH_SLOT = 49; // Bottom center - refresh recipes
    
    // Available crafting slots (avoiding borders and navigation)
    private static final int[] CRAFTING_DISPLAY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 2
        19, 20, 21, 22, 23, 24, 25,  // Row 3
        28, 29, 30, 31, 32, 33, 34,  // Row 4
        37, 38, 39, 40, 41, 42, 43   // Row 5
    };
    
    // Store active auto-crafting inventories for each player
    private static final Map<Player, Inventory> activeAutoCraftingGUIs = new HashMap<>();
    private static final Map<Player, Inventory> activeOverflowGUIs = new HashMap<>();
    private static final Map<Player, List<CraftableItem>> playerCraftableItems = new HashMap<>();
    
    /**
     * Data structure to hold craftable item information
     */
    public static class CraftableItem {
        public final ItemStack result;
        public final ItemStack[] recipe;
        public final int maxCraftable;
        public final boolean isCustomRecipe;
        public final String recipeType; // "shaped", "shapeless", "custom"
        
        public CraftableItem(ItemStack result, ItemStack[] recipe, int maxCraftable, boolean isCustomRecipe, String recipeType) {
            this.result = result.clone();
            this.recipe = recipe != null ? recipe.clone() : new ItemStack[9];
            this.maxCraftable = maxCraftable;
            this.isCustomRecipe = isCustomRecipe;
            this.recipeType = recipeType;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CraftableItem)) return false;
            CraftableItem other = (CraftableItem) obj;
            return this.result.isSimilar(other.result);
        }
        
        @Override
        public int hashCode() {
            return result.getType().hashCode() + 
                   (result.hasItemMeta() && result.getItemMeta().hasCustomModelData() ? 
                    result.getItemMeta().getCustomModelData() : 0);
        }
    }
    
    /**
     * Open the auto-crafting GUI for a player - FIXED: Better data management
     */
    public static void openAutoCraftingGUI(Player player) {
        // Analyze player's inventory for craftable items
        List<CraftableItem> craftableItems = analyzeCraftableItems(player);
        
        // CRITICAL FIX: Always store the craftable items BEFORE creating the GUI
        playerCraftableItems.put(player, craftableItems);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Stored " + craftableItems.size() + " craftable items for " + player.getName());
        }
        
        // Create 6 row inventory (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Create the layout
        createAutoCraftingLayout(gui, player, craftableItems, false);
        
        // Store the GUI for this player
        activeAutoCraftingGUIs.put(player, gui);
        
        // Open the inventory
        player.openInventory(gui);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Opened GUI for " + player.getName() + " with " + craftableItems.size() + " craftable items");
        }
    }

    /**
     * Get craftable item from clicked slot - FIXED with better debugging and null checks
     */
    public static CraftableItem getCraftableItemFromSlot(Player player, int slot, boolean isOverflow) {
        List<CraftableItem> craftableItems = playerCraftableItems.get(player);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            if (craftableItems == null) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] No craftable items found for player " + player.getName() + 
                    " - playerCraftableItems map is null for this player");
            } else {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Found " + craftableItems.size() + " craftable items for player " + player.getName());
            }
        }
        
        if (craftableItems == null) {
            // CRITICAL FIX: If craftable items is null, try to regenerate it
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Attempting to regenerate craftable items for " + player.getName());
            }
            
            craftableItems = analyzeCraftableItems(player);
            playerCraftableItems.put(player, craftableItems);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Regenerated " + craftableItems.size() + " craftable items");
            }
        }
        
        if (craftableItems.isEmpty()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Craftable items list is empty for player " + player.getName());
            }
            return null;
        }
        
        // Find the index in the crafting display slots
        int slotIndex = -1;
        for (int i = 0; i < CRAFTING_DISPLAY_SLOTS.length; i++) {
            if (CRAFTING_DISPLAY_SLOTS[i] == slot) {
                slotIndex = i;
                break;
            }
        }
        
        if (slotIndex == -1) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Slot " + slot + " is not a valid crafting slot. Valid slots: " + 
                    java.util.Arrays.toString(CRAFTING_DISPLAY_SLOTS));
            }
            return null;
        }
        
        // Calculate the actual index in the craftable items list
        int actualIndex = isOverflow ? CRAFTING_DISPLAY_SLOTS.length + slotIndex : slotIndex;
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Slot " + slot + " -> slotIndex " + slotIndex + " -> actualIndex " + actualIndex + 
                " (overflow: " + isOverflow + ", total items: " + craftableItems.size() + ")");
        }
        
        if (actualIndex >= craftableItems.size()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Index " + actualIndex + " is out of bounds for " + craftableItems.size() + " items");
            }
            return null;
        }
        
        CraftableItem item = craftableItems.get(actualIndex);
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            String itemName = item.result.hasItemMeta() && item.result.getItemMeta().hasDisplayName() ?
                            ChatColor.stripColor(item.result.getItemMeta().getDisplayName()) :
                            item.result.getType().name();
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Found craftable item: " + itemName + " (max: " + item.maxCraftable + ")");
        }
        
        return item;
    }
    
    /**
     * Open the overflow GUI (page 2) for a player
     */
    public static void openOverflowGUI(Player player) {
        List<CraftableItem> craftableItems = playerCraftableItems.get(player);
        if (craftableItems == null || craftableItems.size() <= CRAFTING_DISPLAY_SLOTS.length) {
            player.sendMessage(ChatColor.RED + "No additional items to display!");
            return;
        }
        
        // Create 6 row inventory (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, OVERFLOW_GUI_TITLE);
        
        // Create the layout for overflow items
        createAutoCraftingLayout(gui, player, craftableItems, true);
        
        // Store the overflow GUI
        activeOverflowGUIs.put(player, gui);
        
        // Open the inventory
        player.openInventory(gui);
    }
    
    /**
     * Create the auto-crafting layout
     */
    private static void createAutoCraftingLayout(Inventory gui, Player player, List<CraftableItem> craftableItems, boolean isOverflow) {
        // Fill border
        fillAutoCraftingBorder(gui);
        
        // Add decorative elements
        addAutoCraftingDecorations(gui, isOverflow);
        
        // Add navigation
        addAutoCraftingNavigation(gui, craftableItems.size(), isOverflow);
        
        // Display craftable items
        displayCraftableItems(gui, craftableItems, isOverflow);
    }
    
    /**
     * Create decorative border around the GUI
     */
    private static void fillAutoCraftingBorder(Inventory gui) {
        // Create different glass pane types for visual appeal
        ItemStack purpleBorder = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack magentaBorder = createGlassPane(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        ItemStack pinkBorder = createGlassPane(Material.PINK_STAINED_GLASS_PANE, " ");
        ItemStack blackFiller = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // Fill all slots with black glass initially
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, blackFiller);
        }
        
        // Create decorative border pattern
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, i % 2 == 0 ? purpleBorder : magentaBorder);
            gui.setItem(45 + i, i % 2 == 0 ? magentaBorder : purpleBorder);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, pinkBorder);
            gui.setItem(i * 9 + 8, pinkBorder);
        }
        
        // Corners with special color
        gui.setItem(0, pinkBorder);
        gui.setItem(8, pinkBorder);
        gui.setItem(45, pinkBorder);
        gui.setItem(53, pinkBorder);
    }
    
    /**
     * Add decorative elements
     */
    private static void addAutoCraftingDecorations(Inventory gui, boolean isOverflow) {
        // Add auto-crafting icon
        ItemStack icon = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta iconMeta = icon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.AQUA + "✦ Auto Crafting " + (isOverflow ? "- Page 2 " : "") + "✦");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Automatically craft items from your inventory!",
            "",
            ChatColor.YELLOW + "• Click an item to craft 1",
            ChatColor.YELLOW + "• Shift-click to craft maximum amount",
            ChatColor.YELLOW + "• Items are added directly to your inventory",
            ChatColor.YELLOW + "• Uses materials from your inventory",
            "",
            ChatColor.GRAY + "Green border = Can craft multiple",
            ChatColor.GRAY + "Yellow border = Can craft 1",
            ChatColor.GRAY + "Red border = Missing ingredients"
        ));
        icon.setItemMeta(iconMeta);
        gui.setItem(4, icon); // Top center
    }
    
    /**
     * Add navigation elements
     */
    private static void addAutoCraftingNavigation(Inventory gui, int totalItems, boolean isOverflow) {
        // Back arrow (always present)
        ItemStack backArrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta backMeta = backArrow.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "← Back to 3x3 Crafting");
        backMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Return to the standard",
            ChatColor.GRAY + "3x3 crafting table",
            "",
            ChatColor.YELLOW + "Click to go back!"
        ));
        backArrow.setItemMeta(backMeta);
        gui.setItem(BACK_ARROW_SLOT, backArrow);
        
        // Refresh button
        ItemStack refresh = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.GREEN + "⟲ Refresh Recipes");
        refreshMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Re-analyze your inventory",
            ChatColor.GRAY + "for craftable items",
            "",
            ChatColor.GREEN + "Click to refresh!"
        ));
        refresh.setItemMeta(refreshMeta);
        gui.setItem(REFRESH_SLOT, refresh);
        
        // Next page arrow (if needed)
        if (!isOverflow && totalItems > CRAFTING_DISPLAY_SLOTS.length) {
            ItemStack nextArrow = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextArrow.getItemMeta();
            nextMeta.setDisplayName(ChatColor.AQUA + "→ More Items (Page 2)");
            nextMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "View additional craftable items",
                ChatColor.GRAY + "Total items: " + totalItems,
                ChatColor.GRAY + "Items on this page: " + Math.min(totalItems, CRAFTING_DISPLAY_SLOTS.length),
                "",
                ChatColor.AQUA + "Click for more!"
            ));
            nextArrow.setItemMeta(nextMeta);
            gui.setItem(NEXT_PAGE_SLOT, nextArrow);
        }
        
        // Previous page arrow (if on overflow page)
        if (isOverflow) {
            ItemStack prevArrow = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevArrow.getItemMeta();
            prevMeta.setDisplayName(ChatColor.AQUA + "← Back to Page 1");
            prevMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Return to the first page",
                ChatColor.GRAY + "of craftable items",
                "",
                ChatColor.AQUA + "Click to go back!"
            ));
            prevArrow.setItemMeta(prevMeta);
            gui.setItem(NEXT_PAGE_SLOT, prevArrow);
        }
    }
    
    /**
     * Display craftable items in the GUI - FIXED with debugging
     */
    private static void displayCraftableItems(Inventory gui, List<CraftableItem> craftableItems, boolean isOverflow) {
        int startIndex = isOverflow ? CRAFTING_DISPLAY_SLOTS.length : 0;
        int endIndex = isOverflow ? craftableItems.size() : Math.min(craftableItems.size(), CRAFTING_DISPLAY_SLOTS.length);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Displaying items from index " + startIndex + " to " + endIndex + 
                " (overflow: " + isOverflow + ", total items: " + craftableItems.size() + ")");
        }
        
        for (int i = startIndex; i < endIndex; i++) {
            int displayIndex = i - startIndex;
            if (displayIndex >= CRAFTING_DISPLAY_SLOTS.length) break;
            
            CraftableItem craftable = craftableItems.get(i);
            ItemStack displayItem = createCraftableDisplayItem(craftable);
            int slot = CRAFTING_DISPLAY_SLOTS[displayIndex];
            
            gui.setItem(slot, displayItem);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                String itemName = craftable.result.hasItemMeta() && craftable.result.getItemMeta().hasDisplayName() ?
                                ChatColor.stripColor(craftable.result.getItemMeta().getDisplayName()) :
                                craftable.result.getType().name();
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Placed " + itemName + " in slot " + slot + 
                    " (display index " + displayIndex + ", list index " + i + ", max craftable: " + craftable.maxCraftable + ")");
            }
        }
    }
    
    /**
     * Create a display item for a craftable item
     */
    private static ItemStack createCraftableDisplayItem(CraftableItem craftable) {
        ItemStack display = craftable.result.clone();
        ItemMeta meta = display.getItemMeta();
        
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        }
        
        // Enhance the display name
        String originalName = meta.hasDisplayName() ? meta.getDisplayName() : 
                             formatMaterialName(display.getType().name());
        
        // Color code based on craftability
        ChatColor nameColor;
        String statusSymbol;
        if (craftable.maxCraftable >= 64) {
            nameColor = ChatColor.GREEN;
            statusSymbol = "✓✓";
        } else if (craftable.maxCraftable > 1) {
            nameColor = ChatColor.YELLOW;
            statusSymbol = "✓";
        } else if (craftable.maxCraftable == 1) {
            nameColor = ChatColor.GOLD;
            statusSymbol = "!";
        } else {
            nameColor = ChatColor.RED;
            statusSymbol = "✗";
        }
        
        meta.setDisplayName(nameColor + statusSymbol + " " + originalName);
        
        // Create enhanced lore
        List<String> lore = new ArrayList<>();
        
        // Add original lore if it exists
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
            lore.add("");
        }
        
        // Add crafting information
        lore.add(ChatColor.AQUA + "Auto Crafting Info:");
        lore.add(ChatColor.GRAY + "Max craftable: " + ChatColor.WHITE + craftable.maxCraftable);
        lore.add(ChatColor.GRAY + "Recipe type: " + ChatColor.WHITE + 
                (craftable.isCustomRecipe ? "Custom" : "Vanilla"));
        lore.add("");
        
        // Add usage instructions
        if (craftable.maxCraftable > 0) {
            lore.add(ChatColor.GREEN + "Left-click: " + ChatColor.WHITE + "Craft 1 item");
            if (craftable.maxCraftable > 1) {
                int maxShiftCraft = Math.min(64, craftable.maxCraftable);
                lore.add(ChatColor.GREEN + "Shift-click: " + ChatColor.WHITE + "Craft up to " + maxShiftCraft);
            }
        } else {
            lore.add(ChatColor.RED + "Cannot craft - missing ingredients");
        }
        
        meta.setLore(lore);
        display.setItemMeta(meta);
        
        return display;
    }
    
    /**
     * Analyze player's inventory to find all craftable items
     * FIXED: Only show items that can actually be crafted AND are unlocked
     */
    private static List<CraftableItem> analyzeCraftableItems(Player player) {
        List<CraftableItem> craftableItems = new ArrayList<>();
        Set<ItemStack> uniqueResults = new HashSet<>();
        
        // Get player's inventory contents
        ItemStack[] playerInventory = player.getInventory().getStorageContents();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Analyzing inventory for " + player.getName());
        }
        
        // Check custom recipes first (FIXED: Only if unlocked and craftable)
        analyzeCustomRecipes(player, playerInventory, craftableItems, uniqueResults);
        
        // Check vanilla recipes (FIXED: Only if craftable)
        analyzeVanillaRecipes(player, playerInventory, craftableItems, uniqueResults);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Found " + craftableItems.size() + " unique craftable items");
        }
        
        return craftableItems;
    }
    
    /**
     * Analyze custom recipes - FIXED: Check unlocking and actual craftability
     */
    private static void analyzeCustomRecipes(Player player, ItemStack[] playerInventory, 
                                           List<CraftableItem> craftableItems, Set<ItemStack> uniqueResults) {
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        
        // Get all custom recipes and test them
        Map<String, ItemStack> customRecipes = manager.getCustomRecipes();
        for (Map.Entry<String, ItemStack> entry : customRecipes.entrySet()) {
            ItemStack result = entry.getValue();
            
            // Skip if we already found this result
            if (uniqueResults.contains(result)) continue;
            
            // CRITICAL FIX: Check if recipe is unlocked for this player
            if (!manager.isRecipeUnlocked(player, result)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Skipping locked recipe: " + result.getType());
                }
                continue;
            }
            
            // Test if this recipe can be crafted with current inventory
            int maxCraftable = manager.getMaxCraftableAmount(playerInventory, result);
            if (maxCraftable > 0) {
                ItemStack[] recipe = reconstructRecipePattern(entry.getKey());
                craftableItems.add(new CraftableItem(result, recipe, maxCraftable, true, "custom"));
                uniqueResults.add(result.clone());
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Added custom recipe: " + result.getType() + " (max: " + maxCraftable + ")");
                }
            }
        }
    }
    
    /**
     * Analyze vanilla recipes - FIXED: Comprehensive recipe testing
     */
    private static void analyzeVanillaRecipes(Player player, ItemStack[] playerInventory,
                                            List<CraftableItem> craftableItems, Set<ItemStack> uniqueResults) {
        
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        
        // Get all vanilla recipes and test them systematically
        List<Recipe> vanillaRecipes = manager.getVanillaRecipes();
        
        for (Recipe recipe : vanillaRecipes) {
            ItemStack result = recipe.getResult();
            
            // Skip if we already found this result
            if (uniqueResults.contains(result)) continue;
            
            // Test if we can craft this recipe with current inventory
            ItemStack[] testGrid = createTestGridForRecipe(recipe, playerInventory);
            if (testGrid != null) {
                // Verify the recipe actually works
                ItemStack recipeResult = manager.getRecipeResult(testGrid, player);
                if (recipeResult != null && recipeResult.isSimilar(result)) {
                    int maxCraftable = manager.getMaxCraftableAmount(playerInventory, result);
                    if (maxCraftable > 0) {
                        String recipeType = recipe instanceof ShapedRecipe ? "shaped" : "shapeless";
                        craftableItems.add(new CraftableItem(result, testGrid, maxCraftable, false, recipeType));
                        uniqueResults.add(result.clone());
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Auto Crafting] Added vanilla recipe: " + result.getType() + " (max: " + maxCraftable + ")");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Create a test grid for a recipe to see if it can be crafted
     */
    private static ItemStack[] createTestGridForRecipe(Recipe recipe, ItemStack[] playerInventory) {
        if (recipe instanceof ShapedRecipe) {
            return createTestGridForShapedRecipe((ShapedRecipe) recipe, playerInventory);
        } else if (recipe instanceof ShapelessRecipe) {
            return createTestGridForShapelessRecipe((ShapelessRecipe) recipe, playerInventory);
        }
        return null;
    }
    
    /**
     * Create test grid for shaped recipe
     */
    private static ItemStack[] createTestGridForShapedRecipe(ShapedRecipe recipe, ItemStack[] playerInventory) {
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredients = recipe.getIngredientMap();
        
        // Create inventory count map
        Map<Material, Integer> availableItems = new HashMap<>();
        for (ItemStack item : playerInventory) {
            if (item != null && item.getType() != Material.AIR) {
                availableItems.put(item.getType(), availableItems.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        // Check if we have all required ingredients
        Map<Material, Integer> requiredItems = new HashMap<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                ItemStack ingredient = ingredients.get(c);
                if (ingredient != null) {
                    Material mat = ingredient.getType();
                    requiredItems.put(mat, requiredItems.getOrDefault(mat, 0) + ingredient.getAmount());
                }
            }
        }
        
        // Check if we have enough materials
        for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
            int available = availableItems.getOrDefault(required.getKey(), 0);
            if (available < required.getValue()) {
                return null; // Not enough materials
            }
        }
        
        // Create test grid
        ItemStack[] testGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            testGrid[i] = new ItemStack(Material.AIR);
        }
        
        // Fill the grid according to recipe shape
        for (int row = 0; row < shape.length && row < 3; row++) {
            String rowPattern = shape[row];
            for (int col = 0; col < rowPattern.length() && col < 3; col++) {
                char c = rowPattern.charAt(col);
                ItemStack ingredient = ingredients.get(c);
                if (ingredient != null) {
                    testGrid[row * 3 + col] = ingredient.clone();
                }
            }
        }
        
        return testGrid;
    }
    
    /**
     * Create test grid for shapeless recipe
     */
    private static ItemStack[] createTestGridForShapelessRecipe(ShapelessRecipe recipe, ItemStack[] playerInventory) {
        List<ItemStack> ingredientList = recipe.getIngredientList();
        
        // Create inventory count map
        Map<Material, Integer> availableItems = new HashMap<>();
        for (ItemStack item : playerInventory) {
            if (item != null && item.getType() != Material.AIR) {
                availableItems.put(item.getType(), availableItems.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        // Check if we have all required ingredients
        Map<Material, Integer> requiredItems = new HashMap<>();
        for (ItemStack ingredient : ingredientList) {
            if (ingredient != null) {
                Material mat = ingredient.getType();
                requiredItems.put(mat, requiredItems.getOrDefault(mat, 0) + ingredient.getAmount());
            }
        }
        
        // Check if we have enough materials
        for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
            int available = availableItems.getOrDefault(required.getKey(), 0);
            if (available < required.getValue()) {
                return null; // Not enough materials
            }
        }
        
        // Create test grid
        ItemStack[] testGrid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            testGrid[i] = new ItemStack(Material.AIR);
        }
        
        // Place ingredients in grid
        int slotIndex = 0;
        for (ItemStack ingredient : ingredientList) {
            if (ingredient != null && slotIndex < 9) {
                testGrid[slotIndex] = ingredient.clone();
                slotIndex++;
            }
        }
        
        return testGrid;
    }
    
    /**
     * Calculate maximum craftable amount for custom recipes
     */
    private static int calculateMaxCraftableCustom(Player player, ItemStack result, ItemStack[] playerInventory) {
        return CustomCraftingManager.getInstance().getMaxCraftableAmount(playerInventory, result);
    }
    
    /**
     * Calculate maximum craftable amount for vanilla recipes
     */
    private static int calculateMaxCraftableVanilla(Player player, ItemStack result, ItemStack[] playerInventory) {
        return CustomCraftingManager.getInstance().getMaxCraftableAmount(playerInventory, result);
    }
    
    /**
     * Reconstruct recipe pattern from recipe key - FIXED to use CustomCraftingManager
     */
    private static ItemStack[] reconstructRecipePattern(String recipeKey) {
        // This method is now replaced by direct calls to CustomCraftingManager
        // But we keep it for backward compatibility
        ItemStack[] pattern = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            pattern[i] = new ItemStack(Material.AIR);
        }
        return pattern;
    }

    /**
     * Reconstruct custom recipe pattern for actual crafting - FIXED
     */
    private static ItemStack[] reconstructCustomRecipePattern(String recipeKey, ItemStack result) {
        // Use the CustomCraftingManager to get the actual pattern
        ItemStack[] pattern = CustomCraftingManager.getInstance().getCustomRecipePattern(result);
        if (pattern != null) {
            return pattern.clone();
        }
        
        // Fallback to empty pattern if not found
        ItemStack[] emptyPattern = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            emptyPattern[i] = new ItemStack(Material.AIR);
        }
        return emptyPattern;
    }
    
    /**
     * Perform auto-crafting for a specific item - COMPLETELY FIXED
     */
    public static boolean performAutoCraft(Player player, CraftableItem craftable, boolean shiftClick) {
        if (craftable.maxCraftable <= 0) {
            player.sendMessage(ChatColor.RED + "Cannot craft this item - missing ingredients!");
            return false;
        }
        
        int amountToCraft = shiftClick ? Math.min(64, craftable.maxCraftable) : 1;
        
        // Calculate how many items we can actually add to inventory
        int spaceAvailable = getAvailableInventorySpace(player, craftable.result);
        amountToCraft = Math.min(amountToCraft, spaceAvailable);
        
        if (amountToCraft <= 0) {
            player.sendMessage(ChatColor.RED + "No inventory space available!");
            return false;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Starting craft: " + amountToCraft + " of " + craftable.result.getType());
        }
        
        // CRITICAL FIX: Get the ACTUAL working recipe for this item
        ItemStack[] workingRecipe = findActualWorkingRecipe(player, craftable.result);
        if (workingRecipe == null) {
            player.sendMessage(ChatColor.RED + "Could not find working recipe!");
            return false;
        }
        
        int successfulCrafts = 0;
        
        for (int i = 0; i < amountToCraft; i++) {
            // Get current inventory state
            ItemStack[] currentInventory = player.getInventory().getStorageContents();
            
            // CRITICAL: Check if we can still craft with current inventory
            if (!canCraftRecipeWithInventory(workingRecipe, currentInventory)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Cannot craft more - insufficient ingredients after " + i + " crafts");
                }
                break;
            }
            
            // FIXED: Consume the EXACT ingredients from the working recipe
            if (!consumeExactRecipeIngredients(workingRecipe, currentInventory)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Failed to consume ingredients for craft " + (i + 1));
                }
                break;
            }
            
            // Update player's inventory with consumed ingredients
            for (int j = 0; j < Math.min(currentInventory.length, 36); j++) {
                player.getInventory().setItem(j, currentInventory[j]);
            }
            
            // FIXED: Add result properly to inventory
            if (!addItemToInventorySafely(player, craftable.result.clone())) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Failed to add result to inventory for craft " + (i + 1));
                }
                break;
            }
            
            successfulCrafts++;
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Successfully completed craft " + (i + 1) + "/" + amountToCraft);
            }
        }
        
        if (successfulCrafts > 0) {
            // Send success message
            String itemName = craftable.result.hasItemMeta() && craftable.result.getItemMeta().hasDisplayName() ?
                            ChatColor.stripColor(craftable.result.getItemMeta().getDisplayName()) :
                            formatMaterialName(craftable.result.getType().name());
            
            if (successfulCrafts == 1) {
                player.sendMessage(ChatColor.GREEN + "✓ Crafted 1x " + itemName);
            } else {
                player.sendMessage(ChatColor.GREEN + "✓ Crafted " + successfulCrafts + "x " + itemName);
            }
            
            return true;
        }
        
        return false;
    }

    /**
     * Find the actual working recipe for a specific result item from player's inventory
     */
    private static ItemStack[] findActualWorkingRecipe(Player player, ItemStack result) {
        ItemStack[] playerInventory = player.getInventory().getStorageContents();
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        
        // First, try custom recipes
        Map<String, ItemStack> customRecipes = manager.getCustomRecipes();
        for (Map.Entry<String, ItemStack> entry : customRecipes.entrySet()) {
            if (entry.getValue().isSimilar(result)) {
                // Try to create this custom recipe pattern
                ItemStack[] recipePattern = reconstructCustomRecipePattern(entry.getKey(), entry.getValue());
                if (recipePattern != null && canCraftRecipeWithInventory(recipePattern, playerInventory)) {
                    // Test if this recipe actually produces the result
                    ItemStack testResult = manager.getRecipeResult(recipePattern, player);
                    if (testResult != null && testResult.isSimilar(result)) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Auto Crafting] Found working custom recipe for " + result.getType());
                        }
                        return recipePattern;
                    }
                }
            }
        }
        
        // Then, try vanilla recipes
        List<Recipe> vanillaRecipes = manager.getVanillaRecipes();
        for (Recipe recipe : vanillaRecipes) {
            if (recipe.getResult().isSimilar(result)) {
                ItemStack[] recipePattern = createVanillaRecipePattern(recipe, playerInventory);
                if (recipePattern != null && canCraftRecipeWithInventory(recipePattern, playerInventory)) {
                    // Test if this recipe actually produces the result
                    ItemStack testResult = manager.getRecipeResult(recipePattern, player);
                    if (testResult != null && testResult.isSimilar(result)) {
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Auto Crafting] Found working vanilla recipe for " + result.getType());
                        }
                        return recipePattern;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Create a vanilla recipe pattern that can be crafted with current inventory
     */
    private static ItemStack[] createVanillaRecipePattern(Recipe recipe, ItemStack[] playerInventory) {
        if (recipe instanceof ShapedRecipe) {
            return createShapedRecipePattern((ShapedRecipe) recipe, playerInventory);
        } else if (recipe instanceof ShapelessRecipe) {
            return createShapelessRecipePattern((ShapelessRecipe) recipe, playerInventory);
        }
        return null;
    }

    /**
     * Create a shaped recipe pattern from player inventory
     */
    private static ItemStack[] createShapedRecipePattern(ShapedRecipe recipe, ItemStack[] playerInventory) {
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredients = recipe.getIngredientMap();
        
        // Create inventory availability map
        Map<Material, Integer> available = new HashMap<>();
        for (ItemStack item : playerInventory) {
            if (item != null && item.getType() != Material.AIR) {
                available.put(item.getType(), available.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        // Check if we have all required ingredients
        Map<Material, Integer> required = new HashMap<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                ItemStack ingredient = ingredients.get(c);
                if (ingredient != null) {
                    Material mat = ingredient.getType();
                    required.put(mat, required.getOrDefault(mat, 0) + ingredient.getAmount());
                }
            }
        }
        
        // Verify we have enough materials
        for (Map.Entry<Material, Integer> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return null; // Not enough materials
            }
        }
        
        // Create the actual recipe pattern
        ItemStack[] pattern = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            pattern[i] = new ItemStack(Material.AIR);
        }
        
        // Fill pattern according to shape
        for (int row = 0; row < shape.length && row < 3; row++) {
            String rowPattern = shape[row];
            for (int col = 0; col < rowPattern.length() && col < 3; col++) {
                char c = rowPattern.charAt(col);
                ItemStack ingredient = ingredients.get(c);
                if (ingredient != null) {
                    pattern[row * 3 + col] = ingredient.clone();
                }
            }
        }
        
        return pattern;
    }

    /**
     * Create a shapeless recipe pattern from player inventory
     */
    private static ItemStack[] createShapelessRecipePattern(ShapelessRecipe recipe, ItemStack[] playerInventory) {
        List<ItemStack> ingredientList = recipe.getIngredientList();
        
        // Create inventory availability map
        Map<Material, Integer> available = new HashMap<>();
        for (ItemStack item : playerInventory) {
            if (item != null && item.getType() != Material.AIR) {
                available.put(item.getType(), available.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }
        
        // Check if we have all required ingredients
        Map<Material, Integer> required = new HashMap<>();
        for (ItemStack ingredient : ingredientList) {
            if (ingredient != null) {
                Material mat = ingredient.getType();
                required.put(mat, required.getOrDefault(mat, 0) + ingredient.getAmount());
            }
        }
        
        // Verify we have enough materials
        for (Map.Entry<Material, Integer> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return null; // Not enough materials
            }
        }
        
        // Create the pattern
        ItemStack[] pattern = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            pattern[i] = new ItemStack(Material.AIR);
        }
        
        // Place ingredients in the pattern
        int slotIndex = 0;
        for (ItemStack ingredient : ingredientList) {
            if (ingredient != null && slotIndex < 9) {
                pattern[slotIndex] = ingredient.clone();
                slotIndex++;
            }
        }
        
        return pattern;
    }

    /**
     * Check if a recipe can be crafted with current inventory
     */
    private static boolean canCraftRecipeWithInventory(ItemStack[] recipePattern, ItemStack[] inventory) {
        // Count required materials
        Map<Material, Integer> required = new HashMap<>();
        Map<ItemStack, Integer> requiredCustom = new HashMap<>();
        
        for (ItemStack item : recipePattern) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    // For custom items, track exact matches
                    boolean found = false;
                    for (Map.Entry<ItemStack, Integer> entry : requiredCustom.entrySet()) {
                        if (areCustomItemsSame(entry.getKey(), item)) {
                            entry.setValue(entry.getValue() + item.getAmount());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        requiredCustom.put(item.clone(), item.getAmount());
                    }
                } else {
                    required.put(item.getType(), required.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        // Count available materials
        Map<Material, Integer> available = new HashMap<>();
        List<ItemStack> availableCustom = new ArrayList<>();
        
        for (ItemStack item : inventory) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    availableCustom.add(item.clone());
                } else {
                    available.put(item.getType(), available.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        // Check regular materials
        for (Map.Entry<Material, Integer> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return false;
            }
        }
        
        // Check custom items
        for (Map.Entry<ItemStack, Integer> req : requiredCustom.entrySet()) {
            int needed = req.getValue();
            int found = 0;
            
            for (ItemStack availableItem : availableCustom) {
                if (areCustomItemsSame(availableItem, req.getKey())) {
                    found += availableItem.getAmount();
                    if (found >= needed) break;
                }
            }
            
            if (found < needed) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Consume exact recipe ingredients from inventory
     */
    private static boolean consumeExactRecipeIngredients(ItemStack[] recipePattern, ItemStack[] inventory) {
        // Create a map of what we need to consume
        Map<Material, Integer> toConsume = new HashMap<>();
        Map<ItemStack, Integer> toConsumeCustom = new HashMap<>();
        
        for (ItemStack item : recipePattern) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    boolean found = false;
                    for (Map.Entry<ItemStack, Integer> entry : toConsumeCustom.entrySet()) {
                        if (areCustomItemsSame(entry.getKey(), item)) {
                            entry.setValue(entry.getValue() + item.getAmount());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        toConsumeCustom.put(item.clone(), item.getAmount());
                    }
                } else {
                    toConsume.put(item.getType(), toConsume.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            for (Map.Entry<Material, Integer> entry : toConsume.entrySet()) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Auto Crafting] Need to consume: " + entry.getValue() + "x " + entry.getKey());
            }
        }
        
        // Consume regular materials
        for (Map.Entry<Material, Integer> entry : toConsume.entrySet()) {
            Material material = entry.getKey();
            int amountNeeded = entry.getValue();
            
            for (int i = 0; i < inventory.length && amountNeeded > 0; i++) {
                ItemStack slot = inventory[i];
                if (slot != null && slot.getType() == material && !isCustomItem(slot)) {
                    int available = slot.getAmount();
                    int toTake = Math.min(available, amountNeeded);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Auto Crafting] Consuming " + toTake + "x " + material + " from slot " + i + 
                            " (had " + available + ")");
                    }
                    
                    slot.setAmount(available - toTake);
                    if (slot.getAmount() <= 0) {
                        inventory[i] = new ItemStack(Material.AIR);
                    }
                    
                    amountNeeded -= toTake;
                }
            }
            
            if (amountNeeded > 0) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Failed to consume enough " + material + " (still need " + amountNeeded + ")");
                }
                return false; // Couldn't consume enough
            }
        }
        
        // Consume custom items
        for (Map.Entry<ItemStack, Integer> entry : toConsumeCustom.entrySet()) {
            ItemStack targetItem = entry.getKey();
            int amountNeeded = entry.getValue();
            
            for (int i = 0; i < inventory.length && amountNeeded > 0; i++) {
                ItemStack slot = inventory[i];
                if (slot != null && areCustomItemsSame(slot, targetItem)) {
                    int available = slot.getAmount();
                    int toTake = Math.min(available, amountNeeded);
                    
                    slot.setAmount(available - toTake);
                    if (slot.getAmount() <= 0) {
                        inventory[i] = new ItemStack(Material.AIR);
                    }
                    
                    amountNeeded -= toTake;
                }
            }
            
            if (amountNeeded > 0) {
                return false; // Couldn't consume enough custom items
            }
        }
        
        return true;
    }

    /**
     * Safely add item to player inventory, finding the best available slot
     */
    private static boolean addItemToInventorySafely(Player player, ItemStack item) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        
        // First, try to stack with existing similar items
        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(item)) {
                int spaceInSlot = slot.getMaxStackSize() - slot.getAmount();
                if (spaceInSlot > 0) {
                    int toAdd = Math.min(spaceInSlot, item.getAmount());
                    slot.setAmount(slot.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    player.getInventory().setItem(i, slot);
                    
                    if (item.getAmount() <= 0) {
                        return true; // Successfully added all
                    }
                }
            }
        }
        
        // Then, find empty slots
        for (int i = 0; i < contents.length; i++) {
            ItemStack slot = contents[i];
            if (slot == null || slot.getType() == Material.AIR) {
                player.getInventory().setItem(i, item.clone());
                return true;
            }
        }
        
        // If we get here, inventory is full
        player.getWorld().dropItemNaturally(player.getLocation(), item);
        player.sendMessage(ChatColor.YELLOW + "Inventory full! Dropped " + item.getAmount() + "x " + 
                        formatMaterialName(item.getType().name()) + " on the ground.");
        return true; // Still "successful" even if dropped
    }

    /**
     * Find a working custom recipe pattern with current inventory - IMPROVED
     */
    private static ItemStack[] findWorkingCustomRecipePattern(ItemStack result, ItemStack[] inventory) {
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        
        // Get all custom recipes
        Map<String, ItemStack> customRecipes = manager.getCustomRecipes();
        
        for (Map.Entry<String, ItemStack> entry : customRecipes.entrySet()) {
            if (entry.getValue().isSimilar(result)) {
                // Try to reconstruct this recipe pattern
                ItemStack[] pattern = reconstructRecipePattern(entry.getKey());
                
                // Test if we can create this pattern with current inventory
                if (canCreatePattern(pattern, inventory)) {
                    return createActualCraftingGrid(pattern, inventory);
                }
            }
        }
        
        return null;
    }

    /**
     * Find a working vanilla recipe pattern with current inventory - IMPROVED
     */
    private static ItemStack[] createWorkingVanillaRecipeGrid(ItemStack result, ItemStack[] inventory) {
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        List<Recipe> vanillaRecipes = manager.getVanillaRecipes();
        
        for (Recipe recipe : vanillaRecipes) {
            if (recipe.getResult().isSimilar(result)) {
                ItemStack[] testGrid = createTestGridForRecipe(recipe, inventory);
                if (testGrid != null) {
                    // Verify this grid actually works
                    ItemStack testResult = manager.getRecipeResult(testGrid, null);
                    if (testResult != null && testResult.isSimilar(result)) {
                        return testGrid;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Check if we can create a specific pattern with current inventory
     */
    private static boolean canCreatePattern(ItemStack[] pattern, ItemStack[] inventory) {
        // Count required materials
        Map<Material, Integer> required = new HashMap<>();
        Map<ItemStack, Integer> requiredCustomItems = new HashMap<>();
        
        for (ItemStack item : pattern) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    // For custom items, track exact item matches
                    boolean found = false;
                    for (Map.Entry<ItemStack, Integer> entry : requiredCustomItems.entrySet()) {
                        if (areCustomItemsSame(entry.getKey(), item)) {
                            entry.setValue(entry.getValue() + item.getAmount());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        requiredCustomItems.put(item.clone(), item.getAmount());
                    }
                } else {
                    required.put(item.getType(), required.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        // Count available materials
        Map<Material, Integer> available = new HashMap<>();
        List<ItemStack> availableCustomItems = new ArrayList<>();
        
        for (ItemStack item : inventory) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    availableCustomItems.add(item.clone());
                } else {
                    available.put(item.getType(), available.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        // Check if we have enough regular materials
        for (Map.Entry<Material, Integer> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return false;
            }
        }
        
        // Check if we have enough custom items
        for (Map.Entry<ItemStack, Integer> req : requiredCustomItems.entrySet()) {
            int neededCount = req.getValue();
            int foundCount = 0;
            
            for (ItemStack availableItem : availableCustomItems) {
                if (areCustomItemsSame(availableItem, req.getKey())) {
                    foundCount += availableItem.getAmount();
                    if (foundCount >= neededCount) {
                        break;
                    }
                }
            }
            
            if (foundCount < neededCount) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Create an actual crafting grid from a pattern using current inventory
     */
    private static ItemStack[] createActualCraftingGrid(ItemStack[] pattern, ItemStack[] inventory) {
        ItemStack[] grid = new ItemStack[9];
        
        // Create inventory tracking for consumption
        Map<Material, Integer> availableMaterials = new HashMap<>();
        List<ItemStack> availableCustomItems = new ArrayList<>();
        
        for (ItemStack item : inventory) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    availableCustomItems.add(item.clone());
                } else {
                    availableMaterials.put(item.getType(), 
                        availableMaterials.getOrDefault(item.getType(), 0) + item.getAmount());
                }
            }
        }
        
        // Fill the grid according to pattern
        for (int i = 0; i < Math.min(pattern.length, 9); i++) {
            ItemStack patternItem = pattern[i];
            
            if (patternItem == null || patternItem.getType() == Material.AIR) {
                grid[i] = new ItemStack(Material.AIR);
            } else if (isCustomItem(patternItem)) {
                // Find matching custom item
                ItemStack matchingItem = null;
                for (int j = 0; j < availableCustomItems.size(); j++) {
                    ItemStack availableItem = availableCustomItems.get(j);
                    if (areCustomItemsSame(availableItem, patternItem)) {
                        if (availableItem.getAmount() >= patternItem.getAmount()) {
                            matchingItem = availableItem.clone();
                            matchingItem.setAmount(patternItem.getAmount());
                            
                            // Update available items
                            availableItem.setAmount(availableItem.getAmount() - patternItem.getAmount());
                            if (availableItem.getAmount() <= 0) {
                                availableCustomItems.remove(j);
                            }
                            break;
                        }
                    }
                }
                grid[i] = matchingItem != null ? matchingItem : new ItemStack(Material.AIR);
            } else {
                // Regular material
                Material mat = patternItem.getType();
                int needed = patternItem.getAmount();
                int available = availableMaterials.getOrDefault(mat, 0);
                
                if (available >= needed) {
                    grid[i] = new ItemStack(mat, needed);
                    availableMaterials.put(mat, available - needed);
                } else {
                    grid[i] = new ItemStack(Material.AIR);
                }
            }
        }
        
        return grid;
    }

    /**
     * Check if an item is a custom item (has custom model data) - FIXED
     */
    private static boolean isCustomItem(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    /**
     * Check if two custom items are the same - FIXED
     */
    private static boolean areCustomItemsSame(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) return false;
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (!meta1.hasCustomModelData() || !meta2.hasCustomModelData()) return false;
        
        // Check material, custom model data, and display name
        boolean materialMatch = item1.getType() == item2.getType();
        boolean modelMatch = meta1.getCustomModelData() == meta2.getCustomModelData();
        boolean nameMatch = true;
        
        if (meta1.hasDisplayName() && meta2.hasDisplayName()) {
            nameMatch = meta1.getDisplayName().equals(meta2.getDisplayName());
        } else if (meta1.hasDisplayName() || meta2.hasDisplayName()) {
            nameMatch = false;
        }
        
        return materialMatch && modelMatch && nameMatch;
    }
    
    /**
     * Find the custom recipe pattern that matches the result
     */
    private static ItemStack[] findCustomRecipePattern(ItemStack result, ItemStack[] inventory) {
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        
        // Test all possible 3x3 combinations to find a working pattern
        // This is a simplified approach - in reality, you'd want to store the actual patterns
        Map<String, ItemStack> customRecipes = manager.getCustomRecipes();
        
        for (Map.Entry<String, ItemStack> entry : customRecipes.entrySet()) {
            if (entry.getValue().isSimilar(result)) {
                // Reconstruct the pattern from the key (this is simplified)
                return reconstructRecipePattern(entry.getKey());
            }
        }
        
        return null;
    }
    
    /**
     * Create a vanilla recipe grid for crafting
     */
    private static ItemStack[] createVanillaRecipeGrid(ItemStack result, ItemStack[] inventory) {
        CustomCraftingManager manager = CustomCraftingManager.getInstance();
        List<Recipe> vanillaRecipes = manager.getVanillaRecipes();
        
        for (Recipe recipe : vanillaRecipes) {
            if (recipe.getResult().isSimilar(result)) {
                return createTestGridForRecipe(recipe, inventory);
            }
        }
        
        return null;
    }
    
    /**
     * Get available inventory space for a specific item
     */
    private static int getAvailableInventorySpace(Player player, ItemStack item) {
        int space = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        
        for (ItemStack slot : contents) {
            if (slot == null || slot.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                space += item.getMaxStackSize() - slot.getAmount();
            }
        }
        
        return space;
    }
    
    /**
     * Utility methods
     */
    private static ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        pane.setItemMeta(meta);
        return pane;
    }
    
    private static String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return formatted.toString();
    }
    
    // Navigation and state management methods
    public static boolean isAutoCraftingGUI(Inventory inventory) {
        String title = inventory.getViewers().isEmpty() ? null : 
                      inventory.getViewers().get(0).getOpenInventory().getTitle();
        return GUI_TITLE.equals(title) || OVERFLOW_GUI_TITLE.equals(title);
    }
    
    public static boolean isBackArrowSlot(int slot) {
        return slot == BACK_ARROW_SLOT;
    }
    
    public static boolean isNextPageSlot(int slot) {
        return slot == NEXT_PAGE_SLOT;
    }
    
    public static boolean isRefreshSlot(int slot) {
        return slot == REFRESH_SLOT;
    }
    
    /**
     * Check if a slot is a craftable item slot - FIXED with debugging
     */
    public static boolean isCraftableItemSlot(int slot) {
        for (int craftingSlot : CRAFTING_DISPLAY_SLOTS) {
            if (slot == craftingSlot) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Auto Crafting] Slot " + slot + " is a valid craftable item slot");
                }
                return true;
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Slot " + slot + " is NOT a craftable item slot. Valid slots: " +
                java.util.Arrays.toString(CRAFTING_DISPLAY_SLOTS));
        }
        return false;
    }
        
    /**
     * Remove active auto-crafting GUI - FIXED: Don't clear craftable items immediately
     */
    public static void removeActiveAutoCraftingGUI(Player player) {
        activeAutoCraftingGUIs.remove(player);
        activeOverflowGUIs.remove(player);
        // DON'T clear playerCraftableItems here - only clear when player disconnects or switches GUIs
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Removed active GUI for " + player.getName() + " but kept craftable items data");
        }
    }

    /**
     * Clear all data for a player (use this when player disconnects or switches to different GUI system)
     */
    public static void clearPlayerData(Player player) {
        activeAutoCraftingGUIs.remove(player);
        activeOverflowGUIs.remove(player);
        playerCraftableItems.remove(player);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Auto Crafting] Cleared all data for " + player.getName());
        }
    }

    public static Inventory getActiveAutoCraftingGUI(Player player) {
        return activeAutoCraftingGUIs.get(player);
    }
    
    public static Inventory getActiveOverflowGUI(Player player) {
        return activeOverflowGUIs.get(player);
    }
}