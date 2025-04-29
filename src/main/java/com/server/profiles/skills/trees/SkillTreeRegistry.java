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
     * Setup the Ore Extraction skill tree with horizontal and vertical connections only
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
            2, 0, // 2 tiles right of root (with 1 tile space between)
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
        
        // Iron Mining - Right of Coal Refining
        tree.addNode(new SkillTreeNode(
            "iron_mining",
            "Iron Mining",
            "Unlocks the ability to mine Iron Ore",
            Material.IRON_ORE,
            ChatColor.WHITE,
            6, 0, // 2 tiles right of coal_refining
            2
        ));
        tree.addConnection("coal_refining", "iron_mining");
        
        // Copper Mining - Below Iron Mining
        tree.addNode(new SkillTreeNode(
            "copper_mining",
            "Copper Mining",
            "Unlocks the ability to mine Copper Ore",
            Material.COPPER_ORE,
            ChatColor.GOLD,
            6, 2, // 2 tiles below iron_mining
            2
        ));
        tree.addConnection("iron_mining", "copper_mining");
        
        // Gold Mining - Right of Copper Mining
        tree.addNode(new SkillTreeNode(
            "gold_mining",
            "Gold Mining",
            "Unlocks the ability to mine Gold Ore",
            Material.GOLD_ORE,
            ChatColor.YELLOW,
            8, 2, // 2 tiles right of copper_mining
            3
        ));
        tree.addConnection("copper_mining", "gold_mining");
        
        // Redstone Mining - Below Gold Mining
        tree.addNode(new SkillTreeNode(
            "redstone_mining",
            "Redstone Mining",
            "Unlocks the ability to mine Redstone Ore",
            Material.REDSTONE_ORE,
            ChatColor.RED,
            8, 4, // 2 tiles below gold_mining
            3
        ));
        tree.addConnection("gold_mining", "redstone_mining");
        
        // Lapis Mining - Right of Redstone Mining
        tree.addNode(new SkillTreeNode(
            "lapis_mining",
            "Lapis Mining",
            "Unlocks the ability to mine Lapis Ore",
            Material.LAPIS_ORE,
            ChatColor.BLUE,
            10, 4, // 2 tiles right of redstone_mining
            4
        ));
        tree.addConnection("redstone_mining", "lapis_mining");
        
        // Diamond Mining - Below Lapis Mining
        tree.addNode(new SkillTreeNode(
            "diamond_mining",
            "Diamond Mining",
            "Unlocks the ability to mine Diamond Ore",
            Material.DIAMOND_ORE,
            ChatColor.AQUA,
            10, 6, // 2 tiles below lapis_mining
            5
        ));
        tree.addConnection("lapis_mining", "diamond_mining");
        
        // Emerald Mining - Right of Diamond Mining
        tree.addNode(new SkillTreeNode(
            "emerald_mining",
            "Emerald Mining",
            "Unlocks the ability to mine Emerald Ore",
            Material.EMERALD_ORE,
            ChatColor.GREEN,
            12, 6, // 2 tiles right of diamond_mining
            6
        ));
        tree.addConnection("diamond_mining", "emerald_mining");
        
        // Ancient Debris Mining - Below Emerald Mining
        tree.addNode(new SkillTreeNode(
            "ancient_debris_mining",
            "Ancient Debris Mining",
            "Unlocks the ability to mine Ancient Debris",
            Material.ANCIENT_DEBRIS,
            ChatColor.DARK_PURPLE,
            12, 8, // 2 tiles below emerald_mining
            8
        ));
        tree.addConnection("emerald_mining", "ancient_debris_mining");
        
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
        
        // Deepslate Coal Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_coal_mining",
            "Deepslate Coal Mining",
            "Unlocks the ability to mine Deepslate Coal Ore",
            Material.DEEPSLATE_COAL_ORE,
            ChatColor.GRAY,
            2, 2, // 2 tiles right of deepslate_discovery
            2
        ));
        tree.addConnection("deepslate_discovery", "deepslate_coal_mining");
        tree.addConnection("basic_mining", "deepslate_coal_mining"); // Cross-connection
        
        // Deepslate Iron Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_iron_mining",
            "Deepslate Iron Mining",
            "Unlocks the ability to mine Deepslate Iron Ore",
            Material.DEEPSLATE_IRON_ORE,
            ChatColor.WHITE,
            2, 4, // 2 tiles below deepslate_coal_mining
            3
        ));
        tree.addConnection("deepslate_coal_mining", "deepslate_iron_mining");
        tree.addConnection("iron_mining", "deepslate_iron_mining"); // Cross-connection
        
        // Deepslate Copper Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_copper_mining",
            "Deepslate Copper Mining",
            "Unlocks the ability to mine Deepslate Copper Ore",
            Material.DEEPSLATE_COPPER_ORE,
            ChatColor.GOLD,
            4, 4, // 2 tiles right of deepslate_iron_mining
            3
        ));
        tree.addConnection("deepslate_iron_mining", "deepslate_copper_mining");
        tree.addConnection("copper_mining", "deepslate_copper_mining"); // Cross-connection
        
        // Deepslate Gold Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_gold_mining",
            "Deepslate Gold Mining",
            "Unlocks the ability to mine Deepslate Gold Ore",
            Material.DEEPSLATE_GOLD_ORE,
            ChatColor.YELLOW,
            4, 6, // 2 tiles below deepslate_copper_mining
            4
        ));
        tree.addConnection("deepslate_copper_mining", "deepslate_gold_mining");
        tree.addConnection("gold_mining", "deepslate_gold_mining"); // Cross-connection
        
        // Deepslate Redstone Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_redstone_mining",
            "Deepslate Redstone Mining",
            "Unlocks the ability to mine Deepslate Redstone Ore",
            Material.DEEPSLATE_REDSTONE_ORE,
            ChatColor.RED,
            6, 6, // 2 tiles right of deepslate_gold_mining
            4
        ));
        tree.addConnection("deepslate_gold_mining", "deepslate_redstone_mining");
        tree.addConnection("redstone_mining", "deepslate_redstone_mining"); // Cross-connection
        
        // Deepslate Lapis Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_lapis_mining",
            "Deepslate Lapis Mining",
            "Unlocks the ability to mine Deepslate Lapis Ore",
            Material.DEEPSLATE_LAPIS_ORE,
            ChatColor.BLUE,
            6, 8, // 2 tiles below deepslate_redstone_mining
            5
        ));
        tree.addConnection("deepslate_redstone_mining", "deepslate_lapis_mining");
        tree.addConnection("lapis_mining", "deepslate_lapis_mining"); // Cross-connection
        
        // Deepslate Diamond Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_diamond_mining",
            "Deepslate Diamond Mining",
            "Unlocks the ability to mine Deepslate Diamond Ore",
            Material.DEEPSLATE_DIAMOND_ORE,
            ChatColor.AQUA,
            8, 8, // 2 tiles right of deepslate_lapis_mining
            6
        ));
        tree.addConnection("deepslate_lapis_mining", "deepslate_diamond_mining");
        tree.addConnection("diamond_mining", "deepslate_diamond_mining"); // Cross-connection
        
        // Deepslate Emerald Mining
        tree.addNode(new SkillTreeNode(
            "deepslate_emerald_mining",
            "Deepslate Emerald Mining",
            "Unlocks the ability to mine Deepslate Emerald Ore",
            Material.DEEPSLATE_EMERALD_ORE,
            ChatColor.GREEN,
            8, 10, // 2 tiles below deepslate_diamond_mining
            7
        ));
        tree.addConnection("deepslate_diamond_mining", "deepslate_emerald_mining");
        tree.addConnection("emerald_mining", "deepslate_emerald_mining"); // Cross-connection
        
        // =====================================================================
        // BRANCH 3: MINING FORTUNE - Left of root
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
        
        // Fortune I
        tree.addNode(new SkillTreeNode(
            "fortune_i",
            "Mining Fortune I",
            "Gain +0.5 Mining Fortune permanently",
            Material.IRON_PICKAXE,
            ChatColor.WHITE,
            -4, 0, // 2 tiles left of mining_techniques
            2
        ));
        tree.addConnection("mining_techniques", "fortune_i");
        
        // Fortune II
        tree.addNode(new SkillTreeNode(
            "fortune_ii",
            "Mining Fortune II",
            "Gain +1.0 Mining Fortune permanently",
            Material.IRON_PICKAXE,
            ChatColor.WHITE,
            -6, 0, // 2 tiles left of fortune_i
            3
        ));
        tree.addConnection("fortune_i", "fortune_ii");
        
        // Fortune III
        tree.addNode(new SkillTreeNode(
            "fortune_iii",
            "Mining Fortune III",
            "Gain +1.5 Mining Fortune permanently",
            Material.GOLDEN_PICKAXE,
            ChatColor.YELLOW,
            -8, 0, // 2 tiles left of fortune_ii
            4
        ));
        tree.addConnection("fortune_ii", "fortune_iii");
        
        // Fortune IV
        tree.addNode(new SkillTreeNode(
            "fortune_iv",
            "Mining Fortune IV",
            "Gain +2.0 Mining Fortune permanently",
            Material.GOLDEN_PICKAXE,
            ChatColor.YELLOW,
            -10, 0, // 2 tiles left of fortune_iii
            5
        ));
        tree.addConnection("fortune_iii", "fortune_iv");
        
        // Fortune V
        tree.addNode(new SkillTreeNode(
            "fortune_v",
            "Mining Fortune V",
            "Gain +3.0 Mining Fortune permanently",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -12, 0, // 2 tiles left of fortune_iv
            6
        ));
        tree.addConnection("fortune_iv", "fortune_v");
        
        // Master Fortune
        tree.addNode(new SkillTreeNode(
            "master_fortune",
            "Master Fortune",
            "Gain +5.0 Mining Fortune permanently",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -12, -2, // 2 tiles above master_fortune
            8
        ));
        tree.addConnection("fortune_v", "master_fortune");
        
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
        
        // Nether Gold Mining
        tree.addNode(new SkillTreeNode(
            "nether_gold_mining",
            "Nether Gold Mining",
            "Unlocks the ability to mine Nether Gold Ore",
            Material.NETHER_GOLD_ORE,
            ChatColor.GOLD,
            2, -2, // 2 tiles right of nether_exploration
            3
        ));
        tree.addConnection("nether_exploration", "nether_gold_mining");
        tree.addConnection("gold_mining", "nether_gold_mining"); // Cross-connection
        
        // Nether Quartz Mining
        tree.addNode(new SkillTreeNode(
            "nether_quartz_mining",
            "Nether Quartz Mining",
            "Unlocks the ability to mine Nether Quartz Ore",
            Material.NETHER_QUARTZ_ORE,
            ChatColor.WHITE,
            2, -4, // 2 tiles above nether_gold_mining
            4
        ));
        tree.addConnection("nether_gold_mining", "nether_quartz_mining");
        
        // Ancient Debris Seeking
        tree.addNode(new SkillTreeNode(
            "ancient_debris_seeking",
            "Ancient Debris Seeking",
            "Learn techniques to locate Ancient Debris",
            Material.GILDED_BLACKSTONE,
            ChatColor.GOLD,
            4, -4, // 2 tiles right of nether_quartz_mining
            6
        ));
        tree.addConnection("nether_quartz_mining", "ancient_debris_seeking");
        tree.addConnection("ancient_debris_mining", "ancient_debris_seeking"); // Cross-connection
        
        // =====================================================================
        // BRANCH 5: SPECIAL ABILITIES - Center branch below root
        // =====================================================================
        
        // Mining Abilities
        tree.addNode(new SkillTreeNode(
            "mining_abilities",
            "Mining Abilities",
            "Discover special mining abilities",
            Material.ENCHANTED_BOOK,
            ChatColor.LIGHT_PURPLE,
            0, 4, // 2 tiles below deepslate_discovery (4 below root)
            3
        ));
        tree.addConnection("deepslate_discovery", "mining_abilities");
        
        // Vein Miner Branch - Left of Mining Abilities
        tree.addNode(new SkillTreeNode(
            "vein_miner",
            "Vein Miner",
            "Special Ability: Mine connected ore veins with a single block break",
            Material.IRON_PICKAXE,
            ChatColor.GOLD,
            -2, 4, // 2 tiles left of mining_abilities
            6
        ));
        tree.addConnection("mining_abilities", "vein_miner");
        
        // Smelting Touch Branch - Below Mining Abilities
        tree.addNode(new SkillTreeNode(
            "smelting_touch",
            "Smelting Touch",
            "Special Ability: Chance to automatically smelt ore drops",
            Material.FURNACE,
            ChatColor.RED,
            0, 6, // 2 tiles below mining_abilities
            6
        ));
        tree.addConnection("mining_abilities", "smelting_touch");
        
        // Ore Radar Branch - Right of Mining Abilities
        tree.addNode(new SkillTreeNode(
            "ore_radar",
            "Ore Radar",
            "Special Ability: Nearby valuable ores glow through walls",
            Material.CONDUIT,
            ChatColor.AQUA,
            2, 4, // 2 tiles right of mining_abilities
            7
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
        
        // Minor XP Boost
        tree.addNode(new SkillTreeNode(
            "minor_xp_boost",
            "Minor XP Boost",
            "Grants +25 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -4, -2, // 2 tiles left of mining_knowledge
            1
        ));
        tree.addConnection("mining_knowledge", "minor_xp_boost");
        
        // Medium XP Boost
        tree.addNode(new SkillTreeNode(
            "medium_xp_boost",
            "Medium XP Boost",
            "Grants +100 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -4, -4, // 2 tiles above minor_xp_boost
            2
        ));
        tree.addConnection("minor_xp_boost", "medium_xp_boost");
        
        // Major XP Boost
        tree.addNode(new SkillTreeNode(
            "major_xp_boost",
            "Major XP Boost",
            "Grants +250 Mining Skill XP",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            -6, -4, // 2 tiles left of medium_xp_boost
            3
        ));
        tree.addConnection("medium_xp_boost", "major_xp_boost");
        
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
        
        // Token Reward I
        tree.addNode(new SkillTreeNode(
            "token_reward_i",
            "Token Reward I",
            "Grants +1 Ore Extraction Token",
            Material.EMERALD,
            ChatColor.GREEN,
            -4, 2, // 2 tiles left of token_discovery
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
            -4, 4, // 2 tiles below token_reward_i
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
            -6, 4, // 2 tiles left of token_reward_ii
            3
        ));
        tree.addConnection("token_reward_ii", "token_reward_iii");
        
        // =====================================================================
        // BRANCH 8: CRAFTING RECIPES - Upper-right of root
        // =====================================================================
        
        // Mining Crafting
        tree.addNode(new SkillTreeNode(
            "mining_crafting",
            "Mining Crafting",
            "Learn to craft specialized mining tools",
            Material.CRAFTING_TABLE,
            ChatColor.GOLD,
            2, -4, // 2 tiles diagonal from nether_exploration (right)
            3
        ));
        tree.addConnection("nether_exploration", "mining_crafting");
        
        // Basic Pickaxe Recipe
        tree.addNode(new SkillTreeNode(
            "basic_pickaxe_recipe",
            "Basic Pickaxe Recipe",
            "Unlocks crafting recipe for a specialized mining pickaxe",
            Material.STONE_PICKAXE,
            ChatColor.GRAY,
            4, -2, // 2 tiles diagonal from mining_crafting (right and down)
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
            6, -2, // 2 tiles right of basic_pickaxe_recipe
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
            6, 0, // 2 tiles below advanced_pickaxe_recipe
            6
        ));
        tree.addConnection("advanced_pickaxe_recipe", "expert_pickaxe_recipe");
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