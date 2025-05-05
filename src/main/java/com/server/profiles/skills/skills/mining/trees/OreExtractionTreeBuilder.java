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
        // MINING FORTUNE NODE - To the left of root
        // =====================================================================
        
        // Create upgradable node for Fortune with custom descriptions and costs
        Map<Integer, String> fortuneDescriptions = new HashMap<>();
        Map<Integer, Integer> fortuneCosts = new HashMap<>();
        
        // Set descriptions for all 50 levels - Each level gives +0.5 mining fortune
        for (int i = 1; i <= 50; i++) {
            fortuneDescriptions.put(i, "Gain +" + (i * 0.5) + " Mining Fortune");
        }
        
        // Set increasing costs for all 50 levels
        // Formula: cost increases by 1 every 5 levels
        for (int i = 1; i <= 50; i++) {
            int baseCost = 1 + ((i - 1) / 5);
            fortuneCosts.put(i, baseCost);
        }
        
        // Mining Fortune Node (Upgradable up to 50 levels)
        tree.addNode(new SkillTreeNode(
            "mining_fortune",
            "Mining Fortune",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -2, 0, // 2 tiles left of root
            fortuneDescriptions,
            fortuneCosts
        ));
        tree.addConnection("root", "mining_fortune");
        
        // =====================================================================
        // XP BUFF NODE - To the right of root
        // =====================================================================
        
        // Create upgradable node for XP buff with custom descriptions and costs
        Map<Integer, String> xpBuffDescriptions = new HashMap<>();
        Map<Integer, Integer> xpBuffCosts = new HashMap<>();
        
        // Set descriptions for all 50 levels - Each level gives +0.5% XP
        for (int i = 1; i <= 50; i++) {
            xpBuffDescriptions.put(i, "Gain +" + (i * 0.5) + "% Ore Extraction XP");
        }
        
        // Set increasing costs for all 50 levels
        // Formula: cost increases by 1 every 5 levels
        for (int i = 1; i <= 50; i++) {
            int baseCost = 1 + ((i - 1) / 5);
            xpBuffCosts.put(i, baseCost);
        }
        
        // XP Buff Node (Upgradable up to 50 levels)
        tree.addNode(new SkillTreeNode(
            "ore_extraction_xp",
            "Extraction Expertise",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            2, 0, // 2 tiles right of root
            xpBuffDescriptions,
            xpBuffCosts
        ));
        tree.addConnection("root", "ore_extraction_xp");
        
        // =====================================================================
        // ITEM UNLOCKER NODE - Above the root
        // =====================================================================
        
        // Item Unlocker Node (Single level)
        tree.addNode(new SkillTreeNode(
            "item_unlocker",
            "Item Mastery",
            "Unlocks special mining items (Coming Soon)",
            Material.CHEST,
            ChatColor.GOLD,
            0, -2, // 2 tiles above root
            3
        ));
        tree.addConnection("root", "item_unlocker");
        
        // =====================================================================
        // ABILITY UNLOCKER NODE - Below the root
        // =====================================================================
        
        // Ability Unlocker Node (Single level)
        tree.addNode(new SkillTreeNode(
            "ability_unlocker",
            "Ability Mastery",
            "Unlocks special mining abilities",
            Material.ENCHANTED_BOOK,
            ChatColor.LIGHT_PURPLE,
            0, 2, // 2 tiles below root
            3
        ));
        tree.addConnection("root", "ability_unlocker");

        // =====================================================================
        // VEIN MINER ABILITY NODE - Below the Ability Unlocker
        // =====================================================================
        
        // Create descriptions and costs for the vein miner node
        Map<Integer, String> veinMinerDescriptions = new HashMap<>();
        Map<Integer, Integer> veinMinerCosts = new HashMap<>();
        
        // Each level increases the max number of blocks that can be mined
        veinMinerDescriptions.put(1, "Mine up to 2 connected ore blocks at once");
        veinMinerDescriptions.put(2, "Mine up to 3 connected ore blocks at once");
        veinMinerDescriptions.put(3, "Mine up to 4 connected ore blocks at once");
        veinMinerDescriptions.put(4, "Mine up to 5 connected ore blocks at once");
        veinMinerDescriptions.put(5, "Mine up to 6 connected ore blocks at once");
        veinMinerDescriptions.put(6, "Mine up to 7 connected ore blocks at once");
        veinMinerDescriptions.put(7, "Mine up to 8 connected ore blocks at once");
        veinMinerDescriptions.put(8, "Mine up to 9 connected ore blocks at once");
        veinMinerDescriptions.put(9, "Mine up to 10 connected ore blocks at once");
        
        // Costs increase with each level
        veinMinerCosts.put(1, 3);
        veinMinerCosts.put(2, 5);
        veinMinerCosts.put(3, 5);
        veinMinerCosts.put(4, 5);
        veinMinerCosts.put(5, 7);
        veinMinerCosts.put(6, 7);
        veinMinerCosts.put(7, 7);
        veinMinerCosts.put(8, 10);
        veinMinerCosts.put(9, 10);
        
        // Vein Miner Node (Upgradable up to 9 levels)
        tree.addNode(new SkillTreeNode(
            "vein_miner",
            "Vein Miner",
            Material.NETHERITE_PICKAXE,
            ChatColor.RED,
            0, 4, // 2 tiles below ability_unlocker
            veinMinerDescriptions,
            veinMinerCosts
        ));
        tree.addConnection("ability_unlocker", "vein_miner");

        // =====================================================================
        // MYSTERY ABILITY NODES - Around Vein Miner (with 1 slot gap)
        // =====================================================================
        
        // Mystery Ability Node below Vein Miner (2 tiles below)
        tree.addNode(new SkillTreeNode(
            "vein_miner_path_bottom",
            "Vein Miner Mystery Path",
            "A mysterious path that may unlock greater vein mining secrets",
            Material.ANCIENT_DEBRIS,
            ChatColor.DARK_RED,
            0, 6, // 2 tiles below vein_miner
            2
        ));
        tree.addConnection("vein_miner", "vein_miner_path_bottom");
        
        // Mystery Ability Node to the left of Vein Miner (2 tiles left)
        tree.addNode(new SkillTreeNode(
            "vein_miner_path_left",
            "Vein Miner Mystery Path",
            "A mysterious path that may unlock greater vein mining secrets",
            Material.RAW_GOLD,
            ChatColor.GOLD,
            -2, 4, // 2 tiles left of vein_miner
            2
        ));
        tree.addConnection("vein_miner", "vein_miner_path_left");
        
        // Mystery Ability Node to the right of Vein Miner (2 tiles right)
        tree.addNode(new SkillTreeNode(
            "vein_miner_path_right",
            "Vein Miner Mystery Path",
            "A mysterious path that may unlock greater vein mining secrets",
            Material.RAW_COPPER,
            ChatColor.GOLD,
            2, 4, // 2 tiles right of vein_miner
            2
        ));
        tree.addConnection("vein_miner", "vein_miner_path_right");

        // =====================================================================
        // MYSTERY PATH NODES - Above and below the Mining Fortune node
        // =====================================================================
        
        // Mystery Path Node above Mining Fortune (2 tiles away)
        tree.addNode(new SkillTreeNode(
            "mining_fortune_path_upper",
            "Mining Fortune Mystery Path",
            "A mysterious path that may unlock greater fortune secrets",
            Material.IRON_PICKAXE,
            ChatColor.GRAY,
            -2, -2, // 2 tiles above mining_fortune
            2
        ));
        tree.addConnection("mining_fortune", "mining_fortune_path_upper");
        
        // Mystery Path Node below Mining Fortune (2 tiles away)
        tree.addNode(new SkillTreeNode(
            "mining_fortune_path_lower",
            "Mining Fortune Mystery Path",
            "A mysterious path that may unlock greater fortune secrets",
            Material.NETHERITE_PICKAXE,
            ChatColor.DARK_GRAY,
            -2, 2, // 2 tiles below mining_fortune
            2
        ));
        tree.addConnection("mining_fortune", "mining_fortune_path_lower");

        // =====================================================================
        // MYSTERY PATH NODES - Above and below the XP Buff node
        // =====================================================================
        
        // Mystery Path Node above XP Buff (2 tiles away)
        tree.addNode(new SkillTreeNode(
            "ore_extraction_path_upper",
            "Extraction Expertise Mystery Path",
            "A mysterious path that may unlock greater extraction secrets",
            Material.BOOK,
            ChatColor.YELLOW,
            2, -2, // 2 tiles above ore_extraction_xp
            2
        ));
        tree.addConnection("ore_extraction_xp", "ore_extraction_path_upper");
        
        // Mystery Path Node below XP Buff (2 tiles away)
        tree.addNode(new SkillTreeNode(
            "ore_extraction_path_lower",
            "Extraction Expertise Mystery Path",
            "A mysterious path that may unlock greater extraction secrets",
            Material.CLOCK,
            ChatColor.GOLD,
            2, 2, // 2 tiles below ore_extraction_xp
            2
        ));
        tree.addConnection("ore_extraction_xp", "ore_extraction_path_lower");

        // =====================================================================
        // ORE UNLOCK NODES - Connected to the Upper Extraction Path
        // =====================================================================
        
        // Iron Ore Unlock Node
        tree.addNode(new SkillTreeNode(
            "unlock_iron_ore",
            "Iron Mining",
            "Unlocks the ability to mine iron ore",
            Material.IRON_ORE,
            ChatColor.WHITE,
            2, -4, // 2 tiles above ore_extraction_path_upper
            3 // Token cost
        ));
        tree.addConnection("ore_extraction_path_upper", "unlock_iron_ore");

        tree.addNode(new SkillTreeNode(
            "unlock_deepslate_mining",
            "Deepslate Mining",
            "Unlocks the ability to mine deepslate variants of ores,\nwhich provide 2x more XP than regular variants",
            Material.DEEPSLATE,
            ChatColor.DARK_GRAY,
            4, -2, // 2 tiles to the right of ore_extraction_path_upper
            4 // Token cost
        ));
        tree.addConnection("ore_extraction_path_upper", "unlock_deepslate_mining");

        // =====================================================================
        // MINING XP NODE - Connected to Extraction Expertise Mystery Path
        // =====================================================================

        // Create upgradable node for Mining XP with custom descriptions and costs
        Map<Integer, String> miningXpDescriptions = new HashMap<>();
        Map<Integer, Integer> miningXpCosts = new HashMap<>();

        // Set descriptions for all 100 levels - Each level gives a fixed amount of Mining XP
        for (int i = 1; i <= 100; i++) {
            int xpAmount = i * 100; // 100 XP per level (scales with level)
            miningXpDescriptions.put(i, "Grants " + xpAmount + " Mining Skill XP directly\n" + 
                                    ChatColor.YELLOW + "Accumulated XP: " + (i * 100) + "\n" + 
                                    ChatColor.GOLD + "This is a permanent upgrade\n" + 
                                    ChatColor.RED + "No token refund on skill tree reset");
        }

        // Set increasing costs for all 100 levels
        // Formula: cost increases by 1 every 5 levels, starting at 2
        for (int i = 1; i <= 100; i++) {
            int baseCost = 2 + ((i - 1) / 5);
            miningXpCosts.put(i, baseCost);
        }

        // Mining XP Node (Upgradable up to 100 levels)
        // This special node won't refund tokens on reset and will keep its progress
        SkillTreeNode miningXpNode = new SkillTreeNode(
            "mining_xp_boost",
            "Mining Knowledge",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.LIGHT_PURPLE,
            4, 2, // Better position that doesn't overlap
            miningXpDescriptions,
            miningXpCosts
        );

        // IMPORTANT: Mark this node as special to preserve progress during resets
        miningXpNode.setSpecialNode(true);

        // Add the node to the tree
        tree.addNode(miningXpNode);
        tree.addConnection("ore_extraction_path_lower", "mining_xp_boost");

        // =====================================================================
        // ORE CONDUIT NODE - Below vein_miner_path_bottom
        // =====================================================================
        
        // Create upgradable node for Ore Conduit with custom descriptions and costs
        Map<Integer, String> oreConduitDescriptions = new HashMap<>();
        Map<Integer, Integer> oreConduitCosts = new HashMap<>();
        
        // Set descriptions for all 100 levels - Each level gives +0.5% XP split to Mining skill
        for (int i = 1; i <= 100; i++) {
            double splitPercentage = i * 0.5; // 0.5% per level, up to 50% at level 100
            oreConduitDescriptions.put(i, "Split " + splitPercentage + "% of OreExtraction XP to Mining skill");
        }
        
        // Set increasing costs for all 100 levels
        // Cost formula: 1 + (level / 10) rounded down
        for (int i = 1; i <= 100; i++) {
            int baseCost = 1 + (i / 10);
            oreConduitCosts.put(i, baseCost);
        }
        
        // Ore Conduit Node (Upgradable up to 100 levels)
        tree.addNode(new SkillTreeNode(
            "ore_conduit",
            "Ore Conduit",
            Material.CONDUIT,
            ChatColor.AQUA,
            0, 8, // 2 tiles below vein_miner_path_bottom
            oreConduitDescriptions,
            oreConduitCosts
        ));
        tree.addConnection("vein_miner_path_bottom", "ore_conduit");
        }
}