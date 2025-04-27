package com.server.profiles.skills.rewards.rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;

/**
 * A reward that gives the player currency
 */
public class CurrencyReward extends SkillReward {
    private final String currencyType;
    private final int amount;
    
    public CurrencyReward(int amount) {
        this("units", amount); // Default to basic currency units
    }
    
    public CurrencyReward(String currencyType, int amount) {
        super(SkillRewardType.CURRENCY, "+" + amount + " " + formatCurrencyName(currencyType));
        this.currencyType = currencyType;
        this.amount = amount;
    }
    
    @Override
    public void grantTo(Player player) {
        // Get the player's active profile
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return;
        
        // Add the currency based on the type
        switch (currencyType.toLowerCase()) {
            case "units":
            case "coins":
                profile.addUnits(amount);
                break;
            case "premium_units":
            case "premium":
                profile.addPremiumUnits(amount);
                break;
            case "essence":
                profile.addEssence(amount);
                break;
            case "bits":
                profile.addBits(amount);
                break;
            default:
                // Unknown currency type, default to basic units
                profile.addUnits(amount);
                break;
        }
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "Skill Reward: " + getDescription());
    }
    
    /**
     * Format a currency type name for display
     */
    private static String formatCurrencyName(String currencyType) {
        switch (currencyType.toLowerCase()) {
            case "units":
            case "coins":
                return "Coins";
            case "premium_units":
            case "premium":
                return "Premium Coins";
            case "essence":
                return "Essence";
            case "bits":
                return "Bits";
            default:
                return capitalizeWords(currencyType.replace('_', ' '));
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