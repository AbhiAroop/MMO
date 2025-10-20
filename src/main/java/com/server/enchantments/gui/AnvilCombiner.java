package com.server.enchantments.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.items.ElementalFragment;
import com.server.enchantments.items.EnchantmentTome;
import com.server.enchantments.utils.EquipmentTypeValidator;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Handles the logic for combining items in the custom anvil.
 */
public class AnvilCombiner {
    
    private static final Random RANDOM = new Random();
    
    // Base costs
    private static final int BASE_XP_COST = 5;
    private static final int BASE_ESSENCE_COST = 100;
    
    /**
     * Calculates the result of combining two items.
     * Returns a CombineResult with the output item and costs.
     */
    public static CombineResult calculateResult(ItemStack input1, ItemStack input2) {
        if (input1 == null || input2 == null) {
            return null;
        }
        
        // Debug: Log what we're trying to combine
        System.out.println("[Anvil] [DEBUG] === Calculate Result ===");
        System.out.println("[Anvil] [DEBUG] Input1: " + input1.getType() + ", isEnchantedTome: " + EnchantmentTome.isEnchantedTome(input1) + ", isCustomItem: " + isCustomItem(input1));
        System.out.println("[Anvil] [DEBUG] Input2: " + input2.getType() + ", isEnchantedTome: " + EnchantmentTome.isEnchantedTome(input2) + ", isCustomItem: " + isCustomItem(input2));
        
        // Case 1: Two Enchanted Tomes (check BEFORE identical items!)
        if (EnchantmentTome.isEnchantedTome(input1) && EnchantmentTome.isEnchantedTome(input2)) {
            System.out.println("[Anvil] [DEBUG] -> Combining two tomes");
            return combineTomes(input1, input2);
        }
        
        // Case 2: Enchanted Tome + Fragments (boost apply chance)
        if (EnchantmentTome.isEnchantedTome(input1) && ElementalFragment.isFragment(input2)) {
            System.out.println("[Anvil] [DEBUG] -> Boosting tome with fragments");
            return boostTomeWithFragments(input1, input2);
        }
        if (ElementalFragment.isFragment(input1) && EnchantmentTome.isEnchantedTome(input2)) {
            System.out.println("[Anvil] [DEBUG] -> Boosting tome with fragments");
            return boostTomeWithFragments(input2, input1);
        }
        
        // Case 3: Item + Enchanted Tome
        if (isCustomItem(input1) && EnchantmentTome.isEnchantedTome(input2)) {
            System.out.println("[Anvil] [DEBUG] -> Applying tome to item (item in slot 1)");
            return applyTomeToItem(input1, input2);
        }
        if (EnchantmentTome.isEnchantedTome(input1) && isCustomItem(input2)) {
            System.out.println("[Anvil] [DEBUG] -> Applying tome to item (tome in slot 1)");
            return applyTomeToItem(input2, input1);
        }
        
        // Case 4: Two identical items (same material and custom model data)
        if (areIdenticalItems(input1, input2)) {
            System.out.println("[Anvil] [DEBUG] -> Combining two identical items");
            return combineIdenticalItems(input1, input2);
        }
        
        // Invalid combination
        System.out.println("[Anvil] [DEBUG] -> No valid combination found");
        return null;
    }
    
    /**
     * Calculates preview result (shows all enchantments as if they succeeded).
     * Returns result with a hasUncertainty flag if any enchants have <100% success.
     * This is used for the GUI preview - actual RNG is rolled when taking the item.
     */
    public static PreviewResult calculatePreview(ItemStack input1, ItemStack input2) {
        if (input1 == null || input2 == null) {
            return null;
        }
        
        // For tome + item combinations, show ALL enchants as if they succeeded
        if ((isCustomItem(input1) && EnchantmentTome.isEnchantedTome(input2)) ||
            (EnchantmentTome.isEnchantedTome(input1) && isCustomItem(input2))) {
            
            ItemStack item = isCustomItem(input1) ? input1 : input2;
            ItemStack tome = EnchantmentTome.isEnchantedTome(input1) ? input1 : input2;
            
            return previewTomeApplication(item, tome);
        }
        
        // For all other combinations, use normal calculation (no RNG involved)
        CombineResult normalResult = calculateResult(input1, input2);
        if (normalResult == null) {
            return null;
        }
        
        // No uncertainty for non-tome combinations
        return new PreviewResult(normalResult.getResult(), normalResult.getXpCost(), 
                                normalResult.getEssenceCost(), false);
    }
    
