package com.server.enchanting;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.server.enchanting.CustomEnchantment.EnchantmentCategory;
import com.server.enchanting.CustomEnchantment.EnchantmentRarity;

/**
 * Materials that can enhance enchantment outcomes
 */
public class EnchantmentMaterial {
    
    private final String id;
    private final Material material;
    private final String displayName;
    private final String description;
    private final int customModelData;
    
    // Enhancement properties
    private final double successRateBonus;
    private final double rarityBonus;
    private final List<EnchantmentCategory> favoredCategories;
    private final double categoryBonus;
    private final int levelBonus; // Can increase enchantment level
    
    public EnchantmentMaterial(String id, Material material, String displayName, String description,
                             int customModelData, double successRateBonus, double rarityBonus,
                             List<EnchantmentCategory> favoredCategories, double categoryBonus, int levelBonus) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.customModelData = customModelData;
        this.successRateBonus = successRateBonus;
        this.rarityBonus = rarityBonus;
        this.favoredCategories = favoredCategories;
        this.categoryBonus = categoryBonus;
        this.levelBonus = levelBonus;
    }
    
    // Getters
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getCustomModelData() { return customModelData; }
    public double getSuccessRateBonus() { return successRateBonus; }
    public double getRarityBonus() { return rarityBonus; }
    public List<EnchantmentCategory> getFavoredCategories() { return favoredCategories; }
    public double getCategoryBonus() { return categoryBonus; }
    public int getLevelBonus() { return levelBonus; }
    
    /**
     * Check if this material matches an item
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        // For custom items, check model data
        if (customModelData > 0) {
            if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
                return false;
            }
            return item.getItemMeta().getCustomModelData() == customModelData;
        }
        
        // For vanilla items, just material match
        return true;
    }
    
    /**
     * Get enhancement bonus for a specific enchantment
     */
    public double getEnhancementBonus(CustomEnchantment enchantment) {
        double bonus = successRateBonus;
        
        // Add rarity bonus for higher rarity enchantments
        if (enchantment.getRarity().ordinal() >= EnchantmentRarity.RARE.ordinal()) {
            bonus += rarityBonus;
        }
        
        // Add category bonus
        if (favoredCategories.contains(enchantment.getCategory())) {
            bonus += categoryBonus;
        }
        
        return bonus;
    }
    
    /**
     * Get formatted lore for display
     */
    public List<String> getEnhancementLore() {
        List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GOLD + displayName);
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.AQUA + "Enhancement Effects:");
        
        if (successRateBonus > 0) {
            lore.add(ChatColor.GREEN + "+" + String.format("%.1f", successRateBonus * 100) + "% Success Rate");
        }
        
        if (rarityBonus > 0) {
            lore.add(ChatColor.LIGHT_PURPLE + "+" + String.format("%.1f", rarityBonus * 100) + "% Rare+ Enchantments");
        }
        
        if (levelBonus > 0) {
            lore.add(ChatColor.YELLOW + "+" + levelBonus + " Enchantment Level");
        }
        
        if (categoryBonus > 0 && !favoredCategories.isEmpty()) {
            lore.add(ChatColor.YELLOW + "+" + String.format("%.1f", categoryBonus * 100) + "% for:");
            for (EnchantmentCategory category : favoredCategories) {
                lore.add(ChatColor.WHITE + "  • " + category.getColor() + category.getDisplayName());
            }
        }
        
        return lore;
    }
    
    /**
     * Static registry of enhancement materials
     */
    public static class Registry {
        
        // Basic enhancement materials
        public static final EnchantmentMaterial LAPIS_LAZULI = new EnchantmentMaterial(
            "lapis_lazuli", Material.LAPIS_LAZULI, "Lapis Lazuli",
            "A basic enhancement crystal that stabilizes enchantment energy",
            0, 0.10, 0.05, Arrays.asList(EnchantmentCategory.MYSTICAL), 0.15, 0
        );
        
        public static final EnchantmentMaterial DIAMOND = new EnchantmentMaterial(
            "diamond", Material.DIAMOND, "Diamond",
            "A precious gem that amplifies combat enchantments",
            0, 0.15, 0.10, Arrays.asList(EnchantmentCategory.COMBAT), 0.25, 1
        );
        
        public static final EnchantmentMaterial EMERALD = new EnchantmentMaterial(
            "emerald", Material.EMERALD, "Emerald",
            "A rare gem that enhances utility and tool enchantments",
            0, 0.12, 0.08, Arrays.asList(EnchantmentCategory.UTILITY, EnchantmentCategory.TOOL), 0.20, 0
        );
        
        public static final EnchantmentMaterial NETHERITE_SCRAP = new EnchantmentMaterial(
            "netherite_scrap", Material.NETHERITE_SCRAP, "Netherite Scrap",
            "Ancient debris that greatly enhances all enchantments",
            0, 0.25, 0.20, Arrays.asList(EnchantmentCategory.COMBAT, EnchantmentCategory.PROTECTION), 0.30, 2
        );
        
        public static final EnchantmentMaterial BLAZE_POWDER = new EnchantmentMaterial(
            "blaze_powder", Material.BLAZE_POWDER, "Blaze Powder",
            "Magical powder that boosts mystical enchantments",
            0, 0.08, 0.15, Arrays.asList(EnchantmentCategory.MYSTICAL), 0.35, 0
        );
        
        // Advanced enhancement materials (would be custom items)
        public static final EnchantmentMaterial VOID_CRYSTAL = new EnchantmentMaterial(
            "void_crystal", Material.AMETHYST_SHARD, "Void Crystal",
            "A crystallized fragment of pure void energy",
            300001, 0.30, 0.25, Arrays.asList(EnchantmentCategory.CURSED), 0.50, 3
        );
        
        // Array of all materials for easy iteration
        public static final EnchantmentMaterial[] ALL_MATERIALS = {
            LAPIS_LAZULI, DIAMOND, EMERALD, NETHERITE_SCRAP, BLAZE_POWDER, VOID_CRYSTAL
        };
        
        /**
         * Get enhancement material from item
         */
        public static EnchantmentMaterial fromItem(ItemStack item) {
            for (EnchantmentMaterial material : ALL_MATERIALS) {
                if (material.matches(item)) {
                    return material;
                }
            }
            return null;
        }
        
        /**
         * Check if item is an enhancement material
         */
        public static boolean isEnhancementMaterial(ItemStack item) {
            return fromItem(item) != null;
        }
    }
}