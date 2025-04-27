package com.server.profiles.skills.rewards.rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.items.CustomItems;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;

/**
 * A reward that gives the player an item
 */
public class ItemReward extends SkillReward {
    private final String itemId;
    private final int amount;
    
    public ItemReward(String itemId, int amount) {
        super(SkillRewardType.ITEM, amount + "x " + formatItemName(itemId));
        this.itemId = itemId;
        this.amount = amount;
    }
    
    @Override
    public void grantTo(Player player) {
        // Create the item based on the ID
        ItemStack item = createItemFromId(itemId);
        if (item == null) return;
        
        // Set the amount
        item.setAmount(amount);
        
        // Give the item to the player or drop it if inventory is full
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItem(player.getLocation(), item);
            player.sendMessage(ChatColor.YELLOW + "Your inventory is full! Item dropped on the ground.");
        }
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "Skill Reward: " + getDescription());
    }
    
    /**
     * Create an item from its ID
     */
    private ItemStack createItemFromId(String itemId) {
        // Here we use the CustomItems class to create the item
        switch (itemId.toLowerCase()) {
            case "apprentice_edge":
                return CustomItems.createApprenticeEdge();
            case "emberwood_staff":
                return CustomItems.createEmberwoodStaff();
            // Add more items here as needed
            default:
                // Unknown item
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Unknown item ID: " + itemId);
                }
                return null;
        }
    }
    
    /**
     * Format an item ID for display
     */
    private static String formatItemName(String itemId) {
        switch (itemId.toLowerCase()) {
            case "apprentice_edge":
                return "Apprentice's Edge";
            case "emberwood_staff":
                return "Emberwood Staff";
            // Add more items here as needed
            default:
                return capitalizeWords(itemId.replace('_', ' '));
        }
    }
    
    /**
     * Capitalize each word in a string
     */
    private static String capitalizeWords(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}