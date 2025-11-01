package com.server.enchantments.abilities.utility;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;

/**
 * Swift Catch - Water Enchantment (Legendary, Passive)
 * Reduces the number of rounds required in the fishing minigame
 * 
 * FISHING ROD ONLY - Extremely powerful enchantment
 * 
 * Base Stats: [rounds_reduced]
 * Base: 1 round reduced
 * 
 * LEVEL SCALING (Rounds Reduced):
 *   Level I:   1 round (5 → 4 rounds)
 *   Level II:  2 rounds (5 → 3 rounds)
 *   Level III: 3 rounds (5 → 2 rounds)  
 *   Level IV:  4 rounds (5 → 1 round) - Max via Custom Anvil only
 * 
 * QUALITY SCALING (Effectiveness - rounds aren't fractionable):
 *   All qualities: Same effect (rounds can't be fractional)
 *   Quality affects altar drop rate instead
 * 
 * ALTAR RARITY: Legendary (VERY RARE - 0.5% base chance)
 * MAX LEVEL VIA ALTAR: I (Level IV only obtainable via Custom Anvil)
 * 
 * BALANCE NOTES:
 * - Assumes leftover rounds are perfectly completed
 * - Level IV is extremely powerful (1 round only!)
 * - Should be very hard to obtain and expensive to upgrade
 * 
 * Example: Swift Catch IV = Only 1 round needed (from 5 rounds!)
 */
public class SwiftCatch extends CustomEnchantment {
    
    public SwiftCatch() {
        super("swift_catch", 
              "Swift Catch", 
              "Reduces fishing minigame rounds",
              EnchantmentRarity.LEGENDARY,  // Very rare altar drop
              ElementType.WATER);
    }
    
    @Override
    public int getMaxLevel() {
        return 4; // Can go up to Level IV via Custom Anvil (altar gives Level I due to Legendary rarity)
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // FISHING ROD ONLY
        return type == Material.FISHING_ROD;
    }
    
    @Override
    public double[] getBaseStats() {
        // [rounds_reduced]
        // Level scaling is handled manually in the effect
        return new double[]{1.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        // This is a passive enchantment, no trigger needed
        // Effect is applied directly in fishing minigame via hasEnchantment check
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, com.server.enchantments.data.EnchantmentLevel level, Event event) {
        // This is a passive enchantment, no trigger needed
        // Effect is applied directly in fishing minigame via hasEnchantment check
    }
    
    @Override
    public TriggerType getTriggerType() {
        return TriggerType.PASSIVE; // No active trigger, effect applied in minigame
    }
    
    /**
     * Calculate rounds reduced based on enchantment level
     * @param level The enchantment level
     * @return Number of rounds to reduce (1-4)
     */
    public static int getRoundsReduced(com.server.enchantments.data.EnchantmentLevel level) {
        switch (level) {
            case I:
                return 1;
            case II:
                return 2;
            case III:
                return 3;
            case IV:
                return 4;
            default:
                return 1;
        }
    }
}
