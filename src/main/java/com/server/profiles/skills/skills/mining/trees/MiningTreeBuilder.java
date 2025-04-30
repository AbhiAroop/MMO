package com.server.profiles.skills.skills.mining.trees;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.builders.SkillTreeBuilder;

/**
 * Builder for the Mining skill tree
 */
public class MiningTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
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
}