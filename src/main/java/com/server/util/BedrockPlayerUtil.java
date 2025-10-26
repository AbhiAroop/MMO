package com.server.util;

import org.bukkit.entity.Player;

import com.server.profiles.skills.display.SkillActionBarManager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Utility class for detecting and handling Bedrock Edition players
 * 
 * Bedrock players are detected by checking if their UUID starts with "00000000-0000-0000"
 * This is the format used by Geyser/Floodgate for Bedrock players
 */
public class BedrockPlayerUtil {
    
    // Duration for action bar messages (5 seconds = 100 ticks)
    private static final int ACTION_BAR_DURATION = 100;
    
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
    
    /**
     * Send an action bar message to a Bedrock player
     * This uses the custom action bar system which takes priority over skill XP displays
     * and lasts longer (5 seconds instead of 3)
     * 
     * @param player The player to send the message to (should be Bedrock player)
     * @param message The message to display
     */
    public static void sendActionBar(Player player, String message) {
        if (!isBedrockPlayer(player)) {
            return;
        }
        
        try {
            SkillActionBarManager manager = SkillActionBarManager.getInstance();
            manager.showCustomActionBar(player, message, ACTION_BAR_DURATION);
        } catch (IllegalStateException e) {
            // If manager not initialized, send directly
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                TextComponent.fromLegacyText(message));
        }
    }
    
    /**
     * Send both a chat message and an action bar message
     * Chat for Java players and action bar for Bedrock players
     * 
     * @param player The player to send the message to
     * @param chatMessage The message to display in chat
     * @param actionBarMessage The simplified message for action bar
     */
    public static void sendMessage(Player player, String chatMessage, String actionBarMessage) {
        // Always send to chat
        player.sendMessage(chatMessage);
        
        // Also send to action bar if Bedrock player
        if (isBedrockPlayer(player)) {
            sendActionBar(player, actionBarMessage);
        }
    }
}
