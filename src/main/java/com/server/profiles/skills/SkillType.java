package com.server.profiles.skills;

public enum SkillType {
    MINING("Mining"),
    WOODCUTTING("Woodcutting"),
    FISHING("Fishing"),
    COMBAT("Combat"),
    CRAFTING("Crafting");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}