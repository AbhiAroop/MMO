package com.server.util;

import org.bukkit.entity.Player;

/**
 * Utility class for detecting and handling Bedrock Edition players
 * 
 * Bedrock players are detected by checking if their UUID starts with "00000000-0000-0000"
 * This is the format used by Geyser/Floodgate for Bedrock players
 */
public class BedrockPlayerUtil {
    
    /**
     * Check if a player is a Bedrock Edition player
     * 
     * @param player The player to check
     * @return true if the player is connecting from Bedrock Edition
     */
    public static boolean isBedrockPlayer(Player player) {
        // Bedrock players connected through Geyser/Floodgate have UUIDs starting with 00000000-0000-0000
        String uuid = player.getUniqueId().toString();
        return uuid.startsWith("00000000-0000-0000");
    }
    
    /**
     * Check if Geyser/Floodgate is likely available
     * This is a simple check that doesn't require the API
     * 
     * @return true if Bedrock player detection is supported
     */
    public static boolean isGeyserAvailable() {
        // Always return true since we use UUID pattern matching
        return true;
    }
}
