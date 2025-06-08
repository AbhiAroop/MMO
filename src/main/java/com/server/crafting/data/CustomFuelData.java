package com.server.crafting.data;

import org.bukkit.inventory.ItemStack;

/**
 * Represents custom fuel data with exact item matching and custom burn times
 */
public class CustomFuelData {
    
    private final ItemStack fuelItem;
    private final int burnTime; // In ticks (20 ticks = 1 second)
    private final String fuelId;
    
    /**
     * Create a new custom fuel data entry
     * @param fuelId Unique identifier for this fuel type
     * @param fuelItem The exact item that can be used as fuel
     * @param burnTime How long this fuel burns in ticks
     */
    public CustomFuelData(String fuelId, ItemStack fuelItem, int burnTime) {
        this.fuelId = fuelId;
        this.fuelItem = fuelItem.clone();
        this.burnTime = burnTime;
    }
    
    /**
     * Get the unique identifier for this fuel
     * @return The fuel ID
     */
    public String getFuelId() {
        return fuelId;
    }
    
    /**
     * Get the fuel item (cloned for safety)
     * @return A clone of the fuel item
     */
    public ItemStack getFuelItem() {
        return fuelItem.clone();
    }
    
    /**
     * Get the burn time in ticks
     * @return Burn time in ticks (20 ticks = 1 second)
     */
    public int getBurnTime() {
        return burnTime;
    }
    
    /**
     * Get the burn time in seconds
     * @return Burn time in seconds
     */
    public double getBurnTimeInSeconds() {
        return burnTime / 20.0;
    }
    
    /**
     * Check if the given item matches this fuel exactly
     * Compares type, custom model data, display name, and lore
     * @param item The item to check
     * @return true if the item matches this fuel exactly
     */
    public boolean matchesFuel(ItemStack item) {
        if (item == null || fuelItem == null) {
            return false;
        }
        
        // Check material type
        if (item.getType() != fuelItem.getType()) {
            return false;
        }
        
        // Both items must have meta or both must not have meta
        boolean itemHasMeta = item.hasItemMeta();
        boolean fuelHasMeta = fuelItem.hasItemMeta();
        
        if (itemHasMeta != fuelHasMeta) {
            return false;
        }
        
        // If neither has meta, they match
        if (!itemHasMeta) {
            return true;
        }
        
        // Compare custom model data
        Integer itemModelData = item.getItemMeta().hasCustomModelData() ? 
            item.getItemMeta().getCustomModelData() : null;
        Integer fuelModelData = fuelItem.getItemMeta().hasCustomModelData() ? 
            fuelItem.getItemMeta().getCustomModelData() : null;
        
        if (!java.util.Objects.equals(itemModelData, fuelModelData)) {
            return false;
        }
        
        // Compare display names
        String itemDisplayName = item.getItemMeta().hasDisplayName() ? 
            item.getItemMeta().getDisplayName() : null;
        String fuelDisplayName = fuelItem.getItemMeta().hasDisplayName() ? 
            fuelItem.getItemMeta().getDisplayName() : null;
        
        if (!java.util.Objects.equals(itemDisplayName, fuelDisplayName)) {
            return false;
        }
        
        // Compare lore
        java.util.List<String> itemLore = item.getItemMeta().hasLore() ? 
            item.getItemMeta().getLore() : null;
        java.util.List<String> fuelLore = fuelItem.getItemMeta().hasLore() ? 
            fuelItem.getItemMeta().getLore() : null;
        
        return java.util.Objects.equals(itemLore, fuelLore);
    }
    
    /**
     * Check if this fuel is a custom item (has custom model data)
     * @return true if this fuel has custom model data
     */
    public boolean isCustomItem() {
        return fuelItem.hasItemMeta() && fuelItem.getItemMeta().hasCustomModelData();
    }
    
    /**
     * Get the custom model data if present
     * @return The custom model data, or null if not present
     */
    public Integer getCustomModelData() {
        if (fuelItem.hasItemMeta() && fuelItem.getItemMeta().hasCustomModelData()) {
            return fuelItem.getItemMeta().getCustomModelData();
        }
        return null;
    }
    
