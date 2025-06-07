package com.server.crafting.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import com.server.crafting.data.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.listeners.CustomFurnaceListener;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Manager for handling custom furnace operations and recipes
 */
public class CustomFurnaceManager {
    
    private static CustomFurnaceManager instance;
    private final Map<String, FurnaceData> furnaceDataMap;
    private final Map<Material, ItemStack> smeltingRecipes;
    private BukkitTask furnaceUpdateTask;
    
    private CustomFurnaceManager() {
        this.furnaceDataMap = new HashMap<>();
        this.smeltingRecipes = new HashMap<>();
        initializeSmeltingRecipes();
        startFurnaceUpdateTask();
    }
    
    public static CustomFurnaceManager getInstance() {
        if (instance == null) {
            instance = new CustomFurnaceManager();
        }
        return instance;
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
        
        // Add common smelting recipes manually to ensure they exist
        smeltingRecipes.put(Material.RAW_IRON, new ItemStack(Material.IRON_INGOT));
        smeltingRecipes.put(Material.RAW_GOLD, new ItemStack(Material.GOLD_INGOT));
        smeltingRecipes.put(Material.RAW_COPPER, new ItemStack(Material.COPPER_INGOT));
        smeltingRecipes.put(Material.COBBLESTONE, new ItemStack(Material.STONE));
        smeltingRecipes.put(Material.SAND, new ItemStack(Material.GLASS));
        smeltingRecipes.put(Material.CLAY_BALL, new ItemStack(Material.BRICK));
        smeltingRecipes.put(Material.NETHERRACK, new ItemStack(Material.NETHER_BRICK));
        smeltingRecipes.put(Material.CACTUS, new ItemStack(Material.GREEN_DYE));
        smeltingRecipes.put(Material.OAK_LOG, new ItemStack(Material.CHARCOAL));
        smeltingRecipes.put(Material.BIRCH_LOG, new ItemStack(Material.CHARCOAL));
        smeltingRecipes.put(Material.SPRUCE_LOG, new ItemStack(Material.CHARCOAL));
        smeltingRecipes.put(Material.JUNGLE_LOG, new ItemStack(Material.CHARCOAL));
        smeltingRecipes.put(Material.ACACIA_LOG, new ItemStack(Material.CHARCOAL));
        smeltingRecipes.put(Material.DARK_OAK_LOG, new ItemStack(Material.CHARCOAL));
        
        // Food items
        smeltingRecipes.put(Material.BEEF, new ItemStack(Material.COOKED_BEEF));
        smeltingRecipes.put(Material.PORKCHOP, new ItemStack(Material.COOKED_PORKCHOP));
        smeltingRecipes.put(Material.CHICKEN, new ItemStack(Material.COOKED_CHICKEN));
        smeltingRecipes.put(Material.MUTTON, new ItemStack(Material.COOKED_MUTTON));
        smeltingRecipes.put(Material.RABBIT, new ItemStack(Material.COOKED_RABBIT));
        smeltingRecipes.put(Material.COD, new ItemStack(Material.COOKED_COD));
        smeltingRecipes.put(Material.SALMON, new ItemStack(Material.COOKED_SALMON));
        smeltingRecipes.put(Material.POTATO, new ItemStack(Material.BAKED_POTATO));
        smeltingRecipes.put(Material.KELP, new ItemStack(Material.DRIED_KELP));
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
     * Check if an item can be smelted - PUBLIC method for validation
     */
    public boolean canSmelt(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        // Check if this item has a vanilla smelting recipe
        Material inputType = item.getType();
        
        // Common smeltable items
        switch (inputType) {
            case RAW_IRON:
            case RAW_GOLD:
            case RAW_COPPER:
            case IRON_ORE:
            case GOLD_ORE:
            case COPPER_ORE:
            case COAL_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case REDSTONE_ORE:
            case LAPIS_ORE:
            case NETHER_QUARTZ_ORE:
            case NETHER_GOLD_ORE:
            case ANCIENT_DEBRIS:
            case BEEF:
            case PORKCHOP:
            case CHICKEN:
            case MUTTON:
            case RABBIT:
            case COD:
            case SALMON:
            case POTATO:
            case KELP:
            case CACTUS:
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case CHERRY_LOG:
            case CRIMSON_STEM:
            case WARPED_STEM:
            case SAND:
            case COBBLESTONE:
            case STONE:
            case NETHERRACK:
            case CLAY_BALL:
            case WET_SPONGE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if an item can be used as fuel - PUBLIC method for validation
     */
    public boolean isFuel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        Material fuelType = item.getType();
        
        // Common fuel items
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
     * Get the smelting result for an item
     */
    public ItemStack getSmeltingResult(ItemStack item) {
        if (item == null) return null;
        ItemStack result = smeltingRecipes.get(item.getType());
        return result != null ? result.clone() : null;
    }
    
    /**
     * Try to start smelting process - FIXED: Only consume new fuel when current fuel is exhausted
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
                    "[Custom Furnace] Cannot start smelting - item cannot be smelted: " + inputItem.getType());
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
        
        // Check if output can accept result
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
        
        // ENHANCED: Only consume new fuel if we don't have any burning fuel
        if (furnaceData.getFuelTime() <= 0) {
            ItemStack fuelItem = furnaceData.getFuelItem();
            if (fuelItem == null || fuelItem.getType() == Material.AIR || fuelItem.getAmount() <= 0) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Cannot start smelting - no fuel item");
                }
                return false;
            }
            
            // Check if fuel is valid
            if (!isFuel(fuelItem)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Cannot start smelting - invalid fuel: " + fuelItem.getType());
                }
                return false;
            }
            
            // Consume one fuel item and start burning
            int fuelValue = FurnaceData.getFuelValue(fuelItem.getType());
            furnaceData.setFuelTime(fuelValue);
            furnaceData.setMaxFuelTime(fuelValue);
            furnaceData.setHasFuel(true);
            
            // Consume one fuel item
            if (fuelItem.getAmount() > 1) {
                fuelItem.setAmount(fuelItem.getAmount() - 1);
                furnaceData.setFuelItem(fuelItem);
            } else {
                furnaceData.setFuelItem(null);
            }
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Consumed fuel, " + fuelValue + " ticks remaining");
            }
        }
        
        // Start smelting (we have fuel and valid input)
        ItemStack resultClone = result.clone();
        furnaceData.setSmeltingResult(resultClone);
        furnaceData.setActive(true);
        // Don't reset smelt time if we're resuming after adding fuel
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Started smelting: " + inputItem.getType() + " -> " + result.getType());
        }
        
        return true;
    }
    
    /**
     * Start the furnace update task - ENHANCED DEBUG
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
     * Update furnace items safely during live updates - FIXED: Enable input/fuel syncing with safety checks
     */
    private static void updateFurnaceItemsSafely(Inventory gui, FurnaceData furnaceData, Player player) {
        try {
            // CRITICAL: Only skip syncing if player is ACTIVELY clicking (within last 100ms)
            UUID playerId = player.getUniqueId();
            if (isPlayerActivelyClicking(playerId)) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Skipping item sync - player actively clicking");
                }
                return;
            }
            
            // ALWAYS sync OUTPUT when new items are produced
            ItemStack currentOutputInGUI = gui.getItem(CustomFurnaceGUI.OUTPUT_SLOT);
            ItemStack expectedOutput = furnaceData.getOutputItem();
            
            if (shouldSyncOutput(currentOutputInGUI, expectedOutput)) {
                gui.setItem(CustomFurnaceGUI.OUTPUT_SLOT, 
                    expectedOutput != null ? expectedOutput.clone() : null);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, 
                        "[Custom Furnace] Synced output production: " + getItemDebugName(expectedOutput));
                }
            }
            
            // ENHANCED: SAFE input and fuel syncing - only when items are consumed by the furnace
            syncInputSafely(gui, furnaceData, player);
            syncFuelSafely(gui, furnaceData, player);
            
        } catch (Exception e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "[Custom Furnace] Error syncing items: " + e.getMessage());
            }
        }
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
     * Check if input should be synced due to furnace consumption - NEW METHOD
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
            expectedInput.getAmount() < currentInput.getAmount()) {
            return true;
        }
        
        // No consumption detected
        return false;
    }

    /**
     * Check if fuel should be synced due to furnace consumption - NEW METHOD
     */
    private static boolean shouldSyncFuelConsumption(ItemStack currentFuel, ItemStack expectedFuel) {
        // If both are null or air, no sync needed
        if ((currentFuel == null || currentFuel.getType() == Material.AIR) && 
            (expectedFuel == null || expectedFuel.getType() == Material.AIR)) {
            return false;
        }
        
        // If GUI has fuel but furnace data shows null - fuel was completely consumed
        if ((currentFuel != null && currentFuel.getType() != Material.AIR) && 
            (expectedFuel == null || expectedFuel.getType() == Material.AIR)) {
            return true;
        }
        
        // If both have the same type but expected amount is less - fuel was partially consumed
        if (currentFuel != null && expectedFuel != null &&
            currentFuel.getType() == expectedFuel.getType() &&
            expectedFuel.getAmount() < currentFuel.getAmount()) {
            return true;
        }
        
        // No consumption detected
        return false;
    }

    /**
     * Check if player is actively clicking RIGHT NOW - ADJUSTED timing
     */
    private static boolean isPlayerActivelyClicking(UUID playerId) {
        Map<UUID, Long> lastClickTime = getLastClickTimeMap();
        Long lastClick = lastClickTime.get(playerId);
        
        if (lastClick != null) {
            long timeSinceLastClick = System.currentTimeMillis() - lastClick;
            return timeSinceLastClick < 200L; // INCREASED from 100ms to 300ms for better safety
        }
        
        return false;
    }

    /**
     * Check if output should be synced - NEW METHOD
     */
    private static boolean shouldSyncOutput(ItemStack currentOutput, ItemStack expectedOutput) {
        // No output expected
        if (expectedOutput == null || expectedOutput.getType() == Material.AIR) {
            return false; // Don't sync empty output to avoid clearing player items
        }
        
        // No current output - new items produced
        if (currentOutput == null || currentOutput.getType() == Material.AIR) {
            return true;
        }
        
        // Same type, check if amount increased (new items produced)
        if (currentOutput.getType() == expectedOutput.getType()) {
            return expectedOutput.getAmount() > currentOutput.getAmount();
        }
        
        // Different type - sync it
        return true;
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
     * Get debug name for an item - UTILITY METHOD
     */
    private static String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        return item.getAmount() + "x " + item.getType().name();
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
     * Clean up furnace when block is broken - NEW METHOD
     */
    public void handleFurnaceBreak(Location location) {
        String key = locationToString(location);
        FurnaceData data = furnaceDataMap.remove(key);
        
        if (data != null && Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, 
                "[Custom Furnace] Cleaned up data for broken furnace at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
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
     * Add a custom smelting recipe
     */
    public void addSmeltingRecipe(Material input, ItemStack result) {
        smeltingRecipes.put(input, result.clone());
    }
    
    /**
     * Remove a custom smelting recipe
     */
    public void removeSmeltingRecipe(Material input) {
        smeltingRecipes.remove(input);
    }
    
    /**
     * Get all smelting recipes
     */
    public Map<Material, ItemStack> getSmeltingRecipes() {
        return new HashMap<>(smeltingRecipes);
    }
}