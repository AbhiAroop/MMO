package com.server.profiles.skills.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.skills.abilities.active.ActiveAbility;
import com.server.profiles.skills.abilities.active.mining.MiningSpeedBoostAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.abilities.passive.mining.OreConduitAbility;
import com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility;
import com.server.profiles.skills.core.SubskillType;

/**
 * Registry for all skill abilities
 */
public class AbilityRegistry {
    private static AbilityRegistry instance;
    
    private final Main plugin;
    
    // Maps to store abilities
    private final Map<String, SkillAbility> allAbilities;
    private final Map<String, List<PassiveAbility>> passiveAbilitiesBySkill;
    private final Map<String, List<ActiveAbility>> activeAbilitiesBySkill;
    
    private AbilityRegistry(Main plugin) {
        this.plugin = plugin;
        this.allAbilities = new HashMap<>();
        this.passiveAbilitiesBySkill = new HashMap<>();
        this.activeAbilitiesBySkill = new HashMap<>();
        
        // Initialize abilities
        initializeAbilities();
    }
    
    /**
     * Initialize the ability registry
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new AbilityRegistry(plugin);
        }
    }
    
    /**
     * Get the ability registry instance
     */
    public static AbilityRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AbilityRegistry not initialized");
        }
        return instance;
    }
    
    /**
     * Initialize all abilities
     */
    private void initializeAbilities() {
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"Initializing abilities...");
        }
        
        // Register mining abilities first
        registerMiningAbilities();
        
        // Log all registered abilities for debugging
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            logRegisteredAbilities();
        }
    }

    /**
     * Register mining abilities
     */
    private void registerMiningAbilities() {
        // Register passive abilities - assign to proper subskill
        PassiveAbility veinMiner = new VeinMinerAbility();
        registerAbility(veinMiner);
        
        // Map the subskill ability to the parent skill too
        // This ensures it shows up when browsing the Mining skill abilities
        mapSubskillAbilityToParent(veinMiner);
        
        // Register the Ore Conduit ability
        PassiveAbility oreConduit = new OreConduitAbility();
        registerAbility(oreConduit);
        
        // CRITICAL: Map Ore Conduit to parent skill as well
        mapSubskillAbilityToParent(oreConduit);
        
        // Register active abilities
        ActiveAbility miningSpeedBoost = new MiningSpeedBoostAbility();
        registerAbility(miningSpeedBoost);
        
        // Log registration for debugging
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"Registered mining abilities:");
            plugin.debugLog(DebugSystem.SKILLS,"  - Passive: " + veinMiner.getDisplayName() + " (" + veinMiner.getId() + ") for skill: " + veinMiner.getSkillId());
            plugin.debugLog(DebugSystem.SKILLS,"  - Passive: " + oreConduit.getDisplayName() + " (" + oreConduit.getId() + ") for skill: " + oreConduit.getSkillId());
            plugin.debugLog(DebugSystem.SKILLS,"  - Active: " + miningSpeedBoost.getDisplayName() + " (" + miningSpeedBoost.getId() + ") for skill: " + miningSpeedBoost.getSkillId());
        }
    }

    /**
     * Map a subskill ability to its parent skill 
     * This is necessary to show subskill abilities when browsing the parent skill
     */
    private void mapSubskillAbilityToParent(SkillAbility ability) {
        String subskillId = ability.getSkillId();
        
        // Find the parent skill ID
        String parentSkillId = null;
        for (SubskillType subskillType : SubskillType.values()) {
            if (subskillType.getId().equals(subskillId)) {
                parentSkillId = subskillType.getParentSkill().getId();
                break;
            }
        }
        
        if (parentSkillId != null) {
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Mapping " + ability.getId() + " from " + subskillId + 
                                    " to parent skill " + parentSkillId);
            }
            
            // Add the ability to the parent skill's list too
            if (ability instanceof PassiveAbility) {
                passiveAbilitiesBySkill
                    .computeIfAbsent(parentSkillId, k -> new ArrayList<>())
                    .add((PassiveAbility) ability);
            } else if (ability instanceof ActiveAbility) {
                activeAbilitiesBySkill
                    .computeIfAbsent(parentSkillId, k -> new ArrayList<>())
                    .add((ActiveAbility) ability);
            }
        }
    }
    
    /**
     * Register an ability
     */
    public void registerAbility(SkillAbility ability) {
        allAbilities.put(ability.getId(), ability);
        
        // Also add to type-specific map
        if (ability instanceof PassiveAbility) {
            passiveAbilitiesBySkill
                .computeIfAbsent(ability.getSkillId(), k -> new ArrayList<>())
                .add((PassiveAbility) ability);
            
            // Log for debugging
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Registered passive ability " + ability.getDisplayName() + 
                                        " (" + ability.getId() + ") to skill " + ability.getSkillId());
            }
        } else if (ability instanceof ActiveAbility) {
            activeAbilitiesBySkill
                .computeIfAbsent(ability.getSkillId(), k -> new ArrayList<>())
                .add((ActiveAbility) ability);
            
            // Log for debugging
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,"Registered active ability " + ability.getDisplayName() + 
                                        " (" + ability.getId() + ") to skill " + ability.getSkillId());
            }
        }
    }
    
    /**
     * Log all registered abilities for debugging
     */
    private void logRegisteredAbilities() {
        plugin.debugLog(DebugSystem.SKILLS,"=== Registered Abilities ===");
        plugin.debugLog(DebugSystem.SKILLS,"Total abilities: " + allAbilities.size());
        
        // Log passive abilities by skill
        plugin.debugLog(DebugSystem.SKILLS,"Passive abilities by skill:");
        for (Map.Entry<String, List<PassiveAbility>> entry : passiveAbilitiesBySkill.entrySet()) {
            String skillId = entry.getKey();
            List<PassiveAbility> abilities = entry.getValue();
            plugin.debugLog(DebugSystem.SKILLS,"  Skill " + skillId + ": " + abilities.size() + " abilities");
            for (PassiveAbility ability : abilities) {
                plugin.debugLog(DebugSystem.SKILLS,"    - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            }
        }
        
        // Log active abilities by skill
        plugin.debugLog(DebugSystem.SKILLS,"Active abilities by skill:");
        for (Map.Entry<String, List<ActiveAbility>> entry : activeAbilitiesBySkill.entrySet()) {
            String skillId = entry.getKey();
            List<ActiveAbility> abilities = entry.getValue();
            plugin.debugLog(DebugSystem.SKILLS,"  Skill " + skillId + ": " + abilities.size() + " abilities");
            for (ActiveAbility ability : abilities) {
                plugin.debugLog(DebugSystem.SKILLS,"    - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            }
        }
    }
    
    /**
     * Get an ability by ID
     */
    public SkillAbility getAbility(String id) {
        return allAbilities.get(id);
    }
    
    /**
     * Get all passive abilities for a skill
     */
    public List<PassiveAbility> getPassiveAbilities(String skillId) {
        return passiveAbilitiesBySkill.getOrDefault(skillId, new ArrayList<>());
    }
    
    /**
     * Get all active abilities for a skill
     */
    public List<ActiveAbility> getActiveAbilities(String skillId) {
        return activeAbilitiesBySkill.getOrDefault(skillId, new ArrayList<>());
    }
    
    /**
     * Get all unlocked passive abilities for a player and skill
     */
    public List<PassiveAbility> getUnlockedPassiveAbilities(Player player, String skillId) {
        List<PassiveAbility> result = new ArrayList<>();
        List<PassiveAbility> abilities = getPassiveAbilities(skillId);
        
        for (PassiveAbility ability : abilities) {
            if (ability.isUnlocked(player)) {
                result.add(ability);
            }
        }
        
        return result;
    }
    
    /**
     * Get all unlocked active abilities for a player and skill
     */
    public List<ActiveAbility> getUnlockedActiveAbilities(Player player, String skillId) {
        List<ActiveAbility> result = new ArrayList<>();
        List<ActiveAbility> abilities = getActiveAbilities(skillId);
        
        for (ActiveAbility ability : abilities) {
            if (ability.isUnlocked(player)) {
                result.add(ability);
            }
        }
        
        return result;
    }
}