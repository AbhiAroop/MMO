package com.server.crafting.fuel;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents fuel data with temperature output
 * Step 1: Fuel system foundation
 */
public class FuelData {
    
    private final String fuelId;
    private final Material material;
    private final int burnTime;        // In ticks (20 ticks = 1 second)
    private final int temperature;     // Temperature this fuel produces
    private final boolean isCustom;    // Whether this is a custom fuel
    private final ItemStack customItem; // For custom fuels with NBT data
    
    public FuelData(String fuelId, Material material, int burnTime, int temperature) {
        this.fuelId = fuelId;
        this.material = material;
        this.burnTime = burnTime;
        this.temperature = temperature;
        this.isCustom = false;
        this.customItem = null;
    }
    
    public FuelData(String fuelId, ItemStack customItem, int burnTime, int temperature) {
        this.fuelId = fuelId;
        this.material = customItem.getType();
        this.burnTime = burnTime;
        this.temperature = temperature;
        this.isCustom = true;
        this.customItem = customItem.clone();
    }
    
    // Getters
    public String getFuelId() { return fuelId; }
    public Material getMaterial() { return material; }
    public int getBurnTime() { return burnTime; }
    public int getTemperature() { return temperature; }
    public boolean isCustom() { return isCustom; }
    public ItemStack getCustomItem() { return customItem != null ? customItem.clone() : null; }
    
    /**
     * Get burn time in seconds for display
     */
    public int getBurnTimeSeconds() {
        return burnTime / 20;
    }
    
    /**
     * Get formatted burn time string
     */
    public String getFormattedBurnTime() {
        int seconds = getBurnTimeSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
    }
    
    /**
     * Check if an ItemStack matches this fuel
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        if (isCustom) {
            // For custom fuels, compare NBT data
            return customItem != null && item.isSimilar(customItem);
        } else {
            // For vanilla fuels, just compare material
            return true;
        }
    }
    
    /**
     * Apply fuel lore to an ItemStack
     */
    public static ItemStack applyFuelLore(ItemStack item, FuelData fuelData) {
        if (item == null || fuelData == null) {
            return item;
        }
        
        ItemStack fuelItem = item.clone();
        ItemMeta meta = fuelItem.getItemMeta();
        if (meta == null) {
            return fuelItem;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Check if fuel lore already exists
        boolean hasFuelLore = lore.stream().anyMatch(line -> 
            line.contains("Fuel Temperature:") || line.contains("Burn Time:"));
        
        if (!hasFuelLore) {
            // Add fuel information
            lore.add("");
            lore.add(ChatColor.GOLD + "⚡ Fuel Properties:");
            lore.add(ChatColor.GRAY + "Fuel Temperature: " + 
                    com.server.crafting.temperature.TemperatureSystem.formatTemperature(fuelData.getTemperature()));
            lore.add(ChatColor.GRAY + "Burn Time: " + ChatColor.YELLOW + fuelData.getFormattedBurnTime());
            
            meta.setLore(lore);
            fuelItem.setItemMeta(meta);
        }
        
        return fuelItem;
    }
}