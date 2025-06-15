package com.server.enchanting;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a custom enchantment with enhanced mechanics
 */
public class CustomEnchantment {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final EnchantmentCategory category;
    private final int maxLevel;
    private final EnchantmentRarity rarity;
    private final List<ItemCategory> applicableItems;
    private final List<String> conflictingEnchantments;
    
    // Cost scaling
    private final int baseXpCost;
    private final int baseEssenceCost;
    private final double levelMultiplier;
    
    public enum EnchantmentCategory {
        COMBAT("Combat", ChatColor.RED, Material.DIAMOND_SWORD),
        TOOL("Tool", ChatColor.YELLOW, Material.DIAMOND_PICKAXE),
        PROTECTION("Protection", ChatColor.BLUE, Material.DIAMOND_CHESTPLATE),
        UTILITY("Utility", ChatColor.GREEN, Material.EMERALD),
        MYSTICAL("Mystical", ChatColor.DARK_PURPLE, Material.ENCHANTED_BOOK),
        CURSED("Cursed", ChatColor.DARK_RED, Material.WITHER_SKELETON_SKULL);
        
        private final String displayName;
        private final ChatColor color;
        private final Material icon;
        
        EnchantmentCategory(String displayName, ChatColor color, Material icon) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public Material getIcon() { return icon; }
    }
    
    public enum EnchantmentRarity {
        COMMON("Common", ChatColor.GRAY, 1.0),
        UNCOMMON("Uncommon", ChatColor.GREEN, 1.5),
        RARE("Rare", ChatColor.BLUE, 2.0),
        EPIC("Epic", ChatColor.DARK_PURPLE, 3.0),
        LEGENDARY("Legendary", ChatColor.GOLD, 5.0),
        MYTHIC("Mythic", ChatColor.RED, 10.0);
        
        private final String displayName;
        private final ChatColor color;
        private final double costMultiplier;
        
        EnchantmentRarity(String displayName, ChatColor color, double costMultiplier) {
            this.displayName = displayName;
            this.color = color;
            this.costMultiplier = costMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public double getCostMultiplier() { return costMultiplier; }
        public String getFormattedName() { return color + displayName; }
    }
    
    public enum ItemCategory {
        SWORD, AXE, PICKAXE, SHOVEL, HOE, BOW, 
        HELMET, CHESTPLATE, LEGGINGS, BOOTS,
        STAFF, WAND, RELIC, TOOL;
        
        public static ItemCategory fromItemStack(ItemStack item) {
            if (item == null || !item.hasItemMeta()) return null;
            
            // Check custom model data for custom items
            if (item.getItemMeta().hasCustomModelData()) {
                int modelData = item.getItemMeta().getCustomModelData();
                String modelStr = String.valueOf(modelData);
                
                // Custom items with 2XXXXX model data (functional items)
                if (modelStr.startsWith("2") && modelStr.length() >= 6) {
                    String typeCode = modelStr.substring(1, 3);
                    switch (typeCode) {
                        case "10": return SWORD;
                        case "11": return AXE;
                        case "12": return BOW;
                        case "13": return PICKAXE;
                        case "14": return SHOVEL;
                        case "15": return HOE;
                        case "20": // Helmet
                        case "30": return HELMET;
                        case "21": // Chestplate
                        case "31": return CHESTPLATE;
                        case "22": // Leggings
                        case "32": return LEGGINGS;
                        case "23": // Boots
                        case "33": return BOOTS;
                        case "40": return STAFF;
                        case "41": return WAND;
                        case "50": return RELIC;
                        default: return TOOL;
                    }
                }
            }
            
            // Fallback to vanilla item type detection
            Material type = item.getType();
            if (type.name().endsWith("_SWORD")) return SWORD;
            if (type.name().endsWith("_AXE")) return AXE;
            if (type.name().endsWith("_PICKAXE")) return PICKAXE;
            if (type.name().endsWith("_SHOVEL")) return SHOVEL;
            if (type.name().endsWith("_HOE")) return HOE;
            if (type == Material.BOW || type == Material.CROSSBOW) return BOW;
            if (type.name().endsWith("_HELMET")) return HELMET;
            if (type.name().endsWith("_CHESTPLATE")) return CHESTPLATE;
            if (type.name().endsWith("_LEGGINGS")) return LEGGINGS;
            if (type.name().endsWith("_BOOTS")) return BOOTS;
            
            return TOOL; // Default fallback
        }
    }
    
    public CustomEnchantment(String id, String displayName, String description, 
                           EnchantmentCategory category, int maxLevel, EnchantmentRarity rarity,
                           List<ItemCategory> applicableItems, List<String> conflictingEnchantments,
                           int baseXpCost, int baseEssenceCost, double levelMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.maxLevel = maxLevel;
        this.rarity = rarity;
        this.applicableItems = applicableItems;
        this.conflictingEnchantments = conflictingEnchantments;
        this.baseXpCost = baseXpCost;
        this.baseEssenceCost = baseEssenceCost;
        this.levelMultiplier = levelMultiplier;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public EnchantmentCategory getCategory() { return category; }
    public int getMaxLevel() { return maxLevel; }
    public EnchantmentRarity getRarity() { return rarity; }
    public List<ItemCategory> getApplicableItems() { return applicableItems; }
    public List<String> getConflictingEnchantments() { return conflictingEnchantments; }
    
    /**
     * Check if this enchantment can be applied to an item
     */
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        
        ItemCategory itemCategory = ItemCategory.fromItemStack(item);
        return itemCategory != null && applicableItems.contains(itemCategory);
    }
    
    /**
     * Get XP cost for a specific level
     */
    public int getXpCost(int level) {
        double baseCost = baseXpCost * Math.pow(levelMultiplier, level - 1);
        return (int) (baseCost * rarity.getCostMultiplier());
    }
    
    /**
     * Get essence cost for a specific level
     */
    public int getEssenceCost(int level) {
        double baseCost = baseEssenceCost * Math.pow(levelMultiplier, level - 1);
        return (int) (baseCost * rarity.getCostMultiplier());
    }
    
    /**
     * Get formatted enchantment name with level
     */
    public String getFormattedName(int level) {
        if (level == 1 && maxLevel == 1) {
            return rarity.getColor() + displayName;
        }
        return rarity.getColor() + displayName + " " + getRomanNumeral(level);
    }
    
    /**
     * Convert integer to roman numeral (up to 100)
     */
    private String getRomanNumeral(int number) {
        if (number <= 0) return "0";
        if (number > 100) return String.valueOf(number);
        
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] units = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        
        return thousands[number / 1000] +
               hundreds[(number % 1000) / 100] +
               tens[(number % 100) / 10] +
               units[number % 10];
    }
    
    /**
     * Get enchantment lore for display
     */
    public List<String> getEnchantmentLore(int level) {
        return Arrays.asList(
            getFormattedName(level),
            ChatColor.GRAY + description,
            ChatColor.GRAY + "Category: " + category.getColor() + category.getDisplayName(),
            level > 1 || maxLevel > 1 ? 
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + level + "/" + maxLevel : ""
        );
    }
}