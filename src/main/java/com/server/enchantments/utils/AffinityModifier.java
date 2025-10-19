package com.server.enchantments.utils;

import org.bukkit.entity.Player;

import com.server.enchantments.elements.CategorizedAffinity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Calculates damage and effect modifiers based on elemental affinity differences
 * between attacker and defender in PVP combat.
 * 
 * System Overview (NEW - Categorized Affinity):
 * 1. Each player has THREE affinity values per element:
 *    - Offensive: Affects outgoing damage and offensive effects
 *    - Defensive: Affects damage taken and defensive effects
 *    - Utility: Affects non-combat benefits (NOT used in combat)
 * 2. Relative Affinity = Attacker Offense - Defender Defense (for same element)
 * 3. Counter-Mechanic: If attacker's offensive element matches defender's defensive element,
 *    attacks are LESS EFFECTIVE (-20% penalty). Example: Fire offense vs Fire defense.
 * 4. Modifier scales using tanh function to soft-cap extreme values
 * 5. Applies to damage, burn duration, poison ticks, stun duration, proc chance, etc.
 * 
 * Counter-Mechanic Explanation:
 * - Fighting fire with fire is less effective
 * - If you specialize in Fire offense and your opponent specializes in Fire defense,
 *   your Fire attacks deal -20% damage against them
 * - This encourages build diversity and counter-play strategies
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
     * Counter-mechanic penalty when offensive element matches defensive element.
     * Example: -0.20 = 20% damage reduction when Fire offense attacks Fire defense
     */
    private static final double COUNTER_PENALTY = -0.20;
    
    /**
     * Calculate damage modifier based on elemental affinity difference with counter-mechanic.
     * 
     * NEW SYSTEM:
     * 1. Calculate relative affinity: Attacker Offense - Defender Defense
     * 2. Check for counter-mechanic: If elements match, apply penalty
     * 3. Apply tanh soft-cap to relative affinity
     * 4. Combine affinity modifier with counter penalty
     * 
     * Formula: 
     * - Base Modifier = MAX_DAMAGE_MODIFIER × tanh(RelativeAffinity / K)
     * - Counter Check = If attacker offense element == defender defense element
     * - Final Modifier = Base Modifier + Counter Penalty (if applicable)
     * 
     * @param attacker The attacking player
     * @param defender The defending entity (Player, NPC, or Mob)
     * @param element The element type of the attack (attacker's offensive element)
     * @return Damage multiplier (1.0 = no change, >1.0 = bonus, <1.0 = reduction)
     */
    public static double calculateDamageModifier(Player attacker, org.bukkit.entity.Entity defender, ElementType element) {
        if (attacker == null || defender == null || element == null) {
            return 1.0; // No modifier
        }
        
        // If defender is a player, use full affinity system
        if (defender instanceof Player) {
            return calculateDamageModifier(attacker, (Player) defender, element);
        }
        
        // For NPCs/Mobs: Use only attacker's affinity (no counter-mechanic)
        CategorizedAffinity attackerAffinity = getPlayerCategorizedAffinity(attacker);
        
        if (attackerAffinity == null) {
            return 1.0; // No modifier if can't get affinity
        }
        
        // Get offensive affinity value for this element
        double attackerOffense = attackerAffinity.getOffensive(element);
        
        // NPCs/Mobs have 0 defensive affinity
        double defenderDefense = 0.0;
        
        // Log affinity values for debugging
        com.server.Main.getInstance().getLogger().info(
            String.format("[AFFINITY] %s Offense(%s): %.1f | %s Defense(%s): %.1f (NPC/Mob)",
                attacker.getName(), element.name(), attackerOffense,
                defender.getName(), element.name(), defenderDefense));
        
        // Calculate relative affinity (offense vs 0 defense)
        double relativeAffinity = attackerOffense;
        
        // Apply tanh soft-cap function
        double normalizedModifier = Math.tanh(relativeAffinity / SCALING_CONSTANT);
        
        // Scale to max damage modifier
        double affinityModifier = normalizedModifier * MAX_DAMAGE_MODIFIER;
        
        // No counter-mechanic for NPCs/Mobs (they have no defensive affinity)
        // Return as multiplier (1.0 + affinity bonus)
        return 1.0 + affinityModifier;
    }
    
    /**
     * Calculate damage modifier for PVP combat (Player vs Player).
     * This version includes counter-mechanic checks.
     * 
     * @param attacker The attacking player
     * @param defender The defending player
     * @param element The element type of the attack (attacker's offensive element)
     * @return Damage multiplier (1.0 = no change, >1.0 = bonus, <1.0 = reduction)
     */
    public static double calculateDamageModifier(Player attacker, Player defender, ElementType element) {
        if (attacker == null || defender == null || element == null) {
            return 1.0; // No modifier
        }
        
        // Get player affinities
        CategorizedAffinity attackerAffinity = getPlayerCategorizedAffinity(attacker);
        CategorizedAffinity defenderAffinity = getPlayerCategorizedAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 1.0; // No modifier if can't get affinity
        }
        
        // Get offensive and defensive affinity values for this element
        double attackerOffense = attackerAffinity.getOffensive(element);
        double defenderDefense = defenderAffinity.getDefensive(element);
        
        // Log affinity values for debugging
        com.server.Main.getInstance().getLogger().info(
            String.format("[AFFINITY] %s Offense(%s): %.1f | %s Defense(%s): %.1f",
                attacker.getName(), element.name(), attackerOffense,
                defender.getName(), element.name(), defenderDefense));
        
        // Calculate relative affinity (offense vs defense)
        double relativeAffinity = attackerOffense - defenderDefense;
        
        // Apply tanh soft-cap function
        double normalizedModifier = Math.tanh(relativeAffinity / SCALING_CONSTANT);
        
        // Scale to max damage modifier
        double affinityModifier = normalizedModifier * MAX_DAMAGE_MODIFIER;
        
        // Check for counter-mechanic: Does defender have high defense in this element?
        // If attacker uses element X offense and defender has element X defense,
        // the attack is less effective (counter-mechanic)
        double counterModifier = 0.0;
        if (defenderDefense >= 20) { // Threshold: defender must have meaningful defense
            // Counter-mechanic applies: fighting fire with fire is less effective
            counterModifier = COUNTER_PENALTY;
        }
        
        // Combine affinity advantage/disadvantage with counter-mechanic
        double totalModifier = affinityModifier + counterModifier;
        
        // Return as multiplier (1.0 + total change)
        // Example: +25% affinity but -20% counter = 1.05x final
        // Example: -10% affinity and -20% counter = 0.70x final
        return 1.0 + totalModifier;
    }
    
    /**
     * Calculate effect duration modifier based on elemental affinity difference with counter-mechanic.
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
        
        CategorizedAffinity attackerAffinity = getPlayerCategorizedAffinity(attacker);
        CategorizedAffinity defenderAffinity = getPlayerCategorizedAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 1.0;
        }
        
        // Use offensive vs defensive affinity
        double attackerOffense = attackerAffinity.getOffensive(element);
        double defenderDefense = defenderAffinity.getDefensive(element);
        double relativeAffinity = attackerOffense - defenderDefense;
        
        // Apply tanh soft-cap
        double normalizedModifier = Math.tanh(relativeAffinity / SCALING_CONSTANT);
        
        // Scale to max effect modifier
        double affinityModifier = normalizedModifier * MAX_EFFECT_MODIFIER;
        
        // Apply counter-mechanic for effects too
        double counterModifier = 0.0;
        if (defenderDefense >= 20) {
            counterModifier = COUNTER_PENALTY; // Effects also less effective against matching defense
        }
        
        double totalModifier = affinityModifier + counterModifier;
        
        return 1.0 + totalModifier;
    }
    
    /**
     * Calculate proc chance modifier based on elemental affinity difference with counter-mechanic.
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
        
        CategorizedAffinity attackerAffinity = getPlayerCategorizedAffinity(attacker);
        CategorizedAffinity defenderAffinity = getPlayerCategorizedAffinity(defender);
        
        if (attackerAffinity == null || defenderAffinity == null) {
            return 0.0;
        }
        
        // Return offense vs defense comparison
        return attackerAffinity.getOffensive(element) - defenderAffinity.getDefensive(element);
    }
    
    /**
     * Get a player's categorized elemental affinity tracker.
     * 
     * @param player The player
     * @return CategorizedAffinity instance or null if not available
     */
    private static CategorizedAffinity getPlayerCategorizedAffinity(Player player) {
        if (player == null) return null;
        
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return null;
        
        PlayerProfile profile = ProfileManager.getInstance()
            .getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return null;
        
        // Get the new categorized affinity from profile
        // This will be stored in the profile's stats
        return profile.getStats().getCategorizedAffinity();
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
