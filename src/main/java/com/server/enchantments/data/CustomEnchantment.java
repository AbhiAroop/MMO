package com.server.enchantments.data;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.elements.AffinityCategory;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;

/**
 * Base class for all custom enchantments
 */
public abstract class CustomEnchantment {
    
    protected final String id;
    protected final String displayName;
    protected final String description;
    protected final EnchantmentRarity rarity;
    protected final ElementType element;
    protected final HybridElement hybridElement;
    protected final boolean isHybrid;
    
    /**
     * Constructor for regular elemental enchantments
     */
    public CustomEnchantment(String id, String displayName, String description,
                            EnchantmentRarity rarity, ElementType element) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.element = element;
        this.hybridElement = null;
        this.isHybrid = false;
    }
    
    /**
     * Constructor for hybrid enchantments
     */
    public CustomEnchantment(String id, String displayName, String description,
                            EnchantmentRarity rarity, HybridElement hybridElement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.element = null;
        this.hybridElement = hybridElement;
        this.isHybrid = true;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public EnchantmentRarity getRarity() {
        return rarity;
    }
    
    public ElementType getElement() {
        return element;
    }
    
    public HybridElement getHybridElement() {
        return hybridElement;
    }
    
    public boolean isHybrid() {
        return isHybrid;
    }
    
    /**
     * Get the colored display name with element icon
     */
    public String getColoredName() {
        if (isHybrid) {
            return hybridElement.getColoredIcon() + " " + rarity.getColor() + displayName;
        } else {
            return element.getColoredIcon() + " " + rarity.getColor() + displayName;
        }
    }
    
    /**
     * Check if this enchantment can be applied to the given item
     */
    public abstract boolean canApplyTo(ItemStack item);
    
    /**
     * Get base effectiveness values before quality scaling
     * Each enchantment returns its own stat array
     */
    public abstract double[] getBaseStats();
    
    /**
     * Calculate scaled stats based on quality
     * @param quality The quality rating
     * @return Scaled stat values
     */
    public double[] getScaledStats(EnchantmentQuality quality) {
        double[] baseStats = getBaseStats();
        double multiplier = quality.getEffectivenessMultiplier();
        
        double[] scaledStats = new double[baseStats.length];
        for (int i = 0; i < baseStats.length; i++) {
            scaledStats[i] = baseStats[i] * multiplier;
        }
        return scaledStats;
    }
    
    /**
     * Calculate scaled stats based on quality and level
     * @param quality The quality rating
     * @param level The enchantment level
     * @return Scaled stat values
     */
    public double[] getScaledStats(EnchantmentQuality quality, EnchantmentLevel level) {
        double[] baseStats = getBaseStats();
        double qualityMultiplier = quality.getEffectivenessMultiplier();
        double levelMultiplier = level.getPowerMultiplier();
        double combinedMultiplier = qualityMultiplier * levelMultiplier;
        
        double[] scaledStats = new double[baseStats.length];
        for (int i = 0; i < baseStats.length; i++) {
            scaledStats[i] = baseStats[i] * combinedMultiplier;
        }
        return scaledStats;
    }
    
    /**
     * Trigger the enchantment effect
     * @param player The player with the enchantment
     * @param quality The quality rating
     * @param event The triggering event
     */
    public abstract void trigger(Player player, EnchantmentQuality quality, Event event);
    
    /**
     * Trigger the enchantment effect with level
     * @param player The player with the enchantment
     * @param quality The quality rating
     * @param level The enchantment level
     * @param event The triggering event
     */
    public abstract void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event);
    
    /**
     * Get the trigger type for this enchantment
     */
    public abstract TriggerType getTriggerType();
    
    /**
     * Get the anti-synergy group IDs this enchantment belongs to.
     * Enchantments in the same group cannot coexist on the same item.
     * When a new enchantment is added, any existing enchantment in the same group will be replaced.
     * 
     * Anti-Synergy Groups:
     * 1 = Fire Damage (Cinderwake, Stormfire, Embershade)
     * 2 = AOE/Chain Effects (Voltbrand, Deepcurrent, Stormfire, CelestialSurge)
     * 3 = Crowd Control (Burdened Stone, Decayroot, Dawnstrike)
     * 4 = Invisibility (Ashen Veil, Veilborn)
     * 5 = Defensive Response (Mistveil, Whispers, Radiant Grace)
     * 6 = Attack Speed (Arc Nexus)
     * 7 = Movement (GaleStep, MistborneTempest)
     * 8 = Sustain/Barriers (PureReflection, Terraheart)
     * 9 = On-Kill Effects (Hollow Edge, Ashen Veil)
     * 
     * @return Array of group IDs (empty array if no conflicts)
     */
    public int[] getAntiSynergyGroups() {
        return new int[0]; // Default: no conflicts
    }
    
    /**
     * Get human-readable names of enchantments this conflicts with.
     * This is used for display in the /enchant info command.
     * Override this method to provide specific conflict information.
     * 
     * @return Array of conflicting enchantment display names
     */
    public String[] getConflictingEnchantments() {
        return new String[0]; // Default: no conflicts
    }
    
    /**
     * Get the affinity category this enchantment primarily contributes to.
     * Most enchantments will boost one category, but some may boost multiple.
     * 
     * OFFENSE: Damage-dealing, debuffs, offensive procs
     * DEFENSE: Damage mitigation, shields, defensive procs
     * UTILITY: Movement, regeneration, resource management
     * 
     * @return Primary affinity category for this enchantment
     */
    public AffinityCategory getPrimaryAffinityCategory() {
        // Default based on trigger type
        switch (getTriggerType()) {
            case ON_HIT:
            case ON_ATTACK:
            case ON_KILL:
                return AffinityCategory.OFFENSE;
            case ON_DAMAGED:
            case ON_DEFEND:
                return AffinityCategory.DEFENSE;
            case PASSIVE:
            case PERIODIC:
            case ON_MOVE:
            case ON_JUMP:
                return AffinityCategory.UTILITY;
            default:
                return AffinityCategory.OFFENSE;
        }
    }
    
    /**
     * Get the offensive affinity contribution for this enchantment's element(s).
     * Override this to customize how much offensive affinity this enchantment provides.
     * 
     * Default: Returns 10 affinity if category is OFFENSE, 0 otherwise.
     * Most enchantments boost only one category, but some can boost multiple.
     * 
     * @return Offensive affinity value to add
     */
    public int getOffensiveAffinityContribution() {
        if (getPrimaryAffinityCategory() == AffinityCategory.OFFENSE) {
            // Default affinity contribution for offensive enchantments
            return 10;
        }
        return 0;
    }
    
    /**
     * Get the defensive affinity contribution for this enchantment's element(s).
     * Override this to customize how much defensive affinity this enchantment provides.
     * 
     * Default: Returns 10 affinity if category is DEFENSE, 0 otherwise.
     * Most enchantments boost only one category, but some can boost multiple.
     * 
     * @return Defensive affinity value to add
     */
    public int getDefensiveAffinityContribution() {
        if (getPrimaryAffinityCategory() == AffinityCategory.DEFENSE) {
            // Default affinity contribution for defensive enchantments
            return 10;
        }
        return 0;
    }
    
    /**
     * Get the utility affinity contribution for this enchantment's element(s).
     * Override this to customize how much utility affinity this enchantment provides.
     * 
     * Default: Returns 10 affinity if category is UTILITY, 0 otherwise.
     * Most enchantments boost only one category, but some can boost multiple.
     * 
     * @return Utility affinity value to add
     */
    public int getUtilityAffinityContribution() {
        if (getPrimaryAffinityCategory() == AffinityCategory.UTILITY) {
            // Default affinity contribution for utility enchantments
            return 10;
        }
        return 0;
    }
    
    /**
     * Trigger types for different enchantment activations
     */
    public enum TriggerType {
        ON_HIT,           // When the player hits an entity
        ON_DAMAGED,       // When the player takes damage
        ON_KILL,          // When the player kills an entity
        ON_BLOCK_BREAK,   // When the player breaks a block
        PASSIVE,          // Always active (stat bonuses)
        ON_ATTACK,        // When the player attacks (before damage)
        ON_DEFEND,        // When the player is attacked (before damage taken)
        ON_MOVE,          // When the player moves
        ON_JUMP,          // When the player jumps
        PERIODIC          // Triggers every X seconds
    }
}
