package com.server.enchantments.utils;

import java.util.HashMap;
import java.util.Map;

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
    
    // Cooldown tracking for effectiveness messages (prevent spam)
    private static final Map<String, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 10000; // 10 seconds
    
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
            
            // Apply affinity modifier for ALL targets (PVP and PVE)
            String defenderName = (event.getEntity() instanceof Player) ? 
                ((Player) event.getEntity()).getName() : event.getEntity().getName();
            
            // Calculate affinity-based damage modifier
            double affinityModifier = AffinityModifier.calculateDamageModifier(
                damager, event.getEntity(), element);
            
            // Apply the modifier to bonus damage
            bonusDamage *= affinityModifier;
            
            // Debug output - ALWAYS log to help troubleshoot
            String targetType = (event.getEntity() instanceof Player) ? "PVP" : "PVE";
            Main.getInstance().getLogger().info(
                String.format("[AFFINITY] %s: %s vs %s | Element: %s | Modifier: %.3f | Bonus Dmg: %.2f",
                    targetType, damager.getName(), defenderName, element.name(), 
                    affinityModifier, bonusDamage));
            
            if (Main.getInstance().isDebugEnabled(com.server.debug.DebugManager.DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(com.server.debug.DebugManager.DebugSystem.ENCHANTING,
                    String.format("Affinity Modifier: %s vs %s, Element: %s, Modifier: %.2fx, Feedback: %s",
                        damager.getName(), defenderName, element.name(), 
                        affinityModifier, AffinityModifier.getAffinityFeedback(affinityModifier)));
            }
            
            // Send effectiveness feedback to attacker (works for both PVP and PVE)
            sendEffectivenessFeedback(damager, event.getEntity(), element, affinityModifier);
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
    
    /**
     * Send effectiveness feedback messages to attacker.
     * Only sends messages if the modifier is significantly different from neutral (1.0).
     * Messages are rate-limited to once every 10 seconds per player-element combination.
     * For PVP: Both attacker and defender get messages.
     * For PVE: Only attacker gets messages.
     * 
     * Effectiveness Levels:
     * - Super Effective: > 1.15 (advantage)
     * - Effective: 1.05 - 1.15 (slight advantage)
     * - Neutral: 0.95 - 1.05 (no message sent)
     * - Not Very Effective: 0.85 - 0.95 (slight disadvantage)
     * - Ineffective: < 0.85 (strong disadvantage)
     * 
     * @param attacker The attacking player
     * @param defender The defending entity (Player, NPC, or Mob)
     * @param element The element used in the attack
     * @param modifier The damage modifier (1.0 = neutral)
     */
    private static void sendEffectivenessFeedback(Player attacker, org.bukkit.entity.Entity defender, 
                                                   ElementType element, double modifier) {
        // Log for debugging
        String defenderName = defender != null ? 
            ((defender instanceof Player) ? ((Player) defender).getName() : defender.getName()) : "Unknown";
        Main.getInstance().getLogger().info(
            String.format("[FEEDBACK] Called: %s vs %s | Element: %s | Modifier: %.3f | In Range: %s",
                attacker.getName(), defenderName, element.name(), modifier,
                (modifier >= 0.95 && modifier <= 1.05)));
        
        // Don't send message if modifier is neutral
        if (modifier >= 0.95 && modifier <= 1.05) {
            Main.getInstance().getLogger().info("[FEEDBACK] Suppressed (neutral range)");
            return;
        }
        
        // Check cooldown to prevent message spam (10 second cooldown per player-element)
        String cooldownKey = attacker.getUniqueId().toString() + ":" + element.name();
        long currentTime = System.currentTimeMillis();
        
        if (messageCooldowns.containsKey(cooldownKey)) {
            long lastMessageTime = messageCooldowns.get(cooldownKey);
            long timeSinceLastMessage = currentTime - lastMessageTime;
            
            if (timeSinceLastMessage < MESSAGE_COOLDOWN_MS) {
                // Still on cooldown, don't send message
                Main.getInstance().getLogger().info(
                    String.format("[FEEDBACK] Suppressed (cooldown: %.1fs remaining)",
                        (MESSAGE_COOLDOWN_MS - timeSinceLastMessage) / 1000.0));
                return;
            }
        }
        
        // Update cooldown timestamp
        messageCooldowns.put(cooldownKey, currentTime);
        
        String elementDisplay = element.getColoredIcon() + " " + element.getColor() + element.name();
        
        // Messages for attacker (always sent)
        if (modifier > 1.15) {
            // Super Effective
            attacker.sendMessage(org.bukkit.ChatColor.GREEN + "⚡ Super Effective! " + 
                               org.bukkit.ChatColor.GRAY + "(" + elementDisplay + org.bukkit.ChatColor.GRAY + ")");
        } else if (modifier > 1.05) {
            // Effective
            attacker.sendMessage(org.bukkit.ChatColor.GREEN + "✓ It's effective! " + 
                               org.bukkit.ChatColor.GRAY + "(" + elementDisplay + org.bukkit.ChatColor.GRAY + ")");
        } else if (modifier < 0.85) {
            // Ineffective
            attacker.sendMessage(org.bukkit.ChatColor.RED + "✗ It's ineffective... " + 
                               org.bukkit.ChatColor.GRAY + "(" + elementDisplay + org.bukkit.ChatColor.GRAY + ")");
        } else {
            // Not Very Effective
            attacker.sendMessage(org.bukkit.ChatColor.YELLOW + "⚠ Not very effective " + 
                               org.bukkit.ChatColor.GRAY + "(" + elementDisplay + org.bukkit.ChatColor.GRAY + ")");
        }
        
        // Messages for defender (only if they're a player)
        if (defender instanceof Player) {
            Player defenderPlayer = (Player) defender;
            
            if (modifier > 1.15) {
                // Attacker has super advantage = defender is vulnerable
                defenderPlayer.sendMessage(org.bukkit.ChatColor.RED + "⚠ You're vulnerable to " + elementDisplay + "!");
            } else if (modifier > 1.05) {
                // Attacker has advantage = defender is weak
                defenderPlayer.sendMessage(org.bukkit.ChatColor.YELLOW + "⚠ Weak to " + elementDisplay);
            } else if (modifier < 0.85) {
                // Attacker is ineffective = defender resists strongly
                defenderPlayer.sendMessage(org.bukkit.ChatColor.GREEN + "✓ Strong resistance to " + elementDisplay + "!");
            } else {
                // Attacker is not very effective = defender resists
                defenderPlayer.sendMessage(org.bukkit.ChatColor.GREEN + "✓ Resisting " + elementDisplay);
            }
        }
    }
}
