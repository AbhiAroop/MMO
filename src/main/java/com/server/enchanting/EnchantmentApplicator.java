package com.server.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Handles applying custom enchantments to items
 */
public class EnchantmentApplicator {
    
    private static final String ENCHANTMENT_PREFIX = "custom_enchant_";
    
    /**
     * Apply a custom enchantment to an item
     */
    public static ItemStack applyEnchantment(ItemStack item, CustomEnchantment enchantment, int level) {
        if (item == null || enchantment == null || level <= 0) {
            return null;
        }
        
        ItemStack enchantedItem = item.clone();
        ItemMeta meta = enchantedItem.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        // Check if item already has conflicting enchantments
        if (hasConflictingEnchantment(enchantedItem, enchantment)) {
            return null;
        }
        
        // Store enchantment data in persistent data container
        NamespacedKey enchantmentKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantment.getId());
        meta.getPersistentDataContainer().set(enchantmentKey, PersistentDataType.INTEGER, level);
        
        // Add enchantment to lore
        addEnchantmentToLore(meta, enchantment, level);
        
        // Apply visual glint effect
        meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        
        enchantedItem.setItemMeta(meta);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchantment Applicator] Applied " + enchantment.getFormattedName(level) + 
                " to " + item.getType().name());
        }
        
        return enchantedItem;
    }

    /**
     * Apply multiple enchantments to an item - FIXED: Comprehensive debugging and stat fixes
     */
    public static ItemStack applyMultipleEnchantments(ItemStack item, List<EnchantmentRandomizer.AppliedEnchantment> appliedEnchantments) {
        if (item == null || appliedEnchantments.isEmpty()) {
            return item;
        }
        
        // CRITICAL DEBUG: Log before state
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "=== ENCHANTING PROCESS START ===");
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "BEFORE ENCHANTING:");
            debugItemLore(item, "ORIGINAL");
            
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "ENCHANTMENTS TO APPLY:");
            for (EnchantmentRandomizer.AppliedEnchantment applied : appliedEnchantments) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "- " + applied.enchantment.getDisplayName() + " Level " + applied.level);
            }
        }
        
        ItemStack enchantedItem = item.clone();
        ItemMeta meta = enchantedItem.getItemMeta();
        if (meta == null) {
            return enchantedItem;
        }
        
        // CRITICAL FIX: Completely remove existing enchantments AND clean up stat bonuses
        enchantedItem = clearAllEnchantmentsAndStatBonuses(enchantedItem);
        meta = enchantedItem.getItemMeta();
        
        // DEBUG: Log after cleanup
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "AFTER CLEANUP:");
            debugItemLore(enchantedItem, "CLEANED");
        }
        
        // Apply all new enchantments
        for (EnchantmentRandomizer.AppliedEnchantment applied : appliedEnchantments) {
            // Store enchantment data
            NamespacedKey enchantmentKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + applied.enchantment.getId());
            meta.getPersistentDataContainer().set(enchantmentKey, PersistentDataType.INTEGER, applied.level);
        }
        
        // Apply visual glint effect
        meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        
        // Update lore with all enchantments and enhanced stats
        updateItemLoreWithEnchantmentsAndStats(meta, appliedEnchantments);
        
        enchantedItem.setItemMeta(meta);
        
        // CRITICAL DEBUG: Log final result
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "FINAL RESULT:");
            debugItemLore(enchantedItem, "ENCHANTED");
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "=== ENCHANTING PROCESS END ===");
        }
        
        return enchantedItem;
    }

    /**
     * Debug method to log item lore analysis - ENHANCED
     */
    private static void debugItemLore(ItemStack item, String context) {
        if (!Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            return;
        }
        
        Main.getInstance().debugLog(DebugSystem.ENCHANTING, "--- LORE ANALYSIS (" + context + ") ---");
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "No lore present");
            return;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String clean = ChatColor.stripColor(line);
            boolean hasBrackets = clean.contains("(") && clean.contains(")");
            boolean isBlank = line.trim().isEmpty();
            boolean isStatLine = isStatLine(clean);
            
            String flags = "";
            if (isBlank) flags += "[BLANK]";
            if (hasBrackets) flags += "[BRACKETS]";
            if (isStatLine) flags += "[STAT]";
            if (clean.contains("Stats:")) flags += "[STATS_HEADER]";
            if (clean.contains("✦ Enchantments:")) flags += "[ENCHANT_HEADER]";
            
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                String.format("[%d] %s: %s", i, flags, line));
        }
        Main.getInstance().debugLog(DebugSystem.ENCHANTING, "--- END LORE ANALYSIS ---");
    }

    /**
     * Check if a line is a stat line
     */
    private static boolean isStatLine(String cleanLine) {
        String[] statPrefixes = {
            "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
            "Mana:", "Critical Chance:", "Critical Damage:", "Mining Fortune:", 
            "Mining Speed:", "Farming Fortune:", "Looting Fortune:", "Fishing Fortune:",
            "Build Range:", "Cooldown Reduction:", "Health Regen:", "Speed:", "Luck:"
        };
        
        for (String prefix : statPrefixes) {
            if (cleanLine.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clean all stat bonuses (lines with brackets) from lore - ENHANCED with debugging
     */
    private static void cleanStatBonusesFromLore(ItemMeta meta) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "CLEANING STAT BONUSES FROM LORE");
        }
        
        List<String> cleanedLore = new ArrayList<>();
        boolean inStatsSection = false;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String cleanLine = ChatColor.stripColor(line).trim();
            
            // Check if we're entering stats section
            if (cleanLine.equals("Stats:")) {
                inStatsSection = true;
                cleanedLore.add(line);
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Entering stats section");
                }
                continue;
            }
            
            // Check if we're leaving stats section (empty line or new section)
            if (inStatsSection && (cleanLine.isEmpty() || line.contains("✦"))) {
                inStatsSection = false;
                cleanedLore.add(line);
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Leaving stats section");
                }
                continue;
            }
            
            // If we're in stats section, clean the stat lines
            if (inStatsSection) {
                String cleanedStatLine = removeStatBonuses(line);
                if (cleanedStatLine != null) {
                    cleanedLore.add(cleanedStatLine);
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "Cleaned stat: '" + line + "' -> '" + cleanedStatLine + "'");
                    }
                } else {
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "Removed enchantment-only stat: '" + line + "'");
                    }
                }
            } else {
                cleanedLore.add(line);
            }
        }
        
        meta.setLore(cleanedLore);
    }

    /**
     * Extract ONLY the base stat value from a line, ignoring any enchantment bonuses - ENHANCED with debugging
     */
    private static String extractBaseStatValue(String line, String statName) {
        String afterStatName = line.substring(line.indexOf(statName) + statName.length()).trim();
        
        // Remove color codes
        String cleanAfterStat = ChatColor.stripColor(afterStatName);
        
        // If there are brackets, take everything before the first bracket
        if (cleanAfterStat.contains("(")) {
            cleanAfterStat = cleanAfterStat.substring(0, cleanAfterStat.indexOf("(")).trim();
        }
        
        // Remove the "+" prefix if present
        cleanAfterStat = cleanAfterStat.replace("+", "").trim();
        
        // ENHANCED: Remove any trailing suffixes for parsing
        cleanAfterStat = cleanAfterStat.replace("%", "").replace("x", "").trim();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "BASE VALUE EXTRACTION: '" + statName + "' from '" + line + "' = '" + cleanAfterStat + "'");
        }
        
        return cleanAfterStat;
    }

    /**
     * Update stat lines in lore with enchantment bonuses - FIXED: Prevent duplicates and improve stat handling
     */
    private static void updateStatsInLore(List<String> lore, StatBonuses bonuses) {
        if (!bonuses.hasAnyBonuses()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, "No stat bonuses to apply");
            }
            return;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "UPDATING STATS IN LORE");
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Bonuses to apply: " + bonuses.toString());
        }
        
        // Find stats section
        int statsStartIndex = -1;
        int statsEndIndex = -1;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Stats:")) {
                statsStartIndex = i + 1;
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Found stats section at index " + i);
                }
            } else if (statsStartIndex != -1 && (line.trim().isEmpty() || line.contains("✦"))) {
                statsEndIndex = i;
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Stats section ends at index " + i);
                }
                break;
            }
        }
        
        if (statsStartIndex == -1) {
            // No existing stats section, create one
            statsStartIndex = findStatsInsertionPoint(lore);
            
            if (statsStartIndex > 0 && !lore.get(statsStartIndex - 1).trim().isEmpty()) {
                lore.add(statsStartIndex, "");
                statsStartIndex++;
            }
            
            lore.add(statsStartIndex, ChatColor.GRAY + "Stats:");
            statsStartIndex += 1;
            statsEndIndex = statsStartIndex;
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Created new stats section at index " + statsStartIndex);
            }
        }
        
        if (statsEndIndex == -1) {
            statsEndIndex = lore.size();
        }
        
        // CRITICAL FIX: Process existing stat lines more carefully
        List<String> updatedStats = new ArrayList<>();
        Set<String> processedStats = new HashSet<>();
        
        // Process existing stat lines
        for (int i = statsStartIndex; i < statsEndIndex; i++) {
            String line = lore.get(i);
            String statType = getStatTypeFromLine(line);
            
            if (statType != null && !processedStats.contains(statType)) {
                String updatedLine = updateStatLine(line, bonuses);
                if (updatedLine != null) {
                    updatedStats.add(updatedLine);
                    processedStats.add(statType);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "Updated existing stat (" + statType + "): '" + line + "' -> '" + updatedLine + "'");
                    }
                }
            }
        }
        
        // Add new stat lines for stats that weren't already present
        List<String> newStatLines = createNewStatLines(lore, bonuses, processedStats);
        updatedStats.addAll(newStatLines);
        
        // Replace the stats section with updated stats
        for (int i = statsEndIndex - 1; i >= statsStartIndex; i--) {
            lore.remove(i);
        }
        
        for (int i = 0; i < updatedStats.size(); i++) {
            lore.add(statsStartIndex + i, updatedStats.get(i));
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Stats section updated with " + updatedStats.size() + " stats");
        }
    }

    /**
     * Get stat type from a lore line
     */
    private static String getStatTypeFromLine(String line) {
        String cleanLine = ChatColor.stripColor(line).trim();
        
        String[] statTypes = {
            "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
            "Mana:", "Critical Chance:", "Critical Damage:", "Mining Fortune:", 
            "Mining Speed:", "Farming Fortune:", "Looting Fortune:", "Fishing Fortune:",
            "Build Range:", "Cooldown Reduction:", "Health Regen:", "Speed:", "Luck:"
        };
        
        for (String statType : statTypes) {
            if (cleanLine.contains(statType)) {
                return statType;
            }
        }
        return null;
    }

    /**
     * Add toString method to StatBonuses for debugging
     */
    private static class StatBonuses {
        int health = 0;
        int armor = 0;
        int magicResist = 0;
        int physicalDamage = 0;
        int magicDamage = 0;
        int mana = 0;
        
        int criticalChance = 0;
        int criticalDamage = 0;
        
        double miningFortune = 0.0;
        double miningSpeed = 0.0;
        double farmingFortune = 0.0;
        double lootingFortune = 0.0;
        double fishingFortune = 0.0;
        double buildRange = 0.0;
        double healthRegen = 0.0;
        
        int cooldownReduction = 0;
        int speed = 0;
        int luck = 0;
        
        boolean hasAnyBonuses() {
            return health != 0 || armor != 0 || magicResist != 0 || physicalDamage != 0 || 
                magicDamage != 0 || mana != 0 || criticalChance != 0 || criticalDamage != 0 ||
                miningFortune != 0.0 || miningSpeed != 0.0 || farmingFortune != 0.0 ||
                lootingFortune != 0.0 || fishingFortune != 0.0 || buildRange != 0.0 ||
                healthRegen != 0.0 || cooldownReduction != 0 || speed != 0 || luck != 0;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (health != 0) sb.append("Health:").append(health).append(" ");
            if (armor != 0) sb.append("Armor:").append(armor).append(" ");
            if (magicResist != 0) sb.append("MagicResist:").append(magicResist).append(" ");
            if (physicalDamage != 0) sb.append("PhysicalDamage:").append(physicalDamage).append(" ");
            if (magicDamage != 0) sb.append("MagicDamage:").append(magicDamage).append(" ");
            if (mana != 0) sb.append("Mana:").append(mana).append(" ");
            if (criticalChance != 0) sb.append("CritChance:").append(criticalChance).append(" ");
            if (criticalDamage != 0) sb.append("CritDamage:").append(criticalDamage).append(" ");
            if (miningFortune != 0.0) sb.append("MiningFortune:").append(miningFortune).append(" ");
            if (miningSpeed != 0.0) sb.append("MiningSpeed:").append(miningSpeed).append(" ");
            return sb.toString().trim();
        }
    }

    /**
     * Clear all enchantments AND remove stat bonuses - FIXED: Proper return handling
     */
    public static ItemStack clearAllEnchantmentsAndStatBonuses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack cleanedItem = item.clone();
        ItemMeta meta = cleanedItem.getItemMeta();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "CLEARING ALL ENCHANTMENTS AND STAT BONUSES");
            debugItemLore(cleanedItem, "BEFORE CLEARING");
        }
        
        // Remove all custom enchantment persistent data
        Set<NamespacedKey> keysToRemove = new HashSet<>();
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            if (key.getNamespace().equals(Main.getInstance().getName()) && 
                key.getKey().startsWith(ENCHANTMENT_PREFIX)) {
                keysToRemove.add(key);
            }
        }
        
        for (NamespacedKey key : keysToRemove) {
            meta.getPersistentDataContainer().remove(key);
        }
        
        // Remove visual enchantment glint
        meta.removeEnchant(org.bukkit.enchantments.Enchantment.PROTECTION);
        
        // CRITICAL: Apply meta changes before lore operations
        cleanedItem.setItemMeta(meta);
        
        // Clean stat bonuses from lore (lines with brackets)
        meta = cleanedItem.getItemMeta(); // Get fresh meta
        cleanStatBonusesFromLore(meta);
        cleanedItem.setItemMeta(meta);
        
        // Remove enchantment section from lore (including descriptions)
        meta = cleanedItem.getItemMeta(); // Get fresh meta again
        removeEnchantmentSectionFromLore(meta);
        cleanedItem.setItemMeta(meta);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            debugItemLore(cleanedItem, "AFTER CLEARING");
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "Successfully cleared all enchantments and stat bonuses");
        }
        
        return cleanedItem;
    }

    /**
     * Remove stat bonuses (brackets) from a single stat line - FIXED: Preserve base stats
     */
    private static String removeStatBonuses(String statLine) {
        String cleanLine = ChatColor.stripColor(statLine).trim();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "REMOVING BONUSES: Processing line: " + statLine);
        }
        
        // Check if this line has brackets (enchantment bonuses)
        if (cleanLine.contains("(") && cleanLine.contains(")")) {
            // This line has enchantment bonuses - extract the base value
            
            // Find the stat name
            String statName = null;
            String[] statPrefixes = {
                "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
                "Mana:", "Critical Chance:", "Critical Damage:", "Mining Fortune:", 
                "Mining Speed:", "Farming Fortune:", "Looting Fortune:", "Fishing Fortune:",
                "Build Range:", "Cooldown Reduction:", "Health Regen:", "Speed:", "Luck:"
            };
            
            for (String prefix : statPrefixes) {
                if (cleanLine.contains(prefix)) {
                    statName = prefix;
                    break;
                }
            }
            
            if (statName == null) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                        "REMOVING BONUSES: Unknown stat type, returning null");
                }
                return null; // Unknown stat type
            }
            
            // Extract the total value (before brackets) and enchantment bonus (in brackets)
            String beforeBrackets = cleanLine.substring(0, cleanLine.indexOf("(")).trim();
            String inBrackets = cleanLine.substring(cleanLine.indexOf("(") + 1, cleanLine.indexOf(")")).trim();
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "REMOVING BONUSES: Before brackets: '" + beforeBrackets + "', In brackets: '" + inBrackets + "'");
            }
            
            // Extract total value and bonus value
            String totalValueStr = beforeBrackets.substring(beforeBrackets.indexOf(statName) + statName.length()).trim();
            totalValueStr = totalValueStr.replace("+", "").trim();
            
            String bonusValueStr = inBrackets.replace("+", "").replace("-", "").trim();
            
            try {
                // Calculate base value = total - bonus
                if (isIntegerStat(statName)) {
                    int totalValue = Integer.parseInt(totalValueStr);
                    int bonusValue = Integer.parseInt(bonusValueStr);
                    int baseValue = totalValue - bonusValue;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "REMOVING BONUSES: " + statName + " total=" + totalValue + " bonus=" + bonusValue + " base=" + baseValue);
                    }
                    
                    if (baseValue > 0) {
                        // This stat has a base value - preserve it
                        String colorCode = getStatColorCode(statLine);
                        return colorCode + statName + " " + colorCode + "+" + baseValue;
                    } else {
                        // This stat is purely from enchantments - remove it
                        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                                "REMOVING BONUSES: Purely enchantment stat, removing");
                        }
                        return null;
                    }
                } else {
                    // Handle double stats (Mining Speed, Mining Fortune, etc.)
                    double totalValue = Double.parseDouble(totalValueStr);
                    double bonusValue = Double.parseDouble(bonusValueStr);
                    double baseValue = totalValue - bonusValue;
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "REMOVING BONUSES: " + statName + " total=" + totalValue + " bonus=" + bonusValue + " base=" + baseValue);
                    }
                    
                    if (baseValue > 0) {
                        // This stat has a base value - preserve it
                        String colorCode = getStatColorCode(statLine);
                        return colorCode + statName + " " + colorCode + "+" + String.format("%.1f", baseValue);
                    } else {
                        // This stat is purely from enchantments - remove it
                        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                                "REMOVING BONUSES: Purely enchantment stat, removing");
                        }
                        return null;
                    }
                }
            } catch (NumberFormatException e) {
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                        "REMOVING BONUSES: Failed to parse numbers, keeping original line");
                }
                // If we can't parse the numbers, keep the original line
                return statLine;
            }
        } else {
            // This line has no brackets - it's a base stat, keep it as-is
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "REMOVING BONUSES: No brackets found, keeping as base stat");
            }
            return statLine;
        }
    }

    /**
     * Check if a stat uses integer values - NEW HELPER METHOD
     */
    private static boolean isIntegerStat(String statName) {
        String[] integerStats = {
            "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
            "Mana:", "Critical Chance:", "Critical Damage:", "Cooldown Reduction:", "Speed:", "Luck:"
        };
        
        for (String intStat : integerStats) {
            if (statName.equals(intStat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract color code from the original stat line - NEW HELPER METHOD
     */
    private static String getStatColorCode(String statLine) {
        // Extract the color code from the beginning of the line
        if (statLine.startsWith("§")) {
            return statLine.substring(0, 2); // §c, §7, etc.
        }
        return "§7"; // Default gray
    }

    /**
     * Check if a stat line is effectively empty after cleaning - NEW METHOD
     */
    private static boolean isEmptyStatLine(String cleanStatLine) {
        // Remove common stat prefixes and see if anything meaningful remains
        String[] statPrefixes = {
            "Health:", "Armor:", "Magic Resist:", "Physical Damage:", "Magic Damage:",
            "Mana:", "Critical Chance:", "Critical Damage:", "Mining Fortune:", 
            "Mining Speed:", "Farming Fortune:", "Looting Fortune:", "Fishing Fortune:",
            "Build Range:", "Cooldown Reduction:", "Health Regen:", "Speed:", "Luck:"
        };
        
        for (String prefix : statPrefixes) {
            if (cleanStatLine.startsWith(prefix)) {
                String afterPrefix = cleanStatLine.substring(prefix.length()).trim();
                // If nothing meaningful after the prefix (no +, no numbers), it's empty
                if (afterPrefix.isEmpty() || afterPrefix.equals("+") || afterPrefix.equals("+ §7()")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Remove entire enchantment section from lore - ENHANCED: Remove descriptions too
     */
    private static void removeEnchantmentSectionFromLore(ItemMeta meta) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "REMOVING ENCHANTMENT SECTION: Starting with " + lore.size() + " lines");
        }
        
        List<String> cleanedLore = new ArrayList<>();
        boolean inEnchantSection = false;
        boolean foundEnchantSection = false;
        boolean skipNextBlankLine = false;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String cleanLine = ChatColor.stripColor(line).trim();
            
            // Check if this is the start of enchantment section
            if (line.contains("✦ Enchantments:")) {
                inEnchantSection = true;
                foundEnchantSection = true;
                skipNextBlankLine = true; // Skip the blank line that typically follows
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                    Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                        "Found enchantment section header at line " + i);
                }
                continue; // Skip this line
            }
            
            // If we're in enchant section
            if (inEnchantSection) {
                // ENHANCED: Check for enchantment lines (start with "•") OR description lines (start with "»")
                if (line.contains("•") || line.contains("»")) {
                    // Skip enchantment names and descriptions
                    if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                        Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                            "Skipping enchantment/description line: " + line);
                    }
                    continue;
                }
                
                // Check if this line ends the enchant section
                if (line.trim().isEmpty()) {
                    if (skipNextBlankLine) {
                        // This is the blank line right after enchantments header
                        skipNextBlankLine = false;
                        continue;
                    } else {
                        // This might be the end of the enchant section
                        // Check if the next line (if exists) is non-enchantment content
                        if (i + 1 >= lore.size() || 
                            (!lore.get(i + 1).contains("•") && 
                            !lore.get(i + 1).contains("»") && 
                            !lore.get(i + 1).trim().isEmpty())) {
                            inEnchantSection = false;
                            // Don't add this blank line as it was separating enchantments
                            continue;
                        } else {
                            // Still in enchantments, skip this blank line too
                            continue;
                        }
                    }
                } else if (!line.contains("•") && !line.contains("»") && !cleanLine.isEmpty()) {
                    // Hit non-enchantment content, end of section
                    inEnchantSection = false;
                    cleanedLore.add(line);
                }
                // If none of the above, skip the line (it's part of enchantments)
            } else {
                cleanedLore.add(line);
            }
        }
        
        // Remove the blank line that was before enchantments section if it exists
        if (foundEnchantSection && !cleanedLore.isEmpty()) {
            // Check if last line is blank and should be removed
            int lastIndex = cleanedLore.size() - 1;
            if (lastIndex >= 0 && cleanedLore.get(lastIndex).trim().isEmpty()) {
                // Only remove if the line before it exists and isn't blank
                if (lastIndex > 0 && !cleanedLore.get(lastIndex - 1).trim().isEmpty()) {
                    cleanedLore.remove(lastIndex);
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "ENCHANTMENT SECTION REMOVAL: " + lore.size() + " -> " + cleanedLore.size() + " lines");
        }
        
        meta.setLore(cleanedLore);
    }

    /**
     * Update item lore with enchantments and enhanced stat values
     */
    private static void updateItemLoreWithEnchantmentsAndStats(ItemMeta meta, List<EnchantmentRandomizer.AppliedEnchantment> appliedEnchantments) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Calculate total stat bonuses from all enchantments
        StatBonuses totalBonuses = calculateTotalStatBonuses(appliedEnchantments);
        
        // Update existing stat lines or add new ones
        updateStatsInLore(lore, totalBonuses);
        
        // Add enchantment section
        addEnchantmentSectionToLore(lore, appliedEnchantments);
        
        meta.setLore(lore);
    }

    /**
     * Calculate total stat bonuses from all applied enchantments - FIXED: Correct value storage
     */
    private static StatBonuses calculateTotalStatBonuses(List<EnchantmentRandomizer.AppliedEnchantment> appliedEnchantments) {
        StatBonuses bonuses = new StatBonuses();
        
        for (EnchantmentRandomizer.AppliedEnchantment applied : appliedEnchantments) {
            String enchantmentId = applied.enchantment.getId();
            int level = applied.level;
            
            // Apply stat bonuses based on enchantment type - FIXED: Store as raw values for lore display
            switch (enchantmentId) {
                // Combat Enchantments
                case "savagery":
                    bonuses.physicalDamage += (3 * level); // Fixed damage
                    break;
                case "executioner":
                    bonuses.criticalChance += (5 * level); // Store as percentage points (5, 10, 15...)
                    bonuses.criticalDamage += (10 * level); // Store as percentage points (10, 20, 30...)
                    break;
                case "spell_power":
                    bonuses.magicDamage += (2 * level); // Fixed damage
                    break;
                    
                // Tool Enchantments - FIXED: Store as decimal values (0.5, 0.2, etc.)
                case "prospector":
                    bonuses.miningFortune += (0.5 * level); // Store as 0.5, 1.0, 1.5... for display
                    break;
                case "swiftbreak":
                    bonuses.miningSpeed += (0.2 * level); // Store as 0.2, 0.4, 0.6... for display
                    break;
                case "cultivator":
                    bonuses.farmingFortune += (0.3 * level);
                    break;
                case "treasure_hunter":
                    bonuses.lootingFortune += (0.2 * level);
                    break;
                case "angler":
                    bonuses.fishingFortune += (0.3 * level);
                    break;
                case "architect":
                    bonuses.buildRange += (1.0 * level); // Store as 1.0, 2.0, 3.0...
                    break;
                    
                // Protection Enchantments
                case "fortification":
                    bonuses.armor += (3 * level);
                    break;
                case "warding":
                    bonuses.magicResist += (5 * level);
                    break;
                case "regeneration":
                    bonuses.healthRegen += (0.5 * level); // Store as 0.5, 1.0, 1.5...
                    break;
                    
                // Utility Enchantments
                case "swift":
                    bonuses.speed += (1 * level); // Store as percentage points (1, 2, 3...)
                    break;
                case "lucky":
                    bonuses.luck += level;
                    break;
                    
                // Mystical Enchantments
                case "arcane_power":
                    bonuses.mana += (10 * level);
                    break;
                case "spell_focus":
                    bonuses.cooldownReduction += (5 * level);
                    break;
                case "arcane_mastery":
                    bonuses.mana += (5 * level);
                    bonuses.magicDamage += (2 * level);
                    bonuses.cooldownReduction += level;
                    break;
                    
                // Cursed Enchantments
                case "glass_cannon":
                    bonuses.physicalDamage += (bonuses.physicalDamage * 10 * level / 100);
                    bonuses.magicDamage += (bonuses.magicDamage * 10 * level / 100);
                    break;
                case "mana_burn":
                    bonuses.mana += (5 * level);
                    break;
            }
        }
        
        return bonuses;
    }

    /**
     * Update stat line with integer bonus - FIXED: Use original colors and preserve integer format
     */
    private static String updateStatWithBonus(String line, String statName, String color, int bonus) {
        String baseValueStr = extractBaseStatValue(line, statName);
        
        try {
            int baseValue = Integer.parseInt(baseValueStr);
            int totalValue = baseValue + bonus;
            String bonusStr = (bonus > 0 ? "+" : "") + bonus;
            
            // CRITICAL FIX: Use original line's color, not passed color
            String originalColor = getStatColorCode(line);
            
            // CRITICAL FIX: Keep integer format for integer stats
            return originalColor + statName + " " + originalColor + "+" + totalValue + " §7(" + bonusStr + ")";
        } catch (NumberFormatException e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "Failed to parse base value for " + statName + " from line: " + line);
            }
            return line;
        }
    }

    /**
     * Update stat line with double bonus - FIXED: Always show total as decimal when bonus makes it decimal
     */
    private static String updateStatWithDoubleBonus(String line, String statName, String color, double bonus) {
        String baseValueStr = extractBaseStatValue(line, statName);
        
        try {
            // CRITICAL FIX: Check if original value is integer or decimal
            boolean isOriginalInteger = !baseValueStr.contains(".");
            String originalColor = getStatColorCode(line);
            
            if (isOriginalInteger) {
                // Original was integer (like Mining Fortune: +5), but we need to check if total becomes decimal
                int baseValue = Integer.parseInt(baseValueStr);
                double totalValue = baseValue + bonus;
                String bonusStr = (bonus > 0 ? "+" : "") + String.format("%.1f", bonus);
                
                // CRITICAL FIX: If the total is not a whole number, display as decimal
                if (totalValue == Math.floor(totalValue)) {
                    // Total is a whole number, display as integer
                    return originalColor + statName + " " + originalColor + "+" + (int)totalValue + " §7(" + bonusStr + ")";
                } else {
                    // Total is not a whole number, display as decimal
                    return originalColor + statName + " " + originalColor + "+" + String.format("%.1f", totalValue) + " §7(" + bonusStr + ")";
                }
            } else {
                // Original was decimal (like Mining Speed: +0.3), keep as decimal
                double baseValue = Double.parseDouble(baseValueStr);
                double totalValue = baseValue + bonus;
                String bonusStr = (bonus > 0 ? "+" : "") + String.format("%.1f", bonus);
                
                return originalColor + statName + " " + originalColor + "+" + String.format("%.1f", totalValue) + " §7(" + bonusStr + ")";
            }
        } catch (NumberFormatException e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "Failed to parse base double value for " + statName + " from line: " + line);
            }
            return line;
        }
    }

    /**
     * Update a single stat line with enchantment bonuses - FIXED: Don't pass color parameter
     */
    private static String updateStatLine(String line, StatBonuses bonuses) {
        // First, clean any existing bonuses from the line
        String cleanedLine = removeStatBonuses(line);
        
        // If the line was marked for removal (enchantment-only stat), return null
        if (cleanedLine == null) {
            return null;
        }
        
        // Now check if we need to add new bonuses - DON'T PASS COLOR PARAMETER
        // Health stat
        if (line.contains("Health: ")) {
            if (bonuses.health != 0) {
                return updateStatWithBonus(cleanedLine, "Health:", null, bonuses.health);
            }
        }
        // Armor stat
        else if (line.contains("Armor: ")) {
            if (bonuses.armor != 0) {
                return updateStatWithBonus(cleanedLine, "Armor:", null, bonuses.armor);
            }
        }
        // Magic Resist stat
        else if (line.contains("Magic Resist: ")) {
            if (bonuses.magicResist != 0) {
                return updateStatWithBonus(cleanedLine, "Magic Resist:", null, bonuses.magicResist);
            }
        }
        // Physical Damage stat
        else if (line.contains("Physical Damage: ")) {
            if (bonuses.physicalDamage != 0) {
                return updateStatWithBonus(cleanedLine, "Physical Damage:", null, bonuses.physicalDamage);
            }
        }
        // Magic Damage stat
        else if (line.contains("Magic Damage: ")) {
            if (bonuses.magicDamage != 0) {
                return updateStatWithBonus(cleanedLine, "Magic Damage:", null, bonuses.magicDamage);
            }
        }
        // Mana stat
        else if (line.contains("Mana: ")) {
            if (bonuses.mana != 0) {
                return updateStatWithBonus(cleanedLine, "Mana:", null, bonuses.mana);
            }
        }
        // Critical Chance stat - FIXED: Use integer method to preserve original format
        else if (line.contains("Critical Chance: ")) {
            if (bonuses.criticalChance != 0) {
                return updateStatWithBonus(cleanedLine, "Critical Chance:", null, bonuses.criticalChance);
            }
        }
        // Critical Damage stat - FIXED: Use integer method to preserve original format
        else if (line.contains("Critical Damage: ")) {
            if (bonuses.criticalDamage != 0) {
                return updateStatWithBonus(cleanedLine, "Critical Damage:", null, bonuses.criticalDamage);
            }
        }
        // For existing Mining Fortune - CRITICAL FIX: Preserve original format
        else if (line.contains("Mining Fortune: ")) {
            if (bonuses.miningFortune != 0) {
                return updateStatWithDoubleBonus(cleanedLine, "Mining Fortune:", null, bonuses.miningFortune);
            }
        }
        // For existing Mining Speed - FIXED: Preserve original format
        else if (line.contains("Mining Speed: ")) {
            if (bonuses.miningSpeed != 0) {
                return updateStatWithDoubleBonus(cleanedLine, "Mining Speed:", null, bonuses.miningSpeed);
            }
        }
        
        // Return the cleaned line (without bonuses) if no new bonuses to add
        return cleanedLine;
    }

    /**
     * Create new stat lines for stats that only come from enchantments - FIXED: Use correct original colors
     */
    private static List<String> createNewStatLines(List<String> existingLore, StatBonuses bonuses, Set<String> processedStats) {
        List<String> newLines = new ArrayList<>();
        
        // Mining Fortune - NEW STAT: Use original color (§6 for gold)
        if (bonuses.miningFortune != 0 && !processedStats.contains("Mining Fortune:")) {
            String bonusStr = (bonuses.miningFortune > 0 ? "+" : "") + String.format("%.1f", bonuses.miningFortune);
            newLines.add("§6Mining Fortune: §6" + bonusStr + " §7(" + bonusStr + ")");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Created new Mining Fortune stat: " + bonusStr);
            }
        }
        
        // Mining Speed - NEW STAT: Use original color (§9 for blue)
        if (bonuses.miningSpeed != 0 && !processedStats.contains("Mining Speed:")) {
            String bonusStr = (bonuses.miningSpeed > 0 ? "+" : "") + String.format("%.1f", bonuses.miningSpeed);
            newLines.add("§9Mining Speed: §9" + bonusStr + " §7(" + bonusStr + ")");
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, "Created new Mining Speed stat: " + bonusStr);
            }
        }
        
        // Continue for other new stats with appropriate colors...
        if (bonuses.farmingFortune != 0 && !processedStats.contains("Farming Fortune:")) {
            String bonusStr = (bonuses.farmingFortune > 0 ? "+" : "") + String.format("%.1f", bonuses.farmingFortune);
            newLines.add("§aFarming Fortune: §a" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.lootingFortune != 0 && !processedStats.contains("Looting Fortune:")) {
            String bonusStr = (bonuses.lootingFortune > 0 ? "+" : "") + String.format("%.1f", bonuses.lootingFortune);
            newLines.add("§eLootingFortune: §e" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.fishingFortune != 0 && !processedStats.contains("Fishing Fortune:")) {
            String bonusStr = (bonuses.fishingFortune > 0 ? "+" : "") + String.format("%.1f", bonuses.fishingFortune);
            newLines.add("§bFishing Fortune: §b" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.buildRange != 0 && !processedStats.contains("Build Range:")) {
            String bonusStr = (bonuses.buildRange > 0 ? "+" : "") + String.format("%.1f", bonuses.buildRange);
            newLines.add("§dBuild Range: §d" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.cooldownReduction != 0 && !processedStats.contains("Cooldown Reduction:")) {
            String bonusStr = (bonuses.cooldownReduction > 0 ? "+" : "") + bonuses.cooldownReduction;
            newLines.add("§3Cooldown Reduction: §3" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.healthRegen != 0 && !processedStats.contains("Health Regen:")) {
            String bonusStr = (bonuses.healthRegen > 0 ? "+" : "") + String.format("%.1f", bonuses.healthRegen);
            newLines.add("§cHealth Regen: §c" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.speed != 0 && !processedStats.contains("Speed:")) {
            String bonusStr = (bonuses.speed > 0 ? "+" : "") + bonuses.speed;
            newLines.add("§fSpeed: §f" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        if (bonuses.luck != 0 && !processedStats.contains("Luck:")) {
            String bonusStr = (bonuses.luck > 0 ? "+" : "") + bonuses.luck;
            newLines.add("§aLuck: §a" + bonusStr + " §7(" + bonusStr + ")");
        }
        
        return newLines;
    }

    /**
     * Add enchantment section to lore - FIXED: Proper placement after stats
     */
    private static void addEnchantmentSectionToLore(List<String> lore, List<EnchantmentRandomizer.AppliedEnchantment> appliedEnchantments) {
        // Find insertion point - should be right after the last stat line
        int insertIndex = findEnchantmentInsertionPoint(lore);
        
        // Add blank line before enchantments ONLY if previous line is not blank and has content
        boolean needsBlankLine = false;
        if (insertIndex > 0) {
            String previousLine = lore.get(insertIndex - 1).trim();
            // Only add blank line if previous line has actual stat content
            if (!previousLine.isEmpty() && !previousLine.equals("Stats:")) {
                needsBlankLine = true;
            }
        }
        
        if (needsBlankLine) {
            lore.add(insertIndex, "");
            insertIndex++;
        }
        
        lore.add(insertIndex, ChatColor.LIGHT_PURPLE + "✦ Enchantments:");
        insertIndex++;
        
        // Add each enchantment
        for (EnchantmentRandomizer.AppliedEnchantment applied : appliedEnchantments) {
            String enchantmentLine = ChatColor.GRAY + "• " + applied.enchantment.getFormattedName(applied.level);
            lore.add(insertIndex, enchantmentLine);
            insertIndex++;
        }
    }


    /**
     * Find where to insert enchantment section - ENHANCED
     */
    private static int findEnchantmentInsertionPoint(List<String> lore) {
        // Look for the end of the stats section
        boolean foundStats = false;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            
            if (line.contains("Stats:")) {
                foundStats = true;
                continue;
            }
            
            // If we found stats section and hit an empty line or new section, insert here
            if (foundStats && (line.trim().isEmpty() || line.contains("✦") || line.contains("Passive:") || line.contains("§8"))) {
                return i;
            }
        }
        
        // If no stats section found, insert after rarity
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Rarity:")) {
                // Insert after rarity and any description lines
                int insertPoint = i + 1;
                while (insertPoint < lore.size() && 
                    (lore.get(insertPoint).startsWith("§7\"") || lore.get(insertPoint).trim().isEmpty())) {
                    insertPoint++;
                }
                return insertPoint;
            }
        }
        
        // Fallback - insert at end
        return lore.size();
    }

    /**
     * Find where to insert stats section if it doesn't exist
     */
    private static int findStatsInsertionPoint(List<String> lore) {
        // Insert after rarity but before enchantments or flavor text
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Rarity:")) {
                return i + 1;
            }
        }
        
        // If no rarity found, insert at beginning
        return 0;
    }


    /**
     * Check if a stat already exists in the stats lines
     */
    private static boolean hasStatInLines(List<String> lines, String statName) {
        return lines.stream().anyMatch(line -> ChatColor.stripColor(line).contains(statName));
    }

    /**
     * Update stat line with percentage bonus - FIXED: No % suffix for existing stats
     */
    private static String updateStatWithPercentageBonus(String line, String statName, String color, double bonusPercent) {
        String baseValueStr = extractBaseStatValue(line, statName).replace("%", "");
        
        try {
            double baseValue = Double.parseDouble(baseValueStr);
            double totalValue = baseValue + bonusPercent;
            String bonusStr = (bonusPercent > 0 ? "+" : "") + String.format("%.1f", bonusPercent);
            
            // CRITICAL FIX: Check if original line had % suffix
            boolean originalHadPercent = line.contains("%");
            if (originalHadPercent) {
                return color + statName + " " + color + "+" + String.format("%.1f", totalValue) + "% §7(" + bonusStr + "%)";
            } else {
                // If original had no %, don't add %
                return color + statName + " " + color + "+" + String.format("%.1f", totalValue) + " §7(" + bonusStr + ")";
            }
        } catch (NumberFormatException e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "Failed to parse base percentage value for " + statName + " from line: " + line);
            }
            return line;
        }
    }

    /**
     * Update stat line with multiplier bonus - FIXED: No x suffix for existing stats
     */
    private static String updateStatWithMultiplierBonus(String line, String statName, String color, double bonusMultiplier) {
        String baseValueStr = extractBaseStatValue(line, statName).replace("x", "");
        
        try {
            double baseValue = Double.parseDouble(baseValueStr);
            double totalValue = baseValue + bonusMultiplier;
            String bonusStr = (bonusMultiplier > 0 ? "+" : "") + String.format("%.1f", bonusMultiplier);
            
            // CRITICAL FIX: Check if original line had x suffix
            boolean originalHadX = line.contains("x");
            if (originalHadX) {
                return color + statName + " " + color + "+" + String.format("%.1f", totalValue) + "x §7(" + bonusStr + "x)";
            } else {
                // If original had no x, don't add x
                return color + statName + " " + color + "+" + String.format("%.1f", totalValue) + " §7(" + bonusStr + ")";
            }
        } catch (NumberFormatException e) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, 
                    "Failed to parse base multiplier value for " + statName + " from line: " + line);
            }
            return line;
        }
    }
    
    /**
     * Check if item has conflicting enchantments
     */
    public static boolean hasConflictingEnchantment(ItemStack item, CustomEnchantment newEnchantment) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for conflicts with existing custom enchantments
        for (String conflictId : newEnchantment.getConflictingEnchantments()) {
            NamespacedKey conflictKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + conflictId);
            if (meta.getPersistentDataContainer().has(conflictKey, PersistentDataType.INTEGER)) {
                return true;
            }
        }
        
        // Check if trying to apply same enchantment (should upgrade instead)
        NamespacedKey enchantmentKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + newEnchantment.getId());
        return meta.getPersistentDataContainer().has(enchantmentKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Get all custom enchantments on an item
     */
    public static Map<String, Integer> getCustomEnchantments(ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return enchantments;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check all registered enchantments
        for (CustomEnchantment enchantment : CustomEnchantmentRegistry.getInstance().getAllEnchantments()) {
            NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantment.getId());
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
                int level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                enchantments.put(enchantment.getId(), level);
            }
        }
        
        return enchantments;
    }
    
    /**
     * Get the level of a specific enchantment on an item
     */
    public static int getEnchantmentLevel(ItemStack item, String enchantmentId) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantmentId);
        
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Remove a custom enchantment from an item - FIXED: Proper meta handling
     */
    public static ItemStack removeEnchantment(ItemStack item, String enchantmentId) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack modifiedItem = item.clone();
        ItemMeta meta = modifiedItem.getItemMeta();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "REMOVING ENCHANTMENT: " + enchantmentId + " from item");
            debugItemLore(modifiedItem, "BEFORE REMOVAL");
        }
        
        // Check if the item has this enchantment
        NamespacedKey enchKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantmentId);
        if (!meta.getPersistentDataContainer().has(enchKey, PersistentDataType.INTEGER)) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "Item does not have enchantment: " + enchantmentId);
            }
            return modifiedItem; // Item doesn't have this enchantment
        }
        
        // Get the enchantment for reference
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        
        // Remove the enchantment from persistent data
        meta.getPersistentDataContainer().remove(enchKey);
        
        // Get all remaining enchantments
        List<EnchantmentRandomizer.AppliedEnchantment> remainingEnchantments = new ArrayList<>();
        Map<String, Integer> allEnchantments = getCustomEnchantments(modifiedItem);
        allEnchantments.remove(enchantmentId); // Remove the one we're deleting
        
        for (Map.Entry<String, Integer> entry : allEnchantments.entrySet()) {
            CustomEnchantment ench = CustomEnchantmentRegistry.getInstance().getEnchantment(entry.getKey());
            if (ench != null) {
                remainingEnchantments.add(new EnchantmentRandomizer.AppliedEnchantment(ench, entry.getValue()));
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "Remaining enchantments after removal: " + remainingEnchantments.size());
        }
        
        // CRITICAL FIX: Apply the meta changes first, then clear
        modifiedItem.setItemMeta(meta);
        
        // CRITICAL FIX: Completely rebuild the item lore from scratch
        if (remainingEnchantments.isEmpty()) {
            // No enchantments left - remove all enchantment-related content
            modifiedItem = clearAllEnchantmentsAndStatBonuses(modifiedItem);
            
            // FIXED: Get the meta after clearing and remove visual glint effect
            meta = modifiedItem.getItemMeta();
            if (meta != null) {
                meta.removeEnchant(org.bukkit.enchantments.Enchantment.PROTECTION);
                modifiedItem.setItemMeta(meta);
            }
        } else {
            // Rebuild the item with remaining enchantments
            // First, clean all existing enchantment content
            modifiedItem = clearAllEnchantmentsAndStatBonuses(modifiedItem);
            meta = modifiedItem.getItemMeta();
            
            if (meta != null) {
                // Re-add all remaining enchantments to persistent data
                for (EnchantmentRandomizer.AppliedEnchantment applied : remainingEnchantments) {
                    NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + applied.enchantment.getId());
                    meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, applied.level);
                }
                
                // Apply visual glint effect
                meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                
                modifiedItem.setItemMeta(meta);
                
                // Rebuild lore with remaining enchantments and stats
                meta = modifiedItem.getItemMeta();
                if (meta != null) {
                    updateItemLoreWithEnchantmentsAndStats(meta, remainingEnchantments);
                    modifiedItem.setItemMeta(meta);
                }
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            debugItemLore(modifiedItem, "AFTER REMOVAL");
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "Successfully removed enchantment: " + enchantmentId);
        }
        
        return modifiedItem;
    }
    
    /**
     * Add enchantment to item lore
     */
    private static void addEnchantmentToLore(ItemMeta meta, CustomEnchantment enchantment, int level) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Find insertion point for enchantments
        int insertIndex = findEnchantmentInsertionPoint(lore);
        
        // Add enchantment section header if needed
        if (!hasEnchantmentSection(lore)) {
            lore.add(insertIndex, "");
            lore.add(insertIndex + 1, ChatColor.LIGHT_PURPLE + "✦ Enchantments:");
            insertIndex += 2;
        }
        
        // Add the enchantment
        String enchantmentLine = ChatColor.GRAY + "• " + enchantment.getFormattedName(level);
        lore.add(insertIndex, enchantmentLine);
        
        // Add enchantment description if it's the first enchantment
        if (countEnchantmentsInLore(lore) == 1) {
            lore.add(insertIndex + 1, "");
            lore.add(insertIndex + 2, ChatColor.DARK_GRAY + "» " + enchantment.getDescription());
        }
        
        meta.setLore(lore);
    }
    
    /**
     * Remove enchantment from item lore
     */
    private static void removeEnchantmentFromLore(ItemMeta meta, String enchantmentId) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null) {
            return;
        }
        
        // Remove the enchantment line
        lore.removeIf(line -> line.contains(enchantment.getDisplayName()));
        
        // Clean up enchantment section if empty
        if (countEnchantmentsInLore(lore) == 0) {
            // Remove enchantment header and empty lines
            for (int i = lore.size() - 1; i >= 0; i--) {
                String line = lore.get(i);
                if (line.contains("✦ Enchantments:") || 
                    (line.trim().isEmpty() && i > 0 && lore.get(i - 1).contains("✦ Enchantments:"))) {
                    lore.remove(i);
                }
            }
        }
        
        meta.setLore(lore);
    }
    
    /**
     * Check if lore already has an enchantment section
     */
    private static boolean hasEnchantmentSection(List<String> lore) {
        return lore.stream().anyMatch(line -> line.contains("✦ Enchantments:"));
    }
    
    /**
     * Count enchantments in lore
     */
    private static int countEnchantmentsInLore(List<String> lore) {
        int count = 0;
        boolean inEnchantSection = false;
        
        for (String line : lore) {
            if (line.contains("✦ Enchantments:")) {
                inEnchantSection = true;
                continue;
            }
            
            if (inEnchantSection) {
                if (line.trim().isEmpty() && !line.contains("•")) {
                    break; // End of enchant section
                }
                if (line.contains("•")) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Check if an item has any custom enchantments
     */
    public static boolean hasCustomEnchantments(ItemStack item) {
        return !getCustomEnchantments(item).isEmpty();
    }
    
    /**
     * Get a formatted list of all enchantments on an item
     */
    public static List<String> getEnchantmentLore(ItemStack item) {
        List<String> enchantmentLore = new ArrayList<>();
        Map<String, Integer> enchantments = getCustomEnchantments(item);
        
        if (enchantments.isEmpty()) {
            return enchantmentLore;
        }
        
        enchantmentLore.add(ChatColor.LIGHT_PURPLE + "✦ Enchantments:");
        
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(entry.getKey());
            if (enchantment != null) {
                int level = entry.getValue();
                enchantmentLore.add(ChatColor.GRAY + "• " + enchantment.getFormattedName(level));
            }
        }
        
        return enchantmentLore;
    }
    
    /**
     * Upgrade an existing enchantment on an item
     */
    public static ItemStack upgradeEnchantment(ItemStack item, String enchantmentId, int newLevel) {
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null) {
            return item;
        }
        
        // Remove old enchantment
        ItemStack upgradedItem = removeEnchantment(item, enchantmentId);
        
        // Apply new level
        return applyEnchantment(upgradedItem, enchantment, Math.min(newLevel, enchantment.getMaxLevel()));
    }
    
    /**
     * Clear all custom enchantments from an item
     */
    public static ItemStack clearAllEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack clearedItem = item.clone();
        Map<String, Integer> enchantments = getCustomEnchantments(clearedItem);
        
        for (String enchantmentId : enchantments.keySet()) {
            clearedItem = removeEnchantment(clearedItem, enchantmentId);
        }
        
        return clearedItem;
    }
}