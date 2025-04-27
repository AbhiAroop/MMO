package com.server.profiles.skills.rewards.rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.rewards.SkillRewardType;

/**
 * A reward that unlocks a new feature or ability
 */
public class UnlockReward extends SkillReward {
    private final String unlockId;
    
    public UnlockReward(String unlockId, String description) {
        super(SkillRewardType.UNLOCK, "Unlock: " + description);
        this.unlockId = unlockId;
    }
    
    @Override
    public void grantTo(Player player) {
        // Get the player's active profile
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return;
        
        // Here we would unlock the feature/ability in your unlock system
        // This is just a placeholder for your actual unlock implementation
        
        // For example, you might store the unlocked features in your PlayerProfile class:
        // profile.addUnlockedFeature(unlockId);
        
        // Log the unlock for debugging
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Unlocked feature " + unlockId + " for player " + player.getName());
        }
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "Skill Reward: " + getDescription());
    }
}