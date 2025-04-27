package com.server.profiles.skills.skills.fishing;

import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.SkillType;

/**
 * The Fishing skill - focused on catching fish and aquatic treasures
 */
public class FishingSkill extends AbstractSkill {
    
    public FishingSkill() {
        super(SkillType.FISHING.getId(), SkillType.FISHING.getDisplayName(), 
              SkillType.FISHING.getDescription(), 50);
        
        // Initialize subskills and rewards (will be added later)
    }
}