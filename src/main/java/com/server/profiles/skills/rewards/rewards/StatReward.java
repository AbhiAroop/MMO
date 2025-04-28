package com.server.profiles.skills.rewards.rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;
import com.server.profiles.stats.PlayerStats;

/**
 * A reward that boosts a player's stats
 */
public class StatReward extends SkillReward {
    private final String statName;
    private final double amount;
    
    public StatReward(String statName, double amount) {
        super(SkillRewardType.STAT_BOOST, "+" + amount + " " + formatStatName(statName));
        this.statName = statName;
        this.amount = amount;
    }
    
    @Override
    public void grantTo(Player player) {
        // Get the player's active profile
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return;
        
        // Get the player's stats
        PlayerStats stats = profile.getStats();
        
        // Apply the stat boost based on the stat name
        switch (statName.toLowerCase()) {
            case SkillRewardType.HEALTH:
                stats.setHealth(stats.getHealth() + (int)amount);
                break;
            case SkillRewardType.ARMOR:
                stats.setArmor(stats.getArmor() + (int)amount);
                break;
            case SkillRewardType.MAGIC_RESIST:
                stats.setMagicResist(stats.getMagicResist() + (int)amount);
                break;
            case SkillRewardType.PHYSICAL_DAMAGE:
                stats.setPhysicalDamage(stats.getPhysicalDamage() + (int)amount);
                break;
            case SkillRewardType.MAGIC_DAMAGE:
                stats.setMagicDamage(stats.getMagicDamage() + (int)amount);
                break;
            case SkillRewardType.MANA:
                stats.setTotalMana(stats.getTotalMana() + (int)amount);
                break;
            case SkillRewardType.SPEED:
                stats.setSpeed(stats.getSpeed() + amount);
                break;
            case SkillRewardType.CRITICAL_DAMAGE:
                stats.setCriticalDamage(stats.getCriticalDamage() + amount);
                break;
            case SkillRewardType.CRITICAL_CHANCE:
                stats.setCriticalChance(stats.getCriticalChance() + amount);
                break;
            case SkillRewardType.COOLDOWN_REDUCTION:
                stats.setCooldownReduction(stats.getCooldownReduction() + (int)amount);
                break;
            case SkillRewardType.LIFE_STEAL:
                stats.setLifeSteal(stats.getLifeSteal() + amount);
                break;
                
            // Fortune stats
            case SkillRewardType.MINING_FORTUNE:
                // Store the exact value without any rounding by using Java's BigDecimal for precision
                double currentMiningFortune = stats.getMiningFortune();
                // Use BigDecimal for precise calculation
                java.math.BigDecimal preciseValue = new java.math.BigDecimal(Double.toString(currentMiningFortune))
                    .add(new java.math.BigDecimal(Double.toString(amount)));
                // Convert back to double at the last moment to maintain precision
                stats.setMiningFortune(preciseValue.doubleValue());
                break;
                
            case SkillRewardType.FARMING_FORTUNE:
                double currentFarmingFortune = stats.getFarmingFortune();
                java.math.BigDecimal preciseFarmingValue = new java.math.BigDecimal(Double.toString(currentFarmingFortune))
                    .add(new java.math.BigDecimal(Double.toString(amount)));
                stats.setFarmingFortune(preciseFarmingValue.doubleValue());
                break;
                
            case SkillRewardType.LOOTING_FORTUNE:
                double currentLootingFortune = stats.getLootingFortune();
                java.math.BigDecimal preciseLootingValue = new java.math.BigDecimal(Double.toString(currentLootingFortune))
                    .add(new java.math.BigDecimal(Double.toString(amount)));
                stats.setLootingFortune(preciseLootingValue.doubleValue());
                break;
                
            case SkillRewardType.FISHING_FORTUNE:
                double currentFishingFortune = stats.getFishingFortune();
                java.math.BigDecimal preciseFishingValue = new java.math.BigDecimal(Double.toString(currentFishingFortune))
                    .add(new java.math.BigDecimal(Double.toString(amount)));
                stats.setFishingFortune(preciseFishingValue.doubleValue());
                break;
                
            // Other stats
            case SkillRewardType.LUCK:
                stats.setLuck(stats.getLuck() + (int)amount);
                break;
                
            default:
                // Unknown stat
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Unknown stat: " + statName);
                }
                return;
        }
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "Skill Reward: " + getDescription());
        
        // Apply the changes to the player
        stats.applyToPlayer(player);
    }
    
    /**
     * Format a stat name for display
     */
    private static String formatStatName(String statName) {
        switch (statName.toLowerCase()) {
            case SkillRewardType.HEALTH:
                return "Health";
            case SkillRewardType.ARMOR:
                return "Armor";
            case SkillRewardType.MAGIC_RESIST:
                return "Magic Resist";
            case SkillRewardType.PHYSICAL_DAMAGE:
                return "Physical Damage";
            case SkillRewardType.MAGIC_DAMAGE:
                return "Magic Damage";
            case SkillRewardType.MANA:
                return "Mana";
            case SkillRewardType.SPEED:
                return "Speed";
            case SkillRewardType.CRITICAL_DAMAGE:
                return "Critical Damage";
            case SkillRewardType.CRITICAL_CHANCE:
                return "Critical Chance";
            case SkillRewardType.COOLDOWN_REDUCTION:
                return "Cooldown Reduction";
            case SkillRewardType.LIFE_STEAL:
                return "Life Steal";
                
            // Fortune stats
            case SkillRewardType.MINING_FORTUNE:
                return "Mining Fortune";
            case SkillRewardType.FARMING_FORTUNE:
                return "Farming Fortune";
            case SkillRewardType.LOOTING_FORTUNE:
                return "Looting Fortune";
            case SkillRewardType.FISHING_FORTUNE:
                return "Fishing Fortune";
                
            // Other stats
            case SkillRewardType.LUCK:
                return "Luck";
                
            default:
                return capitalizeWords(statName.replace('_', ' '));
        }
    }
    
    /**
     * Capitalize each word in a string
     */
    private static String capitalizeWords(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}