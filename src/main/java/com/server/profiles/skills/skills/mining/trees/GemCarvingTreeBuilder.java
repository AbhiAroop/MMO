package com.server.profiles.skills.skills.mining.trees;

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
        // Add gem finding chance node
        tree.addNode(new SkillTreeNode(
            "gem_finding",
            "Gem Finder",
            "Increases chance of finding gems by 5%",
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE,
            1, 0, // Position to the right of root
            2 // Token cost
        ));
        
        // Add connection from root to gem finding
        tree.addConnection("root", "gem_finding");
        
        // Add extraction accuracy node
        tree.addNode(new SkillTreeNode(
            "extraction_accuracy",
            "Extraction Accuracy",
            "Increases chance of successful gem extraction by 10%",
            Material.SHEARS,
            ChatColor.YELLOW,
            2, 0, // Position to the right of gem finding
            3 // Token cost
        ));
        
        // Add connection from gem finding to extraction accuracy
        tree.addConnection("gem_finding", "extraction_accuracy");
        
        // Add gem quality node
        tree.addNode(new SkillTreeNode(
            "gem_quality",
            "Gem Quality",
            "Increases quality of found gems",
            Material.DIAMOND,
            ChatColor.AQUA,
            1, 1, // Position below gem finding
            3 // Token cost
        ));
        
        // Add connection from gem finding to gem quality
        tree.addConnection("gem_finding", "gem_quality");
        
        // Add rare gem chance node
        tree.addNode(new SkillTreeNode(
            "rare_gem_chance",
            "Rare Gem Finder",
            "Chance to find rare and valuable gems",
            Material.EMERALD,
            ChatColor.GREEN,
            0, -1, // Position above and to the left of root
            4 // Token cost
        ));
        
        // Add connection from root to rare gem chance
        tree.addConnection("root", "rare_gem_chance");
        
        // Add master carver node
        tree.addNode(new SkillTreeNode(
            "master_carver",
            "Master Carver",
            "Significantly increases all gem carving abilities",
            Material.NETHERITE_PICKAXE,
            ChatColor.GOLD,
            3, 0, // Position to the right of extraction accuracy
            5 // Token cost
        ));
        
        // Add connection from extraction accuracy to master carver
        tree.addConnection("extraction_accuracy", "master_carver");
    }
}