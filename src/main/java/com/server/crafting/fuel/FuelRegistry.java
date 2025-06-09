package com.server.crafting.fuel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.items.CustomItems;

/**
 * Registry for all fuel types and their properties
 * Step 1: Fuel management system
 */
public class FuelRegistry {
    
    private static FuelRegistry instance;
    private final Map<String, FuelData> fuels;
    private final Map<Material, FuelData> vanillaFuels;
    
    private FuelRegistry() {
        this.fuels = new ConcurrentHashMap<>();
        this.vanillaFuels = new ConcurrentHashMap<>();
        initializeVanillaFuels();
        initializeCustomFuels();
    }
    
    public static FuelRegistry getInstance() {
        if (instance == null) {
            instance = new FuelRegistry();
        }
        return instance;
    }
    
    /**
     * Initialize vanilla fuel types with temperature values
     * Step 1: Basic fuel system
     */
    private void initializeVanillaFuels() {
        // Low temperature fuels
        registerVanillaFuel("coal", Material.COAL, 1600, 200);
        registerVanillaFuel("charcoal", Material.CHARCOAL, 1600, 200);
        registerVanillaFuel("stick", Material.STICK, 100, 150);
        registerVanillaFuel("wooden_planks", Material.OAK_PLANKS, 300, 180);
        
        // Medium temperature fuels
        registerVanillaFuel("coal_block", Material.COAL_BLOCK, 16000, 400);
        registerVanillaFuel("dried_kelp_block", Material.DRIED_KELP_BLOCK, 4000, 250);
        registerVanillaFuel("blaze_rod", Material.BLAZE_ROD, 2400, 800);
        
        // High temperature fuels
        registerVanillaFuel("lava_bucket", Material.LAVA_BUCKET, 20000, 1200);
        registerVanillaFuel("magma_block", Material.MAGMA_BLOCK, 16000, 900);
        
        // Special fuels
        registerVanillaFuel("netherite_scrap", Material.NETHERITE_SCRAP, 8000, 1800);
        
        // Wood types
        Material[] woodTypes = {
            Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.CRIMSON_PLANKS,
            Material.WARPED_PLANKS
        };
        
        for (Material wood : woodTypes) {
            if (!vanillaFuels.containsKey(wood)) {
                registerVanillaFuel(wood.name().toLowerCase(), wood, 300, 180);
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[FuelRegistry] Initialized " + vanillaFuels.size() + " vanilla fuels");
        }
    }
    
    /**
     * Initialize custom fuel types
     * Step 1: Custom fuel foundation
     */
    private void initializeCustomFuels() {
        // Example: Custom staff as high-temperature fuel
        try {
            ItemStack emberwoodStaff = CustomItems.createEmberwoodStaff();
            registerCustomFuel("emberwood_staff", emberwoodStaff, 12000, 1500);
        } catch (Exception e) {
            // Custom items may not be available yet
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[FuelRegistry] Initialized custom fuels");
        }
    }
    
    /**
     * Register a vanilla fuel type
     */
    private void registerVanillaFuel(String id, Material material, int burnTime, int temperature) {
        FuelData fuelData = new FuelData(id, material, burnTime, temperature);
        fuels.put(id, fuelData);
        vanillaFuels.put(material, fuelData);
    }
    
    /**
     * Register a custom fuel type
     */
    public void registerCustomFuel(String id, ItemStack customItem, int burnTime, int temperature) {
        FuelData fuelData = new FuelData(id, customItem, burnTime, temperature);
        fuels.put(id, fuelData);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[FuelRegistry] Registered custom fuel: " + id + " (" + temperature + "°T)");
        }
    }
    
    /**
     * Get fuel data for an ItemStack
     */
    public FuelData getFuelData(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        // Check custom fuels first
        for (FuelData fuelData : fuels.values()) {
            if (fuelData.isCustom() && fuelData.matches(item)) {
                return fuelData;
            }
        }
        
        // Check vanilla fuels
        return vanillaFuels.get(item.getType());
    }
    
    /**
     * Check if an item is a valid fuel
     */
    public boolean isFuel(ItemStack item) {
        return getFuelData(item) != null;
    }
    
    /**
     * Get fuel temperature for an item
     */
    public int getFuelTemperature(ItemStack item) {
        FuelData fuelData = getFuelData(item);
        return fuelData != null ? fuelData.getTemperature() : 0;
    }
    
    /**
     * Get fuel burn time for an item
     */
    public int getFuelBurnTime(ItemStack item) {
        FuelData fuelData = getFuelData(item);
        return fuelData != null ? fuelData.getBurnTime() : 0;
    }
    
    /**
     * Apply fuel lore to all fuel items in inventory
     */
    public ItemStack enhanceFuelItem(ItemStack item) {
        FuelData fuelData = getFuelData(item);
        if (fuelData != null) {
            return FuelData.applyFuelLore(item, fuelData);
        }
        return item;
    }
    
    /**
     * Get all registered fuels
     */
    public Map<String, FuelData> getAllFuels() {
        return new HashMap<>(fuels);
    }
    
    /**
     * Remove a custom fuel (for admin commands)
     */
    public boolean removeCustomFuel(String id) {
        FuelData removed = fuels.remove(id);
        return removed != null && removed.isCustom();
    }
}