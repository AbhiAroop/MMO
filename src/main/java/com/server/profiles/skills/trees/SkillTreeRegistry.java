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
        // Root node (automatically added by skill tree creation)
        // This node is automatically unlocked and can have more than 3 connections
        
        // =====================================================================
        // BRANCH 1: ORE PROGRESSION - Main path for ore unlocks
        // =====================================================================
        
        // Basic Mining - First node from root
        tree.addNode(new SkillTreeNode(
            "basic_mining",
            "Basic Mining",
            "Unlocks the ability to mine Coal Ore",
            Material.COAL_ORE,
            ChatColor.GRAY,
            2, 0, // Position right of root
            1 // Token cost
        ));
        tree.addConnection("root", "basic_mining");
        
        // Coal refining - Bridge node
        tree.addNode(new SkillTreeNode(
            "coal_refining",
            "Coal Refining",
            "Learn to extract coal more efficiently",
            Material.COAL,
            ChatColor.GRAY,
            3, 1, // Diagonal down-right
            0 // Free connector
        ));
        tree.addConnection("basic_mining", "coal_refining");
        
        // Iron unlock
        tree.addNode(new SkillTreeNode(
            "iron_mining",
            "Iron Mining",
            "Unlocks the ability to mine Iron Ore",
            Material.IRON_ORE,
            ChatColor.WHITE,
            4, 1,
            2
        ));
        tree.addConnection("coal_refining", "iron_mining");
        
        // Iron working - Bridge node
        tree.addNode(new SkillTreeNode(
            "iron_working",
            "Iron Working",
            "Learn techniques for processing iron ore",
            Material.IRON_INGOT,
            ChatColor.WHITE,
            5, 2, // Diagonal down-right
            0 // Free connector
        ));
        tree.addConnection("iron_mining", "iron_working");
        
        // Copper unlock
        tree.addNode(new SkillTreeNode(
            "copper_mining",
            "Copper Mining",
            "Unlocks the ability to mine Copper Ore",
            Material.COPPER_ORE,
            ChatColor.GOLD,
            6, 2,
            2
        ));
        tree.addConnection("iron_working", "copper_mining");
        
        // Copper alloys - Bridge node
        tree.addNode(new SkillTreeNode(
            "copper_alloys",
            "Copper Alloys",
            "Learn to combine copper with other metals",
            Material.COPPER_INGOT,
            ChatColor.GOLD,
            7, 3, // Diagonal down-right
            1
        ));
        tree.addConnection("copper_mining", "copper_alloys");
        
        // Gold unlock
        tree.addNode(new SkillTreeNode(
            "gold_mining",
            "Gold Mining",
            "Unlocks the ability to mine Gold Ore",
            Material.GOLD_ORE,
            ChatColor.YELLOW,
            8, 3,
            3
        ));
        tree.addConnection("copper_alloys", "gold_mining");
        
        // Precious metals - Bridge node
        tree.addNode(new SkillTreeNode(
            "precious_metals",
            "Precious Metals",
            "Study the properties of valuable metals",
            Material.GOLD_INGOT,
            ChatColor.YELLOW,
            9, 4, // Diagonal down-right
            1
        ));
        tree.addConnection("gold_mining", "precious_metals");
        
        // Redstone unlock
        tree.addNode(new SkillTreeNode(
            "redstone_mining",
            "Redstone Mining",
            "Unlocks the ability to mine Redstone Ore",
            Material.REDSTONE_ORE,
            ChatColor.RED,
            10, 4,
            3
        ));
        tree.addConnection("precious_metals", "redstone_mining");
        
        // Circuit theory - Bridge node
        tree.addNode(new SkillTreeNode(
            "circuit_theory",
            "Circuit Theory",
            "Study the conductive properties of redstone",
            Material.REDSTONE,
            ChatColor.RED,
            11, 5, // Diagonal down-right
            1
        ));
        tree.addConnection("redstone_mining", "circuit_theory");
        
        // Lapis unlock
        tree.addNode(new SkillTreeNode(
            "lapis_mining",
            "Lapis Mining",
            "Unlocks the ability to mine Lapis Ore",
            Material.LAPIS_ORE,
            ChatColor.BLUE,
            12, 5,
            4
        ));
        tree.addConnection("circuit_theory", "lapis_mining");
        
        // Crystal formations - Bridge node
        tree.addNode(new SkillTreeNode(
            "crystal_formations",
            "Crystal Formations",
            "Study the formation of crystalline structures",
            Material.LAPIS_LAZULI,
            ChatColor.BLUE,
            13, 6, // Diagonal down-right
            2
        ));
        tree.addConnection("lapis_mining", "crystal_formations");
        
        // Diamond unlock
        tree.addNode(new SkillTreeNode(
            "diamond_mining",
            "Diamond Mining",
            "Unlocks the ability to mine Diamond Ore",
            Material.DIAMOND_ORE,
            ChatColor.AQUA,
            14, 6,
            5
        ));
        tree.addConnection("crystal_formations", "diamond_mining");
        
        // Gem cutting - Bridge node
        tree.addNode(new SkillTreeNode(
            "gem_cutting",
            "Gem Cutting",
            "Learn techniques for cutting perfect gemstones",
            Material.DIAMOND,
            ChatColor.AQUA,
            15, 7, // Diagonal down-right
            2
        ));
        tree.addConnection("diamond_mining", "gem_cutting");
        
        // Emerald unlock
        tree.addNode(new SkillTreeNode(
            "emerald_mining",
            "Emerald Mining",
            "Unlocks the ability to mine Emerald Ore",
            Material.EMERALD_ORE,
            ChatColor.GREEN,
            16, 7,
            6
        ));
        tree.addConnection("gem_cutting", "emerald_mining");
        
        // Ancient materials - Bridge node
        tree.addNode(new SkillTreeNode(
            "ancient_materials",
            "Ancient Materials",
            "Study materials from the dawn of time",
            Material.NETHERRACK,
            ChatColor.DARK_RED,
            16, 8, // Directly below emerald
            3
        ));
        tree.addConnection("emerald_mining", "ancient_materials");
        
        // Ancient Debris unlock
        tree.addNode(new SkillTreeNode(
            "ancient_debris_mining",
            "Ancient Debris Mining",
            "Unlocks the ability to mine Ancient Debris",
            Material.ANCIENT_DEBRIS,
            ChatColor.DARK_PURPLE,
            15, 9, // Diagonal down-left
            8
        ));
        tree.addConnection("ancient_materials", "ancient_debris_mining");
        
        // =====================================================================
        // BRANCH 2: DEEPSLATE PROGRESSION - Parallel path for deepslate variants
        // =====================================================================
        
        // Deepslate discovery - Start of deepslate branch
        tree.addNode(new SkillTreeNode(
            "deepslate_discovery",
            "Deepslate Discovery",
            "Learn about the properties of deep underground stone",
            Material.DEEPSLATE,
            ChatColor.DARK_GRAY,
            1, 1, // Diagonal down-left from root
            1
        ));
        tree.addConnection("root", "deepslate_discovery");
        
        // Deepslate coal
        tree.addNode(new SkillTreeNode(
            "deepslate_coal_mining",
            "Deepslate Coal Mining",
            "Unlocks the ability to mine Deepslate Coal Ore",
            Material.DEEPSLATE_COAL_ORE,
            ChatColor.GRAY,
            0, 2, // Diagonal down-left
            2
        ));
        tree.addConnection("deepslate_discovery", "deepslate_coal_mining");
        tree.addConnection("basic_mining", "deepslate_coal_mining"); // Cross-connection
        
        // Deep pressure
        tree.addNode(new SkillTreeNode(
            "deep_pressure",
            "Deep Pressure",
            "Learn to work under the pressure of the deep underground",
            Material.ANVIL,
            ChatColor.DARK_GRAY,
            -1, 3, // Diagonal down-left
            1
        ));
        tree.addConnection("deepslate_coal_mining", "deep_pressure");
        
        // Deepslate iron
        tree.addNode(new SkillTreeNode(
            "deepslate_iron_mining",
            "Deepslate Iron Mining",
            "Unlocks the ability to mine Deepslate Iron Ore",
            Material.DEEPSLATE_IRON_ORE,
            ChatColor.WHITE,
            -2, 4, // Diagonal down-left
            3
        ));
        tree.addConnection("deep_pressure", "deepslate_iron_mining");
        tree.addConnection("iron_mining", "deepslate_iron_mining"); // Cross-connection
        
        // Compression techniques
        tree.addNode(new SkillTreeNode(
            "compression_techniques",
            "Compression Techniques",
            "Study how materials behave under extreme pressure",
            Material.PISTON,
            ChatColor.GRAY,
            -3, 5, // Diagonal down-left
            1
        ));
        tree.addConnection("deepslate_iron_mining", "compression_techniques");
        
        // Deepslate copper
        tree.addNode(new SkillTreeNode(
            "deepslate_copper_mining",
            "Deepslate Copper Mining",
            "Unlocks the ability to mine Deepslate Copper Ore",
            Material.DEEPSLATE_COPPER_ORE,
            ChatColor.GOLD,
            -4, 6, // Diagonal down-left
            3
        ));
        tree.addConnection("compression_techniques", "deepslate_copper_mining");
        tree.addConnection("copper_mining", "deepslate_copper_mining"); // Cross-connection
        
        // Thermal dynamics
        tree.addNode(new SkillTreeNode(
            "thermal_dynamics",
            "Thermal Dynamics",
            "Learn how heat affects materials under pressure",
            Material.FURNACE,
            ChatColor.RED,
            -5, 7, // Diagonal down-left
            1
        ));
        tree.addConnection("deepslate_copper_mining", "thermal_dynamics");
        
        // Deepslate gold
        tree.addNode(new SkillTreeNode(
            "deepslate_gold_mining",
            "Deepslate Gold Mining",
            "Unlocks the ability to mine Deepslate Gold Ore",
            Material.DEEPSLATE_GOLD_ORE,
            ChatColor.YELLOW,
            -6, 8, // Diagonal down-left
            4
        ));
        tree.addConnection("thermal_dynamics", "deepslate_gold_mining");
        tree.addConnection("gold_mining", "deepslate_gold_mining"); // Cross-connection
        
        // Conductive properties
        tree.addNode(new SkillTreeNode(
            "conductive_properties",
            "Conductive Properties",
            "Study the conductive properties of deep ores",
            Material.LIGHTNING_ROD,
            ChatColor.YELLOW,
            -5, 9, // Diagonal down-right
            2
        ));
        tree.addConnection("deepslate_gold_mining", "conductive_properties");
        
        // Deepslate redstone
        tree.addNode(new SkillTreeNode(
            "deepslate_redstone_mining",
            "Deepslate Redstone Mining",
            "Unlocks the ability to mine Deepslate Redstone Ore",
            Material.DEEPSLATE_REDSTONE_ORE,
            ChatColor.RED,
            -4, 10, // Diagonal down-right
            4
        ));
        tree.addConnection("conductive_properties", "deepslate_redstone_mining");
        tree.addConnection("redstone_mining", "deepslate_redstone_mining"); // Cross-connection
        
        // Crystallography
        tree.addNode(new SkillTreeNode(
            "crystallography",
            "Crystallography",
            "Study the formation of crystals under pressure",
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE,
            -3, 11, // Diagonal down-right
            2
        ));
        tree.addConnection("deepslate_redstone_mining", "crystallography");
        
        // Deepslate lapis
        tree.addNode(new SkillTreeNode(
            "deepslate_lapis_mining",
            "Deepslate Lapis Mining",
            "Unlocks the ability to mine Deepslate Lapis Ore",
            Material.DEEPSLATE_LAPIS_ORE,
            ChatColor.BLUE,
            -2, 12, // Diagonal down-right
            5
        ));
        tree.addConnection("crystallography", "deepslate_lapis_mining");
        tree.addConnection("lapis_mining", "deepslate_lapis_mining"); // Cross-connection
        
        // Gem formation
        tree.addNode(new SkillTreeNode(
            "gem_formation",
            "Gem Formation",
            "Study how precious gems form deep underground",
            Material.AMETHYST_BLOCK,
            ChatColor.LIGHT_PURPLE,
            -1, 13, // Diagonal down-right
            2
        ));
        tree.addConnection("deepslate_lapis_mining", "gem_formation");
        
        // Deepslate diamond
        tree.addNode(new SkillTreeNode(
            "deepslate_diamond_mining",
            "Deepslate Diamond Mining",
            "Unlocks the ability to mine Deepslate Diamond Ore",
            Material.DEEPSLATE_DIAMOND_ORE,
            ChatColor.AQUA,
            0, 14, // Diagonal down-right
            6
        ));
        tree.addConnection("gem_formation", "deepslate_diamond_mining");
        tree.addConnection("diamond_mining", "deepslate_diamond_mining"); // Cross-connection
        
        // Rare gem expertise
        tree.addNode(new SkillTreeNode(
            "rare_gem_expertise",
            "Rare Gem Expertise",
            "Become an expert in the rarest gemstones",
            Material.EMERALD,
            ChatColor.GREEN,
            1, 15, // Diagonal down-right
            3
        ));
        tree.addConnection("deepslate_diamond_mining", "rare_gem_expertise");
        
        // Deepslate emerald
        tree.addNode(new SkillTreeNode(
            "deepslate_emerald_mining",
            "Deepslate Emerald Mining",
            "Unlocks the ability to mine Deepslate Emerald Ore",
            Material.DEEPSLATE_EMERALD_ORE,
            ChatColor.GREEN,
            2, 16, // Diagonal down-right
            7
        ));
        tree.addConnection("rare_gem_expertise", "deepslate_emerald_mining");
        tree.addConnection("emerald_mining", "deepslate_emerald_mining"); // Cross-connection
        
        // =====================================================================
        // BRANCH 3: NETHER ORES - Third major path for nether materials
        // =====================================================================
        
        // Nether exploration
        tree.addNode(new SkillTreeNode(
            "nether_exploration",
            "Nether Exploration",
            "Learn to navigate and mine in the Nether",
            Material.NETHERRACK,
            ChatColor.RED,
            1, -1, // Diagonal up-left from root
            2
        ));
        tree.addConnection("root", "nether_exploration");
        
        // Heat resistance
        tree.addNode(new SkillTreeNode(
            "heat_resistance",
            "Heat Resistance",
            "Develop techniques to mine in extreme heat",
            Material.MAGMA_BLOCK,
            ChatColor.GOLD,
            0, -2, // Diagonal up-left
            1
        ));
        tree.addConnection("nether_exploration", "heat_resistance");
        
        // Nether gold mining
        tree.addNode(new SkillTreeNode(
            "nether_gold_mining",
            "Nether Gold Mining",
            "Unlocks the ability to mine Nether Gold Ore",
            Material.NETHER_GOLD_ORE,
            ChatColor.GOLD,
            -1, -3, // Diagonal up-left
            3
        ));
        tree.addConnection("heat_resistance", "nether_gold_mining");
        tree.addConnection("gold_mining", "nether_gold_mining"); // Cross-connection
        
        // Basalt deltas
        tree.addNode(new SkillTreeNode(
            "basalt_deltas",
            "Basalt Deltas",
            "Learn to navigate the dangerous Basalt Deltas region",
            Material.BASALT,
            ChatColor.DARK_GRAY,
            -2, -4, // Diagonal up-left
            2
        ));
        tree.addConnection("nether_gold_mining", "basalt_deltas");
        
        // Nether quartz mining
        tree.addNode(new SkillTreeNode(
            "nether_quartz_mining",
            "Nether Quartz Mining",
            "Unlocks the ability to mine Nether Quartz Ore",
            Material.NETHER_QUARTZ_ORE,
            ChatColor.WHITE,
            -3, -5, // Diagonal up-left
            4
        ));
        tree.addConnection("basalt_deltas", "nether_quartz_mining");
        
        // Soul sand valley
        tree.addNode(new SkillTreeNode(
            "soul_sand_valley",
            "Soul Sand Valley",
            "Learn to navigate the treacherous Soul Sand Valley",
            Material.SOUL_SAND,
            ChatColor.DARK_BLUE,
            -4, -6, // Diagonal up-left
            2
        ));
        tree.addConnection("nether_quartz_mining", "soul_sand_valley");
        
        // Ancient debris connection
        tree.addNode(new SkillTreeNode(
            "ancient_debris_seeking",
            "Ancient Debris Seeking",
            "Learn techniques to locate Ancient Debris",
            Material.GILDED_BLACKSTONE,
            ChatColor.YELLOW,
            -5, -7, // Diagonal up-left
            3
        ));
        tree.addConnection("soul_sand_valley", "ancient_debris_seeking");
        tree.addConnection("ancient_debris_mining", "ancient_debris_seeking"); // Cross-connection to main branch
        
        // =====================================================================
        // BRANCH 4: MINING FORTUNE - Permanent mining fortune boosts
        // =====================================================================
        
        // Mining techniques
        tree.addNode(new SkillTreeNode(
            "mining_techniques",
            "Mining Techniques",
            "Learn fundamental techniques to improve mining yield",
            Material.STONE_PICKAXE,
            ChatColor.GRAY,
            -1, 0, // Left of root
            1
        ));
        tree.addConnection("root", "mining_techniques");
        
        // Fortune I
        tree.addNode(new SkillTreeNode(
            "fortune_i",
            "Mining Fortune I",
            "Gain +0.5 Mining Fortune permanently",
            Material.IRON_PICKAXE,
            ChatColor.WHITE,
            -2, -1, // Diagonal up-left
            2
        ));
        tree.addConnection("mining_techniques", "fortune_i");
        
        // Resource optimization
        tree.addNode(new SkillTreeNode(
            "resource_optimization",
            "Resource Optimization",
            "Learn to extract maximum resources from ores",
            Material.RAW_IRON,
            ChatColor.WHITE,
            -3, -1, // Left
            1
        ));
        tree.addConnection("fortune_i", "resource_optimization");
        
        // Fortune II
        tree.addNode(new SkillTreeNode(
            "fortune_ii",
            "Mining Fortune II",
            "Gain +1.0 Mining Fortune permanently",
            Material.IRON_PICKAXE,
            ChatColor.WHITE,
            -4, -2, // Diagonal up-left
            3
        ));
        tree.addConnection("resource_optimization", "fortune_ii");
        
        // Efficient extraction
        tree.addNode(new SkillTreeNode(
            "efficient_extraction",
            "Efficient Extraction",
            "Master efficient ore extraction techniques",
            Material.RAW_GOLD,
            ChatColor.YELLOW,
            -5, -2, // Left
            1
        ));
        tree.addConnection("fortune_ii", "efficient_extraction");
        
        // Fortune III
        tree.addNode(new SkillTreeNode(
            "fortune_iii",
            "Mining Fortune III",
            "Gain +1.5 Mining Fortune permanently",
            Material.GOLDEN_PICKAXE,
            ChatColor.YELLOW,
            -6, -3, // Diagonal up-left
            4
        ));
        tree.addConnection("efficient_extraction", "fortune_iii");
        
        // Precision mining
        tree.addNode(new SkillTreeNode(
            "precision_mining",
            "Precision Mining",
            "Learn precise techniques for maximum yield",
            Material.CLOCK,
            ChatColor.GOLD,
            -7, -3, // Left
            2
        ));
        tree.addConnection("fortune_iii", "precision_mining");
        
        // Fortune IV
        tree.addNode(new SkillTreeNode(
            "fortune_iv",
            "Mining Fortune IV",
            "Gain +2.0 Mining Fortune permanently",
            Material.GOLDEN_PICKAXE,
            ChatColor.YELLOW,
            -8, -4, // Diagonal up-left
            5
        ));
        tree.addConnection("precision_mining", "fortune_iv");
        
        // Advanced extraction
        tree.addNode(new SkillTreeNode(
            "advanced_extraction",
            "Advanced Extraction",
            "Master advanced resource extraction techniques",
            Material.DIAMOND,
            ChatColor.AQUA,
            -9, -4, // Left
            2
        ));
        tree.addConnection("fortune_iv", "advanced_extraction");
        
        // Fortune V
        tree.addNode(new SkillTreeNode(
            "fortune_v",
            "Mining Fortune V",
            "Gain +3.0 Mining Fortune permanently",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -10, -5, // Diagonal up-left
            6
        ));
        tree.addConnection("advanced_extraction", "fortune_v");
        
        // Master extraction
        tree.addNode(new SkillTreeNode(
            "master_extraction",
            "Master Extraction",
            "Become a master of resource extraction",
            Material.EMERALD,
            ChatColor.GREEN,
            -11, -5, // Left
            3
        ));
        tree.addConnection("fortune_v", "master_extraction");
        
        // Advanced Fortune I
        tree.addNode(new SkillTreeNode(
            "adv_fortune_i",
            "Advanced Fortune I",
            "Gain +5.0 Mining Fortune permanently",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -12, -6, // Diagonal up-left
            7
        ));
        tree.addConnection("master_extraction", "adv_fortune_i");
        
        // Expert extraction
        tree.addNode(new SkillTreeNode(
            "expert_extraction",
            "Expert Extraction",
            "Become an expert in advanced resource extraction",
            Material.BEACON,
            ChatColor.AQUA,
            -13, -6, // Left
            3
        ));
        tree.addConnection("adv_fortune_i", "expert_extraction");
        
        // Advanced Fortune II
        tree.addNode(new SkillTreeNode(
            "adv_fortune_ii",
            "Advanced Fortune II",
            "Gain +8.0 Mining Fortune permanently",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -14, -7, // Diagonal up-left
            8
        ));
        tree.addConnection("expert_extraction", "adv_fortune_ii");
        
        // Legendary extraction
        tree.addNode(new SkillTreeNode(
            "legendary_extraction",
            "Legendary Extraction",
            "Reach legendary status in resource extraction",
            Material.NETHER_STAR,
            ChatColor.WHITE,
            -15, -7, // Left
            4
        ));
        tree.addConnection("adv_fortune_ii", "legendary_extraction");
        
        // Advanced Fortune III
        tree.addNode(new SkillTreeNode(
            "adv_fortune_iii",
            "Advanced Fortune III",
            "Gain +12.0 Mining Fortune permanently",
            Material.NETHERITE_PICKAXE,
            ChatColor.DARK_PURPLE,
            -16, -8, // Diagonal up-left
            9
        ));
        tree.addConnection("legendary_extraction", "adv_fortune_iii");
        
        // Master Fortune
        tree.addNode(new SkillTreeNode(
            "master_fortune",
            "Master Fortune",
            "Gain +20.0 Mining Fortune permanently",
            Material.NETHERITE_PICKAXE,
            ChatColor.DARK_PURPLE,
            -17, -9, // Diagonal up-left
            10
        ));
        tree.addConnection("adv_fortune_iii", "master_fortune");
        
        // Grandmaster Fortune
        tree.addNode(new SkillTreeNode(
            "grandmaster_fortune",
            "Grandmaster Fortune",
            "Gain +50.0 Mining Fortune permanently",
            Material.NETHERITE_BLOCK,
            ChatColor.LIGHT_PURPLE,
            -18, -10, // Diagonal up-left
            15
        ));
        tree.addConnection("master_fortune", "grandmaster_fortune");
        
        // =====================================================================
        // BRANCH 5: SPECIAL ABILITIES - Five special unlockable abilities
        // =====================================================================
        
        // Mining abilities
        tree.addNode(new SkillTreeNode(
            "mining_abilities",
            "Mining Abilities",
            "Discover special mining abilities",
            Material.ENCHANTED_BOOK,
            ChatColor.LIGHT_PURPLE,
            0, 1, // Below root
            3
        ));
        tree.addConnection("root", "mining_abilities");
        
        // Ability paths - spread out in 5 directions
        
        // Path 1: Vein Miner (Down-left)
        tree.addNode(new SkillTreeNode(
            "ore_sense",
            "Ore Sense",
            "Develop a sense for connected ore veins",
            Material.COMPASS,
            ChatColor.YELLOW,
            -1, 2, // Diagonal down-left
            1
        ));
        tree.addConnection("mining_abilities", "ore_sense");
        
        tree.addNode(new SkillTreeNode(
            "vein_recognition",
            "Vein Recognition",
            "Learn to recognize ore vein patterns",
            Material.IRON_ORE,
            ChatColor.WHITE,
            -2, 3, // Diagonal down-left
            2
        ));
        tree.addConnection("ore_sense", "vein_recognition");
        
        tree.addNode(new SkillTreeNode(
            "vein_miner",
            "Vein Miner",
            "Special Ability: Mine connected ore veins with a single block break",
            Material.IRON_PICKAXE,
            ChatColor.GOLD,
            -3, 4, // Diagonal down-left
            6
        ));
        tree.addConnection("vein_recognition", "vein_miner");
        
        // Path 2: Smelting Touch (Down)
        tree.addNode(new SkillTreeNode(
            "heat_control",
            "Heat Control",
            "Learn to control the heat of your pickaxe",
            Material.MAGMA_CREAM,
            ChatColor.RED,
            0, 2, // Directly below
            1
        ));
        tree.addConnection("mining_abilities", "heat_control");
        
        tree.addNode(new SkillTreeNode(
            "molten_expertise",
            "Molten Expertise",
            "Master working with molten materials",
            Material.LAVA_BUCKET,
            ChatColor.GOLD,
            0, 3, // Directly below
            2
        ));
        tree.addConnection("heat_control", "molten_expertise");
        
        tree.addNode(new SkillTreeNode(
            "smelting_touch",
            "Smelting Touch",
            "Special Ability: Chance to automatically smelt ore drops",
            Material.FURNACE,
            ChatColor.RED,
            0, 4, // Directly below
            6
        ));
        tree.addConnection("molten_expertise", "smelting_touch");
        
        // Path 3: Ore Radar (Down-right)
        tree.addNode(new SkillTreeNode(
            "ore_detection",
            "Ore Detection",
            "Develop techniques to detect nearby ores",
            Material.SPYGLASS,
            ChatColor.AQUA,
            1, 2, // Diagonal down-right
            1
        ));
        tree.addConnection("mining_abilities", "ore_detection");
        
        tree.addNode(new SkillTreeNode(
            "detection_range",
            "Detection Range",
            "Increase your ore detection range",
            Material.ENDER_EYE,
            ChatColor.LIGHT_PURPLE,
            2, 3, // Diagonal down-right
            2
        ));
        tree.addConnection("ore_detection", "detection_range");
        
        tree.addNode(new SkillTreeNode(
            "ore_radar",
            "Ore Radar",
            "Special Ability: Nearby valuable ores glow through walls",
            Material.CONDUIT,
            ChatColor.AQUA,
            3, 4, // Diagonal down-right
            7
        ));
        tree.addConnection("detection_range", "ore_radar");
        
        // Path 4: Rock Blaster (Right)
        tree.addNode(new SkillTreeNode(
            "explosive_theory",
            "Explosive Theory",
            "Study the controlled use of explosives in mining",
            Material.GUNPOWDER,
            ChatColor.GRAY,
            1, 0, // Directly right
            1
        ));
        tree.addConnection("mining_abilities", "explosive_theory");
        
        tree.addNode(new SkillTreeNode(
            "controlled_blast",
            "Controlled Blast",
            "Learn to control explosive force for mining",
            Material.TNT_MINECART,
            ChatColor.RED,
            2, 0, // Directly right
            2
        ));
        tree.addConnection("explosive_theory", "controlled_blast");
        tree.addConnection("basic_mining", "controlled_blast"); // Connect to main branch
        
        tree.addNode(new SkillTreeNode(
            "rock_blaster",
            "Rock Blaster",
            "Special Ability: Break multiple blocks in a small radius",
            Material.TNT,
            ChatColor.RED,
            3, 0, // Directly right
            8
        ));
        tree.addConnection("controlled_blast", "rock_blaster");
        
        // Path 5: Lucky Strike (Up)
        tree.addNode(new SkillTreeNode(
            "lucky_mining",
            "Lucky Mining",
            "Develop techniques to strike ores at their weakest points",
            Material.RABBIT_FOOT,
            ChatColor.GREEN,
            0, -1, // Directly above
            1
        ));
        tree.addConnection("mining_abilities", "lucky_mining");
        tree.addConnection("nether_exploration", "lucky_mining"); // Connect to nether branch
        
        tree.addNode(new SkillTreeNode(
            "critical_mining",
            "Critical Mining",
            "Learn to hit critical mining spots for better yield",
            Material.GOLDEN_APPLE,
            ChatColor.GOLD,
            0, -2, // Directly above
            2
        ));
        tree.addConnection("lucky_mining", "critical_mining");
        tree.addConnection("heat_resistance", "critical_mining"); // Connect to nether branch
        
        tree.addNode(new SkillTreeNode(
            "lucky_strike",
            "Lucky Strike",
            "Special Ability: Chance for triple drops when mining ores",
            Material.DIAMOND,
            ChatColor.AQUA,
            0, -3, // Directly above
            10
        ));
        tree.addConnection("critical_mining", "lucky_strike");
        
        // =====================================================================
        // BRANCH 6: MINING XP REWARDS - Quick XP boosts
        // =====================================================================
        
        // XP rewards
        tree.addNode(new SkillTreeNode(
            "mining_knowledge",
            "Mining Knowledge",
            "Improve your mining knowledge for faster skill growth",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -1, -1, // Diagonal up-left from root
            2
        ));
        tree.addConnection("root", "mining_knowledge");
        
        // Minor XP boost
        tree.addNode(new SkillTreeNode(
            "minor_xp_boost",
            "Minor XP Boost",
            "Grants +25 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -2, -2, // Diagonal up-left
            1
        ));
        tree.addConnection("mining_knowledge", "minor_xp_boost");
        
        // Medium XP boost
        tree.addNode(new SkillTreeNode(
            "medium_xp_boost",
            "Medium XP Boost",
            "Grants +100 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -3, -3, // Diagonal up-left
            2
        ));
        tree.addConnection("minor_xp_boost", "medium_xp_boost");
        
        // Major XP boost
        tree.addNode(new SkillTreeNode(
            "major_xp_boost",
            "Major XP Boost",
            "Grants +250 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -4, -4, // Diagonal up-left
            3
        ));
        tree.addConnection("medium_xp_boost", "major_xp_boost");
        
        // Massive XP boost
        tree.addNode(new SkillTreeNode(
            "massive_xp_boost",
            "Massive XP Boost",
            "Grants +1000 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -5, -5, // Diagonal up-left
            5
        ));
        tree.addConnection("major_xp_boost", "massive_xp_boost");
        
        // Legendary XP boost
        tree.addNode(new SkillTreeNode(
            "legendary_xp_boost",
            "Legendary XP Boost",
            "Grants +5000 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -6, -6, // Diagonal up-left
            10
        ));
        tree.addConnection("massive_xp_boost", "legendary_xp_boost");
        
        // =====================================================================
        // BRANCH 7: SKILL TOKEN REWARDS - Earn more skill tokens
        // =====================================================================
        
        // Token path
        tree.addNode(new SkillTreeNode(
            "token_discovery",
            "Token Discovery",
            "Learn methods to earn skill tokens more efficiently",
            Material.EMERALD,
            ChatColor.GREEN,
            -1, 1, // Diagonal down-left from root
            2
        ));
        tree.addConnection("root", "token_discovery");
        
        // Token Reward I
        tree.addNode(new SkillTreeNode(
            "token_reward_i",
            "Token Reward I",
            "Grants +1 Ore Extraction Token",
            Material.EMERALD,
            ChatColor.GREEN,
            -2, 1, // Directly left
            1
        ));
        tree.addConnection("token_discovery", "token_reward_i");
        
        // Token Reward II
        tree.addNode(new SkillTreeNode(
            "token_reward_ii",
            "Token Reward II",
            "Grants +2 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -3, 2, // Diagonal down-left
            2
        ));
        tree.addConnection("token_reward_i", "token_reward_ii");
        
        // Token Reward III
        tree.addNode(new SkillTreeNode(
            "token_reward_iii",
            "Token Reward III",
            "Grants +3 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -4, 2, // Directly left
            3
        ));
        tree.addConnection("token_reward_ii", "token_reward_iii");
        
        // Token Reward IV
        tree.addNode(new SkillTreeNode(
            "token_reward_iv",
            "Token Reward IV",
            "Grants +4 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -5, 3, // Diagonal down-left
            4
        ));
        tree.addConnection("token_reward_iii", "token_reward_iv");
        
        // Token Reward V
        tree.addNode(new SkillTreeNode(
            "token_reward_v",
            "Token Reward V",
            "Grants +5 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -6, 3, // Directly left
            5
        ));
        tree.addConnection("token_reward_iv", "token_reward_v");
        
        // Major Token Reward
        tree.addNode(new SkillTreeNode(
            "major_token_reward",
            "Major Token Reward",
            "Grants +8 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -7, 4, // Diagonal down-left
            6
        ));
        tree.addConnection("token_reward_v", "major_token_reward");
        
        // Legendary Token Reward
        tree.addNode(new SkillTreeNode(
            "legendary_token_reward",
            "Legendary Token Reward",
            "Grants +10 Ore Extraction Tokens",
            Material.EMERALD,
            ChatColor.GREEN,
            -8, 4, // Directly left
            8
        ));
        tree.addConnection("major_token_reward", "legendary_token_reward");
        
        // Mining Token Exchange
        tree.addNode(new SkillTreeNode(
            "mining_token_exchange",
            "Mining Token Exchange",
            "Converts 5 Ore Extraction Tokens into 1 Mining Token",
            Material.DIAMOND,
            ChatColor.AQUA,
            -9, 5, // Diagonal down-left
            5
        ));
        tree.addConnection("legendary_token_reward", "mining_token_exchange");
        
        // =====================================================================
        // BRANCH 8: CRAFTING RECIPES - Unlock special mining tools
        // =====================================================================
        
        // Crafting branch
        tree.addNode(new SkillTreeNode(
            "mining_crafting",
            "Mining Crafting",
            "Learn to craft specialized mining tools",
            Material.CRAFTING_TABLE,
            ChatColor.GOLD,
            1, 1, // Diagonal down-right from root
            3
        ));
        tree.addConnection("root", "mining_crafting");
        
        // Basic Pickaxe Recipe
        tree.addNode(new SkillTreeNode(
            "basic_pickaxe_recipe",
            "Basic Pickaxe Recipe",
            "Unlocks crafting recipe for a specialized mining pickaxe",
            Material.STONE_PICKAXE,
            ChatColor.GRAY,
            2, 1, // Directly right
            2
        ));
        tree.addConnection("mining_crafting", "basic_pickaxe_recipe");
        
        // Advanced Pickaxe Recipe
        tree.addNode(new SkillTreeNode(
            "advanced_pickaxe_recipe",
            "Advanced Pickaxe Recipe",
            "Unlocks crafting recipe for an advanced mining pickaxe",
            Material.IRON_PICKAXE,
            ChatColor.WHITE,
            3, 2, // Diagonal down-right
            4
        ));
        tree.addConnection("basic_pickaxe_recipe", "advanced_pickaxe_recipe");
        
        // Expert Pickaxe Recipe
        tree.addNode(new SkillTreeNode(
            "expert_pickaxe_recipe",
            "Expert Pickaxe Recipe",
            "Unlocks crafting recipe for an expert mining pickaxe",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            4, 2, // Directly right
            6
        ));
        tree.addConnection("advanced_pickaxe_recipe", "expert_pickaxe_recipe");
        
        // Master Pickaxe Recipe
        tree.addNode(new SkillTreeNode(
            "master_pickaxe_recipe",
            "Master Pickaxe Recipe",
            "Unlocks crafting recipe for a master mining pickaxe",
            Material.NETHERITE_PICKAXE,
            ChatColor.DARK_PURPLE,
            5, 3, // Diagonal down-right
            8
        ));
        tree.addConnection("expert_pickaxe_recipe", "master_pickaxe_recipe");
        
        // Mining helmet
        tree.addNode(new SkillTreeNode(
            "mining_helmet_recipe",
            "Mining Helmet Recipe",
            "Unlocks crafting recipe for a mining helmet with built-in light",
            Material.GOLDEN_HELMET,
            ChatColor.YELLOW,
            2, 2, // Diagonal down-right from basic pickaxe
            3
        ));
        tree.addConnection("mining_crafting", "mining_helmet_recipe");
        
        // Mining boots
        tree.addNode(new SkillTreeNode(
            "mining_boots_recipe",
            "Mining Boots Recipe",
            "Unlocks crafting recipe for boots with improved mining mobility",
            Material.IRON_BOOTS,
            ChatColor.WHITE,
            3, 3, // Diagonal down-right
            4
        ));
        tree.addConnection("mining_helmet_recipe", "mining_boots_recipe");
        
        // Prospector's kit
        tree.addNode(new SkillTreeNode(
            "prospectors_kit_recipe",
            "Prospector's Kit Recipe",
            "Unlocks crafting recipe for a kit that helps locate rare ores",
            Material.BUNDLE,
            ChatColor.LIGHT_PURPLE,
            4, 4, // Diagonal down-right
            6
        ));
        tree.addConnection("mining_boots_recipe", "prospectors_kit_recipe");
        
        // =====================================================================
        // ADDITIONAL CONNECTIONS - Create sensible cross-connections
        // =====================================================================
        
        // Connect Fortune and Abilities
        tree.addConnection("fortune_iii", "mining_abilities");
        
        // Connect XP rewards and Ore progression
        tree.addConnection("medium_xp_boost", "iron_mining");
        
        // Connect tokens to crafting
        tree.addConnection("token_reward_iii", "mining_crafting");
        
        // Connect deepslate branch to fortune branch
        tree.addConnection("deepslate_discovery", "mining_techniques");
        
        // Connect crafting to ore progression
        tree.addConnection("basic_pickaxe_recipe", "iron_mining");
        
        // Connect nether branch to abilities
        tree.addConnection("nether_exploration", "explosive_theory");
        
        // Create some diagonal connections for better navigation
        tree.addConnection("ore_sense", "deepslate_coal_mining");
        tree.addConnection("token_discovery", "deepslate_discovery");
        tree.addConnection("mining_knowledge", "mining_techniques");
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