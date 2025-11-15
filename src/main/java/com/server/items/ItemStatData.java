package com.server.items;

import org.bukkit.inventory.ItemStack;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Stores item stat data in item NBT using persistent data
 * Format: MMO_ItemStat_<statname> for each stat
 * This provides a reliable, non-lore-dependent way to store item statistics
 */
public class ItemStatData {
    
    private static final String NBT_PREFIX = "MMO_ItemStat_";
    
    // All possible stat keys
    private static final String HEALTH = NBT_PREFIX + "Health";
    private static final String ARMOR = NBT_PREFIX + "Armor";
    private static final String MAGIC_RESIST = NBT_PREFIX + "MagicResist";
    private static final String PHYSICAL_DAMAGE = NBT_PREFIX + "PhysicalDamage";
    private static final String RANGED_DAMAGE = NBT_PREFIX + "RangedDamage";
    private static final String MAGIC_DAMAGE = NBT_PREFIX + "MagicDamage";
    private static final String MANA = NBT_PREFIX + "Mana";
    private static final String COOLDOWN_REDUCTION = NBT_PREFIX + "CooldownReduction";
    private static final String HEALTH_REGEN = NBT_PREFIX + "HealthRegen";
    private static final String ATTACK_SPEED = NBT_PREFIX + "AttackSpeed";
    private static final String ATTACK_RANGE = NBT_PREFIX + "AttackRange";
    private static final String SIZE = NBT_PREFIX + "Size";
    private static final String LIFE_STEAL = NBT_PREFIX + "LifeSteal";
    private static final String CRIT_CHANCE = NBT_PREFIX + "CritChance";
    private static final String CRIT_DAMAGE = NBT_PREFIX + "CritDamage";
    private static final String OMNIVAMP = NBT_PREFIX + "Omnivamp";
    private static final String MINING_FORTUNE = NBT_PREFIX + "MiningFortune";
    private static final String MINING_SPEED = NBT_PREFIX + "MiningSpeed";
    private static final String BUILD_RANGE = NBT_PREFIX + "BuildRange";
    private static final String LURE_POTENCY = NBT_PREFIX + "LurePotency";
    private static final String FISHING_FORTUNE = NBT_PREFIX + "FishingFortune";
    private static final String FISHING_RESILIENCE = NBT_PREFIX + "FishingResilience";
    private static final String FISHING_FOCUS = NBT_PREFIX + "FishingFocus";
    private static final String FISHING_PRECISION = NBT_PREFIX + "FishingPrecision";
    private static final String SEA_MONSTER_AFFINITY = NBT_PREFIX + "SeaMonsterAffinity";
    private static final String TREASURE_SENSE = NBT_PREFIX + "TreasureSense";
    
    /**
     * Builder class for creating items with stats
     */
    public static class Builder {
        private final NBTItem nbtItem;
        
        public Builder(ItemStack item) {
            this.nbtItem = new NBTItem(item);
        }
        
        public Builder health(int value) {
            if (value != 0) nbtItem.setInteger(HEALTH, value);
            return this;
        }
        
        public Builder armor(int value) {
            if (value != 0) nbtItem.setInteger(ARMOR, value);
            return this;
        }
        
        public Builder magicResist(int value) {
            if (value != 0) nbtItem.setInteger(MAGIC_RESIST, value);
            return this;
        }
        
        public Builder physicalDamage(int value) {
            if (value != 0) nbtItem.setInteger(PHYSICAL_DAMAGE, value);
            return this;
        }
        
        public Builder rangedDamage(int value) {
            if (value != 0) nbtItem.setInteger(RANGED_DAMAGE, value);
            return this;
        }
        
        public Builder magicDamage(int value) {
            if (value != 0) nbtItem.setInteger(MAGIC_DAMAGE, value);
            return this;
        }
        
        public Builder mana(int value) {
            if (value != 0) nbtItem.setInteger(MANA, value);
            return this;
        }
        
        public Builder cooldownReduction(int value) {
            if (value != 0) nbtItem.setInteger(COOLDOWN_REDUCTION, value);
            return this;
        }
        
        public Builder healthRegen(double value) {
            if (value != 0) nbtItem.setDouble(HEALTH_REGEN, value);
            return this;
        }
        
        public Builder attackSpeed(double value) {
            if (value != 0) nbtItem.setDouble(ATTACK_SPEED, value);
            return this;
        }
        
        public Builder attackRange(double value) {
            if (value != 0) nbtItem.setDouble(ATTACK_RANGE, value);
            return this;
        }
        
