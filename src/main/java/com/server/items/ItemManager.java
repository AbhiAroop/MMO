package com.server.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.crafting.fuel.FuelData;
import com.server.crafting.fuel.FuelRegistry;

public class ItemManager {
    
    /**
     * Apply comprehensive item enhancements including rarity and fuel properties
     * ENHANCED: Now includes automatic fuel lore for all fuel items
     */
    public static ItemStack applyRarity(ItemStack item) {
        ItemStack modifiedItem = item.clone();
        ItemMeta meta = modifiedItem.getItemMeta();
        if (meta == null) return modifiedItem;
        
        // Apply rarity tag if it doesn't have one already
        if (!hasRarity(modifiedItem)) {
            ItemRarity rarity = getItemRarity(modifiedItem.getType());
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            
            lore.add(0, ChatColor.GRAY + "Rarity: " + rarity.getFormattedName());
            
            // Add other stats based on item type
            if (isWeapon(modifiedItem.getType())) {
                double damage = getWeaponDamage(modifiedItem.getType());
                lore.add(ChatColor.GRAY + "Physical Damage: " + ChatColor.RED + "+" + damage);
                
                // For vanilla weapons, set attack speed to 0.5 (changed from 0)
                AttributeModifier attackSpeed = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.attackSpeed",
                    0.5, // Changed from 0 to 0.5 for vanilla weapons
                    AttributeModifier.Operation.ADD_NUMBER
                );
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeed);
            }
            else if (isArmor(modifiedItem.getType())) {
                double armor = getArmorValue(modifiedItem.getType());
                lore.add(ChatColor.GRAY + "Armor: " + ChatColor.GREEN + "+" + armor);
            }

