package com.server.profiles.skills.skills.fishing.baits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Manages bait consumption and validation for fishing
 */
public class BaitManager {
    
    /**
     * Check if player has any bait in inventory
     */
    public static FishingBait findBaitInInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        
        // Check main inventory
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getAmount() > 0) {
                FishingBait bait = FishingBait.fromItem(item);
                if (bait != null) {
                    return bait;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Consume one bait from player's inventory
     * @return The bait that was consumed, or null if no bait found
     */
    public static FishingBait consumeBait(Player player) {
        PlayerInventory inventory = player.getInventory();
        
        // Check main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getAmount() > 0) {
                FishingBait bait = FishingBait.fromItem(item);
                if (bait != null) {
                    // Consume one bait
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        inventory.setItem(i, null);
                    }
                    return bait;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if player has at least one bait
     */
    public static boolean hasBait(Player player) {
        return findBaitInInventory(player) != null;
    }
    
    /**
     * Give bait to player
     */
    public static void giveBait(Player player, FishingBait bait, int amount) {
        ItemStack baitItem = bait.createItem(amount);
        player.getInventory().addItem(baitItem);
    }
}
