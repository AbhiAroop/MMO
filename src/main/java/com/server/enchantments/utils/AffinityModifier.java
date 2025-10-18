package com.server.enchantments.utils;

import org.bukkit.entity.Player;

import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.ElementalAffinity;

/**
 * Calculates damage and effect modifiers based on elemental affinity differences
 * between attacker and defender in PVP combat.
 * 
 * System Overview:
 * 1. Each player has affinity points per element from equipped enchanted items
 * 2. Relative Affinity = Attacker Affinity - Defender Affinity (for same element)
 * 3. Modifier scales using tanh function to soft-cap extreme values
 * 4. Applies to damage, burn duration, poison ticks, stun duration, proc chance, etc.
 */
public class AffinityModifier {
    
    /**
     * Scaling constant for affinity calculations.
     * Higher values = less sensitive to affinity differences.
     * Lower values = more sensitive to affinity differences.
     * 
     * Default: 50.0 means ~50 affinity points difference = ~65% of max modifier
     */
    private static final double SCALING_CONSTANT = 50.0;
    
    /**
     * Maximum damage bonus/reduction from affinity (as a percentage).
     * Example: 0.25 = 25% max bonus or reduction
     */
    private static final double MAX_DAMAGE_MODIFIER = 0.25;
    
    /**
     * Maximum effect duration modifier from affinity.
     * Example: 0.30 = 30% max bonus or reduction to effect durations
     */
    private static final double MAX_EFFECT_MODIFIER = 0.30;
    
    /**
     * Calculate damage modifier based on elemental affinity difference.
     * 
     * Formula: Modifier = MAX_DAMAGE_MODIFIER × tanh(RelativeAffinity / K)
     * Where:
     * - RelativeAffinity = Attacker Affinity - Defender Affinity
     * - K = SCALING_CONSTANT (soft-cap tuning)
     * - tanh ensures value between -1 and +1
     * 
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type of the attack
     * @return Damage multiplier (1.0 = no change, >1.0 = bonus, <1.0 = reduction)
     */
    public static double calculateDamageModifier(Player attacker, Player defender, ElementType element) {
        if (attacker == null || defender == null || element == null) {
            return 1.0; // No modifier
        }
        
        // Get player profiles
        ElementalAffinity attackerAffinity = getPlayerAffinity(attacker);
        ElementalAffinity defenderAffinity = getPlayerAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 1.0; // No modifier if can't get affinity
        }
        
        // Calculate relative affinity
        double attackerValue = attackerAffinity.getAffinity(element);
        double defenderValue = defenderAffinity.getAffinity(element);
        double relativeAffinity = attackerValue - defenderValue;
        
        // Apply tanh soft-cap function
        double normalizedModifier = Math.tanh(relativeAffinity / SCALING_CONSTANT);
        
        // Scale to max damage modifier and convert to multiplier
        double damageChange = normalizedModifier * MAX_DAMAGE_MODIFIER;
        
        // Return as multiplier (1.0 + change)
        // Example: +25% bonus = 1.25x, -25% reduction = 0.75x
        return 1.0 + damageChange;
    }
    
    /**
     * Calculate effect duration modifier based on elemental affinity difference.
     * Used for burn duration, poison ticks, stun duration, etc.
     * 
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type of the effect
     * @return Duration multiplier (1.0 = no change, >1.0 = longer, <1.0 = shorter)
     */
    public static double calculateEffectModifier(Player attacker, Player defender, ElementType element) {
        if (attacker == null || defender == null || element == null) {
            return 1.0;
        }
        
        ElementalAffinity attackerAffinity = getPlayerAffinity(attacker);
        ElementalAffinity defenderAffinity = getPlayerAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 1.0;
        }
        
        // Calculate relative affinity
        double relativeAffinity = attackerAffinity.getAffinity(element) - 
                                  defenderAffinity.getAffinity(element);
        
        // Apply tanh soft-cap
        double normalizedModifier = Math.tanh(relativeAffinity / SCALING_CONSTANT);
        
        // Scale to max effect modifier
        double effectChange = normalizedModifier * MAX_EFFECT_MODIFIER;
        
        return 1.0 + effectChange;
    }
    
    /**
     * Calculate proc chance modifier based on elemental affinity difference.
     * Used for enchantment trigger chances, critical hit bonuses, etc.
     * 
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type
     * @return Proc chance multiplier (1.0 = no change, >1.0 = higher chance, <1.0 = lower)
     */
    public static double calculateProcModifier(Player attacker, Player defender, ElementType element) {
        // Use same calculation as effect modifier
        return calculateEffectModifier(attacker, defender, element);
    }
    
    /**
     * Get raw affinity difference for an element.
     * Useful for UI display or custom calculations.
     * 
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type
     * @return Relative affinity (positive = advantage, negative = disadvantage)
     */
    public static double getRelativeAffinity(Player attacker, Player defender, ElementType element) {
        if (attacker == null || defender == null || element == null) {
            return 0.0;
        }
        
        ElementalAffinity attackerAffinity = getPlayerAffinity(attacker);
        ElementalAffinity defenderAffinity = getPlayerAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 0.0;
        }
        
        return attackerAffinity.getAffinity(element) - defenderAffinity.getAffinity(element);
    }
    
    /**
     * Get a player's elemental affinity tracker.
     * 
     * @param player The player
     * @return ElementalAffinity instance or null if not available
     */
    private static ElementalAffinity getPlayerAffinity(Player player) {
        if (player == null) return null;
        
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return null;
        
        PlayerProfile profile = ProfileManager.getInstance()
            .getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return null;
        
        return profile.getStats().getElementalAffinity();
    }
    
    /**
     * Check if affinity modifiers should apply to a combat interaction.
     * Only applies in PVP situations.
     * 
     * @param attacker The attacking entity
     * @param defender The defending entity
     * @return true if both are players (PVP)
     */
    public static boolean shouldApplyAffinityModifiers(Object attacker, Object defender) {
        return attacker instanceof Player && defender instanceof Player;
    }
    
    /**
     * Get a formatted description of the affinity advantage/disadvantage.
     * Useful for combat feedback messages.
     * 
     * @param modifier The damage modifier value
     * @return Formatted string describing the advantage
     */
    public static String getAffinityFeedback(double modifier) {
        if (modifier > 1.15) {
            return "§a⚡ Strong Elemental Advantage!";
        } else if (modifier > 1.05) {
            return "§a✦ Elemental Advantage";
        } else if (modifier < 0.85) {
            return "§c⚡ Strong Elemental Disadvantage!";
        } else if (modifier < 0.95) {
            return "§c✦ Elemental Disadvantage";
        } else {
            return "§7⚖ Neutral";
        }
    }
    
    /**
     * Apply affinity modifier to effect duration (burn, poison, stun, etc.)
     * Convenience method for enchantment implementations.
     * 
     * @param baseDuration Base duration in ticks
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type
     * @return Modified duration in ticks
     */
    public static int applyEffectDurationModifier(int baseDuration, Player attacker, Player defender, ElementType element) {
        double modifier = calculateEffectModifier(attacker, defender, element);
        return (int) Math.round(baseDuration * modifier);
    }
    
    /**
     * Apply affinity modifier to proc chance.
     * Convenience method for enchantment implementations.
     * 
     * @param baseChance Base chance (0.0 to 1.0)
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type
     * @return Modified chance (0.0 to 1.0, capped at 1.0)
     */
    public static double applyProcChanceModifier(double baseChance, Player attacker, Player defender, ElementType element) {
        double modifier = calculateProcModifier(attacker, defender, element);
        return Math.min(1.0, baseChance * modifier);
    }
}
