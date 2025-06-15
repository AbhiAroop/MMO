package com.server.enchanting;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchanting.CustomEnchantment.EnchantmentCategory;

/**
 * Represents rune books that can be placed in chiseled bookshelves for enhanced enchanting
 */
public class RuneBook {
    
    private final String id;
    private final String displayName;
    private final RuneTier tier;
    private final EnchantmentCategory specialization;
    private final int enchantingPower;
    private final double categoryBonus;
    private final String description;
    
    public enum RuneTier {
        NOVICE("Novice", ChatColor.WHITE, 1, 1.0, "Basic magical knowledge"),
        APPRENTICE("Apprentice", ChatColor.GREEN, 3, 1.2, "Intermediate magical understanding"),
        ADEPT("Adept", ChatColor.BLUE, 5, 1.5, "Advanced magical theories"),
        EXPERT("Expert", ChatColor.DARK_PURPLE, 8, 2.0, "Master-level magical expertise"),
        LEGENDARY("Legendary", ChatColor.GOLD, 12, 3.0, "Legendary magical wisdom"),
        MYTHIC("Mythic", ChatColor.RED, 20, 5.0, "Transcendent magical power");
        
        private final String displayName;
        private final ChatColor color;
        private final int basePower;
        private final double multiplier;
        private final String description;
        
        RuneTier(String displayName, ChatColor color, int basePower, double multiplier, String description) {
            this.displayName = displayName;
            this.color = color;
            this.basePower = basePower;
            this.multiplier = multiplier;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public int getBasePower() { return basePower; }
        public double getMultiplier() { return multiplier; }
        public String getDescription() { return description; }
        public String getFormattedName() { return color + displayName; }
    }
    
    public RuneBook(String id, String displayName, RuneTier tier, EnchantmentCategory specialization,
                   int enchantingPower, double categoryBonus, String description) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.specialization = specialization;
        this.enchantingPower = enchantingPower;
        this.categoryBonus = categoryBonus;
        this.description = description;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public RuneTier getTier() { return tier; }
    public EnchantmentCategory getSpecialization() { return specialization; }
    public int getEnchantingPower() { return enchantingPower; }
    public double getCategoryBonus() { return categoryBonus; }
    public String getDescription() { return description; }
    
    /**
     * Create the physical rune book item
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(tier.getColor() + displayName);
        
        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Tier: " + tier.getFormattedName(),
            ChatColor.GRAY + "Specialization: " + specialization.getColor() + specialization.getDisplayName(),
            "",
            ChatColor.AQUA + "Enchanting Properties:",
            ChatColor.WHITE + "• " + ChatColor.YELLOW + "+" + enchantingPower + " Enchanting Power",
            specialization != null ? 
                ChatColor.WHITE + "• " + ChatColor.GREEN + "+" + String.format("%.0f", categoryBonus * 100) + "% " + 
                specialization.getDisplayName() + " Success Rate" : "",
            "",
            ChatColor.GRAY + description,
            "",
            ChatColor.GOLD + "Place in Chiseled Bookshelf near",
            ChatColor.GOLD + "Enchanting Table to unlock higher",
            ChatColor.GOLD + "level enchantments (Level 31-1000)"
        );
        
        meta.setLore(lore);
        
        // Add custom model data to distinguish rune books
        meta.setCustomModelData(400000 + tier.ordinal() * 1000 + specialization.ordinal());
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Check if an item is a rune book
     */
    public static boolean isRuneBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return false;
        }
        
        int modelData = item.getItemMeta().getCustomModelData();
        return modelData >= 400000 && modelData < 500000;
    }
    
    /**
     * Get rune book data from an item
     */
    public static RuneBook fromItem(ItemStack item) {
        if (!isRuneBook(item)) {
            return null;
        }
        
        int modelData = item.getItemMeta().getCustomModelData();
        int tierIndex = (modelData - 400000) / 1000;
        int categoryIndex = (modelData - 400000) % 1000;
        
        if (tierIndex >= 0 && tierIndex < RuneTier.values().length &&
            categoryIndex >= 0 && categoryIndex < EnchantmentCategory.values().length) {
            
            RuneTier tier = RuneTier.values()[tierIndex];
            EnchantmentCategory category = EnchantmentCategory.values()[categoryIndex];
            
            return RuneBookRegistry.getInstance().getRuneBook(tier, category);
        }
        
        return null;
    }
    
    /**
     * Calculate total enchanting power considering tier and specialization
     */
    public int getTotalEnchantingPower(EnchantmentCategory targetCategory) {
        int basePower = enchantingPower;
        
        // Apply specialization bonus
        if (specialization == targetCategory) {
            basePower = (int) (basePower * (1.0 + categoryBonus));
        }
        
        return basePower;
    }
}