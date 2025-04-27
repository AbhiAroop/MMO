package com.server.profiles.skills.skills.combat;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.SkillType;

/**
 * The Combat skill - focused on fighting enemies and monsters
 */
public class CombatSkill extends AbstractSkill {
    
    public CombatSkill() {
        super(SkillType.COMBAT.getId(), SkillType.COMBAT.getDisplayName(), 
              SkillType.COMBAT.getDescription(), 50);
        
        // Initialize subskills and rewards (will be added later)
    }
}