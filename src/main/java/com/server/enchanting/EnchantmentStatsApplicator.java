package com.server.enchanting;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.stats.PlayerStats;

/**
 * Applies enchantment bonuses to player stats
 */
public class EnchantmentStatsApplicator {
    
    /**
     * Apply all enchantment bonuses from equipped items to player stats
     */
    public static void applyEnchantmentBonuses(Player player, PlayerStats stats) {
        // Reset enchantment bonuses (they will be recalculated)
        resetEnchantmentBonuses(stats);
        
        // Apply bonuses from all equipped items
        applyItemEnchantmentBonuses(player.getInventory().getHelmet(), stats);
        applyItemEnchantmentBonuses(player.getInventory().getChestplate(), stats);
        applyItemEnchantmentBonuses(player.getInventory().getLeggings(), stats);
        applyItemEnchantmentBonuses(player.getInventory().getBoots(), stats);
        applyItemEnchantmentBonuses(player.getInventory().getItemInMainHand(), stats);
        applyItemEnchantmentBonuses(player.getInventory().getItemInOffHand(), stats);
    }
    
    /**
     * Apply enchantment bonuses from a single item
     */
    private static void applyItemEnchantmentBonuses(ItemStack item, PlayerStats stats) {
        if (item == null || !EnchantmentApplicator.hasCustomEnchantments(item)) {
            return;
        }
        
        Map<String, Integer> enchantments = EnchantmentApplicator.getCustomEnchantments(item);
        
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchantmentId = entry.getKey();
            int level = entry.getValue();
            
            CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
            if (enchantment != null) {
                applyEnchantmentBonus(enchantment, level, stats);
            }
        }
    }
    
    /**
     * Apply bonuses from a specific enchantment - ENHANCED: Percentage-based calculations
     */
    private static void applyEnchantmentBonus(CustomEnchantment enchantment, int level, PlayerStats stats) {
        String enchantmentId = enchantment.getId();
        
        switch (enchantmentId) {
            // Combat Enchantments
            case "savagery":
                stats.setPhysicalDamage(stats.getPhysicalDamage() + (3 * level));
                break;
            case "brutality":
                // NEW: Percentage-based physical damage increase
                int currentPhysicalDamage = stats.getPhysicalDamage();
                double percentIncrease = 10 * level; // 10% per level
                int bonusDamage = (int) Math.round(currentPhysicalDamage * (percentIncrease / 100.0));
                stats.setPhysicalDamage(currentPhysicalDamage + bonusDamage);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                        "BRUTALITY: Applied " + percentIncrease + "% bonus to " + currentPhysicalDamage + 
                        " physical damage = +" + bonusDamage + " (new total: " + stats.getPhysicalDamage() + ")");
                }
                break;
            case "executioner":
                stats.setCriticalChance(stats.getCriticalChance() + (5 * level / 100.0));
                stats.setCriticalDamage(stats.getCriticalDamage() + (10 * level / 100.0));
                break;
            case "spell_power":
                stats.setMagicDamage(stats.getMagicDamage() + (2 * level));
                break;
                
            // Tool Enchantments - FIXED: Use proper decimal values
            case "prospector":
                stats.setMiningFortune(stats.getMiningFortune() + (0.5 * level)); // 0.5 per level
                break;
            case "swiftbreak":
                stats.setMiningSpeed(stats.getMiningSpeed() + (0.2 * level)); // 0.2 per level
                break;
            case "cultivator":
                stats.setFarmingFortune(stats.getFarmingFortune() + (0.3 * level)); // 0.3 per level
                break;
            case "treasure_hunter":
                stats.setLootingFortune(stats.getLootingFortune() + (0.2 * level)); // 0.2 per level
                break;
            case "angler":
                stats.setFishingFortune(stats.getFishingFortune() + (0.3 * level)); // 0.3 per level
                break;
            case "architect":
                stats.setBuildRange(stats.getBuildRange() + (1.0 * level)); // 1.0 per level
                break;
                
            // Protection Enchantments
            case "fortification":
                stats.setArmor(stats.getArmor() + (3 * level));
                break;
            case "warding":
                stats.setMagicResist(stats.getMagicResist() + (5 * level));
                break;
            case "regeneration":
                stats.setHealthRegen(stats.getHealthRegen() + (0.5 * level)); // 0.5 per level
                break;
                
            // Utility Enchantments
            case "swift":
                stats.setSpeed(stats.getSpeed() + (0.01 * level)); // 0.01 per level for movement
                break;
            case "lucky":
                stats.setLuck(stats.getLuck() + level);
                break;
                
            // Mystical Enchantments
            case "arcane_power":
                stats.setTotalMana(stats.getTotalMana() + (10 * level));
                break;
            case "spell_focus":
                stats.setCooldownReduction(stats.getCooldownReduction() + (5 * level));
                break;
            case "arcane_mastery":
                stats.setTotalMana(stats.getTotalMana() + (5 * level));
                stats.setMagicDamage(stats.getMagicDamage() + (2 * level));
                stats.setCooldownReduction(stats.getCooldownReduction() + level);
                break;
                
            // Cursed Enchantments
            case "glass_cannon":
                stats.setPhysicalDamage((int)(stats.getPhysicalDamage() * (1.0 + 0.1 * level)));
                stats.setMagicDamage((int)(stats.getMagicDamage() * (1.0 + 0.1 * level)));
                stats.setHealth((int)(stats.getHealth() * (1.0 - 0.05 * level)));
                break;
            case "mana_burn":
                stats.setTotalMana(stats.getTotalMana() + (5 * level));
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Reset enchantment-specific bonuses (called before reapplying)
     */
    private static void resetEnchantmentBonuses(PlayerStats stats) {
        // This would reset only enchantment bonuses if we tracked them separately
        // For now, the StatScanManager handles this by resetting to base values
        // and then reapplying all bonuses including enchantments
    }
}