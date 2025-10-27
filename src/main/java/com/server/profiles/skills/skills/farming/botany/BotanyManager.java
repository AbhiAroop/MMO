package com.server.profiles.skills.skills.farming.botany;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.debug.DebugManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Manages all planted custom crops and crop breeders
 * Handles growth ticks, data persistence, and world interactions
 */
public class BotanyManager {
    
    private static BotanyManager instance;
    
    private final Main plugin;
    private final Map<UUID, PlantedCustomCrop> plantedCrops; // UUID -> crop
    private final Map<Location, PlantedCustomCrop> cropsByLocation; // Location -> crop
    private final Map<UUID, CropBreeder> cropBreeders; // UUID -> breeder
    private final Map<Location, CropBreeder> breedersByLocation; // Location -> breeder
    
    private BukkitTask growthTask;
    private BukkitTask breederTask;
    
    private static final long GROWTH_TICK_INTERVAL = 100L; // 5 seconds (100 ticks)
    private static final long BREEDER_TICK_INTERVAL = 20L; // 1 second
    
    private BotanyManager(Main plugin) {
        this.plugin = plugin;
        this.plantedCrops = new HashMap<>();
        this.cropsByLocation = new HashMap<>();
        this.cropBreeders = new HashMap<>();
        this.breedersByLocation = new HashMap<>();
        
        // Initialize registry
        CustomCropRegistry.getInstance();
        
        // Start growth task
        startGrowthTask();
        startBreederTask();
    }
    
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new BotanyManager(plugin);
        }
    }
    
    public static BotanyManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BotanyManager not initialized!");
        }
        return instance;
    }
    
    /**
     * Start the growth tick task
     */
    private void startGrowthTask() {
        growthTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCropGrowth();
            }
        }.runTaskTimer(plugin, GROWTH_TICK_INTERVAL, GROWTH_TICK_INTERVAL);
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS, 
            "[Botany] Growth task started (interval: " + (GROWTH_TICK_INTERVAL / 20.0) + "s)");
    }
    
    /**
     * Start the breeder update task
     */
    private void startBreederTask() {
        breederTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickBreeders();
            }
        }.runTaskTimer(plugin, BREEDER_TICK_INTERVAL, BREEDER_TICK_INTERVAL);
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS, 
            "[Botany] Breeder task started (interval: " + (BREEDER_TICK_INTERVAL / 20.0) + "s)");
    }
    
    /**
     * Tick all planted crops for growth
     */
    private void tickCropGrowth() {
        int grownCount = 0;
        
        for (PlantedCustomCrop crop : plantedCrops.values()) {
            if (crop.tryGrow()) {
                grownCount++;
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.SKILLS)) {
                    CustomCrop cropData = crop.getCrop();
                    DebugManager.getInstance().debug(DebugSystem.SKILLS,
                        "[Botany] Crop grew: " + cropData.getDisplayName() + 
                        " to stage " + crop.getCurrentStage() + "/" + (cropData.getMaxGrowthStages() - 1));
                }
            }
        }
        
        if (grownCount > 0) {
            DebugManager.getInstance().debug(DebugSystem.SKILLS,
                "[Botany] Growth tick: " + grownCount + "/" + plantedCrops.size() + " crops grew");
        }
    }
    
    /**
     * Tick all active crop breeders
     */
    private void tickBreeders() {
        for (CropBreeder breeder : cropBreeders.values()) {
            if (breeder.isBreeding()) {
                // Check if structure is still valid
                if (!breeder.isStructureValid()) {
                    breeder.cancelBreeding();
                    
                    // Notify owner if online
                    Player owner = Bukkit.getPlayer(breeder.getOwnerUuid());
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage("§c✗ Your crop breeder structure was broken!");
                    }
                    continue;
                }
                
                // Update breeding progress
                Player owner = Bukkit.getPlayer(breeder.getOwnerUuid());
                if (owner != null && owner.isOnline()) {
                    breeder.updateBreeding(owner);
                }
            }
        }
    }
    
    /**
     * Plant a custom crop at a location
     */
    public PlantedCustomCrop plantCrop(String cropId, Location location, UUID plantedBy) {
        // Check if already a crop at this location
        if (cropsByLocation.containsKey(location)) {
            return null;
        }
        
        PlantedCustomCrop crop = new PlantedCustomCrop(cropId, location, plantedBy);
        plantedCrops.put(crop.getUuid(), crop);
        cropsByLocation.put(location, crop);
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS,
            "[Botany] Planted crop: " + cropId + " at " + 
            location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        
        return crop;
    }
    
    /**
     * Get a planted crop at a location
     */
    public PlantedCustomCrop getCropAt(Location location) {
        return cropsByLocation.get(location);
    }
    
    /**
     * Remove a planted crop
     */
    public void removeCrop(PlantedCustomCrop crop) {
        if (crop == null) {
            return;
        }
        
        crop.remove();
        plantedCrops.remove(crop.getUuid());
        cropsByLocation.remove(crop.getBlockLocation());
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS,
            "[Botany] Removed crop: " + crop.getCropId());
    }
    
    /**
     * Register a crop breeder
     */
    public void registerBreeder(CropBreeder breeder) {
        cropBreeders.put(breeder.getId(), breeder);
        breedersByLocation.put(breeder.getCenterLocation(), breeder);
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS,
            "[Botany] Registered breeder at " + breeder.getCenterLocation());
    }
    
    /**
     * Get a breeder at a location
     */
    public CropBreeder getBreederAt(Location location) {
        return breedersByLocation.get(location);
    }
    
    /**
     * Remove a breeder
     */
    public void removeBreeder(CropBreeder breeder) {
        if (breeder == null) {
            return;
        }
        
        breeder.cancelBreeding();
        cropBreeders.remove(breeder.getId());
        breedersByLocation.remove(breeder.getCenterLocation());
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS,
            "[Botany] Removed breeder at " + breeder.getCenterLocation());
    }
    
    /**
     * Get all planted crops
     */
    public List<PlantedCustomCrop> getAllCrops() {
        return new ArrayList<>(plantedCrops.values());
    }
    
    /**
     * Get all breeders
     */
    public List<CropBreeder> getAllBreeders() {
        return new ArrayList<>(cropBreeders.values());
    }
    
    /**
     * Get all crops planted by a player
     */
    public List<PlantedCustomCrop> getCropsByPlayer(UUID playerUuid) {
        List<PlantedCustomCrop> result = new ArrayList<>();
        for (PlantedCustomCrop crop : plantedCrops.values()) {
            if (crop.getPlantedBy().equals(playerUuid)) {
                result.add(crop);
            }
        }
        return result;
    }
    
    /**
     * Get all breeders owned by a player
     */
    public List<CropBreeder> getBreedersByPlayer(UUID playerUuid) {
        List<CropBreeder> result = new ArrayList<>();
        for (CropBreeder breeder : cropBreeders.values()) {
            if (breeder.getOwnerUuid().equals(playerUuid)) {
                result.add(breeder);
            }
        }
        return result;
    }
    
    /**
     * Clear all data (for reload)
     */
    public void clearAll() {
        // Remove all crops
        for (PlantedCustomCrop crop : new ArrayList<>(plantedCrops.values())) {
            removeCrop(crop);
        }
        
        // Remove all breeders
        for (CropBreeder breeder : new ArrayList<>(cropBreeders.values())) {
            removeBreeder(breeder);
        }
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS, "[Botany] Cleared all data");
    }
    
    /**
     * Shutdown the manager
     */
    public void shutdown() {
        if (growthTask != null) {
            growthTask.cancel();
        }
        
        if (breederTask != null) {
            breederTask.cancel();
        }
        
        // Clean up all crops and breeders
        clearAll();
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS, "[Botany] Manager shutdown");
    }
    
    /**
     * Get statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalCrops", plantedCrops.size());
        stats.put("totalBreeders", cropBreeders.size());
        
        int activeBreeders = 0;
        for (CropBreeder breeder : cropBreeders.values()) {
            if (breeder.isBreeding()) {
                activeBreeders++;
            }
        }
        stats.put("activeBreeders", activeBreeders);
        
        int fullyGrown = 0;
        for (PlantedCustomCrop crop : plantedCrops.values()) {
            if (crop.isFullyGrown()) {
                fullyGrown++;
            }
        }
        stats.put("fullyGrownCrops", fullyGrown);
        
        return stats;
    }
}
