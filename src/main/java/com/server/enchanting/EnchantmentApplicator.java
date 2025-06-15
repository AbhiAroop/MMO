package com.server.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Handles applying custom enchantments to items
 */
public class EnchantmentApplicator {
    
    private static final String ENCHANTMENT_PREFIX = "custom_enchant_";
    
    /**
     * Apply a custom enchantment to an item
     */
    public static ItemStack applyEnchantment(ItemStack item, CustomEnchantment enchantment, int level) {
        if (item == null || enchantment == null || level <= 0) {
            return null;
        }
        
        ItemStack enchantedItem = item.clone();
        ItemMeta meta = enchantedItem.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        // Check if item already has conflicting enchantments
        if (hasConflictingEnchantment(enchantedItem, enchantment)) {
            return null;
        }
        
        // Store enchantment data in persistent data container
        NamespacedKey enchantmentKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantment.getId());
        meta.getPersistentDataContainer().set(enchantmentKey, PersistentDataType.INTEGER, level);
        
        // Add enchantment to lore
        addEnchantmentToLore(meta, enchantment, level);
        
        // Apply visual glint effect
        meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        
        enchantedItem.setItemMeta(meta);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchantment Applicator] Applied " + enchantment.getFormattedName(level) + 
                " to " + item.getType().name());
        }
        
        return enchantedItem;
    }
    
    /**
     * Check if item has conflicting enchantments
     */
    public static boolean hasConflictingEnchantment(ItemStack item, CustomEnchantment newEnchantment) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for conflicts with existing custom enchantments
        for (String conflictId : newEnchantment.getConflictingEnchantments()) {
            NamespacedKey conflictKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + conflictId);
            if (meta.getPersistentDataContainer().has(conflictKey, PersistentDataType.INTEGER)) {
                return true;
            }
        }
        
        // Check if trying to apply same enchantment (should upgrade instead)
        NamespacedKey enchantmentKey = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + newEnchantment.getId());
        return meta.getPersistentDataContainer().has(enchantmentKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Get all custom enchantments on an item
     */
    public static Map<String, Integer> getCustomEnchantments(ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return enchantments;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check all registered enchantments
        for (CustomEnchantment enchantment : CustomEnchantmentRegistry.getInstance().getAllEnchantments()) {
            NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantment.getId());
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
                int level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                enchantments.put(enchantment.getId(), level);
            }
        }
        
        return enchantments;
    }
    
    /**
     * Get the level of a specific enchantment on an item
     */
    public static int getEnchantmentLevel(ItemStack item, String enchantmentId) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantmentId);
        
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Remove a custom enchantment from an item
     */
    public static ItemStack removeEnchantment(ItemStack item, String enchantmentId) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack modifiedItem = item.clone();
        ItemMeta meta = modifiedItem.getItemMeta();
        
        // Remove from persistent data
        NamespacedKey key = new NamespacedKey(Main.getInstance(), ENCHANTMENT_PREFIX + enchantmentId);
        meta.getPersistentDataContainer().remove(key);
        
        // Remove from lore
        removeEnchantmentFromLore(meta, enchantmentId);
        
        // Remove glint if no enchantments remain
        if (getCustomEnchantments(modifiedItem).isEmpty()) {
            meta.removeEnchant(org.bukkit.enchantments.Enchantment.PROTECTION);
            meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        modifiedItem.setItemMeta(meta);
        return modifiedItem;
    }
    
    /**
     * Add enchantment to item lore
     */
    private static void addEnchantmentToLore(ItemMeta meta, CustomEnchantment enchantment, int level) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Find insertion point for enchantments
        int insertIndex = findEnchantmentInsertionPoint(lore);
        
        // Add enchantment section header if needed
        if (!hasEnchantmentSection(lore)) {
            lore.add(insertIndex, "");
            lore.add(insertIndex + 1, ChatColor.LIGHT_PURPLE + "✦ Enchantments:");
            insertIndex += 2;
        }
        
        // Add the enchantment
        String enchantmentLine = ChatColor.GRAY + "• " + enchantment.getFormattedName(level);
        lore.add(insertIndex, enchantmentLine);
        
        // Add enchantment description if it's the first enchantment
        if (countEnchantmentsInLore(lore) == 1) {
            lore.add(insertIndex + 1, "");
            lore.add(insertIndex + 2, ChatColor.DARK_GRAY + "» " + enchantment.getDescription());
        }
        
        meta.setLore(lore);
    }
    
    /**
     * Remove enchantment from item lore
     */
    private static void removeEnchantmentFromLore(ItemMeta meta, String enchantmentId) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null) {
            return;
        }
        
        // Remove the enchantment line
        lore.removeIf(line -> line.contains(enchantment.getDisplayName()));
        
        // Clean up enchantment section if empty
        if (countEnchantmentsInLore(lore) == 0) {
            // Remove enchantment header and empty lines
            for (int i = lore.size() - 1; i >= 0; i--) {
                String line = lore.get(i);
                if (line.contains("✦ Enchantments:") || 
                    (line.trim().isEmpty() && i > 0 && lore.get(i - 1).contains("✦ Enchantments:"))) {
                    lore.remove(i);
                }
            }
        }
        
        meta.setLore(lore);
    }
    
    /**
     * Find the best insertion point for enchantments in lore
     */
    private static int findEnchantmentInsertionPoint(List<String> lore) {
        // Insert after stats but before flavor text
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            
            // Look for flavor text indicators
            if (line.startsWith(ChatColor.ITALIC.toString()) || 
                line.startsWith(ChatColor.GRAY + "\"") ||
                line.contains("This ") || line.contains("A ") || line.contains("An ")) {
                return i;
            }
        }
        
        // If no flavor text found, add at the end
        return lore.size();
    }
    
    /**
     * Check if lore already has an enchantment section
     */
    private static boolean hasEnchantmentSection(List<String> lore) {
        return lore.stream().anyMatch(line -> line.contains("✦ Enchantments:"));
    }
    
    /**
     * Count enchantments in lore
     */
    private static int countEnchantmentsInLore(List<String> lore) {
        int count = 0;
        boolean inEnchantSection = false;
        
        for (String line : lore) {
            if (line.contains("✦ Enchantments:")) {
                inEnchantSection = true;
                continue;
            }
            
            if (inEnchantSection) {
                if (line.trim().isEmpty() && !line.contains("•")) {
                    break; // End of enchant section
                }
                if (line.contains("•")) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Check if an item has any custom enchantments
     */
    public static boolean hasCustomEnchantments(ItemStack item) {
        return !getCustomEnchantments(item).isEmpty();
    }
    
    /**
     * Get a formatted list of all enchantments on an item
     */
    public static List<String> getEnchantmentLore(ItemStack item) {
        List<String> enchantmentLore = new ArrayList<>();
        Map<String, Integer> enchantments = getCustomEnchantments(item);
        
        if (enchantments.isEmpty()) {
            return enchantmentLore;
        }
        
        enchantmentLore.add(ChatColor.LIGHT_PURPLE + "✦ Enchantments:");
        
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(entry.getKey());
            if (enchantment != null) {
                int level = entry.getValue();
                enchantmentLore.add(ChatColor.GRAY + "• " + enchantment.getFormattedName(level));
            }
        }
        
        return enchantmentLore;
    }
    
    /**
     * Upgrade an existing enchantment on an item
     */
    public static ItemStack upgradeEnchantment(ItemStack item, String enchantmentId, int newLevel) {
        CustomEnchantment enchantment = CustomEnchantmentRegistry.getInstance().getEnchantment(enchantmentId);
        if (enchantment == null) {
            return item;
        }
        
        // Remove old enchantment
        ItemStack upgradedItem = removeEnchantment(item, enchantmentId);
        
        // Apply new level
        return applyEnchantment(upgradedItem, enchantment, Math.min(newLevel, enchantment.getMaxLevel()));
    }
    
    /**
     * Clear all custom enchantments from an item
     */
    public static ItemStack clearAllEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }
        
        ItemStack clearedItem = item.clone();
        Map<String, Integer> enchantments = getCustomEnchantments(clearedItem);
        
        for (String enchantmentId : enchantments.keySet()) {
            clearedItem = removeEnchantment(clearedItem, enchantmentId);
        }
        
        return clearedItem;
    }
}