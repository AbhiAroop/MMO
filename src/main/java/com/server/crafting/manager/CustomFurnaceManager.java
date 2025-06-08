package com.server.crafting.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.crafting.data.CustomFuelData;
import com.server.crafting.data.CustomFurnaceRecipe;
import com.server.crafting.data.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.listeners.CustomFurnaceListener;
import com.server.debug.DebugManager.DebugSystem;
import com.server.items.CustomItems;
import com.server.items.ItemManager;

/**
 * Manager for handling custom furnace operations and recipes
 */
public class CustomFurnaceManager {
    
    private static CustomFurnaceManager instance;
    private final Map<String, FurnaceData> furnaceDataMap;
    private final Map<Material, ItemStack> smeltingRecipes; // Legacy vanilla recipes
    private final List<CustomFurnaceRecipe> customRecipes; // NEW: Custom recipes with exact matching
    private final List<CustomFuelData> customFuels; // NEW: Custom fuels with exact matching
    private BukkitTask furnaceUpdateTask;
    private final Map<String, UUID> furnaceAccessMap = new HashMap<>();
    private final Map<UUID, String> playerAccessMap = new HashMap<>();
    
    private CustomFurnaceManager() {
        this.furnaceDataMap = new HashMap<>();
        this.smeltingRecipes = new HashMap<>();
        this.customRecipes = new ArrayList<>(); // Initialize custom recipes
        this.customFuels = new ArrayList<>(); // Initialize custom fuels
        initializeSmeltingRecipes();
        initializeCustomRecipes(); // NEW: Initialize custom recipes
        initializeCustomFuels(); // NEW: Initialize custom fuels
        startFurnaceUpdateTask();
    }
    
    public static CustomFurnaceManager getInstance() {
        if (instance == null) {
            instance = new CustomFurnaceManager();
        }
        return instance;
    }

    /**
     * Initialize custom recipes with exact item matching - NEW METHOD
     */
    private void initializeCustomRecipes() {
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Initializing custom recipes with exact item matching");
        }
        
        // Example: Custom pickaxe smelting recipe
        ItemStack rustyCrumbledPickaxe = CustomItems.createRustyCrumbledPickaxe();
        ItemStack rootCrackedPickaxe = CustomItems.createRootCrackedPickaxe();
        
        // Rusty Crumbled Pickaxe -> Root Cracked Pickaxe (takes 30 seconds)
        addCustomRecipe("rusty_to_root", rustyCrumbledPickaxe, rootCrackedPickaxe, 600); // 30 seconds
        
