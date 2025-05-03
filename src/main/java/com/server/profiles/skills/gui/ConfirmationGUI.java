package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.SkillTreeRegistry;

/**
 * GUI for confirming actions like skill tree resets
 */
public class ConfirmationGUI {
    // Constants
    public static final String GUI_TITLE_PREFIX = "Confirm: ";
    public static final String SKILL_TREE_RESET_ACTION = "RESET_SKILL_TREE";
    
    /**
     * Open a confirmation GUI for resetting a skill tree
     */
    public static void openResetConfirmationGUI(Player player, Skill skill) {
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Error: Invalid skill");
            return;
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Calculate tokens to refund
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skill.getId());
        
        int tokensToRefund = calculateTokensToRefund(tree, unlockedNodes, nodeLevels);
        int currentTokens = treeData.getTokenCount(skill.getId());
        
        // Get Premium Units balance
        int premiumUnits = profile.getPremiumUnits();
        final int RESET_COST = 10000; // Cost in Premium Units
        
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + "Reset " + skill.getDisplayName() + " Tree");
        
        // Add info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Reset Skill Tree");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "You are about to reset your");
        infoLore.add(ChatColor.GOLD + skill.getDisplayName() + ChatColor.GRAY + " skill tree.");
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "This will:");
        infoLore.add(ChatColor.RED + "• Lock all nodes");
        infoLore.add(ChatColor.GREEN + "• Refund " + tokensToRefund + " tokens");
        infoLore.add("");
        infoLore.add(ChatColor.GOLD + "Cost: " + ChatColor.RED + RESET_COST + " Premium Units");
        infoLore.add(ChatColor.YELLOW + "Your balance: " + (premiumUnits < RESET_COST ? ChatColor.RED : ChatColor.GREEN) + 
                    premiumUnits + " Premium Units");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Current tokens: " + currentTokens);
        infoLore.add(ChatColor.YELLOW + "After reset: " + (currentTokens + tokensToRefund));
        infoLore.add("");
        infoLore.add(ChatColor.RED + "This action cannot be undone!");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(13, infoItem);
        
        // Add confirm button (only enabled if player has enough Premium Units)
        Material confirmMaterial = premiumUnits >= RESET_COST ? Material.LIME_WOOL : Material.BARRIER;
        ItemStack confirmItem = new ItemStack(confirmMaterial);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(premiumUnits >= RESET_COST ? ChatColor.GREEN + "Confirm Reset" : ChatColor.RED + "Not Enough Premium Units");
        List<String> confirmLore = new ArrayList<>();
        
        if (premiumUnits >= RESET_COST) {
            confirmLore.add(ChatColor.GRAY + "Click to reset your skill tree");
            confirmLore.add(ChatColor.GRAY + "and receive " + tokensToRefund + " tokens back");
            confirmLore.add("");
            confirmLore.add(ChatColor.GOLD + "Cost: " + ChatColor.RED + RESET_COST + " Premium Units");
            confirmLore.add("");
            confirmLore.add(ChatColor.BLACK + "ACTION:" + SKILL_TREE_RESET_ACTION);
            confirmLore.add(ChatColor.BLACK + "SKILL_ID:" + skill.getId());
            confirmLore.add(ChatColor.BLACK + "TOKENS:" + tokensToRefund);
            confirmLore.add(ChatColor.BLACK + "COST:" + RESET_COST);
        } else {
            confirmLore.add(ChatColor.RED + "You need " + RESET_COST + " Premium Units");
            confirmLore.add(ChatColor.RED + "to reset this skill tree.");
            confirmLore.add("");
            confirmLore.add(ChatColor.RED + "Your balance: " + premiumUnits + " Premium Units");
            confirmLore.add(ChatColor.RED + "Missing: " + (RESET_COST - premiumUnits) + " Premium Units");
        }
        
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(11, confirmItem);
        
        // Add cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Click to cancel the reset");
        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(15, cancelItem);
        
        // Fill empty slots
        fillEmptySlots(gui);
        
        // Open the inventory
        player.openInventory(gui);
    }
    
    /**
     * Calculate how many tokens to refund for resetting a skill tree
     * This excludes special nodes that don't refund tokens
     */
    private static int calculateTokensToRefund(SkillTree tree, Set<String> unlockedNodes, Map<String, Integer> nodeLevels) {
        int tokensToRefund = 0;
        
        for (String nodeId : unlockedNodes) {
            SkillTreeNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            // Skip special nodes in the refund calculation
            if (node.isSpecialNode()) continue;
            
            int level = nodeLevels.getOrDefault(nodeId, 0);
            for (int i = 1; i <= level; i++) {
                tokensToRefund += node.getTokenCost(i);
            }
        }
        
        return tokensToRefund;
    }
    
    /**
     * Fill empty slots with filler items
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Handle a click on the confirm button
     */
    public static void handleConfirmAction(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || 
            !clickedItem.getItemMeta().hasLore()) {
            return;
        }
        
        List<String> lore = clickedItem.getItemMeta().getLore();
        String action = null;
        String skillId = null;
        int tokensToRefund = 0;
        int cost = 0;
        
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "ACTION:")) {
                action = line.substring((ChatColor.BLACK + "ACTION:").length());
            } else if (line.startsWith(ChatColor.BLACK + "SKILL_ID:")) {
                skillId = line.substring((ChatColor.BLACK + "SKILL_ID:").length());
            } else if (line.startsWith(ChatColor.BLACK + "TOKENS:")) {
                try {
                    tokensToRefund = Integer.parseInt(line.substring((ChatColor.BLACK + "TOKENS:").length()));
                } catch (NumberFormatException e) {
                    Main.getInstance().getLogger().warning("Error parsing token refund amount from confirmation GUI");
                }
            } else if (line.startsWith(ChatColor.BLACK + "COST:")) {
                try {
                    cost = Integer.parseInt(line.substring((ChatColor.BLACK + "COST:").length()));
                } catch (NumberFormatException e) {
                    Main.getInstance().getLogger().warning("Error parsing cost amount from confirmation GUI");
                }
            }
        }
        
        if (SKILL_TREE_RESET_ACTION.equals(action) && skillId != null) {
            resetSkillTree(player, skillId, tokensToRefund, cost);
        }
    }

    /**
     * Reset a skill tree and refund tokens
     */
    private static void resetSkillTree(Player player, String skillId, int tokensToRefund, int cost) {
        // Get the skill
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Error: Skill not found.");
            return;
        }
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "Error: Profile not found.");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Error: Profile not found.");
            return;
        }
        
        // Check if player has enough Premium Units
        int premiumUnits = profile.getPremiumUnits();
        if (premiumUnits < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Premium Units to reset this skill tree.");
            player.sendMessage(ChatColor.RED + "Required: " + cost + " Premium Units");
            player.sendMessage(ChatColor.RED + "Your balance: " + premiumUnits + " Premium Units");
            player.closeInventory();
            return;
        }
        
        // Deduct the premium units
        profile.removePremiumUnits(cost);
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Store which nodes were unlocked before reset
        Set<String> unlockedNodes = new HashSet<>(treeData.getUnlockedNodes(skillId));
        
        // Reset all unlocked nodes by actually removing them from the data structure
        for (String nodeId : unlockedNodes) {
            // Important: Actually remove the node from the unlocked nodes map
            // Setting level to 0 wasn't properly unlocking in the PlayerSkillTreeData implementation
            treeData.removeNode(skillId, nodeId);
        }
        
        // Add refunded tokens
        treeData.addTokens(skillId, tokensToRefund);
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "Your " + ChatColor.GOLD + skill.getDisplayName() + 
                        ChatColor.GREEN + " skill tree has been reset.");
        player.sendMessage(ChatColor.GREEN + "You have been refunded " + ChatColor.YELLOW + 
                        tokensToRefund + ChatColor.GREEN + " tokens.");
        player.sendMessage(ChatColor.RED + "" + cost + " Premium Units" + ChatColor.GREEN + " have been deducted from your account.");
        
        // Play sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.6f);
        
        // Tag the player to indicate a reset was performed
        player.setMetadata("skill_tree_reset", new FixedMetadataValue(Main.getInstance(), skillId + ":" + tokensToRefund));
        
        // Reopen the skill tree GUI with a brief delay to ensure the reset has fully processed
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            // Clear the view position to go back to the root view after reset
            SkillTreeGUI.clearPlayerViewPosition(player);
            SkillTreeGUI.openSkillTreeGUI(player, skill);
        }, 2L);
    }
}