package com.server.enchantments.utils;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for validating equipment types based on custom model data patterns.
 * 
 * Custom Model Data Ranges:
 * - Weapons: 210000-219999 (Swords, Daggers, Spears, etc.)
 * - Armor: 230000-239999 (Helmets, Chestplates, Leggings, Boots)
 * - Accessories: 240000-249999 (Rings, Amulets, Cloaks)
 * - Shields: 220000-229999
 * - Ranged: 250000-259999 (Bows, Crossbows)
 * - Staves/Magic: 260000-269999
 * - Tools: 270000-279999
 * - Special: 280000-289999
 */
public class EquipmentTypeValidator {
    
    // Custom Model Data Ranges
    private static final int WEAPON_MIN = 210000;
    private static final int WEAPON_MAX = 219999;
    
    private static final int SHIELD_MIN = 220000;
    private static final int SHIELD_MAX = 229999;
    
    private static final int ARMOR_MIN = 230000;
    private static final int ARMOR_MAX = 239999;
    
    private static final int ACCESSORY_MIN = 240000;
    private static final int ACCESSORY_MAX = 249999;
    
    private static final int RANGED_MIN = 250000;
    private static final int RANGED_MAX = 259999;
    
    private static final int STAFF_MIN = 260000;
    private static final int STAFF_MAX = 269999;
    
