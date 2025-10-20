package com.server.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.mobs.MobRegistry;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

/**
 * Manages custom entities with ModelEngine integration
 */
public class CustomEntityManager {

    private final Main plugin;
    private final Map<UUID, ModeledEntity> modeledEntities = new HashMap<>();
    private final Map<UUID, CustomMobStats> mobStats = new HashMap<>();
    private final Map<String, String> registeredModels = new HashMap<>();
    private final Map<UUID, ArmorStand> nameplateStands = new HashMap<>();
    private final Map<UUID, BukkitTask> customBehaviorTasks = new HashMap<>();
    private final MobRegistry mobRegistry;
    
    /**
     * Create a new CustomEntityManager
     * 
     * @param plugin The plugin instance
     */
    public CustomEntityManager(Main plugin) {
        this.plugin = plugin;
        
        // Register available models
        registerModels();
        
        // Create mob registry
        this.mobRegistry = new MobRegistry(plugin, this);
    }
    
    /**
     * Register available models with ModelEngine
     */
    private void registerModels() {
        // Model ID format needs to match what ModelEngine expects
        registeredModels.put("golem_prismarine_gm_rain", "golem_prismarine_gm_rain");
        registeredModels.put("golem_stoneblack_gm_rain", "golem_stoneblack_gm_rain");
        registeredModels.put("golem_stonesand_gm_rain", "golem_stonesand_gm_rain");
        registeredModels.put("darkwolf", "darkwolf");
        // Add more models as needed
    }
    
    /**
     * Spawn a custom mob by type ID
     * 
     * @param mobTypeId The mob type ID
     * @param location The location to spawn at
     * @return The spawned entity
     */
    public LivingEntity spawnCustomMobByType(String mobTypeId, Location location) {
        return mobRegistry.spawnMob(mobTypeId, location);
    }
    
    /**
     * Spawn a custom entity
     * 
     * @param type The entity type to spawn
     * @param location The location to spawn at
     * @param modelId The model ID to apply
     * @param customName The custom name to display
     * @return The spawned entity
     */
    public LivingEntity spawnCustomMob(EntityType type, Location location, String modelId, String customName) {
        if (!registeredModels.containsKey(modelId)) {
            plugin.debugLog(DebugSystem.ENTITY,"Model ID not found: " + modelId);
            return null;
        }
        
        // Check if ModelEngine API is initialized
        if (ModelEngineAPI.getAPI() == null) {
            plugin.getLogger().severe("ModelEngine API is not initialized! Make sure ModelEngine plugin is installed and loaded.");
            plugin.getLogger().severe("Cannot spawn custom mob with model: " + modelId);
            return null;
        }
        
        // Spawn the base entity
        Entity entity = location.getWorld().spawnEntity(location, type);
        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            return null;
        }
        
        LivingEntity livingEntity = (LivingEntity) entity;
        
        try {
            // Common settings for all entity types
            livingEntity.setSilent(true);
            
            // Get the full model ID from our registered models
            String fullModelId = registeredModels.get(modelId);
            
            // Create ModeledEntity directly using ModelEngineAPI
            ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(fullModelId);
            modeledEntity.addModel(activeModel, false);
            modeledEntity.setBaseEntityVisible(false);
            
            // Store the ModeledEntity for future reference
            modeledEntities.put(livingEntity.getUniqueId(), modeledEntity);
            
            // Set custom display name if provided
            if (customName != null && !customName.isEmpty()) {
                livingEntity.setCustomName(customName);
                livingEntity.setCustomNameVisible(true);
            }
            
            if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                plugin.debugLog(DebugSystem.ENTITY,"Spawned custom entity with model: " + modelId);
            }
            
