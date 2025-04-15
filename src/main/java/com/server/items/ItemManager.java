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

public class ItemManager {
    
    public static ItemStack applyRarity(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;
        if (hasRarity(item)) return item;
        
        ItemStack modifiedItem = item.clone();
        ItemMeta meta = modifiedItem.getItemMeta();
        if (meta == null) return modifiedItem;
        
        // Get rarity for this item
        ItemRarity rarity = getItemRarity(modifiedItem.getType());

        // Set display name with rarity color
        String itemName = formatItemName(modifiedItem.getType().toString());
        meta.setDisplayName(rarity.getColor() + itemName);

        // Create new lore list
        List<String> lore = new ArrayList<>();
        
        // Add rarity information
        lore.add(ChatColor.GRAY + "Rarity: " + rarity.getFormattedName());
        lore.add(ChatColor.GRAY + "\"" + rarity.getDescription() + "\"");
        lore.add(""); // Blank line

        // Add stats based on item type
        if (isWeapon(modifiedItem.getType())) {
            double damage = getWeaponDamage(modifiedItem.getType());
            lore.add(ChatColor.GRAY + "Physical Damage: " + ChatColor.RED + "+" + damage);
            
            // No attack speed bonus for vanilla items
            // Will be added for custom items later
            AttributeModifier attackSpeed = new AttributeModifier(
                UUID.randomUUID(),
                "generic.attackSpeed",
                0.0, // No bonus attack speed for vanilla items
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
        return modifiedItem;
    }

    // For future custom items with attack speed
    public static ItemStack addAttackSpeed(ItemStack item, double attackSpeedBonus) {
        if (item == null || !item.hasItemMeta()) return item;
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Add attack speed to lore
        if (attackSpeedBonus > 0) {
            lore.add(ChatColor.GRAY + "Attack Speed: " + ChatColor.YELLOW + "+" + attackSpeedBonus);
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
        // For now, return BASIC for all vanilla items
        // This can be expanded later with custom rarity mappings
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