    /**
     * Get the equipment type from custom model data
     */
    public static EquipmentType getEquipmentType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return EquipmentType.UNKNOWN;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData()) {
            return EquipmentType.UNKNOWN;
        }
        
        int modelData = meta.getCustomModelData();
        
        if (modelData >= WEAPON_MIN && modelData <= WEAPON_MAX) {
            return EquipmentType.WEAPON;
        } else if (modelData >= SHIELD_MIN && modelData <= SHIELD_MAX) {
            return EquipmentType.SHIELD;
        } else if (modelData >= ARMOR_MIN && modelData <= ARMOR_MAX) {
            return EquipmentType.ARMOR;
        } else if (modelData >= ACCESSORY_MIN && modelData <= ACCESSORY_MAX) {
            return EquipmentType.ACCESSORY;
        } else if (modelData >= RANGED_MIN && modelData <= RANGED_MAX) {
            return EquipmentType.RANGED;
        } else if (modelData >= STAFF_MIN && modelData <= STAFF_MAX) {
            return EquipmentType.STAFF;
        }
        
        return EquipmentType.UNKNOWN;
    }
    
    /**
     * Check if an item is a weapon (melee or ranged)
     */
    public static boolean isWeapon(ItemStack item) {
        EquipmentType type = getEquipmentType(item);
        return type == EquipmentType.WEAPON || type == EquipmentType.RANGED || type == EquipmentType.STAFF;
    }
    
    /**
     * Check if an item is armor or accessory
     */
    public static boolean isArmor(ItemStack item) {
        EquipmentType type = getEquipmentType(item);
        return type == EquipmentType.ARMOR || type == EquipmentType.ACCESSORY;
    }
    
    /**
     * Check if an item is a shield
     */
    public static boolean isShield(ItemStack item) {
        return getEquipmentType(item) == EquipmentType.SHIELD;
    }
    
    /**
     * Check if an enchantment can be applied to the item
     * This is a helper method that uses the enchantment's own canApplyTo method
     * but provides additional validation based on equipment type expectations
     */
    public static boolean canEnchantmentApply(ItemStack item, com.server.enchantments.data.CustomEnchantment enchantment) {
        // First check the enchantment's own validation
        if (!enchantment.canApplyTo(item)) {
            return false;
        }
        
        // If item has custom model data, validate it matches expected equipment types
        EquipmentType itemType = getEquipmentType(item);
        if (itemType == EquipmentType.UNKNOWN) {
            // No custom model data, rely on enchantment's canApplyTo
            return true;
        }
        
        // Get expected equipment types from enchantment class name and trigger type
        // Offensive enchantments usually go on weapons
        // Defensive/Utility usually go on armor
        String packageName = enchantment.getClass().getPackage().getName();
        
        if (packageName.contains("offensive")) {
            // Offensive enchantments should be on weapons, ranged, or staves
            return isWeapon(item) || isShield(item);
        } else if (packageName.contains("defensive") || packageName.contains("utility")) {
            // Defensive/Utility enchantments can be on armor, accessories, or some weapons
            return isArmor(item) || isWeapon(item) || isShield(item);
        }
        
        // Default: trust the enchantment's canApplyTo
        return true;
    }
    
    /**
     * Get a human-readable description of valid equipment types for an enchantment
     */
    public static String getEquipmentDescription(com.server.enchantments.data.CustomEnchantment enchantment) {
        // Extract from the javadoc comment in the enchantment class
        String className = enchantment.getClass().getSimpleName();
        
        // Common patterns based on enchantment type
        String packageName = enchantment.getClass().getPackage().getName();
        
        if (packageName.contains("offensive")) {
            // Check specific enchantments
            switch (className) {
                case "Cinderwake":
                    return "Swords, Axes, Tridents";
                case "Deepcurrent":
                    return "Swords, Axes, Tridents (Heavy Weapons)";
                case "BurdenedStone":
                    return "Axes, Tridents, Shields (Hammers/Maces)";
                case "Voltbrand":
                    return "Swords, Tridents, Crossbows";
                case "HollowEdge":
                    return "Swords, Hoes (Daggers/Scythes)";
                case "Dawnstrike":
                    return "Swords, Axes (Maces)";
                case "Stormfire":
                    return "Swords, Axes (Maces)";
                case "Decayroot":
                    return "Axes, Sticks (Maces/Hammers/Staves)";
                case "CelestialSurge":
                    return "Swords, Tridents, Crossbows";
                case "Embershade":
                    return "Swords, Hoes, Bows (Daggers/Scythes)";
                case "InfernoStrike":
                case "Frostflow":
                    return "All Weapons";
                default:
                    return "Weapons";
            }
        } else if (packageName.contains("defensive")) {
            switch (className) {
                case "Mistveil":
                    return "All Armor Pieces";
                case "Terraheart":
                    return "Chestplates, Shields";
                case "Whispers":
                    return "Helmets, Light Armor";
                default:
                    return "Armor";
            }
        } else if (packageName.contains("utility")) {
            switch (className) {
                case "AshenVeil":
                    return "Light Armor (Leather, Chainmail)";
                case "GaleStep":
                    return "Boots, Light Weapons";
                case "ArcNexus":
                    return "All Armor Pieces";
                case "Veilborn":
                    return "All Armor Pieces";
                case "RadiantGrace":
                    return "All Armor Pieces";
                case "MistborneTempest":
                    return "All Armor Pieces (Boots Preferred)";
                case "PureReflection":
                    return "All Armor Pieces (Chestplate Preferred)";
                case "EmberVeil":
                    return "All Armor Pieces";
                default:
                    return "Armor/Accessories";
            }
        }
        
        return "Various Equipment";
    }
    
    /**
     * Get a colored error message when enchantment cannot be applied
     */
    public static String getIncompatibilityMessage(ItemStack item, com.server.enchantments.data.CustomEnchantment enchantment) {
        EquipmentType itemType = getEquipmentType(item);
        String expectedEquipment = getEquipmentDescription(enchantment);
        
        return ChatColor.RED + "✗ " + enchantment.getDisplayName() + ChatColor.GRAY + 
               " cannot be applied to this item!\n" +
               ChatColor.GRAY + "Required: " + ChatColor.YELLOW + expectedEquipment + "\n" +
               ChatColor.GRAY + "Current item type: " + ChatColor.YELLOW + 
               (itemType == EquipmentType.UNKNOWN ? "Unknown (no custom model data)" : itemType.getDisplayName());
    }
    
    /**
     * Equipment type enum
     */
    public enum EquipmentType {
        WEAPON("Weapon", ChatColor.RED),
        SHIELD("Shield", ChatColor.BLUE),
        ARMOR("Armor", ChatColor.AQUA),
        ACCESSORY("Accessory", ChatColor.LIGHT_PURPLE),
        RANGED("Ranged Weapon", ChatColor.GOLD),
        STAFF("Staff/Wand", ChatColor.DARK_PURPLE),
        UNKNOWN("Unknown", ChatColor.GRAY);
        
        private final String displayName;
        private final ChatColor color;
        
        EquipmentType(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return color + displayName;
        }
        
        public ChatColor getColor() {
            return color;
        }
    }
}