        public Builder size(double value) {
            if (value != 0) nbtItem.setDouble(SIZE, value);
            return this;
        }
        
        public Builder lifeSteal(double value) {
            if (value != 0) nbtItem.setDouble(LIFE_STEAL, value);
            return this;
        }
        
        public Builder critChance(double value) {
            if (value != 0) nbtItem.setDouble(CRIT_CHANCE, value);
            return this;
        }
        
        public Builder critDamage(double value) {
            if (value != 0) nbtItem.setDouble(CRIT_DAMAGE, value);
            return this;
        }
        
        public Builder omnivamp(double value) {
            if (value != 0) nbtItem.setDouble(OMNIVAMP, value);
            return this;
        }
        
        public Builder miningFortune(double value) {
            if (value != 0) nbtItem.setDouble(MINING_FORTUNE, value);
            return this;
        }
        
        public Builder miningSpeed(double value) {
            if (value != 0) nbtItem.setDouble(MINING_SPEED, value);
            return this;
        }
        
        public Builder buildRange(double value) {
            if (value != 0) nbtItem.setDouble(BUILD_RANGE, value);
            return this;
        }
        
        public Builder lurePotency(int value) {
            if (value != 0) nbtItem.setInteger(LURE_POTENCY, value);
            return this;
        }
        
        public Builder fishingFortune(double value) {
            if (value != 0) nbtItem.setDouble(FISHING_FORTUNE, value);
            return this;
        }
        
        public Builder fishingResilience(double value) {
            if (value != 0) nbtItem.setDouble(FISHING_RESILIENCE, value);
            return this;
        }
        
        public Builder fishingFocus(double value) {
            if (value != 0) nbtItem.setDouble(FISHING_FOCUS, value);
            return this;
        }
        
        public Builder fishingPrecision(double value) {
            if (value != 0) nbtItem.setDouble(FISHING_PRECISION, value);
            return this;
        }
        
        public Builder seaMonsterAffinity(double value) {
            if (value != 0) nbtItem.setDouble(SEA_MONSTER_AFFINITY, value);
            return this;
        }
        
        public Builder treasureSense(double value) {
            if (value != 0) nbtItem.setDouble(TREASURE_SENSE, value);
            return this;
        }
        
        /**
         * Apply the NBT data to the item and return it
         */
        public ItemStack build() {
            return nbtItem.getItem();
        }
    }
    
    /**
     * Container class for all item stats
     */
    public static class Stats {
        public int health = 0;
        public int armor = 0;
        public int magicResist = 0;
        public int physicalDamage = 0;
        public int rangedDamage = 0;
        public int magicDamage = 0;
        public int mana = 0;
        public int cooldownReduction = 0;
        public double healthRegen = 0;
        public double attackSpeed = 0;
        public double attackRange = 0;
        public double size = 0;
        public double lifeSteal = 0;
        public double critChance = 0;
        public double critDamage = 0;
        public double omnivamp = 0;
        public double miningFortune = 0;
        public double miningSpeed = 0;
        public double buildRange = 0;
        public int lurePotency = 0;
        public double fishingFortune = 0;
        public double fishingResilience = 0;
        public double fishingFocus = 0;
        public double fishingPrecision = 0;
        public double seaMonsterAffinity = 0;
        public double treasureSense = 0;
    }
    
