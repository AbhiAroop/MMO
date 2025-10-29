package com.server.profiles.skills.skills.fishing.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.EntityType;

import com.server.profiles.skills.skills.fishing.types.FishingType;

/**
 * Represents mobs that can be caught while fishing
 */
public enum FishingMob {
    // Normal Water Mobs
    DROWNED(EntityType.DROWNED, 0.05, FishingType.NORMAL_WATER, "Drowned"),
    GUARDIAN(EntityType.GUARDIAN, 0.03, FishingType.NORMAL_WATER, "Guardian"),
    SQUID(EntityType.SQUID, 0.08, FishingType.NORMAL_WATER, "Squid"),
    GLOW_SQUID(EntityType.GLOW_SQUID, 0.05, FishingType.NORMAL_WATER, "Glow Squid"),
    
    // Lava Mobs
    BLAZE(EntityType.BLAZE, 0.06, FishingType.LAVA, "Blaze"),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, 0.08, FishingType.LAVA, "Magma Cube"),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, 0.04, FishingType.LAVA, "Wither Skeleton"),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, 0.07, FishingType.LAVA, "Zombified Piglin"),
    
    // Ice Mobs
    STRAY(EntityType.STRAY, 0.06, FishingType.ICE, "Stray"),
    POLAR_BEAR(EntityType.POLAR_BEAR, 0.05, FishingType.ICE, "Polar Bear"),
    SKELETON(EntityType.SKELETON, 0.07, FishingType.ICE, "Skeleton"),
    
    // Void Mobs (rare and dangerous)
    ENDERMAN(EntityType.ENDERMAN, 0.04, FishingType.VOID, "Enderman"),
    ENDERMITE(EntityType.ENDERMITE, 0.06, FishingType.VOID, "Endermite"),
    SHULKER(EntityType.SHULKER, 0.02, FishingType.VOID, "Shulker"),
    PHANTOM(EntityType.PHANTOM, 0.05, FishingType.VOID, "Phantom");
    
    private final EntityType entityType;
    private final double baseChance;
    private final FishingType fishingType;
    private final String displayName;
    private static final Random random = new Random();
    
    FishingMob(EntityType entityType, double baseChance, FishingType fishingType, String displayName) {
        this.entityType = entityType;
        this.baseChance = baseChance;
        this.fishingType = fishingType;
        this.displayName = displayName;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public double getBaseChance() {
        return baseChance;
    }
    
    public FishingType getFishingType() {
        return fishingType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get all mobs that can spawn in a specific fishing type
     */
    public static List<FishingMob> getMobsForType(FishingType fishingType) {
        List<FishingMob> mobs = new ArrayList<>();
        for (FishingMob mob : values()) {
            if (mob.getFishingType() == fishingType) {
                mobs.add(mob);
            }
        }
        return mobs;
    }
    
    /**
     * Try to spawn a mob based on fishing type and luck
     * @param fishingType The type of fishing environment
     * @param luckBonus Additional luck modifier (0.0 to 1.0+)
     * @return A FishingMob if one should spawn, null otherwise
     */
    public static FishingMob trySpawnMob(FishingType fishingType, double luckBonus) {
        List<FishingMob> possibleMobs = getMobsForType(fishingType);
        if (possibleMobs.isEmpty()) {
            return null;
        }
        
        // Calculate total chance with luck bonus
        // Base chance: 0-10% depending on mob
        // Luck reduces chance (higher luck = less likely to fish up mobs)
        double totalChance = 0.0;
        for (FishingMob mob : possibleMobs) {
            // Reduce mob chance if player has high luck (they want fish, not mobs)
            double adjustedChance = mob.getBaseChance() * (1.0 - (luckBonus * 0.5));
            totalChance += Math.max(0.01, adjustedChance); // Minimum 1% chance per mob
        }
        
        // Roll to see if any mob spawns
        double roll = random.nextDouble();
        if (roll > totalChance) {
            return null; // No mob spawns
        }
        
        // If a mob spawns, select which one based on weighted chances
        double cumulativeChance = 0.0;
        double selectRoll = random.nextDouble() * totalChance;
        
        for (FishingMob mob : possibleMobs) {
            double adjustedChance = mob.getBaseChance() * (1.0 - (luckBonus * 0.5));
            cumulativeChance += Math.max(0.01, adjustedChance);
            
            if (selectRoll <= cumulativeChance) {
                return mob;
            }
        }
        
        // Fallback to first mob if somehow nothing was selected
        return possibleMobs.get(0);
    }
}
