package com.server.profiles.skills.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.data.PlayerSkillData;
import com.server.profiles.skills.skills.combat.CombatSkill;
import com.server.profiles.skills.skills.excavating.ExcavatingSkill;
import com.server.profiles.skills.skills.farming.FarmingSkill;
import com.server.profiles.skills.skills.fishing.FishingSkill;
import com.server.profiles.skills.skills.mining.MiningSkill;

/**
 * Central registry for all skills in the system
 */
public class SkillRegistry {
    private static SkillRegistry instance;
    
    private final Main plugin;
    private final Map<String, Skill> skills;
    private final Map<PlayerProfile, PlayerSkillData> playerSkillData;
    
    private SkillRegistry(Main plugin) {
        this.plugin = plugin;
        this.skills = new HashMap<>();
        this.playerSkillData = new HashMap<>();
        
        // Initialize all skills
        initializeSkills();
    }
    
    /**
     * Initialize the skills registry
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new SkillRegistry(plugin);
        }
    }
    
    /**
     * Get the skills registry instance
     */
    public static SkillRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkillRegistry not initialized");
        }
        return instance;
    }
    
    /**
     * Initialize all skills in the system
     */
    private void initializeSkills() {
        // Create main skills
        MiningSkill miningSkill = new MiningSkill();
        ExcavatingSkill excavatingSkill = new ExcavatingSkill();
        FishingSkill fishingSkill = new FishingSkill();
        FarmingSkill farmingSkill = new FarmingSkill();
        CombatSkill combatSkill = new CombatSkill();
        
        // Register main skills without casting
        registerSkill(miningSkill);
        registerSkill(excavatingSkill);
        registerSkill(fishingSkill);
        registerSkill(farmingSkill);
        registerSkill(combatSkill);
        
        // Initialize subskills (will be created by each main skill class)
    }
    
    /**
     * Register a skill in the registry
     */
    public void registerSkill(Skill skill) {
        if (skill != null && !skills.containsKey(skill.getId())) {
            skills.put(skill.getId(), skill);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Registered skill: " + skill.getDisplayName());
            }
        }
    }
    
    /**
     * Get a skill by its ID
     */
    public Skill getSkill(String id) {
        return skills.get(id);
    }
    
    /**
     * Get a skill by its type
     */
    public Skill getSkill(SkillType type) {
        return getSkill(type.getId());
    }
    
    /**
     * Get a subskill by its type
     */
    public Skill getSubskill(SubskillType type) {
        return getSkill(type.getId());
    }
    
    /**
     * Get a player's skill data
     */
    public PlayerSkillData getPlayerSkillData(PlayerProfile profile) {
        if (!playerSkillData.containsKey(profile)) {
            playerSkillData.put(profile, new PlayerSkillData());
        }
        return playerSkillData.get(profile);
    }
    
    /**
     * Get a player's skill data from the player object
     */
    public PlayerSkillData getPlayerSkillData(Player player) {
        Integer slot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (slot == null) return new PlayerSkillData(); // Return empty data
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[slot];
        if (profile == null) return new PlayerSkillData();
        
        return getPlayerSkillData(profile);
    }
    
    /**
     * Clear skill data for a profile
     */
    public void clearSkillData(PlayerProfile profile) {
        playerSkillData.remove(profile);
    }

    /**
     * Get all registered skills
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }
}