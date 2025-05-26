package com.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * Command to manage player skill tokens for testing and administrative purposes
 * Usage: /admintokens <player> <skill> <amount>
 * Permission: mmo.admin.tokens
 */
public class AdminTokensCommand implements TabExecutor {
    
    private final Main plugin;
    
    // Track original token values for reset
    private Map<UUID, Map<String, Integer>> originalTokens = new HashMap<>();
    
    public AdminTokensCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.tokens")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Display help if not enough arguments
        if (args.length < 2) {
            displayHelp(sender);
            return true;
        }
        
        // Handle special commands first
        if (args[0].equalsIgnoreCase("reset") && args.length > 1) {
            return handleReset(sender, args[1]);
        }
        
        if (args[0].equalsIgnoreCase("list") && args.length > 1) {
            return handleList(sender, args[1]);
        }
        
        // NEW: Handle tiered token commands
        if (args.length >= 4) {
            return handleTieredTokenCommand(sender, args);
        }
        
        // Regular token commands (backwards compatibility)
        if (args.length < 3) {
            displayHelp(sender);
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        // Get skill ID (case-insensitive)
        String skillArg = args[1].toLowerCase();
        String skillId = getSkillIdFromInput(skillArg);
        
        if (skillId == null) {
            sender.sendMessage(ChatColor.RED + "Unknown skill: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Available skills: " + String.join(", ", getAvailableSkillIds()));
            return true;
        }
        
        // Validate amount
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
            return true;
        }
        
