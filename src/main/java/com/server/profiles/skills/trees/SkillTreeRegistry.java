package com.server.profiles.skills.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.Main;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.core.SubskillType;

/**
 * Central registry for all skill trees
 */
public class SkillTreeRegistry {
    private static SkillTreeRegistry instance;
    
    private final Main plugin;
    private final Map<String, SkillTree> skillTrees;
    
    private SkillTreeRegistry(Main plugin) {
        this.plugin = plugin;
        this.skillTrees = new HashMap<>();
        
        // Initialize skill trees
        initializeSkillTrees();
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
        // Initialize trees for all skills
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            // For debugging purposes, log every skill tree creation attempt
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Creating skill tree for: " + skill.getId() + " (" + skill.getDisplayName() + ")");
            }
            
            createSkillTree(skill);
        }
        
        // Log total number of skill trees created
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Total skill trees created: " + skillTrees.size());
            // Log each tree that was created
            for (String skillId : skillTrees.keySet()) {
                plugin.getLogger().info("  - Tree exists for: " + skillId);
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
        
        // Create different trees based on skill type
        if (skill.getId().equals(SkillType.MINING.getId())) {
            setupMiningSkillTree(tree);
        } else if (skill.getId().equals(SkillType.EXCAVATING.getId())) {
            setupExcavatingSkillTree(tree);
        } else if (skill.getId().equals(SkillType.FISHING.getId())) {
            setupFishingSkillTree(tree);
        } else if (skill.getId().equals(SkillType.FARMING.getId())) {
            setupFarmingSkillTree(tree);
        } else if (skill.getId().equals(SkillType.COMBAT.getId())) {
            setupCombatSkillTree(tree);
        } else if (skill.getId().equals(SubskillType.ORE_EXTRACTION.getId())) {
            setupOreExtractionSkillTree(tree);
        } else if (skill.getId().equals(SubskillType.GEM_CARVING.getId())) {
            setupGemCarvingSkillTree(tree);
        } else {
            // Generic skill tree for other skills
            setupGenericSkillTree(tree);
        }
        
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
     * Setup the Mining skill tree
     */
    private void setupMiningSkillTree(SkillTree tree) {
        // Three initial branches from root
        
        // Root node is already at (0,0)
        
        // Left branch (Mining Speed)
        tree.addNode(new SkillTreeNode(
            "mining_speed_1",
            "Mining Speed I",
            "Mine blocks 10% faster.",
            Material.IRON_PICKAXE,
            ChatColor.AQUA,
            -1, 0, // Position is now closer to root
            1
        ));
        tree.addConnection("root", "mining_speed_1");
        
        tree.addNode(new SkillTreeNode(
            "mining_speed_2",
            "Mining Speed II",
            "Mine blocks 20% faster.",
            Material.GOLDEN_PICKAXE,
            ChatColor.AQUA,
            -2, 0, // Position is now closer to previous node
            2
        ));
        tree.addConnection("mining_speed_1", "mining_speed_2");
        
        // Right branch (Miners Luck)
        tree.addNode(new SkillTreeNode(
            "miners_luck",
            "Miner's Luck",
            "Chance to find rare materials while mining.",
            Material.DIAMOND,
            ChatColor.BLUE,
            1, 0, // Position is now closer to root
            2
        ));
        tree.addConnection("root", "miners_luck");
        
        tree.addNode(new SkillTreeNode(
            "ancient_knowledge",
            "Ancient Knowledge",
            "Reveals the exact locations of nearby ores when crouching.",
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE,
            2, 0, // Position is now closer to miners_luck
            3
        ));
        tree.addConnection("miners_luck", "ancient_knowledge");
        
        // Bottom branch (Stamina)
        tree.addNode(new SkillTreeNode(
            "stamina_1",
            "Miner's Endurance I",
            "Mining consumes 10% less hunger.",
            Material.BREAD,
            ChatColor.RED,
            0, 1, // Below root, position unchanged
            1
        ));
        tree.addConnection("root", "stamina_1");
        
        tree.addNode(new SkillTreeNode(
            "stamina_2",
            "Miner's Endurance II",
            "Mining consumes 20% less hunger.",
            Material.COOKED_BEEF,
            ChatColor.RED,
            0, 2, // Below stamina_1, position unchanged
            2
        ));
        tree.addConnection("stamina_1", "stamina_2");
    }
    
    /**
     * Setup the Excavating skill tree
     */
    private void setupExcavatingSkillTree(SkillTree tree) {
        // Similar to mining but with excavating-specific nodes
        // ... (implementation similar to mining tree)
    }
    
    /**
     * Setup the Fishing skill tree
     */
    private void setupFishingSkillTree(SkillTree tree) {
        // ... (implementation similar to mining tree)
    }
    
    /**
     * Setup the Farming skill tree
     */
    private void setupFarmingSkillTree(SkillTree tree) {
        // ... (implementation similar to mining tree)
    }
    
    /**
     * Setup the Combat skill tree
     */
    private void setupCombatSkillTree(SkillTree tree) {
        // ... (implementation similar to mining tree)
    }
    
    /**
     * Setup the Ore Extraction skill tree
     */
    private void setupOreExtractionSkillTree(SkillTree tree) {
        // Add basic mining efficiency node
        tree.addNode(new SkillTreeNode(
            "mining_speed",
            "Mining Speed",
            "Increases mining speed by 10%",
            Material.IRON_PICKAXE,
            ChatColor.YELLOW,
            1, 0, // Position to the right of root
            2 // Token cost
        ));
        
        // Add connection from root to mining speed
        tree.addConnection("root", "mining_speed");
        
        // Add bonus drops node
        tree.addNode(new SkillTreeNode(
            "bonus_drops",
            "Bonus Drops",
            "Increases chance of bonus drops by 5%",
            Material.CHEST,
            ChatColor.GREEN,
            2, 0, // Position to the right of mining speed
            3 // Token cost
        ));
        
        // Add connection from mining speed to bonus drops
        tree.addConnection("mining_speed", "bonus_drops");
        
        // Add cave-in protection node
        tree.addNode(new SkillTreeNode(
            "cave_in_protection",
            "Cave-in Protection",
            "Reduces chance of cave-ins by 15%",
            Material.SHIELD,
            ChatColor.BLUE,
            1, 1, // Position below mining speed
            2 // Token cost
        ));
        
        // Add connection from mining speed to cave-in protection
        tree.addConnection("mining_speed", "cave_in_protection");
        
        // Add ore sense node
        tree.addNode(new SkillTreeNode(
            "ore_sense",
            "Ore Sense",
            "Nearby ores occasionally glow",
            Material.SPYGLASS,
            ChatColor.AQUA,
            0, -1, // Position above and to the left of root
            4 // Token cost
        ));
        
        // Add connection from root to ore sense
        tree.addConnection("root", "ore_sense");
        
        // Add advanced efficiency node
        tree.addNode(new SkillTreeNode(
            "advanced_efficiency",
            "Advanced Efficiency",
            "Further increases mining speed by 15%",
            Material.DIAMOND_PICKAXE,
            ChatColor.LIGHT_PURPLE,
            3, 0, // Position to the right of bonus drops
            5 // Token cost
        ));
        
        // Add connection from bonus drops to advanced efficiency
        tree.addConnection("bonus_drops", "advanced_efficiency");
    }

    /**
     * Setup the Gem Carving skill tree
     */
    private void setupGemCarvingSkillTree(SkillTree tree) {
        // Add gem finding chance node
        tree.addNode(new SkillTreeNode(
            "gem_finding",
            "Gem Finder",
            "Increases chance of finding gems by 5%",
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE,
            1, 0, // Position to the right of root
            2 // Token cost
        ));
        
        // Add connection from root to gem finding
        tree.addConnection("root", "gem_finding");
        
        // Add extraction accuracy node
        tree.addNode(new SkillTreeNode(
            "extraction_accuracy",
            "Extraction Accuracy",
            "Increases chance of successful gem extraction by 10%",
            Material.SHEARS,
            ChatColor.YELLOW,
            2, 0, // Position to the right of gem finding
            3 // Token cost
        ));
        
        // Add connection from gem finding to extraction accuracy
        tree.addConnection("gem_finding", "extraction_accuracy");
        
        // Add gem quality node
        tree.addNode(new SkillTreeNode(
            "gem_quality",
            "Gem Quality",
            "Increases quality of found gems",
            Material.DIAMOND,
            ChatColor.AQUA,
            1, 1, // Position below gem finding
            3 // Token cost
        ));
        
        // Add connection from gem finding to gem quality
        tree.addConnection("gem_finding", "gem_quality");
        
        // Add rare gem chance node
        tree.addNode(new SkillTreeNode(
            "rare_gem_chance",
            "Rare Gem Finder",
            "Chance to find rare and valuable gems",
            Material.EMERALD,
            ChatColor.GREEN,
            0, -1, // Position above and to the left of root
            4 // Token cost
        ));
        
        // Add connection from root to rare gem chance
        tree.addConnection("root", "rare_gem_chance");
        
        // Add master carver node
        tree.addNode(new SkillTreeNode(
            "master_carver",
            "Master Carver",
            "Significantly increases all gem carving abilities",
            Material.NETHERITE_PICKAXE,
            ChatColor.GOLD,
            3, 0, // Position to the right of extraction accuracy
            5 // Token cost
        ));
        
        // Add connection from extraction accuracy to master carver
        tree.addConnection("extraction_accuracy", "master_carver");
    }
        
    /**
     * Setup a generic skill tree for skills without specific implementations
     */
    private void setupGenericSkillTree(SkillTree tree) {
        // Simple tree with one branch in each direction
        
        // North branch
        tree.addNode(new SkillTreeNode(
            "north_1",
            "Skill Mastery I",
            "First step in mastering this skill.",
            Material.BOOK,
            ChatColor.YELLOW,
            0, -1, // Above root
            1
        ));
        tree.addConnection("root", "north_1");
        
        tree.addNode(new SkillTreeNode(
            "north_2",
            "Skill Mastery II",
            "Further develop your skill mastery.",
            Material.ENCHANTED_BOOK,
            ChatColor.YELLOW,
            0, -2, // Above north_1
            2
        ));
        tree.addConnection("north_1", "north_2");
        
        // East branch
        tree.addNode(new SkillTreeNode(
            "east_1",
            "Skill Efficiency I",
            "Improve your efficiency with this skill.",
            Material.CLOCK,
            ChatColor.GOLD,
            1, 0, // Right of root
            1
        ));
        tree.addConnection("root", "east_1");
        
        tree.addNode(new SkillTreeNode(
            "east_2",
            "Skill Efficiency II",
            "Further improve your efficiency.",
            Material.COMPASS,
            ChatColor.GOLD,
            2, 0, // Right of east_1
            2
        ));
        tree.addConnection("east_1", "east_2");
        
        // South branch
        tree.addNode(new SkillTreeNode(
            "south_1",
            "Skill Fortune I",
            "Increase your fortune with this skill.",
            Material.GOLD_INGOT,
            ChatColor.RED,
            0, 1, // Below root
            1
        ));
        tree.addConnection("root", "south_1");
        
        tree.addNode(new SkillTreeNode(
            "south_2",
            "Skill Fortune II",
            "Further increase your fortune.",
            Material.GOLD_BLOCK,
            ChatColor.RED,
            0, 2, // Below south_1
            2
        ));
        tree.addConnection("south_1", "south_2");
        
        // West branch
        tree.addNode(new SkillTreeNode(
            "west_1",
            "Skill Precision I",
            "Improve your precision with this skill.",
            Material.TARGET,
            ChatColor.AQUA,
            -1, 0, // Left of root
            1
        ));
        tree.addConnection("root", "west_1");
        
        tree.addNode(new SkillTreeNode(
            "west_2",
            "Skill Precision II",
            "Further improve your precision.",
            Material.SPECTRAL_ARROW,
            ChatColor.AQUA,
            -2, 0, // Left of west_1
            2
        ));
        tree.addConnection("west_1", "west_2");
    }
    
    /**
     * Get an appropriate icon for a skill
     */
    private Material getIconForSkill(Skill skill) {
        // Try to find an appropriate icon based on skill ID
        switch (skill.getId()) {
            case "mining": return Material.DIAMOND_PICKAXE;
            case "excavating": return Material.IRON_SHOVEL;
            case "fishing": return Material.FISHING_ROD;
            case "farming": return Material.WHEAT;
            case "combat": return Material.IRON_SWORD;
            case "ore_extraction": return Material.RAW_IRON;
            case "gem_carving": return Material.EMERALD;
            case "treasure_hunter": return Material.CHEST;
            case "soil_master": return Material.DIRT;
            case "archaeologist": return Material.BONE;
            case "fisherman": return Material.COD;
            case "aquatic_treasures": return Material.PRISMARINE_CRYSTALS;
            case "master_angler": return Material.TROPICAL_FISH;
            case "crop_growth": return Material.WHEAT_SEEDS;
            case "animal_breeder": return Material.EGG;
            case "harvester": return Material.HAY_BLOCK;
            case "swordsmanship": return Material.DIAMOND_SWORD;
            case "archery": return Material.BOW;
            case "defense": return Material.SHIELD;
            default: return Material.BOOK;
        }
    }
    
    /**
     * Get an appropriate chat color for a skill
     */
    private ChatColor getChatColorForSkill(Skill skill) {
        // Try to find an appropriate color based on skill ID
        switch (skill.getId()) {
            case "mining": return ChatColor.AQUA;
            case "excavating": return ChatColor.GOLD;
            case "fishing": return ChatColor.BLUE;
            case "farming": return ChatColor.GREEN;
            case "combat": return ChatColor.RED;
            case "ore_extraction": return ChatColor.YELLOW;
            case "gem_carving": return ChatColor.GREEN;
            default: return ChatColor.WHITE;
        }
    }
}