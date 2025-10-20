package com.server.enchantments.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchantments.data.EnchantmentData;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Enchantment Tome system - replaces vanilla books for custom enchanting.
 * 
 * Two types:
 * 1. Enchantment Tome (Unenchanted) - Can be enchanted with ANY enchantment
 * 2. Enchanted Tome - Holds enchantments to be applied via anvil
 * 
 * IMPORTANT: Each tome has a unique UUID to prevent stacking.
 * This ensures that only ONE tome is consumed when enchanting, not the entire stack.
 * 
 * Custom Model Data Pattern: 4X0YZZ
 * - 4: Tome prefix
 * - X: Type (0=Unenchanted, 1=Enchanted)
 * - 0: Separator
 * - Y: Reserved (0)
 * - ZZ: Variant (00)
 * 
 * Examples:
 * - Unenchanted Tome: 400000
 * - Enchanted Tome: 410000
 */
public class EnchantmentTome {
    
    private static final Random RANDOM = new Random();
    
    private static final String NBT_KEY_TOME_TYPE = "MMO_Tome_Type";
    private static final String NBT_VALUE_UNENCHANTED = "UNENCHANTED";
    private static final String NBT_VALUE_ENCHANTED = "ENCHANTED";
    
    // Custom Model Data
    private static final int TOME_MODEL_BASE = 400000;
    private static final int UNENCHANTED_TOME_MODEL = 400000; // 4 00 0 00
    private static final int ENCHANTED_TOME_MODEL = 410000;   // 4 10 0 00
    
    /**
     * Create an unenchanted Enchantment Tome
     */
    public static ItemStack createUnenchantedTome() {
        ItemStack tome = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = tome.getItemMeta();
        if (meta == null) return tome;
        
        // Set display name
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Enchantment Tome");
        
        // Set custom model data
        meta.setCustomModelData(UNENCHANTED_TOME_MODEL);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "✦ Universal Enchanting Medium ✦");
        lore.add("");
        lore.add(ChatColor.GRAY + "A mystical tome that can hold");
        lore.add(ChatColor.GRAY + "enchantments for " + ChatColor.WHITE + "any equipment type" + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GOLD + "▸ " + ChatColor.WHITE + "Place in Enchantment Table");
        lore.add(ChatColor.GOLD + "▸ " + ChatColor.WHITE + "Add Elemental Fragments");
        lore.add(ChatColor.GOLD + "▸ " + ChatColor.WHITE + "Receive Enchanted Tome");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Apply enchantments via anvil");
        lore.add(ChatColor.DARK_GRAY + "(Future Feature)");
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        meta.setLore(lore);
        
        // Hide flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        tome.setItemMeta(meta);
        
        // Store tome type in NBT
        NBTItem nbtItem = new NBTItem(tome);
        nbtItem.setString(NBT_KEY_TOME_TYPE, NBT_VALUE_UNENCHANTED);
        
        // IMPORTANT: Add unique UUID to make tome unstackable
        // This prevents entire stacks from being consumed when enchanting
        nbtItem.setString("MMO_Tome_UUID", java.util.UUID.randomUUID().toString());
        
