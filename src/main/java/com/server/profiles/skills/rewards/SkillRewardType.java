package com.server.profiles.skills.rewards;

/**
 * Types of rewards that can be given for skill levels
 */
public enum SkillRewardType {
    STAT_BOOST("Stat Boost", "Increases one of your stats"),
    ITEM("Item", "Gives you an item"),
    CURRENCY("Currency", "Gives you currency"),
    UNLOCK("Unlock", "Unlocks a new feature or ability"),
    PERK("Perk", "Gives you a passive benefit");
    
    // Common stat names as constants
    public static final String HEALTH = "health";
    public static final String ARMOR = "armor";
    public static final String MAGIC_RESIST = "magic_resist";
    public static final String PHYSICAL_DAMAGE = "physical_damage";
    public static final String MAGIC_DAMAGE = "magic_damage";
    public static final String MANA = "mana";
    public static final String SPEED = "speed";
    public static final String CRITICAL_DAMAGE = "critical_damage";
    public static final String CRITICAL_CHANCE = "critical_chance";
    public static final String COOLDOWN_REDUCTION = "cooldown_reduction";
    public static final String LIFE_STEAL = "life_steal";
    
    // Fortune stats
    public static final String MINING_FORTUNE = "mining_fortune";
    public static final String FARMING_FORTUNE = "farming_fortune";
    public static final String LOOTING_FORTUNE = "looting_fortune";
    public static final String FISHING_FORTUNE = "fishing_fortune";
    
    // Farming stats
    public static final String CROP_GROWTH_SPEED = "crop_growth_speed";
    
    // Other stats
    public static final String LUCK = "luck";
    
    private final String displayName;
    private final String description;
    
    SkillRewardType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}