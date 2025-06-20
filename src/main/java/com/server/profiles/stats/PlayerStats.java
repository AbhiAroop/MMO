package com.server.profiles.stats;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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
    private double healthRegen;
    
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
    private double attackRange;
    private double buildRange;
    private double size;
    private double miningSpeed;

    // Default Values
    private int defaultHealth = 100;
    private int defaultArmor = 0;
    private int defaultMR = 0;
    private int defaultPhysicalDamage = 5;
    private int defaultMagicDamage = 5;
    private int defaultMana = 100;
    private double defaultSpeed = 0.1;
    private double defaultCritDmg = 1.5;
    private double defaultCritChance = 0.00;
    private double defaultBurstDmg = 2.0;
    private double defaultBurstChance = 0.01;
    private double defaultCDR = 0;
    private double defaultLifeSteal = 0;
    private int defaultRangedDamage = 5;
    private double defaultAttackSpeed = 0.5; // 0.5 attacks per second
    private double defaultOmnivamp = 0;
    private double defaultHealthRegen = 0.3;
    private int defaultManaRegen = 1;
    private int defaultLuck = 0;
    private double defaultMiningFortune = 1.00;
    private double defaultFarmingFortune = 1.00;
    private double defaultLootingFortune = 1.00;
    private double defaultFishingFortune = 1.00;
    private double defaultAttackRange = 3.0;
    private double defaultBuildRange = 5.0;
    private double defaultSize = 1.0;
    private double defaultMiningSpeed = 0.5;

    // Default Values for Minecraft Stats
    private double defaultCurrentHealth = 100.0; // 10 hearts
    private int defaultFoodLevel = 20;          // Full food bar
    private float defaultSaturation = 5.0f;
    private float defaultExhaustion = 0.0f;
    private int defaultExpLevel = 0;
    private float defaultExpProgress = 0.0f;

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
        this.healthRegen = defaultHealthRegen;
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
        this.attackRange = defaultAttackRange;
        this.size = defaultSize;
        this.miningSpeed = defaultMiningSpeed;
        this.buildRange = defaultBuildRange;
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
    
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = Math.max(3.0, attackRange); }

    public double getSize() { return size; }
    public void setSize(double size) { 
        // Clamp size between Minecraft's allowed range
        this.size = Math.min(16.0, Math.max(0.0625, size)); 
    }

    public double getMiningSpeed() { return miningSpeed; }
    public void setMiningSpeed(double miningSpeed) { this.miningSpeed = Math.max(0.1, miningSpeed); }


    // Add getter and setter for healthRegen
    public double getHealthRegen() { return healthRegen; }
    public void setHealthRegen(double healthRegen) { this.healthRegen = Math.max(0, healthRegen); }

    public double getBuildRange() { return buildRange; }
    public void setBuildRange(double buildRange) { this.buildRange = Math.max(4.5, buildRange); }    

    public int getDefaultHealth() {
        return defaultHealth;
    }

    public int getDefaultArmor() {
        return defaultArmor;
    }

    public int getDefaultMagicResist() {
        return defaultMR;
    }

    public int getDefaultPhysicalDamage() {
        return defaultPhysicalDamage;
    }    

    public int getDefaultMagicDamage() {
        return defaultMagicDamage;
    }

    public int getDefaultMana() {
        return defaultMana;
    }   

    public double getDefaultSpeed() {
        return defaultSpeed;
    }

    public double getDefaultCriticalDamage() {
        return defaultCritDmg;
    }

    public double getDefaultCriticalChance() {
        return defaultCritChance;
    }

    public double getDefaultBurstDamage() {
        return defaultBurstDmg;
    }

    public double getDefaultBurstChance() {
        return defaultBurstChance;
    }

    public int getDefaultCooldownReduction() {
        return (int)defaultCDR;
    }

    public double getDefaultLifeSteal() {
        return defaultLifeSteal;
    }

    public int getDefaultRangedDamage() {
        return defaultRangedDamage;
    }

    public double getDefaultAttackSpeed() {
        return defaultAttackSpeed;
    }   

    public double getDefaultOmnivamp() {
        return defaultOmnivamp;
    }

    public int getDefaultManaRegen() {
        return defaultManaRegen;
    }

    public int getDefaultLuck() {
        return defaultLuck;
    }

    public double getDefaultMiningFortune() {
        return defaultMiningFortune;
    }

    public double getDefaultFarmingFortune() {
        return defaultFarmingFortune;
    }

    public double getDefaultLootingFortune() {
        return defaultLootingFortune;
    }

    public double getDefaultFishingFortune() {
        return defaultFishingFortune;
    }

    public double getDefaultAttackRange() {
        return defaultAttackRange;
    }

    public double getDefaultSize() {
        return defaultSize;
    }

    public double getDefaultMiningSpeed() {
        return defaultMiningSpeed;
    }

    // Default Minecraft stats getters
    public double getDefaultCurrentHealth() {
        return defaultCurrentHealth;
    }

    public int getDefaultFoodLevel() {
        return defaultFoodLevel;
    }

    public float getDefaultSaturation() {
        return defaultSaturation;
    }

    public float getDefaultExhaustion() {
        return defaultExhaustion;
    }

    public int getDefaultExpLevel() {
        return defaultExpLevel;
    }

    public float getDefaultExpProgress() {
        return defaultExpProgress;
    }

    public double getDefaultHealthRegen() {
            return defaultHealthRegen;
    }
    
    public double getDefaultBuildRange() {
        return defaultBuildRange;
    }

    /**
     * Calculate physical damage with manual critical hit control
     * @param forceCritical Whether to force a critical hit
     * @return The calculated damage
     */
    public double calculatePhysicalDamage(boolean forceCritical) {
        double damage = physicalDamage;
        
        // Apply critical damage if forced or random chance
        if (forceCritical || Math.random() < criticalChance) {
            damage *= criticalDamage;
        }
        
        // Apply burst damage
        if (Math.random() < burstChance) {
            damage *= burstDamage;
        }
        
        return damage;
    }

    /**
     * Calculate physical damage with automatic critical hit chance
     * @return The calculated damage
     */
    public double calculatePhysicalDamage() {
        return calculatePhysicalDamage(false);
    }

    /**
     * Calculate magic damage with critical hit potential
     * @param forceCritical Whether to force a critical hit
     * @return The calculated damage
     */
    public double calculateMagicDamage(boolean forceCritical) {
        double damage = magicDamage;
        
        // Apply critical damage if forced or random chance
        if (forceCritical || Math.random() < criticalChance) {
            damage *= criticalDamage;
        }
        
        // Apply burst damage
        if (Math.random() < burstChance) {
            damage *= burstDamage;
        }
        
        return damage;
    }

    /**
     * Calculate magic damage with automatic critical hit chance
     * @return The calculated damage
     */
    public double calculateMagicDamage() {
        return calculateMagicDamage(false);
    }

    /**
     * Check if an attack should be critical based on critical chance
     * @return True if the attack should be critical
     */
    public boolean rollCriticalHit() {
        return Math.random() < criticalChance;
    }

    /**
     * Get the actual critical damage multiplier that will be applied
     * @return The critical damage multiplier
     */
    public double getCriticalMultiplier() {
        return criticalDamage;
    }

        // Apply stats to player's minecraft attributes
    public void applyToPlayer(Player player) {
        // Health (set max health based on the health stat)
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            // Remove all existing modifiers
            Set<AttributeModifier> healthModifiers = new HashSet<>(maxHealth.getModifiers());
            for (AttributeModifier mod : healthModifiers) {
                maxHealth.removeModifier(mod);
            }
            
            // Set base value to vanilla default (20.0)
            maxHealth.setBaseValue(20.0);
            
            // Add our custom modifier for the health stat value
            // This allows health to be properly adjusted when armor is equipped/unequipped
            double healthBonus = health - 20.0;
            if (healthBonus != 0) {
                AttributeModifier healthMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.max_health",
                    healthBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                maxHealth.addModifier(healthMod);
            }
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

        // Attack Speed - handled separately with proper attribute modifiers

        // Apply Scale attribute (added in 1.20.5)
        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttr != null) {
                // Remove any existing modifiers
                Set<AttributeModifier> scaleModifiers = new HashSet<>(scaleAttr.getModifiers());
                for (AttributeModifier mod : scaleModifiers) {
                    scaleAttr.removeModifier(mod);
                }
                
                // Set base value to default (1.0)
                scaleAttr.setBaseValue(1.0);
                
                // Add our custom modifier for the size value
                double sizeBonus = size - 1.0;
                if (sizeBonus != 0) {
                    AttributeModifier sizeMod = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.size",
                        sizeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttr.addModifier(sizeMod);
                }
            }
        } catch (Exception e) {
            // Scale attribute might not be available in older versions
        }

        try {
            AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttr != null) {
                // Remove any existing modifiers
                Set<AttributeModifier> miningSpeedModifiers = new HashSet<>(miningSpeedAttr.getModifiers());
                for (AttributeModifier mod : miningSpeedModifiers) {
                    miningSpeedAttr.removeModifier(mod);
                }
                
                // Set base value to default (1.0)
                miningSpeedAttr.setBaseValue(1.0);
                
                // Add our custom modifier for the mining speed value
                double miningSpeedBonus = miningSpeed - 1.0;
                if (miningSpeedBonus != 0) {
                    AttributeModifier miningSpeedMod = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.mining_speed",
                        miningSpeedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    miningSpeedAttr.addModifier(miningSpeedMod);
                }
            }
        } catch (Exception e) {
            // Mining speed attribute might not be available in older versions
        }

        // Apply additional Minecraft stats
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setLevel(expLevel);
        player.setExp(expProgress);
    }

    // Update stats from player's minecraft attributes
    public void updateFromPlayer(Player player) {
        // Existing attribute updates
        this.currentHealth = player.getHealth();

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

        try {
            AttributeInstance scaleAttr = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttr != null) {
                this.size = scaleAttr.getBaseValue();
            }
        } catch (Exception e) {
            // Scale attribute might not be available in older versions
        }

        try {
            AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttr != null) {
                this.miningSpeed = miningSpeedAttr.getBaseValue();
            }
        } catch (Exception e) {
            // Mining speed attribute might not be available in older versions
        }

        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exhaustion = player.getExhaustion();
        this.expLevel = player.getLevel();
        this.expProgress = player.getExp();
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

    /**
     * Calculate physical damage reduction percentage from armor
     * @return Percentage of physical damage reduced (0-100)
     */
    public double getPhysicalDamageReduction() {
        return (armor * 100.0) / (100.0 + armor);
    }

    /**
     * Calculate magic damage reduction percentage from magic resist
     * @return Percentage of magic damage reduced (0-100)
     */
    public double getMagicDamageReduction() {
        return (magicResist * 100.0) / (100.0 + magicResist);
    }


    public void regenerateHealth() {
        if (currentHealth < health) {
            setCurrentHealth(Math.min(currentHealth + healthRegen, health));
        }
    }
    
    // Update the applyHealthRegeneration method to handle decimal values
    public void applyHealthRegeneration(Player player) {
        if (player.getHealth() < player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
            double newHealth = Math.min(
                player.getHealth() + healthRegen,
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
            );
            player.setHealth(newHealth);
        }
    }

    /**
     * Apply stats to player's minecraft attributes without modifying health
     */
    public void applyToPlayerWithoutHealth(Player player) {
        
        // Apply non-health related Minecraft stats
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setLevel(expLevel);
        player.setExp(expProgress);
    }

    public void increaseDefaultMiningFortune(double amount) {
        this.defaultMiningFortune += amount;
        // Also update the current mining fortune value to reflect the new default
        this.miningFortune += amount;
    }

    public void increaseDefaultMiningSpeed(double amount) {
        this.defaultMiningSpeed += amount;
        // Also update the current mining speed value to reflect the new default
        this.miningSpeed += amount;
    }

    /**
     * Add mining fortune from skill tree nodes
     * This will properly update both the default value and current value
     * @param amount The amount of mining fortune to add
     */
    public void addMiningFortune(double amount) {
        // Update both the current and default values
        this.miningFortune += amount;
    }

    /**
     * Increase the default armor by a specific amount
     * This method ensures both default and current values are updated properly
     */
    public void increaseDefaultArmor(int amount) {
        this.defaultArmor += amount;
        this.armor += amount;
        
        // Ensure values don't go below 0 (minimum armor)
        if (this.defaultArmor < 0) {
            this.defaultArmor = 0;
        }
        if (this.armor < 0) {
            this.armor = 0;
        }
    }
}