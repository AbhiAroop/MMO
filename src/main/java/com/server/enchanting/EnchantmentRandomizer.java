package com.server.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.CustomEnchantment.EnchantmentRarity;
import com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel;

/**
 * Enhanced randomization system for multiple enchantments with probabilities
 */
public class EnchantmentRandomizer {
    
    private static final Random random = new Random();
    
    /**
     * Data structure for enchantment selection with probability
     */
    public static class EnchantmentSelection {
        public final CustomEnchantment enchantment;
        public final int level;
        public final double probability;
        public final boolean isGuaranteed;
        
        public EnchantmentSelection(CustomEnchantment enchantment, int level, double probability, boolean isGuaranteed) {
            this.enchantment = enchantment;
            this.level = level;
            this.probability = probability;
            this.isGuaranteed = isGuaranteed;
        }
    }
    
    /**
     * Generate multiple enchantment selections for an item
     */
    public static List<EnchantmentSelection> generateEnchantmentSelections(org.bukkit.inventory.ItemStack item, 
                                                                        EnchantingLevel enchantingLevel,
                                                                        org.bukkit.inventory.ItemStack[] enhancementMaterials,
                                                                        org.bukkit.entity.Player player) {
        List<EnchantmentSelection> selections = new ArrayList<>();
        
        // Get all applicable enchantments
        List<CustomEnchantment> availableEnchantments = CustomEnchantmentRegistry.getInstance()
            .getApplicableEnchantments(item);
        
        // CRITICAL: Filter by player unlock status
        availableEnchantments = filterByPlayerUnlocks(availableEnchantments, player);
        
        if (availableEnchantments.isEmpty()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "No enchantments available for item after unlock filtering");
            }
            return selections;
        }
        
        // Rest of the method continues as before...
        availableEnchantments = filterByEnchantingLevel(availableEnchantments, enchantingLevel);
        
        if (availableEnchantments.isEmpty()) {
            if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
                Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                    "No enchantments available after level filtering");
            }
            return selections;
        }
        
        // Calculate how many enchantments to offer based on enchanting level
        int maxEnchantments = calculateMaxEnchantments(enchantingLevel);
        
        // Generate primary enchantment (highest chance, guaranteed)
        CustomEnchantment primaryEnchantment = selectPrimaryEnchantment(availableEnchantments, enchantingLevel);
        if (primaryEnchantment != null) {
            int primaryLevel = calculateEnchantmentLevel(primaryEnchantment, enhancementMaterials, enchantingLevel);
            double primaryProbability = calculateBaseSuccessRate(primaryEnchantment, enchantingLevel) + 
                                      calculateMaterialBonus(primaryEnchantment, enhancementMaterials);
            selections.add(new EnchantmentSelection(primaryEnchantment, primaryLevel, 
                                                  Math.min(primaryProbability, 0.95), true));
            
            // Remove from available list to prevent duplicates
            availableEnchantments.remove(primaryEnchantment);
        }
        
        // Generate secondary enchantments with decreasing probabilities
        for (int i = 1; i < maxEnchantments && !availableEnchantments.isEmpty(); i++) {
            CustomEnchantment secondaryEnchantment = selectSecondaryEnchantment(availableEnchantments, enchantingLevel, i);
            if (secondaryEnchantment != null) {
                int secondaryLevel = calculateEnchantmentLevel(secondaryEnchantment, enhancementMaterials, enchantingLevel);
                double baseProbability = calculateBaseSuccessRate(secondaryEnchantment, enchantingLevel) + 
                                       calculateMaterialBonus(secondaryEnchantment, enhancementMaterials);
                
                // Reduce probability for each additional enchantment
                double secondaryProbability = baseProbability * Math.pow(0.7, i); // 30% reduction per additional enchantment
                
                selections.add(new EnchantmentSelection(secondaryEnchantment, secondaryLevel, 
                                                      Math.max(secondaryProbability, 0.05), false));
                
                // Remove from available list
                availableEnchantments.remove(secondaryEnchantment);
            }
        }
        
        // Add randomization and chaos effects
        applyChaosFactor(selections, enchantingLevel);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchantment Randomizer] Generated " + selections.size() + " enchantment selections");
        }
        
        return selections;
    }
    
    /**
     * Calculate maximum number of enchantments based on enchanting level
     */
    private static int calculateMaxEnchantments(EnchantingLevel enchantingLevel) {
        int totalLevel = enchantingLevel.getTotalLevel();
        
        if (totalLevel >= 80) return 5;      // Maximum 5 enchantments at very high levels
        if (totalLevel >= 60) return 4;      // 4 enchantments at high levels
        if (totalLevel >= 40) return 3;      // 3 enchantments at medium-high levels
        if (totalLevel >= 20) return 2;      // 2 enchantments at medium levels
        return 1;                            // 1 enchantment at low levels
    }
    
    /**
     * Select primary enchantment (highest priority)
     */
    private static CustomEnchantment selectPrimaryEnchantment(List<CustomEnchantment> available, EnchantingLevel enchantingLevel) {
        if (available.isEmpty()) return null;
        
        // Weight by rarity and enchanting level compatibility
        Map<CustomEnchantment, Double> weights = new HashMap<>();
        
        for (CustomEnchantment enchantment : available) {
            double weight = calculateEnchantmentWeight(enchantment, enchantingLevel, true);
            weights.put(enchantment, weight);
        }
        
        return selectWeightedRandom(weights);
    }
    
    /**
     * Select secondary enchantment
     */
    private static CustomEnchantment selectSecondaryEnchantment(List<CustomEnchantment> available, 
                                                              EnchantingLevel enchantingLevel, int priority) {
        if (available.isEmpty()) return null;
        
        // Weight by rarity and enchanting level compatibility, but favor different categories
        Map<CustomEnchantment, Double> weights = new HashMap<>();
        
        for (CustomEnchantment enchantment : available) {
            double weight = calculateEnchantmentWeight(enchantment, enchantingLevel, false);
            // Reduce weight for higher priorities to make rarer enchantments less likely
            weight *= Math.pow(0.8, priority);
            weights.put(enchantment, weight);
        }
        
        return selectWeightedRandom(weights);
    }
    
    /**
     * Calculate enchantment weight for selection - ENHANCED: Level-based weighting
     */
    private static double calculateEnchantmentWeight(CustomEnchantment enchantment, EnchantingLevel enchantingLevel, boolean isPrimary) {
        double baseWeight = 1.0;
        
        // Rarity affects weight (rarer = lower chance but better when available)
        switch (enchantment.getRarity()) {
            case COMMON:
                baseWeight = 1.0;
                break;
            case UNCOMMON:
                baseWeight = 0.8;
                break;
            case RARE:
                baseWeight = 0.6;
                break;
            case EPIC:
                baseWeight = 0.4;
                break;
            case LEGENDARY:
                baseWeight = 0.2;
                break;
            case MYTHIC:
                baseWeight = 0.1;
                break;
        }
        
        // Higher enchanting levels favor rarer enchantments
        int totalLevel = enchantingLevel.getTotalLevel();
        double levelMultiplier = 1.0 + (totalLevel / 100.0); // Up to 2x at level 100
        
        // Calculate what level this enchantment would be at current table level
        int enchantmentLevel = calculateEnchantmentLevel(enchantment, new org.bukkit.inventory.ItemStack[0], enchantingLevel);
        
        // Boost weight for enchantments that can reach higher levels
        if (enchantmentLevel > 1) {
            double levelBoost = 1.0 + (enchantmentLevel - 1) * 0.3; // 30% boost per level above 1
            levelMultiplier *= levelBoost;
        }
        
        // Mythic and Legendary enchantments get extra boost at high levels
        if (enchantment.getRarity().ordinal() >= EnchantmentRarity.LEGENDARY.ordinal() && totalLevel >= 200) {
            levelMultiplier *= 2.0;
        }
        
        // Primary enchantments get slightly higher weights
        if (isPrimary) {
            baseWeight *= 1.3;
        }
        
        return baseWeight * levelMultiplier;
    }
    
    /**
     * Select enchantment using weighted random selection
     */
    private static CustomEnchantment selectWeightedRandom(Map<CustomEnchantment, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (totalWeight <= 0) return null;
        
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;
        
        for (Map.Entry<CustomEnchantment, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback - return any available enchantment
        return weights.keySet().iterator().next();
    }
    
    /**
     * Apply chaos factor to all selections
     */
    private static void applyChaosFactor(List<EnchantmentSelection> selections, EnchantingLevel enchantingLevel) {
        double chaosChance = Math.min(enchantingLevel.getTotalLevel() * 0.002, 0.15); // Up to 15% chaos
        
        for (int i = 0; i < selections.size(); i++) {
            EnchantmentSelection selection = selections.get(i);
            
            if (random.nextDouble() < chaosChance && !selection.isGuaranteed) {
                // Apply chaos effect - can increase or decrease probability
                double chaosModifier = 0.8 + (random.nextDouble() * 0.4); // 0.8x to 1.2x multiplier
                double newProbability = Math.max(0.05, Math.min(0.95, selection.probability * chaosModifier));
                
                selections.set(i, new EnchantmentSelection(selection.enchantment, selection.level, 
                                                         newProbability, selection.isGuaranteed));
            }
        }
    }
    
    /**
     * Filter enchantments by required enchanting level
     */
    private static List<CustomEnchantment> filterByEnchantingLevel(List<CustomEnchantment> enchantments, 
                                                                  EnchantingLevel enchantingLevel) {
        List<CustomEnchantment> filtered = new ArrayList<>();
        
        for (CustomEnchantment enchantment : enchantments) {
            int requiredLevel = getRequiredEnchantingLevel(enchantment);
            if (enchantingLevel.getTotalLevel() >= requiredLevel) {
                filtered.add(enchantment);
            }
        }
        
        return filtered;
    }
    
    /**
     * Execute enchantment selections and apply successful ones
     */
    public static List<AppliedEnchantment> executeEnchantmentSelections(List<EnchantmentSelection> selections) {
        List<AppliedEnchantment> appliedEnchantments = new ArrayList<>();
        
        for (EnchantmentSelection selection : selections) {
            boolean success;
            
            if (selection.isGuaranteed) {
                success = true; // Primary enchantment is always successful
            } else {
                success = random.nextDouble() < selection.probability;
            }
            
            if (success) {
                // Add variance to level (small chance for +1 or -1)
                int finalLevel = selection.level;
                if (!selection.isGuaranteed && random.nextDouble() < 0.1) { // 10% chance for level variance
                    if (random.nextBoolean() && finalLevel < selection.enchantment.getMaxLevel()) {
                        finalLevel++; // Bonus level
                    } else if (finalLevel > 1) {
                        finalLevel--; // Reduced level
                    }
                }
                
                appliedEnchantments.add(new AppliedEnchantment(selection.enchantment, finalLevel));
            }
        }
        
        return appliedEnchantments;
    }

    /**
     * Filter enchantments by player unlock status
     */
    private static List<CustomEnchantment> filterByPlayerUnlocks(List<CustomEnchantment> enchantments, 
                                                            org.bukkit.entity.Player player) {
        if (player == null) return enchantments;
        
        List<CustomEnchantment> filtered = new ArrayList<>();
        
        for (CustomEnchantment enchantment : enchantments) {
            if (isEnchantmentUnlockedForPlayer(enchantment, player)) {
                filtered.add(enchantment);
            }
        }
        
        return filtered;
    }

    /**
     * Check if a specific enchantment is unlocked for a player
     */
    private static boolean isEnchantmentUnlockedForPlayer(CustomEnchantment enchantment, 
                                                        org.bukkit.entity.Player player) {
        String enchantmentId = enchantment.getId();
        
        // Check for mining-related enchantments that require skill tree unlocks
        if ("swiftbreak".equals(enchantmentId)) {
            // Get the mining skill and check if swiftbreak is unlocked
            com.server.profiles.skills.core.Skill miningSkill = 
                com.server.profiles.skills.core.SkillRegistry.getInstance().getSkill(
                    com.server.profiles.skills.core.SkillType.MINING);
            
            if (miningSkill instanceof com.server.profiles.skills.skills.mining.MiningSkill) {
                com.server.profiles.skills.skills.mining.MiningSkill mining = 
                    (com.server.profiles.skills.skills.mining.MiningSkill) miningSkill;
                return mining.isSwiftbreakEnchantmentUnlocked(player);
            }
            return false;
        }
        
        // Add checks for other enchantments that require unlocks here
        // For now, all other enchantments are available by default
        return true;
    }
    
    /**
     * Data structure for successfully applied enchantments
     */
    public static class AppliedEnchantment {
        public final CustomEnchantment enchantment;
        public final int level;
        
        public AppliedEnchantment(CustomEnchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }
    
    /**
     * Calculate enchantment level based on enchanting table level and materials - ENHANCED: Proper scaling
     */
    private static int calculateEnchantmentLevel(CustomEnchantment enchantment, org.bukkit.inventory.ItemStack[] enhancementMaterials, 
                                            EnchantingLevel enchantingLevel) {
        int totalLevel = enchantingLevel.getTotalLevel();
        EnchantmentRarity rarity = enchantment.getRarity();
        int maxLevel = enchantment.getMaxLevel();
        
        // Calculate base level from enchanting table level and rarity
        int baseLevel = calculateBaseLevelFromTable(totalLevel, rarity, maxLevel);
        
        // Add bonus from enhancement materials
        int materialBonus = 0;
        for (org.bukkit.inventory.ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != org.bukkit.Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    materialBonus += enhancementMaterial.getLevelBonus();
                }
            }
        }
        
        // Apply material bonus but cap at max level
        int finalLevel = Math.min(baseLevel + materialBonus, maxLevel);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.ENCHANTING)) {
            Main.getInstance().debugLog(DebugSystem.ENCHANTING, 
                "Enchantment Level Calculation: " + enchantment.getId() + 
                " | Table Level: " + totalLevel + 
                " | Rarity: " + rarity.name() + 
                " | Base Level: " + baseLevel + 
                " | Material Bonus: " + materialBonus + 
                " | Final Level: " + finalLevel + "/" + maxLevel);
        }
        
        return Math.max(1, finalLevel); // Minimum level 1
    }

    /**
     * Calculate base enchantment level from table level and rarity - NEW METHOD
     */
    private static int calculateBaseLevelFromTable(int tableLevel, EnchantmentRarity rarity, int maxLevel) {
        // Define level requirements for each rarity to reach max level
        int levelRequiredForMaxLevel;
        
        switch (rarity) {
            case COMMON:
                levelRequiredForMaxLevel = 60;    // Max out at level 30
                break;
            case UNCOMMON:
                levelRequiredForMaxLevel = 100;    // Max out at level 50
                break;
            case RARE:
                levelRequiredForMaxLevel = 250;   // Max out at level 100
                break;
            case EPIC:
                levelRequiredForMaxLevel = 500;   // Max out at level 200
                break;
            case LEGENDARY:
                levelRequiredForMaxLevel = 750;   // Max out at level 400
                break;
            case MYTHIC:
                levelRequiredForMaxLevel = 900;   // Max out at level 800
                break;
            default:
                levelRequiredForMaxLevel = 30;
                break;
        }
        
        // Calculate what level the enchantment should be based on table level
        if (tableLevel >= levelRequiredForMaxLevel) {
            return maxLevel; // Full max level
        }
        
        // Progressive scaling - enchantment level increases as table level approaches requirement
        double progressRatio = (double) tableLevel / levelRequiredForMaxLevel;
        
        // Use a curved progression for better feel
        // Early levels come easier, later levels require more investment
        double curvedProgress = Math.pow(progressRatio, 0.7); // Slight curve favoring early levels
        
        int calculatedLevel = (int) Math.ceil(curvedProgress * maxLevel);
        
        // Ensure minimum level 1 and respect max level
        return Math.max(1, Math.min(calculatedLevel, maxLevel));
    }
    
    /**
     * Calculate base success rate - ENHANCED: Account for level scaling impact
     */
    private static double calculateBaseSuccessRate(CustomEnchantment enchantment, EnchantingLevel enchantingLevel) {
        double baseRate = 0.8 - (enchantment.getRarity().ordinal() * 0.1); // 80% for common, down to 30% for mythic
        
        int requiredLevel = getRequiredEnchantingLevel(enchantment);
        int playerLevel = enchantingLevel.getTotalLevel();
        
        // Bonus from having higher level than required
        if (playerLevel > requiredLevel) {
            double levelBonus = Math.min((playerLevel - requiredLevel) * 0.01, 0.3); // Up to 30% bonus
            baseRate += levelBonus;
        }
        
        // Penalty for being below optimal level for higher enchantment levels
        int enchantmentLevel = calculateEnchantmentLevel(enchantment, new org.bukkit.inventory.ItemStack[0], enchantingLevel);
        if (enchantmentLevel > 1) {
            // Higher level enchantments are slightly harder to get
            double levelPenalty = (enchantmentLevel - 1) * 0.05; // 5% penalty per level above 1
            baseRate -= levelPenalty;
        }
        
        return Math.max(0.1, Math.min(baseRate, 0.95)); // Between 10% and 95%
    }
    
    private static double calculateMaterialBonus(CustomEnchantment enchantment, org.bukkit.inventory.ItemStack[] enhancementMaterials) {
        double totalBonus = 0.0;
        
        for (org.bukkit.inventory.ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != org.bukkit.Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    totalBonus += enhancementMaterial.getEnhancementBonus(enchantment);
                }
            }
        }
        
        return Math.min(totalBonus, 0.4);
    }
    
    private static int getRequiredEnchantingLevel(CustomEnchantment enchantment) {
        switch (enchantment.getRarity()) {
            case COMMON: return 1;
            case UNCOMMON: return 5;
            case RARE: return 15;
            case EPIC: return 25;
            case LEGENDARY: return 40;
            case MYTHIC: return 60;
            default: return 1;
        }
    }
}