    /**
     * Creates a preview of tome application showing all enchantments.
     * Marks as uncertain if any have <100% apply chance.
     */
    private static PreviewResult previewTomeApplication(ItemStack item, ItemStack tome) {
        List<EnchantmentData> tomeEnchants = EnchantmentData.getEnchantmentsFromItem(tome);
        
        if (tomeEnchants.isEmpty()) {
            return null;
        }
        
        // Get apply chances
        NBTItem tomeNBT = new NBTItem(tome);
        boolean hasUncertainty = false;
        int totalEnchants = 0;
        int compatibleCount = 0;
        
        ItemStack result = item.clone();
        result.setAmount(1);
        
        com.server.enchantments.EnchantmentRegistry registry = 
            com.server.enchantments.EnchantmentRegistry.getInstance();
        
        // Apply ALL enchantments for preview (no RNG)
        for (int i = 0; i < tomeEnchants.size(); i++) {
            EnchantmentData enchant = tomeEnchants.get(i);
            totalEnchants++;
            
            // Get enchantment object
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchant.getEnchantmentId());
            if (enchantObj == null) {
                continue; // Skip incompatible
            }
            
            // Check compatibility
            if (!isEnchantmentCompatible(result, enchantObj)) {
                continue; // Skip incompatible
            }
            
            compatibleCount++;
            
            // Check apply chance
            String prefix = "MMO_Enchant_" + i + "_";
            int applyChance = tomeNBT.getInteger(prefix + "ApplyChance");
            if (applyChance < 100) {
                hasUncertainty = true;
            }
            
            // Apply the enchantment (for preview)
            EnchantmentData.addEnchantmentToItem(result, enchantObj, 
                enchant.getQuality(), enchant.getLevel(), null);
        }
        
        // If no compatible enchants, return null
        if (compatibleCount == 0) {
            return null;
        }
        
        // Calculate cost based on ALL compatible enchants
        int xpCost = BASE_XP_COST * compatibleCount;
        int essenceCost = BASE_ESSENCE_COST * compatibleCount;
        
