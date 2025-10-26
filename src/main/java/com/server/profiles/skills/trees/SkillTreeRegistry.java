package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.skills.farming.trees.FarmingTreeBuilder;
import com.server.profiles.skills.skills.mining.trees.MiningTreeBuilder;
import com.server.profiles.skills.trees.builders.GenericTreeBuilder;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Central registry for all skill trees
 */
public class SkillTreeRegistry {
    private static SkillTreeRegistry instance;
    
    private final Main plugin;
    private final Map<String, SkillTree> skillTrees;
    private final Map<String, SkillTreeBuilder> treeBuilders;
    
    private SkillTreeRegistry(Main plugin) {
        this.plugin = plugin;
        this.skillTrees = new HashMap<>();
        this.treeBuilders = new HashMap<>();
        
        // Register tree builders
        registerTreeBuilders();
        
        // Initialize skill trees
        initializeSkillTrees();
    }
    
    /**
     * Register all skill tree builders
     */
    private void registerTreeBuilders() {
        // Register mining tree builders
        treeBuilders.put(SkillType.MINING.getId(), new MiningTreeBuilder());
        
        // Register farming tree builder
        treeBuilders.put(SkillType.FARMING.getId(), new FarmingTreeBuilder());
        
        // Add more tree builders as they are implemented
        // For example:
        // treeBuilders.put(SkillType.EXCAVATING.getId(), new ExcavatingTreeBuilder());
    }
    
    /**
     * Initialize the skill tree registry
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new SkillTreeRegistry(plugin);
        }
    }
    
    /**
     * Get the skill tree registry instance
     */
    public static SkillTreeRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkillTreeRegistry not initialized");
        }
        return instance;
    }
    
    /**
     * Initialize all skill trees
     */
    private void initializeSkillTrees() {
        // Initialize trees for main skills only (subskills are part of the main tree)
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            // Only create trees for main skills, not subskills
            if (skill.isMainSkill()) {
                if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                    plugin.debugLog(DebugSystem.SKILLS,"Creating skill tree for main skill: " + skill.getId() + " (" + skill.getDisplayName() + ")");
                }
                
                createSkillTree(skill);
            }
        }
        
        // Log total number of skill trees created
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"Total skill trees created: " + skillTrees.size());
            for (String skillId : skillTrees.keySet()) {
                plugin.debugLog(DebugSystem.SKILLS,"  - Tree exists for: " + skillId);
            }
        }
    }

    /**
     * Create a skill tree for a skill
     */
    private void createSkillTree(Skill skill) {
        SkillTree tree = new SkillTree(skill);
        
        // Add the root node - always unlocked by default
        tree.addNode(new SkillTreeNode(
            "root",
            "Core " + skill.getDisplayName(),
            "The foundation of your " + skill.getDisplayName() + " knowledge.",
            getIconForSkill(skill),
            getChatColorForSkill(skill),
            0, 0, // Position at the center
            0 // Root node is free
        ));
        
        // Find and use the appropriate tree builder
        SkillTreeBuilder builder = treeBuilders.get(skill.getId());
        
        if (builder == null) {
            // Use generic builder if no specific one is found
            builder = new GenericTreeBuilder();
        }
        
        // Build the tree
        builder.buildSkillTree(tree);
        
        // Register the tree
        skillTrees.put(skill.getId(), tree);
    }
    
    /**
     * Get a skill tree by skill ID
     */
    public SkillTree getSkillTree(String skillId) {
        return skillTrees.get(skillId);
    }
    
    /**
     * Get a skill tree for a skill
     */
    public SkillTree getSkillTree(Skill skill) {
        return getSkillTree(skill.getId());
    }
    
    /**
     * Get an appropriate icon for a skill
     */
    private Material getIconForSkill(Skill skill) {
        // This method can stay in the registry since it's utility code
        switch (skill.getId()) {
            case "mining":
                return Material.DIAMOND_PICKAXE;
            case "excavating":
                return Material.DIAMOND_SHOVEL;
            case "fishing":
                return Material.FISHING_ROD;
            case "farming":
                return Material.WHEAT;
            case "combat":
                return Material.DIAMOND_SWORD;
            case "ore_extraction":
                return Material.IRON_ORE;
            case "gem_carving":
                return Material.DIAMOND;
            default:
                return Material.BOOK;
        }
    }
    
    /**
     * Get an appropriate chat color for a skill
     */
    private ChatColor getChatColorForSkill(Skill skill) {
        // This method can stay in the registry since it's utility code
        switch (skill.getId()) {
            case "mining":
                return ChatColor.AQUA;
            case "excavating":
                return ChatColor.GOLD;
            case "fishing":
                return ChatColor.BLUE;
            case "farming":
                return ChatColor.GREEN;
            case "combat":
                return ChatColor.RED;
            case "ore_extraction":
                return ChatColor.YELLOW;
            case "gem_carving":
                return ChatColor.LIGHT_PURPLE;
            default:
                return ChatColor.WHITE;
        }
    }
}