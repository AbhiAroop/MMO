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
     * Apply fuel lore to an ItemStack - ENHANCED: Better integration with existing lore
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
            // Find insertion point - after rarity but before other properties
            int insertIndex = lore.size();
            
            // Look for rarity line
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Rarity:")) {
                    insertIndex = i + 1;
                    break;
                }
            }
            
            // Add fuel information at the appropriate location
            lore.add(insertIndex, "");
            lore.add(insertIndex + 1, ChatColor.GOLD + "⚡ Fuel Properties:");
            lore.add(insertIndex + 2, ChatColor.GRAY + "Fuel Temperature: " + 
                    com.server.crafting.temperature.TemperatureSystem.formatTemperature(fuelData.getTemperature()));
            lore.add(insertIndex + 3, ChatColor.GRAY + "Burn Time: " + ChatColor.YELLOW + fuelData.getFormattedBurnTime());
            
            // Add fuel efficiency information based on temperature
            String efficiencyInfo = getFuelEfficiencyDescription(fuelData.getTemperature());
            if (!efficiencyInfo.isEmpty()) {
                lore.add(insertIndex + 4, ChatColor.GRAY + "Efficiency: " + efficiencyInfo);
            }
            
            meta.setLore(lore);
            fuelItem.setItemMeta(meta);
        }
        
        return fuelItem;
    }

    /**
     * Get fuel efficiency description based on temperature - NEW METHOD
     */
    private static String getFuelEfficiencyDescription(int temperature) {
        if (temperature >= 1500) {
            return ChatColor.DARK_PURPLE + "Extreme Heat " + ChatColor.GRAY + "(For specialized furnaces)";
        } else if (temperature >= 800) {
            return ChatColor.RED + "High Heat " + ChatColor.GRAY + "(Advanced smelting)";
        } else if (temperature >= 400) {
            return ChatColor.GOLD + "Medium Heat " + ChatColor.GRAY + "(General purpose)";
        } else if (temperature >= 200) {
            return ChatColor.YELLOW + "Low Heat " + ChatColor.GRAY + "(Basic smelting)";
        } else {
            return ChatColor.AQUA + "Cool " + ChatColor.GRAY + "(Specialized processing)";
        }
    }
}