    /**
     * Get the display name of the fuel item
     * @return The display name if present, or the formatted material name
     */
    public String getDisplayName() {
        if (fuelItem.hasItemMeta() && fuelItem.getItemMeta().hasDisplayName()) {
            return fuelItem.getItemMeta().getDisplayName();
        }
        // Convert MATERIAL_NAME to Material Name
        String materialName = fuelItem.getType().name().toLowerCase().replace("_", " ");
        return materialName.substring(0, 1).toUpperCase() + materialName.substring(1);
    }
    
    /**
     * Check if this fuel has a display name
     * @return true if the fuel item has a custom display name
     */
    public boolean hasDisplayName() {
        return fuelItem.hasItemMeta() && fuelItem.getItemMeta().hasDisplayName();
    }
    
    /**
     * Get formatted burn time as a readable string
     * @return Formatted burn time (e.g., "5m 30s" or "45s")
     */
    public String getFormattedBurnTime() {
        int totalSeconds = burnTime / 20;
        
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else if (totalSeconds < 3600) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            if (seconds == 0) {
                return minutes + "m";
            } else {
                return minutes + "m " + seconds + "s";
            }
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;
            
            StringBuilder sb = new StringBuilder();
            sb.append(hours).append("h");
            if (minutes > 0) {
                sb.append(" ").append(minutes).append("m");
            }
            if (seconds > 0) {
                sb.append(" ").append(seconds).append("s");
            }
            return sb.toString();
        }
    }
    
    /**
     * Compare this fuel to another for efficiency
     * @param other The other fuel to compare to
     * @return negative if this fuel is less efficient, positive if more efficient, 0 if equal
     */
    public int compareEfficiency(CustomFuelData other) {
        return Integer.compare(this.burnTime, other.burnTime);
    }
    
    /**
     * Check if this fuel is more efficient than vanilla fuel of the same material
     * @return true if this custom fuel burns longer than the vanilla equivalent
     */
    public boolean isMoreEfficientThanVanilla() {
        int vanillaBurnTime = com.server.crafting.data.FurnaceData.getFuelValue(fuelItem.getType());
        return burnTime > vanillaBurnTime;
    }
    
    @Override
    public String toString() {
        return "CustomFuelData{" +
                "fuelId='" + fuelId + '\'' +
                ", fuelItem=" + getItemDebugName(fuelItem) +
                ", burnTime=" + burnTime + " ticks (" + getFormattedBurnTime() + ")" +
                ", isCustom=" + isCustomItem() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CustomFuelData that = (CustomFuelData) obj;
        return burnTime == that.burnTime &&
               java.util.Objects.equals(fuelId, that.fuelId) &&
               matchesFuel(that.fuelItem);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(fuelId, fuelItem.getType(), burnTime, getCustomModelData());
    }
    
    /**
     * Get debug name for an item
     * @param item The item to get debug name for
     * @return A descriptive name for debugging
     */
    private String getItemDebugName(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == org.bukkit.Material.AIR) return "AIR";
        
        StringBuilder sb = new StringBuilder();
        sb.append(item.getAmount()).append("x ");
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            sb.append(item.getItemMeta().getDisplayName());
        } else {
            sb.append(item.getType().name());
        }
        
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            sb.append(" (CMD:").append(item.getItemMeta().getCustomModelData()).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Create a fuel efficiency report compared to vanilla
     * @return A string describing the efficiency compared to vanilla fuel
     */
    public String getEfficiencyReport() {
        int vanillaBurnTime = com.server.crafting.data.FurnaceData.getFuelValue(fuelItem.getType());
        
        if (vanillaBurnTime == 0) {
            return "Custom fuel (vanilla equivalent: not fuel)";
        }
        
        double efficiency = (double) burnTime / vanillaBurnTime;
        
        if (efficiency > 1.0) {
            return String.format("%.1fx more efficient than vanilla", efficiency);
        } else if (efficiency < 1.0) {
            return String.format("%.1fx less efficient than vanilla", efficiency);
        } else {
            return "Same efficiency as vanilla";
        }
    }
}