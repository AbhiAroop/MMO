package com.server.enchantments.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Custom Anvil placeholder item.
 * Similar to the enchantment altar, this is an armor stand that opens the anvil GUI.
 */
public class CustomAnvil {
    
    private static final String ANVIL_NAME = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⚒ Custom Anvil ⚒";
    private static final int CUSTOM_MODEL_DATA = 500000; // 5X0YZZ pattern for anvil
    
    /**
     * Creates a custom anvil armor stand item.
     */
    public static ItemStack create() {
        ItemStack anvil = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = anvil.getItemMeta();
        if (meta == null) return anvil;
        
        meta.setDisplayName(ANVIL_NAME);
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "⚒ Custom Anvil ⚒");
        lore.add("");
        lore.add(ChatColor.GRAY + "Combine items, enchanted tomes,");
        lore.add(ChatColor.GRAY + "and fragments to enhance your gear.");
        lore.add("");
        lore.add(ChatColor.GOLD + "Features:");
        lore.add(ChatColor.YELLOW + "  • Combine two items");
        lore.add(ChatColor.YELLOW + "  • Apply enchanted tomes");
        lore.add(ChatColor.YELLOW + "  • Boost tome apply chances");
        lore.add(ChatColor.YELLOW + "  • Merge enchantments");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Right-click to use");
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        
        meta.setLore(lore);
        anvil.setItemMeta(meta);
        
        return anvil;
    }
    
    /**
     * Checks if an item is a custom anvil.
     */
    public static boolean isCustomAnvil(ItemStack item) {
        if (item == null || item.getType() != Material.ARMOR_STAND) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return false;
        }
        
        return meta.getCustomModelData() == CUSTOM_MODEL_DATA;
    }
}
