package com.server.profiles.skills.skills.farming;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.SkillType;

/**
 * The Farming skill - focused on growing crops and raising animals
 */
public class FarmingSkill extends AbstractSkill {
    
    public FarmingSkill() {
        super(SkillType.FARMING.getId(), SkillType.FARMING.getDisplayName(), 
              SkillType.FARMING.getDescription(), 50);
        
        // Initialize subskills and rewards (will be added later)
    }
}