package com.server.profiles.skills.skills.mining.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Ore Extraction skill tree
 */
public class OreExtractionTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
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
        
        // Vein Miner (Upgradable to 9 levels)
        Map<Integer, String> veinMinerDesc = new HashMap<>();
        Map<Integer, Integer> veinMinerCosts = new HashMap<>();

        veinMinerDesc.put(1, "Break up to 2 connected ore blocks at once");
        veinMinerDesc.put(2, "Break up to 3 connected ore blocks at once");
        veinMinerDesc.put(3, "Break up to 4 connected ore blocks at once");
        veinMinerDesc.put(4, "Break up to 5 connected ore blocks at once");
        veinMinerDesc.put(5, "Break up to 6 connected ore blocks at once");
        veinMinerDesc.put(6, "Break up to 7 connected ore blocks at once");
        veinMinerDesc.put(7, "Break up to 8 connected ore blocks at once");
        veinMinerDesc.put(8, "Break up to 9 connected ore blocks at once");
        veinMinerDesc.put(9, "Break up to 10 connected ore blocks at once");

        veinMinerCosts.put(1, 3);
        veinMinerCosts.put(2, 4);
        veinMinerCosts.put(3, 5);
        veinMinerCosts.put(4, 6);
        veinMinerCosts.put(5, 7);
        veinMinerCosts.put(6, 8);
        veinMinerCosts.put(7, 9);
        veinMinerCosts.put(8, 10);
        veinMinerCosts.put(9, 12);

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
}