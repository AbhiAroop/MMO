package com.server.profiles.skills.skills.farming.botany;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a custom crop that can be planted and harvested
 * Uses tripwire states and item displays for visual representation
 */
public class CustomCrop {
    
    private final String id;
    private final String displayName;
    private final CropRarity rarity;
    private final int maxGrowthStages;
    private final long growthTimePerStage; // in ticks
    
    // Visual data
    private final int[] customModelData; // One for each growth stage
    private final String textureKey; // Resource pack reference
    
    // Breeding data
    private final List<BreedingRecipe> recipes; // How to create this crop
    private final int breedingLevel; // Botany level required
    
    // Harvest data
    private final ItemStack baseDropItem;
    private final int minDrops;
    private final int maxDrops;
    private final double rareSeedChance; // Chance to drop seed on harvest
    
    // XP rewards
    private final double plantXp;
    private final double harvestXp;
    private final double breedXp;
    
    public CustomCrop(String id, String displayName, CropRarity rarity, 
                     int maxGrowthStages, long growthTimePerStage,
                     int[] customModelData, String textureKey,
                     int breedingLevel, ItemStack baseDropItem,
                     int minDrops, int maxDrops, double rareSeedChance,
                     double plantXp, double harvestXp, double breedXp) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.maxGrowthStages = maxGrowthStages;
        this.growthTimePerStage = growthTimePerStage;
        this.customModelData = customModelData;
        this.textureKey = textureKey;
        this.breedingLevel = breedingLevel;
        this.baseDropItem = baseDropItem;
        this.minDrops = minDrops;
        this.maxDrops = maxDrops;
        this.rareSeedChance = rareSeedChance;
        this.plantXp = plantXp;
        this.harvestXp = harvestXp;
        this.breedXp = breedXp;
        this.recipes = new ArrayList<>();
    }
    
    /**
     * Add a breeding recipe for this crop
     */
    public void addBreedingRecipe(BreedingRecipe recipe) {
        this.recipes.add(recipe);
    }
    
    /**
     * Get the custom model data for a specific growth stage
     */
    public int getCustomModelData(int stage) {
        if (stage < 0 || stage >= customModelData.length) {
            return customModelData[0];
        }
        return customModelData[stage];
    }
    
    /**
     * Create a seed item for this custom crop
     */
    public ItemStack createSeedItem() {
        return createSeedItem(1);
    }
    
    /**
     * Create a seed item with a specific amount
     */
    public ItemStack createSeedItem(int amount) {
        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS, amount);
        org.bukkit.inventory.meta.ItemMeta meta = seed.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(rarity.getColor() + displayName + " Seed");
            meta.setCustomModelData(customModelData[0]);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Rarity: " + rarity.getColor() + rarity.getDisplayName());
            lore.add("§7Growth Stages: §f" + maxGrowthStages);
            lore.add("§7Growth Time: §f" + (growthTimePerStage * maxGrowthStages / 20) + "s total");
            lore.add("");
            lore.add("§7Plant on farmland to grow");
            lore.add("§7Requires Botany Level " + breedingLevel);
            meta.setLore(lore);
            
            seed.setItemMeta(meta);
        }
        return seed;
    }
    
    /**
     * Create a drop item for this custom crop
     */
    public ItemStack createDropItem(int amount) {
        ItemStack drop = baseDropItem.clone();
        drop.setAmount(amount);
        return drop;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public CropRarity getRarity() { return rarity; }
    public int getMaxGrowthStages() { return maxGrowthStages; }
    public long getGrowthTimePerStage() { return growthTimePerStage; }
    public String getTextureKey() { return textureKey; }
    public List<BreedingRecipe> getRecipes() { return new ArrayList<>(recipes); }
    public int getBreedingLevel() { return breedingLevel; }
    public ItemStack getBaseDropItem() { return baseDropItem.clone(); }
    public int getMinDrops() { return minDrops; }
    public int getMaxDrops() { return maxDrops; }
    public double getRareSeedChance() { return rareSeedChance; }
    public double getPlantXp() { return plantXp; }
    public double getHarvestXp() { return harvestXp; }
    public double getBreedXp() { return breedXp; }
    
    /**
     * Rarity levels for custom crops
     */
    public enum CropRarity {
        COMMON("Common", "§f", 1.0),
        UNCOMMON("Uncommon", "§a", 1.2),
        RARE("Rare", "§9", 1.5),
        EPIC("Epic", "§5", 2.0),
        LEGENDARY("Legendary", "§6", 3.0),
        MYTHIC("Mythic", "§c", 5.0);
        
        private final String displayName;
        private final String color;
        private final double xpMultiplier;
        
        CropRarity(String displayName, String color, double xpMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.xpMultiplier = xpMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
        public double getXpMultiplier() { return xpMultiplier; }
    }
    
    /**
     * Represents a breeding recipe for creating this crop
     */
    public static class BreedingRecipe {
        private final String parentCrop1Id;
        private final String parentCrop2Id;
        private final double successChance;
        private final int requiredBotanyLevel;
        
        public BreedingRecipe(String parentCrop1Id, String parentCrop2Id, 
                             double successChance, int requiredBotanyLevel) {
            this.parentCrop1Id = parentCrop1Id;
            this.parentCrop2Id = parentCrop2Id;
            this.successChance = successChance;
            this.requiredBotanyLevel = requiredBotanyLevel;
        }
        
        /**
         * Check if this recipe matches the given parent crops
         */
        public boolean matches(String crop1Id, String crop2Id) {
            return (parentCrop1Id.equals(crop1Id) && parentCrop2Id.equals(crop2Id)) ||
                   (parentCrop1Id.equals(crop2Id) && parentCrop2Id.equals(crop1Id));
        }
        
        // Getters
        public String getParentCrop1Id() { return parentCrop1Id; }
        public String getParentCrop2Id() { return parentCrop2Id; }
        public double getSuccessChance() { return successChance; }
        public int getRequiredBotanyLevel() { return requiredBotanyLevel; }
    }
}
