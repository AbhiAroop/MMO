package com.server.profiles.skills.skills.farming.botany;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

/**
 * Tracks a custom crop that has been planted in the world
 * Handles growth stages, visual updates, and data persistence
 */
public class PlantedCustomCrop {
    
    private final UUID uuid;
    private final String cropId;
    private final Location blockLocation;
    private UUID displayEntityId;
    
    private int currentStage;
    private long lastGrowthTime;
    private UUID plantedBy;
    
    /**
     * Create a new planted custom crop
     */
    public PlantedCustomCrop(String cropId, Location blockLocation, UUID plantedBy) {
        this.uuid = UUID.randomUUID();
        this.cropId = cropId;
        this.blockLocation = blockLocation.clone();
        this.currentStage = 0;
        this.lastGrowthTime = System.currentTimeMillis();
        this.plantedBy = plantedBy;
        
        // Plant the crop visually
        plantVisual();
    }
    
    /**
     * Create from loaded data
     */
    public PlantedCustomCrop(UUID uuid, String cropId, Location blockLocation,
                            UUID displayEntityId, int currentStage, 
                            long lastGrowthTime, UUID plantedBy) {
        this.uuid = uuid;
        this.cropId = cropId;
        this.blockLocation = blockLocation;
        this.displayEntityId = displayEntityId;
        this.currentStage = currentStage;
        this.lastGrowthTime = lastGrowthTime;
        this.plantedBy = plantedBy;
    }
    
    /**
     * Plant the visual representation of the crop
     * Uses tripwire block state and item display entity
     */
    private void plantVisual() {
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null) {
            return;
        }
        
        Block block = blockLocation.getBlock();
        
        // Set block to tripwire (which is invisible but has collision)
        block.setType(Material.TRIPWIRE);
        
        // Create item display entity above the block
        World world = blockLocation.getWorld();
        if (world != null) {
            Location displayLoc = blockLocation.clone().add(0.5, 0.0, 0.5);
            ItemDisplay display = (ItemDisplay) world.spawnEntity(displayLoc, EntityType.ITEM_DISPLAY);
            
            // Set the item with custom model data
            ItemStack cropItem = new ItemStack(Material.WHEAT_SEEDS);
            org.bukkit.inventory.meta.ItemMeta meta = cropItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(crop.getCustomModelData(0));
                cropItem.setItemMeta(meta);
            }
            display.setItemStack(cropItem);
            
            // Configure display properties
            display.setInvulnerable(true);
            display.setPersistent(true);
            display.setBillboard(Display.Billboard.FIXED);
            
            // Set transformation for proper scaling and positioning
            Transformation transform = display.getTransformation();
            // Scale up to make crops taller (like Custom-Crops plugin)
            // Translate upward so crop sits ON TOP of farmland, not centered in block
            org.joml.Vector3f scale = new org.joml.Vector3f(1.5f, 1.5f, 1.5f);
            org.joml.Vector3f translation = new org.joml.Vector3f(0f, 0.75f, 0f);  // Raised up
            transform.getScale().set(scale);
            transform.getTranslation().set(translation);
            display.setTransformation(transform);
            
            this.displayEntityId = display.getUniqueId();
        }
    }
    
    /**
     * Update the visual to the current growth stage
     */
    public void updateVisual() {
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null) {
            return;
        }
        
        if (displayEntityId != null) {
            World world = blockLocation.getWorld();
            if (world != null) {
                ItemDisplay display = (ItemDisplay) Bukkit.getEntity(displayEntityId);
                if (display != null) {
                    // Update the item with new custom model data
                    ItemStack cropItem = new ItemStack(Material.WHEAT_SEEDS);
                    org.bukkit.inventory.meta.ItemMeta meta = cropItem.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(crop.getCustomModelData(currentStage));
                        cropItem.setItemMeta(meta);
                    }
                    display.setItemStack(cropItem);
                }
            }
        }
    }
    
    /**
     * Attempt to grow the crop to the next stage
     * @return true if grew, false if not ready or already fully grown
     */
    public boolean tryGrow() {
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null || isFullyGrown()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastGrowth = currentTime - lastGrowthTime;
        long requiredTime = crop.getGrowthTimePerStage() * 50; // Convert ticks to milliseconds
        
        if (timeSinceLastGrowth >= requiredTime) {
            currentStage++;
            lastGrowthTime = currentTime;
            updateVisual();
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if the crop is fully grown
     */
    public boolean isFullyGrown() {
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null) {
            return false;
        }
        return currentStage >= crop.getMaxGrowthStages() - 1;
    }
    
    /**
     * Remove the crop from the world
     */
    public void remove() {
        // Remove display entity
        if (displayEntityId != null) {
            World world = blockLocation.getWorld();
            if (world != null) {
                ItemDisplay display = (ItemDisplay) Bukkit.getEntity(displayEntityId);
                if (display != null) {
                    display.remove();
                }
            }
        }
        
        // Remove tripwire block
        Block block = blockLocation.getBlock();
        if (block.getType() == Material.TRIPWIRE) {
            block.setType(Material.AIR);
        }
    }
    
    /**
     * Get progress percentage towards next growth stage
     */
    public double getGrowthProgress() {
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null || isFullyGrown()) {
            return 1.0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastGrowth = currentTime - lastGrowthTime;
        long requiredTime = crop.getGrowthTimePerStage() * 50;
        
        return Math.min(1.0, (double) timeSinceLastGrowth / requiredTime);
    }
    
    /**
     * Serialize to map for storage
     */
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid", uuid.toString());
        data.put("cropId", cropId);
        data.put("world", blockLocation.getWorld().getName());
        data.put("x", blockLocation.getBlockX());
        data.put("y", blockLocation.getBlockY());
        data.put("z", blockLocation.getBlockZ());
        data.put("displayEntityId", displayEntityId != null ? displayEntityId.toString() : null);
        data.put("currentStage", currentStage);
        data.put("lastGrowthTime", lastGrowthTime);
        data.put("plantedBy", plantedBy.toString());
        return data;
    }
    
    /**
     * Deserialize from map
     */
    public static PlantedCustomCrop deserialize(Map<String, Object> data) {
        UUID uuid = UUID.fromString((String) data.get("uuid"));
        String cropId = (String) data.get("cropId");
        
        World world = Bukkit.getWorld((String) data.get("world"));
        int x = (Integer) data.get("x");
        int y = (Integer) data.get("y");
        int z = (Integer) data.get("z");
        Location location = new Location(world, x, y, z);
        
        String displayIdStr = (String) data.get("displayEntityId");
        UUID displayEntityId = displayIdStr != null ? UUID.fromString(displayIdStr) : null;
        
        int currentStage = (Integer) data.get("currentStage");
        long lastGrowthTime = ((Number) data.get("lastGrowthTime")).longValue();
        UUID plantedBy = UUID.fromString((String) data.get("plantedBy"));
        
        return new PlantedCustomCrop(uuid, cropId, location, displayEntityId, 
                                    currentStage, lastGrowthTime, plantedBy);
    }
    
    // Getters
    public UUID getUuid() { return uuid; }
    public String getCropId() { return cropId; }
    public Location getBlockLocation() { return blockLocation.clone(); }
    public UUID getDisplayEntityId() { return displayEntityId; }
    public int getCurrentStage() { return currentStage; }
    public long getLastGrowthTime() { return lastGrowthTime; }
    public UUID getPlantedBy() { return plantedBy; }
    
    public CustomCrop getCrop() {
        return CustomCropRegistry.getInstance().getCrop(cropId);
    }
}
