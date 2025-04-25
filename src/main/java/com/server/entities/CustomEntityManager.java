package com.server.entities;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

public class CustomEntityManager {

    private final Main plugin;
    private final Map<UUID, ModeledEntity> modeledEntities = new HashMap<>();
    private final Map<UUID, CustomMobStats> mobStats = new HashMap<>();
    private final Map<String, String> registeredModels = new HashMap<>();
    
    public CustomEntityManager(Main plugin) {
        this.plugin = plugin;
        
        // Register available models
        registerModels();
    }
    
    /**
     * Register available models
     */
    private void registerModels() {
        // Model ID format may need to match what ModelEngine expects
        // Make sure these model IDs match exactly what's registered in ModelEngine
        registeredModels.put("golem_prismarine_gm_rain", "golem_prismarine_gm_rain");
        
        // Add more models as needed
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
            plugin.getLogger().warning("Model ID not found: " + modelId);
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
            // Configure entity properties before applying model
            if (livingEntity instanceof Zombie) {
                ((Zombie) livingEntity).setBaby(false);
                ((Zombie) livingEntity).setSilent(true);
                ((Zombie) livingEntity).setAI(true);
            }
            
            // Common settings for all entity types
            livingEntity.setSilent(true); // Make all custom entities silent
            
            // For Iron Golems specifically
            if (type == EntityType.IRON_GOLEM) {
                // Iron golems don't need special properties, 
                // but we can make it invisible here generically
                livingEntity.setInvisible(true);
            }
            
            // Get the full model ID from our registered models
            String fullModelId = registeredModels.get(modelId);
            
            // Create ModeledEntity and ActiveModel using the correct API method
            // Note: We're adapting to whatever method signature the API has
            try {
                // Create ModeledEntity from the spawned entity - using static methods directly
                ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(livingEntity);
                if (modeledEntity == null) {
                    plugin.getLogger().severe("Failed to create ModeledEntity - API returned null");
                    entity.remove();
                    return null;
                }
                
                // Create an active model - using static methods directly
                ActiveModel activeModel = ModelEngineAPI.createActiveModel(fullModelId);
                if (activeModel == null) {
                    plugin.getLogger().severe("Failed to create ActiveModel for " + fullModelId + " - API returned null");
                    entity.remove();
                    return null;
                }
                
                // Apply the model to the entity - try different method signatures
                try {
                    // First try the method with boolean parameter
                    modeledEntity.addModel(activeModel, true);
                } catch (NoSuchMethodError e1) {
                    try {
                        // Then try without the boolean
                        modeledEntity.addModel(activeModel,false);
                    } catch (NoSuchMethodError e2) {
                        try {
                            // Different method name perhaps
                            modeledEntity.addModel(activeModel,true);
                        } catch (NoSuchMethodError e3) {
                            // Last resort - log the error and fail
                            plugin.getLogger().severe("Could not find compatible method to add model to entity");
                            plugin.getLogger().severe("ModelEngine API version mismatch");
                            entity.remove();
                            return null;
                        }
                    }
                }
                
                // Store the modeled entity for later reference
                modeledEntities.put(entity.getUniqueId(), modeledEntity);
            } catch (NoSuchMethodError e) {
                // Fallback for older ModelEngine versions
                plugin.getLogger().warning("Using alternative ModelEngine API methods due to: " + e.getMessage());
                
                try {
                    // Try static methods if they exist
                    ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(livingEntity);
                    ActiveModel activeModel = ModelEngineAPI.createActiveModel(fullModelId);
                    
                    // Try to find the right method
                    try {
                        modeledEntity.addModel(activeModel, true);
                    } catch (NoSuchMethodError e2) {
                        modeledEntity.addModel(activeModel,false);
                    }
                    
                    modeledEntities.put(entity.getUniqueId(), modeledEntity);
                } catch (Exception | NoSuchMethodError e2) {
                    plugin.getLogger().severe("Failed to create model using alternative methods: " + e2.getMessage());
                    entity.remove();
                    return null;
                }
            }
            
            // Set custom display name if provided
            if (customName != null && !customName.isEmpty()) {
                livingEntity.setCustomName(customName);
                livingEntity.setCustomNameVisible(true);
            }
            
            plugin.getLogger().info("Spawned custom entity with model: " + modelId);
            
            return livingEntity;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn custom entity: " + e.getMessage());
            e.printStackTrace();
            entity.remove();
            return null;
        }
    }

    /**
     * Diagnose animation capabilities and log all available animations
     * @param entity The entity to diagnose
     */
    public void diagnoseAnimations(LivingEntity entity) {
        if (entity == null || !isCustomMob(entity)) {
            plugin.getLogger().info("Entity is not a custom mob: " + (entity == null ? "null" : entity.getUniqueId()));
            return;
        }
        
        ModeledEntity modeledEntity = modeledEntities.get(entity.getUniqueId());
        if (modeledEntity == null) {
            plugin.getLogger().info("ModeledEntity not found for entity: " + entity.getUniqueId());
            return;
        }
        
        plugin.getLogger().info("Diagnosing animations for entity: " + entity.getUniqueId());
        
        try {
            // Get all models
            Map<String, ActiveModel> models = modeledEntity.getModels();
            if (models == null || models.isEmpty()) {
                plugin.getLogger().info("No models found for entity");
                return;
            }
            
            plugin.getLogger().info("Entity has " + models.size() + " models");
            
            for (Map.Entry<String, ActiveModel> entry : models.entrySet()) {
                String modelId = entry.getKey();
                ActiveModel model = entry.getValue();
                
                plugin.getLogger().info("Model ID: " + modelId);
                
                // Try to get animation handler
                try {
                    Object handler = model.getAnimationHandler();
                    plugin.getLogger().info("  Has animation handler: " + (handler != null));
                    
                    // Try reflection to get available animations
                    try {
                        java.lang.reflect.Method getAnimationsMethod = handler.getClass().getMethod("getAnimations");
                        Object result = getAnimationsMethod.invoke(handler);
                        plugin.getLogger().info("  Available animations: " + result);
                    } catch (Exception e) {
                        plugin.getLogger().info("  Could not get animations list: " + e.getMessage());
                    }
                } catch (Exception e) {
                    plugin.getLogger().info("  Error accessing animation handler: " + e.getMessage());
                }
                
                // Try alternative methods to detect animation capabilities
                try {
                    java.lang.reflect.Method[] methods = model.getClass().getMethods();
                    plugin.getLogger().info("  Model class has these methods:");
                    for (java.lang.reflect.Method method : methods) {
                        if (method.getName().contains("nim")) {  // "anim" would be in "animation"
                            plugin.getLogger().info("    " + method.getName() + " " + java.util.Arrays.toString(method.getParameterTypes()));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().info("  Error listing methods: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().info("Error diagnosing animations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a Runemark Colossus
     * 
     * @param location The location to spawn at
     * @return The spawned entity
     */
    public LivingEntity spawnRunemarkColossus(Location location) {
        // Changed entity type from ZOMBIE to IRON_GOLEM
        LivingEntity entity = spawnCustomMob(EntityType.IRON_GOLEM, location, "golem_prismarine_gm_rain", "§6Runemark Colossus");
        
        if (entity != null) {
            // Create and apply custom stats
            CustomMobStats stats = new CustomMobStats();
            stats.setHealth(100);
            stats.setMaxHealth(100);
            stats.setPhysicalDamage(10);
            stats.setLevel(5);
            stats.setMobType(MobType.ELITE);
            stats.setName("Runemark Colossus");
            stats.setHasCustomAbilities(true);
            stats.setAttackSpeed(1.0); // 1 attack per second
            
            // Apply the stats to the entity
            try {
                // Use Bukkit's attribute system
                entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getMaxHealth());
                entity.setHealth(stats.getHealth());
                
                // Important: Set attack damage but disable the entity's attack moves
                if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(stats.getPhysicalDamage());
                }
                
                // Make the base entity invisible but ensure the nameplate is shown
                entity.setInvisible(true);
                
                // COMPLETELY PASSIVE IRON GOLEM - No native attacks at all
                if (entity instanceof IronGolem) {
                    // Mark the golem as player-created to prevent hostile behavior
                    ((IronGolem) entity).setPlayerCreated(true);
                    
                    // Set custom metadata to identify it as our custom entity
                    entity.setMetadata("runemark_colossus", new FixedMetadataValue(plugin, true));
                    
                    // Important for our follow behavior
                    entity.setMetadata("custom_follow_only", new FixedMetadataValue(plugin, true));
                    
                    try {
                        // Access NMS classes with reflection to remove its attack goals
                        Object craftEntity = entity.getClass().getMethod("getHandle").invoke(entity);
                        
                        // Access the goalSelector field
                        Field goalSelectorField = null;
                        for (Field field : craftEntity.getClass().getSuperclass().getDeclaredFields()) {
                            if (field.getType().getSimpleName().contains("PathfinderGoalSelector")) {
                                field.setAccessible(true);
                                goalSelectorField = field;
                                break;
                            }
                        }
                        
                        if (goalSelectorField != null) {
                            // Get the goalSelector
                            Object goalSelector = goalSelectorField.get(craftEntity);
                            
                            // Get the 'goals' Set field inside goalSelector
                            Field goalsField = null;
                            for (Field field : goalSelector.getClass().getDeclaredFields()) {
                                if (field.getType().equals(Set.class)) {
                                    field.setAccessible(true);
                                    goalsField = field;
                                    break;
                                }
                            }
                            
                            if (goalsField != null) {
                                // Get the Set<PathfinderGoalWrapper>
                                Set<?> availableGoals = (Set<?>) goalsField.get(goalSelector);
                                
                                // Clear all goals to create completely custom AI
                                availableGoals.clear();
                                
                                plugin.getLogger().info("Successfully cleared entity goals for Runemark Colossus");
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not modify golem AI via NMS: " + e.getMessage());
                    }
                }
                
                // Keep follow AI on but prevent attack AI
                entity.setAI(true);
                
                // Start custom follow and attack behavior for this entity
                startCustomFollowBehavior(entity, stats);
                
                // Make movement slightly slower
                if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.22);
                }
                
                // Make it fireproof and prevent despawning
                entity.setRemoveWhenFarAway(false);
                entity.setFireTicks(0);
                
                // Store the stats for later reference
                mobStats.put(entity.getUniqueId(), stats);
                
                // Update the display name
                updateEntityNameplate(entity);
                
                // Play idle animation after spawning
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playAnimation(entity, "idle");
                }, 5L);
            } catch (Exception e) {
                plugin.getLogger().warning("Error setting entity attributes: " + e.getMessage());
            }
        }
        
        return entity;
    }

    /**
     * Start custom follow and attack behavior for a Runemark Colossus
     * 
     * @param entity The entity to apply behavior to
     * @param stats The entity's stats
     */
    private void startCustomFollowBehavior(LivingEntity entity, CustomMobStats stats) {
        // Track whether an attack animation is currently playing
        entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
        
        // Start a task to handle custom follow behavior
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // If entity is dead or no longer valid, cancel task
            if (!entity.isValid() || entity.isDead()) {
                return;
            }
            
            // Check if we are on attack cooldown
            boolean onCooldown = entity.hasMetadata("attack_cooldown") ? 
                                entity.getMetadata("attack_cooldown").get(0).asBoolean() : false;
            
            // Find closest player within 15 blocks
            Player targetPlayer = null;
            double closestDistance = 15.0;
            
            for (Entity nearby : entity.getNearbyEntities(15, 15, 15)) {
                if (nearby instanceof Player) {
                    Player player = (Player) nearby;
                    
                    // Skip players in creative/spectator mode
                    if (player.getGameMode() == GameMode.CREATIVE || 
                        player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    
                    double distance = entity.getLocation().distance(player.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        targetPlayer = player;
                    }
                }
            }
            
            // If we found a player to follow
            if (targetPlayer != null) {
                Player player = targetPlayer;
                
                // If within attack range (5 blocks) and not on cooldown, attack
                if (closestDistance <= 5.0 && !onCooldown) {
                    // Set cooldown flag
                    entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, true));
                    
                    // Look at the player
                    Location lookLoc = entity.getLocation().clone();
                    lookLoc.setDirection(player.getLocation().subtract(entity.getLocation()).toVector());
                    entity.teleport(lookLoc);
                    
                    // Play attack animation
                    playAnimation(entity, "attack1");
                    plugin.getLogger().info("Runemark Colossus playing attack1 animation at player: " + player.getName());
                    
                    // Deal damage after a delay to match animation
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (entity.isValid() && !entity.isDead() && player.isOnline()) {
                            // Check if still in range
                            if (entity.getLocation().distance(player.getLocation()) <= 7.0) {
                                // Deal damage based on entity stats
                                double damage = stats.getPhysicalDamage();
                                player.damage(damage, entity);
                                
                                // Add visual effects
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                                        player.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.0);
                                
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Runemark Colossus hit " + player.getName() + 
                                            " for " + damage + " damage");
                                }
                            }
                        }
                    }, 30L); // 0.75 seconds delay to sync with animation
                    
                    // Reset cooldown after attack animation completes (1.5 seconds)
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
                    }, 30L); // 1.5 seconds cooldown
                }
                // If not in attack range, follow the player
                else if (closestDistance > 2.0) {
                    // Use Bukkit's pathfinding to follow the player
                    if (entity instanceof Mob) {
                        Mob mob = (Mob) entity;
                        mob.getPathfinder().moveTo(player.getLocation(), 1.0);
                    }
                }
            }
        }, 10L, 10L); // Check every half second (10 ticks)
        
        // Store the task so we can cancel it later
        UUID entityId = entity.getUniqueId();
        customBehaviorTasks.put(entityId, task);
    }

