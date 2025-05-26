package com.server.profiles.skills.skills.mining.trees;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Mining skill tree - now includes all mining-related progression
 */
public class MiningTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // =====================================================================
        // CORE MINING PROGRESSION BRANCHES
        // =====================================================================
        
        // Left branch - Mining Speed and Efficiency
        tree.addNode(new SkillTreeNode(
            "mining_speed_1",
            "Mining Speed I",
            "Mine blocks 10% faster.",
            Material.IRON_PICKAXE,
            ChatColor.AQUA,
            -2, 0,
            1
        ));
        tree.addConnection("root", "mining_speed_1");
        
        tree.addNode(new SkillTreeNode(
            "mining_speed_2",
            "Mining Speed II",
            "Mine blocks 20% faster.",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -4, 0,
            2
        ));
        tree.addConnection("mining_speed_1", "mining_speed_2");
        
        // Right branch - Mining Fortune
        tree.addNode(new SkillTreeNode(
            "mining_fortune_1",
            "Mining Fortune I",
            "Increases block drops by 0.5.",
            Material.GOLDEN_PICKAXE,
            ChatColor.GOLD,
            2, 0,
            1
        ));
        tree.addConnection("root", "mining_fortune_1");
        
        tree.addNode(new SkillTreeNode(
            "mining_fortune_2",
            "Mining Fortune II", 
            "Increases block drops by 1.0.",
            Material.NETHERITE_PICKAXE,
            ChatColor.GOLD,
            4, 0,
            2
        ));
        tree.addConnection("mining_fortune_1", "mining_fortune_2");
        
        // =====================================================================
        // ORE EXTRACTION SUBSKILL PROGRESSION
        // =====================================================================
        
        // Ore Extraction path (upper branch)
        tree.addNode(new SkillTreeNode(
            "ore_extraction_mastery",
            "Ore Extraction Mastery",
            "Unlocks advanced ore extraction techniques",
            Material.RAW_IRON,
            ChatColor.YELLOW,
            0, -2,
            2
        ));
        tree.addConnection("root", "ore_extraction_mastery");
        
        // Iron Ore Unlock
        tree.addNode(new SkillTreeNode(
            "unlock_iron_ore",
            "Iron Mining",
            "Unlocks the ability to mine iron ore",
            Material.IRON_ORE,
            ChatColor.WHITE,
            -2, -4,
            3
        ));
        tree.addConnection("ore_extraction_mastery", "unlock_iron_ore");
        
        // Deepslate Coal Mining
        tree.addNode(new SkillTreeNode(
            "unlock_deepslate_mining",
            "Deepslate Coal Mining",
            "Unlocks deepslate coal ore mining.\nProvides 2x more XP than regular coal ore",
            Material.DEEPSLATE_COAL_ORE,
            ChatColor.DARK_GRAY,
            2, -4,
            4
        ));
        tree.addConnection("ore_extraction_mastery", "unlock_deepslate_mining");
        
        // Mining XP Boost Node (upgradable, special)
        Map<Integer, String> miningXpDescriptions = new HashMap<>();
        Map<Integer, Integer> miningXpCosts = new HashMap<>();
        
        for (int i = 1; i <= 100; i++) {
            int xpBonus = i * 10; // 10 XP per level
            miningXpDescriptions.put(i, "Grants +" + xpBonus + " bonus Mining XP per block mined\n" +
                                       "Current Level: " + i + "/100");
            miningXpCosts.put(i, 1); // 1 token per level
        }
        
        SkillTreeNode miningXpNode = new SkillTreeNode(
            "mining_xp_boost",
            "Mining Knowledge",
            Material.EXPERIENCE_BOTTLE,
            ChatColor.LIGHT_PURPLE,
            0, -6,
            miningXpDescriptions,
            miningXpCosts
        );
        miningXpNode.setSpecialNode(true);
        tree.addNode(miningXpNode);
        tree.addConnection("ore_extraction_mastery", "mining_xp_boost");
        
        // =====================================================================
        // GEM CARVING SUBSKILL PROGRESSION  
        // =====================================================================
        
        // Gem Carving path (lower branch)
        tree.addNode(new SkillTreeNode(
            "gem_carving_mastery",
            "Gem Carving Mastery", 
            "Unlocks advanced gem carving techniques",
            Material.EMERALD,
            ChatColor.GREEN,
            0, 2,
            2
        ));
        tree.addConnection("root", "gem_carving_mastery");
        
        // Gem Mining Fortune
        tree.addNode(new SkillTreeNode(
            "gem_mining_fortune",
            "Gem Mining Fortune",
            "Increases mining fortune by 0.5 per level.\nSpecifically benefits gem-related drops.",
            Material.DIAMOND,
            ChatColor.AQUA,
            -2, 4,
            1,
            10 // Max 10 levels
        ));
        tree.addConnection("gem_carving_mastery", "gem_mining_fortune");
        
        // Advanced Gem Cutting
        tree.addNode(new SkillTreeNode(
            "advanced_gem_cutting",
            "Advanced Gem Cutting",
            "Improves gem carving success rates and quality",
            Material.EMERALD_BLOCK,
            ChatColor.GREEN,
            2, 4,
            3
        ));
        tree.addConnection("gem_carving_mastery", "advanced_gem_cutting");
        
        // =====================================================================
        // ENDURANCE AND UTILITY NODES
        // =====================================================================
        
        // Stamina reduction (bottom branch)
        tree.addNode(new SkillTreeNode(
            "miners_endurance",
            "Miner's Endurance",
            "Mining consumes 15% less hunger",
            Material.BREAD,
            ChatColor.RED,
            -1, 2,
            1
        ));
        tree.addConnection("root", "miners_endurance");
        
        tree.addNode(new SkillTreeNode(
            "supreme_endurance",
            "Supreme Endurance",
            "Mining consumes 30% less hunger total",
            Material.GOLDEN_APPLE,
            ChatColor.RED,
            -2, 4,
            2
        ));
        tree.addConnection("miners_endurance", "supreme_endurance");
        
        // Luck branch (far right)
        tree.addNode(new SkillTreeNode(
            "miners_luck",
            "Miner's Luck",
            "Increases overall luck while mining",
            Material.RABBIT_FOOT,
            ChatColor.YELLOW,
            1, 2,
            2
        ));
        tree.addConnection("root", "miners_luck");
        
        tree.addNode(new SkillTreeNode(
            "ancient_knowledge",
            "Ancient Knowledge",
            "Unlocks rare mining discoveries and bonuses",
            Material.KNOWLEDGE_BOOK,
            ChatColor.LIGHT_PURPLE,
            2, 4,
            3
        ));
        tree.addConnection("miners_luck", "ancient_knowledge");
    }
}