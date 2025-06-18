package com.server.profiles.stats;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Manages scanning and updating player stats from equipped items
 */
public class StatScanManager {
    private final Main plugin;
    private final Map<UUID, BukkitTask> playerScanTasks = new HashMap<>();
    private Map<UUID, ItemStatBonuses> lastHeldItemBonuses = new HashMap<>();
    private static final int SCAN_INTERVAL = 5; // Update every 5 ticks (1/4 second)
    
    // Attribute modifier name constants for proper tracking and removal
    private static final String MMO_HEALTH_MODIFIER = "mmo.health";
    private static final String MMO_SIZE_MODIFIER = "mmo.size";
    private static final String MMO_ATTACK_RANGE_MODIFIER = "mmo.attack_range";
    private static final String MMO_ATTACK_SPEED_MODIFIER = "mmo.attackspeed";
    private static final String MMO_MOVEMENT_SPEED_MODIFIER = "mmo.movementspeed";
    private static final String MMO_MINING_SPEED_MODIFIER = "mmo.mining_speed";
    private static final String MMO_BUILD_RANGE_MODIFIER = "mmo.build_range";
    
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
        
        // Store the current item in hand to detect when it changes
        final ItemStack[] lastItem = new ItemStack[1];
        lastItem[0] = player.getInventory().getItemInMainHand();
        
        // Initialize last known armor
        ItemStack[] currentArmor = new ItemStack[4];
        currentArmor[0] = player.getInventory().getHelmet();
        currentArmor[1] = player.getInventory().getChestplate();
        currentArmor[2] = player.getInventory().getLeggings();
        currentArmor[3] = player.getInventory().getBoots();
        updateLastKnownArmor(player, currentArmor);
        
        // Do an initial scan
        scanAndUpdatePlayerStats(player);
        
