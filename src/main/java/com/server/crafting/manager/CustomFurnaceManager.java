package com.server.crafting.manager;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.crafting.data.FurnaceData;
import com.server.crafting.gui.CustomFurnaceGUI;
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
     * Check if an item can be smelted
     */
    public boolean canSmelt(ItemStack item) {
        return item != null && smeltingRecipes.containsKey(item.getType());
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
     * Check if an item is fuel
     */
    public boolean isFuel(ItemStack item) {
        return item != null && FurnaceData.isFuel(item.getType());
    }
    
    /**
     * Try to start smelting process
     */
    public boolean tryStartSmelting(FurnaceData furnaceData) {
        ItemStack inputItem = furnaceData.getInputItem();
        ItemStack fuelItem = furnaceData.getFuelItem();
        
        // Check if we can smelt the input item
        if (!canSmelt(inputItem)) {
            return false;
        }
        
        // Check if we have fuel or need to consume fuel
        if (!furnaceData.hasFuel()) {
            if (!isFuel(fuelItem)) {
                return false;
            }
            
            // Consume one fuel item
            int fuelValue = FurnaceData.getFuelValue(fuelItem.getType());
            furnaceData.setFuelTime(fuelValue);
            furnaceData.setMaxFuelTime(fuelValue);
            furnaceData.setHasFuel(true);
            
            // Remove one fuel item
            fuelItem.setAmount(fuelItem.getAmount() - 1);
            if (fuelItem.getAmount() <= 0) {
                furnaceData.setFuelItem(null);
            } else {
                furnaceData.setFuelItem(fuelItem);
            }
        }
        
        // Start smelting
        ItemStack result = getSmeltingResult(inputItem);
        furnaceData.setSmeltingResult(result);
        furnaceData.setActive(true);
        furnaceData.setSmeltTime(0);
        
        return true;
    }
    
    /**
     * Start the furnace update task
     */
    private void startFurnaceUpdateTask() {
        BukkitRunnable runnable = new BukkitRunnable() {
            int tickCounter = 0;
            @Override
            public void run() {
                try {
                    tickCounter++;
                    
                    // Update all furnace data every tick (for accurate processing)
                    for (Map.Entry<String, FurnaceData> entry : furnaceDataMap.entrySet()) {
                        FurnaceData furnaceData = entry.getValue();
                        Location location = furnaceData.getLocation();
                        
                        // Try to start smelting if not active
                        if (!furnaceData.isActive() && furnaceData.getInputItem() != null) {
                            tryStartSmelting(furnaceData);
                        }
                        
                        // Tick the furnace
                        furnaceData.tick();
                        
                        // CRITICAL: Update block animation every 5 ticks to reduce lag
                        if (tickCounter % 5 == 0) {
                            updateFurnaceBlockState(location, furnaceData);
                        }
                    }
                
                    CustomFurnaceGUI.updateAllFurnaceGUIs();
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("Error in furnace update task: " + e.getMessage());
                }
            }
        };
        
        // Run every tick (20 times per second)
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