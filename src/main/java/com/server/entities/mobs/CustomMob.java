package com.server.entities.mobs;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.CustomEntityManager;
import com.server.entities.CustomMobStats;

/**
 * Base class for all custom mobs
 */
public abstract class CustomMob {
    
    protected final Main plugin;
    protected final CustomEntityManager entityManager;
    protected BukkitTask behaviorTask;
    
    /**
     * Create a new custom mob
     * 
     * @param plugin The plugin instance
     * @param entityManager The entity manager
     */
    public CustomMob(Main plugin, CustomEntityManager entityManager) {
        this.plugin = plugin;
        this.entityManager = entityManager;
    }
    
    /**
     * Spawn this custom mob at a location
     * 
     * @param location The location to spawn at
     * @return The spawned entity
     */
    public abstract LivingEntity spawn(Location location);
    
    /**
     * Initialize custom mob behavior
     * 
     * @param entity The entity
     * @param stats The mob stats
     */
    public abstract void initializeBehavior(LivingEntity entity, CustomMobStats stats);
    
    /**
     * Get the entity type for this mob
     * @return The entity type
     */
    public abstract EntityType getEntityType();
    
    /**
     * Get the model ID for this mob
     * @return The model ID
     */
    public abstract String getModelId();
    
    /**
     * Get the custom name for this mob
     * @return The custom name
     */
    public abstract String getCustomName();
    
    /**
     * Get the metadata key for this mob
     * @return The metadata key
     */
    public abstract String getMetadataKey();
    
    /**
     * Get the custom stats for this mob
     * @return The custom stats
     */
    public abstract CustomMobStats createStats();
    
    /**
     * Play the idle animation for this mob
     * @param entity The entity
     */
    public void playIdleAnimation(LivingEntity entity) {
        entityManager.playAnimation(entity, "idle");
    }
    
    /**
     * Play the attack animation for this mob
     * @param entity The entity
     */
    public void playAttackAnimation(LivingEntity entity) {
        entityManager.playAnimation(entity, "attack");
    }
    
    /**
     * Play the hurt animation for this mob
     * @param entity The entity
     */
    public void playHurtAnimation(LivingEntity entity) {
        entityManager.playAnimation(entity, "hurt");
    }
    
    /**
     * Clean up resources for this mob
     * @param entity The entity
     */
    public void cleanup(LivingEntity entity) {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
    }
    
    /**
     * Remove attack goals from an entity using reflection
     * Useful for making certain mobs passive
     * 
     * @param entity The entity to modify
     */
    protected void removeAttackGoals(LivingEntity entity) {
        try {
            // Access NMS classes with reflection to remove its attack goals
            Object craftEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            
            // Access the goalSelector field
            Field goalSelectorField = null;
            Field targetSelectorField = null;
            
            for (Field field : craftEntity.getClass().getSuperclass().getDeclaredFields()) {
                if (field.getType().getSimpleName().contains("PathfinderGoalSelector")) {
                    field.setAccessible(true);
                    
                    // There are typically two PathfinderGoalSelector fields:
                    // - one for general goals (movement, etc.)
                    // - one for target selection goals
                    if (goalSelectorField == null) {
                        goalSelectorField = field;
                    } else {
                        targetSelectorField = field;
                    }
                }
            }
            
            // Clear the target selector goals first (prevents targeting anything)
            if (targetSelectorField != null) {
                Object targetSelector = targetSelectorField.get(craftEntity);
                
                // Get the 'goals' Set field inside targetSelector
                Field goalsField = null;
                for (Field field : targetSelector.getClass().getDeclaredFields()) {
                    if (field.getType().equals(Set.class)) {
                        field.setAccessible(true);
                        goalsField = field;
                        break;
                    }
                }
                
                if (goalsField != null) {
                    // Get the Set<PathfinderGoalWrapper>
                    Set<?> availableTargetGoals = (Set<?>) goalsField.get(targetSelector);
                    
                    // Clear all targeting goals
                    availableTargetGoals.clear();
                    plugin.debugLog(DebugSystem.ENTITY,"Successfully cleared entity target goals for " + getClass().getSimpleName());
                }
            }
            
            // Now clear the regular goals (or just attack goals if we could)
            if (goalSelectorField != null) {
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
                    
                    // Clear all goals - we will handle movement ourselves
                    availableGoals.clear();
                    plugin.debugLog(DebugSystem.ENTITY,"Successfully cleared entity goals for " + getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.ENTITY,"Could not modify entity AI via NMS: " + e.getMessage());
            if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Register a damage prevention listener that stops an entity from being targeted when hit
     * 
     * @param entity The entity to protect
     */
    protected void registerDamagePreventionListener(LivingEntity entity) {
        final UUID entityId = entity.getUniqueId();
        
        // Register a listener that prevents the entity from targeting when damaged
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onEntityDamaged(EntityDamageByEntityEvent event) {
                if (event.getEntity().getUniqueId().equals(entityId)) {
                    // If this specific entity is hurt, prevent it from targeting the damager
                    if (event.getEntity() instanceof Mob) {
                        Mob mob = (Mob) event.getEntity();
                        
                        // Force it not to target due to being hurt
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (mob.isValid() && !mob.isDead()) {
                                    // Let our custom AI handle targeting
                                    mob.setTarget(null);
                                }
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }, plugin);
    }
    
    /**
     * Register a listener that prevents an entity from dealing damage through vanilla mechanics
     * 
     * @param entity The entity to modify
     */
    protected void registerDamageCancellationListener(LivingEntity entity) {
        final UUID entityId = entity.getUniqueId();
        
        // Register a listener that cancels all damage from this entity
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
                // If this specific entity is dealing damage through vanilla mechanics,
                // cancel it - we'll handle damage application manually
                if (event.getDamager().getUniqueId().equals(entityId)) {
                    // Only cancel damage from normal attacks, not our custom damage
                    if (!event.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM)) {
                        // Cancel the damage event completely
                        event.setCancelled(true);
                        if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                            plugin.debugLog(DebugSystem.ENTITY,"Cancelled native attack from " + getClass().getSimpleName());
                        }
                    }
                }
            }
        }, plugin);
    }
    
    /**
     * Apply base entity modifications common to many custom mobs
     * 
     * @param entity The entity to modify
     * @param stats The mob stats
     */
    protected void applyBaseEntityModifications(LivingEntity entity, CustomMobStats stats) {
        // Apply stats using Bukkit's attribute system
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getMaxHealth());
        entity.setHealth(stats.getHealth());
        
        // Set attack damage to 0 to prevent any damage from normal attacks
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0);
        }
        
        // Make the base entity invisible but ensure the nameplate is shown
        entity.setInvisible(true);
        
        // Various entity modifications to make it behave better
        entity.setRemoveWhenFarAway(false);
        entity.setFireTicks(0);
        
        // Add metadata to identify this entity
        entity.setMetadata(getMetadataKey(), new FixedMetadataValue(plugin, true));
        
        // If it's a mob, ensure it's not targeting anything (to start)
        if (entity instanceof Mob) {
            ((Mob) entity).setTarget(null);
        }
    }
}