package com.server.entities.mobs;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.server.Main;
import com.server.entities.CustomEntityManager;

/**
 * Registry for custom mobs
 */
public class MobRegistry {
    
    private final Main plugin;
    private final CustomEntityManager entityManager;
    private final Map<String, CustomMob> mobTypes = new HashMap<>();
    
    /**
     * Create a new mob registry
     * 
     * @param plugin The plugin instance
     * @param entityManager The entity manager
     */
    public MobRegistry(Main plugin, CustomEntityManager entityManager) {
        this.plugin = plugin;
        this.entityManager = entityManager;
        registerMobs();
    }
    
    /**
     * Register all custom mobs
     */
    private void registerMobs() {
        // Add Runemark Colossus
        mobTypes.put("runemarkcolossus", new RunemarkColossus(plugin, entityManager));
        
        // Add more mobs as needed
        // mobTypes.put("ghost", new Ghost(plugin, entityManager));
        // mobTypes.put("lich", new Lich(plugin, entityManager));
        
        plugin.getLogger().info("Registered " + mobTypes.size() + " custom mob types");
    }
    
    /**
     * Spawn a custom mob by type ID
     * 
     * @param mobTypeId The mob type ID
     * @param location The location to spawn at
     * @return The spawned entity or null if not found
     */
    public LivingEntity spawnMob(String mobTypeId, Location location) {
        CustomMob mobType = mobTypes.get(mobTypeId.toLowerCase());
        if (mobType == null) {
            plugin.getLogger().warning("Unknown mob type: " + mobTypeId);
            return null;
        }
        
        return mobType.spawn(location);
    }
    
    /**
     * Get a custom mob by type ID
     * 
     * @param mobTypeId The mob type ID
     * @return The custom mob or null if not found
     */
    public CustomMob getMobType(String mobTypeId) {
        return mobTypes.get(mobTypeId.toLowerCase());
    }
    
    /**
     * Get all registered mob types
     * 
     * @return The mob types
     */
    public Map<String, CustomMob> getMobTypes() {
        return new HashMap<>(mobTypes);
    }
    
    /**
     * Check if an entity is a specific mob type
     * 
     * @param entity The entity to check
     * @param metadataKey The metadata key to check for
     * @return True if the entity is of the specified type
     */
    public boolean isSpecificMobType(LivingEntity entity, String metadataKey) {
        return entity != null && entity.hasMetadata(metadataKey);
    }
}