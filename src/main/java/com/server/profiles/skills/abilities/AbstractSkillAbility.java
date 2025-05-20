package com.server.profiles.skills.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Abstract base class for all skill abilities
 */
public abstract class AbstractSkillAbility implements SkillAbility {
    protected final String id;
    protected final String displayName;
    protected final String description;
    protected final String skillId;
    protected final Material icon;
    protected final String unlockRequirement;
    
    public AbstractSkillAbility(String id, String displayName, String description, 
                               String skillId, Material icon, String unlockRequirement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.skillId = skillId;
        this.icon = icon;
        this.unlockRequirement = unlockRequirement;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getSkillId() {
        return skillId;
    }
    
    @Override
    public Material getIcon() {
        return icon;
    }
    
    @Override
    public String getUnlockRequirement() {
        return unlockRequirement;
    }
    
    @Override
    public boolean isUnlocked(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        return profile.hasUnlockedAbility(id);
    }
    
    @Override
    public ItemStack createDisplayItem(Player player) {
        boolean unlocked = isUnlocked(player);
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with appropriate color
        meta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + displayName);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        
        if (unlocked) {
            lore.add(ChatColor.GREEN + "Unlocked!");
            
            // Add ability specific information
            addAbilityInfoToLore(lore, player);
        } else {
            lore.add(ChatColor.RED + "Locked");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Requirement:");
            lore.add(ChatColor.GRAY + unlockRequirement);
        }
        
        // Add skill ID and ability ID for identification in GUI handler
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        lore.add(ChatColor.BLACK + "ABILITY:" + id);
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + getAbilityType());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Add ability-specific information to the item lore
     */
    protected abstract void addAbilityInfoToLore(List<String> lore, Player player);
    
    /**
     * Get the type of ability (for GUI filtering)
     */
    protected abstract String getAbilityType();
}