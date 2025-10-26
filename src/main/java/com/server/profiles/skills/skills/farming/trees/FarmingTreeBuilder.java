package com.server.profiles.skills.skills.farming.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Farming skill tree - Focused on crop progression
 */
public class FarmingTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // =====================================================================
        // FARMING FORTUNE NODE (LEFT SIDE)
        // =====================================================================
        
        // Create descriptions and costs for Farming Fortune upgrades
        Map<Integer, String> fortuneDescriptions = new HashMap<>();
        Map<Integer, Integer> fortuneCosts = new HashMap<>();
        
        for (int i = 1; i <= 50; i++) {
            double fortuneValue = i * 0.5;
            fortuneDescriptions.put(i, "Increases your farming fortune by " + fortuneValue + "\n" +
                                   "Better drop rates from all crops\n" +
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
        
        SkillTreeNode farmingFortuneNode = new SkillTreeNode(
            "farming_fortune",
            "Farming Fortune",
            Material.GOLDEN_HOE,
            ChatColor.GOLD,
            -2, 0, // Left side of root with 1 slot gap
            fortuneDescriptions,
            fortuneCosts,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(farmingFortuneNode);
        tree.addConnection("root", "farming_fortune");
        
        // =====================================================================
        // FARMING XP BOOST NODE (RIGHT SIDE)
        // =====================================================================
        
        // Create descriptions and costs for Farming XP Boost upgrades
        Map<Integer, String> xpBoostDescriptions = new HashMap<>();
        Map<Integer, Integer> xpBoostCosts = new HashMap<>();
        
        for (int i = 1; i <= 50; i++) {
            double xpBoostValue = i * 0.02; // 2% per level
            double percentage = xpBoostValue * 100;
            xpBoostDescriptions.put(i, "Increases farming XP gain by " + String.format("%.0f", percentage) + "%\n" +
                                 "Gain experience faster from all farming activities\n" +
                                 "Current Level: " + i + "/50");
            
            // Cost increases with level (1 token for levels 1-10, 2 for 11-25, 3 for 26-50)
            if (i <= 10) {
                xpBoostCosts.put(i, 1);
            } else if (i <= 25) {
                xpBoostCosts.put(i, 2);
            } else {
                xpBoostCosts.put(i, 3);
            }
        }
        
        SkillTreeNode farmingXpBoostNode = new SkillTreeNode(
            "farming_xp_boost",
            "Farming XP Boost",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.LIGHT_PURPLE,
            2, 0, // Right side of root with 1 slot gap
            xpBoostDescriptions,
            xpBoostCosts,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(farmingXpBoostNode);
        tree.addConnection("root", "farming_xp_boost");

        // =====================================================================
        // CULTIVATING UNLOCK NODE (ABOVE ROOT)
        // =====================================================================
        
        SkillTreeNode cultivatingUnlockNode = new SkillTreeNode(
            "unlock_cultivating",
            "Cultivating",
            "Unlocks the Cultivating subskill\n" +
            "Allows you to plant crops and gain XP from planting\n" +
            "Prerequisite for planting advanced crops",
            Material.FARMLAND,
            ChatColor.GREEN,
            0, -2, // Above the root node
            2, // Costs 2 basic tokens
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(cultivatingUnlockNode);
        tree.addConnection("root", "unlock_cultivating");

        // =====================================================================
        // HARVESTING CROP UNLOCK NODES (LEFT BRANCH - BELOW FORTUNE)
        // =====================================================================
        
        // Carrots - First crop unlock (1 slot gap from fortune)
        SkillTreeNode carrotsNode = new SkillTreeNode(
            "unlock_carrots",
            "Carrot Harvesting",
            "Unlocks the ability to harvest carrots\n" +
            "Enables XP gain from carrot farming\n" +
            "Basic crop for early game",
            Material.CARROT,
            ChatColor.GOLD,
            -2, 2, // 1 slot below fortune node
            1,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(carrotsNode);
        tree.addConnection("farming_fortune", "unlock_carrots");

        // Potatoes
        SkillTreeNode potatoesNode = new SkillTreeNode(
            "unlock_potatoes",
            "Potato Harvesting",
            "Unlocks the ability to harvest potatoes\n" +
            "Enables XP gain from potato farming\n" +
            "Basic crop for early game",
            Material.POTATO,
            ChatColor.GOLD,
            -2, 4,
            1,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(potatoesNode);
        tree.addConnection("unlock_carrots", "unlock_potatoes");

        // Beetroots
        SkillTreeNode beetrootsNode = new SkillTreeNode(
            "unlock_beetroots",
            "Beetroot Harvesting",
            "Unlocks the ability to harvest beetroots\n" +
            "Enables XP gain from beetroot farming\n" +
            "Intermediate crop",
            Material.BEETROOT,
            ChatColor.RED,
            -2, 6,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(beetrootsNode);
        tree.addConnection("unlock_potatoes", "unlock_beetroots");

        // Sweet Berries
        SkillTreeNode berriesNode = new SkillTreeNode(
            "unlock_sweet_berries",
            "Berry Harvesting",
            "Unlocks the ability to harvest sweet berries\n" +
            "Enables XP gain from berry farming\n" +
            "Bush-based crop with unique mechanics",
            Material.SWEET_BERRIES,
            ChatColor.RED,
            -2, 8,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(berriesNode);
        tree.addConnection("unlock_beetroots", "unlock_sweet_berries");

        // =====================================================================
        // HARVESTING ADVANCED CROP UNLOCKS (CENTER - BELOW CULTIVATING)
        // =====================================================================

        // Cocoa Beans
        SkillTreeNode cocoaNode = new SkillTreeNode(
            "unlock_cocoa",
            "Cocoa Harvesting",
            "Unlocks the ability to harvest cocoa beans\n" +
            "Enables XP gain from cocoa farming\n" +
            "Requires jungle wood to farm",
            Material.COCOA_BEANS,
            ChatColor.GOLD,
            0, 4,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(cocoaNode);
        tree.addConnection("unlock_cultivating", "unlock_cocoa");

        // Nether Wart
        SkillTreeNode netherWartNode = new SkillTreeNode(
            "unlock_nether_wart",
            "Nether Wart Harvesting",
            "Unlocks the ability to harvest nether wart\n" +
            "Enables XP gain from nether wart farming\n" +
            "Essential for potion brewing",
            Material.NETHER_WART,
            ChatColor.DARK_RED,
            0, 6,
            3,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(netherWartNode);
        tree.addConnection("unlock_cocoa", "unlock_nether_wart");

        // =====================================================================
        // HARVESTING LARGE CROP UNLOCKS (RIGHT BRANCH - BELOW XP BOOST)
        // =====================================================================

        // Melons
        SkillTreeNode melonsNode = new SkillTreeNode(
            "unlock_melons",
            "Melon Harvesting",
            "Unlocks the ability to harvest melons\n" +
            "Enables XP gain from melon farming\n" +
            "Large crop with high yield",
            Material.MELON,
            ChatColor.GREEN,
            2, 2,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(melonsNode);
        tree.addConnection("farming_xp_boost", "unlock_melons");

        // Pumpkins
        SkillTreeNode pumpkinsNode = new SkillTreeNode(
            "unlock_pumpkins",
            "Pumpkin Harvesting",
            "Unlocks the ability to harvest pumpkins\n" +
            "Enables XP gain from pumpkin farming\n" +
            "Large crop with multiple uses",
            Material.PUMPKIN,
            ChatColor.GOLD,
            2, 4,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(pumpkinsNode);
        tree.addConnection("unlock_melons", "unlock_pumpkins");
        
        // Connect pumpkins to cocoa to link the right and center branches
        tree.addConnection("unlock_pumpkins", "unlock_cocoa");

        // =====================================================================
        // CULTIVATING PLANTING UNLOCK NODES (UPPER BRANCHES)
        // =====================================================================

        // Carrot Planting (left branch, 1 slot gap above cultivating)
        SkillTreeNode plantCarrotsNode = new SkillTreeNode(
            "unlock_plant_carrots",
            "Carrot Planting",
            "Unlocks the ability to plant carrots\n" +
            "Enables XP gain from carrot planting\n" +
            "Requires carrot harvesting unlock",
            Material.CARROT,
            ChatColor.YELLOW,
            -2, -4,
            1,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantCarrotsNode);
        tree.addConnection("unlock_cultivating", "unlock_plant_carrots");

        // Potato Planting
        SkillTreeNode plantPotatoesNode = new SkillTreeNode(
            "unlock_plant_potatoes",
            "Potato Planting",
            "Unlocks the ability to plant potatoes\n" +
            "Enables XP gain from potato planting\n" +
            "Requires potato harvesting unlock",
            Material.POTATO,
            ChatColor.YELLOW,
            -2, -6,
            1,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantPotatoesNode);
        tree.addConnection("unlock_plant_carrots", "unlock_plant_potatoes");

        // Beetroot Planting
        SkillTreeNode plantBeetrootsNode = new SkillTreeNode(
            "unlock_plant_beetroots",
            "Beetroot Planting",
            "Unlocks the ability to plant beetroots\n" +
            "Enables XP gain from beetroot planting\n" +
            "Requires beetroot harvesting unlock",
            Material.BEETROOT,
            ChatColor.DARK_RED,
            -2, -8,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantBeetrootsNode);
        tree.addConnection("unlock_plant_potatoes", "unlock_plant_beetroots");

        // Sweet Berry Planting
        SkillTreeNode plantBerriesNode = new SkillTreeNode(
            "unlock_plant_sweet_berries",
            "Berry Planting",
            "Unlocks the ability to plant sweet berries\n" +
            "Enables XP gain from berry planting\n" +
            "Requires sweet berry harvesting unlock",
            Material.SWEET_BERRIES,
            ChatColor.RED,
            -2, -10,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantBerriesNode);
        tree.addConnection("unlock_plant_beetroots", "unlock_plant_sweet_berries");

        // Cocoa Planting (center branch)
        SkillTreeNode plantCocoaNode = new SkillTreeNode(
            "unlock_plant_cocoa",
            "Cocoa Planting",
            "Unlocks the ability to plant cocoa beans\n" +
            "Enables XP gain from cocoa planting\n" +
            "Requires cocoa harvesting unlock",
            Material.COCOA_BEANS,
            ChatColor.GOLD,
            0, -6,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantCocoaNode);
        tree.addConnection("unlock_cultivating", "unlock_plant_cocoa");

        // Nether Wart Planting
        SkillTreeNode plantNetherWartNode = new SkillTreeNode(
            "unlock_plant_nether_wart",
            "Nether Wart Planting",
            "Unlocks the ability to plant nether wart\n" +
            "Enables XP gain from nether wart planting\n" +
            "Requires nether wart harvesting unlock",
            Material.NETHER_WART,
            ChatColor.DARK_RED,
            0, -8,
            3,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantNetherWartNode);
        tree.addConnection("unlock_plant_cocoa", "unlock_plant_nether_wart");

        // Melon Planting (right branch)
        SkillTreeNode plantMelonsNode = new SkillTreeNode(
            "unlock_plant_melons",
            "Melon Planting",
            "Unlocks the ability to plant melons\n" +
            "Enables XP gain from melon planting\n" +
            "Requires melon harvesting unlock",
            Material.MELON_SEEDS,
            ChatColor.GREEN,
            2, -4,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantMelonsNode);
        tree.addConnection("unlock_cultivating", "unlock_plant_melons");

        // Pumpkin Planting
        SkillTreeNode plantPumpkinsNode = new SkillTreeNode(
            "unlock_plant_pumpkins",
            "Pumpkin Planting",
            "Unlocks the ability to plant pumpkins\n" +
            "Enables XP gain from pumpkin planting\n" +
            "Requires pumpkin harvesting unlock",
            Material.PUMPKIN_SEEDS,
            ChatColor.GOLD,
            2, -6,
            2,
            SkillToken.TokenTier.BASIC
        );
        tree.addNode(plantPumpkinsNode);
        tree.addConnection("unlock_plant_melons", "unlock_plant_pumpkins");
    }
}
