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
 */
public class SkillToken {
    // Static map of token types with friendly display names
    private static final Map<String, TokenInfo> TOKEN_TYPES = new HashMap<>();
    
    static {
        // Main skills tokens
        TOKEN_TYPES.put("mining", new TokenInfo("Mining Crystal", Material.DIAMOND, ChatColor.AQUA));
        TOKEN_TYPES.put("excavating", new TokenInfo("Earth Shard", Material.CLAY_BALL, ChatColor.GOLD));
        TOKEN_TYPES.put("fishing", new TokenInfo("Aquatic Essence", Material.PRISMARINE_CRYSTALS, ChatColor.BLUE));
        TOKEN_TYPES.put("farming", new TokenInfo("Growth Seed", Material.WHEAT_SEEDS, ChatColor.GREEN));
        TOKEN_TYPES.put("combat", new TokenInfo("Warrior Mark", Material.BLAZE_POWDER, ChatColor.RED));
        
        // Mining subskill tokens
        TOKEN_TYPES.put("ore_extraction", new TokenInfo("Ore Fragment", Material.RAW_IRON, ChatColor.YELLOW));
        TOKEN_TYPES.put("gem_carving", new TokenInfo("Gem Dust", Material.EMERALD, ChatColor.GREEN));
        
        // Excavating subskill tokens
        TOKEN_TYPES.put("treasure_hunter", new TokenInfo("Ancient Coin", Material.GOLD_NUGGET, ChatColor.GOLD));
        TOKEN_TYPES.put("soil_master", new TokenInfo("Rich Soil", Material.DIRT, ChatColor.DARK_GREEN));
        
        // Fishing subskill tokens
        TOKEN_TYPES.put("fisherman", new TokenInfo("Fishing Hook", Material.TRIPWIRE_HOOK, ChatColor.BLUE));
        TOKEN_TYPES.put("aquatic_treasures", new TokenInfo("Pearl Fragment", Material.NAUTILUS_SHELL, ChatColor.AQUA));
        
        // Farming subskill tokens
        TOKEN_TYPES.put("crop_growth", new TokenInfo("Nurturing Pollen", Material.BONE_MEAL, ChatColor.GREEN));
        TOKEN_TYPES.put("animal_breeder", new TokenInfo("Vital Essence", Material.EGG, ChatColor.YELLOW));
        
        // Combat subskill tokens
        TOKEN_TYPES.put("swordsmanship", new TokenInfo("Blade Shard", Material.IRON_SWORD, ChatColor.GRAY));
        TOKEN_TYPES.put("archery", new TokenInfo("Bowstring Fiber", Material.STRING, ChatColor.WHITE));
        TOKEN_TYPES.put("defense", new TokenInfo("Shield Fragment", Material.IRON_INGOT, ChatColor.DARK_GRAY));
    }
    
    private final String tokenType;
    private final UUID tokenId;
    
    /**
     * Create a new skill token
     * @param tokenType The type of token (should match skill/subskill ID)
     */
    public SkillToken(String tokenType) {
        this.tokenType = tokenType;
        this.tokenId = UUID.randomUUID();
    }
    
    /**
     * Get the token type
     */
    public String getTokenType() {
        return tokenType;
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
        
        ItemStack item = new ItemStack(info.material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(info.color + info.displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Use this token to unlock a node");
        lore.add(ChatColor.GRAY + "in the " + info.color + getSkillDisplayName() + ChatColor.GRAY + " skill tree.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Right-click to open skill tree");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
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