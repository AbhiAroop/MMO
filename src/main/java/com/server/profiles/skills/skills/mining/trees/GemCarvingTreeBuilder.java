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
        // Root node (automatically added by skill tree creation)
        // This is at position (0,0)
        
        // =====================================================================
        // MINING FORTUNE NODE - To the left of root (direct connection)
        // =====================================================================
        
        // Create upgradable node for Fortune with custom descriptions and costs
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
        
        // Mining Fortune Node (Upgradable up to 100 levels) - directly connected to root
        SkillTreeNode fortuneNode = new SkillTreeNode(
            "gem_mining_fortune",
            "Gem Fortune",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -2, 0, // 2 tiles left of root (direct connection)
            fortuneDescriptions,
            fortuneCosts
        );
        
        // Add to tree with direct connection
        tree.addNode(fortuneNode);
        tree.addConnection("root", "gem_mining_fortune");
        
        // =====================================================================
        // GEMCARVING XP BOOST NODE - To the right of root (direct connection)
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
        
        // GemCarving XP Boost Node (Upgradable up to 1000 levels) - directly connected to root
        SkillTreeNode xpBoostNode = new SkillTreeNode(
            "gemcarving_xp_boost",
            "Carver's Expertise",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN,
            2, 0, // 2 tiles right of root (direct connection)
            xpBoostDescriptions,
            xpBoostCosts
        );
        
        // Add to tree with direct connection
        tree.addNode(xpBoostNode);
        tree.addConnection("root", "gemcarving_xp_boost");
        
        // =====================================================================
        // BASIC CRYSTALS NODE - Above the XP boost node
        // =====================================================================
        
        // Create a node that unlocks Azuralite crystals
        tree.addNode(new SkillTreeNode(
            "basic_crystals",
            "Basic Crystals",
            "Unlocks the ability to carve Azuralite crystals",
            Material.LAPIS_LAZULI, // Azuralite is represented by lapis in the minigame
            ChatColor.BLUE,
            2, -2, // 2 tiles up from the XP boost node (same x coordinate)
            5 // Token cost
        ));
        
        // Connect to the XP boost node
        tree.addConnection("gemcarving_xp_boost", "basic_crystals");
    }
}