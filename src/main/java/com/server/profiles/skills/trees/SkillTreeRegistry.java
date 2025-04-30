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
     * Setup the Ore Extraction skill tree with upgradable nodes for stats and abilities
     */
    private void setupOreExtractionSkillTree(SkillTree tree) {
        // Root node (automatically added by skill tree creation)
        // This is at position (0,0)
        
        // =====================================================================
        // BRANCH 1: MAIN ORE PATH - Directly to the right of root
        // =====================================================================
        
        // Basic Mining - First tier, right of root
        tree.addNode(new SkillTreeNode(
            "basic_mining",
            "Basic Mining",
            "Unlocks the ability to mine Coal Ore",
            Material.COAL_ORE,
            ChatColor.GRAY,
            2, 0, // 2 tiles right of root
            1
        ));
        tree.addConnection("root", "basic_mining");
        
        // Coal Refining - Right of Basic Mining
        tree.addNode(new SkillTreeNode(
            "coal_refining",
            "Coal Refining",
            "Learn to extract coal more efficiently",
            Material.COAL,
            ChatColor.GRAY,
            4, 0, // 2 tiles right of basic_mining
            1
        ));
        tree.addConnection("basic_mining", "coal_refining");
        
        // Iron Mining - Right of Coal Refining - Upgradable (3 levels)
        Map<Integer, String> ironDescriptions = new HashMap<>();
        ironDescriptions.put(1, "Unlocks the ability to mine Iron Ore");
        ironDescriptions.put(2, "Improves iron mining speed by 10%");
        ironDescriptions.put(3, "Chance to find extra iron nuggets");
        
        Map<Integer, Integer> ironCosts = new HashMap<>();
        ironCosts.put(1, 2);
        ironCosts.put(2, 2);
        ironCosts.put(3, 3);
        
        tree.addNode(new SkillTreeNode(
            "iron_mining",
            "Iron Mining",
            Material.IRON_ORE,
            ChatColor.WHITE,
            6, 0, // 2 tiles right of coal_refining
            ironDescriptions,
            ironCosts
        ));
        tree.addConnection("coal_refining", "iron_mining");
        
        // =====================================================================
        // BRANCH 3: MINING FORTUNE - Left of root - UPGRADABLE NODES
        // =====================================================================
        
        // Mining Techniques
        tree.addNode(new SkillTreeNode(
            "mining_techniques",
            "Mining Techniques",
            "Learn fundamental techniques to improve mining yield",
            Material.STONE_PICKAXE,
            ChatColor.GRAY,
            -2, 0, // 2 tiles left of root
            1
        ));
        tree.addConnection("root", "mining_techniques");
        
        // Create upgradable node for Fortune with custom descriptions and costs
        Map<Integer, String> fortuneDescriptions = new HashMap<>();
        Map<Integer, Integer> fortuneCosts = new HashMap<>();
        
        fortuneDescriptions.put(1, "Gain +0.5 Mining Fortune");
        fortuneDescriptions.put(2, "Gain +1.0 Mining Fortune");
        fortuneDescriptions.put(3, "Gain +1.5 Mining Fortune");
        fortuneDescriptions.put(4, "Gain +2.0 Mining Fortune");
        fortuneDescriptions.put(5, "Gain +3.0 Mining Fortune");
        
        fortuneCosts.put(1, 2);  // Level 1 costs 2 tokens
        fortuneCosts.put(2, 3);  // Level 2 costs 3 more tokens
        fortuneCosts.put(3, 4);  // Level 3 costs 4 more tokens
        fortuneCosts.put(4, 5);  // Level 4 costs 5 more tokens
        fortuneCosts.put(5, 8);  // Level 5 costs 8 more tokens
        
        // Fortune Node (Upgradable)
        tree.addNode(new SkillTreeNode(
            "mining_fortune",
            "Mining Fortune",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -4, 0, // 2 tiles left of mining_techniques
            fortuneDescriptions,
            fortuneCosts
        ));
        tree.addConnection("mining_techniques", "mining_fortune");
        
        // Mining Speed (Upgradable)
        Map<Integer, String> speedDescriptions = new HashMap<>();
        Map<Integer, Integer> speedCosts = new HashMap<>();
        
        speedDescriptions.put(1, "Increase mining speed by 5%");
        speedDescriptions.put(2, "Increase mining speed by 10%");
        speedDescriptions.put(3, "Increase mining speed by 15%");
        speedDescriptions.put(4, "Increase mining speed by 20%");
        speedDescriptions.put(5, "Increase mining speed by 30%");
        
        speedCosts.put(1, 2);
        speedCosts.put(2, 2);
        speedCosts.put(3, 3);
        speedCosts.put(4, 4);
        speedCosts.put(5, 6);
        
        tree.addNode(new SkillTreeNode(
            "mining_speed",
            "Mining Speed",
            Material.GOLDEN_PICKAXE,
            ChatColor.YELLOW,
            -6, 0, // 2 tiles left of mining_fortune
            speedDescriptions,
            speedCosts
        ));
        tree.addConnection("mining_fortune", "mining_speed", 3); // Requires Fortune level 3
        
        // Master Fortune (advanced node, requires Mining Speed level 5)
        tree.addNode(new SkillTreeNode(
            "master_fortune",
            "Master Fortune",
            "Gain +5.0 Mining Fortune permanently",
            Material.NETHERITE_PICKAXE,
            ChatColor.LIGHT_PURPLE,
            -6, -2, // 2 tiles above mining_speed
            8
        ));
        tree.addConnection("mining_speed", "master_fortune", 5); // Requires Mining Speed level 5
        
        // =====================================================================
        // BRANCH 2: DEEPSLATE PATH - Below the root
        // =====================================================================
        
        // Deepslate Discovery
        tree.addNode(new SkillTreeNode(
            "deepslate_discovery",
            "Deepslate Discovery",
            "Learn about the properties of deep underground stone",
            Material.DEEPSLATE,
            ChatColor.DARK_GRAY,
            0, 2, // 2 tiles below root
            1
        ));
        tree.addConnection("root", "deepslate_discovery");
        
        // Deepslate Efficiency (Upgradable)
        Map<Integer, String> deepslateEfficiency = new HashMap<>();
        Map<Integer, Integer> deepslateCosts = new HashMap<>();
        
        deepslateEfficiency.put(1, "Increase mining speed for deepslate blocks by 10%");
        deepslateEfficiency.put(2, "Increase mining speed for deepslate blocks by 20%");
        deepslateEfficiency.put(3, "Increase mining speed for deepslate blocks by 30%");
        
        deepslateCosts.put(1, 2);
        deepslateCosts.put(2, 3);
        deepslateCosts.put(3, 4);
        
        tree.addNode(new SkillTreeNode(
            "deepslate_efficiency",
            "Deepslate Efficiency",
            Material.DEEPSLATE_IRON_ORE,
            ChatColor.DARK_GRAY,
            0, 4, // 2 tiles below deepslate_discovery
            deepslateEfficiency,
            deepslateCosts
        ));
        tree.addConnection("deepslate_discovery", "deepslate_efficiency");
        
        // =====================================================================
        // BRANCH 4: NETHER ORES - Above the root
        // =====================================================================
        
        // Nether Exploration
        tree.addNode(new SkillTreeNode(
            "nether_exploration",
            "Nether Exploration",
            "Learn to navigate and mine in the Nether",
            Material.NETHERRACK,
            ChatColor.RED,
            0, -2, // 2 tiles above root
            2
        ));
        tree.addConnection("root", "nether_exploration");
        
        // Nether Mining (Upgradable)
        Map<Integer, String> netherMiningDesc = new HashMap<>();
        Map<Integer, Integer> netherMiningCosts = new HashMap<>();
        
        netherMiningDesc.put(1, "Mine Nether ores 10% faster");
        netherMiningDesc.put(2, "Mine Nether ores 20% faster");
        netherMiningDesc.put(3, "Mine Nether ores 30% faster");
        netherMiningDesc.put(4, "Mine Nether ores 50% faster");
        
        netherMiningCosts.put(1, 2);
        netherMiningCosts.put(2, 3);
        netherMiningCosts.put(3, 4);
        netherMiningCosts.put(4, 5);
        
        tree.addNode(new SkillTreeNode(
            "nether_mining",
            "Nether Mining",
            Material.NETHER_GOLD_ORE,
            ChatColor.GOLD,
            2, -2, // 2 tiles right of nether_exploration
            netherMiningDesc,
            netherMiningCosts
        ));
        tree.addConnection("nether_exploration", "nether_mining");
        
        // Ancient Debris Seeking
        tree.addNode(new SkillTreeNode(
            "ancient_debris_seeking",
            "Ancient Debris Seeking",
            "Learn techniques to locate Ancient Debris",
            Material.GILDED_BLACKSTONE,
            ChatColor.GOLD,
            4, -2, // 2 tiles right of nether_mining
            6
        ));
        tree.addConnection("nether_mining", "ancient_debris_seeking", 3); // Requires Nether Mining level 3
        
        // =====================================================================
        // BRANCH 5: SPECIAL ABILITIES - Center branch below root
        // =====================================================================
        
        // Mining Abilities Hub
        tree.addNode(new SkillTreeNode(
            "mining_abilities",
            "Mining Abilities",
            "Discover special mining abilities",
            Material.ENCHANTED_BOOK,
            ChatColor.LIGHT_PURPLE,
            0, 6, // 2 tiles below deepslate_efficiency
            3
        ));
        tree.addConnection("deepslate_efficiency", "mining_abilities", 2); // Requires level 2
        
        // Vein Miner (Upgradable)
        Map<Integer, String> veinMinerDesc = new HashMap<>();
        Map<Integer, Integer> veinMinerCosts = new HashMap<>();
        
        veinMinerDesc.put(1, "Break up to 3 connected ore blocks at once");
        veinMinerDesc.put(2, "Break up to 5 connected ore blocks at once");
        veinMinerDesc.put(3, "Break up to 8 connected ore blocks at once");
        
        veinMinerCosts.put(1, 6);
        veinMinerCosts.put(2, 8);
        veinMinerCosts.put(3, 12);
        
        tree.addNode(new SkillTreeNode(
            "vein_miner",
            "Vein Miner",
            Material.IRON_PICKAXE,
            ChatColor.GOLD,
            -2, 6, // 2 tiles left of mining_abilities
            veinMinerDesc,
            veinMinerCosts
        ));
        tree.addConnection("mining_abilities", "vein_miner");
        
        // Smelting Touch (Upgradable)
        Map<Integer, String> smeltingDesc = new HashMap<>();
        Map<Integer, Integer> smeltingCosts = new HashMap<>();
        
        smeltingDesc.put(1, "20% chance to automatically smelt ore drops");
        smeltingDesc.put(2, "40% chance to automatically smelt ore drops");
        smeltingDesc.put(3, "60% chance to automatically smelt ore drops");
        smeltingDesc.put(4, "80% chance to automatically smelt ore drops");
        smeltingDesc.put(5, "100% chance to automatically smelt ore drops");
        
        smeltingCosts.put(1, 4);
        smeltingCosts.put(2, 5);
        smeltingCosts.put(3, 6);
        smeltingCosts.put(4, 7);
        smeltingCosts.put(5, 8);
        
        tree.addNode(new SkillTreeNode(
            "smelting_touch",
            "Smelting Touch",
            Material.FURNACE,
            ChatColor.RED,
            0, 8, // 2 tiles below mining_abilities
            smeltingDesc,
            smeltingCosts
        ));
        tree.addConnection("mining_abilities", "smelting_touch");
        
        // Ore Radar (Upgradable)
        Map<Integer, String> radarDesc = new HashMap<>();
        Map<Integer, Integer> radarCosts = new HashMap<>();
        
        radarDesc.put(1, "Nearby valuable ores glow within 5 blocks");
        radarDesc.put(2, "Nearby valuable ores glow within 8 blocks");
        radarDesc.put(3, "Nearby valuable ores glow within 12 blocks");
        
        radarCosts.put(1, 7);
        radarCosts.put(2, 9);
        radarCosts.put(3, 12);
        
        tree.addNode(new SkillTreeNode(
            "ore_radar",
            "Ore Radar",
            Material.CONDUIT,
            ChatColor.AQUA,
            2, 6, // 2 tiles right of mining_abilities
            radarDesc,
            radarCosts
        ));
        tree.addConnection("mining_abilities", "ore_radar");
        
        // =====================================================================
        // BRANCH 6: XP BOOST REWARDS - Above-left of root
        // =====================================================================
        
        // Mining Knowledge
        tree.addNode(new SkillTreeNode(
            "mining_knowledge",
            "Mining Knowledge",
            "Improve your mining knowledge for faster skill growth",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -2, -2, // 2 tiles diagonal from root (left and up)
            2
        ));
        tree.addConnection("root", "mining_knowledge");
        
        // XP Boost (Upgradable)
        Map<Integer, String> xpBoostDesc = new HashMap<>();
        Map<Integer, Integer> xpBoostCosts = new HashMap<>();
        
        xpBoostDesc.put(1, "Gain +10% mining XP from all sources");
        xpBoostDesc.put(2, "Gain +20% mining XP from all sources");
        xpBoostDesc.put(3, "Gain +30% mining XP from all sources");
        xpBoostDesc.put(4, "Gain +50% mining XP from all sources");
        
        xpBoostCosts.put(1, 3);
        xpBoostCosts.put(2, 4);
        xpBoostCosts.put(3, 5);
        xpBoostCosts.put(4, 8);
        
        tree.addNode(new SkillTreeNode(
            "xp_boost",
            "XP Boost",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -4, -2, // 2 tiles left of mining_knowledge
            xpBoostDesc,
            xpBoostCosts
        ));
        tree.addConnection("mining_knowledge", "xp_boost");
        
        // =====================================================================
        // BRANCH 7: TOKEN REWARDS - Below-left of root
        // =====================================================================
        
        // Token Discovery
        tree.addNode(new SkillTreeNode(
            "token_discovery",
            "Token Discovery",
            "Learn methods to earn skill tokens more efficiently",
            Material.EMERALD,
            ChatColor.GREEN,
            -2, 2, // 2 tiles diagonal from root (left and down)
            2
        ));
        tree.addConnection("root", "token_discovery");
        
        // Token Yield (Upgradable)
        Map<Integer, String> tokenYieldDesc = new HashMap<>();
        Map<Integer, Integer> tokenYieldCosts = new HashMap<>();
        
        tokenYieldDesc.put(1, "10% chance to get an extra token when earning tokens");
        tokenYieldDesc.put(2, "20% chance to get an extra token when earning tokens");
        tokenYieldDesc.put(3, "30% chance to get an extra token when earning tokens");
        tokenYieldDesc.put(4, "50% chance to get an extra token when earning tokens");
        
        tokenYieldCosts.put(1, 5);
        tokenYieldCosts.put(2, 6);
        tokenYieldCosts.put(3, 8);
        tokenYieldCosts.put(4, 12);
        
        tree.addNode(new SkillTreeNode(
            "token_yield",
            "Token Yield",
            Material.EMERALD,
            ChatColor.GREEN,
            -4, 2, // 2 tiles left of token_discovery
            tokenYieldDesc,
            tokenYieldCosts
        ));
        tree.addConnection("token_discovery", "token_yield");
        
        // =====================================================================
        // BRANCH 8: PASSIVE BUFFS - Top-left of root
        // =====================================================================
        
        // Passive Buff Connection
        tree.addNode(new SkillTreeNode(
            "passive_buffs",
            "Passive Mining Buffs",
            "Unlock passive buffs to help with mining",
            Material.BEACON,
            ChatColor.AQUA,
            -3, -3, // Diagonal from mining_knowledge
            3
        ));
        tree.addConnection("mining_knowledge", "passive_buffs");
        
        // Mining Stamina (Upgradable)
        Map<Integer, String> staminaDesc = new HashMap<>();
        Map<Integer, Integer> staminaCosts = new HashMap<>();
        
        staminaDesc.put(1, "Mining consumes 10% less hunger");
        staminaDesc.put(2, "Mining consumes 20% less hunger");
        staminaDesc.put(3, "Mining consumes 30% less hunger");
        
        staminaCosts.put(1, 2);
        staminaCosts.put(2, 3);
        staminaCosts.put(3, 4);
        
        tree.addNode(new SkillTreeNode(
            "mining_stamina",
            "Mining Stamina",
            Material.COOKED_BEEF,
            ChatColor.RED,
            -5, -3, // 2 tiles left of passive_buffs
            staminaDesc,
            staminaCosts
        ));
        tree.addConnection("passive_buffs", "mining_stamina");
        
        // Regeneration (Upgradable)
        Map<Integer, String> regenDesc = new HashMap<>();
        Map<Integer, Integer> regenCosts = new HashMap<>();
        
        regenDesc.put(1, "Regenerate 0.2 health per second while mining");
        regenDesc.put(2, "Regenerate 0.4 health per second while mining");
        regenDesc.put(3, "Regenerate 0.6 health per second while mining");
        
        regenCosts.put(1, 3);
        regenCosts.put(2, 5);
        regenCosts.put(3, 7);
        
        tree.addNode(new SkillTreeNode(
            "mining_regeneration",
            "Mining Regeneration",
            Material.GOLDEN_APPLE,
            ChatColor.GOLD,
            -3, -5, // 2 tiles above passive_buffs
            regenDesc,
            regenCosts
        ));
        tree.addConnection("passive_buffs", "mining_regeneration");
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