package com.server.profiles.skills.abilities.passive;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.AbstractSkillAbility;

/**
 * Abstract base class for passive abilities
 */
public abstract class AbstractPassiveAbility extends AbstractSkillAbility implements PassiveAbility {
    
    public AbstractPassiveAbility(String id, String displayName, String description, 
                                String skillId, Material icon, String unlockRequirement) {
        super(id, displayName, description, skillId, icon, unlockRequirement);
    }
    
    @Override
    public boolean isEnabled(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        return profile.isAbilityEnabled(id);
    }
    
    @Override
    public boolean toggleEnabled(Player player) {
        boolean newState = !isEnabled(player);
        setEnabled(player, newState);
        return newState;
    }
    
    @Override
    public void setEnabled(Player player, boolean enabled) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        profile.setAbilityEnabled(id, enabled);
        
        if (enabled) {
            onEnable(player);
        } else {
            onDisable(player);
        }
    }
    
    @Override
    protected void addAbilityInfoToLore(List<String> lore, Player player) {
        boolean enabled = isEnabled(player);
        
        lore.add(ChatColor.YELLOW + "Status: " + (enabled ? 
            ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to " + (enabled ? "disable" : "enable"));
        
        // Add passive-specific details
        addPassiveDetailsToLore(lore);
    }
    
    @Override
    protected String getAbilityType() {
        return "PASSIVE";
    }
    
    /**
     * Add passive ability-specific details to the lore
     */
    protected abstract void addPassiveDetailsToLore(List<String> lore);
}