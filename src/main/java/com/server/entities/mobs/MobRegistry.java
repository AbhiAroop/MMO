package com.server.entities.mobs;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.CustomEntityManager;
import com.server.entities.mobs.colossus.DuneetchedColossus;
import com.server.entities.mobs.colossus.RunemarkColossus;
import com.server.entities.mobs.colossus.VerdigranColossus;
import com.server.entities.mobs.duskhollow.DuskhollowFang;

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
        // Add Colossus variants
        mobTypes.put("runemarkcolossus", new RunemarkColossus(plugin, entityManager));
        mobTypes.put("verdigrancolossus", new VerdigranColossus(plugin, entityManager));
        mobTypes.put("duneetchedcolossus", new DuneetchedColossus(plugin, entityManager));
        mobTypes.put("duskhollowfang", new DuskhollowFang(plugin, entityManager));
        // Add more mobs as needed
        // mobTypes.put("ghost", new Ghost(plugin, entityManager));
        // mobTypes.put("lich", new Lich(plugin, entityManager));
        
        plugin.debugLog(DebugSystem.ENTITY,"Registered " + mobTypes.size() + " custom mob types");
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
            plugin.debugLog(DebugSystem.ENTITY,"Unknown mob type: " + mobTypeId);
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