        return new PreviewResult(result, xpCost, essenceCost, hasUncertainty);
    }
    
    /**
     * Checks if two items are identical (same type and custom model data).
     */
    private static boolean areIdenticalItems(ItemStack item1, ItemStack item2) {
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null || meta2 == null) {
            return false;
        }
        
        if (!meta1.hasCustomModelData() || !meta2.hasCustomModelData()) {
            return false;
        }
        
        return meta1.getCustomModelData() == meta2.getCustomModelData();
    }
    
    /**
     * Checks if an item is a custom item (has custom model data, not a tome/fragment).
     */
    private static boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        if (EnchantmentTome.isEnchantedTome(item) || EnchantmentTome.isUnenchantedTome(item)) {
            return false;
        }
        
        if (ElementalFragment.isFragment(item)) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData();
    }
    
    /**
     * Combines two identical items by merging their enchantments.
     */
    private static CombineResult combineIdenticalItems(ItemStack item1, ItemStack item2) {
        List<EnchantmentData> enchants1 = EnchantmentData.getEnchantmentsFromItem(item1);
        List<EnchantmentData> enchants2 = EnchantmentData.getEnchantmentsFromItem(item2);
        
        // Result starts as a clone of item1 (preserves name, lore, custom model data)
        ItemStack result = item1.clone();
        result.setAmount(1);
        
        // Store original metadata before clearing enchantments
        ItemMeta originalMeta = result.getItemMeta();
        String displayName = (originalMeta != null && originalMeta.hasDisplayName()) ? originalMeta.getDisplayName() : null;
        List<String> baseLore = extractBaseLore(originalMeta); // Extract only non-enchantment lore
        Integer customModelData = (originalMeta != null && originalMeta.hasCustomModelData()) ? originalMeta.getCustomModelData() : null;
        
        // Clear existing enchantments
        EnchantmentData.clearEnchantments(result);
        
        // Merge enchantments
        Map<String, EnchantmentData> mergedEnchants = new HashMap<>();
        
        // Add all from item1
        for (EnchantmentData enchant : enchants1) {
            mergedEnchants.put(enchant.getEnchantmentId(), enchant);
        }
        
        // Merge with item2
        for (EnchantmentData enchant2 : enchants2) {
            String id = enchant2.getEnchantmentId();
            
            if (mergedEnchants.containsKey(id)) {
                EnchantmentData enchant1 = mergedEnchants.get(id);
                
                // Same enchantment - try to upgrade
                EnchantmentData upgraded = tryUpgradeEnchantment(enchant1, enchant2);
                mergedEnchants.put(id, upgraded);
            } else {
                // New enchantment - add it
                mergedEnchants.put(id, enchant2);
            }
        }
        
        // Apply merged enchantments to result
        com.server.enchantments.EnchantmentRegistry registry = 
            com.server.enchantments.EnchantmentRegistry.getInstance();
        
        // Track which need level upgrade
        Map<String, Boolean> needsUpgrade = new HashMap<>();
        for (String id : mergedEnchants.keySet()) {
            EnchantmentData e1 = null, e2 = null;
            for (EnchantmentData e : enchants1) {
                if (e.getEnchantmentId().equals(id)) {
                    e1 = e;
                    break;
                }
            }
            for (EnchantmentData e : enchants2) {
                if (e.getEnchantmentId().equals(id)) {
                    e2 = e;
                    break;
                }
            }
            // Mark for upgrade if both exist with same level and quality
            if (e1 != null && e2 != null && 
                e1.getLevel() == e2.getLevel() && 
                e1.getQuality() == e2.getQuality() &&
                e1.getLevel().ordinal() < EnchantmentLevel.VIII.ordinal()) {
                // Check if upgrade would exceed max level
                com.server.enchantments.data.CustomEnchantment enchantObj = 
                    registry.getEnchantment(id);
                if (enchantObj != null) {
                    EnchantmentLevel nextLevel = EnchantmentLevel.values()[e1.getLevel().ordinal() + 1];
                    // getMaxLevel() returns int (0-based), compare with ordinal
                    if (nextLevel.ordinal() <= enchantObj.getMaxLevel()) {
                        needsUpgrade.put(id, true);
                    }
                }
            }
        }
        
        for (EnchantmentData enchant : mergedEnchants.values()) {
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchant.getEnchantmentId());
            if (enchantObj != null) {
                // Check if this needs level upgrade
                EnchantmentLevel levelToApply = enchant.getLevel();
                if (needsUpgrade.getOrDefault(enchant.getEnchantmentId(), false)) {
                    // Upgrade to next level
                    levelToApply = EnchantmentLevel.values()[enchant.getLevel().ordinal() + 1];
                }
                
                EnchantmentData.addEnchantmentToItem(result, enchantObj, 
                    enchant.getQuality(), levelToApply, null);
            }
        }
        
        // Restore original item metadata (name, lore, custom model data)
        ItemMeta finalMeta = result.getItemMeta();
        if (finalMeta != null) {
            if (displayName != null) {
                finalMeta.setDisplayName(displayName);
            }
            if (customModelData != null) {
                finalMeta.setCustomModelData(customModelData);
            }
            // Restore base lore (non-enchantment lore)
            // The enchantment lore is already added by addEnchantmentToItem -> updateItemLore
            if (baseLore != null && !baseLore.isEmpty()) {
                List<String> currentLore = finalMeta.hasLore() ? new ArrayList<>(finalMeta.getLore()) : new ArrayList<>();
                // Prepend base lore before enchantment lore
                List<String> combinedLore = new ArrayList<>(baseLore);
                combinedLore.addAll(currentLore);
                finalMeta.setLore(combinedLore);
            }
            result.setItemMeta(finalMeta);
        }
        
        // Calculate cost based on number of enchantments
        int totalEnchants = mergedEnchants.size();
        int xpCost = BASE_XP_COST * totalEnchants;
        int essenceCost = BASE_ESSENCE_COST * totalEnchants;
        
        return new CombineResult(result, xpCost, essenceCost);
    }
    
    /**
     * Tries to upgrade an enchantment by combining two instances.
     * If same level and quality, and not maxed, increase level by 1.
     * Otherwise, pick higher quality or higher level.
     * Note: Returns one of the existing EnchantmentData objects, doesn't create new ones.
     */
    private static EnchantmentData tryUpgradeEnchantment(EnchantmentData enchant1, EnchantmentData enchant2) {
        EnchantmentLevel level1 = enchant1.getLevel();
        EnchantmentLevel level2 = enchant2.getLevel();
        EnchantmentQuality quality1 = enchant1.getQuality();
        EnchantmentQuality quality2 = enchant2.getQuality();
        
        // If same level and quality, we'll need to return the higher level version
        // Since we can't create new EnchantmentData, we return enchant1 and note that
        // the calling code should handle level upgrade manually
        if (level1 == level2 && quality1 == quality2) {
            // Check if not maxed - return enchant1, caller will need to apply at next level
            if (level1.ordinal() < EnchantmentLevel.VIII.ordinal()) {
                // Signal upgrade needed by returning enchant1
                // The caller should check this and apply at (level + 1)
                return enchant1;
            }
        }
        
        // Pick better quality
        if (quality1.ordinal() > quality2.ordinal()) {
            return enchant1;
        } else if (quality2.ordinal() > quality1.ordinal()) {
            return enchant2;
        }
        
        // Same quality - pick higher level
        if (level1.ordinal() > level2.ordinal()) {
            return enchant1;
        } else {
            return enchant2;
        }
    }
    
    /**
     * Applies an enchanted tome to an item.
     * Uses apply chance and compatibility checking.
     */
    private static CombineResult applyTomeToItem(ItemStack item, ItemStack tome) {
        List<EnchantmentData> tomeEnchants = EnchantmentData.getEnchantmentsFromItem(tome);
        
        System.out.println("[Anvil] [DEBUG] === Apply Tome to Item ===");
        System.out.println("[Anvil] [DEBUG] Item: " + item.getType());
        System.out.println("[Anvil] [DEBUG] Tome enchants: " + tomeEnchants.size());
        
        if (tomeEnchants.isEmpty()) {
            System.out.println("[Anvil] [DEBUG] No enchants on tome, returning null");
            return null;
        }
        
        // Get apply chances from tome
        NBTItem tomeNBT = new NBTItem(tome);
        Map<Integer, Integer> applyChances = new HashMap<>();
        for (int i = 0; i < tomeEnchants.size(); i++) {
            String prefix = "MMO_Enchant_" + i + "_";
            int applyChance = tomeNBT.getInteger(prefix + "ApplyChance");
            applyChances.put(i, applyChance);
            System.out.println("[Anvil] [DEBUG] Enchant " + i + " (" + tomeEnchants.get(i).getEnchantmentId() + ") apply chance: " + applyChance);
        }
        
        ItemStack result = item.clone();
        result.setAmount(1);
        
        int successCount = 0;
        int failCount = 0;
        int incompatibleCount = 0;
        
        com.server.enchantments.EnchantmentRegistry registry = 
            com.server.enchantments.EnchantmentRegistry.getInstance();
        
        // Try to apply each enchantment
        for (int i = 0; i < tomeEnchants.size(); i++) {
            EnchantmentData enchant = tomeEnchants.get(i);
            
            // Get the enchantment object
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchant.getEnchantmentId());
            if (enchantObj == null) {
                System.out.println("[Anvil] [DEBUG] Enchant " + enchant.getEnchantmentId() + " not found in registry");
                incompatibleCount++;
                continue;
            }
            
            // Check compatibility
            if (!isEnchantmentCompatible(result, enchantObj)) {
                System.out.println("[Anvil] [DEBUG] Enchant " + enchant.getEnchantmentId() + " not compatible with item");
                incompatibleCount++;
                continue;
            }
            
            // Roll apply chance
            int applyChance = applyChances.getOrDefault(i, 50);
            int roll = RANDOM.nextInt(101); // 0-100
            
            System.out.println("[Anvil] [DEBUG] Enchant " + enchant.getEnchantmentId() + " - Roll: " + roll + " vs " + applyChance + "%");
            
            if (roll <= applyChance) {
                // Success! Apply the enchantment
                EnchantmentData.addEnchantmentToItem(result, enchantObj, 
                    enchant.getQuality(), enchant.getLevel(), null);
                successCount++;
                System.out.println("[Anvil] [DEBUG] SUCCESS! Applied " + enchant.getEnchantmentId());
            } else {
                // Failed apply chance
                failCount++;
                System.out.println("[Anvil] [DEBUG] FAILED! Didn't apply " + enchant.getEnchantmentId());
            }
        }
        
        System.out.println("[Anvil] [DEBUG] Results - Success: " + successCount + ", Failed: " + failCount + ", Incompatible: " + incompatibleCount);
        
        // If no enchantments applied, return null
        if (successCount == 0) {
            System.out.println("[Anvil] [DEBUG] No enchants applied, returning null");
            return null;
        }
        
        // Calculate cost
        int xpCost = BASE_XP_COST * (successCount + failCount + incompatibleCount);
        int essenceCost = BASE_ESSENCE_COST * successCount;
        
        System.out.println("[Anvil] [DEBUG] Returning result with cost: " + xpCost + " XP, " + essenceCost + " Essence");
        return new CombineResult(result, xpCost, essenceCost);
    }
    
    /**
     * Checks if an enchantment is compatible with an item.
     */
    private static boolean isEnchantmentCompatible(ItemStack item, com.server.enchantments.data.CustomEnchantment enchant) {
        return EquipmentTypeValidator.canEnchantmentApply(item, enchant);
    }
    
    /**
     * Combines two enchanted tomes.
     * Similar to combining items, but averages apply chances on duplicate enchantments.
     */
    private static CombineResult combineTomes(ItemStack tome1, ItemStack tome2) {
        // Debug NBT contents
        NBTItem tome1NBT = new NBTItem(tome1);
        NBTItem tome2NBT = new NBTItem(tome2);
        
        System.out.println("[Anvil] [DEBUG] === Tome 1 NBT Debug ===");
        System.out.println("[Anvil] [DEBUG] Material: " + tome1.getType());
        System.out.println("[Anvil] [DEBUG] Has MMO_EnchantCount: " + tome1NBT.hasKey("MMO_EnchantCount"));
        if (tome1NBT.hasKey("MMO_EnchantCount")) {
            System.out.println("[Anvil] [DEBUG] MMO_EnchantCount value: " + tome1NBT.getInteger("MMO_EnchantCount"));
        }
        System.out.println("[Anvil] [DEBUG] All NBT keys: " + tome1NBT.getKeys());
        
        System.out.println("[Anvil] [DEBUG] === Tome 2 NBT Debug ===");
        System.out.println("[Anvil] [DEBUG] Material: " + tome2.getType());
        System.out.println("[Anvil] [DEBUG] Has MMO_EnchantCount: " + tome2NBT.hasKey("MMO_EnchantCount"));
        if (tome2NBT.hasKey("MMO_EnchantCount")) {
            System.out.println("[Anvil] [DEBUG] MMO_EnchantCount value: " + tome2NBT.getInteger("MMO_EnchantCount"));
        }
        System.out.println("[Anvil] [DEBUG] All NBT keys: " + tome2NBT.getKeys());
        
        List<EnchantmentData> enchants1 = EnchantmentData.getEnchantmentsFromItem(tome1);
        List<EnchantmentData> enchants2 = EnchantmentData.getEnchantmentsFromItem(tome2);
        
        System.out.println("[Anvil] [DEBUG] Tome1 enchants: " + enchants1.size());
        System.out.println("[Anvil] [DEBUG] Tome2 enchants: " + enchants2.size());
        
        Map<String, EnchantmentData> mergedEnchants = new LinkedHashMap<>();
        Map<String, Integer> applyChances = new HashMap<>();
        
        // Process tome1 enchantments
        for (int i = 0; i < enchants1.size(); i++) {
            EnchantmentData enchant = enchants1.get(i);
            String id = enchant.getEnchantmentId();
            // FIXED: Use correct prefix "MMO_Enchant_" not "MMO_Enchantment_"
            int applyChance = tome1NBT.getInteger("MMO_Enchant_" + i + "_ApplyChance");
            
            mergedEnchants.put(id, enchant);
            applyChances.put(id, applyChance);
        }
        
        // Process tome2 enchantments
        for (int i = 0; i < enchants2.size(); i++) {
            EnchantmentData enchant2 = enchants2.get(i);
            String id = enchant2.getEnchantmentId();
            // FIXED: Use correct prefix "MMO_Enchant_" not "MMO_Enchantment_"
            int applyChance2 = tome2NBT.getInteger("MMO_Enchant_" + i + "_ApplyChance");
            
            if (mergedEnchants.containsKey(id)) {
                EnchantmentData enchant1 = mergedEnchants.get(id);
                int applyChance1 = applyChances.get(id);
                
                // Same enchantment - try to upgrade and average apply chances
                EnchantmentData upgraded = tryUpgradeEnchantment(enchant1, enchant2);
                int avgApplyChance = (applyChance1 + applyChance2) / 2;
                
                mergedEnchants.put(id, upgraded);
                applyChances.put(id, avgApplyChance);
            } else {
                // New enchantment - add it
                mergedEnchants.put(id, enchant2);
                applyChances.put(id, applyChance2);
            }
        }
        
        // Create unenchanted tome
        ItemStack resultTome = EnchantmentTome.createUnenchantedTome();
        
        System.out.println("[Anvil] [DEBUG] Created unenchanted tome: " + (resultTome != null));
        System.out.println("[Anvil] [DEBUG] Merged enchants to add: " + mergedEnchants.size());
        
        // Apply enchantments
        com.server.enchantments.EnchantmentRegistry registry = 
            com.server.enchantments.EnchantmentRegistry.getInstance();
        
        // Track which need level upgrade
        Map<String, Boolean> needsUpgrade = new HashMap<>();
        for (String id : mergedEnchants.keySet()) {
            // Check if this enchantment exists in both tomes with same level/quality
            EnchantmentData e1 = null, e2 = null;
            for (int i = 0; i < enchants1.size(); i++) {
                if (enchants1.get(i).getEnchantmentId().equals(id)) {
                    e1 = enchants1.get(i);
                    break;
                }
            }
            for (int i = 0; i < enchants2.size(); i++) {
                if (enchants2.get(i).getEnchantmentId().equals(id)) {
                    e2 = enchants2.get(i);
                    break;
                }
            }
            
            if (e1 != null && e2 != null && 
                e1.getLevel() == e2.getLevel() && 
                e1.getQuality() == e2.getQuality() &&
                e1.getLevel().ordinal() < EnchantmentLevel.VIII.ordinal()) {
                // Check if upgrade would exceed max level
                com.server.enchantments.data.CustomEnchantment enchantObj = 
                    registry.getEnchantment(id);
                if (enchantObj != null) {
                    EnchantmentLevel nextLevel = EnchantmentLevel.values()[e1.getLevel().ordinal() + 1];
                    // getMaxLevel() returns int (0-based), compare with ordinal
                    if (nextLevel.ordinal() <= enchantObj.getMaxLevel()) {
                        needsUpgrade.put(id, true);
                    }
                }
            }
        }
        
        for (Map.Entry<String, EnchantmentData> entry : mergedEnchants.entrySet()) {
            EnchantmentData enchant = entry.getValue();
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchant.getEnchantmentId());
            
            System.out.println("[Anvil] [DEBUG] Adding enchant: " + enchant.getEnchantmentId() + 
                             ", registry found: " + (enchantObj != null));
            
            if (enchantObj != null) {
                // Check if needs upgrade
                EnchantmentLevel levelToApply = enchant.getLevel();
                boolean shouldUpgrade = needsUpgrade.getOrDefault(enchant.getEnchantmentId(), false);
                if (shouldUpgrade) {
                    levelToApply = EnchantmentLevel.values()[enchant.getLevel().ordinal() + 1];
                }
                
                System.out.println("[Anvil] [DEBUG] Enchant: " + enchant.getEnchantmentId() + 
                                 ", Original Level: " + enchant.getLevel() + 
                                 ", Should Upgrade: " + shouldUpgrade + 
                                 ", Applying at Level: " + levelToApply);
                
                System.out.println("[Anvil] [DEBUG] Before addEnchant - tome type: " + resultTome.getType());
                EnchantmentData.addEnchantmentToItem(resultTome, enchantObj, 
                    enchant.getQuality(), levelToApply, null);
                System.out.println("[Anvil] [DEBUG] After addEnchant - tome type: " + resultTome.getType());
            }
        }
        
        // Debug: Check enchantments on unenchanted tome before conversion
        List<EnchantmentData> preConversionEnchants = EnchantmentData.getEnchantmentsFromItem(resultTome);
        System.out.println("[Anvil] [DEBUG] Tome before conversion has " + preConversionEnchants.size() + " enchantments");
        
        // Convert to enchanted tome
        resultTome = EnchantmentTome.createEnchantedTome(resultTome);
        
        // Check if conversion failed
        if (resultTome == null) {
            System.out.println("[Anvil] [ERROR] createEnchantedTome returned null!");
            return null;
        }
        
        // Update apply chances with averaged values
        NBTItem resultNBT = new NBTItem(resultTome);
        int index = 0;
        for (Map.Entry<String, EnchantmentData> entry : mergedEnchants.entrySet()) {
            String prefix = "MMO_Enchant_" + index + "_";
            int avgChance = applyChances.get(entry.getKey());
            resultNBT.setInteger(prefix + "ApplyChance", avgChance);
            index++;
        }
        resultTome = resultNBT.getItem();
        
        // Read the ACTUAL enchantments from the tome (after upgrade was applied)
        List<EnchantmentData> actualEnchants = EnchantmentData.getEnchantmentsFromItem(resultTome);
        
        // Rebuild lore with ACTUAL enchantment levels and correct apply chances
        resultTome = rebuildTomeLore(resultTome, actualEnchants, applyChances);
        
        // Calculate cost
        int totalEnchants = mergedEnchants.size();
        int xpCost = BASE_XP_COST * totalEnchants;
        int essenceCost = BASE_ESSENCE_COST * totalEnchants;
        
        return new CombineResult(resultTome, xpCost, essenceCost);
    }
    
    /**
     * Boosts an enchanted tome's apply chances using fragments.
     * Only affects enchantments with matching elements.
     */
    private static CombineResult boostTomeWithFragments(ItemStack tome, ItemStack fragments) {
        List<EnchantmentData> enchants = EnchantmentData.getEnchantmentsFromItem(tome);
        
        if (enchants.isEmpty()) {
            return null;
        }
        
        // Get fragment element
        ElementType fragmentElement = ElementalFragment.getElement(fragments);
        if (fragmentElement == null) {
            return null;
        }
        
        // Get fragment amount
        int availableFragments = fragments.getAmount();
        
        // Clone tome and update apply chances
        ItemStack result = tome.clone();
        NBTItem resultNBT = new NBTItem(result);
        
        int boostedCount = 0;
        int maxFragmentsNeeded = 0; // Track the maximum fragments needed for any enchantment
        Map<String, Integer> newApplyChances = new HashMap<>();
        
        // First pass: determine how many fragments are needed for each matching enchantment
        for (int i = 0; i < enchants.size(); i++) {
            EnchantmentData enchant = enchants.get(i);
            String prefix = "MMO_Enchant_" + i + "_";
            int currentChance = resultNBT.getInteger(prefix + "ApplyChance");
            
            // Check if enchantment matches fragment element
            boolean matches = false;
            if (enchant.getElement() == fragmentElement) {
                matches = true;
            } else if (enchant.getHybridElement() != null) {
                // Check hybrid elements
                if (enchant.getHybridElement().getPrimary() == fragmentElement ||
                    enchant.getHybridElement().getSecondary() == fragmentElement) {
                    matches = true;
                }
            }
            
            if (matches) {
                // Calculate how many fragments needed to reach 100% (5% per fragment)
                int chanceNeeded = 100 - currentChance;
                int fragmentsNeeded = (int) Math.ceil(chanceNeeded / 5.0);
                maxFragmentsNeeded = Math.max(maxFragmentsNeeded, fragmentsNeeded);
                boostedCount++;
            }
        }
        
        // If no enchantments boosted, return null
        if (boostedCount == 0) {
            return null;
        }
        
        // Calculate actual fragments to consume (minimum of available and needed)
        int fragmentsToConsume = Math.min(availableFragments, maxFragmentsNeeded);
        int boostAmount = fragmentsToConsume * 5; // 5% per fragment
        
        // Second pass: apply the boost to all matching enchantments
        for (int i = 0; i < enchants.size(); i++) {
            EnchantmentData enchant = enchants.get(i);
            String prefix = "MMO_Enchant_" + i + "_";
            int currentChance = resultNBT.getInteger(prefix + "ApplyChance");
            
            // Check if enchantment matches fragment element
            boolean matches = false;
            if (enchant.getElement() == fragmentElement) {
                matches = true;
            } else if (enchant.getHybridElement() != null) {
                // Check hybrid elements
                if (enchant.getHybridElement().getPrimary() == fragmentElement ||
                    enchant.getHybridElement().getSecondary() == fragmentElement) {
                    matches = true;
                }
            }
            
            if (matches) {
                // Apply boost, capped at 100%
                int newChance = Math.min(currentChance + boostAmount, 100);
                resultNBT.setInteger(prefix + "ApplyChance", newChance);
                newApplyChances.put(enchant.getEnchantmentId(), newChance);
            } else {
                newApplyChances.put(enchant.getEnchantmentId(), currentChance);
            }
        }
        
        result = resultNBT.getItem();
        
        // Rebuild lore with updated apply chances
        result = rebuildTomeLore(result, enchants, newApplyChances);
        
        // Calculate cost
        int xpCost = BASE_XP_COST * boostedCount;
        int essenceCost = BASE_ESSENCE_COST * fragmentsToConsume;
        
        // Calculate refund (excess fragments)
        ItemStack refund = null;
        int excessFragments = availableFragments - fragmentsToConsume;
        if (excessFragments > 0) {
            refund = fragments.clone();
            refund.setAmount(excessFragments);
        }
        
        return new CombineResult(result, xpCost, essenceCost, refund);
    }
    
    /**
     * Rebuilds tome lore with correct apply chances.
     */
    private static ItemStack rebuildTomeLore(ItemStack tome, List<EnchantmentData> enchants, Map<String, Integer> applyChances) {
        ItemMeta meta = tome.getItemMeta();
        if (meta == null) return tome;
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "✦ Enchanted Tome ✦");
        lore.add("");
        
        if (enchants.size() == 1) {
            lore.add(ChatColor.GOLD + "Contains " + ChatColor.WHITE + "1 Enchantment" + ChatColor.GOLD + ":");
        } else {
            lore.add(ChatColor.GOLD + "Contains " + ChatColor.WHITE + enchants.size() + " Enchantments" + ChatColor.GOLD + ":");
        }
        lore.add("");
        
        com.server.enchantments.EnchantmentRegistry registry = 
            com.server.enchantments.EnchantmentRegistry.getInstance();
        
        for (EnchantmentData enchant : enchants) {
            // Get enchantment object for display name and description
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchant.getEnchantmentId());
            
            String enchantName = enchantObj != null ? enchantObj.getDisplayName() : enchant.getEnchantmentId();
            String qualityColor = enchant.getQuality().getColor().toString();
            String levelRoman = enchant.getLevel().getRoman();
            String qualityName = enchant.getQuality().getDisplayName();
            
            // Get apply chance
            int applyChance = applyChances.getOrDefault(enchant.getEnchantmentId(), 50);
            
            // Determine color
            ChatColor chanceColor;
            if (applyChance >= 80) {
                chanceColor = ChatColor.GREEN;
            } else if (applyChance >= 50) {
                chanceColor = ChatColor.YELLOW;
            } else if (applyChance >= 25) {
                chanceColor = ChatColor.GOLD;
            } else {
                chanceColor = ChatColor.RED;
            }
            
            lore.add(qualityColor + "▸ " + enchantName + " " + levelRoman + " [" + qualityName + "]");
            lore.add(ChatColor.GRAY + "  Apply Chance: " + chanceColor + applyChance + "%");
            
            // Add description if available
            if (enchantObj != null) {
                String description = enchantObj.getDescription();
                if (description != null && !description.isEmpty()) {
                    // Word wrap the description at ~40 characters
                    String[] words = description.split(" ");
                    StringBuilder line = new StringBuilder(ChatColor.GRAY + "  ");
                    for (String word : words) {
                        if (line.length() + word.length() > 42) {
                            lore.add(line.toString().trim());
                            line = new StringBuilder(ChatColor.GRAY + "  ");
                        }
                        line.append(word).append(" ");
                    }
                    if (line.length() > 3) {
                        lore.add(line.toString().trim());
                    }
                }
            }
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Use in an anvil to apply");
        lore.add(ChatColor.GRAY + "enchantments to equipment.");
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "✦ Universal - Works on any gear ✦");
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        
        meta.setLore(lore);
        tome.setItemMeta(meta);
        
        return tome;
    }
    
    /**
     * Extracts only the base lore (non-enchantment lore) from an item's metadata.
     * This removes the enchantment section to avoid duplication when re-applying enchantments.
     */
    private static List<String> extractBaseLore(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        
        List<String> baseLore = new ArrayList<>();
        
        // Find where the enchantment section starts
        // Look for the specific enchantment header format: "§m          §r §6⚔ Enchantments §r§m          §r"
        int enchantSectionStart = -1;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String stripped = ChatColor.stripColor(line);
            
            // Find enchantment header - look for "Enchantments" with decorative formatting
            if (stripped != null && stripped.contains("Enchantments") && line.contains("§m")) {
                // Check if there's an empty line before this header
                if (i > 0 && lore.get(i - 1).trim().isEmpty()) {
                    enchantSectionStart = i - 1; // Include the empty line before header
                } else {
                    enchantSectionStart = i; // Start from the header itself
                }
                break;
            }
        }
        
        // If we found the enchantment section, only keep the lore before it
        if (enchantSectionStart >= 0) {
            for (int i = 0; i < enchantSectionStart; i++) {
                baseLore.add(lore.get(i));
            }
        } else {
            // No enchantment section found, keep all lore
            baseLore.addAll(lore);
        }
        
        return baseLore.isEmpty() ? null : baseLore;
    }
    
    /**
     * Result of a combine operation.
     */
    
    /**
     * Result of a preview calculation.
     * Includes an uncertainty flag if some enchants might fail.
     */
    public static class PreviewResult {
        private final ItemStack result;
        private final int xpCost;
        private final int essenceCost;
        private final boolean hasUncertainty;
        
        public PreviewResult(ItemStack result, int xpCost, int essenceCost, boolean hasUncertainty) {
            this.result = result;
            this.xpCost = xpCost;
            this.essenceCost = essenceCost;
            this.hasUncertainty = hasUncertainty;
        }
        
        public ItemStack getResult() {
            return result;
        }
        
        public int getXpCost() {
            return xpCost;
        }
        
        public int getEssenceCost() {
            return essenceCost;
        }
        
        public boolean hasUncertainty() {
            return hasUncertainty;
        }
    }
    
    public static class CombineResult {
        private final ItemStack result;
        private final int xpCost;
        private final int essenceCost;
        private final ItemStack refund; // Optional refund item (e.g., excess fragments)
        
        public CombineResult(ItemStack result, int xpCost, int essenceCost) {
            this.result = result;
            this.xpCost = xpCost;
            this.essenceCost = essenceCost;
            this.refund = null;
        }
        
        public CombineResult(ItemStack result, int xpCost, int essenceCost, ItemStack refund) {
            this.result = result;
            this.xpCost = xpCost;
            this.essenceCost = essenceCost;
            this.refund = refund;
        }
        
        public ItemStack getResult() {
            return result;
        }
        
        public int getXpCost() {
            return xpCost;
        }
        
        public int getEssenceCost() {
            return essenceCost;
        }
        
        public ItemStack getRefund() {
            return refund;
        }
        
        public boolean hasRefund() {
            return refund != null && refund.getAmount() > 0;
        }
    }
}