        // Get player's active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + "Player has no active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile not found.");
            return true;
        }
        
        // Get the skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) {
            sender.sendMessage(ChatColor.RED + "Player skill tree data not found.");
            return true;
        }
        
        // Store original value if not already tracked
        if (!originalTokens.containsKey(target.getUniqueId())) {
            originalTokens.put(target.getUniqueId(), new HashMap<>());
        }
        
        Map<String, Integer> playerOriginals = originalTokens.get(target.getUniqueId());
        if (!playerOriginals.containsKey(skillId)) {
            playerOriginals.put(skillId, treeData.getTokenCount(skillId));
        }
        
        // Set the new token amount (defaults to Basic tier for backwards compatibility)
        treeData.setTokenCount(skillId, SkillToken.TokenTier.BASIC, amount);
        
        // Get skill display name for nicer messages
        String skillName = getSkillDisplayName(skillId);
        
        // Notify admin and player
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                ChatColor.GOLD + skillName + ChatColor.GREEN + " Basic tokens to " + amount);
        
        if (!sender.equals(target)) {
            target.sendMessage(ChatColor.GREEN + "Your " + ChatColor.GOLD + skillName + 
                            ChatColor.GREEN + " Basic tokens have been set to " + amount);
        }
        
        return true;
    }

    /**
     * Handle tiered token commands
     * Usage: /admintokens <player> <skill> <tier> <amount>
     */
    private boolean handleTieredTokenCommand(CommandSender sender, String[] args) {
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        // Get skill ID
        String skillArg = args[1].toLowerCase();
        String skillId = getSkillIdFromInput(skillArg);
        
        if (skillId == null) {
            sender.sendMessage(ChatColor.RED + "Unknown skill: " + args[1]);
            return true;
        }
        
        // Get tier
        SkillToken.TokenTier tier = null;
        String tierArg = args[2].toLowerCase();
        switch (tierArg) {
            case "basic":
            case "b":
            case "1":
                tier = SkillToken.TokenTier.BASIC;
                break;
            case "advanced":
            case "adv":
            case "a":
            case "2":
                tier = SkillToken.TokenTier.ADVANCED;
                break;
            case "master":
            case "m":
            case "3":
                tier = SkillToken.TokenTier.MASTER;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid tier: " + args[2]);
                sender.sendMessage(ChatColor.YELLOW + "Available tiers: basic, advanced, master");
                return true;
        }
        
        // Validate amount
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
            return true;
        }
        
        // Get player's profile and tree data
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + "Player has no active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile not found.");
            return true;
        }
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) {
            sender.sendMessage(ChatColor.RED + "Player skill tree data not found.");
            return true;
        }
        
        // Store original value for reset functionality
        String resetKey = skillId + "_" + tier.name().toLowerCase();
        if (!originalTokens.containsKey(target.getUniqueId())) {
            originalTokens.put(target.getUniqueId(), new HashMap<>());
        }
        
        Map<String, Integer> playerOriginals = originalTokens.get(target.getUniqueId());
        if (!playerOriginals.containsKey(resetKey)) {
            playerOriginals.put(resetKey, treeData.getTokenCount(skillId, tier));
        }
        
        // Set the new token amount for the specific tier
        treeData.setTokenCount(skillId, tier, amount);
        
        // Get skill display name for nicer messages
        String skillName = getSkillDisplayName(skillId);
        
        // Notify admin and player
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                ChatColor.GOLD + skillName + " " + tier.getColor() + tier.getDisplayName() + 
                ChatColor.GREEN + " tokens to " + amount);
        
        if (!sender.equals(target)) {
            target.sendMessage(ChatColor.GREEN + "Your " + ChatColor.GOLD + skillName + " " + 
                            tier.getColor() + tier.getDisplayName() + ChatColor.GREEN + 
                            " tokens have been set to " + amount);
        }
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Check if we have original values stored
        if (!originalTokens.containsKey(target.getUniqueId()) || 
            originalTokens.get(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No modified tokens to reset for " + target.getName());
            return true;
        }
        
        // Get player's active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have an active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get " + target.getName() + "'s profile.");
            return true;
        }
        
        // Get the skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) {
            sender.sendMessage(ChatColor.RED + "Failed to get skill tree data for " + target.getName());
            return true;
        }
        
        // Reset all stored tokens
        Map<String, Integer> playerOriginals = originalTokens.get(target.getUniqueId());
        for (Map.Entry<String, Integer> entry : playerOriginals.entrySet()) {
            String skillId = entry.getKey();
            int originalValue = entry.getValue();
            
            treeData.setTokenCount(skillId, originalValue);
        }
        
        // Clear stored values
        originalTokens.remove(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GREEN + "Reset all modified tokens for " + target.getName() + " to original values.");
        target.sendMessage(ChatColor.YELLOW + "Your skill token values have been reset to their original values.");
        
        return true;
    }
    
    private boolean handleList(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }
        
        // Get player's profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + "Player has no active profile.");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile not found.");
            return true;
        }
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) {
            sender.sendMessage(ChatColor.RED + "Player skill tree data not found.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== " + target.getName() + "'s Skill Tokens ===");
        
        // List tokens for each skill
        for (String skillId : getAvailableSkillIds()) {
            String skillName = getSkillDisplayName(skillId);
            Map<SkillToken.TokenTier, Integer> tokenCounts = treeData.getAllTokenCounts(skillId);
            int totalTokens = treeData.getTokenCount(skillId);
            
            if (totalTokens > 0) {
                sender.sendMessage(ChatColor.YELLOW + skillName + ":");
                for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
                    int count = tokenCounts.getOrDefault(tier, 0);
                    if (count > 0) {
                        sender.sendMessage("  " + tier.getColor() + tier.getSymbol() + " " + 
                                        tier.getDisplayName() + ": " + ChatColor.WHITE + count);
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get a skill ID from a more flexible input format
     */
    private String getSkillIdFromInput(String input) {
        // Convert to lowercase for case-insensitive matching
        String lowerInput = input.toLowerCase();
                
        // Direct skill ID match (main skills only)
        for (SkillType skillType : SkillType.values()) {
            if (skillType.getId().equals(lowerInput)) {
                return skillType.getId();
            }
        }
        
        // Display name match (main skills only)
        for (SkillType skillType : SkillType.values()) {
            if (skillType.getDisplayName().toLowerCase().equals(lowerInput)) {
                return skillType.getId();
            }
            
            // Also check without spaces
            if (skillType.getDisplayName().toLowerCase().replace(" ", "").equals(lowerInput)) {
                return skillType.getId();
            }
        }
        
        // Partial matches for main skills only
        Map<String, String> partialMatches = new HashMap<>();
        partialMatches.put("mine", "mining");
        partialMatches.put("dig", "excavating");
        partialMatches.put("fish", "fishing");
        partialMatches.put("farm", "farming");
        partialMatches.put("fight", "combat");
        partialMatches.put("combat", "combat");
        
        return partialMatches.get(lowerInput);
    }

    /**
     * Get a skill's display name from its ID
     */
    private String getSkillDisplayName(String skillId) {
        // UPDATED: Only handle main skills
        for (SkillType skillType : SkillType.values()) {
            if (skillType.getId().equals(skillId)) {
                return skillType.getDisplayName();
            }
        }
        
        // If not found, format the ID nicely
        return formatSkillName(skillId);
    }
    
    /**
     * Format a skill ID to be more readable
     */
    private String formatSkillName(String skillId) {
        StringBuilder formatted = new StringBuilder();
        boolean nextUpper = true;
        
        for (char c : skillId.toCharArray()) {
            if (c == '_' || c == '.') {
                formatted.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                formatted.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    private void displayHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Admin Tokens Command Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens <player> <skill> <amount>");
        sender.sendMessage(ChatColor.GRAY + "  Set Basic tier tokens for a skill (backwards compatible)");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens <player> <skill> <tier> <amount>");
        sender.sendMessage(ChatColor.GRAY + "  Set specific tier tokens for a skill");
        sender.sendMessage(ChatColor.GRAY + "  Tiers: basic, advanced, master");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens list <player>");
        sender.sendMessage(ChatColor.GRAY + "  List all tokens for a player");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens reset <player>");
        sender.sendMessage(ChatColor.GRAY + "  Reset all tokens to original values");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "Available skills: " + String.join(", ", getAvailableSkillIds()));
        sender.sendMessage(ChatColor.AQUA + "Available tiers: " + 
                        SkillToken.TokenTier.BASIC.getDisplayName() + ", " +
                        SkillToken.TokenTier.ADVANCED.getDisplayName() + ", " +
                        SkillToken.TokenTier.MASTER.getDisplayName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Player names + special commands
            completions.add("list");
            completions.add("reset");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("reset")) {
                // Player names for list/reset commands
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else {
                // Skill names
                completions.addAll(getAvailableSkillIds());
            }
        } else if (args.length == 3) {
            if (!args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("reset")) {
                // Could be tier or amount (old format)
                completions.add("basic");
                completions.add("advanced");
                completions.add("master");
                completions.add("1");
                completions.add("5");
                completions.add("10");
            }
        } else if (args.length == 4) {
            // Amount for tiered command
            completions.add("1");
            completions.add("5");
            completions.add("10");
            completions.add("25");
            completions.add("50");
        }
        
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Get available skill IDs for tab completion
     */
    private List<String> getAvailableSkillIds() {
        List<String> skillIds = new ArrayList<>();
        
        // Add main skill IDs
        for (SkillType type : SkillType.values()) {
            skillIds.add(type.getId());
        }
        
        return skillIds;
    }
}