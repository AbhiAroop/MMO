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
        veinMinerCosts.put(1, 1);
        veinMinerCosts.put(2, 2);
        veinMinerCosts.put(3, 3);
        veinMinerCosts.put(4, 4);
        veinMinerCosts.put(5, 5);
        veinMinerCosts.put(6, 6);
        veinMinerCosts.put(7, 7);
        veinMinerCosts.put(8, 8);
        veinMinerCosts.put(9, 9);
        
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
    }
}