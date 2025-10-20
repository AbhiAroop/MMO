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
import com.server.enchantments.effects.ElementalParticles;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.FragmentTier;
import com.server.enchantments.elements.HybridElement;
import com.server.enchantments.items.ElementalFragment;
import com.server.enchantments.items.EnchantmentTome;

/**
 * Handles the actual enchantment application process.
 * Validates fragments, calculates hybrid formation, selects enchantments,
 * rolls quality, and applies the enchantment to items.
 * 
 * NOW SUPPORTS MULTIPLE ENCHANTMENTS:
 * - More fragments = more enchantments possible
 * - Higher quality fragments = bonus enchantments (even if lower quality each)
 * - Can grant both hybrid and single-element enchantments
 * - Fragment distribution determines enchantment pool
 * 
 * ENCHANTMENT COUNT CALCULATION (PROBABILITY-BASED):
 * - Always get 1 enchantment minimum (guaranteed)
 * - Additional enchantments are PROBABILITY-BASED (never guaranteed)
 * - More fragments + better quality = higher chance for additional enchantments
 * - Probability formula:
 *   - Base chance = fragmentCount / 192
 *   - Quality bonus = (qualityMultiplier - 1.0) * 0.5 (max +25% from Pristine)
 *   - 3rd enchantment is harder (60% of chance)
 *   - Maximum probability capped at 85% (never 100% guaranteed)
 * - Examples:
 *   - 48 Basic fragments = 25% chance for 2nd enchantment
 *   - 96 Basic fragments = 50% chance for 2nd, then 30% for 3rd
 *   - 192 Basic fragments = 85% chance for 2nd, then 51% for 3rd
 *   - 96 Pristine fragments = 75% chance for 2nd, then 45% for 3rd
 *   - 192 Pristine fragments = 85% chance for 2nd, then 51% for 3rd (capped)
 */
public class EnchantmentApplicator {
    
    private static final Random RANDOM = new Random();
    
    // Thresholds for multiple enchantments
    // NOTE: GUI has 3 slots, max 64 per slot = 192 fragments maximum
    private static final int FRAGMENTS_PER_ENCHANTMENT = 48; // Base: 48 fragments = 1 enchantment
    private static final int MAX_ENCHANTMENTS = 3; // Maximum enchantments per application (3 slots limit)
    
