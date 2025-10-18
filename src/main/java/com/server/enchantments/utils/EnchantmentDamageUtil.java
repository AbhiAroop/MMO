package com.server.enchantments.utils;

import org.bukkit.entity.Player;

import com.server.Main;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

/**
 * Utility class for calculating and applying enchantment damage with proper damage types.
 * 
 * Damage Types:
 * - Physical Damage: Fire, Water, Earth, Air, Nature (reduced by Armor stat)
 * - Magical Damage: Lightning, Shadow, Light (reduced by Magic Resist stat)
 */
public class EnchantmentDamageUtil {
    
    /**
     * Determine if an element deals physical damage
     * Physical: Fire, Water, Earth, Air, Nature
     * Magical: Lightning, Shadow, Light
     */
    public static boolean isPhysicalDamage(ElementType element) {
        switch (element) {
            case FIRE:
            case WATER:
            case EARTH:
            case AIR:
            case NATURE:
                return true;
            case LIGHTNING:
            case SHADOW:
            case LIGHT:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Calculate damage after applying player defenses
     * @param rawDamage The raw damage before defense reduction
     * @param target The player receiving damage
     * @param element The element type of the damage
     * @return The final damage after defense reduction
     */
    public static double calculateReducedDamage(double rawDamage, Player target, ElementType element) {
        // Get target's profile and stats
        ProfileManager profileManager = Main.getInstance().getProfileManager();
        Integer activeSlot = profileManager.getActiveProfile(target.getUniqueId());
        
        if (activeSlot == null) {
            // No active profile means no defense stats, return raw damage
            return rawDamage;
        }
        
        PlayerProfile[] profiles = profileManager.getProfiles(target.getUniqueId());
        PlayerProfile profile = profiles[activeSlot];
        
        if (profile == null) {
            // No profile means no defense stats, return raw damage
            return rawDamage;
        }
        
        PlayerStats stats = profile.getStats();
        
        // Determine damage type and get appropriate defense stat
        boolean isPhysical = isPhysicalDamage(element);
        int defenseStat = isPhysical ? stats.getArmor() : stats.getMagicResist();
        
        // Apply damage reduction formula
        // Formula: finalDamage = rawDamage * (100 / (100 + defense))
        // This gives diminishing returns: 
        // 10 defense = 9.1% reduction
        // 50 defense = 33.3% reduction
        // 100 defense = 50% reduction
        // 200 defense = 66.7% reduction
        double damageMultiplier = 100.0 / (100.0 + defenseStat);
        double reducedDamage = rawDamage * damageMultiplier;
        
        return reducedDamage;
    }
    
    /**
     * Apply enchantment damage to a player target with proper damage type handling
     * 
     * NOTE: This method is DEPRECATED - use addBonusDamageToEvent() instead!
     * Direct health modification bypasses CombatListener's armor/magic resist calculations.
     * 
     * @param rawDamage The raw damage amount
     * @param target The player receiving damage
     * @param element The element type of the damage
     * @return The actual damage dealt after reductions
     * @deprecated Use addBonusDamageToEvent() to properly integrate with CombatListener
     */
    @Deprecated
    public static double applyEnchantmentDamage(double rawDamage, Player target, ElementType element) {
        // Calculate damage after defense reduction
        double finalDamage = calculateReducedDamage(rawDamage, target, element);
        
        // Get current health
        double currentHealth = target.getHealth();
        
        // Apply damage (don't let health go below 0)
        double newHealth = Math.max(0, currentHealth - finalDamage);
        target.setHealth(newHealth);
        
        return finalDamage;
    }
    
    /**
     * Add enchantment bonus damage to the damage event.
     * This allows CombatListener to properly apply armor/magic resist reductions.
     * 
     * For PVP: Applies affinity-based damage modifiers based on elemental affinity differences.
     * For PVE: Applies base damage without affinity modifiers.
     * 
     * @param event The damage event
     * @param bonusDamage The raw bonus damage to add (before affinity modifier)
     * @param element The element type (determines physical vs magical and affinity calculations)
     */
    public static void addBonusDamageToEvent(org.bukkit.event.entity.EntityDamageByEntityEvent event, 
                                              double bonusDamage, ElementType element) {
        // Mark the damager with element metadata so CombatListener knows the damage type
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            
            // Set temporary metadata with the element name
            // CombatListener will read this and determine if damage is physical or magical
            damager.setMetadata("enchantment_element", 
                new org.bukkit.metadata.FixedMetadataValue(Main.getInstance(), element.name()));
            
            // Apply affinity modifier in PVP situations
            if (event.getEntity() instanceof Player) {
                Player defender = (Player) event.getEntity();
                
                // Calculate affinity-based damage modifier
                double affinityModifier = AffinityModifier.calculateDamageModifier(damager, defender, element);
                
                // Apply the modifier to bonus damage
                bonusDamage *= affinityModifier;
                
                // Debug output
                if (Main.getInstance().isDebugEnabled(com.server.debug.DebugManager.DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(com.server.debug.DebugManager.DebugSystem.ENCHANTING,
                        String.format("PVP Affinity Modifier: %s vs %s, Element: %s, Modifier: %.2fx, Feedback: %s",
                            damager.getName(), defender.getName(), element.name(), 
                            affinityModifier, AffinityModifier.getAffinityFeedback(affinityModifier)));
                }
                
                // Send feedback to players if modifier is significant
                if (affinityModifier > 1.05 || affinityModifier < 0.95) {
                    String feedback = AffinityModifier.getAffinityFeedback(affinityModifier);
                    damager.sendMessage(feedback + " §8(" + element.getColoredIcon() + element.getColor() + element.name() + "§8)");
                }
            }
        }
        
        // Add the bonus damage to the event (after affinity modification)
        // CombatListener will handle armor/magic resist reduction automatically
        double currentDamage = event.getDamage();
        event.setDamage(currentDamage + bonusDamage);
        
        // Note: CombatListener's onPlayerDamaged() will apply:
        // - Armor reduction if physical damage (Fire, Water, Earth, Air, Nature)
        // - Magic Resist reduction if magical damage (Lightning, Shadow, Light)
    }
    
    /**
     * Get the damage type name for display purposes
     * @param element The element type
     * @return "Physical" or "Magical"
     */
    public static String getDamageTypeName(ElementType element) {
        return isPhysicalDamage(element) ? "Physical" : "Magical";
    }
    
    /**
     * Get the defense stat name that reduces this element's damage
     * @param element The element type
     * @return "Armor" or "Magic Resist"
     */
    public static String getDefenseStatName(ElementType element) {
        return isPhysicalDamage(element) ? "Armor" : "Magic Resist";
    }
    
    /**
     * Calculate damage reduction percentage for display
     * @param target The player whose defense to check
     * @param element The element type
     * @return Reduction percentage (0.0 to 1.0)
     */
    public static double getDamageReductionPercent(Player target, ElementType element) {
        ProfileManager profileManager = Main.getInstance().getProfileManager();
        Integer activeSlot = profileManager.getActiveProfile(target.getUniqueId());
        
        if (activeSlot == null) return 0.0;
        
        PlayerProfile[] profiles = profileManager.getProfiles(target.getUniqueId());
        PlayerProfile profile = profiles[activeSlot];
        
        if (profile == null) return 0.0;
        
        PlayerStats stats = profile.getStats();
        boolean isPhysical = isPhysicalDamage(element);
        int defenseStat = isPhysical ? stats.getArmor() : stats.getMagicResist();
        
        // Calculate reduction: 1 - (100 / (100 + defense))
        double reduction = 1.0 - (100.0 / (100.0 + defenseStat));
        return reduction;
    }
}
