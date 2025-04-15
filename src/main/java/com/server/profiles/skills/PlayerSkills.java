package com.server.profiles.skills;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkills {
    private Map<SkillType, Integer> skillLevels;

    public PlayerSkills() {
        skillLevels = new HashMap<>();
        for (SkillType skill : SkillType.values()) {
            skillLevels.put(skill, 1);
        }
    }

    public int getSkillLevel(SkillType skill) {
        return skillLevels.getOrDefault(skill, 1);
    }

    public void setSkillLevel(SkillType skill, int level) {
        skillLevels.put(skill, Math.max(1, Math.min(100, level)));
    }
}