    /**
     * Attempts to enchant an item with the provided fragments.
     * Can apply multiple enchantments based on fragment count.
     * 
     * @param player The player enchanting the item
     * @param item The item to enchant
     * @param fragments The fragments to consume (1-3 stacks)
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
        
        // Calculate total fragment count
        int totalFragmentCount = fragmentData.stream()
            .mapToInt(fd -> fd.amount)
            .sum();
        
        // Calculate quality multiplier for probability calculations
        // Higher quality fragments = better chance for additional enchantments
        double qualityMultiplier = calculateQualityMultiplier(fragmentData);
        
        // PROBABILITY-BASED ENCHANTMENT COUNT (never guaranteed, always random)
        // Base: Always get 1 enchantment minimum
        int enchantmentCount = 1;
        
        // Roll for 2nd enchantment (based on fragment count and quality)
        double chance2nd = calculateAdditionalEnchantmentChance(totalFragmentCount, qualityMultiplier, 1);
        if (RANDOM.nextDouble() < chance2nd) {
            enchantmentCount = 2;
            
            // Roll for 3rd enchantment (harder to get)
            double chance3rd = calculateAdditionalEnchantmentChance(totalFragmentCount, qualityMultiplier, 2);
            if (RANDOM.nextDouble() < chance3rd) {
                enchantmentCount = 3;
            }
        }
        
        // Generate multiple enchantment results
        List<EnchantmentApplication> enchantmentsToApply = new ArrayList<>();
        List<String> usedEnchantmentIds = new ArrayList<>(); // Track used enchantments
        int remainingFragments = totalFragmentCount;
        int maxAttempts = enchantmentCount * 3; // Allow multiple attempts to find unique enchantments
        int attempts = 0;
        
        for (int i = 0; i < enchantmentCount && remainingFragments > 0 && attempts < maxAttempts; attempts++) {
            // Distribute fragments for this enchantment
            int fragmentsForThis = remainingFragments / (enchantmentCount - i);
            
            // Determine element pool (hybrid or single)
            ElementResult elementResult = determineElement(fragmentData);
            ElementType targetElement = elementResult.element;
            boolean isHybrid = elementResult.isHybrid;
            HybridElement hybridType = elementResult.hybridType;
            
            // Select enchantment (can be hybrid or single-element)
            CustomEnchantment enchantment = selectEnchantment(targetElement, isHybrid, hybridType, usedEnchantmentIds);
            if (enchantment == null) continue; // Skip if no valid enchantment or all used
            
            // Check if this enchantment was already selected
            if (usedEnchantmentIds.contains(enchantment.getId())) {
                continue; // Try again with different selection
            }
            
            // Roll quality based on fragment tier
            EnchantmentQuality quality = rollQuality(fragmentData);
            
            // Calculate level based on fragments for this enchantment
            com.server.enchantments.data.EnchantmentLevel level = 
                com.server.enchantments.data.EnchantmentLevel.calculateLevel(fragmentsForThis);
            
            // Cap level at enchantment's maximum
            int maxLevel = enchantment.getMaxLevel();
            if (level.getNumericLevel() > maxLevel) {
                level = com.server.enchantments.data.EnchantmentLevel.fromNumeric(maxLevel);
            }
            
            // Store this enchantment application
            enchantmentsToApply.add(new EnchantmentApplication(
                enchantment, quality, level, isHybrid, hybridType
            ));
            
            // Mark this enchantment as used
            usedEnchantmentIds.add(enchantment.getId());
            
            remainingFragments -= fragmentsForThis;
            i++; // Only increment when we successfully add an enchantment
        }
        
        // Check if we got any valid enchantments
        if (enchantmentsToApply.isEmpty()) {
            return new EnchantmentResult(false, "No valid enchantments found for elements", null);
        }
        
        // Calculate XP cost (based on number of enchantments and their rarities)
        int xpCost = calculateMultiEnchantmentXPCost(enchantmentsToApply, fragmentData.size());
        
        // Check if player has enough XP
        if (player.getLevel() < xpCost) {
            return new EnchantmentResult(false, 
                ChatColor.RED + "Not enough XP! Need " + xpCost + " levels", null);
        }
        
        // Apply all enchantments
        ItemStack enchantedItem = item.clone();
        
        // Preserve custom model data if present
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int customModelData = item.getItemMeta().getCustomModelData();
            // Store it before adding enchantments
            org.bukkit.inventory.meta.ItemMeta meta = enchantedItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                enchantedItem.setItemMeta(meta);
            }
        }
        
        // Apply all enchantments
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(ChatColor.GREEN).append("Successfully enchanted with:\n");
        
        int hybridCount = 0;
        for (EnchantmentApplication app : enchantmentsToApply) {
            // Add enchantment with player parameter for conflict messages
            EnchantmentData.addEnchantmentToItem(enchantedItem, app.enchantment, app.quality, app.level, player);
            
            // Build message line for this enchantment
            messageBuilder.append(ChatColor.GRAY).append("  • ")
                         .append(app.enchantment.getDisplayName()).append(" ")
                         .append(app.level.getDisplayName()).append(" ")
                         .append(app.quality.getColor()).append("[").append(app.quality.getDisplayName()).append("]");
            
            if (app.isHybrid) {
                messageBuilder.append(ChatColor.LIGHT_PURPLE).append(" ⚡HYBRID");
                hybridCount++;
            }
            messageBuilder.append("\n");
        }
        
        // Re-apply custom model data after enchantments (ensure it's preserved)
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
        
        // Spawn particle effects based on applied enchantments
        for (EnchantmentApplication app : enchantmentsToApply) {
            ElementType element = app.enchantment.getElement();
            if (element != null) {
                // Burst effect at player location
                ElementalParticles.spawnElementalBurst(player.getLocation().add(0, 1, 0), element, 1.0);
            }
        }
        
        // Create helix effect around player with primary element
        if (!enchantmentsToApply.isEmpty()) {
            ElementType primaryElement = enchantmentsToApply.get(0).enchantment.getElement();
            if (primaryElement != null) {
                ElementalParticles.spawnPlayerHelix(player.getLocation(), primaryElement, 40);
            }
        }
        
        // Add special effect for multiple enchantments
        if (enchantmentsToApply.size() > 1) {
            player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);
            messageBuilder.append(ChatColor.GOLD).append("⭐ MULTI-ENCHANTMENT! (")
                         .append(enchantmentsToApply.size()).append(" enchantments)");
            
            // Extra ring effect for multi-enchants
            if (enchantmentsToApply.size() >= 2 && enchantmentsToApply.get(1).enchantment.getElement() != null) {
                ElementalParticles.spawnElementalRing(
                    player.getLocation().add(0, 0.1, 0), 
                    enchantmentsToApply.get(1).enchantment.getElement(), 
                    1.5
                );
            }
        }
        
        // Add hybrid count if any
        if (hybridCount > 0) {
            if (enchantmentsToApply.size() > 1) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(ChatColor.LIGHT_PURPLE).append("⚡ ")
                         .append(hybridCount).append(" Hybrid").append(hybridCount > 1 ? "s" : "");
        }
        
        // SPECIAL CASE: If enchanting an unenchanted tome, convert it to an enchanted tome
        if (EnchantmentTome.isUnenchantedTome(item)) {
            ItemStack enchantedTomeResult = EnchantmentTome.createEnchantedTome(enchantedItem);
            if (enchantedTomeResult != null) {
                messageBuilder.append("\n").append(ChatColor.GOLD)
                             .append("⚡ Tome successfully enchanted!");
                return new EnchantmentResult(true, messageBuilder.toString(), enchantedTomeResult);
            }
        }
        
        return new EnchantmentResult(true, messageBuilder.toString(), enchantedItem);
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
     * (Backwards compatible version without exclusions)
     */
    private static CustomEnchantment selectEnchantment(ElementType element, boolean isHybrid, HybridElement hybridType) {
        return selectEnchantment(element, isHybrid, hybridType, new ArrayList<>());
    }
    
