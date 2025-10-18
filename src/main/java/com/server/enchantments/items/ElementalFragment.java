package com.server.enchantments.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.FragmentTier;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Elemental Fragment items used for enchanting
 * 8 elements × 3 tiers = 24 types
 */
public class ElementalFragment {
    
    private static final String NBT_KEY_ELEMENT = "MMO_Fragment_Element";
    private static final String NBT_KEY_TIER = "MMO_Fragment_Tier";
    
    /**
     * Create an elemental fragment item
     */
    public static ItemStack createFragment(ElementType element, FragmentTier tier) {
        if (element == null || tier == null) return null;
        
        ItemStack fragment = new ItemStack(element.getFragmentMaterial(), 1);
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return fragment;
        
        // Set display name with colors
        String displayName = tier.getColor() + tier.getDisplayName() + " " + 
                            element.getDisplayName() + " Fragment";
        meta.setDisplayName(displayName);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        lore.add(element.getColoredIcon() + " " + element.getDisplayName() + " Element");
        lore.add(tier.getColor() + "Tier: " + tier.getDisplayName());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Hybrid Chance: " + 
                ChatColor.WHITE + (int)(tier.getHybridChanceBoost() * 100) + "%");
        lore.add(ChatColor.YELLOW + "Base Affinity: " + 
                ChatColor.WHITE + tier.getAffinityBase());
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Use in an Enchantment Table");
        lore.add(ChatColor.DARK_GRAY + "to enchant your gear!");
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        meta.setLore(lore);
        
        // Add glow effect for higher tiers
        if (tier == FragmentTier.REFINED || tier == FragmentTier.PRISTINE) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Hide other flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        fragment.setItemMeta(meta);
        
        // Store element and tier in NBT
        NBTItem nbtItem = new NBTItem(fragment);
        nbtItem.setString(NBT_KEY_ELEMENT, element.name());
        nbtItem.setString(NBT_KEY_TIER, tier.name());
        
        return nbtItem.getItem();
    }
    
    /**
     * Check if an item is an elemental fragment
     */
    public static boolean isFragment(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_KEY_ELEMENT) && nbtItem.hasKey(NBT_KEY_TIER);
    }
    
    /**
     * Get the element type from a fragment item
     */
    public static ElementType getElement(ItemStack item) {
        if (!isFragment(item)) return null;
        
        NBTItem nbtItem = new NBTItem(item);
        try {
            return ElementType.valueOf(nbtItem.getString(NBT_KEY_ELEMENT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Get the tier from a fragment item
     */
    public static FragmentTier getTier(ItemStack item) {
        if (!isFragment(item)) return null;
        
        NBTItem nbtItem = new NBTItem(item);
        try {
            return FragmentTier.valueOf(nbtItem.getString(NBT_KEY_TIER));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Get fragment data as a readable string
     */
    public static String getFragmentInfo(ItemStack item) {
        if (!isFragment(item)) return "Not a fragment";
        
        ElementType element = getElement(item);
        FragmentTier tier = getTier(item);
        
        return tier.getDisplayName() + " " + element.getDisplayName() + " Fragment";
    }
    
    /**
     * Create a full set of fragments for testing (all 24 types)
     */
    public static List<ItemStack> createAllFragments() {
        List<ItemStack> fragments = new ArrayList<>();
        
        for (ElementType element : ElementType.values()) {
            for (FragmentTier tier : FragmentTier.values()) {
                fragments.add(createFragment(element, tier));
            }
        }
        
        return fragments;
    }
}
