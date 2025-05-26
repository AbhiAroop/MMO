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
 * Builder for the Mining skill tree - now includes all mining-related progression
 */
public class MiningTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // =====================================================================
        // CORE MINING PROGRESSION BRANCHES (BASIC TIER)
        // =====================================================================
        
        // Left branch - Mining Speed and Efficiency (Basic Tier)
        tree.addNode(new SkillTreeNode(
            "mining_speed_1",
            "Mining Speed I",
            "Mine blocks 10% faster.",
            Material.IRON_PICKAXE,
            ChatColor.AQUA,
            -2, 0,
            1, // Cost: 1 token
            SkillToken.TokenTier.BASIC // Requires Basic tier tokens
        ));
        tree.addConnection("root", "mining_speed_1");
        
        tree.addNode(new SkillTreeNode(
            "mining_speed_2",
            "Mining Speed II",
            "Mine blocks 20% faster.",
            Material.DIAMOND_PICKAXE,
            ChatColor.AQUA,
            -4, 0,
            2, // Cost: 2 tokens
            SkillToken.TokenTier.BASIC
        ));
        tree.addConnection("mining_speed_1", "mining_speed_2");
        
        // Right branch - Mining Fortune (Basic Tier)
        tree.addNode(new SkillTreeNode(
            "mining_fortune_1",
            "Mining Fortune I",
            "Increases block drops by 0.5.",
            Material.GOLDEN_PICKAXE,
            ChatColor.GOLD,
            2, 0,
            1,
            SkillToken.TokenTier.BASIC
        ));
        tree.addConnection("root", "mining_fortune_1");
        
        tree.addNode(new SkillTreeNode(
            "mining_fortune_2",
            "Mining Fortune II", 
            "Increases block drops by 1.0.",
            Material.NETHERITE_PICKAXE,
            ChatColor.GOLD,
            4, 0,
            3, // Higher cost for better effect
            SkillToken.TokenTier.ADVANCED // Requires Advanced tier tokens
        ));
        tree.addConnection("mining_fortune_1", "mining_fortune_2");
        
        // =====================================================================
        // ORE EXTRACTION SUBSKILL PROGRESSION (ADVANCED TIER)
        // =====================================================================
        
        // Ore Extraction path (upper branch) - Advanced Tier
        tree.addNode(new SkillTreeNode(
            "ore_extraction_mastery",
            "Ore Extraction Mastery",
            "Unlocks advanced ore extraction techniques",
            Material.RAW_IRON,
            ChatColor.YELLOW,
            0, -2,
            2,
            SkillToken.TokenTier.ADVANCED // Requires Advanced tokens
        ));
        tree.addConnection("root", "ore_extraction_mastery");
        
        // Iron Ore Unlock (Basic Tier)
        tree.addNode(new SkillTreeNode(
            "unlock_iron_ore",
            "Iron Mining",
            "Unlocks the ability to mine iron ore",
            Material.IRON_ORE,
            ChatColor.WHITE,
            -2, -4,
            2,
            SkillToken.TokenTier.BASIC
        ));
        tree.addConnection("ore_extraction_mastery", "unlock_iron_ore");
        
        // Deepslate Mining (Advanced Tier)
        tree.addNode(new SkillTreeNode(
            "unlock_deepslate_mining",
            "Deepslate Mining",
            "Unlocks deepslate ore mining.\nProvides 25% more XP than regular ores",
            Material.DEEPSLATE_COAL_ORE,
            ChatColor.DARK_GRAY,
            2, -4,
            3,
            SkillToken.TokenTier.ADVANCED
        ));
        tree.addConnection("ore_extraction_mastery", "unlock_deepslate_mining");
        
        // =====================================================================
        // GEM CARVING SUBSKILL PROGRESSION (ADVANCED TIER)
        // =====================================================================
        
        // Gem Carving path (lower branch) - Advanced Tier
        tree.addNode(new SkillTreeNode(
            "gem_carving_mastery",
            "Gem Carving Mastery", 
            "Unlocks advanced gem carving techniques",
            Material.EMERALD,
            ChatColor.GREEN,
            0, 2,
            2,
            SkillToken.TokenTier.ADVANCED
        ));
        tree.addConnection("root", "gem_carving_mastery");
        
        // Gem Mining Fortune (Advanced Tier)
        tree.addNode(new SkillTreeNode(
            "gem_mining_fortune",
            "Gem Mining Fortune",
            "Increases mining fortune by 0.5 per level.\nSpecifically benefits gem-related drops.",
            Material.DIAMOND,
            ChatColor.AQUA,
            -2, 4,
            1,
            10, // Max 10 levels
            SkillToken.TokenTier.ADVANCED
        ));
        tree.addConnection("gem_carving_mastery", "gem_mining_fortune");
        
        // Advanced Gem Cutting (Master Tier)
        tree.addNode(new SkillTreeNode(
            "advanced_gem_cutting",
            "Advanced Gem Cutting",
            "Improves gem carving success rates and quality",
            Material.EMERALD_BLOCK,
            ChatColor.GREEN,
            2, 4,
            5, // High cost for master tier
            SkillToken.TokenTier.MASTER // Requires Master tokens
        ));
        tree.addConnection("gem_carving_mastery", "advanced_gem_cutting");
        
        // =====================================================================
        // ENDURANCE AND UTILITY NODES (MIXED TIERS)
        // =====================================================================
        
        // Stamina reduction (Basic Tier)
        tree.addNode(new SkillTreeNode(
            "miners_endurance",
            "Miner's Endurance",
            "Mining consumes 15% less hunger",
            Material.BREAD,
            ChatColor.RED,
            -1, 2,
            1,
            SkillToken.TokenTier.BASIC
        ));
        tree.addConnection("root", "miners_endurance");
        
        // Supreme Endurance (Advanced Tier)
        tree.addNode(new SkillTreeNode(
            "supreme_endurance",
            "Supreme Endurance",
            "Mining consumes 30% less hunger total",
            Material.GOLDEN_APPLE,
            ChatColor.RED,
            -2, 4,
            3,
            SkillToken.TokenTier.ADVANCED
        ));
        tree.addConnection("miners_endurance", "supreme_endurance");
        
        // =====================================================================
        // MASTER TIER NODES (HIGH-END CONTENT)
        // =====================================================================
        
        // Ultimate Mining Mastery (Master Tier)
        tree.addNode(new SkillTreeNode(
            "ultimate_mining_mastery",
            "Ultimate Mining Mastery",
            "Grants 50% chance to not consume tool durability when mining",
            Material.NETHERITE_BLOCK,
            ChatColor.DARK_PURPLE,
            0, -6,
            10, // Very expensive
            SkillToken.TokenTier.MASTER
        ));
        tree.addConnection("ore_extraction_mastery", "ultimate_mining_mastery");
        tree.addConnection("gem_carving_mastery", "ultimate_mining_mastery");
        
        // Ancient Mining Techniques (Master Tier)
        tree.addNode(new SkillTreeNode(
            "ancient_mining_techniques",
            "Ancient Mining Techniques",
            "Unlock ability to mine ancient debris with 2x drop rate",
            Material.ANCIENT_DEBRIS,
            ChatColor.GOLD,
            0, 6,
            15, // Very expensive master node
            SkillToken.TokenTier.MASTER
        ));
        tree.addConnection("ultimate_mining_mastery", "ancient_mining_techniques");
        
        // Mining XP Boost Node (Special, upgradable, Basic Tier)
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
            4, -2,
            miningXpDescriptions,
            miningXpCosts,
            SkillToken.TokenTier.BASIC // Basic tier for accessibility
        );
        miningXpNode.setSpecialNode(true);
        tree.addNode(miningXpNode);
        tree.addConnection("ore_extraction_mastery", "mining_xp_boost");
    }
}