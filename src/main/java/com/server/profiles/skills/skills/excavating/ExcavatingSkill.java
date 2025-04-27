package com.server.profiles.skills.skills.excavating;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.SkillType;

/**
 * The Excavating skill - focused on digging soil and finding treasures
 */
public class ExcavatingSkill extends AbstractSkill {
    
    public ExcavatingSkill() {
        super(SkillType.EXCAVATING.getId(), SkillType.EXCAVATING.getDisplayName(), 
              SkillType.EXCAVATING.getDescription(), 50);
        
        // Initialize subskills and rewards (will be added later)
    }
}