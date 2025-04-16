package com.server.profiles.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class StatsUpdateManager {
    private static final long UPDATE_INTERVAL = 5L; // Update every 5 ticks (1/4 second)
    private final Main plugin;
    private final Map<UUID, BukkitTask> playerTrackingTasks = new HashMap<>();
    
    // Patterns for extracting stat values from lore - updated to handle color codes (§)
    private static final Pattern PHYSICAL_DAMAGE_PATTERN = Pattern.compile("Physical Damage: .*?\\+(\\d+)");
    private static final Pattern MAGIC_DAMAGE_PATTERN = Pattern.compile("Magic Damage: .*?\\+(\\d+)");
    private static final Pattern MANA_PATTERN = Pattern.compile("Mana: .*?\\+(\\d+)");
    private static final Pattern ARMOR_PATTERN = Pattern.compile("Armor: .*?\\+(\\d+)");
    private static final Pattern MAGIC_RESIST_PATTERN = Pattern.compile("Magic Resist: .*?\\+(\\d+)");
    private static final Pattern COOLDOWN_REDUCTION_PATTERN = Pattern.compile("Cooldown Reduction: .*?\\+(\\d+)%");
    private static final Pattern ATTACK_SPEED_PATTERN = Pattern.compile("Attack Speed: .*?\\+(\\d+\\.?\\d*)");
    private static final Pattern ATTACK_RANGE_PATTERN = Pattern.compile("Attack Range: .*?\\+(\\d+\\.?\\d*)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("Size: .*?\\+(\\d+\\.?\\d*)");
    private static final Pattern HEALTH_PATTERN = Pattern.compile("Health: .*?\\+(\\d+)");

    // Attribute modifier name constants for proper tracking and removal
    private static final String MMO_HEALTH_MODIFIER = "mmo.health";
    private static final String MMO_SIZE_MODIFIER = "mmo.size";
    private static final String MMO_ATTACK_RANGE_MODIFIER = "mmo.attack_range_override";
    private static final String MMO_ATTACK_SPEED_MODIFIER = "mmo.attackspeed.override";

    public StatsUpdateManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Start tracking and updating a player's stats
     * @param player The player to track
     */
    public void startTracking(Player player) {
        stopTracking(player); // Stop any existing tracking first
        
        // Initial stats update
        updatePlayerStats(player);
        
        // Schedule regular updates
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    playerTrackingTasks.remove(player.getUniqueId());
                    return;
                }
                
                // Check if player still has an active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile == null) return;
                
                updatePlayerStats(player);
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
        
        // Store the task reference
        playerTrackingTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop tracking a player's stats
     * @param player The player to stop tracking
     */
    public void stopTracking(Player player) {
        BukkitTask task = playerTrackingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        
        // Restore vanilla attributes when stopping tracking
        resetVanillaAttributes(player);
    }

    /**
     * Reset player's attributes to vanilla defaults when they quit or unload
     */
    public void resetVanillaAttributes(Player player) {
        try {
            // Reset health
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttribute != null) {
                removeAttributeModifiersByName(maxHealthAttribute, MMO_HEALTH_MODIFIER);
                maxHealthAttribute.setBaseValue(20.0); // Vanilla default
            }
            
            // Reset attack speed
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                removeAttributeModifiersByName(attackSpeedAttribute, MMO_ATTACK_SPEED_MODIFIER);
                attackSpeedAttribute.setBaseValue(4.0); // Vanilla default
            }
            
            // Reset scale (size)
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                removeAttributeModifiersByName(scaleAttribute, MMO_SIZE_MODIFIER);
                scaleAttribute.setBaseValue(1.0); // Vanilla default
            }
            
            // Reset attack range
            try {
                AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
                if (attackRangeAttribute != null) {
                    removeAttributeModifiersByName(attackRangeAttribute, MMO_ATTACK_RANGE_MODIFIER);
                    attackRangeAttribute.setBaseValue(3.0); // Vanilla default
                }
            } catch (Exception e) {
                // Ignore - attribute may not exist in all versions
            }
            
            plugin.getLogger().info("Reset vanilla attributes for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting vanilla attributes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to remove attribute modifiers by name
     */
    private void removeAttributeModifiersByName(AttributeInstance attribute, String modifierName) {
        Set<AttributeModifier> modifiersToRemove = new HashSet<>();
        
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getName().equals(modifierName)) {
                modifiersToRemove.add(modifier);
            }
        }
        
        for (AttributeModifier modifier : modifiersToRemove) {
            attribute.removeModifier(modifier);
        }
    }

    /**
     * Update a player's stats based on their equipment and profile
     */
    public void updatePlayerStats(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Get the stats object to update
        PlayerStats stats = profile.getStats();
        
        // Save current state before any changes
        saveCurrentState(player, stats);
        
        // Reset to defaults first
        resetStatsToDefaults(stats);
        
        // Apply equipment bonuses
        applyEquipmentBonuses(player, stats);
        
        // Apply attributes to player
        applyAttributesToPlayer(player, stats);
        
        // Restore player state properly
        restorePlayerState(player, stats);
    }
    
    /**
     * Store current player state before updating stats
     */
    private PlayerState saveCurrentState(Player player, PlayerStats stats) {
        PlayerState state = new PlayerState();
        
        // Get current health percentage instead of absolute value
        try {
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            state.healthPercentage = player.getHealth() / maxHealth;
        } catch (Exception e) {
            state.healthPercentage = 1.0;
        }
        
        // Store current mana
        state.mana = stats.getMana();
        if (stats.getTotalMana() > 0) {
            state.manaPercentage = (double) state.mana / stats.getTotalMana();
        }
        
        return state;
    }
    
    /**
     * Reset stats to defaults before applying equipment bonuses
     */
    private void resetStatsToDefaults(PlayerStats stats) {
        // Store permanent stat bonuses first if needed
        // For now we're not tracking those, but this is where you'd store them
        
        // Reset all stats to default values
        stats.resetToDefaults();
    }
    
    /**
     * Apply equipment (armor and weapon) bonuses to stats
     */
    private void applyEquipmentBonuses(Player player, PlayerStats stats) {
        // Track total bonuses from equipment
        EquipmentBonuses bonuses = new EquipmentBonuses();
        
        // Scan armor for bonuses
        extractArmorBonuses(player, bonuses);
        
        // Apply all gathered bonuses
        applyBonusesToStats(stats, bonuses);
        
        // Log for debugging
        logBonuses(player, bonuses);
    }
    
    /**
     * Extract stat bonuses from all armor pieces
     */
    private void extractArmorBonuses(Player player, EquipmentBonuses bonuses) {
        // Check each armor slot
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || !armor.hasItemMeta() || !armor.getItemMeta().hasLore()) continue;
            
            for (String loreLine : armor.getItemMeta().getLore()) {
                // Strip color codes for regex matching
                String cleanLine = stripColorCodes(loreLine);
                
                // Parse stats from lore using our patterns
                bonuses.armor += extractStat(cleanLine, ARMOR_PATTERN);
                bonuses.magicResist += extractStat(cleanLine, MAGIC_RESIST_PATTERN);
                bonuses.physicalDamage += extractStat(cleanLine, PHYSICAL_DAMAGE_PATTERN);
                bonuses.magicDamage += extractStat(cleanLine, MAGIC_DAMAGE_PATTERN);
                bonuses.mana += extractStat(cleanLine, MANA_PATTERN);
                bonuses.health += extractStat(cleanLine, HEALTH_PATTERN);
                bonuses.cooldownReduction += extractStat(cleanLine, COOLDOWN_REDUCTION_PATTERN);
                bonuses.attackSpeed += extractDoubleStat(cleanLine, ATTACK_SPEED_PATTERN);
                bonuses.attackRange += extractDoubleStat(cleanLine, ATTACK_RANGE_PATTERN);
                bonuses.size += extractDoubleStat(cleanLine, SIZE_PATTERN);
            }
        }
    }
    
    /**
     * Apply extracted bonuses to the player's stats
     */
    private void applyBonusesToStats(PlayerStats stats, EquipmentBonuses bonuses) {
        // Apply bonuses to base stats
        stats.setArmor(stats.getDefaultArmor() + bonuses.armor);
        stats.setMagicResist(stats.getDefaultMagicResist() + bonuses.magicResist);
        stats.setPhysicalDamage(stats.getDefaultPhysicalDamage() + bonuses.physicalDamage);
        stats.setMagicDamage(stats.getDefaultMagicDamage() + bonuses.magicDamage);
        stats.setTotalMana(stats.getDefaultMana() + bonuses.mana);
        stats.setHealth(stats.getDefaultHealth() + bonuses.health);
        stats.setCooldownReduction(stats.getDefaultCooldownReduction() + bonuses.cooldownReduction);
        stats.setAttackSpeed(stats.getDefaultAttackSpeed() + bonuses.attackSpeed);
        stats.setAttackRange(stats.getDefaultAttackRange() + bonuses.attackRange);
        stats.setSize(stats.getDefaultSize() + bonuses.size);
    }
    
    /**
     * Apply all stat values to player's Minecraft attributes
     */
    private void applyAttributesToPlayer(Player player, PlayerStats stats) {
        // Apply health via attribute modifier
        applyHealthAttribute(player, stats.getHealth());
        
        // Apply size (scale) via attribute modifier
        applySizeAttribute(player, stats.getSize());
        
        // Apply attack range
        applyAttackRangeAttribute(player, stats.getAttackRange());
        
        // Apply attack speed
        applyAttackSpeedAttribute(player, stats.getAttackSpeed());
        
        // Apply other non-attribute stats
        stats.applyToPlayer(player);

        // Ensure health display is always 10 hearts
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
    }
    
    /**
     * Apply correct health attribute with modifiers
     */
    private void applyHealthAttribute(Player player, int health) {
        try {
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttribute != null) {
                // Remove existing custom modifiers
                removeAttributeModifiersByName(maxHealthAttribute, MMO_HEALTH_MODIFIER);
                
                // Base value should be the vanilla default (20.0)
                maxHealthAttribute.setBaseValue(20.0);
                
                // Calculate the health bonus over vanilla default
                double healthBonus = health - 20.0;
                
                // Add modifier if needed
                if (healthBonus != 0) {
                    AttributeModifier healthModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_HEALTH_MODIFIER,
                        healthBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    maxHealthAttribute.addModifier(healthModifier);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying health attribute: " + e.getMessage());
        }
    }
    
    /**
     * Apply size (scale) attribute with modifiers
     */
    private void applySizeAttribute(Player player, double size) {
        try {
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                // Remove existing custom modifiers
                removeAttributeModifiersByName(scaleAttribute, MMO_SIZE_MODIFIER);
                
                // Base value should be the vanilla default (1.0)
                scaleAttribute.setBaseValue(1.0);
                
                // Calculate the size bonus over vanilla default
                double sizeBonus = size - 1.0;
                
                // Add modifier if needed
                if (sizeBonus != 0) {
                    AttributeModifier sizeModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_SIZE_MODIFIER,
                        sizeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttribute.addModifier(sizeModifier);
                }
            }
        } catch (Exception e) {
            // Scale attribute might not be available in older versions
            plugin.getLogger().fine("Scale attribute not supported in this version");
        }
    }
    
    /**
     * Apply attack range attribute with modifiers
     */
    private void applyAttackRangeAttribute(Player player, double attackRange) {
        try {
            // Check if the attribute exists (only in 1.20.5+)
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                // Remove existing custom modifiers
                removeAttributeModifiersByName(attackRangeAttribute, MMO_ATTACK_RANGE_MODIFIER);
                
                // Base value should be the vanilla default (3.0)
                attackRangeAttribute.setBaseValue(3.0);
                
                // Calculate bonus over default
                double rangeBonus = attackRange - 3.0;
                
                // Add modifier if needed
                if (rangeBonus > 0) {
                    AttributeModifier rangeModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_ATTACK_RANGE_MODIFIER,
                        rangeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackRangeAttribute.addModifier(rangeModifier);
                }
            }
        } catch (Exception e) {
            // This will catch errors if the attribute doesn't exist in older versions
            plugin.getLogger().fine("Entity interaction range attribute not supported in this version");
        }
    }
    
    /**
     * Apply attack speed attribute with modifiers
     */
    private void applyAttackSpeedAttribute(Player player, double attackSpeed) {
        try {
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                // Remove existing custom modifiers
                removeAttributeModifiersByName(attackSpeedAttribute, MMO_ATTACK_SPEED_MODIFIER);
                
                // Base value should be the vanilla default (4.0)
                attackSpeedAttribute.setBaseValue(4.0);
                
                // Calculate bonus over default
                double speedBonus = attackSpeed - 4.0;
                
                // Add modifier if needed
                if (speedBonus != 0) {
                    AttributeModifier speedModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_ATTACK_SPEED_MODIFIER,
                        speedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackSpeedAttribute.addModifier(speedModifier);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying attack speed attribute: " + e.getMessage());
        }
    }
    
    /**
     * Restore player's state (health, mana, etc.) after applying stats
     */
    private void restorePlayerState(Player player, PlayerStats stats) {
        try {
            // Calculate current max health after all attribute changes
            double newMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            
            // Restore health by percentage, not absolute value
            PlayerState state = saveCurrentState(player, stats);
            double newHealth = state.healthPercentage * newMaxHealth;
            player.setHealth(Math.max(1.0, newHealth));
            
            // Restore mana percentage
            if (stats.getTotalMana() > 0 && state.manaPercentage > 0) {
                int newMana = (int) (state.manaPercentage * stats.getTotalMana());
                stats.setMana(Math.max(1, Math.min(newMana, stats.getTotalMana())));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error restoring player state: " + e.getMessage());
        }
    }
    
    /**
     * Log equipment bonuses to debug
     */
    private void logBonuses(Player player, EquipmentBonuses bonuses) {
        // Only log when debug mode is enabled
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Player " + player.getName() + " equipment bonuses: " +
                                "Health: +" + bonuses.health +
                                ", Armor: +" + bonuses.armor + 
                                ", Magic Resist: +" + bonuses.magicResist +
                                ", Phys Dmg: +" + bonuses.physicalDamage +
                                ", Magic Dmg: +" + bonuses.magicDamage +
                                ", Mana: +" + bonuses.mana +
                                ", CDR: +" + bonuses.cooldownReduction + 
                                ", Attack Speed: +" + bonuses.attackSpeed +
                                ", Attack Range: +" + bonuses.attackRange +
                                ", Size: +" + bonuses.size);
        }
    }
    
    /**
     * Helper class to store equipment bonuses
     */
    private static class EquipmentBonuses {
        int armor = 0;
        int magicResist = 0;
        int physicalDamage = 0;
        int magicDamage = 0;
        int mana = 0;
        int health = 0;
        int cooldownReduction = 0;
        double attackSpeed = 0;
        double attackRange = 0;
        double size = 0;
    }
    
    /**
     * Helper class to store player state
     */
    private static class PlayerState {
        double healthPercentage = 1.0;
        int mana = 0;
        double manaPercentage = 0.0;
    }
    
    /**
     * Strip color codes for better regex matching
     */
    private String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * Extract integer stat from lore line
     */
    private int extractStat(String loreLine, Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(loreLine);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    /**
     * Extract double stat from lore line
     */
    private double extractDoubleStat(String loreLine, Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(loreLine);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }

}