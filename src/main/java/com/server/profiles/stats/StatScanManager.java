package com.server.profiles.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Manages scanning and updating player stats from equipped items
 */
public class StatScanManager {
    private final Main plugin;
    private final Map<UUID, BukkitTask> playerScanTasks = new HashMap<>();
    private static final int SCAN_INTERVAL = 5; // Update every 5 ticks (1/4 second)
    
    // Stat extraction patterns for parsing lore
    private static final Pattern HEALTH_PATTERN = Pattern.compile("Health: \\+(\\d+)");
    private static final Pattern ARMOR_PATTERN = Pattern.compile("Armor: \\+(\\d+)");
    private static final Pattern MAGIC_RESIST_PATTERN = Pattern.compile("Magic Resist: \\+(\\d+)");
    private static final Pattern PHYSICAL_DAMAGE_PATTERN = Pattern.compile("Physical Damage: \\+(\\d+)");
    private static final Pattern MAGIC_DAMAGE_PATTERN = Pattern.compile("Magic Damage: \\+(\\d+)");
    private static final Pattern MANA_PATTERN = Pattern.compile("Mana: \\+(\\d+)");
    private static final Pattern COOLDOWN_REDUCTION_PATTERN = Pattern.compile("Cooldown Reduction: \\+(\\d+)%");
    private static final Pattern HEALTH_REGEN_PATTERN = Pattern.compile("Health Regen: \\+(\\d+\\.?\\d*)");
    private static final Pattern ATTACK_SPEED_PATTERN = Pattern.compile("Attack Speed: \\+(\\d+\\.?\\d*)");
    private static final Pattern ATTACK_RANGE_PATTERN = Pattern.compile("Attack Range: \\+(\\d+\\.?\\d*)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("Size: \\+(\\d+\\.?\\d*)");
    private static final Pattern LIFE_STEAL_PATTERN = Pattern.compile("Life Steal: \\+(\\d+\\.?\\d*)%");
    private static final Pattern CRIT_CHANCE_PATTERN = Pattern.compile("Critical Chance: \\+(\\d+\\.?\\d*)%");
    private static final Pattern CRIT_DAMAGE_PATTERN = Pattern.compile("Critical Damage: \\+(\\d+\\.?\\d*)x");

    // Attribute modifier name constants for proper tracking and removal
    private static final String MMO_HEALTH_MODIFIER = "mmo.health";
    private static final String MMO_SIZE_MODIFIER = "mmo.size";
    private static final String MMO_ATTACK_RANGE_MODIFIER = "mmo.attack_range";
    private static final String MMO_ATTACK_SPEED_MODIFIER = "mmo.attackspeed";
    private static final String MMO_MOVEMENT_SPEED_MODIFIER = "mmo.movementspeed";
    
    /**
     * Constructor
     */
    public StatScanManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start scanning and updating a player's stats
     */
    public void startScanning(Player player) {
        stopScanning(player); // Stop any existing scanning first
        
        // Do an initial scan
        scanAndUpdatePlayerStats(player);
        
        // Schedule regular scans
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    playerScanTasks.remove(player.getUniqueId());
                    return;
                }
                
                // Check if player still has an active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile == null) return;
                
                // Scan and update stats
                scanAndUpdatePlayerStats(player);
            }
        }.runTaskTimer(plugin, SCAN_INTERVAL, SCAN_INTERVAL);
        
        // Store the task reference
        playerScanTasks.put(player.getUniqueId(), task);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Started stat scanning for " + player.getName());
        }
    }
    
    /**
     * Stop scanning a player's stats
     */
    public void stopScanning(Player player) {
        BukkitTask task = playerScanTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Stopped stat scanning for " + player.getName());
            }
        }
    }
    
    /**
     * Reset player's attributes to vanilla defaults for logout
     * IMPORTANT: This should not actually reset the player's health
     */
    public void resetAttributes(Player player) {
        try {
            // Get profile to save current health
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile != null) {
                    // Ensure current health is saved before resetting attributes
                    profile.getStats().setCurrentHealth(player.getHealth());
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Saved " + player.getName() + "'s health (" + player.getHealth() + 
                                    ") before attribute reset");
                    }
                }
            }
            
            // Reset attributes but DO NOT modify health at all
            // Only reset modifiers so vanilla doesn't override values
            AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttribute != null) {
                // Just remove modifiers, don't change base value or current health
                removeModifiersByName(healthAttribute, MMO_HEALTH_MODIFIER);
                // DO NOT SET BASE VALUE OR CURRENT HEALTH HERE
            }
            
            // Reset other attributes similarly
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                removeModifiersByName(attackSpeedAttribute, MMO_ATTACK_SPEED_MODIFIER);
                attackSpeedAttribute.setBaseValue(4.0); // Vanilla default
            }
            
            // Reset other attributes
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting attributes: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
        
    /**
     * Helper method to remove attribute modifiers by name
     */
    private void removeModifiersByName(AttributeInstance attribute, String modifierName) {
        Set<AttributeModifier> modifiers = new HashSet<>(attribute.getModifiers());
        for (AttributeModifier mod : modifiers) {
            if (mod.getName().contains(modifierName)) {
                attribute.removeModifier(mod);
            }
        }
    }
    
    /**
     * Main method to scan equipment and update player stats
     */
    public void scanAndUpdatePlayerStats(Player player) {
        try {
            // Get the player's active profile
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) return;
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile == null) return;
            
            // Get the stats object to update
            PlayerStats stats = profile.getStats();
            
            // IMPORTANT: Reset stats to base values BEFORE scanning equipment
            // This ensures we don't double-apply stats
            resetStatsToBase(stats);
            
            // Create a container for all item bonuses
            ItemStatBonuses bonuses = scanAllEquipment(player);
            
            // Apply bonuses to player stats
            applyBonusesToStats(stats, bonuses);
            
            // Apply attributes to player based on the updated stats
            applyAttributesToPlayer(player, stats);
            
            // Debug output
            if (plugin.isDebugMode()) {
                logBonuses(player, bonuses);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error scanning and updating player stats: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Scan all equipment (armor and main hand) for stat bonuses
     */
    private ItemStatBonuses scanAllEquipment(Player player) {
        ItemStatBonuses bonuses = new ItemStatBonuses();
        PlayerInventory inventory = player.getInventory();
        
        // Create a set to track processed items and prevent duplicates
        Set<ItemStack> processedItems = new HashSet<>();
        
        // Process armor pieces first (these take priority)
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Scanning equipment for " + player.getName() + ":");
        }
        
        // Explicitly process each armor piece to ensure they're all captured
        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();
        
        // Process helmet
        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasLore()) {
            String itemName = helmet.hasItemMeta() && helmet.getItemMeta().hasDisplayName() ? 
                            helmet.getItemMeta().getDisplayName() : helmet.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing helmet: " + itemName);
                logBonusesDebug("    Before extraction", bonuses);
            }
            
            extractStatsFromItem(helmet, bonuses);
            processedItems.add(helmet);
            
            if (plugin.isDebugMode()) {
                logBonusesDebug("    After extraction", bonuses);
            }
        }
        
        // Process chestplate
        if (chestplate != null && chestplate.hasItemMeta() && chestplate.getItemMeta().hasLore()) {
            String itemName = chestplate.hasItemMeta() && chestplate.getItemMeta().hasDisplayName() ? 
                            chestplate.getItemMeta().getDisplayName() : chestplate.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing chestplate: " + itemName);
                logBonusesDebug("    Before extraction", bonuses);
            }
            
            extractStatsFromItem(chestplate, bonuses);
            processedItems.add(chestplate);
            
            if (plugin.isDebugMode()) {
                logBonusesDebug("    After extraction", bonuses);
            }
        }
        
        // Process leggings
        if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().hasLore()) {
            String itemName = leggings.hasItemMeta() && leggings.getItemMeta().hasDisplayName() ? 
                            leggings.getItemMeta().getDisplayName() : leggings.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing leggings: " + itemName);
                logBonusesDebug("    Before extraction", bonuses);
            }
            
            extractStatsFromItem(leggings, bonuses);
            processedItems.add(leggings);
            
            if (plugin.isDebugMode()) {
                logBonusesDebug("    After extraction", bonuses);
            }
        }
        
        // Process boots
        if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
            String itemName = boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() ? 
                            boots.getItemMeta().getDisplayName() : boots.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing boots: " + itemName);
                logBonusesDebug("    Before extraction", bonuses);
            }
            
            extractStatsFromItem(boots, bonuses);
            processedItems.add(boots);
            
            if (plugin.isDebugMode()) {
                logBonusesDebug("    After extraction", bonuses);
            }
        }
        
        // Then process main hand item if it's not already processed as armor
        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta() && mainHand.getItemMeta().hasLore() && 
            !processedItems.contains(mainHand) && !isArmorItem(mainHand)) {
            
            String itemName = mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() ? 
                            mainHand.getItemMeta().getDisplayName() : mainHand.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing main hand: " + itemName);
                logBonusesDebug("    Before extraction", bonuses);
            }
            
            extractStatsFromItem(mainHand, bonuses);
            
            if (plugin.isDebugMode()) {
                logBonusesDebug("    After extraction", bonuses);
            }
        }
        
        return bonuses;
    }

    /**
     * Helper method to log bonuses in a compact format for debugging
     */
    private void logBonusesDebug(String prefix, ItemStatBonuses bonuses) {
        plugin.getLogger().info(prefix + " - H:" + bonuses.health + 
                        " A:" + bonuses.armor + 
                        " MR:" + bonuses.magicResist + 
                        " PD:" + bonuses.physicalDamage + 
                        " MD:" + bonuses.magicDamage +
                        " M:" + bonuses.mana +
                        " S:" + bonuses.size);
    }
        
    /**
     * Check if an item is an armor piece or should be treated as one
     */
    private boolean isArmorItem(ItemStack item) {
        if (item == null) return false;
        
        String name = item.getType().name();
        
        // Standard armor types
        boolean isStandardArmor = name.endsWith("_HELMET") || 
                                name.endsWith("_CHESTPLATE") || 
                                name.endsWith("_LEGGINGS") || 
                                name.endsWith("_BOOTS");
        
        // Special headgear
        boolean isSpecialHeadgear = name.equals("CARVED_PUMPKIN") || 
                                    name.equals("PLAYER_HEAD") ||
                                    name.equals("SKULL_ITEM") ||
                                    name.equals("TURTLE_HELMET");
        
        return isStandardArmor || isSpecialHeadgear;
    }
    
    /**
     * Extract all stats from a single item
     */
    private void extractStatsFromItem(ItemStack item, ItemStatBonuses bonuses) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        
        // Extract stats from any item with lore that contains stat information
        for (String loreLine : item.getItemMeta().getLore()) {
            // Strip color codes for regex matching
            String cleanLine = stripColorCodes(loreLine);
            
            // Debug log for understanding what's being processed
            if (plugin.isDebugMode()) {
                if (cleanLine.contains("Health:") || 
                    cleanLine.contains("Armor:") || 
                    cleanLine.contains("Magic Resist:") ||
                    cleanLine.contains("Physical Damage:") ||
                    cleanLine.contains("Magic Damage:") ||
                    cleanLine.contains("Mana:") ||
                    cleanLine.contains("Size:")) {
                    plugin.getLogger().info("Processing stat line: " + cleanLine);
                }
            }
            
            // Extract all possible stats
            int healthBonus = extractIntStat(cleanLine, HEALTH_PATTERN);
            if (healthBonus > 0) {
                bonuses.health += healthBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added health: " + healthBonus);
            }
            
            int armorBonus = extractIntStat(cleanLine, ARMOR_PATTERN);
            if (armorBonus > 0) {
                bonuses.armor += armorBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added armor: " + armorBonus);
            }
            
            int mrBonus = extractIntStat(cleanLine, MAGIC_RESIST_PATTERN);
            if (mrBonus > 0) {
                bonuses.magicResist += mrBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added magic resist: " + mrBonus);
            }
            
            int pdBonus = extractIntStat(cleanLine, PHYSICAL_DAMAGE_PATTERN);
            if (pdBonus > 0) {
                bonuses.physicalDamage += pdBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added physical damage: " + pdBonus);
            }
            
            int mdBonus = extractIntStat(cleanLine, MAGIC_DAMAGE_PATTERN);
            if (mdBonus > 0) {
                bonuses.magicDamage += mdBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added magic damage: " + mdBonus);
            }
            
            int manaBonus = extractIntStat(cleanLine, MANA_PATTERN);
            if (manaBonus > 0) {
                bonuses.mana += manaBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added mana: " + manaBonus);
            }
            
            // Process the rest of the stats similarly with debug logging
            bonuses.cooldownReduction += extractIntStat(cleanLine, COOLDOWN_REDUCTION_PATTERN);
            bonuses.healthRegen += extractDoubleStat(cleanLine, HEALTH_REGEN_PATTERN);
            bonuses.attackSpeed += extractDoubleStat(cleanLine, ATTACK_SPEED_PATTERN);
            bonuses.attackRange += extractDoubleStat(cleanLine, ATTACK_RANGE_PATTERN);
            
            double sizeBonus = extractDoubleStat(cleanLine, SIZE_PATTERN);
            if (sizeBonus > 0) {
                bonuses.size += sizeBonus;
                if (plugin.isDebugMode()) plugin.getLogger().info("Added size: " + sizeBonus);
            }
            
            bonuses.lifeSteal += extractDoubleStat(cleanLine, LIFE_STEAL_PATTERN);
            bonuses.critChance += extractDoubleStat(cleanLine, CRIT_CHANCE_PATTERN);
            bonuses.critDamage += extractDoubleStat(cleanLine, CRIT_DAMAGE_PATTERN);
        }
    }

    /**
     * Helper method to extract int stat from lore line
     */
    private int extractIntStat(String loreLine, Pattern pattern) {
        Matcher matcher = pattern.matcher(loreLine);
        if (matcher.find()) {
            try {
                String valuePart = matcher.group(1);
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Extracted value: " + valuePart + " from line: " + loreLine);
                }
                return Integer.parseInt(valuePart);
            } catch (NumberFormatException e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Failed to parse int value from: " + loreLine);
                }
            }
        }
        return 0;
    }

    /**
     * Helper method to extract double stat from lore line
     */
    private double extractDoubleStat(String loreLine, Pattern pattern) {
        Matcher matcher = pattern.matcher(loreLine);
        if (matcher.find()) {
            try {
                String valuePart = matcher.group(1);
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Extracted value: " + valuePart + " from line: " + loreLine);
                }
                return Double.parseDouble(valuePart);
            } catch (NumberFormatException e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Failed to parse double value from: " + loreLine);
                }
            }
        }
        return 0.0;
    }

    /**
     * Check if an item is a vanilla item (not custom)
     */
    private boolean isVanillaItem(ItemStack item) {
        // If the item has custom model data, it's definitely not vanilla
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            return false;
        }
        
        Material material = item.getType();
        String name = material.name();
        return name.endsWith("_SWORD") || 
            name.endsWith("_AXE") ||
            name.endsWith("_PICKAXE") ||
            name.endsWith("_SHOVEL") ||
            name.endsWith("_HOE") ||
            name.endsWith("_HELMET") ||
            name.endsWith("_CHESTPLATE") ||
            name.endsWith("_LEGGINGS") ||
            name.endsWith("_BOOTS");
    }
    
    /**
     * Apply the extracted bonuses to player stats
     */
    private void applyBonusesToStats(PlayerStats stats, ItemStatBonuses bonuses) {
        // Store current health and mana directly (no percentage calculation)
        double currentHealth = stats.getCurrentHealth();
        int currentMana = stats.getMana();
        
        // IMPORTANT: Only apply item bonuses, don't double up stats
        stats.setHealth(stats.getDefaultHealth() + bonuses.health);
        stats.setArmor(stats.getDefaultArmor() + bonuses.armor);
        stats.setMagicResist(stats.getDefaultMagicResist() + bonuses.magicResist);
        stats.setPhysicalDamage(stats.getDefaultPhysicalDamage() + bonuses.physicalDamage);
        stats.setMagicDamage(stats.getDefaultMagicDamage() + bonuses.magicDamage);
        stats.setTotalMana(stats.getDefaultMana() + bonuses.mana);
        stats.setCooldownReduction(stats.getDefaultCooldownReduction() + bonuses.cooldownReduction);
        stats.setHealthRegen(stats.getDefaultHealthRegen() + bonuses.healthRegen);
        stats.setAttackSpeed(stats.getDefaultAttackSpeed() + bonuses.attackSpeed);
        stats.setAttackRange(stats.getDefaultAttackRange() + bonuses.attackRange);
        stats.setSize(stats.getDefaultSize() + bonuses.size);
        stats.setLifeSteal(stats.getDefaultLifeSteal() + bonuses.lifeSteal);
        stats.setCriticalChance(stats.getDefaultCriticalChance() + (bonuses.critChance / 100.0)); // Convert % to decimal
        stats.setCriticalDamage(stats.getDefaultCriticalDamage() + bonuses.critDamage);
        
        // Keep current health as is, just cap it if needed
        stats.setCurrentHealth(Math.min(currentHealth, stats.getHealth()));
        
        // Keep current mana as is, just cap it if needed
        stats.setMana(Math.min(currentMana, stats.getTotalMana()));
    }
        
    /**
     * Reset stats to base values
     */
    private void resetStatsToBase(PlayerStats stats) {
        // Store current health value directly (no percentage calculation)
        double currentHealth = stats.getCurrentHealth();
        
        // Store mana directly too
        int currentMana = stats.getMana();
        
        // Reset ALL equipment-related stats to defaults
        stats.setHealth(stats.getDefaultHealth());
        stats.setArmor(stats.getDefaultArmor());
        stats.setMagicResist(stats.getDefaultMagicResist());
        stats.setPhysicalDamage(stats.getDefaultPhysicalDamage());
        stats.setMagicDamage(stats.getDefaultMagicDamage());
        stats.setTotalMana(stats.getDefaultMana());
        stats.setCooldownReduction(stats.getDefaultCooldownReduction());
        stats.setHealthRegen(stats.getDefaultHealthRegen());
        stats.setAttackSpeed(stats.getDefaultAttackSpeed());
        stats.setAttackRange(stats.getDefaultAttackRange());
        stats.setSize(stats.getDefaultSize());
        stats.setLifeSteal(stats.getDefaultLifeSteal());
        stats.setCriticalChance(stats.getDefaultCriticalChance());
        stats.setCriticalDamage(stats.getDefaultCriticalDamage());
        
        // Maintain current health as is, just cap at max health if needed
        stats.setCurrentHealth(Math.min(currentHealth, stats.getHealth()));
        
        // Maintain current mana as is, just cap at max mana if needed
        stats.setMana(Math.min(currentMana, stats.getTotalMana()));
    }
    
    /**
     * Apply stats to player's Minecraft attributes
     */
    private void applyAttributesToPlayer(Player player, PlayerStats stats) {
        try {
            // Apply health
            applyHealthAttribute(player, stats);
            
            // Apply attack speed
            applyAttackSpeedAttribute(player, stats);
            
            // Apply movement speed
            applyMovementSpeedAttribute(player, stats);
            
            // Apply size/scale
            applySizeAttribute(player, stats);
            
            // Apply attack range
            applyAttackRangeAttribute(player, stats);
            
            // Ensure health display is always 10 hearts
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying attributes to player: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Apply health attribute with proper health preservation
     */
    private void applyHealthAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                // CRITICAL: Store current health value BEFORE any attribute changes
                double currentHealth = player.getHealth();
                
                // Store if this is likely a vanilla reset situation (health exactly 20)
                boolean isVanillaReset = Math.abs(currentHealth - 20.0) < 0.1;
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Health check for " + player.getName() + ": current=" + currentHealth + 
                                ", stored=" + stats.getCurrentHealth() + 
                                ", vanilla reset=" + isVanillaReset);
                }
                
                // Remove existing modifiers
                for (AttributeModifier mod : new HashSet<>(maxHealth.getModifiers())) {
                    maxHealth.removeModifier(mod);
                }
                
                // Set base value to vanilla default (20.0)
                maxHealth.setBaseValue(20.0);
                
                // Apply bonus health if needed
                int totalHealth = stats.getHealth();
                double healthBonus = totalHealth - 20.0;
                
                if (healthBonus != 0) {
                    AttributeModifier healthMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_HEALTH_MODIFIER,
                        healthBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    maxHealth.addModifier(healthMod);
                }
                
                double newMaxHealth = maxHealth.getValue();
                
                // CRUCIAL PRIORITY LOGIC:
                // 1. Respawn handling - set to default health
                // 2. Vanilla reset handling - restore from saved state
                // 3. General equipment changes - preserve current value but cap if needed
                
                // Case 1: Respawn detection (health near 0)
                boolean isRespawned = currentHealth <= 1.0 && player.getFoodLevel() == 20;
                
                if (isRespawned) {
                    // After respawn, set to default health
                    double healthToSet = Math.min(stats.getDefaultHealth(), newMaxHealth);
                    player.setHealth(healthToSet);
                    stats.setCurrentHealth(healthToSet);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Set respawned player " + player.getName() + "'s health to " + 
                                    healthToSet + "/" + newMaxHealth);
                    }
                }
                // Case 2: Vanilla reset (health is exactly 20 but shouldn't be)
                else if (isVanillaReset && Math.abs(stats.getCurrentHealth() - 20.0) > 0.1) {
                    // Player has rejoined and vanilla reset their health - restore from saved value
                    double healthToSet = Math.min(stats.getCurrentHealth(), newMaxHealth);
                    player.setHealth(healthToSet);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Restored " + player.getName() + "'s health from vanilla reset: " + 
                                    currentHealth + " -> " + healthToSet + "/" + newMaxHealth);
                    }
                }
                // Case 3: Equipment changes affecting max health
                else if (currentHealth > newMaxHealth) {
                    // Only cap health if it exceeds new maximum
                    player.setHealth(newMaxHealth);
                    stats.setCurrentHealth(newMaxHealth);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Capped " + player.getName() + "'s health from " + 
                                    currentHealth + " to " + newMaxHealth);
                    }
                }
                // In all other cases, PRESERVE current health
                else {
                    // Update the stored value but don't change player's current health
                    stats.setCurrentHealth(currentHealth);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Preserved " + player.getName() + "'s current health at " + 
                                    currentHealth + "/" + newMaxHealth);
                    }
                }
                
                // Always set health display scale for consistent UI
                player.setHealthScaled(true);
                player.setHealthScale(20.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying health attribute: " + e.getMessage());
        }
    }
        
    /**
     * Apply attack speed attribute
     */
    private void applyAttackSpeedAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeed != null) {
                // Remove existing modifiers
                removeModifiersByName(attackSpeed, MMO_ATTACK_SPEED_MODIFIER);
                
                // Set base to vanilla default
                attackSpeed.setBaseValue(4.0);
                
                // Apply bonus attack speed if needed
                double totalAttackSpeed = stats.getAttackSpeed();
                double attackSpeedBonus = totalAttackSpeed - 4.0;
                
                if (attackSpeedBonus != 0) {
                    AttributeModifier attackSpeedMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_ATTACK_SPEED_MODIFIER,
                        attackSpeedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackSpeed.addModifier(attackSpeedMod);
                }
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied attack speed attribute to " + player.getName() + 
                                    ": " + totalAttackSpeed + " (Final: " + attackSpeed.getValue() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying attack speed attribute: " + e.getMessage());
        }
    }
    
    /**
     * Apply movement speed attribute
     */
    private void applyMovementSpeedAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed != null) {
                // Remove existing modifiers
                removeModifiersByName(movementSpeed, MMO_MOVEMENT_SPEED_MODIFIER);
                
                // Just set base value directly for movement speed
                movementSpeed.setBaseValue(stats.getSpeed());
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied movement speed attribute to " + player.getName() + 
                                    ": " + stats.getSpeed());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying movement speed attribute: " + e.getMessage());
        }
    }
    
    /**
     * Apply size attribute
     */
    private void applySizeAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                // Remove existing modifiers
                removeModifiersByName(scaleAttribute, MMO_SIZE_MODIFIER);
                
                // Set base to vanilla default (1.0)
                scaleAttribute.setBaseValue(1.0);
                
                // Apply bonus size if needed
                double totalSize = stats.getSize();
                double sizeBonus = totalSize - 1.0;
                
                if (sizeBonus != 0) {
                    AttributeModifier sizeMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_SIZE_MODIFIER,
                        sizeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttribute.addModifier(sizeMod);
                }
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied size attribute to " + player.getName() + 
                                    ": " + totalSize + " (Final: " + scaleAttribute.getValue() + ")");
                }
            }
        } catch (Exception e) {
            // Scale attribute might not be available in older versions
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Error applying size attribute: " + e.getMessage());
            }
        }
    }
    
    /**
     * Apply attack range attribute
     */
    private void applyAttackRangeAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance rangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (rangeAttribute != null) {
                // Remove existing modifiers
                removeModifiersByName(rangeAttribute, MMO_ATTACK_RANGE_MODIFIER);
                
                // Set base to vanilla default
                rangeAttribute.setBaseValue(3.0);
                
                // Apply bonus range if needed
                double totalRange = stats.getAttackRange();
                double rangeBonus = totalRange - 3.0;
                
                if (rangeBonus != 0) {
                    AttributeModifier rangeMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_ATTACK_RANGE_MODIFIER,
                        rangeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    rangeAttribute.addModifier(rangeMod);
                }
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Applied attack range attribute to " + player.getName() + 
                                    ": " + totalRange + " (Final: " + rangeAttribute.getValue() + ")");
                }
            }
        } catch (Exception e) {
            // Range attribute might not be available in older versions
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Attack range attribute not supported: " + e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to log bonuses for debugging
     */
    private void logBonuses(Player player, ItemStatBonuses bonuses) {
        plugin.getLogger().info("Equipment bonuses for " + player.getName() + ":");
        plugin.getLogger().info("  Health: +" + bonuses.health);
        plugin.getLogger().info("  Armor: +" + bonuses.armor);
        plugin.getLogger().info("  Magic Resist: +" + bonuses.magicResist);
        plugin.getLogger().info("  Physical Damage: +" + bonuses.physicalDamage);
        plugin.getLogger().info("  Magic Damage: +" + bonuses.magicDamage);
        plugin.getLogger().info("  Mana: +" + bonuses.mana);
        plugin.getLogger().info("  Cooldown Reduction: +" + bonuses.cooldownReduction + "%");
        plugin.getLogger().info("  Health Regen: +" + bonuses.healthRegen);
        plugin.getLogger().info("  Attack Speed: +" + bonuses.attackSpeed);
        plugin.getLogger().info("  Attack Range: +" + bonuses.attackRange);
        plugin.getLogger().info("  Size: +" + bonuses.size);
        plugin.getLogger().info("  Life Steal: +" + bonuses.lifeSteal + "%");
        plugin.getLogger().info("  Crit Chance: +" + bonuses.critChance + "%");
        plugin.getLogger().info("  Crit Damage: +" + bonuses.critDamage + "x");
    }
    
    
    /**
     * Helper method to strip color codes for better regex matching
     */
    private String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    
    /**
     * Helper class to store all equipment bonuses
     */
    private static class ItemStatBonuses {
        int health = 0;
        int armor = 0;
        int magicResist = 0;
        int physicalDamage = 0;
        int magicDamage = 0;
        int mana = 0;
        int cooldownReduction = 0;
        double healthRegen = 0;
        double attackSpeed = 0;
        double attackRange = 0;
        double size = 0;
        double lifeSteal = 0;
        double critChance = 0;
        double critDamage = 0;
    }

}