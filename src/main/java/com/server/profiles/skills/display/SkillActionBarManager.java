package com.server.profiles.skills.display;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.passive.mining.OreConduitAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SubskillType;
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
                if (ticks++ < DISPLAY_DURATION) {
                    // Check if player is still online
                    if (!player.isOnline()) {
                        this.cancel();
                        skillActionBarTasks.remove(playerUuid);
                        return;
                    }
                    
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
        // Get the progress percentage
        double progress = (xpForNextLevel > 0) ? Math.min(1.0, currentXp / xpForNextLevel) : 1.0;
        
        // Create progress bar (10 characters wide)
        String progressBar = createProgressBar(progress, 10);
        
        // Format the skill name and XP amount
        String skillName = skill.getDisplayName();
        String formattedAmount = String.format("+%.1f", amount);
        
        // Get skill color and symbol based on skill type
        ChatColor skillColor = getSkillColor(skill);
        String skillSymbol = getSkillSymbol(skill);
        
        // Check if this is a split XP gain (for OreExtraction)
        if (skill.getId().equals(SubskillType.ORE_EXTRACTION.getId())) {
            // Try to get the player from the cache
            for (Map.Entry<UUID, Skill> entry : lastSkills.entrySet()) {
                UUID playerUUID = entry.getKey();
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && entry.getValue() == skill) {
                    // Check if the player has OreConduit active
                    AbilityRegistry registry = AbilityRegistry.getInstance();
                    OreConduitAbility oreConduit = (OreConduitAbility) registry.getAbility("ore_conduit");
                    
                    if (oreConduit != null && 
                        oreConduit.isUnlocked(player) && 
                        oreConduit.isEnabled(player)) {
                        
                        // Get the actual split percentage from the ability
                        double splitPercentage = oreConduit.getSplitPercentage(player);
                        
                        // Calculate the original XP amount before the split
                        // Since the current 'amount' is the already-split amount for OreExtraction
                        // We need to calculate the original amount
                        double originalAmount = amount / (1.0 - splitPercentage);
                        
                        // Now calculate how much went to Mining skill
                        double mainSkillAmount = originalAmount * splitPercentage;
                        
                        // Return action bar with split info
                        return skillColor + skillSymbol + " " + skillName + " " + ChatColor.GREEN + formattedAmount + "XP " +
                            ChatColor.GRAY + "[" + progressBar + "] " + 
                            ChatColor.YELLOW + Math.round(currentXp) + "/" + Math.round(xpForNextLevel) + " " +
                            ChatColor.GOLD + "⟿ " + ChatColor.YELLOW + "Mining " + ChatColor.GREEN + String.format("+%.1f", mainSkillAmount) + "XP";
                    }
                    break;
                }
            }
        }
        
        // Default action bar (no split)
        return skillColor + skillSymbol + " " + skillName + " " + ChatColor.GREEN + formattedAmount + "XP " +
            ChatColor.GRAY + "[" + progressBar + "] " + 
            ChatColor.YELLOW + Math.round(currentXp) + "/" + Math.round(xpForNextLevel);
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
     * Get a skill symbol based on skill type
     */
    private String getSkillSymbol(Skill skill) {
        String skillId = skill.getId().toLowerCase();
        
        // Main skills
        if (skillId.equals("mining")) return "⛏";
        if (skillId.equals("excavating")) return "⚒";
        if (skillId.equals("fishing")) return "🎣";
        if (skillId.equals("farming")) return "🌾";
        if (skillId.equals("combat")) return "⚔";
        
        // Mining subskills
        if (skillId.equals("ore_extraction")) return "⛏";
        if (skillId.equals("gem_carving")) return "💎";
        
        // Excavating subskills
        if (skillId.equals("treasure_hunter")) return "🔍";
        if (skillId.equals("soil_master")) return "⚒";
        
        // Fishing subskills
        if (skillId.equals("fisherman")) return "🎣";
        if (skillId.equals("aquatic_treasures")) return "⚓";
        
        // Farming subskills
        if (skillId.equals("crop_growth")) return "🌱";
        if (skillId.equals("animal_breeder")) return "🐄";
        
        // Combat subskills
        if (skillId.equals("swordsmanship")) return "⚔";
        if (skillId.equals("archery")) return "🏹";
        if (skillId.equals("defense")) return "🛡";
        
        // Default symbol for unknown skills
        return "✦";
    }

    /**
     * Get a skill color based on skill type
     */
    private ChatColor getSkillColor(Skill skill) {
        if (skill.isMainSkill()) {
            String skillId = skill.getId().toLowerCase();
            
            if (skillId.equals("mining")) return ChatColor.AQUA;
            if (skillId.equals("excavating")) return ChatColor.GOLD;
            if (skillId.equals("fishing")) return ChatColor.BLUE;
            if (skillId.equals("farming")) return ChatColor.GREEN;
            if (skillId.equals("combat")) return ChatColor.RED;
            
            return ChatColor.YELLOW; // Default for main skills
        } else {
            return ChatColor.LIGHT_PURPLE; // For subskills
        }
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