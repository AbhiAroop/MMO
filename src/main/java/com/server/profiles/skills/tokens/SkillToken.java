package com.server.profiles.skills.tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.core.SubskillType;

/**
 * Represents a token that can be used to unlock nodes in a skill tree
 * Now supports 3 tiers: Basic, Advanced, and Master
 */
public class SkillToken {
    // Token tiers
    public enum TokenTier {
        BASIC(1, "Basic", ChatColor.GREEN, "◆"),
        ADVANCED(2, "Advanced", ChatColor.BLUE, "♦"),
        MASTER(3, "Master", ChatColor.LIGHT_PURPLE, "❖");
        
        private final int level;
        private final String displayName;
        private final ChatColor color;
        private final String symbol;
        
        TokenTier(int level, String displayName, ChatColor color, String symbol) {
            this.level = level;
            this.displayName = displayName;
            this.color = color;
            this.symbol = symbol;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
        public ChatColor getColor() { return color; }
        public String getSymbol() { return symbol; }
        
        public static TokenTier fromLevel(int level) {
            for (TokenTier tier : values()) {
                if (tier.level == level) return tier;
            }
            return BASIC;
        }
    }
    
    // Static map of token types with friendly display names
    private static final Map<String, TokenInfo> TOKEN_TYPES = new HashMap<>();
    
    static {
        // Main skill tokens - now with tier-specific materials and colors
        TOKEN_TYPES.put("mining", new TokenInfo("Mining Token", Material.IRON_INGOT, ChatColor.GRAY));
        TOKEN_TYPES.put("combat", new TokenInfo("Combat Token", Material.IRON_SWORD, ChatColor.RED));
        TOKEN_TYPES.put("farming", new TokenInfo("Farming Token", Material.WHEAT, ChatColor.GREEN));
        TOKEN_TYPES.put("fishing", new TokenInfo("Fishing Token", Material.FISHING_ROD, ChatColor.BLUE));
        TOKEN_TYPES.put("excavating", new TokenInfo("Excavating Token", Material.IRON_SHOVEL, ChatColor.YELLOW));
        TOKEN_TYPES.put("woodcutting", new TokenInfo("Woodcutting Token", Material.IRON_AXE, ChatColor.DARK_GREEN));
        TOKEN_TYPES.put("archery", new TokenInfo("Archery Token", Material.BOW, ChatColor.GOLD));
        TOKEN_TYPES.put("alchemy", new TokenInfo("Alchemy Token", Material.BREWING_STAND, ChatColor.DARK_PURPLE));
        TOKEN_TYPES.put("enchanting", new TokenInfo("Enchanting Token", Material.ENCHANTING_TABLE, ChatColor.LIGHT_PURPLE));
        TOKEN_TYPES.put("taming", new TokenInfo("Taming Token", Material.BONE, ChatColor.AQUA));
    }
        
    private final String tokenType;
    private final TokenTier tier;
    private final UUID tokenId;
    
    /**
     * Create a new skill token with tier
     * @param tokenType The type of token (should match skill/subskill ID)
     * @param tier The tier of the token
     */
    public SkillToken(String tokenType, TokenTier tier) {
        this.tokenType = tokenType;
        this.tier = tier;
        this.tokenId = UUID.randomUUID();
    }
    
    /**
     * Create a basic tier token (backward compatibility)
     * @param tokenType The type of token (should match skill/subskill ID)
     */
    public SkillToken(String tokenType) {
        this(tokenType, TokenTier.BASIC);
    }
    
    /**
     * Get the token type
     */
    public String getTokenType() {
        return tokenType;
    }
    
    /**
     * Get the token tier
     */
    public TokenTier getTier() {
        return tier;
    }
    
    /**
     * Get a unique ID for this token instance
     */
    public UUID getTokenId() {
        return tokenId;
    }
    