        return nbtItem.getItem();
    }
    
    /**
     * Convert an unenchanted tome to an enchanted tome with the given enchantments
     */
    public static ItemStack createEnchantedTome(ItemStack unenchantedTome) {
        if (!isUnenchantedTome(unenchantedTome)) {
            return null;
        }
        
        // Get enchantments from the tome (should have been added by EnchantmentApplicator)
        List<EnchantmentData> enchantments = 
            EnchantmentData.getEnchantmentsFromItem(unenchantedTome);
        
        if (enchantments.isEmpty()) {
            return null;
        }
        
        // Create new enchanted tome
        ItemStack enchantedTome = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = enchantedTome.getItemMeta();
        if (meta == null) return enchantedTome;
        
        // Set display name
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Enchanted Tome");
        
        // Set custom model data
        meta.setCustomModelData(ENCHANTED_TOME_MODEL);
        
        // Add enchantment glow
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Hide flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        // IMPORTANT: Apply meta BEFORE NBT operations
        enchantedTome.setItemMeta(meta);
        
        // Copy enchantment data from unenchanted tome and generate apply chances
        NBTItem sourceNBT = new NBTItem(unenchantedTome);
        NBTItem targetNBT = new NBTItem(enchantedTome);
        
        // Copy all enchantment NBT data
        int enchantCount = sourceNBT.getInteger("MMO_EnchantmentCount");
        targetNBT.setInteger("MMO_EnchantmentCount", enchantCount);
        
        // Store apply chances for use in lore
        int[] applyChances = new int[enchantCount];
        
        for (int i = 0; i < enchantCount; i++) {
            String prefix = "MMO_Enchantment_" + i + "_";
            
            // Copy all enchantment data
            if (sourceNBT.hasKey(prefix + "ID")) {
                targetNBT.setString(prefix + "ID", sourceNBT.getString(prefix + "ID"));
            }
            if (sourceNBT.hasKey(prefix + "Quality")) {
                targetNBT.setString(prefix + "Quality", sourceNBT.getString(prefix + "Quality"));
            }
            if (sourceNBT.hasKey(prefix + "Level")) {
                targetNBT.setString(prefix + "Level", sourceNBT.getString(prefix + "Level"));
            }
            if (sourceNBT.hasKey(prefix + "Element")) {
                targetNBT.setString(prefix + "Element", sourceNBT.getString(prefix + "Element"));
            }
            if (sourceNBT.hasKey(prefix + "Hybrid")) {
                targetNBT.setString(prefix + "Hybrid", sourceNBT.getString(prefix + "Hybrid"));
            }
            if (sourceNBT.hasKey(prefix + "Affinity_Offensive")) {
                targetNBT.setInteger(prefix + "Affinity_Offensive", sourceNBT.getInteger(prefix + "Affinity_Offensive"));
            }
            if (sourceNBT.hasKey(prefix + "Affinity_Defensive")) {
                targetNBT.setInteger(prefix + "Affinity_Defensive", sourceNBT.getInteger(prefix + "Affinity_Defensive"));
            }
            if (sourceNBT.hasKey(prefix + "Affinity_Utility")) {
                targetNBT.setInteger(prefix + "Affinity_Utility", sourceNBT.getInteger(prefix + "Affinity_Utility"));
            }
            
            // IMPORTANT: Generate and store random apply chance (0-100%)
            // This will be used by the anvil system to determine if enchantment transfers
            int applyChance = RANDOM.nextInt(101); // 0 to 100 inclusive
            targetNBT.setInteger(prefix + "ApplyChance", applyChance);
            applyChances[i] = applyChance; // Store for lore generation
        }
        
        // Mark as enchanted tome
        targetNBT.setString(NBT_KEY_TOME_TYPE, NBT_VALUE_ENCHANTED);
        
        // Get the item with NBT data
        ItemStack tomeWithNBT = targetNBT.getItem();
        
        // Now create lore with the apply chances we generated
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "✦ Enchanted Tome ✦");
        lore.add("");
        
        // List enchantments
        if (enchantments.size() == 1) {
            lore.add(ChatColor.GOLD + "Contains " + ChatColor.WHITE + "1 Enchantment" + ChatColor.GOLD + ":");
        } else {
            lore.add(ChatColor.GOLD + "Contains " + ChatColor.WHITE + enchantments.size() + " Enchantments" + ChatColor.GOLD + ":");
        }
        lore.add("");
        
        for (int i = 0; i < enchantments.size(); i++) {
            EnchantmentData enchantment = enchantments.get(i);
            
            // Get enchantment display text from EnchantmentData
            com.server.enchantments.EnchantmentRegistry registry = 
                com.server.enchantments.EnchantmentRegistry.getInstance();
            com.server.enchantments.data.CustomEnchantment enchantObj = 
                registry.getEnchantment(enchantment.getEnchantmentId());
            
            String enchantName = enchantObj != null ? enchantObj.getDisplayName() : enchantment.getEnchantmentId();
            String qualityColor = enchantment.getQuality().getColor().toString();
            String levelRoman = enchantment.getLevel().getRoman();
            String qualityName = enchantment.getQuality().getDisplayName();
            
            // Get the apply chance we generated earlier
            int applyChance = applyChances[i];
            
            // Determine chance color based on value
            ChatColor chanceColor;
            if (applyChance >= 80) {
                chanceColor = ChatColor.GREEN; // High chance (80-100%)
            } else if (applyChance >= 50) {
                chanceColor = ChatColor.YELLOW; // Medium chance (50-79%)
            } else if (applyChance >= 25) {
                chanceColor = ChatColor.GOLD; // Low-medium chance (25-49%)
            } else {
                chanceColor = ChatColor.RED; // Low chance (0-24%)
            }
            
            // Add enchantment line with apply chance
            lore.add(qualityColor + "▸ " + enchantName + " " + levelRoman + " [" + qualityName + "]");
            lore.add(ChatColor.GRAY + "  Apply Chance: " + chanceColor + applyChance + "%");
            
            // Add description if available
            if (enchantObj != null) {
                String description = enchantObj.getDescription();
                if (description != null && !description.isEmpty()) {
                    // Word wrap the description at ~40 characters
                    String[] words = description.split(" ");
                    StringBuilder line = new StringBuilder(ChatColor.GRAY + "  ");
                    for (String word : words) {
                        if (line.length() + word.length() > 42) {
                            lore.add(line.toString().trim());
                            line = new StringBuilder(ChatColor.GRAY + "  ");
                        }
                        line.append(word).append(" ");
                    }
                    if (line.length() > 3) {
                        lore.add(line.toString().trim());
                    }
                }
            }
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Use in an anvil to apply");
        lore.add(ChatColor.GRAY + "enchantments to equipment.");
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "✦ Universal - Works on any gear ✦");
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        
        // Apply the lore to the tome with NBT
        ItemMeta finalMeta = tomeWithNBT.getItemMeta();
        if (finalMeta != null) {
            finalMeta.setLore(lore);
            // SAFEGUARD: Re-apply custom model data to ensure it's preserved
            finalMeta.setCustomModelData(ENCHANTED_TOME_MODEL);
            tomeWithNBT.setItemMeta(finalMeta);
        }
        
        return tomeWithNBT;
    }
    
    /**
     * Check if an item is an unenchanted tome
     */
    public static boolean isUnenchantedTome(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }
        
        // Check custom model data
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            if (modelData == UNENCHANTED_TOME_MODEL) {
                return true;
            }
        }
        
        // Check NBT as fallback
        NBTItem nbtItem = new NBTItem(item);
        return NBT_VALUE_UNENCHANTED.equals(nbtItem.getString(NBT_KEY_TOME_TYPE));
    }
    
    /**
     * Check if an item is an enchanted tome
     */
    public static boolean isEnchantedTome(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }
        
        // Check custom model data
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            if (modelData == ENCHANTED_TOME_MODEL) {
                return true;
            }
        }
        
        // Check NBT as fallback
        NBTItem nbtItem = new NBTItem(item);
        return NBT_VALUE_ENCHANTED.equals(nbtItem.getString(NBT_KEY_TOME_TYPE));
    }
    
    /**
     * Check if an item is any type of tome (enchanted or unenchanted)
     */
    public static boolean isTome(ItemStack item) {
        return isUnenchantedTome(item) || isEnchantedTome(item);
    }
    
    /**
     * Get tome type as string for display
     */
    public static String getTomeType(ItemStack item) {
        if (isUnenchantedTome(item)) {
            return "Unenchanted Tome";
        } else if (isEnchantedTome(item)) {
            return "Enchanted Tome";
        }
        return "Not a tome";
    }
    
    /**
     * Check if a tome can be enchanted (must be unenchanted and have no enchantments yet)
     */
    public static boolean canBeEnchanted(ItemStack item) {
        if (!isUnenchantedTome(item)) {
            return false;
        }
        
        // Check if already has enchantments
        List<EnchantmentData> enchants = 
            EnchantmentData.getEnchantmentsFromItem(item);
        
        return enchants.isEmpty();
    }
}