        // Schedule regular scans
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    playerScanTasks.remove(player.getUniqueId());
                    player.removeMetadata("last_known_armor", plugin);
                    return;
                }
                
                // Check if player still has an active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile == null) return;
                
                // Check if the main hand item has changed to avoid unnecessary scans
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                boolean itemChanged = !itemsEqual(lastItem[0], currentItem);
                
                if (itemChanged || shouldScanRegardless(player)) {
                    // Update the last known item
                    lastItem[0] = currentItem != null ? currentItem.clone() : null;
                    
                    // Scan and update stats
                    scanAndUpdatePlayerStats(player);
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS) && itemChanged) {
                        plugin.debugLog(DebugSystem.STATS,"Item in hand changed for " + player.getName() + ", updating stats");
                    }
                }
            }
        }.runTaskTimer(plugin, SCAN_INTERVAL, SCAN_INTERVAL);
        
        // Store the task reference
        playerScanTasks.put(player.getUniqueId(), task);
        
        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
            plugin.debugLog(DebugSystem.STATS,"Started stat scanning for " + player.getName());
        }
    }

    /**
     * Compare two items for functional equality (ignoring durability/amount)
     */
    private boolean itemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;
        
        // Check basic properties
        if (item1.getType() != item2.getType()) return false;
        
        // Check meta existence
        if (item1.hasItemMeta() != item2.hasItemMeta()) return false;
        if (!item1.hasItemMeta()) return true; // Both don't have meta, so equal
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        // Check custom model data
        if (meta1.hasCustomModelData() != meta2.hasCustomModelData()) return false;
        if (meta1.hasCustomModelData() && meta1.getCustomModelData() != meta2.getCustomModelData()) return false;
        
        // Check display name
        if (meta1.hasDisplayName() != meta2.hasDisplayName()) return false;
        if (meta1.hasDisplayName() && !meta1.getDisplayName().equals(meta2.getDisplayName())) return false;
        
        // Check lore
        if (meta1.hasLore() != meta2.hasLore()) return false;
        if (meta1.hasLore() && !meta1.getLore().equals(meta2.getLore())) return false;
        
        return true;
    }

    /**
     * Determine if we should scan regardless of item changes
     */
    private boolean shouldScanRegardless(Player player) {
        // Store current armor pieces in a temporary array
        ItemStack[] currentArmor = new ItemStack[4];
        currentArmor[0] = player.getInventory().getHelmet();
        currentArmor[1] = player.getInventory().getChestplate();
        currentArmor[2] = player.getInventory().getLeggings();
        currentArmor[3] = player.getInventory().getBoots();
        
        // Get the last known armor from the player metadata
        ItemStack[] lastArmor = getLastKnownArmor(player);
        
        // Check if any armor piece has changed
        boolean armorChanged = false;
        for (int i = 0; i < 4; i++) {
            if (!itemsEqual(currentArmor[i], lastArmor[i])) {
                armorChanged = true;
                break;
            }
        }
        
        // Update last known armor
        if (armorChanged) {
            updateLastKnownArmor(player, currentArmor);
        }
        
        // Every 20 ticks (1 second) do a full scan regardless
        boolean timeBased = System.currentTimeMillis() % 20 == 0;
        
        return armorChanged || timeBased;
    }

    /**
     * Get the last known armor from player metadata
     */
    private ItemStack[] getLastKnownArmor(Player player) {
        ItemStack[] armor = new ItemStack[4];
        
        // Try to get stored armor from metadata
        if (player.hasMetadata("last_known_armor")) {
            try {
                Object obj = player.getMetadata("last_known_armor").get(0).value();
                if (obj instanceof ItemStack[]) {
                    return (ItemStack[]) obj;
                }
            } catch (Exception e) {
                // Ignore and return empty array
            }
        }
        
        return armor;
    }

    /**
     * Update the last known armor in player metadata
     */
    private void updateLastKnownArmor(Player player, ItemStack[] armor) {
        player.removeMetadata("last_known_armor", plugin);
        player.setMetadata("last_known_armor", new FixedMetadataValue(plugin, armor.clone()));
    }
        
    /**
     * Stop scanning a player's stats 
     */
    public void stopScanning(Player player) {
        BukkitTask task = playerScanTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            
            // Important: Also clear any stored held item bonuses
            lastHeldItemBonuses.remove(player.getUniqueId());
            player.removeMetadata("last_known_armor", plugin); // Clean up armor metadata
            
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"Stopped stat scanning for " + player.getName());
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
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS, "Saved " + player.getName() + "'s health (" + player.getHealth() + 
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
            
            // Reset attack speed attribute
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                removeModifiersByName(attackSpeedAttribute, MMO_ATTACK_SPEED_MODIFIER);
                attackSpeedAttribute.setBaseValue(0.5); // Our custom default
            }

            // Reset mining speed attribute
            AttributeInstance miningSpeedAttribute = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttribute != null) {
                removeModifiersByName(miningSpeedAttribute, MMO_MINING_SPEED_MODIFIER);
                miningSpeedAttribute.setBaseValue(0.5); // Our custom default
            }
            
            // Reset size attribute
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                removeModifiersByName(scaleAttribute, MMO_SIZE_MODIFIER);
                // Don't reset base value as it's handled by baseline modifier
            }

            AttributeInstance buildRangeAttribute = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
            if (buildRangeAttribute != null) {
                removeModifiersByName(buildRangeAttribute, MMO_BUILD_RANGE_MODIFIER);
                // Don't reset base value as it's handled by baseline modifier
            }
            
            // Reset step height attribute
            try {
                AttributeInstance stepHeightAttribute = player.getAttribute(Attribute.GENERIC_STEP_HEIGHT);
                if (stepHeightAttribute != null) {
                    for (AttributeModifier mod : new HashSet<>(stepHeightAttribute.getModifiers())) {
                        if (mod.getName().equals("mmo.step_height")) {
                            stepHeightAttribute.removeModifier(mod);
                        }
                    }
                    stepHeightAttribute.setBaseValue(0.6); // Vanilla default
                }
            } catch (Exception e) {
                // Step height attribute might not be available
            }
            
            // Reset jump strength attribute
            try {
                AttributeInstance jumpStrengthAttribute = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
                if (jumpStrengthAttribute != null) {
                    for (AttributeModifier mod : new HashSet<>(jumpStrengthAttribute.getModifiers())) {
                        if (mod.getName().equals("mmo.jump_strength")) {
                            jumpStrengthAttribute.removeModifier(mod);
                        }
                    }
                    jumpStrengthAttribute.setBaseValue(0.42); // Vanilla default
                }
            } catch (Exception e) {
                // Jump strength attribute might not be available
            }
            
            // Reset attack range attribute
            AttributeInstance rangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (rangeAttribute != null) {
                removeModifiersByName(rangeAttribute, MMO_ATTACK_RANGE_MODIFIER);
                // Don't reset base value as it's handled by baseline modifier
            }
            
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS, "Error resetting attributes: " + e.getMessage());
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
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
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                logBonuses(player, bonuses);
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error scanning and updating player stats: " + e.getMessage());
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
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
        
        // Process armor pieces first
        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
            plugin.debugLog(DebugSystem.STATS,"Scanning equipment for " + player.getName() + ":");
        }

        // Process main hand item ONLY if it's a weapon, not if it's armor or something else that double-processes
        ItemStack mainHandItem = inventory.getItemInMainHand();
        if (mainHandItem != null && mainHandItem.getType() != Material.AIR && 
            mainHandItem.hasItemMeta() && mainHandItem.getItemMeta().hasLore()) {
            
            // Skip if it's armor - armors are processed separately
            if (!isArmorItem(mainHandItem)) {
                // Only process if it's an identifiable weapon or has weapon stats
                // This prevents double counting
                if (isWeaponItem(mainHandItem)) {
                    String itemName = mainHandItem.hasItemMeta() && mainHandItem.getItemMeta().hasDisplayName() ? 
                                mainHandItem.getItemMeta().getDisplayName() : mainHandItem.getType().toString();
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS,"  Processing main hand: " + itemName);
                    }
                    
                    extractStatsFromItem(mainHandItem, bonuses);
                } else if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"  Skipping non-weapon item in main hand: " + 
                                        mainHandItem.getType().toString());
                }
            } else if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"  Skipping armor item in main hand: " + 
                                    mainHandItem.getType().toString());
            }
        }
            
        // Explicitly process each armor piece
        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();
        
        // Process armor pieces
        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasLore()) {
            String itemName = helmet.hasItemMeta() && helmet.getItemMeta().hasDisplayName() ? 
                            helmet.getItemMeta().getDisplayName() : helmet.getType().toString();
            
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"  Processing helmet: " + itemName);
            }
            
            extractStatsFromItem(helmet, bonuses);
        }
        
        if (chestplate != null && chestplate.hasItemMeta() && chestplate.getItemMeta().hasLore()) {
            String itemName = chestplate.hasItemMeta() && chestplate.getItemMeta().hasDisplayName() ? 
                            chestplate.getItemMeta().getDisplayName() : chestplate.getType().toString();
            
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"  Processing chestplate: " + itemName);
            }
            
            extractStatsFromItem(chestplate, bonuses);
        }
        
        if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().hasLore()) {
            String itemName = leggings.hasItemMeta() && leggings.getItemMeta().hasDisplayName() ? 
                            leggings.getItemMeta().getDisplayName() : leggings.getType().toString();
            
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"  Processing leggings: " + itemName);
            }
            
            extractStatsFromItem(leggings, bonuses);
        }
        
        if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
            String itemName = boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() ? 
                            boots.getItemMeta().getDisplayName() : boots.getType().toString();
            
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"  Processing boots: " + itemName);
            }
            
            extractStatsFromItem(boots, bonuses);
        }
        
        return bonuses;
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
     * Determine if an item is a weapon that shouldn't be processed twice
     */
    private boolean isWeaponItem(ItemStack item) {
        if (item == null) return false;
        
        // If it's a recognized weapon by material type
        Material type = item.getType();
        String name = type.name();
        boolean isVanillaWeapon = name.endsWith("_SWORD") || 
                                name.endsWith("_AXE") || 
                                name.contains("BOW") ||
                                name.contains("TRIDENT");
                                
        // If it's a custom weapon (based on custom model data)
        boolean isCustomWeapon = false;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            // This is our custom pattern for weapons (2xxxxx)
            isCustomWeapon = (modelData >= 210000 && modelData < 300000);
        }
        
        // If it contains weapon stat patterns in the lore
        boolean hasWeaponStats = false;
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            for (String loreLine : item.getItemMeta().getLore()) {
                String cleanLine = stripColorCodes(loreLine);
                if (cleanLine.contains("Physical Damage:") || 
                    cleanLine.contains("Critical Damage:") ||
                    cleanLine.contains("Attack Speed:") ||
                    cleanLine.contains("Life Steal:") ||
                    cleanLine.contains("Lifesteal:")) {
                    hasWeaponStats = true;
                    break;
                }
            }
        }
        
        // It's a weapon if it fits any of these criteria
        return isVanillaWeapon || isCustomWeapon || hasWeaponStats;
    }

    /**
     * Extract base integer stat value from a line - FIXED: Handle integer format after enchanting
     */
    private int extractBaseIntStat(String loreLine, String statName) {
        if (!loreLine.contains(statName)) {
            return 0;
        }
        
        // Find the stat name in the line
        int statIndex = loreLine.indexOf(statName);
        if (statIndex == -1) {
            return 0;
        }
        
        // Get everything after the stat name
        String afterStat = loreLine.substring(statIndex + statName.length()).trim();
        
        // CRITICAL FIX: Look for the TOTAL value (first number), NOT the bracketed bonus
        // Pattern: "Health: +15 (5)" - we want 15, not 5
        // ENHANCED: Handle both integer and decimal total values
        Pattern totalPattern = Pattern.compile("\\+(\\d+(?:\\.\\d+)?)");
        Matcher matcher = totalPattern.matcher(afterStat);
        
        if (matcher.find()) {
            try {
                String valueStr = matcher.group(1);
                // FIXED: Parse as double first, then convert to int to handle decimal totals
                double totalValue = Double.parseDouble(valueStr);
                int result = (int) totalValue; // Truncate to integer
                
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, 
                        "STAT SCAN: " + statName + " extracted TOTAL value " + result + " (from " + valueStr + ") from: " + loreLine);
                }
                
                return result;
            } catch (NumberFormatException e) {
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, 
                        "STAT SCAN: Failed to parse " + statName + " from: " + loreLine);
                }
            }
        } else {
            if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                plugin.debugLog(DebugSystem.ENCHANTING, 
                    "STAT SCAN: No value found for " + statName + " in: " + loreLine);
            }
        }
        
        return 0;
    }

    /**
     * Extract base double stat value from a line - FIXED: Handle both integer and decimal totals
     */
    private double extractBaseDoubleStat(String loreLine, String statName) {
        if (!loreLine.contains(statName)) {
            return 0.0;
        }
        
        // Find the stat name in the line
        int statIndex = loreLine.indexOf(statName);
        if (statIndex == -1) {
            return 0.0;
        }
        
        // Get everything after the stat name
        String afterStat = loreLine.substring(statIndex + statName.length()).trim();
        
        // CRITICAL FIX: Look for the TOTAL value (first number), NOT the bracketed bonus
        // Pattern: "Mining Speed: +0.4 (0.2)" - we want 0.4, not 0.2
        // Pattern: "Mining Fortune: +5 (0.5)" - we want 5, not 0.5
        Pattern totalPattern = Pattern.compile("\\+(\\d+(?:\\.\\d+)?)");
        Matcher matcher = totalPattern.matcher(afterStat);
        
        if (matcher.find()) {
            try {
                String valueStr = matcher.group(1);
                double totalValue = Double.parseDouble(valueStr);
                
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, 
                        "STAT SCAN: " + statName + " extracted TOTAL value " + totalValue + " from: " + loreLine);
                }
                
                return totalValue;
            } catch (NumberFormatException e) {
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, 
                        "STAT SCAN: Failed to parse " + statName + " from: " + loreLine);
                }
            }
        } else {
            if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                plugin.debugLog(DebugSystem.ENCHANTING, 
                    "STAT SCAN: No value found for " + statName + " in: " + loreLine);
            }
        }
        
        return 0.0;
    }

    /**
     * Enhanced extractStatsFromItem method - FIXED: Process critical stats properly
     */
    private void extractStatsFromItem(ItemStack item, ItemStatBonuses bonuses) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        
        if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
            plugin.debugLog(DebugSystem.ENCHANTING, 
                "STAT SCAN: Extracting stats from " + item.getType().name() + 
                (item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                    " (" + ChatColor.stripColor(item.getItemMeta().getDisplayName()) + ")" : ""));
        }
        
        // Extract stats from any item with lore that contains stat information
        for (String loreLine : item.getItemMeta().getLore()) {
            // Strip color codes for regex matching
            String cleanLine = stripColorCodes(loreLine);
            
            // CRITICAL FIX: Don't skip bracketed lines - read the total value from them
            boolean hasBrackets = cleanLine.contains("(") && cleanLine.contains(")");
            
            if (hasBrackets && plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                plugin.debugLog(DebugSystem.ENCHANTING, 
                    "STAT SCAN: Processing enchanted stat line: " + cleanLine);
            }
            
            // SPECIAL CASE: Always process Health Regen lines regardless of enchantment-only status
            if (cleanLine.contains("Health Regen:")) {
                double healthRegenBonus = extractBaseDoubleStat(cleanLine, "Health Regen:");
                if (healthRegenBonus > 0) {
                    bonuses.healthRegen += healthRegenBonus;
                    if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                        plugin.debugLog(DebugSystem.ENCHANTING, 
                            "STAT SCAN: FORCED Health Regen processing: " + healthRegenBonus + " (total: " + bonuses.healthRegen + ")");
                    }
                }
                continue; // Skip the normal enchantment-only check for Health Regen
            }
            
            // CRITICAL FIX: Process critical stats from items (store as decimals for PlayerStats)
            if (cleanLine.contains("Critical Chance:")) {
                double critChanceBonus = extractBaseDoubleStat(cleanLine, "Critical Chance:");
                if (critChanceBonus > 0) {
                    // StatScanManager stores as decimals for PlayerStats (15 becomes 0.15)
                    bonuses.critChance += (critChanceBonus / 100.0);
                    if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                        plugin.debugLog(DebugSystem.ENCHANTING, 
                            "STAT SCAN: Added critical chance: " + critChanceBonus + "% (" + (critChanceBonus / 100.0) + " decimal) (total: " + bonuses.critChance + ")");
                    }
                }
                continue;
            }

            if (cleanLine.contains("Critical Damage:")) {
                double critDamageBonus = extractBaseDoubleStat(cleanLine, "Critical Damage:");
                if (critDamageBonus > 0) {
                    // StatScanManager stores as decimals for PlayerStats (30 becomes 0.30)
                    bonuses.critDamage += (critDamageBonus / 100.0);
                    if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                        plugin.debugLog(DebugSystem.ENCHANTING, 
                            "STAT SCAN: Added critical damage: " + critDamageBonus + "% (" + (critDamageBonus / 100.0) + " decimal) (total: " + bonuses.critDamage + ")");
                    }
                }
                continue;
            }
            
            // Skip lines that are clearly enchantment-only stats (where base value would be 0)
            if (isEnchantmentOnlyStatLine(cleanLine)) {
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, 
                        "STAT SCAN: SKIPPING enchantment-only stat: " + cleanLine);
                }
                continue;
            }
            
            // Process each stat type - ALL use consistent extraction methods
            int healthBonus = extractBaseIntStat(cleanLine, "Health:");
            if (healthBonus > 0) {
                bonuses.health += healthBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added health: " + healthBonus + " (total: " + bonuses.health + ")");
                }
            }
            
            int armorBonus = extractBaseIntStat(cleanLine, "Armor:");
            if (armorBonus > 0) {
                bonuses.armor += armorBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added armor: " + armorBonus + " (total: " + bonuses.armor + ")");
                }
            }
            
            int mrBonus = extractBaseIntStat(cleanLine, "Magic Resist:");
            if (mrBonus > 0) {
                bonuses.magicResist += mrBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added magic resist: " + mrBonus + " (total: " + bonuses.magicResist + ")");
                }
            }
            
            int pdBonus = extractBaseIntStat(cleanLine, "Physical Damage:");
            if (pdBonus > 0) {
                bonuses.physicalDamage += pdBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added physical damage: " + pdBonus + " (total: " + bonuses.physicalDamage + ")");
                }
            }

            int rdBonus = extractBaseIntStat(cleanLine, "Ranged Damage:");
            if (rdBonus > 0) {
                bonuses.rangedDamage += rdBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added ranged damage: " + rdBonus + " (total: " + bonuses.rangedDamage + ")");
                }
            }
            
            int mdBonus = extractBaseIntStat(cleanLine, "Magic Damage:");
            if (mdBonus > 0) {
                bonuses.magicDamage += mdBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added magic damage: " + mdBonus + " (total: " + bonuses.magicDamage + ")");
                }
            }
            
            int manaBonus = extractBaseIntStat(cleanLine, "Mana:");
            if (manaBonus > 0) {
                bonuses.mana += manaBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added mana: " + manaBonus + " (total: " + bonuses.mana + ")");
                }
            }
            
            // CRITICAL FIX: Process double stats from enchanted lines
            double miningSpeedBonus = extractBaseDoubleStat(cleanLine, "Mining Speed:");
            if (miningSpeedBonus > 0) {
                bonuses.miningSpeed += miningSpeedBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added mining speed: " + miningSpeedBonus + " (total: " + bonuses.miningSpeed + ")");
                }
            }
            
            // CRITICAL FIX: Process Mining Fortune from enchanted lines
            double miningFortuneBonus = extractBaseDoubleStat(cleanLine, "Mining Fortune:");
            if (miningFortuneBonus > 0) {
                bonuses.miningFortune += miningFortuneBonus;
                if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                    plugin.debugLog(DebugSystem.ENCHANTING, "STAT SCAN: Added mining fortune: " + miningFortuneBonus + " (total: " + bonuses.miningFortune + ")");
                }
            }
            
            // Process other stats...
            bonuses.cooldownReduction += extractBaseIntStat(cleanLine, "Cooldown Reduction:");
            
            bonuses.attackSpeed += extractBaseDoubleStat(cleanLine, "Attack Speed:");
            bonuses.attackRange += extractBaseDoubleStat(cleanLine, "Attack Range:");
            bonuses.size += extractBaseDoubleStat(cleanLine, "Size:");
            bonuses.lifeSteal += extractBaseDoubleStat(cleanLine, "Life Steal:");
            bonuses.omnivamp += extractBaseDoubleStat(cleanLine, "Omnivamp:");
            bonuses.buildRange += extractBaseDoubleStat(cleanLine, "Build Range:");
        }
        
        if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
            plugin.debugLog(DebugSystem.ENCHANTING, 
                "STAT SCAN: Final bonuses - Health:" + bonuses.health + 
                " Armor:" + bonuses.armor + " PhysDmg:" + bonuses.physicalDamage + 
                " MagicDmg:" + bonuses.magicDamage + " Mana:" + bonuses.mana + 
                " MiningSpeed:" + bonuses.miningSpeed + " MiningFortune:" + bonuses.miningFortune +
                " HealthRegen:" + bonuses.healthRegen +
                " CritChance:" + bonuses.critChance + " CritDamage:" + bonuses.critDamage); // Added critical stats to debug output
        }
    }

    /**
     * Check if a stat line is an enchantment-only stat - FIXED: Proper Health Regen handling
     */
    private boolean isEnchantmentOnlyStatLine(String cleanLine) {
        // For lines with brackets, check if they would have a base value of 0
        if (cleanLine.contains("(") && cleanLine.contains(")")) {
            String beforeBrackets = cleanLine.substring(0, cleanLine.indexOf("(")).trim();
            String inBrackets = cleanLine.substring(cleanLine.indexOf("(") + 1, cleanLine.indexOf(")")).trim();
            
            // Find stat name
            String[] statPrefixes = {
                "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
                "Mana:", "Critical Chance:", "Critical Damage:", "Mining Fortune:", 
                "Mining Speed:", "Farming Fortune:", "Looting Fortune:", "Fishing Fortune:",
                "Build Range:", "Cooldown Reduction:", "Health Regen:", "Speed:", "Luck:"
            };
            
            String statName = null;
            for (String prefix : statPrefixes) {
                if (cleanLine.contains(prefix)) {
                    statName = prefix;
                    break;
                }
            }
            
            if (statName != null) {
                try {
                    // Extract total and bonus values
                    String totalValueStr = beforeBrackets.substring(beforeBrackets.indexOf(statName) + statName.length()).trim();
                    totalValueStr = totalValueStr.replace("+", "").trim();
                    
                    String bonusValueStr = inBrackets.replace("+", "").replace("-", "").trim();
                    
                    // Parse as double to handle both integer and decimal values
                    double totalValue = Double.parseDouble(totalValueStr);
                    double bonusValue = Double.parseDouble(bonusValueStr);
                    
                    // CRITICAL FIX: Health Regen should NEVER be considered enchantment-only
                    // Health Regen always has a base value (even if it's 0), so it should always be processed
                    if (statName.equals("Health Regen:")) {
                        if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                            plugin.debugLog(DebugSystem.ENCHANTING, 
                                "ENCHANTMENT-ONLY CHECK: " + statName + " is NEVER enchantment-only - always process");
                        }
                        return false; // Always process Health Regen stats
                    }
                    
                    // Check if total equals bonus (meaning base value is 0)
                    boolean isEnchantmentOnly = Math.abs(totalValue - bonusValue) < 0.001; // Use small epsilon for floating point comparison
                    
                    if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                        plugin.debugLog(DebugSystem.ENCHANTING, 
                            "ENCHANTMENT-ONLY CHECK: " + statName + " total=" + totalValue + " bonus=" + bonusValue + " -> " + isEnchantmentOnly);
                    }
                    
                    return isEnchantmentOnly;
                } catch (NumberFormatException e) {
                    if (plugin.isDebugEnabled(DebugSystem.ENCHANTING)) {
                        plugin.debugLog(DebugSystem.ENCHANTING, 
                            "ENCHANTMENT-ONLY CHECK: Parse error for " + cleanLine + ", assuming not enchantment-only");
                    }
                    return false;
                }
            }
        }
        
        // Lines without brackets are never enchantment-only
        return false;
    }
        
    /**
     * Apply the extracted bonuses to player stats
     */
    private void applyBonusesToStats(PlayerStats stats, ItemStatBonuses bonuses) {
        // Store current health and mana to preserve them
        double currentHealth = stats.getCurrentHealth();
        int currentMana = stats.getMana();
        
        // Reset to defaults first
        stats.resetToDefaults();
        
        // Apply bonuses
        stats.setHealth(stats.getDefaultHealth() + bonuses.health);
        stats.setArmor(stats.getDefaultArmor() + bonuses.armor);
        stats.setMagicResist(stats.getDefaultMagicResist() + bonuses.magicResist);
        stats.setPhysicalDamage(stats.getDefaultPhysicalDamage() + bonuses.physicalDamage);
        stats.setRangedDamage(stats.getDefaultRangedDamage() + bonuses.rangedDamage);
        stats.setMagicDamage(stats.getDefaultMagicDamage() + bonuses.magicDamage);
        stats.setTotalMana(stats.getDefaultMana() + bonuses.mana);
        
        // CRITICAL FIX: Apply health regen bonus with enhanced debugging
        double oldHealthRegen = stats.getHealthRegen();
        double newHealthRegen = stats.getDefaultHealthRegen() + bonuses.healthRegen;
        stats.setHealthRegen(newHealthRegen);
        
        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
            plugin.debugLog(DebugSystem.STATS, 
                "HEALTH REGEN APPLICATION: default=" + stats.getDefaultHealthRegen() + 
                " + bonus=" + bonuses.healthRegen + " = " + newHealthRegen + 
                " (was " + oldHealthRegen + ")");
        }
        
        stats.setCooldownReduction(stats.getDefaultCooldownReduction() + bonuses.cooldownReduction);
        stats.setAttackSpeed(stats.getDefaultAttackSpeed() + bonuses.attackSpeed);
        stats.setAttackRange(stats.getDefaultAttackRange() + bonuses.attackRange);
        stats.setSize(stats.getDefaultSize() + bonuses.size);
        stats.setLifeSteal(stats.getDefaultLifeSteal() + bonuses.lifeSteal);
        stats.setCriticalChance(stats.getDefaultCriticalChance() + bonuses.critChance);
        stats.setCriticalDamage(stats.getDefaultCriticalDamage() + bonuses.critDamage);
        stats.setOmnivamp(stats.getDefaultOmnivamp() + bonuses.omnivamp);
        
        // Keep current health as is, just cap it if needed
        stats.setCurrentHealth(Math.min(currentHealth, stats.getHealth()));
        
        // Keep current mana as is, just cap it if needed
        stats.setMana(Math.min(currentMana, stats.getTotalMana()));

        // Mining stats
        stats.setMiningFortune(stats.getDefaultMiningFortune() + bonuses.miningFortune);
        stats.setMiningSpeed(stats.getDefaultMiningSpeed() + bonuses.miningSpeed);
        stats.setBuildRange(stats.getDefaultBuildRange() + bonuses.buildRange);
        
        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
            plugin.debugLog(DebugSystem.STATS,
                "Applied stat bonuses to " + stats.toString() + 
                " | HealthRegen: " + stats.getHealthRegen() + " (+" + bonuses.healthRegen + ")");
        }
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
        stats.setRangedDamage(stats.getDefaultRangedDamage());
        stats.setTotalMana(stats.getDefaultMana());
        stats.setCooldownReduction(stats.getDefaultCooldownReduction());
        
        // CRITICAL: Reset health regen to default before applying equipment bonuses
        stats.setHealthRegen(stats.getDefaultHealthRegen());
        
        stats.setAttackSpeed(stats.getDefaultAttackSpeed());
        stats.setAttackRange(stats.getDefaultAttackRange());
        stats.setSize(stats.getDefaultSize());
        stats.setLifeSteal(stats.getDefaultLifeSteal());
        stats.setOmnivamp(stats.getDefaultOmnivamp());
        stats.setCriticalChance(stats.getDefaultCriticalChance());
        stats.setCriticalDamage(stats.getDefaultCriticalDamage());
        
        // Maintain current health as is, just cap at max health if needed
        stats.setCurrentHealth(Math.min(currentHealth, stats.getHealth()));
        
        // Maintain current mana as is, just cap at max mana if needed
        stats.setMana(Math.min(currentMana, stats.getTotalMana()));

        stats.setMiningSpeed(stats.getDefaultMiningSpeed());
        stats.setMiningFortune(stats.getDefaultMiningFortune());
        stats.setBuildRange(stats.getDefaultBuildRange());
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

            // Apply mining speed
            applyMiningSpeedAttribute(player, stats);

            // Apply build range - add this line
            applyBuildRangeAttribute(player, stats);
            
            // Ensure health display is always 10 hearts
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error applying attributes to player: " + e.getMessage());
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
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
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Health check for " + player.getName() + ": current=" + currentHealth + 
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
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS,"Set respawned player " + player.getName() + "'s health to " + 
                                    healthToSet + "/" + newMaxHealth);
                    }
                }
                // Case 2: Vanilla reset (health is exactly 20 but shouldn't be)
                else if (isVanillaReset && Math.abs(stats.getCurrentHealth() - 20.0) > 0.1) {
                    // Player has rejoined and vanilla reset their health - restore from saved value
                    double healthToSet = Math.min(stats.getCurrentHealth(), newMaxHealth);
                    player.setHealth(healthToSet);
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS,"Restored " + player.getName() + "'s health from vanilla reset: " + 
                                    currentHealth + " -> " + healthToSet + "/" + newMaxHealth);
                    }
                }
                // Case 3: Equipment changes affecting max health
                else if (currentHealth > newMaxHealth) {
                    // Only cap health if it exceeds new maximum
                    player.setHealth(newMaxHealth);
                    stats.setCurrentHealth(newMaxHealth);
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS,"Capped " + player.getName() + "'s health from " + 
                                    currentHealth + " to " + newMaxHealth);
                    }
                }
                // In all other cases, PRESERVE current health
                else {
                    // Update the stored value but don't change player's current health
                    stats.setCurrentHealth(currentHealth);
                    
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS,"Preserved " + player.getName() + "'s current health at " + 
                                    currentHealth + "/" + newMaxHealth);
                    }
                }
                
                // Always set health display scale for consistent UI
                player.setHealthScaled(true);
                player.setHealthScale(20.0);
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error applying health attribute: " + e.getMessage());
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
                
                // Set base to new default value (0.5 instead of vanilla 4.0)
                attackSpeed.setBaseValue(0.5);
                
                // Apply bonus attack speed if needed
                double totalAttackSpeed = stats.getAttackSpeed();
                double attackSpeedBonus = totalAttackSpeed - 0.5; // Adjusted from 1.0
                
                if (attackSpeedBonus != 0) {
                    AttributeModifier attackSpeedMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_ATTACK_SPEED_MODIFIER,
                        attackSpeedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackSpeed.addModifier(attackSpeedMod);
                }
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Applied attack speed attribute to " + player.getName() + 
                                ": " + totalAttackSpeed + " (Final: " + attackSpeed.getValue() + ")");
                }
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error applying attack speed attribute: " + e.getMessage());
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
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Applied movement speed attribute to " + player.getName() + 
                                    ": " + stats.getSpeed());
                }
            }
        } catch (Exception e) {
            plugin.debugLog(DebugSystem.STATS,"Error applying movement speed attribute: " + e.getMessage());
        }
    }
    
    /**
     * Apply size attribute and related effects (step height and jump strength)
     */
    private void applySizeAttribute(Player player, PlayerStats stats) {
        try {
            // 1. Update the GENERIC_SCALE attribute
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                // CRITICAL CHANGE: Only remove specific modifiers, keep the baseline modifier
                for (AttributeModifier mod : new HashSet<>(scaleAttribute.getModifiers())) {
                    if (mod.getName().equals(MMO_SIZE_MODIFIER) || 
                        mod.getName().equals("mmo.temp_size_fix") ||
                        mod.getName().equals("mmo.size.fixed")) {
                        scaleAttribute.removeModifier(mod);
                    }
                }
                
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
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS, "Applied size attribute to " + player.getName() + 
                                    ": " + totalSize + " (Final: " + scaleAttribute.getValue() + ")");
                }
                
                // 2. Update GENERIC_STEP_HEIGHT attribute based on size (0.6 * size)
                try {
                    AttributeInstance stepHeightAttribute = player.getAttribute(Attribute.GENERIC_STEP_HEIGHT);
                    if (stepHeightAttribute != null) {
                        // Remove existing modifiers
                        for (AttributeModifier mod : new HashSet<>(stepHeightAttribute.getModifiers())) {
                            if (mod.getName().equals("mmo.step_height")) {
                                stepHeightAttribute.removeModifier(mod);
                            }
                        }
                        
                        // Calculate new step height (default step height is 0.6)
                        // Formula: stepHeight = 0.6 * size
                        double baseStepHeight = 0.6;
                        double newStepHeight = baseStepHeight * totalSize;
                        double stepHeightBonus = newStepHeight - baseStepHeight;
                        
                        // Apply step height modifier
                        if (stepHeightBonus != 0) {
                            AttributeModifier stepHeightMod = new AttributeModifier(
                                UUID.randomUUID(),
                                "mmo.step_height",
                                stepHeightBonus,
                                AttributeModifier.Operation.ADD_NUMBER
                            );
                            stepHeightAttribute.addModifier(stepHeightMod);
                        }
                        
                        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                            plugin.debugLog(DebugSystem.STATS, "Applied step height attribute to " + player.getName() + 
                                            ": " + newStepHeight + " (base: " + baseStepHeight + 
                                            ", size: " + totalSize + ")");
                        }
                    }
                } catch (Exception e) {
                    // Step height attribute might not be available
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS, "Step height attribute not supported: " + e.getMessage());
                    }
                }
                
                // 3. Update GENERIC_JUMP_STRENGTH attribute based on size (0.42 * sqrt(size))
                try {
                    AttributeInstance jumpStrengthAttribute = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
                    if (jumpStrengthAttribute != null) {
                        // Remove existing modifiers
                        for (AttributeModifier mod : new HashSet<>(jumpStrengthAttribute.getModifiers())) {
                            if (mod.getName().equals("mmo.jump_strength")) {
                                jumpStrengthAttribute.removeModifier(mod);
                            }
                        }
                        
                        // Calculate new jump strength (default is 0.42)
                        // Formula: jumpStrength = 0.42 * sqrt(size)
                        double baseJumpStrength = 0.42;
                        double newJumpStrength = baseJumpStrength * Math.sqrt(totalSize);
                        double jumpStrengthBonus = newJumpStrength - baseJumpStrength;
                        
                        // Apply jump strength modifier
                        if (jumpStrengthBonus != 0) {
                            AttributeModifier jumpStrengthMod = new AttributeModifier(
                                UUID.randomUUID(),
                                "mmo.jump_strength",
                                jumpStrengthBonus,
                                AttributeModifier.Operation.ADD_NUMBER
                            );
                            jumpStrengthAttribute.addModifier(jumpStrengthMod);
                        }
                        
                        if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                            plugin.debugLog(DebugSystem.STATS, "Applied jump strength attribute to " + player.getName() + 
                                            ": " + newJumpStrength + " (base: " + baseJumpStrength + 
                                            ", size: " + totalSize + ", sqrt(size): " + Math.sqrt(totalSize) + ")");
                        }
                    }
                } catch (Exception e) {
                    // Jump strength attribute might not be available
                    if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                        plugin.debugLog(DebugSystem.STATS, "Jump strength attribute not supported: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Scale attribute might not be available in older versions
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS, "Error applying size attribute: " + e.getMessage());
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
                // CRITICAL CHANGE: Only remove specific modifiers, keep the baseline modifier
                for (AttributeModifier mod : new HashSet<>(rangeAttribute.getModifiers())) {
                    if (mod.getName().equals(MMO_ATTACK_RANGE_MODIFIER) || 
                        mod.getName().equals("mmo.temp_range_fix")) {
                        rangeAttribute.removeModifier(mod);
                    }
                }
                
                // CRITICAL CHANGE: Don't change base value once initialized
                // rangeAttribute.setBaseValue(3.0); - REMOVE THIS LINE
                
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
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Applied attack range attribute to " + player.getName() + 
                                    ": " + totalRange + " (Final: " + rangeAttribute.getValue() + ")");
                }
            }
        } catch (Exception e) {
            // Range attribute might not be available in older versions
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"Attack range attribute not supported: " + e.getMessage());
            }
        }
    }

    /**
     * Apply build range attribute
     */
    private void applyBuildRangeAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance buildRangeAttribute = player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
            if (buildRangeAttribute != null) {
                // CRITICAL CHANGE: Only remove specific modifiers, keep the baseline modifier
                for (AttributeModifier mod : new HashSet<>(buildRangeAttribute.getModifiers())) {
                    if (mod.getName().equals(MMO_BUILD_RANGE_MODIFIER) || 
                        mod.getName().equals("mmo.temp_build_range_fix")) {
                        buildRangeAttribute.removeModifier(mod);
                    }
                }
                
                // CRITICAL CHANGE: Don't change base value once initialized
                // buildRangeAttribute.setBaseValue(5.0); - Don't set this
                
                // Apply bonus range if needed
                double totalBuildRange = stats.getBuildRange();
                double buildRangeBonus = totalBuildRange - 5.0; // Default is 5.0
                
                if (buildRangeBonus != 0) {
                    AttributeModifier buildRangeMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_BUILD_RANGE_MODIFIER,
                        buildRangeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    buildRangeAttribute.addModifier(buildRangeMod);
                }
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS, "Applied build range attribute to " + player.getName() + 
                                    ": " + totalBuildRange + " (Final: " + buildRangeAttribute.getValue() + ")");
                }
            }
        } catch (Exception e) {
            // Build range attribute might not be available in older versions
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS, "Build range attribute not supported: " + e.getMessage());
            }
        }
    }

    /**
     * Apply mining speed attribute
     */
    private void applyMiningSpeedAttribute(Player player, PlayerStats stats) {
        try {
            AttributeInstance miningSpeedAttr = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED);
            if (miningSpeedAttr != null) {
                // Debug the current state before modification
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Mining speed attribute before update for " + player.getName() + 
                                ": base=" + miningSpeedAttr.getBaseValue() + 
                                ", total=" + miningSpeedAttr.getValue());
                }
                
                // CRITICAL CHANGE: Only remove specific modifiers, keep the baseline modifier
                for (AttributeModifier mod : new HashSet<>(miningSpeedAttr.getModifiers())) {
                    if (mod.getName().equals(MMO_MINING_SPEED_MODIFIER) || 
                        mod.getName().equals("mmo.temp_mining_speed_fix")) {
                        miningSpeedAttr.removeModifier(mod);
                    }
                }
                
                // CRITICAL CHANGE: Don't change base value once initialized
                // miningSpeedAttr.setBaseValue(0.5); - REMOVE THIS LINE
                
                // Apply bonus mining speed if needed
                double totalMiningSpeed = stats.getMiningSpeed();
                double miningSpeedBonus = totalMiningSpeed - 0.5; // Adjust based on default
                
                if (miningSpeedBonus > 0) {
                    AttributeModifier miningSpeedMod = new AttributeModifier(
                        UUID.randomUUID(),
                        MMO_MINING_SPEED_MODIFIER,
                        miningSpeedBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    miningSpeedAttr.addModifier(miningSpeedMod);
                }
                
                if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                    plugin.debugLog(DebugSystem.STATS,"Applied mining speed attribute to " + player.getName() + 
                                ": " + totalMiningSpeed + " (Final: " + miningSpeedAttr.getValue() + ")");
                }
            }
        } catch (Exception e) {
            // Mining speed attribute might not be available in older versions
            if (plugin.isDebugEnabled(DebugSystem.STATS)) {
                plugin.debugLog(DebugSystem.STATS,"Error applying mining speed attribute: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Helper method to log bonuses for debugging
     */
    private void logBonuses(Player player, ItemStatBonuses bonuses) {
        if (!plugin.isDebugEnabled(DebugSystem.STATS)) return;

        plugin.debugLog(DebugSystem.STATS,"Equipment bonuses for " + player.getName() + ":");
        plugin.debugLog(DebugSystem.STATS,"  Health: +" + bonuses.health);
        plugin.debugLog(DebugSystem.STATS,"  Armor: +" + bonuses.armor);
        plugin.debugLog(DebugSystem.STATS,"  Magic Resist: +" + bonuses.magicResist);
        plugin.debugLog(DebugSystem.STATS,"  Physical Damage: +" + bonuses.physicalDamage);
        plugin.debugLog(DebugSystem.STATS,"  Magic Damage: +" + bonuses.magicDamage);
        plugin.debugLog(DebugSystem.STATS,"  Ranged Damage: +" + bonuses.rangedDamage);
        plugin.debugLog(DebugSystem.STATS,"  Mana: +" + bonuses.mana);
        plugin.debugLog(DebugSystem.STATS,"  Cooldown Reduction: +" + bonuses.cooldownReduction + "%");
        
        // ENHANCED: Better health regen debugging
        plugin.debugLog(DebugSystem.STATS,"  Health Regen: +" + bonuses.healthRegen + " (CRITICAL STAT)");
        
        plugin.debugLog(DebugSystem.STATS,"  Attack Speed: +" + bonuses.attackSpeed);
        plugin.debugLog(DebugSystem.STATS,"  Attack Range: +" + bonuses.attackRange);
        plugin.debugLog(DebugSystem.STATS,"  Size: +" + bonuses.size);
        plugin.debugLog(DebugSystem.STATS,"  Life Steal: +" + bonuses.lifeSteal + "%");
        plugin.debugLog(DebugSystem.STATS,"  Omnivamp: +" + bonuses.omnivamp + "%");
        plugin.debugLog(DebugSystem.STATS,"  Critical Chance: +" + bonuses.critChance + "%");
        plugin.debugLog(DebugSystem.STATS,"  Critical Damage: +" + bonuses.critDamage + "%");
        plugin.debugLog(DebugSystem.STATS,"  Mining Fortune: +" + bonuses.miningFortune);
        plugin.debugLog(DebugSystem.STATS,"  Mining Speed: +" + bonuses.miningSpeed);
        plugin.debugLog(DebugSystem.STATS,"  Build Range: +" + bonuses.buildRange);
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
        int rangedDamage = 0;
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
        double omnivamp = 0;
        double miningFortune = 0;
        double miningSpeed = 0;
        double buildRange = 0;
    }

}