package com.server.profiles.skills.core;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.events.SkillLevelUpEvent;
import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * Listener for skill levelup events to award skill tokens
 */
public class SkillLevelupListener implements Listener {
    
    private final Main plugin;
    
    public SkillLevelupListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle skill level up events to award skill tokens
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        Player player = event.getPlayer();
        Skill skill = event.getSkill();
        int newLevel = event.getNewLevel();
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Award tokens based on the level achieved
        int tokensToAward = calculateTokensForLevel(newLevel);
        
        if (tokensToAward > 0) {
            // Add tokens to the player's skill tree data
            treeData.addTokens(skill.getId(), tokensToAward);
            
            // Get token display info
            SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
            
            // Notify the player
            player.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + tokensToAward + " " + 
                             tokenInfo.color + tokenInfo.displayName + " Token" + 
                             (tokensToAward > 1 ? "s" : "") + ChatColor.GREEN + "!");
            
            // Play sound for token reward
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,player.getName() + " received " + tokensToAward + " " + 
                                     tokenInfo.displayName + " Tokens for reaching level " + 
                                     newLevel + " in " + skill.getDisplayName());
            }
        }
    }
    
    /**
     * Calculate how many tokens to award for reaching a level
     * @param level The level reached
     * @return Number of tokens to award
     */
    private int calculateTokensForLevel(int level) {
        // Special milestones get more tokens
        if (level % 25 == 0) {
            return 5; // Every 25 levels: 25, 50, 75, 100
        } else if (level % 10 == 0) {
            return 3; // Every 10 levels: 10, 20, 30, etc. (except those already covered)
        } else if (level % 5 == 0) {
            return 2; // Every 5 levels: 5, 15, etc. (except those already covered)
        } else {
            return 1; // All other levels
        }
    }
}