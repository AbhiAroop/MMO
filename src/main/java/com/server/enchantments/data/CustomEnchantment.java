package com.server.enchantments.data;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

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
