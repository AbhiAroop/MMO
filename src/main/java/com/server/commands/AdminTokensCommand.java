package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.server.profiles.skills.core.SubskillType;
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
        
        // Regular token commands
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /admintokens <player> <skill> <amount>");
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
            sender.sendMessage(ChatColor.YELLOW + "Use /admintokens list " + target.getName() + " to see available skills.");
            return true;
        }
        
        // Validate amount
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2] + ". Please provide a number.");
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
        
        // Store original value if not already tracked
        if (!originalTokens.containsKey(target.getUniqueId())) {
            originalTokens.put(target.getUniqueId(), new HashMap<>());
        }
        
        Map<String, Integer> playerOriginals = originalTokens.get(target.getUniqueId());
        if (!playerOriginals.containsKey(skillId)) {
            // Store original value for potential reset
            playerOriginals.put(skillId, treeData.getTokenCount(skillId));
        }
        
        // Set the new token amount
        treeData.setTokenCount(skillId, amount);
        
        // Get skill display name for nicer messages
        String skillName = getSkillDisplayName(skillId);
        
        // Notify admin and player
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + 
                ChatColor.GOLD + skillName + ChatColor.GREEN + " tokens to " + amount);
        
        target.sendMessage(ChatColor.YELLOW + "Your " + ChatColor.GOLD + skillName + 
                ChatColor.YELLOW + " tokens have been set to " + ChatColor.WHITE + amount);
        
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
        
        // Display all token counts
        sender.sendMessage(ChatColor.GOLD + "===== " + target.getName() + "'s Skill Tokens =====");
        
        // Display main skills first
        sender.sendMessage(ChatColor.YELLOW + "Main Skills:");
        
        // List token counts for main skills
        for (SkillType type : SkillType.values()) {
            String skillId = type.getId();
            String displayName = getSkillDisplayName(skillId);
            int tokens = treeData.getTokenCount(skillId);
            
            // Skip skills with 0 tokens to keep the list clean
            if (tokens > 0 || sender.hasPermission("mmo.admin.tokens.showempty")) {
                sender.sendMessage(ChatColor.GRAY + displayName + ": " + ChatColor.WHITE + tokens);
            }
        }
        
        // Display subskills
        sender.sendMessage(ChatColor.YELLOW + "Subskills:");
        
        // List token counts for subskills
        for (SubskillType type : SubskillType.values()) {
            String skillId = type.getId();
            String displayName = getSkillDisplayName(skillId);
            int tokens = treeData.getTokenCount(skillId);
            
            // Skip skills with 0 tokens to keep the list clean
            if (tokens > 0 || sender.hasPermission("mmo.admin.tokens.showempty")) {
                sender.sendMessage(ChatColor.GRAY + displayName + ": " + ChatColor.WHITE + tokens);
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
        sender.sendMessage(ChatColor.GOLD + "===== AdminTokens Command Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens <player> <skill> <amount> " + 
                        ChatColor.WHITE + "- Set skill tokens");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens reset <player> " + 
                        ChatColor.WHITE + "- Reset all tokens to original values");
        sender.sendMessage(ChatColor.YELLOW + "/admintokens list <player> " + 
                        ChatColor.WHITE + "- List all token counts");
        
        sender.sendMessage(ChatColor.GRAY + "Available Skills (Main Skills Only):");
        sender.sendMessage(ChatColor.WHITE + "  mining, excavating, fishing, farming, combat");
        
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.WHITE + "  /admintokens Steve mining 10");
        sender.sendMessage(ChatColor.WHITE + "  /admintokens Steve combat 5");
        sender.sendMessage(ChatColor.WHITE + "  /admintokens list Steve");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mmo.admin.tokens")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument: player name or special commands
            List<String> specialCommands = Arrays.asList("reset", "list");
            for (String special : specialCommands) {
                if (special.startsWith(args[0].toLowerCase())) {
                    completions.add(special);
                }
            }
            
            // Add online player names
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList()));
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("list")) {
                // Second argument is player name for special commands
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList()));
            } else {
                // UPDATED: Only suggest main skills
                List<String> mainSkills = Arrays.asList("mining", "excavating", "fishing", "farming", "combat");
                completions.addAll(mainSkills.stream()
                                .filter(skill -> skill.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList()));
            }
        }
        else if (args.length == 3) {
            // Third argument is the amount
            List<String> suggestions = Arrays.asList("1", "5", "10", "25", "50", "100");
            completions.addAll(suggestions.stream()
                            .filter(suggestion -> suggestion.startsWith(args[2]))
                            .collect(Collectors.toList()));
        }
        
        return completions;
    }
}