    /**
     * Read all stats from an item
     */
    public static Stats getStats(ItemStack item) {
        Stats stats = new Stats();
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return stats;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        
        // Read integer stats
        if (nbtItem.hasKey(HEALTH)) stats.health = nbtItem.getInteger(HEALTH);
        if (nbtItem.hasKey(ARMOR)) stats.armor = nbtItem.getInteger(ARMOR);
        if (nbtItem.hasKey(MAGIC_RESIST)) stats.magicResist = nbtItem.getInteger(MAGIC_RESIST);
        if (nbtItem.hasKey(PHYSICAL_DAMAGE)) stats.physicalDamage = nbtItem.getInteger(PHYSICAL_DAMAGE);
        if (nbtItem.hasKey(RANGED_DAMAGE)) stats.rangedDamage = nbtItem.getInteger(RANGED_DAMAGE);
        if (nbtItem.hasKey(MAGIC_DAMAGE)) stats.magicDamage = nbtItem.getInteger(MAGIC_DAMAGE);
        if (nbtItem.hasKey(MANA)) stats.mana = nbtItem.getInteger(MANA);
        if (nbtItem.hasKey(COOLDOWN_REDUCTION)) stats.cooldownReduction = nbtItem.getInteger(COOLDOWN_REDUCTION);
        if (nbtItem.hasKey(LURE_POTENCY)) stats.lurePotency = nbtItem.getInteger(LURE_POTENCY);
        
        // Read double stats
        if (nbtItem.hasKey(HEALTH_REGEN)) stats.healthRegen = nbtItem.getDouble(HEALTH_REGEN);
        if (nbtItem.hasKey(ATTACK_SPEED)) stats.attackSpeed = nbtItem.getDouble(ATTACK_SPEED);
        if (nbtItem.hasKey(ATTACK_RANGE)) stats.attackRange = nbtItem.getDouble(ATTACK_RANGE);
        if (nbtItem.hasKey(SIZE)) stats.size = nbtItem.getDouble(SIZE);
        if (nbtItem.hasKey(LIFE_STEAL)) stats.lifeSteal = nbtItem.getDouble(LIFE_STEAL);
        if (nbtItem.hasKey(CRIT_CHANCE)) stats.critChance = nbtItem.getDouble(CRIT_CHANCE);
        if (nbtItem.hasKey(CRIT_DAMAGE)) stats.critDamage = nbtItem.getDouble(CRIT_DAMAGE);
        if (nbtItem.hasKey(OMNIVAMP)) stats.omnivamp = nbtItem.getDouble(OMNIVAMP);
        if (nbtItem.hasKey(MINING_FORTUNE)) stats.miningFortune = nbtItem.getDouble(MINING_FORTUNE);
        if (nbtItem.hasKey(MINING_SPEED)) stats.miningSpeed = nbtItem.getDouble(MINING_SPEED);
        if (nbtItem.hasKey(BUILD_RANGE)) stats.buildRange = nbtItem.getDouble(BUILD_RANGE);
        if (nbtItem.hasKey(FISHING_FORTUNE)) stats.fishingFortune = nbtItem.getDouble(FISHING_FORTUNE);
        if (nbtItem.hasKey(FISHING_RESILIENCE)) stats.fishingResilience = nbtItem.getDouble(FISHING_RESILIENCE);
        if (nbtItem.hasKey(FISHING_FOCUS)) stats.fishingFocus = nbtItem.getDouble(FISHING_FOCUS);
        if (nbtItem.hasKey(FISHING_PRECISION)) stats.fishingPrecision = nbtItem.getDouble(FISHING_PRECISION);
        if (nbtItem.hasKey(SEA_MONSTER_AFFINITY)) stats.seaMonsterAffinity = nbtItem.getDouble(SEA_MONSTER_AFFINITY);
        if (nbtItem.hasKey(TREASURE_SENSE)) stats.treasureSense = nbtItem.getDouble(TREASURE_SENSE);
        
        return stats;
    }
    
    /**
     * Check if an item has any stat data
     */
    public static boolean hasStats(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        
        // Check if any stat key exists
        return nbtItem.hasKey(HEALTH) || nbtItem.hasKey(ARMOR) || nbtItem.hasKey(MAGIC_RESIST) ||
               nbtItem.hasKey(PHYSICAL_DAMAGE) || nbtItem.hasKey(RANGED_DAMAGE) || nbtItem.hasKey(MAGIC_DAMAGE) ||
               nbtItem.hasKey(MANA) || nbtItem.hasKey(COOLDOWN_REDUCTION) || nbtItem.hasKey(HEALTH_REGEN) ||
               nbtItem.hasKey(ATTACK_SPEED) || nbtItem.hasKey(ATTACK_RANGE) || nbtItem.hasKey(SIZE) ||
               nbtItem.hasKey(LIFE_STEAL) || nbtItem.hasKey(CRIT_CHANCE) || nbtItem.hasKey(CRIT_DAMAGE) ||
               nbtItem.hasKey(OMNIVAMP) || nbtItem.hasKey(MINING_FORTUNE) || nbtItem.hasKey(MINING_SPEED) ||
               nbtItem.hasKey(BUILD_RANGE) || nbtItem.hasKey(LURE_POTENCY) || nbtItem.hasKey(FISHING_FORTUNE) ||
               nbtItem.hasKey(FISHING_RESILIENCE) || nbtItem.hasKey(FISHING_FOCUS) || nbtItem.hasKey(FISHING_PRECISION) ||
               nbtItem.hasKey(SEA_MONSTER_AFFINITY) || nbtItem.hasKey(TREASURE_SENSE);
    }
}
