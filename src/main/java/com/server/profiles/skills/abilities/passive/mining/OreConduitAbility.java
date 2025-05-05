package com.server.profiles.skills.abilities.passive.mining;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
    
    /**
     * Get the current split percentage based on node level
     * @param player The player to check
     * @return The split percentage as a decimal (0.0 to 0.5)
     */
    public double getSplitPercentage(Player player) {
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

    @Override
    protected void addPassiveDetailsToLore(List<String> lore) {
        lore.add("");
        lore.add(ChatColor.GRAY + "When active, a portion of your");
        lore.add(ChatColor.GRAY + "OreExtraction XP is split to your");
        lore.add(ChatColor.GRAY + "Mining skill based on the node level.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Level 1: " + ChatColor.YELLOW + "0.5% split");
        lore.add(ChatColor.GRAY + "Level 50: " + ChatColor.YELLOW + "25% split");
        lore.add(ChatColor.GRAY + "Level 100: " + ChatColor.YELLOW + "50% split (max)");
        lore.add("");
        lore.add(ChatColor.GRAY + "This ability helps level up your main");
        lore.add(ChatColor.GRAY + "Mining skill while focusing on OreExtraction.");
    }
}