// Map to track custom behavior tasks
private final Map<UUID, BukkitTask> customBehaviorTasks = new HashMap<>();

    /**
     * Play a special ability animation and sound
     * 
     * @param entity The entity to play the special ability for
     * @param abilityIndex The index of the ability to use (1, 2, 3, etc.)
     */
    public void playSpecialAbility(LivingEntity entity, int abilityIndex) {
        if (!isCustomMob(entity)) return;
        
        CustomMobStats stats = getMobStats(entity);
        if (stats == null || !stats.hasCustomAbilities()) return;
        
        // Get the entity ID
        UUID entityId = entity.getUniqueId();
        ModeledEntity modeledEntity = modeledEntities.get(entityId);
        if (modeledEntity == null) return;
        
        String animationName;
        String mobName = stats.getName();
        
        // Determine which animation to play based on mob type and ability index
        if (mobName.equals("Runemark Colossus")) {
            switch (abilityIndex) {
                case 1:
                    animationName = "special1"; // Ground slam
                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 0.5f);
                    break;
                case 2:
                    animationName = "special2"; // Energy beam
                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.8f);
                    break;
                default:
                    animationName = "attack"; // Default attack
                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.0f);
                    break;
            }
        } else {
            // Default for other mobs
            animationName = "attack";
            entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);
        }
        
        // Play the animation
        playAnimation(entity, animationName);
    }

    /**
     * Schedule periodic nameplate refreshes for an entity
     * This helps ensure the nameplate remains visible
     */
    private void scheduleNameplateRefresh(LivingEntity entity) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!entity.isValid() || entity.isDead()) {
                task.cancel();
                return;
            }
            
            // Refresh the nameplate
            updateEntityNameplate(entity);
        }, 20L, 100L); // Initial delay 1 second, then every 5 seconds
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
            // Stand no longer valid, create a new one
            createNameplateStand(entity, displayName);
        }
    }
}

    /**
     * Map to track nameplate ArmorStands for each entity
     */
    private final Map<UUID, ArmorStand> nameplateStands = new HashMap<>();

    /**
     * Create a nameplate ArmorStand for an entity
     */
    private void createNameplateStand(LivingEntity entity, String displayName) {
        // Remove any existing nameplate stand
        removeNameplateStand(entity.getUniqueId());
        
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
            stand.teleport(entity.getLocation().add(0, entity.getHeight() + 0.25, 0));
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
                    plugin.getLogger().warning("Error removing model: " + e.getMessage());
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
                plugin.getLogger().warning("Error removing models from entity: " + e.getMessage());
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
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Removed tracking for entity: " + entityId);
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
     * Get the ModeledEntity for an entity
     * 
     * @param entity The entity
     * @return The ModeledEntity or null if not found
     */
    public ModeledEntity getModeledEntity(Entity entity) {
        if (entity == null) return null;
        return modeledEntities.get(entity.getUniqueId());
    }

    /**
     * Play an animation on a custom mob with more reliable animation triggering
     * 
     * @param entity The entity to play the animation on
     * @param animationName The name of the animation to play
     */
    public void playAnimation(LivingEntity entity, String animationName) {
        if (entity == null || !isCustomMob(entity)) return;
        
        ModeledEntity modeledEntity = modeledEntities.get(entity.getUniqueId());
        if (modeledEntity == null) return;

        // Log attempt to play animation
        plugin.getLogger().info("Attempting to play animation '" + animationName + "' on entity " + entity.getUniqueId());
        
        // Direct approach using ModelEngine API specific to your version
        try {
            // Get all models
            for (ActiveModel model : modeledEntity.getModels().values()) {
                try {
                    // Convert animation name for Runemark Colossus if needed
                    String finalAnimName = animationName;
                    if (entity.hasMetadata("runemark_colossus")) {
                        // Convert any "attack" to "attack1" for this specific entity
                        if (animationName.equals("attack")) {
                            finalAnimName = "attack1";
                        }
                        plugin.getLogger().info("Runemark Colossus using animation: " + finalAnimName);
                    }
                    
                    // Direct approach - using ModelEngine API method
                    model.getAnimationHandler().playAnimation(finalAnimName, 1.0, 1.0, 1.0, true);
                    plugin.getLogger().info("Successfully played animation '" + finalAnimName + "' on entity " + entity.getUniqueId());
                    return;
                } catch (NoSuchMethodError methodError) {
                    plugin.getLogger().warning("Method error trying to play animation: " + methodError.getMessage());
                    // Try fallback methods
                }
            }
            
            // If we reach here, the direct approach failed, use an alternative
            for (Map.Entry<String, ActiveModel> entry : modeledEntity.getModels().entrySet()) {
                ActiveModel model = entry.getValue();
                
                // Convert animation name for Runemark Colossus if needed
                String finalAnimName = animationName;
                if (entity.hasMetadata("runemark_colossus")) {
                    if (animationName.equals("attack")) {
                        finalAnimName = "attack1";
                    }
                }
                
                // Fallback 1: Try animation handler
                try {
                    // Use the correct method signature with appropriate parameters
                    model.getAnimationHandler().playAnimation(finalAnimName, 1.0, 1.0, 1.0, true);
                    plugin.getLogger().info("Played animation via handler");
                    return;
                } catch (Exception e) {
                    plugin.getLogger().warning("Animation handler failed: " + e.getMessage());
                }
                
                // Fallback 2: Try via reflection
                try {
                    Object animHandler = model.getAnimationHandler();
                    java.lang.reflect.Method method = animHandler.getClass().getDeclaredMethod("playAnimation", String.class);
                    method.setAccessible(true);
                    method.invoke(animHandler, finalAnimName);
                    plugin.getLogger().info("Played animation via reflection");
                    return;
                } catch (Exception e) {
                    plugin.getLogger().warning("Reflection approach failed: " + e.getMessage());
                }
            }
            
            // Final fallback - try a completely different approach using ModelEngine API
            try {
                // Get the first model and try to play the animation through its handler
                if (!modeledEntity.getModels().isEmpty()) {
                    ActiveModel firstModel = modeledEntity.getModels().values().iterator().next();
                    firstModel.getAnimationHandler().playAnimation(animationName, 1.0, 1.0, 1.0, true);
                    plugin.getLogger().info("Played animation via first model's animation handler");
                } else {
                    plugin.getLogger().warning("No models found to play animation");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("All animation approaches failed: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error playing animation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stop an animation on a custom mob
     * 
     * @param entity The entity to stop the animation on
     * @param animationName The name of the animation to stop
     */
    public void stopAnimation(LivingEntity entity, String animationName) {
        if (entity == null || !isCustomMob(entity)) return;
        
        ModeledEntity modeledEntity = modeledEntities.get(entity.getUniqueId());
        if (modeledEntity == null) return;
        
        try {
            // Similar approach to playAnimation but for stopping
            if (modeledEntity.getModels() != null && !modeledEntity.getModels().isEmpty()) {
                for (Map.Entry<String, ActiveModel> entry : modeledEntity.getModels().entrySet()) {
                    ActiveModel model = entry.getValue();
                    try {
                        // Try different ways to stop animations
                        try {
                            // Try direct access to animation handler
                            model.getAnimationHandler().stopAnimation(animationName);
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("Stopped animation '" + animationName + "' using handler on entity " + entity.getUniqueId());
                            }
                            return;
                        } catch (NoSuchMethodError | NullPointerException e1) {
                            try {
                                // First try to find if there's a direct method
                                java.lang.reflect.Method method = model.getClass().getMethod("stopAnimation", String.class);
                                method.invoke(model, animationName);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("Stopped animation '" + animationName + "' using reflection on entity " + entity.getUniqueId());
                                }
                                return;
                            } catch (Exception e2) {
                                // Try alternative method names
                                try {
                                    model.getClass().getMethod("cancelAnimation", String.class).invoke(model, animationName);
                                    if (plugin.isDebugMode()) {
                                        plugin.getLogger().info("Cancelled animation '" + animationName + "' on entity " + entity.getUniqueId());
                                    }
                                    return;
                                } catch (Exception e3) {
                                    plugin.getLogger().warning("Failed to stop animation: No compatible method found in this ModelEngine version");
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error with model animation: " + e.getMessage());
                    }
                }
            } else {
                plugin.getLogger().warning("No models found for entity " + entity.getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error stopping animation '" + animationName + "': " + e.getMessage());
        }
    }

    /**
     * Play a death animation and handle death sequence for a custom mob
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
            if (entity.isValid() && !entity.isDead()) {
                // Remove the entity if it hasn't been removed yet
                entity.remove();
            }
            // Clean up tracking regardless
            removeTracking(entity.getUniqueId());
        }, 60L); // 3 seconds (60 ticks) for death animation to complete
    }
    
}