        // Add more custom recipes here...
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Initialized " + customRecipes.size() + " custom recipes");
        }
    }
    
    /**
     * Initialize custom fuels with exact item matching - NEW METHOD
     */
    private void initializeCustomFuels() {
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Initializing custom fuels with exact item matching");
        }
        
        // Example: Custom staff as fuel (burns for 5 minutes)
        ItemStack emberwoodStaff = CustomItems.createEmberwoodStaff();
        addCustomFuel("emberwood_staff_fuel", emberwoodStaff, 6000); // 5 minutes
        
        // Add more custom fuels here...
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Initialized " + customFuels.size() + " custom fuels");
        }
    }
    
    /**
     * Add a custom recipe with exact item matching - NEW METHOD
     */
    public void addCustomRecipe(String recipeId, ItemStack input, ItemStack result, int cookTime) {
        CustomFurnaceRecipe recipe = new CustomFurnaceRecipe(recipeId, input, result, cookTime);
        customRecipes.add(recipe);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Added custom recipe: " + recipe);
        }
    }
    
    /**
     * Add a custom recipe with default cook time - NEW METHOD
     */
    public void addCustomRecipe(String recipeId, ItemStack input, ItemStack result) {
        addCustomRecipe(recipeId, input, result, 200); // Default 10 seconds
    }
    
    /**
     * Add a custom fuel with exact item matching - NEW METHOD
     */
    public void addCustomFuel(String fuelId, ItemStack fuelItem, int burnTime) {
        CustomFuelData fuel = new CustomFuelData(fuelId, fuelItem, burnTime);
        customFuels.add(fuel);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Added custom fuel: " + fuel);
        }
    }
    
    /**
     * Get the smelting result for an item - ENHANCED: Check custom recipes first
     */
    public ItemStack getSmeltingResult(ItemStack item) {
        if (item == null) return null;
        
        // FIRST: Check custom recipes with exact matching
        for (CustomFurnaceRecipe recipe : customRecipes) {
            if (recipe.matchesInput(item)) {
                ItemStack result = recipe.getResult();
                
                // Apply rarity if not already present
                if (!ItemManager.hasRarity(result)) {
                    result = ItemManager.applyRarity(result);
                }
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Found custom recipe: " + recipe.getRecipeId() + 
                        " for item: " + getItemDebugName(item));
                }
                
                return result;
            }
        }
        
        // SECOND: Fall back to vanilla material-based recipes
        ItemStack result = smeltingRecipes.get(item.getType());
        if (result != null) {
            ItemStack clonedResult = result.clone();
            
            // Apply rarity if not already present
            if (!ItemManager.hasRarity(clonedResult)) {
                clonedResult = ItemManager.applyRarity(clonedResult);
            }
            
            return clonedResult;
        }
        
        return null;
    }
    
    /**
     * Get the cook time for an item - NEW METHOD
     */
    public int getCookTime(ItemStack item) {
        if (item == null) return 200; // Default 10 seconds
        
        // Check custom recipes first
        for (CustomFurnaceRecipe recipe : customRecipes) {
            if (recipe.matchesInput(item)) {
                return recipe.getCookTime();
            }
        }
        
        // Default cook time for vanilla items
        return 200; // 10 seconds
    }
    
    /**
     * Check if an item can be smelted - ENHANCED: Better debugging
     */
    public boolean canSmelt(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // FIRST: Check custom recipes with exact matching
        for (CustomFurnaceRecipe recipe : customRecipes) {
            if (recipe.matchesInput(item)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Item " + getItemDebugName(item) + " can be smelted (custom recipe)");
                }
                return true;
            }
        }
        
        // SECOND: Check vanilla material recipes
        boolean canSmeltVanilla = smeltingRecipes.containsKey(item.getType());
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Item " + getItemDebugName(item) + " canSmelt check: " + canSmeltVanilla + 
                " (type: " + item.getType() + ")");
        }
        
        return canSmeltVanilla;
    }
    
    /**
     * Check if an item can be used as fuel - ENHANCED: Check custom fuels first
     */
    public boolean isFuel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        // Check custom fuels first
        for (CustomFuelData fuel : customFuels) {
            if (fuel.matchesFuel(item)) {
                return true;
            }
        }
        
        // Fall back to vanilla material check
        Material fuelType = item.getType();
        
        switch (fuelType) {
            case COAL:
            case CHARCOAL:
            case COAL_BLOCK:
            case DRIED_KELP_BLOCK:
            case BLAZE_ROD:
            case LAVA_BUCKET:
            case OAK_PLANKS:
            case BIRCH_PLANKS:
            case SPRUCE_PLANKS:
            case JUNGLE_PLANKS:
            case ACACIA_PLANKS:
            case DARK_OAK_PLANKS:
            case MANGROVE_PLANKS:
            case CHERRY_PLANKS:
            case CRIMSON_PLANKS:
            case WARPED_PLANKS:
            case STICK:
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case CHERRY_LOG:
            case STRIPPED_OAK_LOG:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_MANGROVE_LOG:
            case STRIPPED_CHERRY_LOG:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get the fuel value for an item - ENHANCED: Check custom fuels first
     */
    public static int getFuelValue(ItemStack item) {
        if (item == null) return 0;
        
        // Check custom fuels first
        for (CustomFuelData fuel : getInstance().customFuels) {
            if (fuel.matchesFuel(item)) {
                return fuel.getBurnTime();
            }
        }
        
        // Fall back to vanilla material-based fuel values
        return FurnaceData.getFuelValue(item.getType());
    }
    
    /**
     * Remove a custom recipe - NEW METHOD
     */
    public void removeCustomRecipe(String recipeId) {
        customRecipes.removeIf(recipe -> recipe.getRecipeId().equals(recipeId));
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Removed custom recipe: " + recipeId);
        }
    }
    
    /**
     * Remove a custom fuel - NEW METHOD
     */
    public void removeCustomFuel(String fuelId) {
        customFuels.removeIf(fuel -> fuel.getFuelId().equals(fuelId));
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Removed custom fuel: " + fuelId);
        }
    }
    
    /**
     * Get all custom recipes - NEW METHOD
     */
    public List<CustomFurnaceRecipe> getCustomRecipes() {
        return new ArrayList<>(customRecipes);
    }
    
    /**
     * Get all custom fuels - NEW METHOD
     */
    public List<CustomFuelData> getCustomFuels() {
        return new ArrayList<>(customFuels);
    }

    /**
     * Check if a furnace is currently being accessed by another player - NEW METHOD
     */
    public boolean isFurnaceInUse(Location location, Player player) {
        String locationKey = locationToString(location);
        UUID currentUser = furnaceAccessMap.get(locationKey);
        
        // Furnace is not in use
        if (currentUser == null) {
            return false;
        }
        
        // Same player is accessing it
        if (currentUser.equals(player.getUniqueId())) {
            return false;
        }
        
        // Check if the current user is still online and has the furnace open
        Player currentPlayer = Bukkit.getPlayer(currentUser);
        if (currentPlayer == null || !currentPlayer.isOnline()) {
            // Player is offline, release the furnace
            releaseFurnaceAccess(location);
            return false;
        }
        
        // Check if the player still has the furnace GUI open
        if (currentPlayer.getOpenInventory() == null || 
            !CustomFurnaceGUI.GUI_TITLE.equals(currentPlayer.getOpenInventory().getTitle())) {
            // Player doesn't have furnace GUI open anymore, release the furnace
            releaseFurnaceAccess(location);
            return false;
        }
        
        // Furnace is actively being used by another player
        return true;
    }
    
    /**
     * Acquire exclusive access to a furnace for a player - NEW METHOD
     */
    public boolean acquireFurnaceAccess(Location location, Player player) {
        String locationKey = locationToString(location);
        UUID playerId = player.getUniqueId();
        
        // Check if player already has access to this furnace
        if (playerId.equals(furnaceAccessMap.get(locationKey))) {
            return true;
        }
        
        // Check if furnace is in use by another player
        if (isFurnaceInUse(location, player)) {
            UUID currentUser = furnaceAccessMap.get(locationKey);
            Player currentPlayer = Bukkit.getPlayer(currentUser);
            
            if (currentPlayer != null) {
                player.sendMessage(ChatColor.RED + "This furnace is currently being used by " + 
                    currentPlayer.getName() + "!");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] " + player.getName() + " tried to access furnace in use by " + 
                        currentPlayer.getName());
                }
            }
            return false;
        }
        
        // Release any previous furnace access for this player
        String previousFurnace = playerAccessMap.get(playerId);
        if (previousFurnace != null) {
            furnaceAccessMap.remove(previousFurnace);
        }
        
        // Grant access to this furnace
        furnaceAccessMap.put(locationKey, playerId);
        playerAccessMap.put(playerId, locationKey);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] " + player.getName() + " acquired access to furnace at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
        
        return true;
    }
    
    /**
     * Release a player's access to a furnace - NEW METHOD
     */
    public void releaseFurnaceAccess(Location location) {
        String locationKey = locationToString(location);
        UUID playerId = furnaceAccessMap.remove(locationKey);
        
        if (playerId != null) {
            playerAccessMap.remove(playerId);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Player player = Bukkit.getPlayer(playerId);
                String playerName = player != null ? player.getName() : playerId.toString();
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Released furnace access for " + playerName + " at " + 
                    location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            }
        }
    }
    
    /**
     * Release a player's access to any furnace they might be using - NEW METHOD
     */
    public void releasePlayerAccess(Player player) {
        UUID playerId = player.getUniqueId();
        String furnaceLocation = playerAccessMap.remove(playerId);
        
        if (furnaceLocation != null) {
            furnaceAccessMap.remove(furnaceLocation);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Released all furnace access for " + player.getName());
            }
        }
    }
    
    /**
     * Get the player currently accessing a furnace - NEW METHOD
     */
    public Player getFurnaceUser(Location location) {
        String locationKey = locationToString(location);
        UUID playerId = furnaceAccessMap.get(locationKey);
        
        if (playerId != null) {
            return Bukkit.getPlayer(playerId);
        }
        
        return null;
    }
    
    /**
     * Check if a player has access to a specific furnace - NEW METHOD
     */
    public boolean hasAccessToFurnace(Location location, Player player) {
        String locationKey = locationToString(location);
        UUID currentUser = furnaceAccessMap.get(locationKey);
        
        return player.getUniqueId().equals(currentUser);
    }
    
    /**
     * Initialize vanilla smelting recipes
     */
    private void initializeSmeltingRecipes() {
        // Load vanilla furnace recipes
        for (Recipe recipe : Bukkit.getRecipesFor(new ItemStack(Material.IRON_INGOT))) {
            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                ItemStack input = furnaceRecipe.getInput();
                ItemStack result = furnaceRecipe.getResult();
                smeltingRecipes.put(input.getType(), result);
            }
        }
        
        // Ores to ingots
        smeltingRecipes.put(Material.RAW_IRON, ItemManager.applyRarity(new ItemStack(Material.IRON_INGOT)));
        smeltingRecipes.put(Material.IRON_ORE, ItemManager.applyRarity(new ItemStack(Material.IRON_INGOT)));
        smeltingRecipes.put(Material.RAW_GOLD, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        smeltingRecipes.put(Material.GOLD_ORE, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        smeltingRecipes.put(Material.RAW_COPPER, ItemManager.applyRarity(new ItemStack(Material.COPPER_INGOT)));
        smeltingRecipes.put(Material.COPPER_ORE, ItemManager.applyRarity(new ItemStack(Material.COPPER_INGOT)));
        smeltingRecipes.put(Material.NETHER_GOLD_ORE, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        smeltingRecipes.put(Material.ANCIENT_DEBRIS, ItemManager.applyRarity(new ItemStack(Material.NETHERITE_SCRAP)));
        
        // Other ores
        smeltingRecipes.put(Material.COAL_ORE, ItemManager.applyRarity(new ItemStack(Material.COAL)));
        smeltingRecipes.put(Material.DIAMOND_ORE, ItemManager.applyRarity(new ItemStack(Material.DIAMOND)));
        smeltingRecipes.put(Material.EMERALD_ORE, ItemManager.applyRarity(new ItemStack(Material.EMERALD)));
        smeltingRecipes.put(Material.REDSTONE_ORE, ItemManager.applyRarity(new ItemStack(Material.REDSTONE, 4)));
        smeltingRecipes.put(Material.LAPIS_ORE, ItemManager.applyRarity(new ItemStack(Material.LAPIS_LAZULI, 4)));
        smeltingRecipes.put(Material.NETHER_QUARTZ_ORE, ItemManager.applyRarity(new ItemStack(Material.QUARTZ)));
        
        // Food items
        smeltingRecipes.put(Material.BEEF, ItemManager.applyRarity(new ItemStack(Material.COOKED_BEEF)));
        smeltingRecipes.put(Material.PORKCHOP, ItemManager.applyRarity(new ItemStack(Material.COOKED_PORKCHOP)));
        smeltingRecipes.put(Material.CHICKEN, ItemManager.applyRarity(new ItemStack(Material.COOKED_CHICKEN)));
        smeltingRecipes.put(Material.MUTTON, ItemManager.applyRarity(new ItemStack(Material.COOKED_MUTTON)));
        smeltingRecipes.put(Material.RABBIT, ItemManager.applyRarity(new ItemStack(Material.COOKED_RABBIT)));
        smeltingRecipes.put(Material.COD, ItemManager.applyRarity(new ItemStack(Material.COOKED_COD)));
        smeltingRecipes.put(Material.SALMON, ItemManager.applyRarity(new ItemStack(Material.COOKED_SALMON)));
        smeltingRecipes.put(Material.POTATO, ItemManager.applyRarity(new ItemStack(Material.BAKED_POTATO)));
        smeltingRecipes.put(Material.KELP, ItemManager.applyRarity(new ItemStack(Material.DRIED_KELP)));
        
        // Building materials
        smeltingRecipes.put(Material.COBBLESTONE, ItemManager.applyRarity(new ItemStack(Material.STONE)));
        smeltingRecipes.put(Material.STONE, ItemManager.applyRarity(new ItemStack(Material.SMOOTH_STONE)));
        smeltingRecipes.put(Material.SAND, ItemManager.applyRarity(new ItemStack(Material.GLASS)));
        smeltingRecipes.put(Material.NETHERRACK, ItemManager.applyRarity(new ItemStack(Material.NETHER_BRICK)));
        smeltingRecipes.put(Material.CLAY_BALL, ItemManager.applyRarity(new ItemStack(Material.BRICK)));
        smeltingRecipes.put(Material.WET_SPONGE, ItemManager.applyRarity(new ItemStack(Material.SPONGE)));
        smeltingRecipes.put(Material.CACTUS, ItemManager.applyRarity(new ItemStack(Material.GREEN_DYE)));
        
        // Logs to charcoal
        smeltingRecipes.put(Material.OAK_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.BIRCH_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.SPRUCE_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.JUNGLE_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.ACACIA_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.DARK_OAK_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.MANGROVE_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.CHERRY_LOG, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.CRIMSON_STEM, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        smeltingRecipes.put(Material.WARPED_STEM, ItemManager.applyRarity(new ItemStack(Material.CHARCOAL)));
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Initialized " + smeltingRecipes.size() + " smelting recipes with rarity");
        }
    }
    
    /**
     * Get or create furnace data for a location
     */
    public FurnaceData getFurnaceData(Location location) {
        String key = locationToString(location);
        return furnaceDataMap.computeIfAbsent(key, k -> new FurnaceData(location));
    }
    
    /**
     * Remove furnace data for a location
     */
    public void removeFurnaceData(Location location) {
        String key = locationToString(location);
        furnaceDataMap.remove(key);
    }
    
    /**
     * Convert location to string key
     */
    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + 
               location.getBlockX() + "," + 
               location.getBlockY() + "," + 
               location.getBlockZ();
    }
    
    /**
     * Try to start smelting process - FIXED: Only consume fuel when all conditions are met
     */
    public boolean tryStartSmelting(FurnaceData furnaceData) {
        // Check if already active and has fuel
        if (furnaceData.isActive() && furnaceData.getFuelTime() > 0) {
            return true;
        }
        
        // Check for input item
        ItemStack inputItem = furnaceData.getInputItem();
        if (inputItem == null || inputItem.getType() == Material.AIR || inputItem.getAmount() <= 0) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Cannot start smelting - no input item");
            }
            return false;
        }
        
        // Check if item can be smelted
        if (!canSmelt(inputItem)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Cannot start smelting - item cannot be smelted: " + getItemDebugName(inputItem));
            }
            return false;
        }
        
        // Get smelting result
        ItemStack result = getSmeltingResult(inputItem);
        if (result == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Cannot start smelting - no recipe result");
            }
            return false;
        }
        
        // CRITICAL: Check if output can accept result BEFORE consuming fuel
        ItemStack currentOutput = furnaceData.getOutputItem();
        if (currentOutput != null && currentOutput.getType() != Material.AIR) {
            if (currentOutput.getType() != result.getType() || 
                (currentOutput.getAmount() + result.getAmount()) > currentOutput.getMaxStackSize()) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Cannot start smelting - output slot full or incompatible");
                }
                return false;
            }
        }
        
        // ENHANCED: Only consume new fuel if we don't have any burning fuel AND all conditions are met
        if (furnaceData.getFuelTime() <= 0) {
            ItemStack fuelItem = furnaceData.getFuelItem();
            if (fuelItem == null || fuelItem.getType() == Material.AIR || fuelItem.getAmount() <= 0) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Cannot start smelting - no fuel item");
                }
                return false;
            }
            
            // ENHANCED: Check if fuel is valid using ItemStack
            if (!isFuel(fuelItem)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Cannot start smelting - invalid fuel: " + getItemDebugName(fuelItem));
                }
                return false;
            }
            
            // ENHANCED: Only consume fuel if ALL conditions are met (input, recipe, output space)
            int fuelValue = getFuelValue(fuelItem);
            furnaceData.setFuelTime(fuelValue);
            furnaceData.setMaxFuelTime(fuelValue);
            furnaceData.setHasFuel(true);
            
            // CRITICAL: Consume one fuel item immediately and update FurnaceData
            ItemStack originalFuel = fuelItem.clone();
            if (fuelItem.getAmount() > 1) {
                fuelItem.setAmount(fuelItem.getAmount() - 1);
                furnaceData.setFuelItem(fuelItem);
            } else {
                furnaceData.setFuelItem(null);
            }
            
            // CRITICAL: Immediately update all open GUIs for this furnace to show fuel consumption
            updateAllGUIsForFurnace(furnaceData);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Consumed fuel for guaranteed smelting: " + originalFuel.getType() + 
                    ", " + fuelValue + " ticks remaining, fuel slot now: " + 
                    (fuelItem != null ? fuelItem.getAmount() + "x " + fuelItem.getType() : "empty"));
            }
        } else {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Using existing fuel - " + furnaceData.getFuelTime() + " ticks remaining");
            }
        }
        
        // Start smelting (we have fuel and valid input and can output)
        ItemStack resultClone = result.clone();
        furnaceData.setSmeltingResult(resultClone);
        furnaceData.setActive(true);
        
        // Set cook time based on item type
        int cookTime = getCookTime(inputItem);
        furnaceData.setMaxSmeltTime(cookTime);
        
        // Don't reset smelt time if we're resuming after adding fuel
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Started smelting: " + getItemDebugName(inputItem) + " -> " + getItemDebugName(result) +
                " (cook time: " + furnaceData.getMaxSmeltTime() + " ticks)");
        }
        
        return true;
    }

    /**
     * Update all open GUIs for a specific furnace - NEW METHOD
     */
    private void updateAllGUIsForFurnace(FurnaceData furnaceData) {
        try {
            Location furnaceLocation = furnaceData.getLocation();
            
            // Get all players who have this furnace open
            Map<Player, Location> activeGUIs = CustomFurnaceGUI.getActiveFurnaceGUIs();
            
            for (Map.Entry<Player, Location> entry : activeGUIs.entrySet()) {
                Player player = entry.getKey();
                Location guiLocation = entry.getValue();
                
                // Check if this player has the same furnace open
                if (guiLocation.equals(furnaceLocation)) {
                    // Update this player's GUI immediately
                    Inventory gui = player.getOpenInventory().getTopInventory();
                    if (CustomFurnaceGUI.isCustomFurnaceGUI(gui)) {
                        // Load current furnace contents into the GUI
                        CustomFurnaceGUI.loadFurnaceContents(gui, furnaceData);
                        
                        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                            Main.getInstance().debugLog(DebugSystem.GUI, 
                                "[Custom Furnace] Updated GUI for " + player.getName() + 
                                " after fuel consumption");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error updating GUIs for furnace: " + e.getMessage());
        }
    }

    private static String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
    
    /**
     * Start the furnace update task - ENHANCED: Include access cleanup
     */
    private void startFurnaceUpdateTask() {
        BukkitRunnable runnable = new BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                try {
                    tickCounter++;
                    
                    // Update all furnace data every tick (for accurate processing)
                    for (FurnaceData furnaceData : furnaceDataMap.values()) {
                        // ENHANCED: Always try to start smelting if conditions are met
                        if (!furnaceData.isActive() && furnaceData.getInputItem() != null && 
                            furnaceData.getInputItem().getType() != Material.AIR) {
                            
                            boolean started = tryStartSmelting(furnaceData);
                            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && started) {
                                Main.getInstance().debugLog(DebugSystem.GUI, 
                                    "[Custom Furnace] Successfully started smelting");
                            }
                        }
                        
                        // Tick the furnace
                        furnaceData.tick();
                    }
                    
                    // Update GUI displays every 5 ticks for better responsiveness during debugging
                    if (tickCounter % 5 == 0) {
                        updateAllFurnaceTimersSafely();
                    }
                    
                    // Update block animations every 20 ticks to reduce lag
                    if (tickCounter % 20 == 0) {
                        updateAllFurnaceBlockStates();
                    }
                    
                    // NEW: Clean up stale furnace access every 5 seconds (100 ticks)
                    if (tickCounter % 100 == 0) {
                        cleanupStaleAccess();
                    }
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("Error in furnace update task: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        // Run every tick for furnace processing
        furnaceUpdateTask = runnable.runTaskTimer(Main.getInstance(), 1L, 1L);
    }

    /**
     * Clean up stale furnace access entries - NEW METHOD
     */
    private void cleanupStaleAccess() {
        Iterator<Map.Entry<String, UUID>> iterator = furnaceAccessMap.entrySet().iterator();
        int cleanedUp = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, UUID> entry = iterator.next();
            String locationKey = entry.getKey();
            UUID playerId = entry.getValue();
            
            Player player = Bukkit.getPlayer(playerId);
            boolean shouldRemove = false;
            
            // Player is offline
            if (player == null || !player.isOnline()) {
                shouldRemove = true;
            }
            // Player doesn't have furnace GUI open
            else if (player.getOpenInventory() == null || 
                    !CustomFurnaceGUI.GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                shouldRemove = true;
            }
            // Player is not associated with this furnace location
            else {
                Location playerFurnaceLocation = CustomFurnaceGUI.getFurnaceLocation(player);
                if (playerFurnaceLocation == null || 
                    !locationKey.equals(locationToString(playerFurnaceLocation))) {
                    shouldRemove = true;
                }
            }
            
            if (shouldRemove) {
                iterator.remove();
                playerAccessMap.remove(playerId);
                cleanedUp++;
            }
        }
        
        if (cleanedUp > 0 && Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Cleaned up " + cleanedUp + " stale access entries");
        }
    }

    /**
     * Update the physical furnace block to show cooking animation - NEW METHOD
     */
    private void updateFurnaceBlockState(Location location, FurnaceData furnaceData) {
        try {
            Block block = location.getBlock();
            if (block.getType() != Material.FURNACE) {
                // Furnace was removed, clean up data
                removeFurnaceData(location);
                return;
            }
            
            // Get the block data for animation
            if (block.getBlockData() instanceof Lightable) {
                Lightable furnaceData_block = (Lightable) block.getBlockData();
                boolean shouldBeLit = furnaceData.shouldShowCookingAnimation();
                
                // Only update if the state needs to change (reduces unnecessary block updates)
                if (furnaceData_block.isLit() != shouldBeLit) {
                    furnaceData_block.setLit(shouldBeLit);
                    block.setBlockData(furnaceData_block);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Updated furnace animation at " + 
                            location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + 
                            " - lit: " + shouldBeLit);
                    }
                }
            }
            
            // Update the furnace tile entity for burn time animation
            if (block.getState() instanceof Furnace) {
                Furnace furnaceState = (Furnace) block.getState();
                
                if (furnaceData.shouldShowCookingAnimation()) {
                    // Set burn time for animation (scaled to match our fuel time)
                    short burnTime = (short) Math.max(1, Math.min(200, furnaceData.getRemainingFuelTime() / 10));
                    furnaceState.setBurnTime(burnTime);
                    
                    // Set cook time for cooking animation
                    short cookTime = (short) Math.max(0, Math.min(200, furnaceData.getSmeltTime()));
                    furnaceState.setCookTime(cookTime);
                    
                    // Set cook time total for the cooking progress bar
                    furnaceState.setCookTimeTotal((short) furnaceData.getMaxSmeltTime());
                } else {
                    // Reset animation values when not cooking
                    furnaceState.setBurnTime((short) 0);
                    furnaceState.setCookTime((short) 0);
                    furnaceState.setCookTimeTotal((short) 200);
                }
                
                // Update the block state
                furnaceState.update(false, false); // Don't force update or apply physics
            }
            
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error updating block state: " + e.getMessage());
            }
        }
    }

    /**
     * Update all furnace block states safely
     */
    private void updateAllFurnaceBlockStates() {
        for (Map.Entry<String, FurnaceData> entry : furnaceDataMap.entrySet()) {
            try {
                FurnaceData furnaceData = entry.getValue();
                Location location = furnaceData.getLocation();
                updateFurnaceBlockState(location, furnaceData);
            } catch (Exception e) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Error updating block state: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Update all open furnace timer displays safely - FIXED: Don't block smelting with interaction checks
     */
    public static void updateAllFurnaceTimersSafely() {
        // Create a copy of the map to avoid concurrent modification
        Map<Player, Location> activeGUIsCopy = new HashMap<>(CustomFurnaceGUI.getActiveFurnaceGUIs());
        
        for (Map.Entry<Player, Location> entry : activeGUIsCopy.entrySet()) {
            Player player = entry.getKey();
            Location location = entry.getValue();
            
            try {
                // Validate player and GUI state
                if (!player.isOnline()) {
                    CustomFurnaceGUI.removeActiveFurnaceGUI(player);
                    continue;
                }
                
                if (player.getOpenInventory() == null || 
                    !CustomFurnaceGUI.GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                    CustomFurnaceGUI.removeActiveFurnaceGUI(player);
                    continue;
                }
                
                FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(location);
                Inventory gui = player.getOpenInventory().getTopInventory();
                
                // ALWAYS update timer displays - these don't interfere with items
                updateCookTimerSafely(gui, furnaceData);
                updateFuelTimerSafely(gui, furnaceData);
                
                // ONLY sync items when safe AND when there are actual changes from furnace processing
                updateFurnaceItemsSafely(gui, furnaceData, player);
                
            } catch (Exception e) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Error updating timers for " + player.getName() + ": " + e.getMessage());
                }
                // Remove problematic GUI to prevent repeated errors
                CustomFurnaceGUI.removeActiveFurnaceGUI(player);
            }
        }
    }

    /**
     * Update furnace items safely during live updates - ENHANCED: Better output handling
     */
    private static void updateFurnaceItemsSafely(Inventory gui, FurnaceData furnaceData, Player player) {
        try {
            UUID playerId = player.getUniqueId();
            
            // Check if player is actively clicking
            boolean isActivelyClicking = isPlayerActivelyClicking(playerId);
            boolean hasRecentViolations = CustomFurnaceListener.hasRecentViolations(playerId);
            
            // ENHANCED: Only sync output when it's clearly a furnace operation, not player action
            ItemStack currentOutputInGUI = gui.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
            ItemStack expectedOutput = furnaceData.getOutputItem();
            
            // CRITICAL: Don't sync output if player is actively clicking or has cursor item
            boolean playerHasCursorItem = player.getItemOnCursor() != null && 
                                        player.getItemOnCursor().getType() != Material.AIR;
            
            if (!isActivelyClicking && !hasRecentViolations && !playerHasCursorItem) {
                if (shouldSyncOutput(currentOutputInGUI, expectedOutput)) {
                    gui.setItem(CustomFurnaceGUI.OUTPUT_SLOT, expectedOutput != null ? expectedOutput.clone() : null);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI, 
                            "[Custom Furnace] Synced output production for " + player.getName() + 
                            " - new: " + getItemDebugName(expectedOutput));
                    }
                }
            } else {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    String reason = isActivelyClicking ? "player actively clicking" : 
                                hasRecentViolations ? "recent violations detected" : "player has cursor item";
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Skipping output sync - " + reason);
                }
            }
            
            // ENHANCED: Be more conservative about input/fuel syncing during rapid clicking
            if (isActivelyClicking || hasRecentViolations) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    String reason = isActivelyClicking ? "player actively clicking" : "recent violations detected";
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Skipping input/fuel sync - " + reason + " (output handling complete)");
                }
                return;
            }
            
            // IMPROVED: Only sync input when it's clearly been consumed by the furnace
            ItemStack currentInputInGUI = gui.getItem(CustomFurnaceGUI.INPUT_SLOT);
            ItemStack expectedInput = furnaceData.getInputItem();
            
            // More conservative input syncing - only when clearly consumed
            if (currentInputInGUI != null && expectedInput == null && furnaceData.isActive()) {
                gui.setItem(CustomFurnaceGUI.INPUT_SLOT, null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Synced input consumption for " + player.getName() + 
                        " - consumed: " + getItemDebugName(currentInputInGUI));
                }
            }
            
            // IMPROVED: Only sync fuel when it's clearly been consumed by the furnace
            ItemStack currentFuelInGUI = gui.getItem(CustomFurnaceGUI.FUEL_SLOT);
            ItemStack expectedFuel = furnaceData.getFuelItem();
            
            // More conservative fuel syncing - only when clearly consumed
            if (currentFuelInGUI != null && expectedFuel == null && furnaceData.hasFuel()) {
                gui.setItem(CustomFurnaceGUI.FUEL_SLOT, null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Synced fuel consumption for " + player.getName() + 
                        " - consumed: " + getItemDebugName(currentFuelInGUI));
                }
            }
            
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[Custom Furnace] Error updating furnace items safely: " + e.getMessage());
        }
    }

    /**
     * Check if output should be synced - ENHANCED: Prevent duplication during player actions
     */
    private static boolean shouldSyncOutput(ItemStack currentOutput, ItemStack expectedOutput) {
        // Never sync if both are null
        if ((currentOutput == null || currentOutput.getType() == Material.AIR) && 
            (expectedOutput == null || expectedOutput.getType() == Material.AIR)) {
            return false;
        }
        
        // Only sync if output was produced from nothing (new smelting result)
        if ((currentOutput == null || currentOutput.getType() == Material.AIR) && 
            expectedOutput != null && expectedOutput.getType() != Material.AIR) {
            return true; // New output produced
        }
        
        // ENHANCED: Only sync if output amount increased (more items smelted)
        // Don't sync if amount decreased (that means player took items)
        if (currentOutput != null && expectedOutput != null &&
            currentOutput.getType() == expectedOutput.getType()) {
            if (currentOutput.getAmount() < expectedOutput.getAmount()) {
                return true; // More items were smelted
            }
            // Don't sync if amount decreased or stayed the same
            return false;
        }
        
        // Sync if output type changed (different recipe result)
        if (currentOutput != null && expectedOutput != null &&
            currentOutput.getType() != expectedOutput.getType()) {
            return true; // Different item was produced
        }
        
        return false;
    }

    /**
     * Check if input should be synced due to furnace consumption - IMPROVED
     */
    private static boolean shouldSyncInputConsumption(ItemStack currentInput, ItemStack expectedInput) {
        // If both are null or air, no sync needed
        if ((currentInput == null || currentInput.getType() == Material.AIR) && 
            (expectedInput == null || expectedInput.getType() == Material.AIR)) {
            return false;
        }
        
        // If GUI has item but furnace data shows null - item was completely consumed
        if ((currentInput != null && currentInput.getType() != Material.AIR) && 
            (expectedInput == null || expectedInput.getType() == Material.AIR)) {
            return true;
        }
        
        // If both have the same type but expected amount is less - item was partially consumed
        if (currentInput != null && expectedInput != null &&
            currentInput.getType() == expectedInput.getType() &&
            currentInput.getAmount() > expectedInput.getAmount()) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if player is actively clicking - IMPROVED timing
     */
    private static boolean isPlayerActivelyClicking(UUID playerId) {
        Map<UUID, Long> lastClickMap = getLastClickTimeMap();
        if (lastClickMap == null) return false;
        
        Long lastClickTime = lastClickMap.get(playerId);
        if (lastClickTime == null) return false;
        
        long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
        
        // IMPROVED: Longer protection window to prevent conflicts
        return timeSinceLastClick < 500L; // Increased from 100ms to 500ms
    }

    /**
     * Check if fuel should be synced - IMPROVED logic
     */
    private static boolean shouldSyncFuelConsumption(ItemStack currentFuel, ItemStack expectedFuel) {
        // Only sync if fuel was clearly consumed by the system (not by player action)
        if (currentFuel == null && expectedFuel == null) return false;
        
        // If we have fuel in GUI but furnace data says it should be null (consumed)
        if (currentFuel != null && expectedFuel == null) {
            return true; // Fuel was consumed
        }
        
        // If amounts differ (partial consumption)
        if (currentFuel != null && expectedFuel != null) {
            if (currentFuel.getType() == expectedFuel.getType() && 
                currentFuel.getAmount() > expectedFuel.getAmount()) {
                return true; // Fuel was partially consumed
            }
        }
        
        return false;
    }

    /**
     * Safely sync input items - only when consumed by furnace processing
     */
    private static void syncInputSafely(Inventory gui, FurnaceData furnaceData, Player player) {
        try {
            ItemStack currentInputInGUI = gui.getItem(CustomFurnaceGUI.INPUT_SLOT);
            ItemStack expectedInput = furnaceData.getInputItem();
            
            // Only sync if items were consumed by the furnace (amount decreased or became null)
            if (shouldSyncInputConsumption(currentInputInGUI, expectedInput)) {
                gui.setItem(CustomFurnaceGUI.INPUT_SLOT, 
                    expectedInput != null ? expectedInput.clone() : null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Synced input consumption for " + player.getName() + 
                        " - was: " + getItemDebugName(currentInputInGUI) + 
                        " now: " + getItemDebugName(expectedInput));
                }
            }
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error syncing input: " + e.getMessage());
            }
        }
    }

    /**
     * Safely sync fuel items - only when consumed by furnace processing
     */
    private static void syncFuelSafely(Inventory gui, FurnaceData furnaceData, Player player) {
        try {
            ItemStack currentFuelInGUI = gui.getItem(CustomFurnaceGUI.FUEL_SLOT);
            ItemStack expectedFuel = furnaceData.getFuelItem();
            
            // Only sync if fuel was consumed by the furnace (amount decreased or became null)
            if (shouldSyncFuelConsumption(currentFuelInGUI, expectedFuel)) {
                gui.setItem(CustomFurnaceGUI.FUEL_SLOT, 
                    expectedFuel != null ? expectedFuel.clone() : null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Synced fuel consumption for " + player.getName() + 
                        " - was: " + getItemDebugName(currentFuelInGUI) + 
                        " now: " + getItemDebugName(expectedFuel));
                }
            }
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error syncing fuel: " + e.getMessage());
            }
        }
    }

    /**
     * Get the last click time map from CustomFurnaceListener - HELPER METHOD
     */
    private static Map<UUID, Long> getLastClickTimeMap() {
        // We need to access this from CustomFurnaceListener
        // This is a temporary solution - ideally we'd have a shared interaction tracker
        return CustomFurnaceListener.getLastClickTimeMap();
    }

    /**
     * Check if two items are equal for GUI update purposes - NEW METHOD
     */
    private static boolean itemsEqual(ItemStack item1, ItemStack item2) {
        // Both null
        if (item1 == null && item2 == null) return true;
        
        // One null, one not
        if (item1 == null || item2 == null) return false;
        
        // Both air
        if (item1.getType() == Material.AIR && item2.getType() == Material.AIR) return true;
        
        // One air, one not
        if (item1.getType() == Material.AIR || item2.getType() == Material.AIR) return false;
        
        // Compare type and amount
        if (item1.getType() != item2.getType()) return false;
        if (item1.getAmount() != item2.getAmount()) return false;
        
        // For our purposes, this is sufficient comparison
        return true;
    }

    /**
     * Drop furnace contents with smart positioning - ENHANCED VERSION (Fixed null handling)
     */
    public void dropFurnaceContentsEnhanced(Location furnaceLocation, Player breaker) {
        try {
            FurnaceData furnaceData = getFurnaceData(furnaceLocation);
            if (furnaceData == null) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] No furnace data found for enhanced drop");
                }
                return;
            }
            
            // Ensure we have a valid world
            org.bukkit.World world = furnaceLocation.getWorld();
            if (world == null) {
                if (breaker != null) {
                    breaker.sendMessage(ChatColor.RED + "Error: Cannot drop items - invalid world!");
                }
                Main.getInstance().getLogger().warning("[Custom Furnace] Cannot drop items - world is null");
                return;
            }
            
            // Calculate drop positions around the furnace
            Location[] dropPositions = calculateDropPositions(furnaceLocation);
            int positionIndex = 0;
            int totalDropped = 0;
            
            // Drop input item
            ItemStack inputItem = furnaceData.getInputItem();
            if (inputItem != null && inputItem.getType() != Material.AIR && inputItem.getAmount() > 0) {
                Location dropPos = dropPositions[positionIndex % dropPositions.length];
                world.dropItemNaturally(dropPos, inputItem.clone());
                positionIndex++;
                totalDropped++;
                
                // Send message to breaker (if present)
                if (breaker != null) {
                    breaker.sendMessage(ChatColor.YELLOW + "Dropped " + getItemDisplayName(inputItem) + 
                        " from furnace input slot");
                }
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Enhanced drop - input: " + getItemDisplayName(inputItem));
                }
            }
            
            // Drop fuel item
            ItemStack fuelItem = furnaceData.getFuelItem();
            if (fuelItem != null && fuelItem.getType() != Material.AIR && fuelItem.getAmount() > 0) {
                Location dropPos = dropPositions[positionIndex % dropPositions.length];
                world.dropItemNaturally(dropPos, fuelItem.clone());
                positionIndex++;
                totalDropped++;
                
                if (breaker != null) {
                    breaker.sendMessage(ChatColor.YELLOW + "Dropped " + getItemDisplayName(fuelItem) + 
                        " from furnace fuel slot");
                }
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Enhanced drop - fuel: " + getItemDisplayName(fuelItem));
                }
            }
            
            // Drop output item
            ItemStack outputItem = furnaceData.getOutputItem();
            if (outputItem != null && outputItem.getType() != Material.AIR && outputItem.getAmount() > 0) {
                Location dropPos = dropPositions[positionIndex % dropPositions.length];
                world.dropItemNaturally(dropPos, outputItem.clone());
                positionIndex++;
                totalDropped++;
                
                if (breaker != null) {
                    breaker.sendMessage(ChatColor.YELLOW + "Dropped " + getItemDisplayName(outputItem) + 
                        " from furnace output slot");
                }
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Enhanced drop - output: " + getItemDisplayName(outputItem));
                }
            }
            
            if (totalDropped == 0) {
                if (breaker != null) {
                    breaker.sendMessage(ChatColor.GRAY + "Furnace was empty");
                }
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Enhanced drop - furnace was empty");
                }
            }
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Enhanced drop completed: " + totalDropped + " item stack(s)");
            }
            
        } catch (Exception e) {
            if (breaker != null) {
                breaker.sendMessage(ChatColor.RED + "Error dropping furnace contents!");
            }
            Main.getInstance().getLogger().warning("[Custom Furnace] Error in enhanced drop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clean up furnace data without dropping items - NEW METHOD
     */
    public void cleanupFurnaceDataOnly(Location location) {
        String key = locationToString(location);
        
        // Remove the furnace data WITHOUT dropping contents
        FurnaceData data = furnaceDataMap.remove(key);
        
        if (data != null && Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Cleaned up data for broken furnace at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }

    /**
     * Update handleFurnaceBreak to use the enhanced method
     */
    public void handleFurnaceBreak(Location location) {
        String key = locationToString(location);
        
        // UPDATED: Don't drop contents here since enhanced method already handles it
        // Just remove the furnace data
        FurnaceData data = furnaceDataMap.remove(key);
        
        if (data != null && Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Cleaned up data for broken furnace at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
    }

    /**
     * Update dropFurnaceContents to use enhanced method (for backward compatibility)
     */
    public void dropFurnaceContents(Location location) {
        // For backward compatibility, just call the enhanced version with null player
        dropFurnaceContentsEnhanced(location, null);
    }

    /**
     * Update cook timer safely without touching item slots - ENHANCED (make sure it's static)
     */
    private static void updateCookTimerSafely(Inventory gui, FurnaceData furnaceData) {
        try {
            // Create the timer display item
            ItemStack timerItem = createCookTimerDisplay(furnaceData);
            gui.setItem(CustomFurnaceGUI.COOK_TIMER_SLOT, timerItem);
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error updating cook timer: " + e.getMessage());
            }
        }
    }

    /**
     * Update fuel timer safely without touching item slots - ENHANCED (make sure it's static)
     */
    private static void updateFuelTimerSafely(Inventory gui, FurnaceData furnaceData) {
        try {
            // Create the timer display item
            ItemStack timerItem = createFuelTimerDisplay(furnaceData);
            gui.setItem(CustomFurnaceGUI.FUEL_TIMER_SLOT, timerItem);
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error updating fuel timer: " + e.getMessage());
            }
        }
    }

    /**
     * Create cook timer display item - NEW METHOD
     */
    private static ItemStack createCookTimerDisplay(FurnaceData furnaceData) {
        // Use the existing updateCookTimer logic but return the item instead of setting it
        return CustomFurnaceGUI.createCookTimerItem(furnaceData);
    }

    /**
     * Create fuel timer display item - NEW METHOD
     */
    private static ItemStack createFuelTimerDisplay(FurnaceData furnaceData) {
        // Use the existing updateFuelTimer logic but return the item instead of setting it
        return CustomFurnaceGUI.createFuelTimerItem(furnaceData);
    }
    
    /**
     * Stop the furnace update task
     */
    public void stopFurnaceUpdateTask() {
        if (furnaceUpdateTask != null) {
            furnaceUpdateTask.cancel();
            furnaceUpdateTask = null;
        }
    }

    /**
     * Calculate smart drop positions around the furnace - NEW METHOD
     */
    private Location[] calculateDropPositions(Location furnaceLocation) {
        Location[] positions = new Location[8];
        
        // Create positions in a circle around the furnace
        double[] xOffsets = {0.3, 0.7, 0.7, 0.3, -0.3, -0.7, -0.7, -0.3};
        double[] zOffsets = {0.7, 0.3, -0.3, -0.7, -0.7, -0.3, 0.3, 0.7};
        
        for (int i = 0; i < 8; i++) {
            positions[i] = furnaceLocation.clone().add(xOffsets[i], 0.5, zOffsets[i]);
        }
        
        return positions;
    }

    /**
     * Get display name for an item - NEW METHOD
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "Nothing";
        
        String itemName;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            itemName = item.getItemMeta().getDisplayName();
        } else {
            // Convert MATERIAL_NAME to Material Name
            itemName = item.getType().name().toLowerCase().replace("_", " ");
            itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
        }
        
        return item.getAmount() + "x " + itemName;
    }
    
    /**
     * Add a custom smelting recipe with automatic rarity application
     */
    public void addSmeltingRecipe(Material input, ItemStack result) {
        // Ensure the result has rarity applied
        ItemStack rarityResult = ItemManager.hasRarity(result) ? result.clone() : ItemManager.applyRarity(result.clone());
        smeltingRecipes.put(input, rarityResult);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Added custom recipe: " + input + " -> " + rarityResult.getType() + " with rarity");
        }
    }

    /**
     * Update all existing recipes to have rarity - UTILITY METHOD
     */
    public void updateRecipesWithRarity() {
        Map<Material, ItemStack> updatedRecipes = new HashMap<>();
        
        for (Map.Entry<Material, ItemStack> entry : smeltingRecipes.entrySet()) {
            Material input = entry.getKey();
            ItemStack result = entry.getValue();
            
            // Apply rarity if not already present
            if (!ItemManager.hasRarity(result)) {
                result = ItemManager.applyRarity(result);
            }
            
            updatedRecipes.put(input, result);
        }
        
        // Replace the old recipes
        smeltingRecipes.clear();
        smeltingRecipes.putAll(updatedRecipes);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Updated " + smeltingRecipes.size() + " recipes with rarity");
        }
    }
    
    /**
     * Remove a custom smelting recipe
     */
    public void removeSmeltingRecipe(Material input) {
        smeltingRecipes.remove(input);
    }

    /**
     * Get smelting recipes - HELPER METHOD with rarity application
     */
    private Map<Material, ItemStack> getSmeltingRecipes() {
        Map<Material, ItemStack> recipes = new HashMap<>();
        
        // Apply rarity to all recipe results
        recipes.put(Material.RAW_IRON, ItemManager.applyRarity(new ItemStack(Material.IRON_INGOT)));
        recipes.put(Material.IRON_ORE, ItemManager.applyRarity(new ItemStack(Material.IRON_INGOT)));
        recipes.put(Material.RAW_GOLD, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        recipes.put(Material.GOLD_ORE, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        recipes.put(Material.RAW_COPPER, ItemManager.applyRarity(new ItemStack(Material.COPPER_INGOT)));
        recipes.put(Material.COPPER_ORE, ItemManager.applyRarity(new ItemStack(Material.COPPER_INGOT)));
        recipes.put(Material.COAL_ORE, ItemManager.applyRarity(new ItemStack(Material.COAL)));
        recipes.put(Material.DIAMOND_ORE, ItemManager.applyRarity(new ItemStack(Material.DIAMOND)));
        recipes.put(Material.EMERALD_ORE, ItemManager.applyRarity(new ItemStack(Material.EMERALD)));
        recipes.put(Material.REDSTONE_ORE, ItemManager.applyRarity(new ItemStack(Material.REDSTONE, 4)));
        recipes.put(Material.LAPIS_ORE, ItemManager.applyRarity(new ItemStack(Material.LAPIS_LAZULI, 4)));
        recipes.put(Material.NETHER_QUARTZ_ORE, ItemManager.applyRarity(new ItemStack(Material.QUARTZ)));
        recipes.put(Material.NETHER_GOLD_ORE, ItemManager.applyRarity(new ItemStack(Material.GOLD_INGOT)));
        recipes.put(Material.ANCIENT_DEBRIS, ItemManager.applyRarity(new ItemStack(Material.NETHERITE_SCRAP)));
        
        // Food with rarity
        recipes.put(Material.BEEF, ItemManager.applyRarity(new ItemStack(Material.COOKED_BEEF)));
        recipes.put(Material.PORKCHOP, ItemManager.applyRarity(new ItemStack(Material.COOKED_PORKCHOP)));
        recipes.put(Material.CHICKEN, ItemManager.applyRarity(new ItemStack(Material.COOKED_CHICKEN)));
        recipes.put(Material.MUTTON, ItemManager.applyRarity(new ItemStack(Material.COOKED_MUTTON)));
        recipes.put(Material.COD, ItemManager.applyRarity(new ItemStack(Material.COOKED_COD)));
        recipes.put(Material.SALMON, ItemManager.applyRarity(new ItemStack(Material.COOKED_SALMON)));
        recipes.put(Material.POTATO, ItemManager.applyRarity(new ItemStack(Material.BAKED_POTATO)));
        recipes.put(Material.KELP, ItemManager.applyRarity(new ItemStack(Material.DRIED_KELP)));
        
        return recipes;
    }
}