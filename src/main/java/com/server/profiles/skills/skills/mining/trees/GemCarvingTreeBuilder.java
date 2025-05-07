package com.server.profiles.skills.skills.mining.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Gem Carving skill tree
 */
public class GemCarvingTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // =====================================================================
        // GEMCARVING XP BOOST NODE - To the right of root (highly upgradeable)
        // =====================================================================
        
        // Create descriptions and costs for the XP boost node
        Map<Integer, String> xpBoostDescriptions = new HashMap<>();
        Map<Integer, Integer> xpBoostCosts = new HashMap<>();
        
        // Set descriptions for all 1000 levels - Each level gives +1 GemCarving XP per extraction
        for (int i = 1; i <= 1000; i++) {
            xpBoostDescriptions.put(i, "Gain +" + i + " GemCarving XP per successful gem extraction");
        }
        
        // Set increasing costs for all 1000 levels
        // Formula: base cost increases by 1 every 10 levels
        for (int i = 1; i <= 1000; i++) {
            int baseCost = 1 + ((i - 1) / 10);
            xpBoostCosts.put(i, baseCost);
        }
        
        // GemCarving XP Boost Node (Upgradable up to 1000 levels)
        SkillTreeNode xpBoostNode = new SkillTreeNode(
            "gemcarving_xp_boost",
            "Carver's Expertise",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            2, 0, // 2 tiles right of root
            xpBoostDescriptions,
            xpBoostCosts
        );
        
        // Mark this as a special node to preserve progress through resets
        xpBoostNode.setSpecialNode(true);
        tree.addNode(xpBoostNode);
        tree.addConnection("root", "gemcarving_xp_boost");
        
        // Path node to the right of XP boost (for visual spacing)
        tree.addNode(new SkillTreeNode(
            "gemcarving_xp_path",
            "Carving Mastery",
            "A path to greater gem carving knowledge",
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE,
            3, 0, // 1 tile right of XP boost
            3 // Token cost
        ));
        tree.addConnection("gemcarving_xp_boost", "gemcarving_xp_path");
        
        // =====================================================================
        // MINING FORTUNE NODE - To the left of root (upgradeable)
        // =====================================================================
        
        // Create descriptions and costs for the Mining Fortune node
        Map<Integer, String> fortuneDescriptions = new HashMap<>();
        Map<Integer, Integer> fortuneCosts = new HashMap<>();
        
        // Set descriptions for all 100 levels - Each level gives +0.5 mining fortune
        for (int i = 1; i <= 100; i++) {
            fortuneDescriptions.put(i, "Gain +" + (i * 0.5) + " Mining Fortune");
        }
        
        // Set increasing costs for all 100 levels
        // Formula: cost increases by 1 every 5 levels
        for (int i = 1; i <= 100; i++) {
            int baseCost = 1 + ((i - 1) / 5);
            fortuneCosts.put(i, baseCost);
        }
        
        // Mining Fortune Node (Upgradable up to 100 levels)
        SkillTreeNode fortuneNode = new SkillTreeNode(
            "gem_mining_fortune",
            "Gem Fortune",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -2, 0, // 2 tiles left of root
            fortuneDescriptions,
            fortuneCosts
        );
        
        // Mark this as a special node to preserve progress through resets
        fortuneNode.setSpecialNode(true);
        tree.addNode(fortuneNode);
        tree.addConnection("root", "gem_mining_fortune");
        
        // Path node to the left of Mining Fortune (for visual spacing)
        tree.addNode(new SkillTreeNode(
            "fortune_path",
            "Fortune Mastery",
            "A path to greater fortune-finding abilities",
            Material.EMERALD,
            ChatColor.GREEN,
            -3, 0, // 1 tile left of Mining Fortune
            3 // Token cost
        ));
        tree.addConnection("gem_mining_fortune", "fortune_path");
        
        // =====================================================================
        // ROOT NODE CONNECTIONS
        // =====================================================================
        
        // Add some visual nodes around the root for additional paths later
        
        // Path node above root
        tree.addNode(new SkillTreeNode(
            "gem_upper_path",
            "Crystal Knowledge",
            "Understanding the crystalline structures",
            Material.AMETHYST_CLUSTER,
            ChatColor.DARK_PURPLE,
            0, -2, // 2 tiles above root
            3 // Token cost
        ));
        tree.addConnection("root", "gem_upper_path");
        
        // Path node below root
        tree.addNode(new SkillTreeNode(
            "gem_lower_path",
            "Gemstone Mastery",
            "Deeper understanding of precious stones",
            Material.LAPIS_BLOCK,
            ChatColor.BLUE,
            0, 2, // 2 tiles below root
            3 // Token cost
        ));
        tree.addConnection("root", "gem_lower_path");
    }
}