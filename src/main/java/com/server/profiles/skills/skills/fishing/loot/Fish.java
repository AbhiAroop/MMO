package com.server.profiles.skills.skills.fishing.loot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.skills.fishing.baits.FishingBait;
import com.server.profiles.skills.skills.fishing.types.FishingType;

/**
 * Represents a caught fish with rarity, size, and quality
 */
public class Fish {
    private final FishType fishType;
    private final FishRarity rarity;
    private final FishQuality quality;
    private final double size; // In centimeters
    private final boolean isTrophy;
    
    public Fish(FishType fishType, FishRarity rarity, FishQuality quality, double size, boolean isTrophy) {
        this.fishType = fishType;
        this.rarity = rarity;
        this.quality = quality;
        this.size = size;
        this.isTrophy = isTrophy;
    }
    
    /**
     * Create a fish based on fishing performance and type
     * @param fishingType The environment being fished in
     * @param accuracy The accuracy percentage from minigame
     * @param treasureBonus Treasure bonus stat
     * @param perfectCatches Number of perfect catches
     * @param bait The bait used (can be null)
     */
    public static Fish createFish(FishingType fishingType, double accuracy, double treasureBonus, 
                                 int perfectCatches, FishingBait bait) {
        // Determine fish type based on bait or fishing environment
        FishType fishType;
        if (bait != null && !bait.getAttractedFish().isEmpty()) {
            // If bait is used, get a random fish from the attracted list
            fishType = bait.getRandomAttractedFish();
        } else {
            // No bait or empty list, use environment-based selection
            fishType = FishType.getRandomFish(fishingType);
        }
        
        // Determine rarity based on treasure bonus
        FishRarity rarity = FishRarity.getRandomRarity(treasureBonus);
        
        // Determine quality based on accuracy, perfect catches, and bait bonus
        double qualityBoost = bait != null ? bait.getQualityBoost() : 0.0;
        FishQuality quality = FishQuality.fromAccuracy(accuracy, perfectCatches, qualityBoost);
        
        // Calculate size (affected by quality and rarity)
        double baseSize = fishType.getBaseSize();
        double sizeVariation = baseSize * 0.3; // ±30% variation
        double size = baseSize + (Math.random() * sizeVariation * 2 - sizeVariation);
        
        // Apply quality bonus to size
        size *= (1.0 + (quality.getValueMultiplier() - 1.0) * 0.5);
        
        // Apply rarity bonus to size
        size *= (1.0 + (rarity.getValueMultiplier() - 1.0) * 0.2);
        
        // Check if trophy (very large fish with perfect catches)
        boolean isTrophy = perfectCatches >= 3 && quality == FishQuality.PERFECT && 
                          size > baseSize * 1.5 && Math.random() < 0.1;
        
        return new Fish(fishType, rarity, quality, size, isTrophy);
    }
    
    /**
     * Legacy method for backwards compatibility
     */
    public static Fish createFish(FishingType fishingType, double accuracy, double treasureBonus, int perfectCatches) {
        return createFish(fishingType, accuracy, treasureBonus, perfectCatches, null);
    }
    
    /**
     * Convert to ItemStack with custom name and lore
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(fishType.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Build display name
            StringBuilder nameBuilder = new StringBuilder();
            
            if (isTrophy) {
                nameBuilder.append("§6§l[TROPHY] ");
            }
            
            nameBuilder.append(quality.getColoredName()).append(" ");
            nameBuilder.append(rarity.getColoredName()).append(" ");
            nameBuilder.append(fishType.getDisplayName());
            
            meta.setDisplayName(nameBuilder.toString());
            
            // Build lore
            List<String> lore = new ArrayList<>();
            lore.add("§7Size: §f" + String.format("%.1f", size) + " cm");
            lore.add("§7Quality: " + quality.getColoredName());
            lore.add("§7Rarity: " + rarity.getColoredName());
            lore.add("");
            lore.add("§7Value: §6" + calculateValue() + " coins");
            
            if (isTrophy) {
                lore.add("");
                lore.add("§6§l★ TROPHY FISH ★");
                lore.add("§7A legendary catch!");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Calculate the coin value of the fish
     */
    public int calculateValue() {
        double baseValue = fishType.getBaseValue();
        
        // Apply multipliers
        baseValue *= rarity.getValueMultiplier();
        baseValue *= quality.getValueMultiplier();
        
        // Size bonus (larger = more valuable)
        double sizeRatio = size / fishType.getBaseSize();
        baseValue *= sizeRatio;
        
        // Trophy bonus
        if (isTrophy) {
            baseValue *= 5.0;
        }
        
        return (int) Math.max(1, baseValue);
    }
    
    // Getters
    
    public FishType getFishType() {
        return fishType;
    }
    
    public FishRarity getRarity() {
        return rarity;
    }
    
    public FishQuality getQuality() {
        return quality;
    }
    
    public double getSize() {
        return size;
    }
    
    public boolean isTrophy() {
        return isTrophy;
    }
}
