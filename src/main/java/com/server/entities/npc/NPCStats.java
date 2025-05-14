package com.server.entities.npc;

/**
 * Represents the stats of an NPC
 * Similar to CustomMobStats but specifically designed for NPCs
 */
public class NPCStats {
    // Combat stats
    private double maxHealth;
    private int physicalDamage;
    private int magicDamage;
    private int armor;
    private int magicResist;
    private double attackSpeed;
    private double attackRange;
    
    // Additional properties
    private int level;
    private boolean hasCustomAbilities;
    private NPCType npcType;
    
    // Rewards
    private int expReward;
    private int minGoldDrop;
    private int maxGoldDrop;
    
    /**
     * Create default NPC stats
     */
    public NPCStats() {
        this.maxHealth = 100.0;
        this.physicalDamage = 10;
        this.magicDamage = 0;
        this.armor = 0;
        this.magicResist = 0;
        this.attackSpeed = 0.5; // Two attacks per second
        this.attackRange = 4.0; // Default attack range
        this.level = 1;
        this.hasCustomAbilities = false;
        this.npcType = NPCType.NORMAL;
        this.expReward = 5;
        this.minGoldDrop = 5;
        this.maxGoldDrop = 15;
    }

    // Getters and setters
    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
    }

    public int getPhysicalDamage() {
        return physicalDamage;
    }

    public void setPhysicalDamage(int physicalDamage) {
        this.physicalDamage = Math.max(0, physicalDamage);
    }

    public int getMagicDamage() {
        return magicDamage;
    }

    public void setMagicDamage(int magicDamage) {
        this.magicDamage = Math.max(0, magicDamage);
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = Math.max(0, armor);
    }

    public int getMagicResist() {
        return magicResist;
    }

    public void setMagicResist(int magicResist) {
        this.magicResist = Math.max(0, magicResist);
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = Math.max(0.1, attackSpeed);
    }

    public double getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = Math.max(1.0, attackRange);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public boolean hasCustomAbilities() {
        return hasCustomAbilities;
    }

    public void setHasCustomAbilities(boolean hasCustomAbilities) {
        this.hasCustomAbilities = hasCustomAbilities;
    }

    public NPCType getNpcType() {
        return npcType;
    }

    public void setNpcType(NPCType npcType) {
        this.npcType = npcType;
    }

    public int getExpReward() {
        return expReward;
    }

    public void setExpReward(int expReward) {
        this.expReward = Math.max(0, expReward);
    }

    public int getMinGoldDrop() {
        return minGoldDrop;
    }

    public void setMinGoldDrop(int minGoldDrop) {
        this.minGoldDrop = Math.max(0, minGoldDrop);
    }

    public int getMaxGoldDrop() {
        return maxGoldDrop;
    }

    public void setMaxGoldDrop(int maxGoldDrop) {
        this.maxGoldDrop = Math.max(minGoldDrop, maxGoldDrop);
    }

    /**
     * Calculate damage reduction from armor
     * @return Percentage of damage reduced (0-100)
     */
    public double getArmorDamageReduction() {
        return (armor * 100.0) / (armor + 100.0);
    }

    /**
     * Calculate magic damage reduction from magic resist
     * @return Percentage of damage reduced (0-100)
     */
    public double getMagicDamageReduction() {
        return (magicResist * 100.0) / (magicResist + 100.0);
    }
    
    /**
     * Calculate attack interval in ticks
     * @return Number of ticks between attacks
     */
    public int getAttackIntervalTicks() {
        return Math.max(1, (int)(20 / attackSpeed));
    }
}