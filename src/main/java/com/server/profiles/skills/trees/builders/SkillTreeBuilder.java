package com.server.profiles.skills.trees.builders;

import com.server.profiles.skills.trees.SkillTree;

/**
 * Interface for skill tree builders
 * Each skill should have its own implementation to set up its skill tree
 */
public interface SkillTreeBuilder {
    
    /**
     * Build a skill tree by adding all nodes and connections
     * 
     * @param tree The skill tree to build
     */
    void buildSkillTree(SkillTree tree);
}