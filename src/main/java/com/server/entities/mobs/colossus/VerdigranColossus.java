package com.server.entities.mobs.colossus;

import org.bukkit.entity.EntityType;

import com.server.Main;
import com.server.entities.CustomEntityManager;
import com.server.entities.CustomMobStats;
import com.server.entities.MobType;

/**
 * Verdigran Colossus - a variant of the Runemark Colossus with a stone-black appearance
 */
public class VerdigranColossus extends RunemarkColossus {
    
    private static final String MODEL_ID = "golem_stoneblack_gm_rain";
    private static final String CUSTOM_NAME = "§6Verdigran Colossus";
    private static final String METADATA_KEY = "verdigran_colossus";
    
    public VerdigranColossus(Main plugin, CustomEntityManager entityManager) {
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
        // Create slightly stronger stats than the base Runemark Colossus
        CustomMobStats stats = new CustomMobStats();
        stats.setHealth(120);
        stats.setMaxHealth(120);
        stats.setPhysicalDamage(12);
        stats.setArmor(55);
        stats.setLevel(6);
        stats.setMobType(MobType.ELITE);
        stats.setName("Verdigran Colossus");
        stats.setHasCustomAbilities(true);
        stats.setAttackSpeed(1.0);
        stats.setExpReward(30);
        stats.setMinGoldDrop(18);
        stats.setMaxGoldDrop(28);
        return stats;
    }
}