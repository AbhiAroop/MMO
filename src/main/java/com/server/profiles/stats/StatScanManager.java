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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
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
    private Map<UUID, ItemStatBonuses> lastHeldItemBonuses = new HashMap<>();
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
    private static final Pattern LIFE_STEAL_PATTERN = Pattern.compile("(?:Life Steal|Lifesteal): \\+(\\d+\\.?\\d*)%?");
    private static final Pattern CRIT_CHANCE_PATTERN = Pattern.compile("Critical Chance: \\+(\\d+\\.?\\d*)%");
    private static final Pattern CRIT_DAMAGE_PATTERN = Pattern.compile("Critical Damage: \\+(\\d+\\.?\\d*)x");
    private static final Pattern OMNIVAMP_PATTERN = Pattern.compile("(?:Omnivamp|Omni Vamp): \\+(\\d+\\.?\\d*)%?");

    // Mining-specific patterns
    private static final Pattern MINING_FORTUNE_PATTERN = Pattern.compile("Mining Fortune: \\+(\\d+\\.?\\d*)");

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
                    
                    if (plugin.isDebugMode() && itemChanged) {
                        plugin.getLogger().info("Item in hand changed for " + player.getName() + ", updating stats");
                    }
                }
            }
        }.runTaskTimer(plugin, SCAN_INTERVAL, SCAN_INTERVAL);
        
        // Store the task reference
        playerScanTasks.put(player.getUniqueId(), task);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Started stat scanning for " + player.getName());
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
                attackSpeedAttribute.setBaseValue(0.5); // Vanilla default
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
        
        // Process armor pieces first
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Scanning equipment for " + player.getName() + ":");
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
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("  Processing main hand: " + itemName);
                    }
                    
                    extractStatsFromItem(mainHandItem, bonuses);
                } else if (plugin.isDebugMode()) {
                    plugin.getLogger().info("  Skipping non-weapon item in main hand: " + 
                                        mainHandItem.getType().toString());
                }
            } else if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Skipping armor item in main hand: " + 
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
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing helmet: " + itemName);
            }
            
            extractStatsFromItem(helmet, bonuses);
        }
        
        if (chestplate != null && chestplate.hasItemMeta() && chestplate.getItemMeta().hasLore()) {
            String itemName = chestplate.hasItemMeta() && chestplate.getItemMeta().hasDisplayName() ? 
                            chestplate.getItemMeta().getDisplayName() : chestplate.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing chestplate: " + itemName);
            }
            
            extractStatsFromItem(chestplate, bonuses);
        }
        
        if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().hasLore()) {
            String itemName = leggings.hasItemMeta() && leggings.getItemMeta().hasDisplayName() ? 
                            leggings.getItemMeta().getDisplayName() : leggings.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing leggings: " + itemName);
            }
            
            extractStatsFromItem(leggings, bonuses);
        }
        
        if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
            String itemName = boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() ? 
                            boots.getItemMeta().getDisplayName() : boots.getType().toString();
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("  Processing boots: " + itemName);
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
                    cleanLine.contains("Size:") ||
                    cleanLine.contains("Lifesteal:")||
                    cleanLine.contains("Omnivamp:")) {
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
               
            // Try specific pattern first
            bonuses.lifeSteal += extractDoubleStat(cleanLine, LIFE_STEAL_PATTERN);
            bonuses.omnivamp += extractDoubleStat(cleanLine, OMNIVAMP_PATTERN);


            bonuses.critChance += extractDoubleStat(cleanLine, CRIT_CHANCE_PATTERN);
            bonuses.critDamage += extractDoubleStat(cleanLine, CRIT_DAMAGE_PATTERN);

            double miningFortuneBonus = extractDoubleStat(cleanLine, MINING_FORTUNE_PATTERN);
            if (miningFortuneBonus > 0) {
                // We'll add the mining fortune to a new field in ItemStatBonuses
                bonuses.miningFortune += miningFortuneBonus;
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Added mining fortune: " + miningFortuneBonus);
                }
            }
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
        stats.setOmnivamp(stats.getDefaultOmnivamp() + bonuses.omnivamp);
        stats.setCriticalChance(stats.getDefaultCriticalChance() + (bonuses.critChance / 100.0)); // Convert % to decimal
        stats.setCriticalDamage(stats.getDefaultCriticalDamage() + bonuses.critDamage);
        
        // Keep current health as is, just cap it if needed
        stats.setCurrentHealth(Math.min(currentHealth, stats.getHealth()));
        
        // Keep current mana as is, just cap it if needed
        stats.setMana(Math.min(currentMana, stats.getTotalMana()));

        // Mining stats
        stats.setMiningFortune(stats.getDefaultMiningFortune() + bonuses.miningFortune);
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
        stats.setOmnivamp(stats.getDefaultOmnivamp());
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
        plugin.getLogger().info("  Omnivamp: +" + bonuses.omnivamp + "%");
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
        double omnivamp = 0;
        double miningFortune = 0;
    }

}