package com.server.profiles.stats;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class PlayerStats {
    // Combat Stats
    private int health;
    private int armor;
    private int magicResist;
    private int physicalDamage;
    private int magicDamage;
    private int mana;
    private int totalMana;
    private double speed;
    private double criticalDamage;
    private double criticalChance;
    private double burstDamage;
    private double burstChance;
    private int cooldownReduction;
    private double lifeSteal;
    private int rangedDamage;
    private double attackSpeed;
    private double omnivamp;
    
    // Fortune Stats
    private double miningFortune;
    private double farmingFortune;
    private double lootingFortune;
    private double fishingFortune;
    
    // Resource Stats
    private int manaRegen;
    private int luck;

    // Minecraft Base Stats
    private double currentHealth;
    private int foodLevel;
    private float saturation;
    private float exhaustion;
    private int expLevel;
    private float expProgress;
    private boolean isFlying;
    private boolean allowFlight;
    private double attackRange;

    // Default Values
    private final int defaultHealth = 100;
    private final int defaultArmor = 0;
    private final int defaultMR = 0;
    private final int defaultPhysicalDamage = 5;
    private final int defaultMagicDamage = 5;
    private final int defaultMana = 100;
    private final double defaultSpeed = 0.1;
    private final double defaultCritDmg = 1.5;
    private final double defaultCritChance = 0.05;
    private final double defaultBurstDmg = 2.0;
    private final double defaultBurstChance = 0.01;
    private final double defaultCDR = 0;
    private final double defaultLifeSteal = 0;
    private final int defaultRangedDamage = 5;
    private final double defaultAttackSpeed = 4.0; // 4 attacks per second
    private final double defaultOmnivamp = 0;
    private final int defaultManaRegen = 1;
    private final int defaultLuck = 0;
    private final double defaultMiningFortune = 1.0;
    private final double defaultFarmingFortune = 1.0;
    private final double defaultLootingFortune = 1.0;
    private final double defaultFishingFortune = 1.0;
    private final double defaultAttackRange = 3.0;

    // Default Values for Minecraft Stats
    private final double defaultCurrentHealth = 100.0; // 10 hearts
    private final int defaultFoodLevel = 20;          // Full food bar
    private final float defaultSaturation = 5.0f;
    private final float defaultExhaustion = 0.0f;
    private final int defaultExpLevel = 0;
    private final float defaultExpProgress = 0.0f;

    public PlayerStats() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        this.health = defaultHealth;
        this.armor = defaultArmor;
        this.magicResist = defaultMR;
        this.physicalDamage = defaultPhysicalDamage;
        this.magicDamage = defaultMagicDamage;
        this.mana = defaultMana;
        this.totalMana = defaultMana;
        this.speed = defaultSpeed;
        this.criticalDamage = defaultCritDmg;
        this.criticalChance = defaultCritChance;
        this.burstDamage = defaultBurstDmg;
        this.burstChance = defaultBurstChance;
        this.cooldownReduction = (int)defaultCDR;
        this.lifeSteal = defaultLifeSteal;
        this.rangedDamage = defaultRangedDamage;
        this.attackSpeed = defaultAttackSpeed;
        this.omnivamp = defaultOmnivamp;
        this.manaRegen = defaultManaRegen;
        this.luck = defaultLuck;
        this.miningFortune = defaultMiningFortune;
        this.farmingFortune = defaultFarmingFortune;
        this.lootingFortune = defaultLootingFortune;
        this.fishingFortune = defaultFishingFortune;
        this.currentHealth = defaultCurrentHealth;
        this.foodLevel = defaultFoodLevel;
        this.saturation = defaultSaturation;
        this.exhaustion = defaultExhaustion;
        this.expLevel = defaultExpLevel;
        this.expProgress = defaultExpProgress;
        this.isFlying = false;
        this.allowFlight = false;
        this.attackRange = defaultAttackRange;
    }

    // Getters and Setters for all stats
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = Math.max(0, health); }
    
    public int getArmor() { return armor; }
    public void setArmor(int armor) { this.armor = armor; }
    
    public int getMagicResist() { return magicResist; }
    public void setMagicResist(int magicResist) { this.magicResist = magicResist; }
    
    public int getPhysicalDamage() { return physicalDamage; }
    public void setPhysicalDamage(int physicalDamage) { this.physicalDamage = Math.max(0, physicalDamage); }    
    public int getMagicDamage() { return magicDamage; }
    public void setMagicDamage(int magicDamage) { this.magicDamage = Math.max(0, magicDamage); }
    
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = Math.max(0, mana); }
    
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.1, speed); }
    
    public double getCriticalDamage() { return criticalDamage; }
    public void setCriticalDamage(double criticalDamage) { this.criticalDamage = Math.max(1.0, criticalDamage); }
    
    public double getCriticalChance() { return criticalChance; }
    public void setCriticalChance(double criticalChance) { this.criticalChance = Math.min(1.0, Math.max(0, criticalChance)); }

    public double getBurstDamage() { return burstDamage; }
    public void setBurstDamage(double burstDamage) { this.burstDamage = Math.max(1.0, burstDamage); }
    
    public double getBurstChance() { return burstChance; }
    public void setBurstChance(double burstChance) { this.burstChance = Math.min(1.0, Math.max(0, burstChance)); }
    
    public int getCooldownReduction() { return cooldownReduction; }
    public void setCooldownReduction(int cooldownReduction) { this.cooldownReduction = Math.min(100, Math.max(0, cooldownReduction)); }
    
    public double getLifeSteal() { return lifeSteal; }
    public void setLifeSteal(double lifeSteal) { this.lifeSteal = Math.max(0, lifeSteal); }
    
    public int getRangedDamage() { return rangedDamage; }
    public void setRangedDamage(int rangedDamage) { this.rangedDamage = Math.max(0, rangedDamage); }
    
    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = Math.max(0.1, attackSpeed); }
    
    public double getOmnivamp() { return omnivamp; }
    public void setOmnivamp(double omnivamp) { this.omnivamp = Math.max(0, omnivamp); }

    // Fortune Stats
    public double getMiningFortune() { return miningFortune; }
    public void setMiningFortune(double miningFortune) { this.miningFortune = Math.max(0, miningFortune); }
    
    public double getFarmingFortune() { return farmingFortune; }
    public void setFarmingFortune(double farmingFortune) { this.farmingFortune = Math.max(0, farmingFortune); }
    
    public double getLootingFortune() { return lootingFortune; }
    public void setLootingFortune(double lootingFortune) { this.lootingFortune = Math.max(0, lootingFortune); }
    
    public double getFishingFortune() { return fishingFortune; }
    public void setFishingFortune(double fishingFortune) { this.fishingFortune = Math.max(0, fishingFortune); }

    // Resource Stats
    public int getManaRegen() { return manaRegen; }
    public void setManaRegen(int manaRegen) { this.manaRegen = Math.max(0, manaRegen); }
    
    public int getLuck() { return luck; }
    public void setLuck(int luck) { this.luck = luck; }
    
    public int getTotalMana() { return totalMana; }
    public void setTotalMana(int totalMana) { 
        this.totalMana = Math.max(0, totalMana);
        this.mana = Math.min(this.mana, this.totalMana); // Ensure current mana doesn't exceed new total
    }

    // Minecraft stat getters and setters
    public double getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(double currentHealth) { 
        this.currentHealth = Math.min(health, Math.max(0, currentHealth));
    }
    
    public int getFoodLevel() { return foodLevel; }
    public void setFoodLevel(int foodLevel) { 
        this.foodLevel = Math.min(20, Math.max(0, foodLevel));
    }
    
    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) {
        this.saturation = Math.min(20.0f, Math.max(0.0f, saturation));
    }
    
    public float getExhaustion() { return exhaustion; }
    public void setExhaustion(float exhaustion) {
        this.exhaustion = Math.min(4.0f, Math.max(0.0f, exhaustion));
    }
    
    public int getExpLevel() { return expLevel; }
    public void setExpLevel(int expLevel) {
        this.expLevel = Math.max(0, expLevel);
    }
    
    public float getExpProgress() { return expProgress; }
    public void setExpProgress(float expProgress) {
        this.expProgress = Math.min(1.0f, Math.max(0.0f, expProgress));
    }
    
    public boolean isFlying() { return isFlying; }
    public void setFlying(boolean flying) { this.isFlying = flying; }
    
    public boolean getAllowFlight() { return allowFlight; }
    public void setAllowFlight(boolean allowFlight) { this.allowFlight = allowFlight; }

    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = Math.max(3.0, attackRange); }


    public int getDefaultPhysicalDamage() {
        return defaultPhysicalDamage;
    }    

    public int getDefaultMagicDamage() {
    return defaultMagicDamage;
    }

    public int getDefaultMana() {
    return defaultMana;
    }   

    public double getDefaultAttackSpeed() {
    return defaultAttackSpeed;
    }   

    public double getDefaultAttackRange() {
    return defaultAttackRange;
    }

    public double calculatePhysicalDamage() {
        double damage = physicalDamage;
        if (Math.random() < criticalChance) {
            damage *= criticalDamage;
        }
        if (Math.random() < burstChance) {
            damage *= burstDamage;
        }
        return damage;
    }

    public double calculateMagicDamage() {
        double damage = magicDamage;
        if (Math.random() < criticalChance) {
            damage *= criticalDamage;
        }
        if (Math.random() < burstChance) {
            damage *= burstDamage;
        }
        return damage;
    }

        // Apply stats to player's minecraft attributes
    public void applyToPlayer(Player player) {
        // Health (scale to keep visual hearts at 10 while affecting actual health)
         AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            // Set current health
            player.setHealth(Math.min(currentHealth, health));
        }

        // Attack Damage
        AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(physicalDamage);
        }

        // Movement Speed
        AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(speed);
        }

        // Attack Speed
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            attackSpeedAttr.setBaseValue(this.attackSpeed);
        }

        // Apply additional Minecraft stats
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setLevel(expLevel);
        player.setExp(expProgress);
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(isFlying);
        }
    }

    // Update stats from player's minecraft attributes
    public void updateFromPlayer(Player player) {
        // Existing attribute updates
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            this.health = (int) maxHealth.getBaseValue();
            this.currentHealth = player.getHealth();
        }

        AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            this.physicalDamage = defaultPhysicalDamage; // Store base value instead of current
        }

        AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) {
            this.speed = movementSpeed.getBaseValue();
        }

        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            this.attackSpeed = attackSpeedAttr.getBaseValue();
        }

        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exhaustion = player.getExhaustion();
        this.expLevel = player.getLevel();
        this.expProgress = player.getExp();
        this.allowFlight = player.getAllowFlight();
        this.isFlying = player.isFlying();

    }

    // Modify existing calculatePhysicalDamage method to handle critical hits
    public double calculatePhysicalDamage(boolean isCritical) {
        double damage = physicalDamage;
        
        // Random critical hit based on critical chance
        if (!isCritical && Math.random() < criticalChance) {
            isCritical = true;
        }
        
        // Apply critical damage
        if (isCritical) {
            damage *= criticalDamage;
        }
        
        // Apply burst damage
        if (Math.random() < burstChance) {
            damage *= burstDamage;
        }
        
        return damage;
    }    

    // Add stat validation helper
    private double clampValue(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    // Update this method to accept temporary bonus to total mana
    public boolean canUseMana(int cost) {
        return mana >= cost;
    }

    // Update this method to accept temporary bonus to total mana
    public void useMana(int cost) {
        if (canUseMana(cost)) {
            setMana(mana - cost);
        }
    }

    // Update this method to accept a temporary mana cap
    public void regenerateMana() {
        setMana(Math.min(mana + manaRegen, totalMana));
    }

    // Add this method to set mana with a temporary cap
    public void setManaWithTemporaryCap(int mana, int tempBonus) {
        this.mana = Math.min(Math.max(0, mana), totalMana + tempBonus);
    }

    // Add this method to set mana with consideration for the total mana cap
    public void setManaWithCap(int mana) {
        this.mana = Math.min(totalMana, Math.max(0, mana));
    }

    // Add this method to get effective total mana with a bonus
    public int getEffectiveTotalMana(int tempBonus) {
        return totalMana + tempBonus;
    }
}