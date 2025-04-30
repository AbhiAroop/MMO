package com.server.profiles.skills.trees.builders;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;

/**
 * Generic builder for skill trees that don't have a specific implementation
 */
public class GenericTreeBuilder implements SkillTreeBuilder {

    @Override
    public void buildSkillTree(SkillTree tree) {
        // Simple tree with one branch in each direction
        
        // North branch
        tree.addNode(new SkillTreeNode(
            "north_1",
            "Skill Mastery I",
            "First step in mastering this skill.",
            Material.BOOK,
            ChatColor.YELLOW,
            0, -1, // Above root
            1
        ));
        tree.addConnection("root", "north_1");
        
        tree.addNode(new SkillTreeNode(
            "north_2",
            "Skill Mastery II",
            "Further develop your skill mastery.",
            Material.ENCHANTED_BOOK,
            ChatColor.YELLOW,
            0, -2, // Above north_1
            2
        ));
        tree.addConnection("north_1", "north_2");
        
        // East branch
        tree.addNode(new SkillTreeNode(
            "east_1",
            "Skill Efficiency I",
            "Improve your efficiency with this skill.",
            Material.CLOCK,
            ChatColor.GOLD,
            1, 0, // Right of root
            1
        ));
        tree.addConnection("root", "east_1");
        
        tree.addNode(new SkillTreeNode(
            "east_2",
            "Skill Efficiency II",
            "Further improve your efficiency.",
            Material.COMPASS,
            ChatColor.GOLD,
            2, 0, // Right of east_1
            2
        ));
        tree.addConnection("east_1", "east_2");
        
        // South branch
        tree.addNode(new SkillTreeNode(
            "south_1",
            "Skill Fortune I",
            "Increase your fortune with this skill.",
            Material.GOLD_INGOT,
            ChatColor.RED,
            0, 1, // Below root
            1
        ));
        tree.addConnection("root", "south_1");
        
        tree.addNode(new SkillTreeNode(
            "south_2",
            "Skill Fortune II",
            "Further increase your fortune.",
            Material.GOLD_BLOCK,
            ChatColor.RED,
            0, 2, // Below south_1
            2
        ));
        tree.addConnection("south_1", "south_2");
        
        // West branch
        tree.addNode(new SkillTreeNode(
            "west_1",
            "Skill Precision I",
            "Improve your precision with this skill.",
            Material.TARGET,
            ChatColor.AQUA,
            -1, 0, // Left of root
            1
        ));
        tree.addConnection("root", "west_1");
        
        tree.addNode(new SkillTreeNode(
            "west_2",
            "Skill Precision II",
            "Further improve your precision.",
            Material.SPECTRAL_ARROW,
            ChatColor.AQUA,
            -2, 0, // Left of west_1
            2
        ));
        tree.addConnection("west_1", "west_2");
    }
}