            return livingEntity;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn custom entity: " + e.getMessage());
            e.printStackTrace();
            entity.remove();
            return null;
        }
    }
    
    /**
     * Register mob stats for an entity
     * 
     * @param entityId The entity UUID
     * @param stats The mob stats
     */
    public void registerMobStats(UUID entityId, CustomMobStats stats) {
        mobStats.put(entityId, stats);
    }
    
    /**
     * Update the entity's nameplate to display its stats
     * 
     * @param entity The entity to update
     */
    public void updateEntityNameplate(LivingEntity entity) {
        CustomMobStats stats = mobStats.get(entity.getUniqueId());
        if (stats == null) return;
        
        // Format: [Lv.5] ❈ Runemark Colossus ❤ 100/100
        String prefix;
        switch (stats.getMobType()) {
            case ELITE:
                prefix = "§6❈";
                break;
            case BOSS:
                prefix = "§4☠";
                break;
            case MINIBOSS:
                prefix = "§c✵";
                break;
            default:
                prefix = "§c❈";
                break;
        }
        
        String displayName = String.format("§7[Lv.%d] %s §f%s §c❤ %.1f/%.1f",
                stats.getLevel(), 
                prefix, 
                stats.getName(),
                entity.getHealth(),
                stats.getMaxHealth());
        
        // Use a separate ArmorStand for the nameplate
        UUID entityId = entity.getUniqueId();
        
        // Check if we already have a nameplate ArmorStand for this entity
        if (!nameplateStands.containsKey(entityId)) {
            // Create a new ArmorStand for the nameplate
            createNameplateStand(entity, displayName);
        } else {
            // Update the existing ArmorStand
            ArmorStand stand = nameplateStands.get(entityId);
            if (stand != null && stand.isValid() && !stand.isDead()) {
                stand.setCustomName(displayName);
            } else {
                // Stand is gone but entity exists, create a new one
                createNameplateStand(entity, displayName);
            }
        }
    }
    

    /**
     * Create a nameplate ArmorStand for an entity
     */
    private void createNameplateStand(LivingEntity entity, String displayName) {
        // Remove any existing nameplate stand
        removeNameplateStand(entity.getUniqueId());
        
        // Get custom height offset if it exists
        double tempHeightOffset = 0.25;
        if (entity.hasMetadata("nameplate_height_offset")) {
            tempHeightOffset = entity.getMetadata("nameplate_height_offset").get(0).asDouble();
        }
        final double heightOffset = tempHeightOffset;
        
        // Create a new ArmorStand for the nameplate
        ArmorStand stand = entity.getWorld().spawn(entity.getLocation().add(0, 0.5, 0), ArmorStand.class);
        
        // Configure the ArmorStand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName(displayName);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        
        // Register the ArmorStand
        nameplateStands.put(entity.getUniqueId(), stand);
        
        // Start tracking the entity
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!entity.isValid() || entity.isDead()) {
                // Entity is gone, remove the nameplate
                removeNameplateStand(entity.getUniqueId());
                task.cancel();
                return;
            }
            
            if (!stand.isValid() || stand.isDead()) {
                // Stand is gone but entity exists, create a new one
                task.cancel();
                createNameplateStand(entity, entity.getCustomName());
                return;
            }
            
            // Update the ArmorStand position to follow the entity
            // Use the custom height offset instead of the default
            stand.teleport(entity.getLocation().add(0, entity.getHeight() + heightOffset, 0));
        }, 1L, 1L); // Update every tick for smooth following
    }
    
    /**
     * Remove a nameplate ArmorStand
     */
    private void removeNameplateStand(UUID entityId) {
        ArmorStand stand = nameplateStands.remove(entityId);
        if (stand != null && stand.isValid() && !stand.isDead()) {
            stand.remove();
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Clean up ModeledEntities
        for (UUID entityId : modeledEntities.keySet()) {
            ModeledEntity modeledEntity = modeledEntities.get(entityId);
            if (modeledEntity != null) {
                try {
                    modeledEntity.destroy();
                } catch (Exception e) {
                    plugin.debugLog(DebugSystem.ENTITY,"Error removing model: " + e.getMessage());
                }
            }
        }
        
        // Clean up nameplate ArmorStands
        for (UUID entityId : new HashSet<>(nameplateStands.keySet())) {
            removeNameplateStand(entityId);
        }
        
        // Cancel custom behavior tasks
        for (UUID entityId : customBehaviorTasks.keySet()) {
            BukkitTask task = customBehaviorTasks.get(entityId);
            if (task != null) {
                task.cancel();
            }
        }
        
        modeledEntities.clear();
        mobStats.clear();
        nameplateStands.clear();
        customBehaviorTasks.clear();
    }
    
    /**
     * Remove tracking for an entity
     */
    public void removeTracking(UUID entityId) {
        // Clean up ModeledEntity
        ModeledEntity modeledEntity = modeledEntities.get(entityId);
        if (modeledEntity != null) {
            try {
                modeledEntity.destroy();
            } catch (Exception e) {
                plugin.debugLog(DebugSystem.ENTITY,"Error removing models from entity: " + e.getMessage());
            }
        }
        
        // Clean up nameplate ArmorStand
        removeNameplateStand(entityId);
        
        // Cancel custom behavior task
        BukkitTask task = customBehaviorTasks.remove(entityId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove from tracking maps
        modeledEntities.remove(entityId);
        mobStats.remove(entityId);
        
        if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
            plugin.debugLog(DebugSystem.ENTITY,"Removed tracking for entity: " + entityId);
        }
    }
    
    /**
     * Check if an entity is a custom mob
     * 
     * @param entity The entity to check
     * @return True if the entity is a custom mob
     */
    public boolean isCustomMob(Entity entity) {
        if (entity == null) return false;
        return modeledEntities.containsKey(entity.getUniqueId());
    }
    
    /**
     * Get the stats for a custom mob
     * 
     * @param entity The entity to get stats for
     * @return The custom mob stats
     */
    public CustomMobStats getMobStats(Entity entity) {
        if (entity == null) return null;
        return mobStats.get(entity.getUniqueId());
    }
    
    /**
     * Play an animation on a custom mob
     * 
     * @param entity The entity to play the animation on
     * @param animationName The name of the animation to play
     */
    public void playAnimation(LivingEntity entity, String animationName) {
        if (entity == null || !isCustomMob(entity)) return;
        
        ModeledEntity modeledEntity = modeledEntities.get(entity.getUniqueId());
        if (modeledEntity == null) return;
        
        try {
            // Try to play the animation on all models
            for (Map.Entry<String, ActiveModel> entry : modeledEntity.getModels().entrySet()) {
                ActiveModel model = entry.getValue();
                try {
                    model.getAnimationHandler().playAnimation(animationName, 1.0, 1.0, 1.0, true);
                    return;
                } catch (Exception e) {
                    if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                        plugin.debugLog(DebugSystem.ENTITY,"Error playing animation: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.ENTITY,"Failed to play animation: " + e.getMessage());
        }
    }
    
    /**
     * Handle death sequence for a custom mob
     * 
     * @param entity The entity that died
     */
    public void handleDeath(LivingEntity entity) {
        if (entity == null || !isCustomMob(entity)) return;
        
        // Play death animation
        playAnimation(entity, "death");
        
        // Keep the entity in place during death animation
        entity.setAI(false);
        entity.setInvulnerable(true);
        
        // Schedule removal of the entity after the animation completes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid()) {
                entity.remove();
            }
            
            // Clean up tracking for our custom systems
            removeTracking(entity.getUniqueId());
        }, 40L); // 2 seconds
    }

    /**
     * Plays a special ability animation/effect for a custom mob
     * @param entity The living entity performing the ability
     * @param abilityIndex The index of the ability to play (1 or 2)
     */
    public void playSpecialAbility(LivingEntity entity, int abilityIndex) {
        // Implementation depends on your needs
        // Example implementation:
        if (entity == null) return;
        
        // Play particles and sounds based on ability
        Location loc = entity.getLocation();
        World world = entity.getWorld();
        
        if (abilityIndex == 1) {
            // Ability 1 effects
            world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5, 0.1);
            world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        } else {
            // Ability 2 effects
            world.spawnParticle(Particle.WITCH, loc, 20, 0.5, 1.0, 0.5, 0.1);
            world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.0f);
        }
    }
    
    /**
     * Get the mob registry
     * 
     * @return The mob registry
     */
    public MobRegistry getMobRegistry() {
        return mobRegistry;
    }
}