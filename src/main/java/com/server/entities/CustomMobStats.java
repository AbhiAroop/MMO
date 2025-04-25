package com.server.entities;

/**
 * Represents the stats of a custom mob
 */
public class CustomMobStats {
    
    private double health;
    private double maxHealth;
    private int physicalDamage;
    private int magicDamage;
    private int armor;
    private int magicResist;
    private int level;
    private String name;
    private MobType mobType;
    
    // Experience and loot settings
    private int expReward;
    private int minGoldDrop;
    private int maxGoldDrop;
    
    // Additional stats
    private double attackSpeed;
    private boolean hasCustomAbilities;
    
    /**
     * Create a new CustomMobStats with default values
     */
    public CustomMobStats() {
        this.health = 20;
        this.maxHealth = 20;
        this.physicalDamage = 5;
        this.magicDamage = 0;
        this.armor = 0;
        this.magicResist = 0;
        this.level = 1;
        this.name = "Custom Mob";
        this.mobType = MobType.NORMAL;
        this.expReward = 5;
        this.minGoldDrop = 1;
        this.maxGoldDrop = 5;
        this.attackSpeed = 1.0;
        this.hasCustomAbilities = false;
    }
    
    // Getters and setters
    public double getHealth() {
        return health;
    }
    
    public void setHealth(double health) {
        this.health = health;
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }
    
    public int getPhysicalDamage() {
        return physicalDamage;
    }
    
    public void setPhysicalDamage(int physicalDamage) {
        this.physicalDamage = physicalDamage;
    }
    
    public int getMagicDamage() {
        return magicDamage;
    }
    
    public void setMagicDamage(int magicDamage) {
        this.magicDamage = magicDamage;
    }
    
    public int getArmor() {
        return armor;
    }
    
    public void setArmor(int armor) {
        this.armor = armor;
    }
    
    public int getMagicResist() {
        return magicResist;
    }
    
    public void setMagicResist(int magicResist) {
        this.magicResist = magicResist;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public MobType getMobType() {
        return mobType;
    }
    
    public void setMobType(MobType mobType) {
        this.mobType = mobType;
    }
    
    public int getExpReward() {
        return expReward;
    }
    
    public void setExpReward(int expReward) {
        this.expReward = expReward;
    }
    
    public int getMinGoldDrop() {
        return minGoldDrop;
    }
    
    public void setMinGoldDrop(int minGoldDrop) {
        this.minGoldDrop = minGoldDrop;
    }
    
    public int getMaxGoldDrop() {
        return maxGoldDrop;
    }
    
    public void setMaxGoldDrop(int maxGoldDrop) {
        this.maxGoldDrop = maxGoldDrop;
    }
    
    public double getAttackSpeed() {
        return attackSpeed;
    }
    
    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }
    
    public boolean hasCustomAbilities() {
        return hasCustomAbilities;
    }
    
    public void setHasCustomAbilities(boolean hasCustomAbilities) {
        this.hasCustomAbilities = hasCustomAbilities;
    }
    
    /**
     * Calculate damage reduction from armor
     * @return The percentage of damage reduced (0-100)
     */
    public double getArmorDamageReduction() {
        return (armor * 100.0) / (armor + 100.0);
    }
    
    /**
     * Calculate magic damage reduction from magic resist
     * @return The percentage of damage reduced (0-100)
     */
    public double getMagicDamageReduction() {
        return (magicResist * 100.0) / (magicResist + 100.0);
    }
}