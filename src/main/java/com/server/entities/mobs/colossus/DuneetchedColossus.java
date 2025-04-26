package com.server.entities.mobs.colossus;

import org.bukkit.entity.EntityType;

import com.server.Main;
import com.server.entities.CustomEntityManager;
import com.server.entities.CustomMobStats;
import com.server.entities.MobType;

/**
 * Duneetched Colossus - a variant of the Runemark Colossus with a sand-colored appearance
 */
public class DuneetchedColossus extends RunemarkColossus {
    
    private static final String MODEL_ID = "golem_stonesand_gm_rain";
    private static final String CUSTOM_NAME = "§6Duneetched Colossus";
    private static final String METADATA_KEY = "duneetched_colossus";
    
    public DuneetchedColossus(Main plugin, CustomEntityManager entityManager) {
        super(plugin, entityManager);
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.IRON_GOLEM;
    }
    
    @Override
    public String getModelId() {
        return MODEL_ID;
    }
    
    @Override
    public String getCustomName() {
        return CUSTOM_NAME;
    }
    
    @Override
    public String getMetadataKey() {
        return METADATA_KEY;
    }
    
    @Override
    public CustomMobStats createStats() {
        // Create stats different from the base Runemark Colossus with higher speed but less armor
        CustomMobStats stats = new CustomMobStats();
        stats.setHealth(110);
        stats.setMaxHealth(110);
        stats.setPhysicalDamage(14);  // Higher damage
        stats.setArmor(40);          // Less armor
        stats.setLevel(6);
        stats.setMobType(MobType.ELITE);
        stats.setName("Duneetched Colossus");
        stats.setHasCustomAbilities(true);
        stats.setAttackSpeed(1.2);   // Faster attack speed
        stats.setExpReward(30);
        stats.setMinGoldDrop(20);
        stats.setMaxGoldDrop(30);
        return stats;
    }
}