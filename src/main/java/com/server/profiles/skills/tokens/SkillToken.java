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
        // Main skill tokens only
        TOKEN_TYPES.put("mining", new TokenInfo("Mining Token", Material.IRON_INGOT, ChatColor.GRAY));
        TOKEN_TYPES.put("combat", new TokenInfo("Combat Token", Material.IRON_SWORD, ChatColor.RED));
        TOKEN_TYPES.put("farming", new TokenInfo("Farming Token", Material.WHEAT, ChatColor.GREEN));
        TOKEN_TYPES.put("fishing", new TokenInfo("Fishing Token", Material.FISHING_ROD, ChatColor.BLUE));
        TOKEN_TYPES.put("excavation", new TokenInfo("Excavation Token", Material.IRON_SHOVEL, ChatColor.YELLOW));
        TOKEN_TYPES.put("woodcutting", new TokenInfo("Woodcutting Token", Material.IRON_AXE, ChatColor.DARK_GREEN));
        TOKEN_TYPES.put("archery", new TokenInfo("Archery Token", Material.BOW, ChatColor.GOLD));
        TOKEN_TYPES.put("alchemy", new TokenInfo("Alchemy Token", Material.BREWING_STAND, ChatColor.DARK_PURPLE));
        TOKEN_TYPES.put("enchanting", new TokenInfo("Enchanting Token", Material.ENCHANTING_TABLE, ChatColor.LIGHT_PURPLE));
        TOKEN_TYPES.put("taming", new TokenInfo("Taming Token", Material.BONE, ChatColor.AQUA));
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