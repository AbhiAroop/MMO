package com.server.profiles.skills.core;

/**
 * Enum of all subskills in the system
 */
public enum SubskillType {
    // Mining subskills
    ORE_EXTRACTION("ore_extraction", "Ore Extraction", "Mine ores faster and with higher yield for bulk harvesting", SkillType.MINING),
    GEM_CARVING("gem_carving", "Gem Carving", "Extract fragile crystals and gems embedded in rocks with precision", SkillType.MINING),
    ORE_EFFICIENCY("ore_efficiency", "Ore Efficiency", "Mine ores faster and with higher yield", SkillType.MINING),
    RARE_FINDS("rare_finds", "Rare Finds", "Chance to find rare materials while mining", SkillType.MINING),
    STONE_BREAKER("stone_breaker", "Stone Breaker", "Break stone blocks faster", SkillType.MINING),
    
    // Excavating subskills
    TREASURE_HUNTER("treasure_hunter", "Treasure Hunter", "Find treasures while digging", SkillType.EXCAVATING),
    SOIL_MASTER("soil_master", "Soil Master", "Dig soil blocks faster", SkillType.EXCAVATING),
    ARCHAEOLOGIST("archaeologist", "Archaeologist", "Discover ancient artifacts", SkillType.EXCAVATING),
    
    // Fishing subskills
    FISHERMAN("fisherman", "Fisherman", "Catch fish faster", SkillType.FISHING),
    AQUATIC_TREASURES("aquatic_treasures", "Aquatic Treasures", "Find valuable items while fishing", SkillType.FISHING),
    MASTER_ANGLER("master_angler", "Master Angler", "Catch rare fish", SkillType.FISHING),
    
    // Farming subskills
    HARVESTING("harvesting", "Harvesting", "Grants XP upon breaking crops and unlocks access to different crop types", SkillType.FARMING),
    CULTIVATING("cultivating", "Cultivating", "Gain XP from planting and growing crops (requires Harvesting level 10)", SkillType.FARMING),
    BOTANY("botany", "Botany", "Advanced plant manipulation and custom crop mechanics", SkillType.FARMING),
    
    // Combat subskills
    SWORDSMANSHIP("swordsmanship", "Swordsmanship", "Deal more damage with swords", SkillType.COMBAT),
    ARCHERY("archery", "Archery", "Deal more damage with bows", SkillType.COMBAT),
    DEFENSE("defense", "Defense", "Take less damage from attacks", SkillType.COMBAT);
    
    private final String id;
    private final String displayName;
    private final String description;
    private final SkillType parentSkill;
    
    SubskillType(String id, String displayName, String description, SkillType parentSkill) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.parentSkill = parentSkill;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public SkillType getParentSkill() {
        return parentSkill;
    }
    
    /**
     * Get all subskills for a parent skill
     */
    public static SubskillType[] getForParent(SkillType parent) {
        if (parent == null) return new SubskillType[0];
        
        return java.util.Arrays.stream(values())
                .filter(subskill -> subskill.getParentSkill() == parent)
                .toArray(SubskillType[]::new);
    }
}