package com.server.profiles.skills.skills.mining.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Mining skill tree - Clean, focused progression
 */
public class MiningTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // =====================================================================
        // MINING FORTUNE NODE (LEFT SIDE)
        // =====================================================================
        
        // Create descriptions and costs for Mining Fortune upgrades
        Map<Integer, String> fortuneDescriptions = new HashMap<>();
        Map<Integer, Integer> fortuneCosts = new HashMap<>();
        
        for (int i = 1; i <= 50; i++) {
            double fortuneValue = i * 0.5;
            fortuneDescriptions.put(i, "Increases your mining fortune by " + fortuneValue + "\n" +
                                   "Better drop rates from all mining activities\n" +
                                   "Current Level: " + i + "/50");
            
            // Cost increases with level (1 token for levels 1-10, 2 for 11-25, 3 for 26-50)
            if (i <= 10) {
                fortuneCosts.put(i, 1);
            } else if (i <= 25) {
                fortuneCosts.put(i, 2);
            } else {
                fortuneCosts.put(i, 3);
            }
        }
        
        SkillTreeNode miningFortuneNode = new SkillTreeNode(
            "mining_fortune",
            "Mining Fortune",
            Material.GOLDEN_PICKAXE,
            ChatColor.GOLD,
            -2, 0, // Left side of root with 1 slot gap
            fortuneDescriptions,
            fortuneCosts,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(miningFortuneNode);
        tree.addConnection("root", "mining_fortune");
        
        // =====================================================================
        // MINING SPEED NODE (RIGHT SIDE)
        // =====================================================================
        
        // Create descriptions and costs for Mining Speed upgrades
        Map<Integer, String> speedDescriptions = new HashMap<>();
        Map<Integer, Integer> speedCosts = new HashMap<>();
        
        for (int i = 1; i <= 50; i++) {
            double speedValue = i * 0.01;
            double percentage = speedValue * 100;
            speedDescriptions.put(i, "Increases your mining speed by " + 0.01 + "\n" +
                                 "Mine blocks faster and more efficiently\n" +
                                 "Current Level: " + i + "/50");
            
            // Cost increases with level (1 token for levels 1-10, 2 for 11-25, 3 for 26-50)
            if (i <= 10) {
                speedCosts.put(i, 1);
            } else if (i <= 25) {
                speedCosts.put(i, 2);
            } else {
                speedCosts.put(i, 3);
            }
        }
        
        SkillTreeNode miningSpeedNode = new SkillTreeNode(
            "mining_speed",
            "Mining Speed",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            2, 0, // Right side of root with 1 slot gap
            speedDescriptions,
            speedCosts,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(miningSpeedNode);
        tree.addConnection("root", "mining_speed");
    }
}