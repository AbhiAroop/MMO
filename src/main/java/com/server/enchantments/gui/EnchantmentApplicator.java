package com.server.enchantments.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.EnchantmentRegistry;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.FragmentTier;
import com.server.enchantments.elements.HybridElement;
import com.server.enchantments.items.ElementalFragment;

/**
 * Handles the actual enchantment application process.
 * Validates fragments, calculates hybrid formation, selects enchantments,
 * rolls quality, and applies the enchantment to items.
 */
public class EnchantmentApplicator {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Attempts to enchant an item with the provided fragments.
     * 
     * @param player The player enchanting the item
     * @param item The item to enchant
     * @param fragments The fragments to consume (1-3)
     * @return The enchanted item if successful, null if failed
     */
    public static EnchantmentResult enchantItem(Player player, ItemStack item, List<ItemStack> fragments) {
        // Validate inputs
        if (item == null || fragments == null || fragments.isEmpty()) {
            return new EnchantmentResult(false, "Invalid item or fragments", null);
        }
        
        if (fragments.size() < 1 || fragments.size() > 3) {
            return new EnchantmentResult(false, "Requires 1-3 fragments", null);
        }
        
        // Validate all are fragments
        for (ItemStack fragment : fragments) {
            if (!ElementalFragment.isFragment(fragment)) {
                return new EnchantmentResult(false, "Invalid fragment detected", null);
            }
        }
        
        // Extract fragment data
        List<FragmentData> fragmentData = fragments.stream()
            .map(FragmentData::new)
            .collect(Collectors.toList());
        
        // Determine element (hybrid or single)
        ElementResult elementResult = determineElement(fragmentData);
        ElementType targetElement = elementResult.element;
        boolean isHybrid = elementResult.isHybrid;
        HybridElement hybridType = elementResult.hybridType;
        
        // Select enchantment
        CustomEnchantment enchantment = selectEnchantment(targetElement, isHybrid, hybridType);
        if (enchantment == null) {
            return new EnchantmentResult(false, "No valid enchantment found for elements", null);
        }
        
        // Roll quality based on fragment tier
        EnchantmentQuality quality = rollQuality(fragmentData);
        
        // Calculate level based on total fragment count
        int totalFragmentCount = fragmentData.stream()
            .mapToInt(fd -> fd.amount)
            .sum();
        com.server.enchantments.data.EnchantmentLevel level = 
            com.server.enchantments.data.EnchantmentLevel.calculateLevel(totalFragmentCount);
        
        // Calculate XP cost
        int xpCost = calculateXPCost(enchantment.getRarity(), fragmentData.size());
        
        // Check if player has enough XP
        if (player.getLevel() < xpCost) {
            return new EnchantmentResult(false, 
                ChatColor.RED + "Not enough XP! Need " + xpCost + " levels", null);
        }
        
        // Apply enchantment
        ItemStack enchantedItem = item.clone();
        
        // Preserve custom model data if present
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModelData = item.getItemMeta().getCustomModelData();
            // Store it before adding enchantment
            org.bukkit.inventory.meta.ItemMeta meta = enchantedItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                enchantedItem.setItemMeta(meta);
            }
        }
        
        EnchantmentData.addEnchantmentToItem(enchantedItem, enchantment, quality, level);
        
        // Re-apply custom model data after enchantment (ensure it's preserved)
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModelData = item.getItemMeta().getCustomModelData();
            org.bukkit.inventory.meta.ItemMeta meta = enchantedItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                enchantedItem.setItemMeta(meta);
            }
        }
        
        // Deduct XP
        player.setLevel(player.getLevel() - xpCost);
        
        // NOTE: Elemental affinity is now tracked dynamically from equipped items
        // via StatScanManager.scanAndUpdateAffinity() - no need to track here
        
        // Success effects
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        
        String message = ChatColor.GREEN + "Successfully enchanted with " + 
                        enchantment.getDisplayName() + " " +
                        level.getDisplayName() + " " +
                        quality.getColor() + "[" + quality.getDisplayName() + "]";
        
        if (isHybrid) {
            message += ChatColor.LIGHT_PURPLE + " ⚡ HYBRID!";
        }
        
        return new EnchantmentResult(true, message, enchantedItem);
    }
    
    /**
     * Determines the target element from fragments (hybrid or single element).
     * Now counts TOTAL fragment amounts (not just stacks) for proper hybrid detection.
     */
    private static ElementResult determineElement(List<FragmentData> fragmentData) {
        // Count elements by TOTAL AMOUNT (not just stacks)
        Map<ElementType, Integer> elementCounts = new HashMap<>();
        for (FragmentData data : fragmentData) {
            elementCounts.put(data.element, 
                elementCounts.getOrDefault(data.element, 0) + data.amount);
        }
        
        // Single element - all same
        if (elementCounts.size() == 1) {
            return new ElementResult(elementCounts.keySet().iterator().next(), false, null);
        }
        
        // Multiple elements - attempt hybrid formation
        List<ElementType> elements = new ArrayList<>(elementCounts.keySet());
        
        // Calculate hybrid chance based on tiers
        double hybridChance = calculateHybridChance(fragmentData);
        
        // Check for all possible hybrid combinations
        HybridElement hybrid = detectHybrid(elements, elementCounts);
        
        if (hybrid != null && RANDOM.nextDouble() < hybridChance) {
            // Hybrid formed! Return primary element with hybrid info
            return new ElementResult(hybrid.getPrimary(), true, hybrid);
        }
        
        // Hybrid failed or not applicable - use dominant element
        ElementType dominant = elementCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .get()
            .getKey();
        
        return new ElementResult(dominant, false, null);
    }
    
    /**
     * Detects which hybrid element can form from the given elements.
     */
    private static HybridElement detectHybrid(List<ElementType> elements, Map<ElementType, Integer> elementCounts) {
        // Check all hybrid combinations (order matters for priority)
        
        // STORM (Fire + Lightning)
        if (elements.contains(ElementType.FIRE) && elements.contains(ElementType.LIGHTNING)) {
            return HybridElement.STORM;
        }
        
        // MIST/ICE (Water + Air) - Using MIST as the hybrid
        if (elements.contains(ElementType.WATER) && elements.contains(ElementType.AIR)) {
            return HybridElement.MIST;
        }
        
        // DECAY (Earth + Shadow)
        if (elements.contains(ElementType.EARTH) && elements.contains(ElementType.SHADOW)) {
            return HybridElement.DECAY;
        }
        
        // RADIANCE (Light + Lightning)
        if (elements.contains(ElementType.LIGHT) && elements.contains(ElementType.LIGHTNING)) {
            return HybridElement.RADIANCE;
        }
        
        // ASH (Fire + Shadow)
        if (elements.contains(ElementType.FIRE) && elements.contains(ElementType.SHADOW)) {
            return HybridElement.ASH;
        }
        
        // PURITY (Water + Light)
        if (elements.contains(ElementType.WATER) && elements.contains(ElementType.LIGHT)) {
            return HybridElement.PURITY;
        }
        
        // Note: elementCounts parameter kept for future weighted hybrid logic
        return null;
    }
    
    /**
     * Calculates hybrid formation chance based on fragment tiers.
     */
    private static double calculateHybridChance(List<FragmentData> fragmentData) {
        double totalChance = 0.0;
        int count = 0;
        
        for (FragmentData data : fragmentData) {
            totalChance += data.tier.getHybridChanceBoost();
            count++;
        }
        
        return count > 0 ? totalChance / count : 0.0;
    }
    
    /**
     * Selects an appropriate enchantment based on element and hybrid status.
     */
    private static CustomEnchantment selectEnchantment(ElementType element, boolean isHybrid, HybridElement hybridType) {
        if (isHybrid && hybridType != null) {
            // Try to get hybrid enchantment for this specific hybrid type
            List<CustomEnchantment> hybridEnchantments = 
                EnchantmentRegistry.getInstance().getEnchantmentsByHybrid(hybridType);
            
            if (!hybridEnchantments.isEmpty()) {
                return hybridEnchantments.get(RANDOM.nextInt(hybridEnchantments.size()));
            }
        }
        
        // Get enchantments for this element
        List<CustomEnchantment> elementEnchantments = 
            EnchantmentRegistry.getInstance().getEnchantmentsByElement(element);
        
        if (elementEnchantments.isEmpty()) {
            return null;
        }
        
        // Weighted random selection based on rarity
        return weightedRandomSelect(elementEnchantments);
    }
    
    /**
     * Performs weighted random selection of enchantments based on rarity.
     * Higher rarity = lower chance to select.
     */
    private static CustomEnchantment weightedRandomSelect(List<CustomEnchantment> enchantments) {
        // Calculate weights (inverse of rarity ordinal for lower chance on rare items)
        Map<CustomEnchantment, Double> weights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (CustomEnchantment ench : enchantments) {
            // Common = 5, Uncommon = 4, Rare = 3, Epic = 2, Legendary = 1
            double weight = 6.0 - ench.getRarity().ordinal();
            weights.put(ench, weight);
            totalWeight += weight;
        }
        
        // Random selection
        double randomValue = RANDOM.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        
        for (Map.Entry<CustomEnchantment, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback
        return enchantments.get(0);
    }
    
    /**
     * Rolls quality based on fragment tiers.
     */
    private static EnchantmentQuality rollQuality(List<FragmentData> fragmentData) {
        // Use highest tier for quality weights
        FragmentTier highestTier = fragmentData.stream()
            .map(data -> data.tier)
            .max(Comparator.comparingInt(FragmentTier::ordinal))
            .orElse(FragmentTier.BASIC);
        
        int[] qualityWeightsArray = highestTier.getQualityWeights();
        
        // Convert int[] to Map<EnchantmentQuality, Double>
        Map<EnchantmentQuality, Double> qualityWeights = new HashMap<>();
        EnchantmentQuality[] qualities = EnchantmentQuality.values();
        for (int i = 0; i < qualityWeightsArray.length && i < qualities.length; i++) {
            qualityWeights.put(qualities[i], (double) qualityWeightsArray[i]);
        }
        
        // Weighted random selection
        double totalWeight = qualityWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = RANDOM.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        
        for (Map.Entry<EnchantmentQuality, Double> entry : qualityWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback
        return EnchantmentQuality.COMMON;
    }
    
    /**
     * Calculates XP cost based on rarity and number of fragments.
     */
    private static int calculateXPCost(EnchantmentRarity rarity, int fragmentCount) {
        int baseCost = rarity.getMinXPCost();
        int fragmentMultiplier = fragmentCount * 2; // 2 XP per fragment
        return baseCost + fragmentMultiplier;
    }
    
    /**
     * DEPRECATED: Elemental affinity tracking removed - now handled dynamically
     * by StatScanManager which scans equipped items for enchantments.
     * 
     * Old approach: Tracked fragments consumed during enchanting
     * New approach: Scans current equipment for enchantments and calculates affinity
     * 
     * This allows affinity to update in real-time when equipping/holding enchanted items.
     */
    /*
    private static void trackElementalAffinity(Player player, List<FragmentData> fragmentData, 
                                                EnchantmentQuality quality) {
        // REMOVED - See StatScanManager.scanAndUpdateAffinity() instead
    }
    */
    
    /**
     * Helper class to store fragment data.
     */
    private static class FragmentData {
        final ElementType element;
        final FragmentTier tier;
        final int amount;
        
        FragmentData(ItemStack fragment) {
            this.element = ElementalFragment.getElement(fragment);
            this.tier = ElementalFragment.getTier(fragment);
            this.amount = fragment.getAmount();
        }
    }
    
    /**
     * Helper class to store element determination result.
     */
    private static class ElementResult {
        final ElementType element;
        final boolean isHybrid;
        final HybridElement hybridType;
        
        ElementResult(ElementType element, boolean isHybrid, HybridElement hybridType) {
            this.element = element;
            this.isHybrid = isHybrid;
            this.hybridType = hybridType;
        }
    }
    
    /**
     * Result of enchantment attempt.
     */
    public static class EnchantmentResult {
        private final boolean success;
        private final String message;
        private final ItemStack enchantedItem;
        
        public EnchantmentResult(boolean success, String message, ItemStack enchantedItem) {
            this.success = success;
            this.message = message;
            this.enchantedItem = enchantedItem;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ItemStack getEnchantedItem() {
            return enchantedItem;
        }
    }
}
