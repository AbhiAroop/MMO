package com.server.profiles.skills.display;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.events.SkillExpGainEvent;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Manages temporary action bar displays for skill XP gains
 */
public class SkillActionBarManager {
    
    private static SkillActionBarManager instance;
    private final Main plugin;
    
    // Map of player UUIDs to their current skill action bar tasks
    private final Map<UUID, BukkitTask> skillActionBarTasks = new HashMap<>();
    
    // Map of player UUIDs to their last displayed skill
    private final Map<UUID, Skill> lastSkills = new HashMap<>();
    
    // Duration in ticks to show skill action bar
    private static final int DISPLAY_DURATION = 60; // 3 seconds
    
    private SkillActionBarManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the skill action bar manager
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new SkillActionBarManager(plugin);
        }
    }
    
    /**
     * Get the skill action bar manager instance
     */
    public static SkillActionBarManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkillActionBarManager not initialized");
        }
        return instance;
    }
    
    /**
     * Handle a skill XP gain event
     */
    public void handleSkillXpGain(SkillExpGainEvent event) {
        Player player = event.getPlayer();
        Skill skill = event.getSkill();
        double amount = event.getAmount();
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get current skill level
        SkillLevel level = profile.getSkillData().getSkillLevel(skill);
        
        // Get XP required for next level
        double xpForNextLevel = skill.getXpForLevel(level.getLevel() + 1);
        
        // Show action bar
        showSkillActionBar(player, skill, amount, level.getCurrentXp(), xpForNextLevel);
    }
    
    /**
     * Show skill XP gain in action bar
     */
    private void showSkillActionBar(Player player, Skill skill, double amount, double currentXp, double xpForNextLevel) {
        UUID playerUuid = player.getUniqueId();
        
        // Cancel existing task if there is one
        BukkitTask existingTask = skillActionBarTasks.remove(playerUuid);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Update last skill
        lastSkills.put(playerUuid, skill);
        
        // Create a new task
        BukkitTask task = new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Display the skill action bar for the specified duration
                if (ticks <= DISPLAY_DURATION) {
                    // Get fresh data from player profile to ensure we have the latest values
                    Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                    if (activeSlot == null) return;
                    
                    PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                    if (profile == null) return;
                    
                    // Get the most current skill level data
                    SkillLevel currentLevel = profile.getSkillData().getSkillLevel(skill);
                    double updatedCurrentXp = currentLevel.getCurrentXp();
                    double updatedRequiredXp = skill.getXpForLevel(currentLevel.getLevel() + 1);
                    
                    // Use the updated values for display
                    String actionBar = createSkillActionBar(skill, amount, updatedCurrentXp, updatedRequiredXp);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                                            TextComponent.fromLegacyText(actionBar));
                } else {
                    // Remove the task and mapping after the duration
                    this.cancel();
                    skillActionBarTasks.remove(playerUuid);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Store the task reference
        skillActionBarTasks.put(playerUuid, task);
    }
    
    /**
     * Create action bar text for skill XP gain
     */
    private String createSkillActionBar(Skill skill, double amount, double currentXp, double xpForNextLevel) {
        double progress = Math.min(1.0, currentXp / xpForNextLevel);
        
        return ChatColor.GREEN + "+" + String.format("%.1f", amount) + " " + skill.getDisplayName() + " XP " +
               ChatColor.GRAY + "[" + ChatColor.YELLOW + createProgressBar(progress, 10) + ChatColor.GRAY + "] " +
               ChatColor.WHITE + String.format("%.1f", currentXp) + "/" + String.format("%.1f", xpForNextLevel);
    }
    
    /**
     * Create a progress bar for the action bar
     */
    private String createProgressBar(double progress, int length) {
        StringBuilder bar = new StringBuilder();
        int filledBars = (int) Math.floor(progress * length);
        
        for (int i = 0; i < length; i++) {
            if (i < filledBars) {
                bar.append("■");
            } else {
                bar.append("□");
            }
        }
        
        return bar.toString();
    }

    /**
     * Check if a player currently has an active subskill display
     * @param player The player to check
     * @return True if the player has an active subskill display
     */
    public boolean hasActiveSubskillDisplay(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Check if the player has a task running
        if (!skillActionBarTasks.containsKey(playerUuid)) {
            return false;
        }
        
        // Check if the last skill was a subskill
        Skill lastSkill = lastSkills.get(playerUuid);
        return lastSkill != null && !lastSkill.isMainSkill();
    }
}