            meta.setLore(lore);
            modifiedItem.setItemMeta(meta);
        }
        
        // CRITICAL NEW FEATURE: Apply fuel lore for all fuel items automatically
        modifiedItem = applyFuelLoreIfApplicable(modifiedItem);
        
        return modifiedItem;
    }
    
    /**
     * Apply fuel lore to items that are valid fuels - NEW METHOD
     * This ensures all fuel items always display their properties
     */
    public static ItemStack applyFuelLoreIfApplicable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        
        // Check if this item is a valid fuel
        FuelData fuelData = FuelRegistry.getInstance().getFuelData(item);
        if (fuelData != null) {
            // Apply fuel lore using the existing method from FuelData
            return FuelData.applyFuelLore(item, fuelData);
        }
        
        return item;
    }
    
    /**
     * Enhanced method to apply fuel properties to any item - PUBLIC ACCESS
     * This can be called from anywhere to ensure fuel items have proper lore
     */
    public static ItemStack enhanceItemWithAllProperties(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        
        // Apply rarity first (which includes fuel lore)
        ItemStack enhanced = applyRarity(item);
        
        // Double-check fuel lore is applied
        enhanced = applyFuelLoreIfApplicable(enhanced);
        
        return enhanced;
    }
    
    /**
     * Check if an item is a fuel and needs fuel lore - NEW METHOD
     */
    public static boolean needsFuelLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check if it's a fuel
        FuelData fuelData = FuelRegistry.getInstance().getFuelData(item);
        if (fuelData == null) {
            return false;
        }
        
        // Check if it already has fuel lore
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return true;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        boolean hasFuelLore = lore.stream().anyMatch(line -> 
            line.contains("Fuel Temperature:") || line.contains("Burn Time:"));
        
        return !hasFuelLore;
    }

    // For future custom items with attack speed
    public static ItemStack addAttackSpeed(ItemStack item, double attackSpeedBonus) {
        if (item == null || !item.hasItemMeta()) return item;
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Add attack speed to lore
        if (attackSpeedBonus != 0) {
            lore.add(ChatColor.GRAY + "Attack Speed: " + 
                (attackSpeedBonus > 0 ? ChatColor.YELLOW + "+" : ChatColor.RED) + 
                attackSpeedBonus);
        }
        
        // Add attack speed attribute
        AttributeModifier attackSpeed = new AttributeModifier(
            UUID.randomUUID(),
            "generic.attackSpeed",
            attackSpeedBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeed);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Add method to check if item already has rarity
    public static boolean hasRarity(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        return lore.stream().anyMatch(line -> line.contains("Rarity:"));
    }

    private static boolean isWeapon(Material material) {
        return material.name().endsWith("_SWORD") || 
               material.name().endsWith("_AXE") ||
               material.name().endsWith("_PICKAXE") ||
               material.name().endsWith("_SHOVEL") ||
               material.name().endsWith("_HOE");
    }

    private static boolean isArmor(Material material) {
        return material.name().endsWith("_HELMET") ||
               material.name().endsWith("_CHESTPLATE") ||
               material.name().endsWith("_LEGGINGS") ||
               material.name().endsWith("_BOOTS");
    }

    private static double getWeaponDamage(Material material) {
        // Base damage values for weapons
        if (material.name().contains("NETHERITE")) return 8;
        if (material.name().contains("DIAMOND")) return 7;
        if (material.name().contains("IRON")) return 6;
        if (material.name().contains("STONE")) return 5;
        if (material.name().contains("WOODEN")) return 4;
        return 1; // Default damage
    }

    private static double getArmorValue(Material material) {
        // Base armor values for all vanilla armor types
        if (material.name().contains("NETHERITE")) {
            if (material.name().contains("HELMET")) return 3;
            if (material.name().contains("CHESTPLATE")) return 8;
            if (material.name().contains("LEGGINGS")) return 6;
            if (material.name().contains("BOOTS")) return 3;
        }
        if (material.name().contains("DIAMOND")) {
            if (material.name().contains("HELMET")) return 3;
            if (material.name().contains("CHESTPLATE")) return 8;
            if (material.name().contains("LEGGINGS")) return 6;
            if (material.name().contains("BOOTS")) return 3;
        }
        if (material.name().contains("IRON")) {
            if (material.name().contains("HELMET")) return 2;
            if (material.name().contains("CHESTPLATE")) return 6;
            if (material.name().contains("LEGGINGS")) return 5;
            if (material.name().contains("BOOTS")) return 2;
        }
        if (material.name().contains("CHAINMAIL")) {
            if (material.name().contains("HELMET")) return 2;
            if (material.name().contains("CHESTPLATE")) return 5;
            if (material.name().contains("LEGGINGS")) return 4;
            if (material.name().contains("BOOTS")) return 1;
        }
        if (material.name().contains("GOLDEN")) {
            if (material.name().contains("HELMET")) return 2;
            if (material.name().contains("CHESTPLATE")) return 5;
            if (material.name().contains("LEGGINGS")) return 3;
            if (material.name().contains("BOOTS")) return 1;
        }
        if (material.name().contains("LEATHER")) {
            if (material.name().contains("HELMET")) return 1;
            if (material.name().contains("CHESTPLATE")) return 3;
            if (material.name().contains("LEGGINGS")) return 2;
            if (material.name().contains("BOOTS")) return 1;
        }
        
        return 1; // Default armor value
    }

    private static ItemRarity getItemRarity(Material material) {
        // Enhanced rarity detection for fuel items
        if (FuelRegistry.getInstance().isFuel(new ItemStack(material))) {
            FuelData fuelData = FuelRegistry.getInstance().getFuelData(new ItemStack(material));
            if (fuelData != null) {
                int temperature = fuelData.getTemperature();
                
                // Assign rarity based on fuel temperature
                if (temperature >= 1500) return ItemRarity.ARCANE;      // Very high temp fuels
                if (temperature >= 800) return ItemRarity.RARE;        // High temp fuels  
                if (temperature >= 400) return ItemRarity.ENHANCED;    // Medium temp fuels
                return ItemRarity.BASIC;                               // Low temp fuels
            }
        }
        
        // For non-fuel items, return BASIC for all vanilla items
        return ItemRarity.BASIC;
    }

    private static String formatItemName(String materialName) {
        // Convert DIAMOND_SWORD to Diamond Sword
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();
        
        for (String word : words) {
            if (formattedName.length() > 0) formattedName.append(" ");
            formattedName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return formattedName.toString();
    }
}