    /**
     * Selects an appropriate enchantment based on element and hybrid status.
     * Excludes enchantments that have already been selected.
     */
    private static CustomEnchantment selectEnchantment(ElementType element, boolean isHybrid, 
                                                       HybridElement hybridType, List<String> excludeIds) {
        if (isHybrid && hybridType != null) {
            // Try to get hybrid enchantment for this specific hybrid type
            List<CustomEnchantment> hybridEnchantments = 
                EnchantmentRegistry.getInstance().getEnchantmentsByHybrid(hybridType);
            
            // Filter out already used enchantments
            List<CustomEnchantment> availableHybrids = hybridEnchantments.stream()
                .filter(e -> !excludeIds.contains(e.getId()))
                .collect(Collectors.toList());
            
            if (!availableHybrids.isEmpty()) {
                return weightedRandomSelect(availableHybrids);
            }
        }
        
        // Get enchantments for this element
        List<CustomEnchantment> elementEnchantments = 
            EnchantmentRegistry.getInstance().getEnchantmentsByElement(element);
        
        // Filter out already used enchantments
        List<CustomEnchantment> availableEnchants = elementEnchantments.stream()
            .filter(e -> !excludeIds.contains(e.getId()))
            .collect(Collectors.toList());
        
        if (availableEnchants.isEmpty()) {
            return null;
        }
        
        // Weighted random selection based on rarity
        return weightedRandomSelect(availableEnchants);
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
     * Calculates a quality multiplier based on fragment tiers.
     * Higher quality fragments = higher multiplier for enchantment count bonuses.
     * 
     * @param fragmentData List of fragment data
     * @return Quality multiplier (1.0 = Basic, 1.5 = Pristine)
     */
    private static double calculateQualityMultiplier(List<FragmentData> fragmentData) {
        // Calculate average tier level
        double avgTierLevel = fragmentData.stream()
            .mapToInt(data -> data.tier.ordinal())
            .average()
            .orElse(0.0);
        
        // Convert tier level to multiplier
        // Basic (0) = 1.0x
        // Refined (1) = 1.2x
        // Superior (2) = 1.35x
        // Pristine (3) = 1.5x
        return 1.0 + (avgTierLevel * 0.15);
    }
    
    /**
     * Calculates the probability of getting an additional enchantment.
     * NEVER GUARANTEED - always involves randomness!
     * 
     * @param fragmentCount Total number of fragments
     * @param qualityMultiplier Quality multiplier from fragment tiers
     * @param currentEnchantCount Current enchantment count (1 for 2nd, 2 for 3rd)
     * @return Probability (0.0 to 1.0) of getting the next enchantment
     */
    private static double calculateAdditionalEnchantmentChance(int fragmentCount, double qualityMultiplier, int currentEnchantCount) {
        // Base chance from fragment count
        // 48 fragments = 25% base chance for 2nd enchantment
        // 96 fragments = 50% base chance
        // 144 fragments = 75% base chance
        // 192 fragments = 100% base chance (but quality reduces it)
        double baseChance = Math.min(1.0, (fragmentCount / 192.0));
        
        // Apply quality multiplier (better quality = better chance)
        double qualityBonus = (qualityMultiplier - 1.0) * 0.5; // Max +25% from Pristine
        double adjustedChance = baseChance + qualityBonus;
        
        // Each additional enchantment is harder to get
        // 2nd enchantment: Full chance
        // 3rd enchantment: 60% of the chance
        double difficultyMultiplier = (currentEnchantCount == 1) ? 1.0 : 0.6;
        
        // Final probability (capped at 85% to never guarantee)
        return Math.min(0.85, adjustedChance * difficultyMultiplier);
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
     * Calculates XP cost for multiple enchantments.
     */
    private static int calculateMultiEnchantmentXPCost(List<EnchantmentApplication> enchantments, int fragmentCount) {
        int totalCost = 0;
        
        for (EnchantmentApplication app : enchantments) {
            int baseCost = app.enchantment.getRarity().getMinXPCost();
            totalCost += baseCost;
        }
        
        // Add fragment multiplier
        int fragmentMultiplier = fragmentCount * 2; // 2 XP per fragment
        totalCost += fragmentMultiplier;
        
        // Discount for multiple enchantments (encourage using more fragments)
        if (enchantments.size() > 1) {
            totalCost = (int) (totalCost * 0.8); // 20% discount
        }
        
        return Math.max(1, totalCost);
    }
    
    /**
     * DEPRECATED: Single enchantment XP cost - replaced by multi-enchantment version
     */
    @Deprecated
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
     * Helper class to store a single enchantment application.
     */
    private static class EnchantmentApplication {
        final CustomEnchantment enchantment;
        final EnchantmentQuality quality;
        final com.server.enchantments.data.EnchantmentLevel level;
        final boolean isHybrid;
        final HybridElement hybridType;
        
        EnchantmentApplication(CustomEnchantment enchantment, EnchantmentQuality quality,
                              com.server.enchantments.data.EnchantmentLevel level,
                              boolean isHybrid, HybridElement hybridType) {
            this.enchantment = enchantment;
            this.quality = quality;
            this.level = level;
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
