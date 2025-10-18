package com.server.enchantments;

import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.FragmentTier;
import com.server.enchantments.items.ElementalFragment;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Registry for elemental fragments
 * Handles fragment creation, identification, and management
 */
public class FragmentRegistry {
    
    /**
     * Create a fragment item
     */
    public static ItemStack createFragment(ElementType element, FragmentTier tier) {
        return ElementalFragment.createFragment(element, tier);
    }
    
    /**
     * Create a fragment item with custom amount
     */
    public static ItemStack createFragment(ElementType element, FragmentTier tier, int amount) {
        ItemStack fragment = ElementalFragment.createFragment(element, tier);
        if (fragment != null) {
            fragment.setAmount(Math.min(amount, 64));
        }
        return fragment;
    }
    
    /**
     * Check if item is a fragment
     */
    public static boolean isFragment(ItemStack item) {
        return ElementalFragment.isFragment(item);
    }
    
    /**
     * Get element from fragment
     */
    public static ElementType getElement(ItemStack item) {
        return ElementalFragment.getElement(item);
    }
    
    /**
     * Get tier from fragment
     */
    public static FragmentTier getTier(ItemStack item) {
        return ElementalFragment.getTier(item);
    }
    
    /**
     * Get fragment display info
     */
    public static String getFragmentInfo(ItemStack item) {
        return ElementalFragment.getFragmentInfo(item);
    }
    
    /**
     * Give a fragment to a player
     */
    public static void giveFragment(Player player, ElementType element, FragmentTier tier, int amount) {
        ItemStack fragment = createFragment(element, tier, amount);
        if (fragment != null) {
            player.getInventory().addItem(fragment);
            player.sendMessage(ChatColor.GREEN + "Received " + amount + "x " + 
                             getFragmentInfo(fragment));
        }
    }
    
    /**
     * Give all fragments to a player (for testing)
     */
    public static void giveAllFragments(Player player) {
        List<ItemStack> allFragments = ElementalFragment.createAllFragments();
        for (ItemStack fragment : allFragments) {
            player.getInventory().addItem(fragment);
        }
        player.sendMessage(ChatColor.GREEN + "Received all " + allFragments.size() + 
                         " fragment types!");
    }
    
    /**
     * Create a full set of fragments for display/testing
     */
    public static List<ItemStack> getAllFragments() {
        return ElementalFragment.createAllFragments();
    }
    
    /**
     * Validate fragment for enchanting
     */
    public static boolean isValidFragment(ItemStack item) {
        if (!isFragment(item)) return false;
        ElementType element = getElement(item);
        FragmentTier tier = getTier(item);
        return element != null && tier != null;
    }
    
    /**
     * Get fragment count in inventory
     */
    public static int countFragments(Player player, ElementType element, FragmentTier tier) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFragment(item) && 
                getElement(item) == element && 
                getTier(item) == tier) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Remove fragments from inventory
     */
    public static boolean removeFragments(Player player, ElementType element, 
                                         FragmentTier tier, int amount) {
        // First check if player has enough
        int available = countFragments(player, element, tier);
        if (available < amount) return false;
        
        // Remove the fragments
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            
            if (isFragment(item) && 
                getElement(item) == element && 
                getTier(item) == tier) {
                
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        return true;
    }
}