    /**
     * Create an item stack for this token
     */
    public ItemStack createItemStack() {
        TokenInfo info = TOKEN_TYPES.getOrDefault(
            tokenType, 
            new TokenInfo("Unknown Token", Material.PAPER, ChatColor.WHITE)
        );
        
        // Choose material based on tier
        Material material = getTierMaterial(info.material, tier);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Create tiered display name
        String tierPrefix = tier.getColor() + tier.getSymbol() + " " + tier.getDisplayName() + " ";
        meta.setDisplayName(tierPrefix + info.color + info.displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(tier.getColor() + "Tier " + tier.getLevel() + " " + tier.getDisplayName() + " Token");
        lore.add("");
        lore.add(ChatColor.GRAY + "Use this token to unlock " + tier.getDisplayName().toLowerCase());
        lore.add(ChatColor.GRAY + "tier nodes in the " + info.color + getSkillDisplayName());
        lore.add(ChatColor.GRAY + "" + info.color + " skill tree.");
        lore.add("");
        
        // Add tier-specific information
        switch (tier) {
            case BASIC:
                lore.add(ChatColor.GREEN + "✓ Unlocks basic skill nodes");
                lore.add(ChatColor.GRAY + "• Foundation abilities");
                lore.add(ChatColor.GRAY + "• Core improvements");
                break;
            case ADVANCED:
                lore.add(ChatColor.BLUE + "✓ Unlocks advanced skill nodes");
                lore.add(ChatColor.GRAY + "• Specialized techniques");
                lore.add(ChatColor.GRAY + "• Powerful bonuses");
                break;
            case MASTER:
                lore.add(ChatColor.LIGHT_PURPLE + "✓ Unlocks master skill nodes");
                lore.add(ChatColor.GRAY + "• Elite abilities");
                lore.add(ChatColor.GRAY + "• Game-changing effects");
                break;
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Right-click to open skill tree");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Get tier-appropriate material for a token
     */
    private Material getTierMaterial(Material baseMaterial, TokenTier tier) {
        // Upgrade materials based on tier
        switch (baseMaterial) {
            case IRON_INGOT:
                switch (tier) {
                    case BASIC: return Material.IRON_INGOT;
                    case ADVANCED: return Material.GOLD_INGOT;
                    case MASTER: return Material.NETHERITE_INGOT;
                }
                break;
            case IRON_SWORD:
                switch (tier) {
                    case BASIC: return Material.IRON_SWORD;
                    case ADVANCED: return Material.DIAMOND_SWORD;
                    case MASTER: return Material.NETHERITE_SWORD;
                }
                break;
            case IRON_PICKAXE:
                switch (tier) {
                    case BASIC: return Material.IRON_PICKAXE;
                    case ADVANCED: return Material.DIAMOND_PICKAXE;
                    case MASTER: return Material.NETHERITE_PICKAXE;
                }
                break;
            case IRON_SHOVEL:
                switch (tier) {
                    case BASIC: return Material.IRON_SHOVEL;
                    case ADVANCED: return Material.DIAMOND_SHOVEL;
                    case MASTER: return Material.NETHERITE_SHOVEL;
                }
                break;
            case IRON_AXE:
                switch (tier) {
                    case BASIC: return Material.IRON_AXE;
                    case ADVANCED: return Material.DIAMOND_AXE;
                    case MASTER: return Material.NETHERITE_AXE;
                }
                break;
            case WHEAT:
                switch (tier) {
                    case BASIC: return Material.WHEAT;
                    case ADVANCED: return Material.GOLDEN_CARROT;
                    case MASTER: return Material.ENCHANTED_GOLDEN_APPLE;
                }
                break;
            case FISHING_ROD:
                switch (tier) {
                    case BASIC: return Material.FISHING_ROD;
                    case ADVANCED: return Material.COD;
                    case MASTER: return Material.SALMON;
                }
                break;
            default:
                // For materials without clear upgrades
                switch (tier) {
                    case BASIC: return baseMaterial;
                    case ADVANCED: return Material.GOLD_NUGGET;
                    case MASTER: return Material.NETHER_STAR;
                }
                break;
        }
        return baseMaterial;
    }
    
    /**
     * Get the display name for this token's associated skill
     */
    private String getSkillDisplayName() {
        // Try to find a matching SkillType
        for (SkillType type : SkillType.values()) {
            if (type.getId().equals(tokenType)) {
                return type.getDisplayName();
            }
        }
        
        // Try to find a matching SubskillType
        for (SubskillType type : SubskillType.values()) {
            if (type.getId().equals(tokenType)) {
                return type.getDisplayName();
            }
        }
        
        return tokenType;
    }
    
    /**
     * Get the token info for a skill
     */
    public static TokenInfo getTokenInfo(Skill skill) {
        return TOKEN_TYPES.getOrDefault(
            skill.getId(), 
            new TokenInfo("Unknown Token", Material.PAPER, ChatColor.WHITE)
        );
    }
    
    /**
     * Get the token info for a token type
     */
    public static TokenInfo getTokenInfo(String tokenType) {
        return TOKEN_TYPES.getOrDefault(
            tokenType, 
            new TokenInfo("Unknown Token", Material.PAPER, ChatColor.WHITE)
        );
    }
    
    /**
     * Create a token key for storage (combines type and tier)
     */
    public String getTokenKey() {
        return tokenType + "_tier_" + tier.getLevel();
    }
    
    /**
     * Parse a token key back into type and tier
     */
    public static String[] parseTokenKey(String tokenKey) {
        String[] parts = tokenKey.split("_tier_");
        return new String[]{parts[0], parts.length > 1 ? parts[1] : "1"};
    }
    
    /**
     * Information about a token type
     */
    public static class TokenInfo {
        public final String displayName;
        public final Material material;
        public final ChatColor color;
        
        public TokenInfo(String displayName, Material material, ChatColor color) {
            this.displayName = displayName;
            this.material = material;
            this.color = color;
        }
    }
}