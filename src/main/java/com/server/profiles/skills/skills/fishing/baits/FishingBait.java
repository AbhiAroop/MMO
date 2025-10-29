package com.server.profiles.skills.skills.fishing.baits;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.skills.fishing.loot.FishType;

/**
 * Represents different types of fishing baits
 */
public enum FishingBait {
    WORM(
        "§aWorm Bait",
        200001, // Changed from 100001 to avoid conflict with Golden Wheat (100001-100004)
        Arrays.asList(
            "§7A common earthworm that attracts",
            "§7basic freshwater fish.",
            "",
            "§8Attracts: §fCod, Tropical Fish",
            "§8Quality Boost: §e+5%"
        ),
        Arrays.asList(FishType.COD, FishType.TROPICAL_FISH),
        5.0
    ),
    
    CRICKET(
        "§2Cricket Bait",
        200002, // Changed from 100002
        Arrays.asList(
            "§7A lively cricket that attracts",
            "§7surface-dwelling fish.",
            "",
            "§8Attracts: §fSalmon, Pufferfish",
            "§8Quality Boost: §e+8%"
        ),
        Arrays.asList(FishType.SALMON, FishType.PUFFERFISH),
        8.0
    ),
    
    MINNOW(
        "§bMinnow Bait",
        200003, // Changed from 100003
        Arrays.asList(
            "§7A small live fish that attracts",
            "§7larger predatory fish.",
            "",
            "§8Attracts: §fFrozen Cod, Ice Pike, Frost Trout",
            "§8Quality Boost: §e+12%"
        ),
        Arrays.asList(FishType.FROZEN_COD, FishType.FROZEN_SALMON, FishType.ICE_PIKE, FishType.FROST_TROUT),
        12.0
    ),
    
    LEECH(
        "§5Leech Bait",
        200004, // Changed from 100004
        Arrays.asList(
            "§7A blood-sucking leech that attracts",
            "§7exotic and dangerous fish.",
            "",
            "§8Attracts: §fMagma Fish, Fire Bass, Molten Grouper",
            "§8Quality Boost: §e+15% §7+ Mob Chance"
        ),
        Arrays.asList(FishType.MAGMA_FISH, FishType.FIRE_BASS, FishType.MOLTEN_GROUPER),
        15.0
    ),
    
    MAGIC_LURE(
        "§6§lMagic Lure",
        200005, // Changed from 100005
        Arrays.asList(
            "§7An enchanted lure that attracts",
            "§7the rarest and most powerful fish.",
            "",
            "§8Attracts: §fVoid Fish, Eldritch Creatures",
            "§8Quality Boost: §e+20% §7+ Rare Mob Chance"
        ),
        Arrays.asList(FishType.BLAZEFIN, FishType.VOID_ANGLER, FishType.ELDRITCH_EEL, 
                     FishType.ABYSS_DWELLER, FishType.DIMENSION_LEVIATHAN),
        20.0
    );
    
    private final String displayName;
    private final int customModelData;
    private final List<String> lore;
    private final List<FishType> attractedFish;
    private final double qualityBoost; // Percentage boost to quality
    
    FishingBait(String displayName, int customModelData, List<String> lore, 
                List<FishType> attractedFish, double qualityBoost) {
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.lore = lore;
        this.attractedFish = attractedFish;
        this.qualityBoost = qualityBoost;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getCustomModelData() {
        return customModelData;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public List<FishType> getAttractedFish() {
        return attractedFish;
    }
    
    public double getQualityBoost() {
        return qualityBoost;
    }
    
    /**
     * Create an ItemStack for this bait
     */
    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(Material.WHEAT_SEEDS, amount); // Using wheat seeds as base
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Check if an item is a bait
     */
    public static FishingBait fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return null;
        }
        
        int customModelData = meta.getCustomModelData();
        for (FishingBait bait : values()) {
            if (bait.getCustomModelData() == customModelData) {
                return bait;
            }
        }
        
        return null;
    }
    
    /**
     * Check if this bait can attract a specific fish type
     */
    public boolean canAttractFish(FishType fishType) {
        return attractedFish.contains(fishType);
    }
    
    /**
     * Get a random fish type from the attracted fish list
     */
    public FishType getRandomAttractedFish() {
        if (attractedFish.isEmpty()) {
            return FishType.COD; // Fallback
        }
        return attractedFish.get((int) (Math.random() * attractedFish.size()));
    }
}
