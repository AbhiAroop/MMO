package com.server.profiles.skills.skills.fishing.subskills;

import org.bukkit.entity.Player;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SubskillType;

/**
 * Rod Fishing Subskill - Timing-based minigame fishing with stat progression
 * 
 * Features:
 * - Timing minigame with moving indicator and catch zones
 * - Multiple rounds with increasing difficulty
 * - Stat system (Line Strength, Flexibility, Reel Speed, Treasure Chance)
 * - Combo system with XP/loot multipliers
 * - Perfect timing bonuses and zero-mistake rewards
 * - Four fishing types: Normal Water, Ice, Lava, Void
 * - Loot categories: Fish (rarity/size/quality), Hostile Mobs, Treasures/Junk
 */
public class RodFishingSubskill extends AbstractSkill {
    
    // Stat multipliers for level scaling
    private static final double LINE_STRENGTH_PER_LEVEL = 0.5; // Widens catch zone
    private static final double FLEXIBILITY_PER_LEVEL = 0.3; // Reduces indicator speed
    private static final double REEL_SPEED_PER_LEVEL = 0.4; // Reduces rounds needed
    private static final double TREASURE_CHANCE_PER_LEVEL = 0.2; // Increases treasure chance
    
    public RodFishingSubskill(Skill parentSkill) {
        super(SubskillType.ROD_FISHING.getId(), 
              SubskillType.ROD_FISHING.getDisplayName(), 
              SubskillType.ROD_FISHING.getDescription(), 
              100,
              parentSkill);
    }
    
    /**
     * Get the player's Line Strength stat
     * Determines catch zone width (wider = easier)
     */
    public double getLineStrength(Player player) {
        int level = getSkillLevel(player).getLevel();
        double base = 10.0; // Base catch zone width (percentage)
        return base + (level * LINE_STRENGTH_PER_LEVEL);
    }
    
    /**
     * Get the player's Flexibility stat
     * Determines indicator movement speed (higher = slower indicator)
     */
    public double getFlexibility(Player player) {
        int level = getSkillLevel(player).getLevel();
        double base = 5.0; // Base flexibility
        return base + (level * FLEXIBILITY_PER_LEVEL);
    }
    
    /**
     * Get the player's Reel Speed stat
     * Reduces number of rounds required to catch (max reduction of 2 rounds)
     */
    public int getReelSpeed(Player player) {
        int level = getSkillLevel(player).getLevel();
        double reduction = level * REEL_SPEED_PER_LEVEL;
        return (int) Math.min(2, reduction / 20); // Max 2 rounds reduction at level 100
    }
    
    /**
     * Get the player's Treasure Chance stat
     * Increases chance of getting treasure instead of junk
     */
    public double getTreasureChance(Player player) {
        int level = getSkillLevel(player).getLevel();
        double base = 5.0; // Base 5% treasure chance
        return base + (level * TREASURE_CHANCE_PER_LEVEL);
    }
    
    /**
     * Calculate indicator speed based on flexibility
     * Returns blocks per tick the indicator moves
     */
    public double getIndicatorSpeed(Player player) {
        double flexibility = getFlexibility(player);
        double baseSpeed = 2.0; // Base speed (blocks per tick)
        // Higher flexibility = slower speed
        return baseSpeed / (1 + (flexibility / 100));
    }
    
    /**
     * Calculate catch zone size based on line strength
     * Returns percentage of boss bar covered by catch zone
     */
    public double getCatchZoneSize(Player player) {
        double lineStrength = getLineStrength(player);
        // Catch zone is 10-30% of boss bar
        return Math.min(30.0, lineStrength);
    }
    
    /**
     * Calculate number of rounds required for a catch
     * Base is 5 rounds, reduced by reel speed
     */
    public int getRequiredRounds(Player player) {
        int baseRounds = 5;
        int reelSpeedReduction = getReelSpeed(player);
        return Math.max(3, baseRounds - reelSpeedReduction); // Minimum 3 rounds
    }
}
