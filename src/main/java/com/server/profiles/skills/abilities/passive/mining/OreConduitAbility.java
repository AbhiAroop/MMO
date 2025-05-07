package com.server.profiles.skills.abilities.passive.mining;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.passive.AbstractPassiveAbility;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * Ore Conduit ability - Splits XP between OreExtraction and Mining skills
 */
public class OreConduitAbility extends AbstractPassiveAbility {
    
    public OreConduitAbility() {
        super(
            "ore_conduit",
            "Ore Conduit",
            "Split a portion of your OreExtraction XP into Mining XP",
            SubskillType.ORE_EXTRACTION.getId(),
            Material.CONDUIT,
            "Unlock the Ore Conduit node in the Ore Extraction skill tree"
        );
    }

    @Override
    public void onEnable(Player player) {
        player.sendMessage(ChatColor.GREEN + "Ore Conduit " + ChatColor.WHITE + "ability enabled. " +
                          "XP will be split between OreExtraction and Mining skills.");
    }

    @Override
    public void onDisable(Player player) {
        player.sendMessage(ChatColor.RED + "Ore Conduit " + ChatColor.WHITE + "ability disabled. " +
                          "XP will no longer be split to the Mining skill.");
    }
    
    @Override
    public boolean isUnlocked(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        return treeData.isNodeUnlocked("ore_extraction", "ore_conduit");
    }
    
    @Override
    protected void addPassiveDetailsToLore(List<String> lore) {
        lore.add("");
        lore.add(ChatColor.GRAY + "When active, a portion of your");
        lore.add(ChatColor.GRAY + "OreExtraction XP is split to your");
        lore.add(ChatColor.GRAY + "Mining skill based on the node level.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "◆ Node Level 1: " + ChatColor.WHITE + "0.5% split");
        lore.add(ChatColor.YELLOW + "◆ Node Level 50: " + ChatColor.WHITE + "25% split");
        lore.add(ChatColor.YELLOW + "◆ Node Level 100: " + ChatColor.WHITE + "50% split (max)");
        lore.add("");
        lore.add(ChatColor.GRAY + "This ability helps level up your main");
        lore.add(ChatColor.GRAY + "Mining skill while focusing on OreExtraction.");
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "LEFT-CLICK" + ChatColor.YELLOW + " to toggle on/off");
        lore.add(ChatColor.LIGHT_PURPLE + "• " + ChatColor.YELLOW + "RIGHT-CLICK" + ChatColor.YELLOW + " to configure");
    }

    /**
     * Get the user's custom split percentage
     * @param player The player
     * @return The custom split percentage, or -1 if not set
     */
    public double getUserSplitPercentage(Player player) {
        // Check if the player has a custom setting stored in metadata
        if (player.hasMetadata("oreconduit_split_percent")) {
            double setting = player.getMetadata("oreconduit_split_percent").get(0).asDouble();
            
            // Ensure setting is valid
            if (setting < 0 || setting > 50.0) {
                return -1; // Invalid value, return -1 to use default
            }
            
            return setting;
        }
        return -1; // Default to node level based percentage
    }

    /**
     * Set the user's custom split percentage
     * @param player The player
     * @param percentage The split percentage (0-50)
     */
    public void setUserSplitPercentage(Player player, double percentage) {
        // Ensure value is within valid range
        double setValue = Math.max(0, Math.min(50.0, percentage));
        
        // Round to 1 decimal place
        setValue = Math.round(setValue * 10) / 10.0;
        
        // Store the setting
        player.setMetadata("oreconduit_split_percent", new FixedMetadataValue(Main.getInstance(), setValue));
        
        // Log for debugging
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Set OreConduit split percentage for " + 
                                            player.getName() + " to " + setValue + "%");
        }
    }

    public double getSplitPercentage(Player player) {
        // Check for custom user setting first
        double userSetting = getUserSplitPercentage(player);
        if (userSetting >= 0) {
            // Convert percentage to decimal (divide by 100)
            return userSetting / 100.0;
        }
        
        // Otherwise use node-level based setting
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int level = treeData.getNodeLevel("ore_extraction", "ore_conduit");
        
        // Each level gives +0.5% XP split to Mining skill, up to 50% at level 100
        // Fix: Use 0.005 (0.5%) instead of 0.5 (50%) per level
        return Math.min(0.5, level * 0.005); // 0.5% per level = 0.005 in decimal
    }
}