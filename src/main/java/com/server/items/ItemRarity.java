package com.server.items;

import org.bukkit.ChatColor;

public enum ItemRarity {
    BASIC("Basic", ChatColor.GRAY, "Standard gear found in every corner of the realm"),
    ENHANCED("Enhanced", ChatColor.GREEN, "Slightly upgraded with minor magical traits"),
    RARE("Rare", ChatColor.BLUE, "Forged by master crafters or discovered in distant worlds"),
    ARCANE("Arcane", ChatColor.DARK_PURPLE, "Imbued with ancient magic"),
    CELESTIAL("Celestial", ChatColor.GOLD, "Wielded by starborn warriors or blessed by the gods"),
    ETHEREAL("Ethereal", ChatColor.LIGHT_PURPLE, "Transcends physical form, existing between dimensions"),
    ASCENDANT("Ascendant", ChatColor.RED, "One-of-a-kind relics tied to universal forces");

    private final String displayName;
    private final ChatColor color;
    private final String description;

    ItemRarity(String displayName, ChatColor color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedName() {
        return color + displayName;
    }
}