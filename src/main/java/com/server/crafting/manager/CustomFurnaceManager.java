package com.server.crafting.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.crafting.fuel.FuelData;
import com.server.crafting.fuel.FuelRegistry;
import com.server.crafting.furnace.FurnaceData;
import com.server.crafting.furnace.FurnaceType;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.temperature.TemperatureSystem;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Manager for custom furnace placement, state management, and processing
 * Step 2: Furnace management system
 */
public class CustomFurnaceManager {
    
    private static CustomFurnaceManager instance;
    private final Map<String, FurnaceData> furnaceDataMap;
    private final Map<UUID, Location> playerFurnaceAccess;
    private BukkitTask furnaceUpdateTask;
    
    private CustomFurnaceManager() {
        this.furnaceDataMap = new ConcurrentHashMap<>();
        this.playerFurnaceAccess = new ConcurrentHashMap<>();
        startFurnaceUpdateTask();
    }
    
    public static CustomFurnaceManager getInstance() {
        if (instance == null) {
            instance = new CustomFurnaceManager();
        }
        return instance;
    }
    
    /**
     * Create a new custom furnace at the specified location
     * Step 2: Furnace placement system
     */
    public boolean createCustomFurnace(Location location, FurnaceType furnaceType, Player placer) {
        if (location == null || furnaceType == null) {
            return false;
        }
        
        String locationKey = locationToString(location);
        
        // Check if furnace already exists at this location
        if (furnaceDataMap.containsKey(locationKey)) {
            if (placer != null) {
                placer.sendMessage("§cA furnace already exists at this location!");
            }
            return false;
        }
        
        // Create furnace data
        FurnaceData furnaceData = new FurnaceData(location, furnaceType);
        furnaceDataMap.put(locationKey, furnaceData);
        
        // Update the physical block
        Block block = location.getBlock();
        if (block.getType() != furnaceType.getBlockMaterial()) {
            block.setType(furnaceType.getBlockMaterial());
        }
        
        // Set block data to show as lit/unlit
        updateFurnaceBlockState(location, furnaceData);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Custom Furnace] Created " + furnaceType.getDisplayName() + 
                " at " + locationKey + (placer != null ? " by " + placer.getName() : ""));
        }
        
        if (placer != null) {
            placer.sendMessage("§a✓ " + furnaceType.getColoredName() + " §acreated successfully!");
            placer.sendMessage("§7" + furnaceType.getTemperatureRange());
        }
        
        return true;
    }
    
    /**
     * Remove a custom furnace at the specified location
     * Step 2: Furnace removal system
     */
    public boolean removeCustomFurnace(Location location, Player remover) {
        if (location == null) {
            return false;
        }
        
        String locationKey = locationToString(location);
        FurnaceData furnaceData = furnaceDataMap.remove(locationKey);
        
        if (furnaceData == null) {
            if (remover != null) {
                remover.sendMessage("§cNo custom furnace found at this location!");
            }
            return false;
        }
        
        // Clear any player access
        playerFurnaceAccess.entrySet().removeIf(entry -> entry.getValue().equals(location));
        
        // Reset block to regular furnace or remove
        Block block = location.getBlock();
        if (block.getType() == furnaceData.getFurnaceType().getBlockMaterial()) {
            block.setType(Material.FURNACE); // Reset to vanilla furnace
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Custom Furnace] Removed " + furnaceData.getFurnaceType().getDisplayName() + 
                " at " + locationKey + (remover != null ? " by " + remover.getName() : ""));
        }
        
        if (remover != null) {
            remover.sendMessage("§a✓ Custom furnace removed successfully!");
        }
        
        return true;
    }
    
    /**
     * Get furnace data for a location
     * Step 2: Data access system
     */
    public FurnaceData getFurnaceData(Location location) {
        if (location == null) {
            return null;
        }
        return furnaceDataMap.get(locationToString(location));
    }
    
    /**
     * Check if a location has a custom furnace
     * Step 2: Location validation
     */
    public boolean hasCustomFurnace(Location location) {
        return getFurnaceData(location) != null;
    }
    
    /**
     * Grant a player access to a furnace
     * Step 2: Access control system
     */
    public void grantFurnaceAccess(Player player, Location furnaceLocation) {
        if (player == null || furnaceLocation == null) {
            return;
        }
        
        // Check if furnace exists
        if (!hasCustomFurnace(furnaceLocation)) {
            player.sendMessage("§cNo custom furnace found at this location!");
            return;
        }
        
        playerFurnaceAccess.put(player.getUniqueId(), furnaceLocation);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Custom Furnace] Granted access to " + player.getName() + 
                " for furnace at " + locationToString(furnaceLocation));
        }
    }
    
    /**
     * Remove a player's access to their current furnace
     * Step 2: Access cleanup
     */
    public void removeFurnaceAccess(Player player) {
        if (player == null) {
            return;
        }
        
        Location removed = playerFurnaceAccess.remove(player.getUniqueId());
        
        if (removed != null && Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Custom Furnace] Removed access for " + player.getName() + 
                " from furnace at " + locationToString(removed));
        }
    }
    
    /**
     * Check if a player has access to a specific furnace
     * Step 2: Access validation
     */
    public boolean hasAccessToFurnace(Location furnaceLocation, Player player) {
        if (player == null || furnaceLocation == null) {
            return false;
        }
        
        Location playerAccess = playerFurnaceAccess.get(player.getUniqueId());
        return playerAccess != null && playerAccess.equals(furnaceLocation);
    }
    
    /**
     * Start the main furnace update task
     * Step 2: Core processing loop
     */
    private void startFurnaceUpdateTask() {
        furnaceUpdateTask = new BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                try {
                    tickCounter++;
                    
                    // Process all furnaces every tick
                    for (Map.Entry<String, FurnaceData> entry : furnaceDataMap.entrySet()) {
                        try {
                            FurnaceData furnaceData = entry.getValue();
                            processFurnaceTick(furnaceData, tickCounter);
                            
                        } catch (Exception e) {
                            Main.getInstance().getLogger().warning(
                                "[Custom Furnace] Error processing furnace " + entry.getKey() + ": " + e.getMessage());
                        }
                    }
                    
                    // Update block states every 20 ticks (1 second)
                    if (tickCounter % 20 == 0) {
                        updateAllFurnaceBlockStates();
                    }
                    
                    // Clean up stale access every 100 ticks (5 seconds)
                    if (tickCounter % 100 == 0) {
                        cleanupStaleAccess();
                    }
                    
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("[Custom Furnace] Critical error in update task: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Started furnace update task");
        }
    }
    
    /**
     * Process a single furnace tick - ENHANCED: Complete processing system
     * Step 4: Recipe processing integration
     */
    private void processFurnaceTick(FurnaceData furnaceData, int tickCounter) {
        // Safety checks FIRST - if exploded, exit immediately
        processSafety(furnaceData);
        
        // Check if furnace was destroyed by explosion
        String locationKey = locationToString(furnaceData.getLocation());
        if (!furnaceDataMap.containsKey(locationKey)) {
            return; // Furnace was destroyed, stop processing
        }
        
        // Continue with normal processing only if furnace still exists
        processTemperature(furnaceData);
        processFuel(furnaceData);
        processRecipes(furnaceData);
        
        // Update GUI for all viewers (every 10 ticks to avoid spam)
        if (tickCounter % 10 == 0) {
            CustomFurnaceGUI.updateFurnaceGUI(furnaceData);
        }
    }

    /**
     * Process recipe cooking and completion - ENHANCED: Output space checking
     * Step 4: Recipe processing system
     */
    private void processRecipes(FurnaceData furnaceData) {
        // Check if furnace is in emergency shutdown
        if (furnaceData.isEmergencyShutdown()) {
            furnaceData.setActive(false);
            furnaceData.setPaused(true);
            return;
        }
        
        // Get current inputs from furnace
        List<org.bukkit.inventory.ItemStack> currentInputs = getCurrentInputItems(furnaceData);
        
        if (currentInputs.isEmpty()) {
            // No inputs - stop cooking
            if (furnaceData.isActive()) {
                furnaceData.setActive(false);
                furnaceData.setCookTime(0);
                furnaceData.setMaxCookTime(0);
            }
            return;
        }
        
        // Find matching recipe
        com.server.crafting.recipes.FurnaceRecipe currentRecipe = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().findRecipe(currentInputs);
        
        if (currentRecipe == null) {
            // No valid recipe - stop cooking
            if (furnaceData.isActive()) {
                furnaceData.setActive(false);
                furnaceData.setCookTime(0);
                furnaceData.setMaxCookTime(0);
            }
            return;
        }
        
        // CRITICAL: Check if output slots have space for the recipe outputs
        if (!hasOutputSpaceForRecipe(furnaceData, currentRecipe)) {
            // Output slots are full - pause cooking but don't reset progress
            furnaceData.setPaused(true);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Furnace] Output slots full - pausing cooking at " + 
                    locationToString(furnaceData.getLocation()));
            }
            return;
        }
        
        // Check temperature requirements
        int currentTemp = furnaceData.getCurrentTemperature();
        int requiredTemp = currentRecipe.getRequiredTemperature();
        
        if (currentTemp < requiredTemp) {
            // Temperature too low - pause cooking
            furnaceData.setPaused(true);
            return;
        }
        
        // Temperature is sufficient and we have output space - continue/start cooking
        furnaceData.setPaused(false);
        
        if (!furnaceData.isActive()) {
            // Start new cooking process
            furnaceData.setActive(true);
            furnaceData.setCookTime(0);
            furnaceData.setMaxCookTime(currentRecipe.getCookTime());
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Furnace] Started cooking " + currentRecipe.getDisplayName() + 
                    " at " + locationToString(furnaceData.getLocation()));
            }
        }
        
        // Calculate cooking efficiency based on temperature
        double efficiency = com.server.crafting.temperature.TemperatureSystem
            .getTemperatureEfficiency(currentTemp, requiredTemp);
        
        // Progress cooking (efficiency affects speed)
        int cookProgress = (int) (efficiency * 1.0); // Base 1 tick progress, modified by efficiency
        furnaceData.setCookTime(furnaceData.getCookTime() + cookProgress);
        
        // Check if cooking is complete
        if (furnaceData.getCookTime() >= furnaceData.getMaxCookTime()) {
            // Double-check output space before completing
            if (hasOutputSpaceForRecipe(furnaceData, currentRecipe)) {
                completeCooking(furnaceData, currentRecipe);
            } else {
                // Output became full during cooking - pause at 99% completion
                furnaceData.setCookTime(furnaceData.getMaxCookTime() - 1);
                furnaceData.setPaused(true);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Furnace] Output full just before completion - pausing at " + 
                        locationToString(furnaceData.getLocation()));
                }
            }
        }
    }

    /**
     * Get current input items from furnace
     * Step 4: Input processing
     */
    private List<org.bukkit.inventory.ItemStack> getCurrentInputItems(FurnaceData furnaceData) {
        List<org.bukkit.inventory.ItemStack> inputs = new ArrayList<>();
        
        for (int i = 0; i < furnaceData.getFurnaceType().getInputSlots(); i++) {
            org.bukkit.inventory.ItemStack item = furnaceData.getInputSlot(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                inputs.add(item);
            }
        }
        
        return inputs;
    }

    /**
     * Complete the cooking process and produce outputs
     * Step 4: Recipe completion
     */
    private void completeCooking(FurnaceData furnaceData, com.server.crafting.recipes.FurnaceRecipe recipe) {
        // Consume input ingredients
        consumeRecipeIngredients(furnaceData, recipe);
        
        // Produce outputs
        produceRecipeOutputs(furnaceData, recipe);
        
        // Reset cooking state
        furnaceData.setActive(false);
        furnaceData.setCookTime(0);
        furnaceData.setMaxCookTime(0);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Furnace] Completed cooking " + recipe.getDisplayName() + 
                " at " + locationToString(furnaceData.getLocation()));
        }
        
        // Check if we can start cooking another batch immediately
        processRecipes(furnaceData);
    }

    /**
     * Consume ingredients according to recipe requirements
     * Step 4: Ingredient consumption
     */
    private void consumeRecipeIngredients(FurnaceData furnaceData, com.server.crafting.recipes.FurnaceRecipe recipe) {
        List<org.bukkit.inventory.ItemStack> requiredInputs = recipe.getInputs();
        
        // Create a working copy of required ingredients
        Map<String, Integer> ingredientsToConsume = new HashMap<>();
        
        for (org.bukkit.inventory.ItemStack required : requiredInputs) {
            if (required != null) {
                String key = createItemKey(required);
                ingredientsToConsume.put(key, 
                    ingredientsToConsume.getOrDefault(key, 0) + required.getAmount());
            }
        }
        
        // Consume from input slots
        for (int i = 0; i < furnaceData.getFurnaceType().getInputSlots(); i++) {
            org.bukkit.inventory.ItemStack inputItem = furnaceData.getInputSlot(i);
            
            if (inputItem != null && inputItem.getType() != org.bukkit.Material.AIR) {
                String itemKey = createItemKey(inputItem);
                
                if (ingredientsToConsume.containsKey(itemKey)) {
                    int needed = ingredientsToConsume.get(itemKey);
                    int available = inputItem.getAmount();
                    int toConsume = Math.min(needed, available);
                    
                    // Consume the items
                    inputItem.setAmount(available - toConsume);
                    if (inputItem.getAmount() <= 0) {
                        furnaceData.setInputSlot(i, null);
                    }
                    
                    // Update needed amount
                    ingredientsToConsume.put(itemKey, needed - toConsume);
                    if (ingredientsToConsume.get(itemKey) <= 0) {
                        ingredientsToConsume.remove(itemKey);
                    }
                    
                    if (ingredientsToConsume.isEmpty()) {
                        break; // All ingredients consumed
                    }
                }
            }
        }
    }

    /**
     * Produce recipe outputs in furnace output slots
     * Step 4: Output production
     */
    private void produceRecipeOutputs(FurnaceData furnaceData, com.server.crafting.recipes.FurnaceRecipe recipe) {
        List<org.bukkit.inventory.ItemStack> outputs = recipe.getOutputs();
        
        for (org.bukkit.inventory.ItemStack output : outputs) {
            if (output != null) {
                addToOutputSlots(furnaceData, output.clone());
            }
        }
    }

    /**
     * Add an item to the furnace output slots - ENHANCED: Better overflow handling
     * Step 4: Output slot management
     */
    private void addToOutputSlots(FurnaceData furnaceData, org.bukkit.inventory.ItemStack item) {
        int outputSlots = furnaceData.getFurnaceType().getOutputSlots();
        
        // Try to add to existing stacks first
        for (int i = 0; i < outputSlots; i++) {
            org.bukkit.inventory.ItemStack outputItem = furnaceData.getOutputSlot(i);
            
            if (outputItem != null && outputItem.isSimilar(item)) {
                int maxStack = outputItem.getMaxStackSize();
                int currentAmount = outputItem.getAmount();
                int spaceAvailable = maxStack - currentAmount;
                
                if (spaceAvailable > 0) {
                    int toAdd = Math.min(spaceAvailable, item.getAmount());
                    outputItem.setAmount(currentAmount + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    
                    if (item.getAmount() <= 0) {
                        return; // All items placed
                    }
                }
            }
        }
        
        // Try to place in empty slots
        for (int i = 0; i < outputSlots; i++) {
            org.bukkit.inventory.ItemStack outputItem = furnaceData.getOutputSlot(i);
            
            if (outputItem == null || outputItem.getType() == org.bukkit.Material.AIR) {
                furnaceData.setOutputSlot(i, item.clone());
                return; // Item placed successfully
            }
        }
        
        // If we reach here, output slots are full - this should be prevented by pre-checks
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Furnace] CRITICAL: Output overflow at " + locationToString(furnaceData.getLocation()) + 
                " - could not place " + item.getType().name() + " (this should not happen!)");
        }
        
        // Emergency: Drop the item at the furnace location
        furnaceData.getLocation().getWorld().dropItemNaturally(furnaceData.getLocation(), item);
    }

    /**
     * Create a unique key for an item (for ingredient matching)
     * Step 4: Item identification
     */
    private String createItemKey(org.bukkit.inventory.ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        StringBuilder key = new StringBuilder();
        key.append(item.getType().name());
        
        // Include custom model data for custom items
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            key.append(":cmd:").append(item.getItemMeta().getCustomModelData());
        }
        
        // Include display name for named items
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            key.append(":name:").append(item.getItemMeta().getDisplayName());
        }
        
        return key.toString();
    }
    
    /**
     * Process temperature changes
     * Step 2: Temperature system implementation
     */
    private void processTemperature(FurnaceData furnaceData) {
        int currentTemp = furnaceData.getCurrentTemperature();
        int targetTemp = furnaceData.getTargetTemperature();
        
        // Calculate temperature change
        int tempChange = 0;
        
        if (currentTemp < targetTemp) {
            // Heating up
            furnaceData.setHeating(true);
            furnaceData.setCooling(false);
            
            // Heating rate based on fuel and furnace type
            if (furnaceData.hasFuel()) {
                tempChange = calculateHeatingRate(furnaceData);
            }
            
        } else if (currentTemp > targetTemp) {
            // Cooling down
            furnaceData.setHeating(false);
            furnaceData.setCooling(true);
            
            // Natural cooling rate
            tempChange = -TemperatureSystem.getTemperatureDecay(currentTemp);
            
        } else {
            // At target temperature
            furnaceData.setHeating(false);
            furnaceData.setCooling(false);
        }
        
        // Apply temperature change
        if (tempChange != 0) {
            int newTemp = Math.max(TemperatureSystem.ROOM_TEMPERATURE, currentTemp + tempChange);
            furnaceData.setCurrentTemperature(newTemp);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI) && tempChange != 0) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Temperature] Furnace at " + locationToString(furnaceData.getLocation()) + 
                    " temperature: " + currentTemp + " -> " + newTemp + 
                    " (target: " + targetTemp + ", change: " + tempChange + ")");
            }
        }
    }
    
   /**
     * Complete the calculate heating rate method - FIXED
     */
    private int calculateHeatingRate(FurnaceData furnaceData) {
        // Base heating rate: 2-5 degrees per tick depending on furnace type
        int baseRate = 2;
        
        FurnaceType type = furnaceData.getFurnaceType();
        switch (type) {
            case STONE_FURNACE:
                baseRate = 2;
                break;
            case CLAY_KILN:
                baseRate = 3;
                break;
            case IRON_FORGE:
                baseRate = 4;
                break;
            case STEEL_FURNACE:
                baseRate = 5;
                break;
            case MAGMATIC_FORGE:
                baseRate = 6;
                break;
            case ARCANE_CRUCIBLE:
                baseRate = 7;
                break;
            case VOID_EXTRACTOR:
                baseRate = 1; // Slower heating for specialized furnace
                break;
            default:
                baseRate = 2;
                break;
        }
        
        // Apply fuel efficiency multiplier
        if (furnaceData.hasFuel()) {
            double fuelEfficiency = Math.min(2.0, furnaceData.getFuelProgress() + 0.5);
            baseRate = (int) (baseRate * fuelEfficiency);
        }
        
        return baseRate;
    }

    /**
     * Calculate cooling rate when no fuel is burning
     */
    private int calculateCoolingRate(FurnaceData furnaceData) {
        int currentTemp = furnaceData.getCurrentTemperature();
        
        // Use the temperature system's decay calculation
        return TemperatureSystem.getTemperatureDecay(currentTemp);
    }
    
    /**
     * Process fuel consumption and heating - ENHANCED: Smart fuel consumption
     * Step 2: Fuel system - ENHANCED with better debugging
     */
    private void processFuel(FurnaceData furnaceData) {
        if (furnaceData.getFuelTime() > 0) {
            // Consume fuel
            furnaceData.setFuelTime(furnaceData.getFuelTime() - 1);
            furnaceData.setHasFuel(true);
            
            if (furnaceData.getFuelTime() <= 0) {
                // Fuel depleted, check if we should consume more
                if (shouldConsumeFuel(furnaceData)) {
                    consumeNextFuelItem(furnaceData);
                } else {
                    // No need for more fuel
                    furnaceData.setHasFuel(false);
                    furnaceData.setTargetTemperature(TemperatureSystem.ROOM_TEMPERATURE);
                }
            }
        } else {
            // No fuel burning, check if we should start burning fuel
            if (shouldConsumeFuel(furnaceData)) {
                consumeNextFuelItem(furnaceData);
            } else {
                furnaceData.setHasFuel(false);
                furnaceData.setTargetTemperature(TemperatureSystem.ROOM_TEMPERATURE);
            }
        }
    }

    /**
     * Determine if fuel should be consumed - SMART FUEL LOGIC
     */
    private boolean shouldConsumeFuel(FurnaceData furnaceData) {
        // Check if there are items to process
        List<org.bukkit.inventory.ItemStack> currentInputs = getCurrentInputItems(furnaceData);
        if (currentInputs.isEmpty()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Furnace] No items to process - not consuming fuel at " + 
                    locationToString(furnaceData.getLocation()));
            }
            return false;
        }
        
        // Check if there's a valid recipe
        com.server.crafting.recipes.FurnaceRecipe recipe = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().findRecipe(currentInputs);
        
        if (recipe == null) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Furnace] No valid recipe found - not consuming fuel at " + 
                    locationToString(furnaceData.getLocation()));
            }
            return false;
        }
        
        // Check if output slots have space for the recipe outputs
        if (!hasOutputSpaceForRecipe(furnaceData, recipe)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI,
                    "[Furnace] Output slots full - not consuming fuel at " + 
                    locationToString(furnaceData.getLocation()));
            }
            return false;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Furnace] Valid recipe and space available - can consume fuel at " + 
                locationToString(furnaceData.getLocation()));
        }
        
        return true;
    }

    /**
     * Check if output slots have space for a recipe's outputs
     */
    private boolean hasOutputSpaceForRecipe(FurnaceData furnaceData, com.server.crafting.recipes.FurnaceRecipe recipe) {
        for (org.bukkit.inventory.ItemStack output : recipe.getOutputs()) {
            if (output != null && !furnaceData.hasOutputSpaceFor(output)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consume the next available fuel item from fuel slots
     * Step 2: Fuel consumption system - FIXED
     */
    private void consumeNextFuelItem(FurnaceData furnaceData) {
        for (int i = 0; i < furnaceData.getFurnaceType().getFuelSlots(); i++) {
            ItemStack fuelItem = furnaceData.getFuelSlot(i);
            
            if (fuelItem != null && fuelItem.getType() != Material.AIR) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Furnace] Checking fuel slot " + i + ": " + fuelItem.getType().name() + 
                        " x" + fuelItem.getAmount());
                }
                
                FuelData fuelData = FuelRegistry.getInstance().getFuelData(fuelItem);
                
                if (fuelData != null) {
                    // Consume one fuel item
                    fuelItem.setAmount(fuelItem.getAmount() - 1);
                    if (fuelItem.getAmount() <= 0) {
                        furnaceData.setFuelSlot(i, null);
                    }
                    
                    // Set fuel properties
                    furnaceData.setFuelTime(fuelData.getBurnTime());
                    furnaceData.setMaxFuelTime(fuelData.getBurnTime());
                    furnaceData.setHasFuel(true);
                    
                    // Set target temperature based on fuel
                    furnaceData.setTargetTemperature(fuelData.getTemperature());
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Furnace] Consumed fuel: " + fuelData.getFuelId() + 
                            " (" + fuelData.getTemperature() + "°T, " + 
                            fuelData.getFormattedBurnTime() + ") at " + 
                            locationToString(furnaceData.getLocation()));
                    }
                    
                    return; // Successfully consumed fuel
                }
            }
        }
        
        // No fuel found
        furnaceData.setHasFuel(false);
        furnaceData.setTargetTemperature(TemperatureSystem.ROOM_TEMPERATURE);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Furnace] No fuel available at " + locationToString(furnaceData.getLocation()));
        }
    }
    
    /**
     * Try to automatically consume new fuel when current fuel runs out
     * Step 2: Automatic fuel consumption
     */
    private void tryConsumeNewFuel(FurnaceData furnaceData) {
        // Look for fuel in fuel slots
        for (int i = 0; i < furnaceData.getFurnaceType().getFuelSlots(); i++) {
            ItemStack fuelSlot = furnaceData.getFuelSlot(i);
            if (fuelSlot != null && fuelSlot.getAmount() > 0) {
                
                FuelRegistry fuelRegistry = FuelRegistry.getInstance();
                if (fuelRegistry.isFuel(fuelSlot)) {
                    
                    // Get fuel properties
                    int burnTime = fuelRegistry.getFuelBurnTime(fuelSlot);
                    int temperature = fuelRegistry.getFuelTemperature(fuelSlot);
                    
                    // Consume one fuel item
                    if (fuelSlot.getAmount() > 1) {
                        fuelSlot.setAmount(fuelSlot.getAmount() - 1);
                    } else {
                        furnaceData.setFuelSlot(i, null);
                    }
                    
                    // Set new fuel properties
                    furnaceData.setFuelTime(burnTime);
                    furnaceData.setMaxFuelTime(burnTime);
                    furnaceData.setHasFuel(true);
                    furnaceData.setTargetTemperature(temperature);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Fuel] Auto-consumed fuel at " + locationToString(furnaceData.getLocation()) + 
                            " - " + burnTime + " ticks, " + temperature + "°T");
                    }
                    
                    return; // Successfully consumed fuel
                }
            }
        }
    }
    
    /**
     * Process safety checks and handle overheating/explosions
     * Step 2: Safety system - ENHANCED: Close GUIs on explosion
     */
    private void processSafety(FurnaceData furnaceData) {
        int currentTemp = furnaceData.getCurrentTemperature();
        int maxSafe = furnaceData.getFurnaceType().getMaxTemperature();
        int explosionTemp = furnaceData.getFurnaceType().getExplosionTemperature();
        
        if (currentTemp > maxSafe) {
            // Furnace is overheating
            furnaceData.setOverheatingTime(furnaceData.getOverheatingTime() + 1);
            
            if (currentTemp >= explosionTemp) {
                // Start explosion countdown
                if (furnaceData.getExplosionCountdown() <= 0) {
                    furnaceData.setExplosionCountdown(100); // 5 seconds at 20 TPS
                } else {
                    furnaceData.setExplosionCountdown(furnaceData.getExplosionCountdown() - 1);
                    
                    if (furnaceData.getExplosionCountdown() <= 0) {
                        // EXPLOSION! - Close all GUIs first, then explode
                        handleFurnaceExplosion(furnaceData);
                        return; // Exit early since furnace is destroyed
                    }
                }
            }
        } else {
            // Reset overheating if temperature is back to safe levels
            if (furnaceData.getOverheatingTime() > 0) {
                furnaceData.setOverheatingTime(Math.max(0, furnaceData.getOverheatingTime() - 2));
            }
            furnaceData.setExplosionCountdown(0);
        }
        
        // Emergency shutdown for extremely high temperatures
        if (currentTemp > explosionTemp + 200) {
            if (!furnaceData.isEmergencyShutdown()) {
                furnaceData.setEmergencyShutdown(true);
                furnaceData.setActive(false);
                furnaceData.setTargetTemperature(TemperatureSystem.ROOM_TEMPERATURE);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Safety] Emergency shutdown activated at " + locationToString(furnaceData.getLocation()));
                }
            }
        }
    }

    /**
     * Handle furnace explosion - ENHANCED: Close GUIs and cleanup
     */
    private void handleFurnaceExplosion(FurnaceData furnaceData) {
        Location location = furnaceData.getLocation();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Safety] Furnace explosion at " + locationToString(location));
        }
        
        // CRITICAL: Close all GUIs viewing this furnace BEFORE explosion
        closeAllGUIsForFurnace(furnaceData);
        
        // Drop furnace contents
        dropFurnaceContents(location, furnaceData);
        
        // Create explosion effect
        location.getWorld().createExplosion(location, 4.0f, false, true);
        
        // Remove furnace data
        String locationKey = locationToString(location);
        furnaceDataMap.remove(locationKey);
        
        // Replace block with air
        location.getBlock().setType(org.bukkit.Material.AIR);
        
        // Notify nearby players
        for (org.bukkit.entity.Player nearbyPlayer : location.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(location) <= 50) {
                nearbyPlayer.sendMessage(org.bukkit.ChatColor.RED + "⚠ A furnace has exploded due to overheating!");
            }
        }
    }

    /**
     * Close all GUIs currently viewing this furnace
     * Step 2: GUI cleanup on explosion
     */
    private void closeAllGUIsForFurnace(FurnaceData furnaceData) {
        // Get all players currently viewing this furnace's GUI
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            // Check if player has this furnace's GUI open
            if (com.server.crafting.gui.CustomFurnaceGUI.isPlayerViewingFurnace(player, furnaceData)) {
                // Close their inventory (which will trigger the close event handler)
                player.closeInventory();
                
                // Send explosion message
                player.sendMessage(org.bukkit.ChatColor.DARK_RED + "💥 The furnace exploded due to overheating!");
                player.sendMessage(org.bukkit.ChatColor.RED + "Temperature safety limits were exceeded!");
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Safety] Closed GUI for " + player.getName() + " due to furnace explosion");
                }
            }
        }
    }

    /**
     * Drop all furnace contents when exploded or destroyed
     */
    private void dropFurnaceContents(Location location, FurnaceData furnaceData) {
        // Drop input items
        for (int i = 0; i < furnaceData.getFurnaceType().getInputSlots(); i++) {
            ItemStack item = furnaceData.getInputSlot(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
        
        // Drop fuel items
        for (int i = 0; i < furnaceData.getFurnaceType().getFuelSlots(); i++) {
            ItemStack item = furnaceData.getFuelSlot(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
        
        // Drop output items
        for (int i = 0; i < furnaceData.getFurnaceType().getOutputSlots(); i++) {
            ItemStack item = furnaceData.getOutputSlot(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }
    
    /**
     * Handle furnace explosion
     * Step 2: Explosion system
     */
    private void explodeFurnace(FurnaceData furnaceData) {
        Location location = furnaceData.getLocation();
        
        // Create explosion effect
        location.getWorld().createExplosion(location, 4.0f, false, true);
        
        // Remove the furnace
        removeCustomFurnace(location, null);
        
        // Drop some materials as compensation
        location.getWorld().dropItemNaturally(location, new ItemStack(Material.IRON_INGOT, 2));
        location.getWorld().dropItemNaturally(location, new ItemStack(Material.COBBLESTONE, 8));
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Explosion] Furnace exploded at " + locationToString(location) + 
                " due to critical overheating (" + furnaceData.getCurrentTemperature() + "°T)");
        }
    }
    
    /**
     * Update the physical block state to show furnace activity
     * Step 2: Visual feedback system
     */
    private void updateFurnaceBlockState(Location location, FurnaceData furnaceData) {
        Block block = location.getBlock();
        
        if (block.getBlockData() instanceof Furnace) {
            Furnace furnaceBlockData = (Furnace) block.getBlockData();
            
            // Set lit state based on fuel and activity
            boolean shouldBeLit = furnaceData.hasFuel() && 
                                 furnaceData.getCurrentTemperature() > TemperatureSystem.ROOM_TEMPERATURE + 50;
            
            if (furnaceBlockData.isLit() != shouldBeLit) {
                furnaceBlockData.setLit(shouldBeLit);
                block.setBlockData(furnaceBlockData);
            }
        }
    }
    
    /**
     * Update all furnace block states
     * Step 2: Batch visual updates
     */
    private void updateAllFurnaceBlockStates() {
        for (FurnaceData furnaceData : furnaceDataMap.values()) {
            try {
                updateFurnaceBlockState(furnaceData.getLocation(), furnaceData);
            } catch (Exception e) {
                // Skip individual furnaces that error
            }
        }
    }
    
    /**
     * Clean up stale furnace access entries
     * Step 2: Memory management
     */
    private void cleanupStaleAccess() {
        playerFurnaceAccess.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            Location furnaceLocation = entry.getValue();
            
            // Remove if player is offline
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return true;
            }
            
            // Remove if furnace no longer exists
            if (!hasCustomFurnace(furnaceLocation)) {
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * Convert location to string key for mapping
     * Step 2: Location serialization
     */
    private String locationToString(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName() + ":" + 
               location.getBlockX() + ":" + 
               location.getBlockY() + ":" + 
               location.getBlockZ();
    }
    
    /**
     * Get all active furnaces (for debugging)
     * Step 2: Debug utilities
     */
    public Map<String, FurnaceData> getAllFurnaces() {
        return furnaceDataMap;
    }
    
    /**
     * Shutdown the furnace manager
     * Step 2: Cleanup on disable
     */
    public void shutdown() {
        if (furnaceUpdateTask != null) {
            furnaceUpdateTask.cancel();
            furnaceUpdateTask = null;
        }
        
        furnaceDataMap.clear();
        playerFurnaceAccess.clear();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Custom Furnace] Manager shutdown complete");
        }
    }
}